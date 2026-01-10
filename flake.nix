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
        ANDROID_NDK_ROOT = "${android-sdk}/share/android-sdk/ndk/26.1.10909125";
        JAVA_HOME = "${pkgs.jdk17}";

        shellHook = ''
          # Define a local writable SDK directory
          export LOCAL_SDK_DIR="$PWD/.android-sdk"
          export ANDROID_HOME="$LOCAL_SDK_DIR"

          unset ANDROID_SDK_ROOT

          echo "Setting up writable Android SDK in $LOCAL_SDK_DIR..."

          # Target NDK version Gradle wants
          TARGET_NDK_VER="28.0.13004108"
          FAKE_NDK_DIR="$LOCAL_SDK_DIR/ndk/$TARGET_NDK_VER"

          mkdir -p "$FAKE_NDK_DIR"

          # Symlink everything from the real NDK except source.properties
          if [ -z "$(ls -A $FAKE_NDK_DIR)" ]; then
             echo "Creating fake NDK structure..."
             for file in "$ANDROID_NDK_ROOT"/*; do
               name=$(basename "$file")
               if [ "$name" != "source.properties" ]; then
                 ln -sfn "$file" "$FAKE_NDK_DIR/$name"
               fi
             done

             # Create fake source.properties
             echo "Pkg.Desc = Android NDK" > "$FAKE_NDK_DIR/source.properties"
             echo "Pkg.Revision = $TARGET_NDK_VER" >> "$FAKE_NDK_DIR/source.properties"
             echo "Fake NDK created."
          fi

          # Create local.properties for libsignal
          echo "sdk.dir=$ANDROID_HOME" > local.properties
          echo "ndk.dir=$FAKE_NDK_DIR" >> local.properties

          if [ -d "libsignal/java" ]; then
             echo "sdk.dir=$ANDROID_HOME" > libsignal/java/local.properties
             echo "ndk.dir=$FAKE_NDK_DIR" >> libsignal/java/local.properties
          fi

          echo "Signal WearOS Dev Environment Ready!"
          echo "Android SDK: $ANDROID_HOME"
          echo "Rust Version: $(rustc --version)"
        '';
      };
    };
}
