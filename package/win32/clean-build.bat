dir C:\Users\ContainerAdministrator\AppData\Local\node-gyp
rmdir /s /q C:\Users\ContainerAdministrator\AppData\Local\node-gyp\Cache
@echo off

if exist build rmdir /s /q build 
pushd ..\..\src\gwt
ant clean 
popd
