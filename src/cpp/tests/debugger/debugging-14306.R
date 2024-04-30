
#
# https://github.com/rstudio/rstudio/issues/14306
#

# trace(.rs.simulateSourceRefs, quote(str(var)), print = FALSE)
fn <- NULL
eval(parse(text = "
fn <- function() {
  identity({
    1 + 1
    2 + 2
    3 + 3
  })
}
", keep.source = FALSE))

debugonce(fn)
fn()


# Compare with source references
fn <- function() {
  identity({
    1 + 1
    2 + 2
    3 + 3
    4 + 4
  })
}

debugonce(fn)
fn()
