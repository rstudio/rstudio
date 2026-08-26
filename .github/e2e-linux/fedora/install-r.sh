#!/usr/bin/env bash
set -euo pipefail

# Fedora is not a supported platform for Posit's r-builds -- the source the
# rstudio-buildtools R mirror is fed from -- or for PPM binaries, so install R
# from Fedora's own repos and let pak compile CRAN packages from source (see
# e2e-setup.sh and os-e2e-deps). The r_version input is ignored on Fedora --
# dnf provides whatever R the distro ships.
#
# Clear tsflags for this transaction so R's documentation tree is installed.
# Fedora container images ship /etc/dnf/dnf.conf with tsflags=nodocs (a
# space-saving default), which makes dnf skip /usr/share/doc/R. RStudio's R
# discovery (r/session/RDiscovery.cpp) requires R_DOC_DIR to exist and aborts
# the session otherwise, so a docless R install starts but never reaches
# window.rstudio.ready.
sudo dnf -y install --setopt=tsflags='' R-core R-core-devel
R --version

# Diagnostics: the doc dir R reports, and confirm it now exists.
Rscript -e 'cat("doc=", R.home("doc"), " R_DOC_DIR=", Sys.getenv("R_DOC_DIR"), "\n", sep="")'
ls -ld /usr/share/doc/R || echo "MISSING /usr/share/doc/R"

mkdir -p "$R_LIBS_USER"
