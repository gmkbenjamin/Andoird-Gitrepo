Andoird-Gitrepo
======

Git server on Android based on Gidder 
https://github.com/antoniy/Gidder

Features: 
Support Ethernet and Hotspot mode (experimental)
Support backup and restore of configurations using Android backup service (5MB limit) or Google Drive (26MB limit) (experimental)
Basic SSH server (experimental)
SCP server (experimental)
Public key authentication (experimental)
Backup of repositories with encryption

If the hostspot mode doesn't work for you please go to settings and send a log and specify that hotspot mode doesn't work.

git clone won't work before the initial commit
Initial commit:
git init
git add .
git commit -m "initial commit"
git remote add origin ssh://[username]@[git server hostname]:[port number]/[repo name].git
git push origin master