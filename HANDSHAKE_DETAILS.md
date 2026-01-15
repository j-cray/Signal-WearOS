# Handshake Implementation Details

This document provides technical details about the phone-watch handshake implementation.

## Overview

The handshake uses the Signal Protocol's device linking mechanism, which allows a secondary device (Wear OS watch) to be linked to a primary device (phone) using end-to-end encryption.

## Step-by-Step Process

### 1. Key Generation (Watch)

When the app starts, the watch generates cryptographic material:

```kotlin
// Identity Key Pair (long-term identity)
val identityKey = Curve.generateKeyPair()
identityKeyPair = IdentityKeyPair(
    IdentityKey(identityKey.publicKey), 
    identityKey.privateKey
)

// Registration ID (random identifier)
registrationId = KeyHelper.generateRegistrationId(false)

// Pre-Keys (100 one-time use keys)
for (i in 0 until 100) {
    val keyPair = Curve.generateKeyPair()
    preKeyList.add(PreKeyRecord(i, keyPair))
}

// Signed Pre-Key (signed with identity key)
val signedPreKeyPair = Curve.generateKeyPair()
val signature = Curve.calculateSignature(
    identityKeyPair.privateKey, 
    signedPreKeyPair.publicKey.serialize()
)
signedPreKey = SignedPreKeyRecord(
    signedPreKeyId, 
    System.currentTimeMillis(), 
    signedPreKeyPair, 
    signature
)
```

**Keys Stored:**
- Identity Key Pair → `SignalProtocolStoreImpl`
- Pre-Keys (0-99) → `SignalProtocolStoreImpl`
- Signed Pre-Key → `SignalProtocolStoreImpl`

### 2. Link URI Generation

The watch creates a URI containing:
- UUID for WebSocket session identification
- Base64-encoded public key for ECDH

```kotlin
suspend fun generateLinkUri(): String {
    val uuid = UUID.randomUUID().toString()
    val publicKey = identityKeyPair.publicKey.serialize()
    
    return "tsdevice:/?uuid=$uuid&pub_key=${
        android.util.Base64.encodeToString(publicKey, android.util.Base64.NO_WRAP)
    }"
}
```

**URI Format:**
```
tsdevice:/?uuid=<UUID>&pub_key=<BASE64_ENCODED_PUBLIC_KEY>
```

### 3. QR Code Display

The link URI is encoded into a QR code:

```kotlin
val bitMatrix: BitMatrix = MultiFormatWriter().encode(
    content,
    BarcodeFormat.QR_CODE,
    width,
    height
)
```

The user scans this QR code with their Signal app on the phone.

### 4. WebSocket Connection

The watch connects to the provisioning WebSocket:

```kotlin
fun connectToWebSocket(uuid: String) {
    val request = Request.Builder()
        .url("wss://chat.signal.org/v1/websocket/provisioning/$uuid")
        .build()

    webSocket = client.newWebSocket(request, listener)
}
```

**WebSocket Endpoint:**
```
wss://chat.signal.org/v1/websocket/provisioning/<UUID>
```

The watch listens for provisioning messages from the phone.

### 5. Provisioning Message Reception

When the phone scans the QR code, it sends a provisioning message:

**Message Structure (Protobuf):**
```protobuf
message ProvisioningMessage {
    optional string uuid = 1;
    optional bytes publicKey = 2;  // Phone's ephemeral public key
    optional bytes body = 3;       // Encrypted payload
}
```

**Received via WebSocket:**
```kotlin
override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
    val message = ProvisioningMessage.parseFrom(bytes.toByteArray())
    handleProvisioningMessage(message)
}
```

### 6. Key Derivation (ECDH + HKDF)

The watch decrypts the provisioning message:

```kotlin
// 1. Decode phone's ephemeral public key
val theirPublicKey = Curve.decodePoint(message.publicKey, 0)

// 2. Perform ECDH to get shared secret
val sharedSecret = Curve.calculateAgreement(
    theirPublicKey, 
    identityKeyPair.privateKey
)

// 3. Derive AES key using HKDF
val hkdf = HKDF.createFor(3)  // Signal Protocol v3
val info = "WhisperProvisioning".toByteArray(Charsets.UTF_8)
val salt = ByteArray(32)  // Zero salt
val derivedSecrets = hkdf.deriveSecrets(sharedSecret, salt, info, 64)

// 4. Split into AES key and IV
val aesKey = ByteArray(32)
val iv = ByteArray(12)  // GCM IV
System.arraycopy(derivedSecrets, 0, aesKey, 0, 32)
System.arraycopy(derivedSecrets, 32, iv, 0, 12)
```

**HKDF Parameters:**
- **Version:** 3 (Signal Protocol v3)
- **Info:** "WhisperProvisioning"
- **Salt:** 32 bytes of zeros
- **Output:** 64 bytes (32 for key, 12 for IV, rest unused)

