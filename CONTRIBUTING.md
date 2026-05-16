# Contributing to SmartFoo

## One-time publishing setup

These steps are required once per machine (or CI environment) before you can publish releases to Maven Central.
Local credentials are only needed if you intentionally publish from a developer
machine; the normal release path uses GitHub Actions secrets.

### 1. Sonatype Central Portal account

Register at [central.sonatype.com](https://central.sonatype.com) and verify the `com.smartfoo` namespace.

### 2. Central Portal token

Go to Account → Generate User Token. Keep the username and password it gives you — you will need them in the steps below.

### 3. GPG key

The signing key fingerprint is `68A9F48759586BD553B0A2FA95B723E3101ED163` (`publish@smartfoo.com`). The public key is published to `keys.openpgp.org`. Export the armored private key with:

```bash
gpg --armor --export-secret-keys publish@smartfoo.com
```

### 4. Optional local credentials

Only maintainers who need to publish from a developer machine should add the
following to `~/.gradle/gradle.properties` (**never commit this file**):

```properties
mavenCentralUsername=<Central Portal token username>
mavenCentralPassword=<Central Portal token password>
signingKey=<armored GPG private key>
signingPassword=<GPG passphrase>
```

### 5. GitHub Actions secrets

Go to the repository → Settings → Secrets and variables → Actions, and add the following repository secrets:

| Secret | Value |
|---|---|
| `MAVEN_CENTRAL_USERNAME` | Central Portal token username |
| `MAVEN_CENTRAL_PASSWORD` | Central Portal token password |
| `GPG_SIGNING_KEY` | Armored GPG private key (same value as `signingKey` above) |
| `GPG_SIGNING_PASSWORD` | GPG passphrase (same value as `signingPassword` above) |

## Publishing a New Release

This section is for maintainers only. The normal release path is tag-driven:
push a `v*` tag and let GitHub Actions publish the artifacts with repository
secrets.

1. Choose the new version number. The version is derived automatically from the Git tag during CI publishing, so no file edits are required for a tag-triggered release. The fallback version in `android/smartfoo-android-lib-core/build.gradle.kts` can be kept in sync for reference.

2. Tag the release:
   ```bash
   git tag v<version> && git push origin v<version>
   ```
   Pushing a `v*` tag triggers the [publish GitHub Actions workflow](.github/workflows/publish.yml), which runs:
   ```bash
   ./gradlew publishAggregationToCentralPortal -PreleaseVersion=<version>
   ```
   The aggregation task collects all library publications and uploads them as a single bundle to the Central Portal.

3. Because `publishingType` is set to `USER_MANAGED`, log in to [central.sonatype.com](https://central.sonatype.com) → Deployments, verify the artifacts, then click **Publish**. The release appears on Maven Central within ~15 minutes. Switch `publishingType` to `AUTOMATIC` in `android/settings.gradle.kts` to skip this manual step once you're confident in the setup.

## Optional Publishing Smoke Test

Use this only when changing publishing configuration or diagnosing a release
problem. It builds the release publication and installs it into the local Maven
repository without uploading anything:

```bash
cd android
./gradlew :smartfoo-android-lib-core:publishToMavenLocal \
  -PsigningKey=SKIP \
  -PsigningPassword=SKIP
```

The `SKIP` values are only for local smoke tests. Use real Central Portal and
GPG credentials for any actual publish.
