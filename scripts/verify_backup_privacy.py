#!/usr/bin/env python3
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
android = '{http://schemas.android.com/apk/res/android}'
application = ET.parse(root/'app/src/main/AndroidManifest.xml').getroot().find('application')
assert application.get(android+'allowBackup') == 'false'
assert application.get(android+'backupAgent') is None
assert application.get(android+'fullBackupContent') == '@xml/backup_rules'
assert application.get(android+'dataExtractionRules') == '@xml/data_extraction_rules'
expected = {'root','file','database','sharedpref','external','device_root','device_file','device_database','device_sharedpref'}
old = ET.parse(root/'app/src/main/res/xml/backup_rules.xml').getroot()
new = ET.parse(root/'app/src/main/res/xml/data_extraction_rules.xml').getroot()
for policy in [old, new.find('cloud-backup'), new.find('device-transfer')]:
    assert {entry.get('domain') for entry in policy.findall('exclude') if entry.get('path') == '.'} == expected
    assert not policy.findall('include')
java = root/'app/src/main/java/io/github/gmkbenjamin/gitrepo/beta'
all_code = '\n'.join(path.read_text() for path in java.rglob('*.java'))
assert 'putString("password"' not in all_code, 'A plaintext password write remains'
assert 'Password SHA256:' not in all_code
assert not (java/'ui/util/GitrepoAutoBackup.java').exists()
print('Verified automatic/cloud/device-transfer backup exclusions and no plaintext password persistence')
