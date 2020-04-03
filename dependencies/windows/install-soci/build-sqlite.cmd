call vcvarsall.bat amd64
cd sqlite
cd sqlite-*
cl /c /EHsc /MDd sqlite3.c
lib /OUT:sqlite3-debug-x64.lib sqlite3.obj
move sqlite3-debug-x64.lib ..
cl /c /EHsc /MD sqlite3.c
lib /OUT:sqlite3-release-x64.lib sqlite3.obj
move sqlite3-release-x64.lib ..
call vcvarsall.bat x86
cl /c /EHsc /MDd sqlite3.c
lib /OUT:sqlite3-debug-x86.lib sqlite3.obj
move sqlite3-debug-x86.lib ..
cl /c /EHsc /MD sqlite3.c
lib /OUT:sqlite3-release-x86.lib sqlite3.obj
move sqlite3-release-x86.lib ..