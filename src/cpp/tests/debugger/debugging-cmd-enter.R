
#
# Test that the debugger is able to highlight within
# files that are currently open, even for code that
# was executed via 'Cmd + Enter'. Note that this relies
# on some heuristics (matching source references against
# the document contents itself) so it may not be perfect.
#

test1 <- function() {
   1 + 1
   test2()
   2 + 2
}

test2 <- function() {
   3 + 3
   browser()
   4 + 4
}

test()
