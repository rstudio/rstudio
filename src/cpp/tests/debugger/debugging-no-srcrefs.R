
#
# Source this file, run test(), and then check whether the highlighted code is correct.
#

eval(parse(keep.source = FALSE, text = "
test <- function() {
   apple() + banana() + cherry()
}

apple <- function() {
   1 + 1 # comment
   browser()
   2 + 2 # comment
}

banana <- function() {
   3 + 3
   browser()
   4 + 4
}

cherry <- function() {
   browser()
}
"))
