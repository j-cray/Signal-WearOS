# Build Troubleshooting Guide

## Known Issues

### 1. Android Gradle Plugin Resolution Failure

**Error:**
```
Plugin [id: 'com.android.application', version: '8.3.2', apply: false] was not found
```

**Cause:** 
The Google Maven repository may be blocked or inaccessible from your network environment.

**Solutions:**

#### Option A: Use a Different Network
Try building from a different network that has access to Google's Maven repository.

#### Option B: Use a Mirror Repository
Add a mirror repository in `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }  // Alibaba mirror
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        google()
        mavenCentral()
    }
}
```

#### Option C: Use a VPN/Proxy
Configure Gradle to use a proxy:

Create or edit `gradle.properties`:
```properties
systemProp.http.proxyHost=your.proxy.host
systemProp.http.proxyPort=8080
systemProp.https.proxyHost=your.proxy.host
systemProp.https.proxyPort=8080
```

#### Option D: Update to Latest Stable Versions
Try using the latest stable versions that are available in your environment:

In `gradle/libs.versions.toml`:
```toml
[versions]
agp = "8.1.0"  # or latest available
kotlin = "1.9.0"  # or latest available
```

### 2. Gradle Version Compatibility

**Issue:** Gradle 9.x may have compatibility issues with some plugins.

**Solution:** Use Gradle 8.9 (already configured):
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.9-bin.zip
```

### 3. JDK Version Issues

**Required:** JDK 17
**Check your version:**
```bash
java -version
```

**Set JAVA_HOME if needed:**
```bash
export JAVA_HOME=/path/to/jdk-17
```

### 4. Build Cache Issues

**Solution:** Clear Gradle cache:
```bash
rm -rf ~/.gradle/caches/
./gradlew clean --no-daemon
./gradlew build --no-daemon
```

## Verification Steps

After resolving build issues, verify the setup:

1. **Check Gradle:**
   ```bash
   ./gradlew --version
   ```

2. **List Tasks:**
   ```bash
   ./gradlew tasks
   ```

3. **Build Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Run Lint:**
   ```bash
   ./gradlew lint
   ```

5. **Run Tests:**
   ```bash
   ./gradlew test
   ```

## Alternative: Build with Android Studio

If command-line builds fail:

1. Open the project in Android Studio
2. Wait for Gradle sync to complete
3. Use Build menu → Build Bundle(s) / APK(s) → Build APK(s)
4. Android Studio may handle repository issues better with its built-in resolver

## CI/CD Considerations

The GitHub Actions workflows are configured to work in standard CI environments where Google Maven is accessible. If you're running builds in a restricted environment:

- Consider using a self-hosted runner with appropriate network access
- Configure repository mirrors in the workflow files
- Use dependency caching to minimize repository access

## Getting Help

If you continue to experience build issues:

1. Check the [Android Developers documentation](https://developer.android.com/studio/build)
2. Review [Gradle documentation](https://docs.gradle.org/)
3. Open an issue in this repository with:
   - Full error message
   - Gradle version (`./gradlew --version`)
   - Java version (`java -version`)
   - OS and network environment details
