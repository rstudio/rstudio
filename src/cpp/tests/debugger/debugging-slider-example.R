
#
# https://github.com/rstudio/rstudio/issues/10664
#
# Make sure that you can still step through the function
# even after evaluating some of the variables during the
# debug session.
#
# Install the package with:
#
# install.packages("slider", type = "source", INSTALL_opts = "--with-keep.source")
#

library(slider)
debugonce(slider:::slide_impl)
slide_dbl(1:5, identity)
