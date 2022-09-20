@echo off

if exist build rmdir /s /q build 
pushd ..\..\src\gwt
ant clean 
popd
