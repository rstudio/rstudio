
#
# Source this file, run test(), and then check whether the highlighted code is correct.
#

eval(parse(keep.source = FALSE, text = "
test <- function() {
   apple() + banana() + cherry()
}

apple <- function() {
   'apple'
   browser()
}

banana <- function() {
   'banana'
   browser()
}

cherry <- function() {
   'cherry'
   browser()
}
"))
