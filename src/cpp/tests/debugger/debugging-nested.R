
bottom <- function() {
   "bottom"
   browser()
}

middle <- function() {
   "middle"
   bottom()
}

top <- function() {
   "top"
   middle()
}
