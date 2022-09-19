echo node-gyp cache
dir C:\Users\ContainerAdministrator\AppData\Local\node-gyp
rmdir /s /q C:\Users\ContainerAdministrator\AppData\Local\node-gyp\Cache
dir C:\Users\ContainerAdministrator\AppData\Local\node-gyp
echo node-gyp cache deleted
@echo off

if exist build rmdir /s /q build 
pushd ..\..\src\gwt
ant clean 
popd
