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
   expect_identical(.rs.julia.helpHandler("url", "println", ""), "")
   expect_null(.rs.julia.helpHandler("parameter", NULL, ""))
})
