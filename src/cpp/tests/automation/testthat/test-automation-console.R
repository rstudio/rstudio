
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

# https://github.com/rstudio/rstudio/issues/16031
.rs.test("warnings are treated as errors when options(warn = 2)", {
   
   remote$console.executeExpr({
      
      options(warn = 2)
      
      x <- tryCatch(
         as.numeric("oops"),
         error = identity
      )
      
      options(warn = 0)
      inherits(x, "error")
      
   })
   
   output <- remote$console.getOutput(n = 1L)
   expect_equal(output, "[1] TRUE")
   
})

# https://github.com/rstudio/rstudio/issues/16038
.rs.test("carriage returns don't break output annotation", {
   
   remote$console.clear()
   remote$console.executeExpr({
      message("M1"); cat("O1\r"); message("M2")
   })
   
   spanOutputs <- list()
   spanEls <- consoleOutputEl$querySelectorAll("span")
   for (i in seq_len(spanEls$length))
   {
      spanEl <- spanEls[[i - 1L]]
      spanOutputs[[spanEl$innerText]] <- TRUE
   }
   
   spanOutputs <- names(spanOutputs)
   
   expect_true("M1" %in% spanOutputs)
   expect_true("M2" %in% spanOutputs)
   expect_false("O1" %in% spanOutputs)
   
   m1idx <- which(spanOutputs == "M1")
   m2idx <- which(spanOutputs == "M2")
   expect_equal(m2idx - m1idx, 1L)
   
   remote$console.executeExpr({
      writeLines("This is some more output.")
   })
   
   output <- remote$console.getOutput(n = 1L)
   expect_equal(output, "This is some more output.")
   
   remote$console.clear()
   remote$console.executeExpr({
      message("M1", appendLF = FALSE); message("\b", appendLF = FALSE); message("2")
   })
   
   output <- remote$console.getOutput(n = 1L)
   expect_equal(output, "M2")
   
})

.rs.test("text is truncated appropriately in console", {
   
   remote$console.executeExpr({
      .rs.uiPrefs$consoleLineLengthLimit$set(10L)
   })
   
   remote$console.executeExpr({
      long <- paste(rep.int("a", 1E4), collapse = "")
      cat(long, sep = "\n")
   })
   
   output <- remote$console.getOutput(n = 1L)
   expect_equal(output, "aaaaaaaaaa ... <truncated>")
   
   remote$console.executeExpr({
      .rs.uiPrefs$consoleLineLengthLimit$set(2000L)
   })
   
})

# https://github.com/rstudio/rstudio/issues/16337
.rs.test("error output is not lost following caught error", {
   
   code <- quote({
      foo <- function() {
         writeLines("Some output.")
         try(stop("try(silent = FALSE)"), silent = FALSE)
         writeLines("Some more output.")
         stop("Error.")
      }
   })
   
   remote$editor.openWithContents(".R", deparse(code))
   remote$commands.execute(.rs.appCommands$sourceActiveDocument)
   remote$console.executeExpr(foo())
   
   output <- remote$console.getOutput()
   expect_true("Some output." %in% output)
   expect_true("Some more output." %in% output)

})

# https://github.com/rstudio/rstudio/issues/16973
.rs.test("AceEditorCommandDispatcher shortcuts work in console", {

   remote$console.clear()

   # Focus the console input
   remote$keyboard.insertText("<Ctrl + 2>")
   Sys.sleep(0.5)

   # Execute insert pipe operator shortcut
   remote$keyboard.executeShortcut("Ctrl + Shift + M")
   Sys.sleep(0.5)

   # Read the console input editor's value
   contents <- remote$js.eval(
      "document.getElementById('rstudio_console_input').env.editor.getValue()"
   )
   expect_true(grepl("|>", contents, fixed = TRUE) || grepl("%>%", contents, fixed = TRUE))

   # Clear and try insert assignment operator
   remote$keyboard.insertText("<Ctrl + A>", "<Backspace>")
   remote$keyboard.executeShortcut("Alt + -")
   Sys.sleep(0.5)

   contents <- remote$js.eval(
      "document.getElementById('rstudio_console_input').env.editor.getValue()"
   )
   expect_true(grepl("<-", contents, fixed = TRUE))

   # Clean up
   remote$keyboard.insertText("<Ctrl + A>", "<Backspace>")

})
