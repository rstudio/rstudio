# Test file for playing around with 'Rename in Scope'.
# Try activating the command on the various 'var's
# within this document and checking that the correct
# instances are selected.
var <- "outside"
env <- new.env(parent = emptyenv())
global <- function(var = "inside") {
  print(var)
  env$var <- 1
  nested <- function(var = "nested") {
    print(var)
  }
  print(var)
}
var

fn <- function() {
  var <- 1
  print(var)
  function(var) {
    var
  }
}
