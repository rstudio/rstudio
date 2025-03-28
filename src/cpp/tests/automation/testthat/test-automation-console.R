
library(testthat)

self <- remote <- .rs.automation.newRemote()
withr::defer(.rs.automation.deleteRemote())


.rs.test("\r is handled as expected within the console", {
   
   remote$console.clear()
   
   lines <- c(
      "✔ xxx \033[34myyy\033[39m xxx",
      "\r",
      "✔ xxx \033[31myyy\033[39m zzz",
      "\n",
      "✔ xxx \033[34myyy\033[39m xxx",
      "\r",
      "✔ xxx \033[31myyy\033[39m zzz",
      "\n"
   )
   
   remote$console.executeExpr(cat(!!lines))
   output <- remote$console.getOutput()
   expected <- c(" ✔ xxx yyy zzz ", " ✔ xxx yyy zzz ")
   expect_equal(tail(output, 2L), expected)
   
})
