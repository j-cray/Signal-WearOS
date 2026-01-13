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
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
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
        
        // Use the full key (with 0x05 prefix)
        // Use URL_SAFE encoding, NO_WRAP, NO_PADDING
        val pubKeyBase64 = android.util.Base64.encodeToString(
            publicKeyFull, 
            android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
        )
        
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
                        val body: ByteArray = request.body.toByteArray()
                        try {
                            val provisioningMessage = SignalServiceProtos.ProvisioningMessage.parseFrom(body)
                            handleProvisioningMessage(provisioningMessage)
                        } catch (e: Exception) {
                            Log.e("SignalClient", "Failed to parse provisioning message", e)
                        }
                    }
                }
            }
        }
    }
    
    private fun handleProvisioningMessage(message: SignalServiceProtos.ProvisioningMessage) {
        try {
            // Use getters to avoid property access issues
            val publicKeyBytes = message.getPublicKey().toByteArray()
            val theirPublicKey = Curve.decodePoint(publicKeyBytes, 0)
            
            val sharedSecret = Curve.calculateAgreement(theirPublicKey, identityKeyPair.privateKey)
            
            // Correct HKDF derivation
            val info = "TextSecure Provisioning Message".toByteArray()
            val derivedSecrets = HKDF.deriveSecrets(sharedSecret, info, 64)
            
            val aesKey = ByteArray(32)
            val macKey = ByteArray(32)
            System.arraycopy(derivedSecrets, 0, aesKey, 0, 32)
            System.arraycopy(derivedSecrets, 32, macKey, 0, 32)

            // Parse Body Structure: Version(1) + IV(16) + Ciphertext(...) + MAC(32)
            val body: ByteArray = message.getBody().toByteArray()

            if (body.size < 1 + 16 + 32) {
                Log.e("SignalClient", "Body too short")
                return
            }

            val version = body[0]
            if (version != 0x01.toByte()) {
                Log.e("SignalClient", "Invalid version: $version")
                return
            }

            val macOffset = body.size - 32
            val receivedMac = body.copyOfRange(macOffset, body.size)
            val ivAndCiphertext = body.copyOfRange(1, macOffset)
            
            // Verify MAC
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(macKey, "HmacSHA256"))
            mac.update(body, 0, macOffset) // Version + IV + Ciphertext
            val calculatedMac = mac.doFinal()
            
            if (!java.util.Arrays.equals(receivedMac, calculatedMac)) {
                Log.e("SignalClient", "MAC verification failed")
                return
            }

            // Decrypt
            val iv = ivAndCiphertext.copyOfRange(0, 16)
            val ciphertext = ivAndCiphertext.copyOfRange(16, ivAndCiphertext.size)
            
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(aesKey, "AES"), IvParameterSpec(iv))
            val plaintext = cipher.doFinal(ciphertext)
            
            Log.d("SignalClient", "Decryption Successful! Plaintext size: ${plaintext.size}")
            
            // TODO: Parse the plaintext (ProvisionMessage) to get the Master Key
            
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
