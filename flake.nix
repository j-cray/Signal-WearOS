{
  description = "Signal WearOS Dev Environment with Rust and Android SDK";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    android-nixpkgs.url = "github:tadfisher/android-nixpkgs";
    rust-overlay.url = "github:oxalica/rust-overlay";
  };

  outputs = { self, nixpkgs, android-nixpkgs, rust-overlay }:
    let
      system = "x86_64-linux";
      overlays = [ (import rust-overlay) ];
      pkgs = import nixpkgs {
        inherit system overlays;
        config.allowUnfree = true;
      };

      android-sdk = android-nixpkgs.sdk.${system} (sdkPkgs: with sdkPkgs; [
        cmdline-tools-latest
        build-tools-34-0-0
        platform-tools
        platforms-android-34
        ndk-26-1-10909125
        cmake-3-22-1
      ]);

      # Use Nightly Rust for -Z flags
      rust-toolchain = pkgs.rust-bin.nightly.latest.default.override {
        extensions = [ "rust-src" ];
        targets = [
          "armv7-linux-androideabi"
          "aarch64-linux-android"
          "i686-linux-android"
          "x86_64-linux-android"
        ];
      };

    in
    {
      devShells.${system}.default = pkgs.mkShell {
        buildInputs = with pkgs; [
          # Java
          jdk17

          # Rust (with targets)
          rust-toolchain

          # Scripting
          python3

          # Build Tools
          cmake
          gradle
          protobuf

          # Android
          android-sdk
        ];

        # Environment variables
        # We set ANDROID_SDK_ROOT to the Nix store path for reference
        # But we will override ANDROID_HOME in the shellHook
        ANDROID_NDK_ROOT = "${android-sdk}/share/android-sdk/ndk/26.1.10909125";
        JAVA_HOME = "${pkgs.jdk17}";

        shellHook = ''
          # Define a local writable SDK directory
          export LOCAL_SDK_DIR="$PWD/.android-sdk"
          export ANDROID_HOME="$LOCAL_SDK_DIR"

          # Unset the read-only one to avoid Gradle confusion
          unset ANDROID_SDK_ROOT

          echo "Setting up writable Android SDK in $LOCAL_SDK_DIR..."

          # Create the directory structure
          mkdir -p "$LOCAL_SDK_DIR/ndk"

          # Symlink the NDK to the version Gradle expects (28.0.13004108)
          # We link it from the Nix store NDK (26.1.10909125)
          if [ ! -d "$LOCAL_SDK_DIR/ndk/28.0.13004108" ]; then
             ln -sfn "$ANDROID_NDK_ROOT" "$LOCAL_SDK_DIR/ndk/28.0.13004108"
             echo "Symlinked NDK 26 as 28.0.13004108"
          fi

          # Create local.properties for libsignal
          # We assume the user might run this from root or libsignal/java
          echo "sdk.dir=$ANDROID_HOME" > local.properties
          echo "ndk.dir=$ANDROID_HOME/ndk/28.0.13004108" >> local.properties

          # Also create it in libsignal/java if it exists
          if [ -d "libsignal/java" ]; then
             echo "sdk.dir=$ANDROID_HOME" > libsignal/java/local.properties
             echo "ndk.dir=$ANDROID_HOME/ndk/28.0.13004108" >> libsignal/java/local.properties
          fi

          echo "Signal WearOS Dev Environment Ready!"
          echo "Android SDK: $ANDROID_HOME"
          echo "Rust Version: $(rustc --version)"
          echo "CMake Version: $(cmake --version)"
        '';
      };
    };
}
