
#
# Try debugging this file with:
# - First, source() this file, then run foo().
# - Try executing code via Cmd + Enter, then run foo().
#

bar <- function()
{
   1 + 1
   browser()
   2 + 2
}

foo <- function() {
   withCallingHandlers({
      tryCatch({
         identity({
            bar()
            2 + 2
         })
      })
   })
}

