
#
# Try debugging this file with:
# - First, source() this file, then run foo().
# - Try executing code via Cmd + Enter, then run foo().
#

bar <- function()
{
   1 + 1
   browser()
   3 + 3
}

foo <- function() {
   withCallingHandlers({
      tryCatch({
         identity({
            eval({
               bar()
               2 + 2
            })
         })
      })
   })
}

