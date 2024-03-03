
#
# https://github.com/rstudio/rstudio/issues/14306
#

# trace(.rs.simulateSourceRefs, quote(str(var)), print = FALSE)
eval(parse(text = "fn <- function() {
  identity({
    1 + 1
    browser()
    3 + 3
    4 + 4
    5 + 5
  })
}
", keep.source = FALSE))

fn()


# Compare with source references
fn2 <- function() {
   identity({
     identity({
      1 + 1
      browser()
      3 + 3
      4 + 4
      5 + 5
     })
   })
}

fn2()
