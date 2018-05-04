#
# This is a Plumber API document. Plumber allows...
#
# TODO: more description, examples, links to documentation...
#
# API may be published to RStudio Connect...

#* @apiTitle Plumber Example
#* @apiDescription Demonstrate common Plumber API annotations

#* Echo back the input
#* @param msg The message to echo
#* @get /echo
function(msg=""){
  list(msg = paste0("The message is: '", msg, "'"))
}

#* Return mean of random generation for the normal distribution
#* @param samples Number of samples (default=10)
#* @get /mean
normalMean <- function(samples=10){
  data <- rnorm(samples)
  mean(data)
}

#* Return the sum of two numbers
#* @param a
#* @param b
#* @post /sum
addTwo <- function(a, b){
  as.numeric(a) + as.numeric(b)
}
