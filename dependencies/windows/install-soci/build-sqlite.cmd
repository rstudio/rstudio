call "C:\Program Files (x86)\Microsoft Visual Studio 14.0\VC\vcvarsall.bat" amd64
cd sqlite
cd sqlite-*
cl /c /EHsc sqlite3.c
lib sqlite3.obj
move sqlite3.lib ..