# compute five-number summary
fivenum <- function(x) {
  # handle non-numeric input
   if (!is.numeric(x))
      stop("`x` must be numeric.")

  # handle empty input
  n <- length(x)
  if (n == 0)
     return(rep.int(NA, 5))
   
  # compute quartile indices
  n5 <- 1
  n4 <- ((n + 3) %/% 2) / 2
  n3 <- (n + 1) / 2
  n2 <- n + 1 - n4
  n1 <- n
  i <- c(n5, n4, n3, n2, n1)
  
  # compute quartile values
  x <- sort(x)
  xf <- x[floor(i)]
  xc <- x[ceiling(i)]
  0.5 * (xf + xc)

}
