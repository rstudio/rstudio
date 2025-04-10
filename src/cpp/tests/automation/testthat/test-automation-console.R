
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

.rs.test("errors are rendered as expected", {
   
   remote$console.clear()
   remote$console.executeExpr({
      .rs.uiPrefs$consoleHighlightConditions$set("errors_warnings_messages")
   })
   
   remote$console.executeExpr({
      stop("This is an error.")
   })
   
   output <- remote$console.getOutput()
   expect_contains(output, "Error: This is an error.")
   
   consoleOutputEl <- remote$js.querySelector("#rstudio_console_output")
   
   # Ideally, we'd look for an element with the expected classes, but
   # we can't do that as the class names might change (e.g. GWT obfuscation)
   # so we just hunt for elements with the expected inner text contents.
   foundErrorSpan <- FALSE
   spanEls <- consoleOutputEl$querySelectorAll("span")
   for (i in seq_len(spanEls$length))
   {
      spanEl <- spanEls[[i - 1L]]
      if (identical(spanEl$innerText, "Error"))
      {
         foundErrorSpan <- TRUE
         break
      }
   }
   
   expect_true(foundErrorSpan)
   
})

.rs.test("warnings are rendered as expected", {
   
   remote$console.executeExpr({
      .rs.uiPrefs$consoleHighlightConditions$set("errors_warnings_messages")
   })
   
   remote$console.clear()
   
   remote$console.executeExpr({
      options(warn = 0)
      warning("This is a warning.")
   })
   
   consoleOutputEl <- remote$js.querySelector("#rstudio_console_output")
   
   # Ideally, we'd look for an element with the expected classes, but
   # we can't do that as the class names might change (e.g. GWT obfuscation)
   # so we just hunt for elements with the expected inner text contents.
   foundWarningSpan <- FALSE
   spanEls <- consoleOutputEl$querySelectorAll("span")
   for (i in seq_len(spanEls$length))
   {
      spanEl <- spanEls[[i - 1L]]
      if (identical(spanEl$innerText, "Warning message"))
      {
         foundWarningSpan <- TRUE
         break
      }
   }
   
   expect_true(foundWarningSpan)
   
   
   remote$console.clear()
   
   remote$console.executeExpr({
      options(warn = 1)
      warning("This is a warning.")
   })
   
   output <- remote$console.getOutput()
   expect_contains(output, "Warning: This is a warning.")
   
   consoleOutputEl <- remote$js.querySelector("#rstudio_console_output")
   
   # Ideally, we'd look for an element with the expected classes, but
   # we can't do that as the class names might change (e.g. GWT obfuscation)
   # so we just hunt for elements with the expected inner text contents.
   foundWarningSpan <- FALSE
   spanEls <- consoleOutputEl$querySelectorAll("span")
   for (i in seq_len(spanEls$length))
   {
      spanEl <- spanEls[[i - 1L]]
      if (identical(spanEl$innerText, "Warning"))
      {
         foundWarningSpan <- TRUE
         break
      }
   }
   
   expect_true(foundWarningSpan)
   
})
