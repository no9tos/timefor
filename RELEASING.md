# Releasing Timefor

Releases are built and signed by GitHub Actions (`.github/workflows/release.yml`) when a tag
like `v1.2.3` is pushed. The APK is attached to a GitHub release, and the newest one is always at
<https://github.com/no9tos/timefor/releases/latest/download/timefor.apk>.

## One-time setup: the signing key

Android only installs an update if it is signed with the same key as the installed app, so every
release must use one permanent key. **Back it up. If it is lost, users cannot update and must
reinstall.** Never commit it to the repository.

1. Create the key on your computer (`keytool` ships with Java and Android Studio). Choose your own
   passwords when asked:

   ```sh
   keytool -genkeypair -v -keystore timefor-release.jks -alias timefor \
     -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=Timefor"
   ```

2. Encode it for GitHub:

   ```sh
   # macOS / Linux
   base64 -i timefor-release.jks | tr -d '\n' > timefor-release.b64
   # Windows (PowerShell)
   [Convert]::ToBase64String([IO.File]::ReadAllBytes("timefor-release.jks")) > timefor-release.b64
   ```

3. In the repository, open **Settings → Secrets and variables → Actions → New repository secret**
   and add:

   | Secret | Value |
   | --- | --- |
   | `TIMEFOR_KEYSTORE_BASE64` | contents of `timefor-release.b64` |
   | `TIMEFOR_KEYSTORE_PASSWORD` | the keystore password |
   | `TIMEFOR_KEY_ALIAS` | `timefor` |
   | `TIMEFOR_KEY_PASSWORD` | the key password (the same as the keystore password if you only entered one) |

4. Delete `timefor-release.b64`. Keep `timefor-release.jks` and the passwords in a safe place,
   such as a password manager.

## Publishing a version

1. Add the changes to `CHANGELOG.md` and merge them into `main`.
2. Start the release in one of two ways:
   - **From the browser or phone:** **Actions → Release → Run workflow**, enter the version
     (for example `1.2.3`) and press **Run workflow**. The tag `v1.2.3` is created on `main`.
   - **From a terminal:**

     ```sh
     git tag v1.2.3
     git push origin v1.2.3
     ```

3. The workflow builds the APK and publishes the release in about 3 minutes.
   Version `1.2.3` gets the version code `10203`, so each release must have a higher number than
   the previous one.

Don't create the release by hand on the Releases page: the workflow creates it, and fails if a
release for that tag already exists.
