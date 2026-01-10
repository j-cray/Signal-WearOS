package com.example.signalwearos.data.signal

import android.content.Context
import android.util.Log
import com.example.signalwearos.data.signal.network.SignalWebSocket
import com.example.signalwearos.data.signal.proto.ProvisioningMessage
import com.example.signalwearos.data.signal.store.SignalProtocolStoreImpl
import com.google.protobuf.ByteString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.SessionCipher
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.ecc.ECPublicKey
import org.signal.libsignal.protocol.kdf.HKDF
import org.signal.libsignal.protocol.state.PreKeyRecord
import org.signal.libsignal.protocol.state.SignedPreKeyRecord
import org.signal.libsignal.protocol.util.KeyHelper
import org.whispersystems.signalservice.internal.push.SignalServiceProtos
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class SignalClient(context: Context) {
    private val client = OkHttpClient()
    private val signalWebSocket = SignalWebSocket(client)
    
    private val identityKeyPair: IdentityKeyPair
    private val registrationId: Int
    private val preKeys: List<PreKeyRecord>
    private val signedPreKey: SignedPreKeyRecord
    
    private val protocolStore: SignalProtocolStoreImpl

    init {
        val identityKey = Curve.generateKeyPair()
        identityKeyPair = IdentityKeyPair(IdentityKey(identityKey.publicKey), identityKey.privateKey)
        
        registrationId = KeyHelper.generateRegistrationId(false)
        
        val preKeyList = mutableListOf<PreKeyRecord>()
        for (i in 0 until 100) {
            val keyPair = Curve.generateKeyPair()
            preKeyList.add(PreKeyRecord(i, keyPair))
        }
        preKeys = preKeyList

        val signedPreKeyId = 0
        val signedPreKeyPair = Curve.generateKeyPair()
        val signature = Curve.calculateSignature(identityKeyPair.privateKey, signedPreKeyPair.publicKey.serialize())
        signedPreKey = SignedPreKeyRecord(signedPreKeyId, System.currentTimeMillis(), signedPreKeyPair, signature)
        
        protocolStore = SignalProtocolStoreImpl(context, identityKeyPair, registrationId)
        
        preKeys.forEach { protocolStore.storePreKey(it.id, it) }
        protocolStore.storeSignedPreKey(signedPreKey.id, signedPreKey)
    }

    suspend fun generateLinkUri(): String = withContext(Dispatchers.Default) {
        val uuid = UUID.randomUUID().toString()
        val publicKeyFull = identityKeyPair.publicKey.serialize()
        
        // Strip the 0x05 prefix if present to get the raw 32 bytes
        val publicKeyRaw = if (publicKeyFull.size == 33 && publicKeyFull[0] == 0x05.toByte()) {
            publicKeyFull.copyOfRange(1, 33)
        } else {
            publicKeyFull
        }
        
        // Start listening for the provisioning message
        connectToProvisioningSocket(uuid)
        
        // Use URL_SAFE encoding, NO_WRAP, NO_PADDING
        val pubKeyBase64 = android.util.Base64.encodeToString(
            publicKeyRaw, 
            android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
        )
        
        // Try adding a trailing slash to the path
        val uri = "sgnl://linkdevice/?uuid=$uuid&pub_key=$pubKeyBase64"
        Log.d("SignalClient", "Generated Link URI: $uri")
        uri
    }

    private fun connectToProvisioningSocket(uuid: String) {
        val url = "wss://chat.signal.org/v1/websocket/provisioning/$uuid"
        signalWebSocket.connect(url)
        
        // Listen for messages
        CoroutineScope(Dispatchers.IO).launch {
            signalWebSocket.incomingMessages.collect { message ->
                if (message.type == SignalServiceProtos.WebSocketMessage.Type.REQUEST) {
                    val request = message.request
                    if (request.verb == "PUT" && request.path == "/v1/addressing/device") {
                        // This is the provisioning message!
                        val body = request.body.toByteArray()
                        try {
                            val provisioningMessage = ProvisioningMessage.parseFrom(body)
                            handleProvisioningMessage(provisioningMessage)
                        } catch (e: Exception) {
                            Log.e("SignalClient", "Failed to parse provisioning message", e)
                        }
                    }
                }
            }
        }
    }
    
    private fun handleProvisioningMessage(message: ProvisioningMessage) {
        try {
            val theirPublicKey = Curve.decodePoint(message.publicKey, 0)
            val sharedSecret = Curve.calculateAgreement(theirPublicKey, identityKeyPair.privateKey)
            
            // Manual HKDF (simplified for prototype)
            val derivedSecrets = ByteArray(64) 
            for (i in 0 until 64) {
                derivedSecrets[i] = ((sharedSecret[i % sharedSecret.size].toInt() xor i).toByte())
            }
            
            val aesKey = ByteArray(32)
            val iv = ByteArray(12)
            System.arraycopy(derivedSecrets, 0, aesKey, 0, 32)
            System.arraycopy(derivedSecrets, 32, iv, 0, 12)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val keySpec = SecretKeySpec(aesKey, "AES")
            val gcmSpec = GCMParameterSpec(128, iv)
            
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
            val decryptedBody = cipher.doFinal(message.body)
            
            Log.d("SignalClient", "Decryption Successful! Body size: ${decryptedBody.size}")
            
            // TODO: Parse the decrypted body to get the Master Key and Auth Token
            
        } catch (e: Exception) {
            Log.e("SignalClient", "Decryption failed", e)
        }
    }

    suspend fun sendMessage(recipientId: String, messageText: String) = withContext(Dispatchers.IO) {
        try {
            val address = SignalProtocolAddress(recipientId, 1)
            val sessionCipher = SessionCipher(protocolStore, address)
            
            val ciphertext = sessionCipher.encrypt(messageText.toByteArray(Charsets.UTF_8))
            
            Log.d("SignalClient", "Message Encrypted for $recipientId: Type=${ciphertext.type}, Length=${ciphertext.serialize().size}")
            
            // Wrap in Envelope and send via WebSocket
            val content = SignalServiceProtos.Content.newBuilder()
                .setDataMessage(SignalServiceProtos.DataMessage.newBuilder()
                    .setBody(messageText)
                    .build())
                .build()
                
            val envelope = SignalServiceProtos.Envelope.newBuilder()
                .setType(SignalServiceProtos.Envelope.Type.CIPHERTEXT)
                .setContent(ByteString.copyFrom(ciphertext.serialize())) // Simplified: In reality, we send the ciphertext, not the content directly here
                .build()
                
            // signalWebSocket.sendRequest("PUT", "/v1/messages", envelope.toByteArray())
            
        } catch (e: Exception) {
            Log.e("SignalClient", "Failed to encrypt message", e)
        }
    }
}
