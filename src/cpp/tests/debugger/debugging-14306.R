
#
# https://github.com/rstudio/rstudio/issues/14306
#

# trace(.rs.simulateSourceRefs, quote(str(var)), print = FALSE)
fn <- NULL
eval(parse(text = "
fn <- function() {
  identity({
    browser()
  })
}
", keep.source = FALSE))

fn()


# Compare with source references
fn <- function() {
  identity({
    browser()
  })
}

fn()
