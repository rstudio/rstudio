library(Matrix)

env <- function(...) {
   list2env(list(...))
}

object <- list(
   integer = c(a = 1L, b = 2L, c = 3L),
   double  = 1,
   string  = "string",
   symbol  = as.name("symbol"),
   list    = list(1, 2, 3),
   environment = env(a = 1, b = 2, c = 3),
   expression = quote(1 + 1),
   formula = y ~ x + 1,
   factor = factor(letters),
   ordered = ordered(letters),
   closure = function() {},
   primitive = c
)

# debug(.rs.rpc.explorer_inspect_object)
context <- .rs.explorer.createContext(recursive = 1)
i <- .rs.explorer.inspectObject(object, context)
.rs.explorer.viewObject(object)

library(xml2)
doc <- read_xml("<root id='1'><child id ='a' /><child id='b' d='b'/></root>")
.rs.explorer.viewObject(doc)

f <- function(x) {
   c(1)
   c(2)
   c(3)
}

context <- .rs.explorer.createContext(recursive = 1)
inspected <- .rs.explorer.inspectObject(f, context)
str(inspected)
.rs.explorer.viewObject(f)

.rs.explorer.viewObject(rnorm)

big <- replicate(1000, setNames(letters, LETTERS), simplify = FALSE)
context <- .rs.explorer.createContext(recursive = 1)
i <- .rs.explorer.inspectObject(big, context)
.rs.explorer.viewObject(big)
