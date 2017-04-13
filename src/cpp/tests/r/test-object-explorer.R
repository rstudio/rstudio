library(Matrix)

env <- function(...) {
   list2env(list(...))
}

m <- Matrix(0, 3, 2)

object <- list(
   a = 1:100,
   
   b = list(
      b1 = "b1",
      b2 = list(
         b21 = "b21",
         b22 = "b22"
      )
   ),
   
   c = env(
      c1 = "c1",
      c2 = "c2"
   ),
   
   m = m
)

# debug(.rs.rpc.explorer_inspect_object)
context <- .rs.explorer.createContext(recursive = 1)
i <- .rs.explorer.inspectObject(object, context)
.rs.explorer.viewObject(object)


library(xml2)
x <- read_xml("<root id='1'><child id ='a' /><child id='b' d='b'/></root>")
.rs.explorer.viewObject(x)
