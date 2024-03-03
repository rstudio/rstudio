
#
# https://github.com/rstudio/rstudio/issues/10664
#
# Make sure that you can still step through the function
# even after evaluating some of the variables during the
# debug session.
#

library(slider)
debugonce(slider:::slide_impl)
slide_dbl(1:5, identity)
