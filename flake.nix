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
          llvmPackages.libclang

          # C Headers
          glibc.dev

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

          # Set LIBCLANG_PATH for bindgen
          export LIBCLANG_PATH="${pkgs.llvmPackages.libclang.lib}/lib"

          # Set BINDGEN_EXTRA_CLANG_ARGS to find C headers
          export BINDGEN_EXTRA_CLANG_ARGS="-I${pkgs.glibc.dev}/include -I${pkgs.llvmPackages.libclang.lib}/lib/clang/${pkgs.llvmPackages.libclang.version}/include"

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

          # Create licenses if missing
          mkdir -p "$LOCAL_SDK_DIR/licenses"
          echo "8933bad161af4178b1185d1a37fbf41ea5269c55" > "$LOCAL_SDK_DIR/licenses/android-sdk-license"
          echo "d56f5187479451eabf01fb78af6dfcb131a6481e" >> "$LOCAL_SDK_DIR/licenses/android-sdk-license"
          echo "24333f8a63b6825ea9c5514f83c2829b004d1fee" >> "$LOCAL_SDK_DIR/licenses/android-sdk-license"
          echo "84831b9409646a918e30573bab4c9c91346d8abd" > "$LOCAL_SDK_DIR/licenses/android-sdk-preview-license"

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
          echo "CMake Version: $(cmake --version)"
          echo "LibClang Path: $LIBCLANG_PATH"
        '';
      };
    };
}
