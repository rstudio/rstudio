
#
# Run this code, and then try to explore the Traceback frames.
# You should get sensible views into the running code.
#

debugonce(dplyr:::new_error_context)
dplyr::mutate(mtcars, mpg2 = mpg * 2)
