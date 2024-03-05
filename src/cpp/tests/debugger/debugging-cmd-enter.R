
#
# Test that the debugger is able to highlight within
# files that are currently open, even for code that
# was executed via 'Cmd + Enter'. Note that this relies
# on some heuristics (matching source references against
# the document contents itself) so it may not be perfect.
#
# https://github.com/rstudio/rstudio/issues/13925
#

inner <- function() {
   3 + 3
   4 + 4
}

outer <- function() {
   1 + 1
   inner()
   2 + 2
}

debugonce(outer)
outer()
