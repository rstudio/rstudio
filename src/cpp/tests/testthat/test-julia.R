#
# test-julia.R
#
# Copyright (C) 2026 by Posit Software, PBC
#
# Unless you have received this program directly from Posit Software pursuant
# to the terms of a commercial license agreement with Posit Software, then
# this program is licensed to you under the terms of version 3 of the
# GNU Affero General Public License. This program is distributed WITHOUT
# ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
# MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
# AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
#
#

context("julia")

test_that("complete UTF-8 text is passed on whole", {
   # built from code points, to keep this file ASCII
   texts <- c(
      "",
      "abc",
      intToUtf8(c(0x63, 0x61, 0x66, 0xE9)),
      intToUtf8(0x2208),
      intToUtf8(0x1F600),
      intToUtf8(c(0x61, 0xE9, 0x2208, 0x1F600))
   )

   for (text in texts)
   {
      bytes <- charToRaw(enc2utf8(text))
      expect_identical(.rs.julia.completeUtf8Length(bytes), length(bytes), info = text)
   }
})

test_that("a character cut off part-way through is held back", {
   # 2-, 3- and 4-byte characters, after one complete character
   for (char in intToUtf8(c(0xE9, 0x2208, 0x1F600), multiple = TRUE))
   {
      bytes <- c(charToRaw("a"), charToRaw(enc2utf8(char)))
      for (cut in seq_len(length(bytes) - 2L))
      {
         truncated <- bytes[seq_len(length(bytes) - cut)]
         expect_identical(.rs.julia.completeUtf8Length(truncated), 1L, info = paste(char, cut))
      }
   }
})

test_that("bytes that aren't UTF-8 aren't held back", {
   # stray continuation bytes, with no lead byte among the last four
   bytes <- as.raw(c(0x61, 0x80, 0x80, 0x80, 0x80))
   expect_identical(.rs.julia.completeUtf8Length(bytes), length(bytes))
})

test_that("the help handler only answers completion help", {
   expect_identical(.rs.julia.helpHandler("url", "println", "Main"), "")
   expect_null(.rs.julia.helpHandler("parameter", NULL, "Main"))

   # a completion that isn't a binding, such as a field, has no help to find
   expect_null(.rs.julia.helpHandler("completion", "length", ""))
})

test_that("completions and their help come from Julia", {
   skip_if_not_installed("JuliaConnectoR")
   skip_if(is.null(.rs.julia.executable()), "Julia isn't installed")
   skip_if(.rs.julia.isFirstStart(), "Julia's first start installs packages")

   # e.g. 'julia version 1.13.4'
   version <- suppressWarnings(system2(.rs.julia.executable(), "--version", stdout = TRUE, stderr = FALSE))
   version <- regmatches(version, regexpr("[0-9]+\\.[0-9]+", version))
   skip_if(length(version) == 0L, "Julia's version can't be told")
   skip_if(numeric_version(version) < .rs.julia.minimumVersion, "Julia is older than the console supports")

   .rs.julia.start()
   on.exit(.rs.julia.stop(), add = TRUE)

   types <- .rs.acCompletionTypes

   # a module's member, typed so that the client asks the help handler
   completions <- .rs.julia.getCompletions("Base.Mat")
   expect_identical(completions$token, "Mat")
   expect_identical(completions$helpHandler, .rs.scalar(".rs.julia.helpHandler"))
   math <- match("Math", completions$results)
   expect_false(is.na(math))
   expect_identical(completions$packages[[math]], "Base")
   expect_identical(completions$type[[math]], types$ENVIRONMENT)

   # a mutating function, and a macro, which isn't called with parentheses
   completions <- .rs.julia.getCompletions("pushfirs")
   expect_identical(completions$type[completions$results == "pushfirst!"], types$FUNCTION)
   completions <- .rs.julia.getCompletions("@tim")
   expect_identical(completions$type[completions$results == "@time"], types$UNKNOWN)

   # documentation, headed by the signature it begins with
   help <- .rs.julia.helpHandler("completion", "println", "Main")
   expect_identical(help$signature, "println([io::IO], xs...)")
   expect_identical(help$title, "")
   expect_match(help$description, "followed by a newline")
   expect_false(help$has_help)

   # or, without one, by the topic
   help <- .rs.julia.helpHandler("completion", "Math", "Base")
   expect_identical(help$signature, "")
   expect_identical(help$title, "Math")
})
