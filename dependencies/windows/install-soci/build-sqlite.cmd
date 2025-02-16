echo "Configuring Visual C++ for 64-bit..."
call vcvarsall.bat amd64

echo "building 64-bit debug..."
cd sqlite
cd sqlite-*
cl /c /EHsc /MDd sqlite3.c
lib /OUT:sqlite3-debug-x64.lib sqlite3.obj
move sqlite3-debug-x64.lib ..

echo "building 64-bit release..."
cl /c /EHsc /MD sqlite3.c
lib /OUT:sqlite3-release-x64.lib sqlite3.obj
move sqlite3-release-x64.lib ..

echo "Configuring Visual C++ for 32-bit..."
call vcvarsall.bat x86

echo "building 32-bit debug..."
cl /c /EHsc /MDd sqlite3.c
lib /OUT:sqlite3-debug-x86.lib sqlite3.obj
move sqlite3-debug-x86.lib ..

echo "building 32-bit release..."
cl /c /EHsc /MD sqlite3.c
lib /OUT:sqlite3-release-x86.lib sqlite3.obj
move sqlite3-release-x86.lib ..

echo "Done building."