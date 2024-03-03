
#
# https://github.com/rstudio/rstudio/issues/14306
#

eval(parse(text = "fn <- function() {
  tryCatch({
    1 + 1
    2 + 2
    3 + 3
  })
}
", keep.source = FALSE))

debugonce(fn)
fn()
