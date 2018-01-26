@echo off

cd install-openssl
R --vanilla --slave -f install-openssl.R
cd ..