# Contributing to SmartFoo

## One-time publishing setup

These steps are required once per machine (or CI environment) before you can publish releases to Maven Central.

### 1. Sonatype Central Portal account

Register at [central.sonatype.com](https://central.sonatype.com) and verify the `com.smartfoo` namespace.

### 2. Central Portal token

Go to Account → Generate User Token. Keep the username and password it gives you — you will need them in the steps below.

### 3. GPG key

The signing key fingerprint is `68A9F48759586BD553B0A2FA95B723E3101ED163` (`publish@smartfoo.com`). The public key is published to `keys.openpgp.org`. Export the armored private key with:

```bash
gpg --armor --export-secret-keys publish@smartfoo.com
```

### 4. Local credentials

Add the following to `~/.gradle/gradle.properties` (**never commit this file**):

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
