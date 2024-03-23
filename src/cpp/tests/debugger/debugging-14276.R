
#
# Make sure the debug position always advances when stepping through.
#
# https://github.com/rstudio/rstudio/issues/14276
#

# trace(.rs.simulateSourceRefs, quote(str(var)))
eval(parse(text = "fn <- function() {
  x <- 1
  1 + 1
  x <- 1
  2 + 2
  3 + 3
  x <- 1
  4 + 4
}", keep.source = FALSE))

debugonce(fn)
fn()


# No issue stepping through when we have srcrefs.
fn <- function() {
  x <- 1
  1 + 1
  x <- 1
  2 + 2
  3 + 3
  x <- 1
  4 + 4
  5 + 5
}

debugonce(fn)
fn()

