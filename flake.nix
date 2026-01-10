{
  description = "Signal WearOS Dev Environment with Rust and Android SDK";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    android-nixpkgs.url = "github:tadfisher/android-nixpkgs";
  };

  outputs = { self, nixpkgs, android-nixpkgs }:
    let
      system = "x86_64-linux"; # Adjust if you are on aarch64-linux or darwin
      pkgs = import nixpkgs {
        inherit system;
        config.allowUnfree = true;
      };

      android-sdk = android-nixpkgs.sdk.${system} (sdkPkgs: with sdkPkgs; [
        cmdline-tools-latest
        build-tools-34-0-0
        platform-tools
        platforms-android-34
        ndk-26-1-10909125 # NDK version required by libsignal
        cmake-3-22-1
      ]);

    in
    {
      devShells.${system}.default = pkgs.mkShell {
        buildInputs = with pkgs; [
          # Java
          jdk17

          # Rust
          rustc
          cargo
          rustfmt
          clippy

          # Android
          android-sdk

          # Build Tools
          gradle
          protobuf # For compiling signal protos
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

          # Add Android targets for Rust
          rustup target add armv7-linux-androideabi || echo "Rustup not found, assuming nix managed rust"
          rustup target add aarch64-linux-android || echo "Rustup not found, assuming nix managed rust"
        '';
      };
    };
}
