# Credential protection

New and changed SSH user passwords use versioned PBKDF2-HMAC-SHA256, 600,000
iterations, a fresh 128-bit salt, and a 256-bit derived key. The implementation
uses the existing Bouncy Castle dependency so Android API 21 remains supported.
Legacy SHA-256 records are verified in constant time and upgraded after a
successful login using a compare-and-set database write. A concurrent reset or
account disable is not overwritten. No password hash is printed in logs or UI.

The unsafe legacy Android automatic backup feature is now disabled. Both cloud
backup and device transfer explicitly exclude app data; the old backup agents
are no longer registered. Old plaintext backup password preferences and leftover
plaintext archives are cleared on startup. Already uploaded Android backups must
be removed separately through the account/device's backup controls.

Existing encrypted Gitrepo archives can still be restored by entering their
password for that operation. It is never retained in preferences. Temporary
plaintext lives in the private no-backup directory and is deleted on success or
failure. Cancellation retains the encrypted archive. Restore paths are confined
to the gitrepo directory and decompression is limited to 100,000 entries / 4 GiB.
Keep independent repository copies with normal Git transfers. Automatic encrypted
cloud backup can be reintroduced only with a separate, tested credential-safe design.

Tests: bash gradlew :app:testDebugUnitTest :app:assembleDebug
Backup policy regression: python3 scripts/verify_backup_privacy.py