### 7. Decryption (AES-GCM)

```kotlin
val cipher = Cipher.getInstance("AES/GCM/NoPadding")
val keySpec = SecretKeySpec(aesKey, "AES")
val gcmSpec = GCMParameterSpec(128, iv)  // 128-bit auth tag

cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
val decryptedBody = cipher.doFinal(message.body)
```

**Decrypted Payload Contains:**
- Master Key (for account encryption)
- Profile Key (for profile encryption)
- Phone number
- Device ID
- Other account credentials

### 8. Provisioning Complete

Once decrypted successfully:

```kotlin
// Notify UI that linking is complete
onProvisioningComplete()

// In production, also:
// - Store master key securely
// - Send confirmation to server
// - Start contact sync
// - Register for push notifications
```

## Security Considerations

### Current Implementation ✅
- ✅ Proper ECDH key agreement (Curve25519)
- ✅ Correct HKDF derivation (v3)
- ✅ AES-GCM authenticated encryption
- ✅ Ephemeral keys for forward secrecy
- ✅ Network permissions properly configured

### Production Requirements ⚠️
- ⚠️ **Persistent Storage:** Keys currently in-memory only
- ⚠️ **Key Security:** Use Android Keystore for sensitive keys
- ⚠️ **Certificate Pinning:** Add for WebSocket connections
- ⚠️ **Session Management:** Implement proper lifecycle
- ⚠️ **Error Recovery:** Add retry logic and error handling
- ⚠️ **Master Key Storage:** Encrypt with hardware-backed key
- ⚠️ **Confirmation Message:** Send ACK back to phone
- ⚠️ **WebSocket Reconnection:** Handle network interruptions

## Protocol References

### Signal Protocol Documentation
- [Signal Protocol Specification](https://signal.org/docs/)
- [X3DH Key Agreement](https://signal.org/docs/specifications/x3dh/)
- [Double Ratchet](https://signal.org/docs/specifications/doubleratchet/)

### Cryptographic Primitives
- **Key Agreement:** Curve25519 ECDH
- **Key Derivation:** HKDF-SHA256
- **Encryption:** AES-256-GCM
- **Signatures:** Ed25519

## Testing

### Manual Testing
1. Build and install on Wear OS device
2. Open app to see QR code
3. Scan with Signal app on phone
4. Observe WebSocket connection in logs
5. Verify decryption success in logs

### Verification Points
```bash
# Watch logs for key generation
adb logcat -s SignalClient:D | grep "Identity"

# Watch logs for WebSocket connection
adb logcat -s SignalClient:D | grep "WebSocket"

# Watch logs for provisioning
adb logcat -s SignalClient:D | grep "Provisioning"

# Watch logs for decryption
adb logcat -s SignalClient:D | grep "Decryption"
```

### Expected Log Output
```
SignalClient: WebSocket Connected
SignalClient: Received binary message: <size> bytes
SignalClient: Parsed Provisioning Message: ...
SignalClient: Decryption Successful! Body size: <size>
```

## Future Enhancements

1. **Persistent Storage**
   - Implement Room database for keys
   - Use SQLCipher for encryption at rest
   - Migrate from in-memory to persistent store

2. **WebSocket Management**
   - Implement reconnection logic
   - Add heartbeat/keep-alive
   - Handle network changes gracefully

3. **Message Handling**
   - Parse decrypted provisioning payload
   - Extract and store master key
   - Send confirmation back to phone
   - Implement contact sync

4. **Security Hardening**
   - Android Keystore integration
   - Certificate pinning
   - Key rotation policies
   - Secure wipe on unlink

5. **User Experience**
   - Show connection status
   - Display linking progress
   - Handle errors gracefully
   - Add timeout handling

## Code References

- **SignalClient.kt**: Main handshake logic
- **QrCodeScreen.kt**: QR code generation and WebSocket initiation
- **ProvisioningMessage.kt**: Protobuf message parsing
- **SignalProtocolStoreImpl.kt**: Key storage
- **AndroidManifest.xml**: Network permissions

## Troubleshooting

### WebSocket Won't Connect
- Check `INTERNET` permission in manifest
- Verify network connectivity
- Check firewall/proxy settings
- Ensure UUID is correctly extracted

### Decryption Fails
- Verify HKDF implementation is correct
- Check that public key is properly decoded
- Ensure IV and key lengths are correct
- Validate GCM parameters (128-bit tag)

### No Provisioning Message Received
- Check WebSocket connection is open
- Verify UUID matches between QR and WebSocket
- Check phone can reach Signal servers
- Ensure QR code contains correct data

## Contact & Support

For questions about the handshake implementation:
1. Check this document
2. Review the source code
3. Check Signal Protocol documentation
4. Open an issue on GitHub
