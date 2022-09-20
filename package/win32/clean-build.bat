echo npm set msvs_version 2019
npm set msvs_version 2019
dir C:\Users\ContainerAdministrator
type C:\Users\ContainerAdministrator\.npmrc
npx node-gyp install 16.14.0
dir C:\Users\ContainerAdministrator\AppData\Local\node-gyp\Cache\16.14.0\include\node
@echo off

if exist build rmdir /s /q build 
pushd ..\..\src\gwt
ant clean 
popd
