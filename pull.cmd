call git pull
call git submodule sync
call git submodule update --init
cd src/gwt/tools/ace
call git submodule sync
cd ../../../..
call git submodule update --recursive
