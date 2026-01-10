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

      rust-toolchain = pkgs.rust-bin.stable.latest.default.override {
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

          # Android
          android-sdk

          # Build Tools
          gradle
          protobuf
        ];

        # Environment variables
        ANDROID_HOME = "${android-sdk}/share/android-sdk";
        ANDROID_NDK_ROOT = "${android-sdk}/share/android-sdk/ndk/26.1.10909125";
        JAVA_HOME = "${pkgs.jdk17}";

        shellHook = ''
          echo "Signal WearOS Dev Environment Loaded"
          echo "Android SDK: $ANDROID_HOME"
          echo "Android NDK: $ANDROID_NDK_ROOT"
          echo "Rust Version: $(rustc --version)"
          echo "Targets installed:"
          rustc --print target-list | grep android
        '';
      };
    };
}
