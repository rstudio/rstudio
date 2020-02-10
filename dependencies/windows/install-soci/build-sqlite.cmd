call "C:\Program Files (x86)\Microsoft Visual Studio 14.0\VC\vcvarsall.bat" amd64
cd sqlite
cd sqlite-*
cl /c /EHsc /MDd sqlite3.c
lib /OUT:sqlite3-debug.lib sqlite3.obj
move sqlite3-debug.lib ..
cl /c /EHsc /MD sqlite3.c
lib /OUT:sqlite3-release.lib sqlite3.obj
move sqlite3-release.lib ..