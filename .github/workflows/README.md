# GitHub Actions Workflows

This directory contains CI/CD workflows for the Signal WearOS application.

## Available Workflows

### 1. CI (`ci.yml`)
**Trigger:** Push to main/develop/copilot branches, Pull Requests
**Purpose:** Main continuous integration workflow
- Builds debug APK
- Runs lint checks
- Runs unit tests
- Uploads artifacts

### 2. Android Build (`android-build.yml`)
**Trigger:** Push to main/develop/copilot branches, Pull Requests, Manual
**Purpose:** Build debug APK
- Compiles the application
- Uploads debug APK as artifact

### 3. Android Lint (`android-lint.yml`)
**Trigger:** Push to main/develop/copilot branches, Pull Requests, Manual
**Purpose:** Static code analysis
- Runs Android Lint
- Uploads lint reports

### 4. Android Tests (`android-test.yml`)
**Trigger:** Push to main/develop/copilot branches, Pull Requests, Manual
**Purpose:** Run unit tests
- Executes unit tests
- Uploads test reports

### 5. PR Validation (`pr-validation.yml`)
**Trigger:** Pull Request events
**Purpose:** Validate pull requests
- Code formatting checks
- Static analysis
- Build and test validation

### 6. Release Build (`release.yml`)
**Trigger:** Version tags (v*), Manual
**Purpose:** Build release APKs
- Builds release APK
- Uploads release artifacts

### 7. Code Quality (`code-quality.yml`)
**Trigger:** Push to main/develop, Pull Requests, Weekly schedule, Manual
**Purpose:** Comprehensive code quality analysis
- Android Lint
- Detekt (if configured)
- Dependency vulnerability checks

## Artifacts

All workflows upload artifacts that can be downloaded from the Actions tab:
- **APK files**: Debug and release builds
- **Lint reports**: HTML reports of code quality issues
- **Test reports**: Unit test results

## Caching

Workflows use Gradle caching to speed up builds:
- Gradle wrapper cache
- Gradle dependencies cache
- Java setup with cache

## Manual Triggering

Most workflows support manual triggering via `workflow_dispatch`. You can trigger them from the Actions tab in GitHub.

## Branch Protection

Consider setting up branch protection rules for `main` and `develop` branches to require:
- CI workflow to pass
- PR validation to pass
- Code review approval

## Future Enhancements

Potential improvements:
- Add code signing for release builds
- Integrate with Google Play for automated deployment
- Add dependency update automation (Dependabot/Renovate)
- Add security scanning (CodeQL, Snyk)
- Add instrumented tests with Android emulator
- Add code coverage reporting (JaCoCo)
