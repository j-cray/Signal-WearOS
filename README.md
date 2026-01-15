# Signal WearOS

A Signal Protocol implementation for Wear OS, enabling secure messaging from your smartwatch.

## Features

- 🔐 **End-to-end Encryption**: Uses the Signal Protocol for secure messaging
- ⌚ **Wear OS Native**: Built specifically for Wear OS watches
- 🔗 **Device Linking**: QR code-based linking with your phone
- 💬 **Message Viewing**: Read messages directly on your watch
- 🎨 **Material Design**: Modern Wear OS Compose UI

## Handshake Implementation

The handshake between phone and watch follows the Signal Protocol device linking flow:

### How It Works

1. **Key Generation**: The watch generates cryptographic keys on first launch
   - Identity key pair (for long-term identity)
   - Registration ID
   - Pre-keys (100 one-time keys)
   - Signed pre-key

2. **QR Code Display**: The watch generates a link URI containing:
   - UUID for the WebSocket connection
   - Base64-encoded public key
   - Format: `tsdevice:/?uuid=<UUID>&pub_key=<BASE64_KEY>`

3. **WebSocket Connection**: The watch connects to the provisioning WebSocket
   - Endpoint: `wss://chat.signal.org/v1/websocket/provisioning/<UUID>`
   - Listens for provisioning messages from the phone

4. **Provisioning Message**: When the phone scans the QR code:
   - Phone sends encrypted provisioning data
   - Contains master key, profile key, and account information
   - Encrypted with ECDH shared secret + HKDF key derivation

5. **Decryption & Setup**: The watch decrypts the provisioning message
   - Performs ECDH with the phone's ephemeral public key
   - Derives AES key using HKDF (proper implementation)
   - Decrypts with AES-GCM
   - Stores account credentials securely

### Technical Details

#### Permissions Required
- `INTERNET` - For WebSocket communication
- `ACCESS_NETWORK_STATE` - To check network availability
- `POST_NOTIFICATIONS` - For message notifications
- `WAKE_LOCK` - To keep the watch awake during sync

#### Key Components

**SignalClient.kt**
- Manages WebSocket connections
- Handles key generation and storage
- Implements ECDH and HKDF for provisioning
- Encrypts/decrypts messages using Signal Protocol

**QrCodeScreen.kt**
- Generates QR code from link URI
- Initiates WebSocket connection
- Handles linking completion callback

**SignalProtocolStoreImpl.kt**
- Stores identity keys, pre-keys, and sessions
- Currently in-memory (consider persistent storage for production)

## Build & Run

### Prerequisites
- Android Studio (latest stable version)
- JDK 17
- Wear OS device or emulator (API 30+)

### Building

```bash
# Build debug APK
./gradlew assembleDebug

# Run lint
./gradlew lint

# Run tests
./gradlew test
```

**Note:** If you encounter build issues (especially with plugin resolution), see [BUILD_TROUBLESHOOTING.md](BUILD_TROUBLESHOOTING.md) for solutions.

### Installation

```bash
# Install on connected watch
adb install app/build/outputs/apk/debug/app-debug.apk
```

## CI/CD

The project includes GitHub Actions workflows for:
- ✅ Automated builds on push/PR
- ✅ Android Lint analysis
- ✅ Unit test execution
- ✅ PR validation
- ✅ Release builds
- ✅ Code quality checks

See [.github/workflows/README.md](.github/workflows/README.md) for details.

## Development Status

### Completed ✅
- Basic project structure
- Signal Protocol key generation
- QR code generation and display
- WebSocket connection setup
- HKDF key derivation (proper implementation)
- Network permissions configuration
- CI/CD workflows

### In Progress 🚧
- WebSocket handshake testing
- Message encryption/decryption
- Contact synchronization
- Persistent storage

### Planned 📋
- Message sending from watch
- Voice reply integration
- Notification improvements
- Multi-device sync
- Group messaging support

## Security Considerations

⚠️ **Important**: This is a prototype implementation. For production use:

1. **Persistent Storage**: Implement encrypted database storage (SQLCipher + Room)
2. **Key Security**: Use Android Keystore for sensitive key material
3. **Certificate Pinning**: Add certificate pinning for WebSocket connections
4. **Session Management**: Implement proper session lifecycle and cleanup
5. **Error Handling**: Add comprehensive error handling and recovery
6. **Testing**: Add integration tests for cryptographic operations

## Architecture

```
app/
├── src/main/java/com/example/signalwearos/
│   ├── data/
│   │   ├── signal/              # Signal Protocol implementation
│   │   │   ├── SignalClient.kt
│   │   │   ├── proto/           # Protocol buffers
│   │   │   └── store/           # Key storage
│   │   └── UserPreferencesRepository.kt
│   ├── presentation/
│   │   ├── ui/                  # Compose UI screens
│   │   ├── model/               # Data models
│   │   └── theme/               # Material theme
│   ├── complication/            # Watch face complications
│   ├── tile/                    # Wear OS tiles
│   └── notification/            # Notification handling
```

## Dependencies

- **Signal Protocol**: `org.signal:libsignal-client:0.22.0`
- **OkHttp**: WebSocket support
- **ZXing**: QR code generation
- **Jetpack Compose**: UI framework
- **Wear Compose**: Wear OS components
- **DataStore**: Preferences storage

## Contributing

Contributions are welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## License

[Specify your license here]

## Acknowledgments

- Signal Foundation for the Signal Protocol
- Android Open Source Project for Wear OS samples
- ZXing project for QR code generation

## Disclaimer

This is an unofficial implementation and is not affiliated with Signal Messenger or the Signal Foundation. Use at your own risk.
