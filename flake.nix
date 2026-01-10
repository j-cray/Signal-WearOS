{
  description = "Signal WearOS FHS Dev Environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    android-nixpkgs.url = "github:tadfisher/android-nixpkgs";
  };

  outputs = { self, nixpkgs, android-nixpkgs }:
    let
      system = "x86_64-linux";
      pkgs = import nixpkgs { inherit system; config.allowUnfree = true; };

      # Get Android SDK from Nix
      android-sdk = android-nixpkgs.sdk.${system} (sdkPkgs: with sdkPkgs; [
        cmdline-tools-latest
        build-tools-34-0-0
        platform-tools
        platforms-android-34
        ndk-26-1-10909125
        cmake-3-22-1
      ]);

      # Create an FHS environment (simulates standard Linux paths)
      fhs = pkgs.buildFHSUserEnv {
        name = "signal-build-env";
        targetPkgs = pkgs: with pkgs; [
          # Build Essentials
          git
          curl
          wget
          gnumake
          gcc
          clang
          llvmPackages.libclang
          cmake
          pkg-config

          # Libraries often needed by bindgen/cargo
          zlib
          ncurses
          openssl
          glibc.dev

          # Java/Android
          jdk17
          gradle
          protobuf
          python3

          # Rust Manager
          rustup
        ];

        # Expose the Android SDK from Nix store
        profile = ''
          export JAVA_HOME=${pkgs.jdk17}

          # Reference the read-only SDK
          export NIX_ANDROID_SDK_ROOT=${android-sdk}/share/android-sdk

          # Setup local writable SDK for Gradle
          export LOCAL_SDK_DIR="$PWD/.android-sdk"
          export ANDROID_HOME="$LOCAL_SDK_DIR"

          # Unset conflicting var
          unset ANDROID_SDK_ROOT

          echo "Initializing Signal Build Environment..."

          # 1. Setup Writable SDK Structure
          if [ ! -d "$LOCAL_SDK_DIR/ndk/28.0.13004108" ]; then
            echo "-> Setting up NDK symlinks..."
            mkdir -p "$LOCAL_SDK_DIR/ndk"
            mkdir -p "$LOCAL_SDK_DIR/licenses"

            # Symlink NDK 26 as 28 (Gradle deception)
            ln -sfn "$NIX_ANDROID_SDK_ROOT/ndk/26.1.10909125" "$LOCAL_SDK_DIR/ndk/28.0.13004108"

            # Accept Licenses
            echo "8933bad161af4178b1185d1a37fbf41ea5269c55" > "$LOCAL_SDK_DIR/licenses/android-sdk-license"
            echo "d56f5187479451eabf01fb78af6dfcb131a6481e" >> "$LOCAL_SDK_DIR/licenses/android-sdk-license"
            echo "24333f8a63b6825ea9c5514f83c2829b004d1fee" >> "$LOCAL_SDK_DIR/licenses/android-sdk-license"
            echo "84831b9409646a918e30573bab4c9c91346d8abd" > "$LOCAL_SDK_DIR/licenses/android-sdk-preview-license"
          fi

          # 2. Setup Rust via Rustup (Local installation)
          export RUSTUP_HOME="$PWD/.rustup"
          export CARGO_HOME="$PWD/.cargo"
          export PATH="$CARGO_HOME/bin:$PATH"

          if ! command -v rustc &> /dev/null; then
             echo "-> Installing Rust Nightly (required for libsignal)..."
             rustup install nightly
             rustup default nightly

             echo "-> Adding Android Targets..."
             rustup target add armv7-linux-androideabi aarch64-linux-android i686-linux-android x86_64-linux-android
          fi

          # 3. Setup Local Properties
          echo "sdk.dir=$ANDROID_HOME" > local.properties
          echo "ndk.dir=$ANDROID_HOME/ndk/28.0.13004108" >> local.properties
          if [ -d "libsignal/java" ]; then
             echo "sdk.dir=$ANDROID_HOME" > libsignal/java/local.properties
             echo "ndk.dir=$ANDROID_HOME/ndk/28.0.13004108" >> libsignal/java/local.properties
          fi

          echo "=================================================="
          echo "Environment Ready!"
          echo "To build libsignal:"
          echo "  cd libsignal/java"
          echo "  ./gradlew bundleReleaseAar"
          echo "=================================================="
        '';
      };
    in
    {
      devShells.${system}.default = fhs.env;
    };
}
