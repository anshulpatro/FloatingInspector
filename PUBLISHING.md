# Publishing FloatingInspector to Maven Central

Publishing uses the [vanniktech maven-publish plugin](https://github.com/vanniktech/gradle-maven-publish-plugin)
(configured in `build.gradle.kts`) and Sonatype's **Central Portal**. Coordinate comes from
`gradle.properties`: `GROUP` / `ARTIFACT_ID` / `VERSION_NAME` → **`com.anshulpatro:floatinginspector:0.1.0`**.

> Build with JDK 17 or 21: `export JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home"`

---

## One-time setup

### 1. Claim the namespace

Sign in at <https://central.sonatype.com> → **Add Namespace**.

Add the namespace **`com.anshulpatro`**. Central gives you a DNS **TXT** record — add it to the **`anshulpatro.com`** domain you own, then click **Verify**. (Reverse-domain of `anshulpatro.com` = `com.anshulpatro`.)

### 2. Generate a Central Portal user token

Central Portal → **Account → Generate User Token**. You'll get a username/password pair.

### 3. Create a GPG signing key (Central requires signed artifacts)

```bash
gpg --quick-generate-key "Anshul Patro <anshulpatro@gmail.com>"
gpg --list-secret-keys --keyid-format=long          # note the long key id
gpg --keyserver keyserver.ubuntu.com --send-keys <LONG_KEY_ID>   # publish public key
# export the secret key as a single-line value for in-memory signing:
gpg --armor --export-secret-keys <LONG_KEY_ID> | grep -v '^---' | tr -d '\n'
```

### 4. Put credentials in `~/.gradle/gradle.properties` (NOT in this repo)

```properties
mavenCentralUsername=<central portal token username>
mavenCentralPassword=<central portal token password>

signingInMemoryKey=<output of the gpg --armor export command above>
signingInMemoryKeyPassword=<your gpg key passphrase>
```

---

## Cut a release

1. Bump `VERSION_NAME` in `gradle.properties` (e.g. `0.1.0` → `0.1.1`).
2. Publish:
   ```bash
   ./gradlew publishAndReleaseToMavenCentral --no-configuration-cache
   ```
   This builds the AAR + sources + javadoc jars, signs them, uploads to the Central Portal, and
   (because `automaticRelease = true`) releases automatically once validation passes. It appears on
   Maven Central a few minutes later.
3. Tag the release in git:
   ```bash
   git tag 0.1.0 && git push origin 0.1.0
   ```

To stage without auto-releasing (inspect on the Portal first), run `publishToMavenCentral` instead and
click **Publish** in the Central Portal UI.

---

## Consumers then use

```groovy
// mavenCentral() is already in every Android project — no extra repository needed
implementation 'com.anshulpatro:floatinginspector:0.1.0'
```
