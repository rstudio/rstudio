@echo off
cd install-boost
R --vanilla --slave -f install-boost.R --args debug static
R --vanilla --slave -f install-boost.R --args release static
cd ..
