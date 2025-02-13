/*
 * SessionDiagnosticsTests.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <tests/TestThat.hpp>

#include "SessionDiagnostics.hpp"

#include <iostream>

#include <core/collection/Tree.hpp>
#include <shared_core/FilePath.hpp>
#include <core/system/FileScanner.hpp>
#include <core/FileUtils.hpp>

#include <boost/algorithm/string.hpp>
#include <boost/bind/bind.hpp>

#include <session/SessionOptions.hpp>
#include "SessionRParser.hpp"

using namespace boost::placeholders;

namespace rstudio {
namespace session {
namespace modules {
namespace diagnostics {

using namespace rparser;

static const ParseOptions s_parseOptions(true, true, true, true, true, true);

using namespace core;
using namespace core::r_util;

// We use macros so that the test output gives
// meaningful line numbers.
#define EXPECT_ERRORS(__STRING__)                                              \
   do                                                                          \
   {                                                                           \
      ParseResults results = parse(__STRING__, s_parseOptions);                \
      expect_true(results.lint().hasErrors());                                 \
   } while (0)

#define EXPECT_NO_ERRORS(__STRING__)                                           \
   do                                                                          \
   {                                                                           \
      ParseResults results = parse(__STRING__, s_parseOptions);                \
      if (results.lint().hasErrors())                                          \
         results.lint().dump();                                                \
      expect_false(results.lint().hasErrors());                                \
   } while (0)

#define EXPECT_LINT(__STRING__)                                                \
   do                                                                          \
   {                                                                           \
      ParseResults results = parse(__STRING__, s_parseOptions);                \
      expect_false(results.lint().get().empty());                              \
   } while (0)

#define EXPECT_NO_LINT(__STRING__)                                             \
   do                                                                          \
   {                                                                           \
      ParseResults results = parse(__STRING__, s_parseOptions);                \
      expect_true(results.lint().get().empty());                               \
   } while (0)

bool isRFile(const FileInfo& info)
{
   std::string ext = string_utils::getExtension(info.absolutePath());
   return string_utils::toLower(ext) == ".r";
}

void lintRFilesInSubdirectory(const FilePath& path)
{
   tree<core::FileInfo> fileTree;
   
   core::system::FileScannerOptions fsOptions;
   fsOptions.recursive = true;
   fsOptions.yield = true;
   
   core::system::scanFiles(core::toFileInfo(path),
             fsOptions,
             &fileTree);
   
   tree<core::FileInfo>::leaf_iterator it = fileTree.begin_leaf();
   for (; fileTree.is_valid(it); ++it)
   {
      const FileInfo& info = *it;
      
      if (info.isDirectory())
         continue;
      
      if (!isRFile(info))
         continue;
      
      FilePath filePath = core::toFilePath(info);
      std::cerr << "Parsing: " << filePath << std::endl;
      ParseResults results = parse(filePath);
      
      if (results.lint().hasErrors())
      {
         FAIL("Lint errors: '" + info.absolutePath() + "'");
      }
   }
}

void lintRStudioRFiles()
{
   lintRFilesInSubdirectory(options().coreRSourcePath());
   lintRFilesInSubdirectory(options().modulesRSourcePath());
}

test_context("Diagnostics")
{
   test_that("valid expressions generate no lint")
   {
      EXPECT_NO_ERRORS("print(1)");
      EXPECT_NO_ERRORS("1 + 1");
      EXPECT_NO_ERRORS("1; 2; 3; 4; 5");

      EXPECT_NO_ERRORS("(1)(1, 2, 3)");

      EXPECT_NO_ERRORS("{{{}}}");

      EXPECT_NO_ERRORS("for (i in 1) 1");
      EXPECT_NO_ERRORS("(for (i in 1:10) i)");
      EXPECT_NO_ERRORS("for (i in 10) {}");
      EXPECT_NO_ERRORS("(1) * (2)");
      EXPECT_NO_ERRORS("while ((for (i in 10) {})) 1");
      EXPECT_NO_ERRORS("while (for (i in 0) 0) 0");
      EXPECT_NO_ERRORS("({while(1){}})");

      EXPECT_NO_ERRORS("if (foo) bar");
      EXPECT_NO_ERRORS("if (foo) bar else baz");
      EXPECT_NO_ERRORS("if (foo) bar else if (baz) bam");
      EXPECT_NO_ERRORS("if (foo) bar else if (baz) bam else bat");
      EXPECT_NO_ERRORS("if (foo) {} else if (bar) {}");
      EXPECT_NO_ERRORS("if (foo) {(1)} else {(1)}");
      EXPECT_ERRORS("if (foo) {()} else {()}"); // () with no contents invalid if not function
      EXPECT_NO_ERRORS("if(foo){bar}else{baz}");
      EXPECT_NO_ERRORS("if (a) a() else if (b()) b");
      
      EXPECT_NO_ERRORS("if(1)if(2)if(3)if(4)if(5)5");
      EXPECT_NO_ERRORS("if(1)if(2)if(3)if(4)if(5)5 else 6");

      EXPECT_NO_ERRORS("a(b()); b");
      EXPECT_NO_ERRORS("a(x = b()); b");
      EXPECT_NO_ERRORS("a({a();})");
      EXPECT_NO_ERRORS("a({a(); if (b) c})");
      EXPECT_NO_ERRORS("{a()\nb()}");
      EXPECT_NO_ERRORS("a()[[1]]");

      EXPECT_NO_ERRORS("a[,a]");
      EXPECT_NO_ERRORS("a[,,]");
      EXPECT_NO_ERRORS("a(,,1,,,{{}},)");
      EXPECT_NO_ERRORS("x[x,,a]");
      EXPECT_NO_ERRORS("x(x,,a)");
      EXPECT_NO_ERRORS("x[1,,]");
      EXPECT_NO_ERRORS("x(1,,)");
      EXPECT_NO_ERRORS("a=1 #\nb");
      
      EXPECT_ERRORS("foo(a = 1 b = 2)");
      EXPECT_ERRORS("foo(a = 1\nb = 2)");
      
      EXPECT_NO_ERRORS("rnorm(n = 1)");
      EXPECT_NO_ERRORS("rnorm(`n` = 1)");
      EXPECT_NO_ERRORS("rnorm('n' = 1)");
      
      EXPECT_NO_ERRORS("c(a=function()a,)");
      EXPECT_NO_ERRORS("function(a) a");
      EXPECT_NO_ERRORS("function(a)\nwhile (1) 1\n");
      EXPECT_NO_ERRORS("function(a)\nfor (i in 1) 1\n");

      EXPECT_NO_ERRORS("{if(!(a)){};if(b){}}");
      EXPECT_NO_ERRORS("if (1) foo(1) <- 1 else 2; 1 + 2");
      EXPECT_ERRORS("if (1)\nfoo(1) <- 1\nelse 2; 4 + 8"); // invalid 'else' at top level
      EXPECT_NO_ERRORS("{if (1)\nfoo(1) <- 1\nelse 2\n4 + 8}");
      EXPECT_NO_ERRORS("if (1) (foo(1) <- {{1}})\n2 + 1");
      EXPECT_NO_ERRORS("if (1) function() 1 else 2");
      EXPECT_NO_ERRORS("if (1) function() b()() else 2");
      EXPECT_NO_ERRORS("if (1) if (2) function() a() else 3 else 4");
      
      EXPECT_NO_ERRORS("if(1)while(2) 2 else 3");
      EXPECT_NO_ERRORS("if(1)if(2)while(3)while(4)if(5) 6 else 7 else 8 else 9");
      EXPECT_NO_ERRORS("if(1)if(2)while(3)while(4)if(5) foo()[]() else bar() else 8 else 9");
      EXPECT_ERRORS("if(1)while(2)function(3)repeat(4)if(5)(function())() else 6");
      
      EXPECT_NO_ERRORS("if(1)function(){}else 2");
      EXPECT_NO_ERRORS("if(1)function()function(){}else 2");
      EXPECT_NO_ERRORS("if(1){}\n{}");
      EXPECT_NO_ERRORS("foo(1, 'x'=,\"y\"=,,,z=1,,,`k`=,)");
      
      EXPECT_NO_ERRORS("foo()\n{}");
      EXPECT_NO_ERRORS("{}\n{}");
      EXPECT_NO_ERRORS("1\n{}");
      
      // function body cannot be empty paren list; in general, '()' not allowed
      // at 'start' scope
      EXPECT_ERRORS("(function() ())()");
      
      // EXPECT_ERRORS("if (1) (1)\nelse (2)");
      EXPECT_NO_ERRORS("{if (1) (1)\nelse (2)}");
      
      EXPECT_NO_ERRORS("{if (a)\nF(b) <- 'c'\nelse if (d) e}");

      EXPECT_NO_ERRORS("lapply(x, `[[`, 1)");

      EXPECT_NO_ERRORS("a((function() {})())");
      EXPECT_NO_ERRORS("function(a=1,b=2) {}");

      EXPECT_ERRORS("for {i in 1:10}");
      EXPECT_ERRORS("((()})");
      
      EXPECT_ERRORS("(a +)");
      EXPECT_ERRORS("{a +}");
      EXPECT_ERRORS("foo[[bar][baz]]");
      
      EXPECT_NO_ERRORS("myvar <- con; readLines(con = stdin())");

      EXPECT_NO_LINT("(function(a) a)");
      
      EXPECT_LINT("a <- 1\nb <- 2\na+b");
      EXPECT_NO_LINT("a <- 1\nb <- 2\na +\nb");
      EXPECT_NO_LINT("a <- 1\nb <- 2\na()$'b'");
      EXPECT_LINT("a <- 1\na$1");
      EXPECT_LINT("- 1");
      
      EXPECT_LINT("foo <- 1 + foo");
      EXPECT_NO_LINT("foo <- 1 + foo()");
      EXPECT_LINT("foo <- rnorm(n = foo)");
      EXPECT_LINT("rnorm (1)");
      EXPECT_NO_LINT("n <- 1; rnorm(n = n)");
      
      EXPECT_NO_LINT("n <- 1 ## a comment\nprint(n)");
      EXPECT_NO_LINT("n <- 1 + 2 ## a comment\nprint(n)");
      
      EXPECT_NO_ERRORS("{lm(formula = log(y - 1) ~ x, data = mtcars)}");
      // EXPECT_NO_ERRORS("list(par = function(a) par(mar = a))");
      EXPECT_NO_LINT("f <- function(x) {\n  TRUE\n  !grepl(':$', x)\n}");
      
      EXPECT_NO_ERRORS("# ouch"); // previously segfaulted due to lack of significant tokens
      
      EXPECT_NO_ERRORS("if(1)while(2)while(3)while(4)foo() else 5");
      
      EXPECT_NO_ERRORS("{\nif (1) {} else if (2) {}\nif (1)\n1\nelse if (2)\n2}");
      
      EXPECT_NO_ERRORS("({if (1) for(i in 1) {}})");
      
      EXPECT_LINT("c(1,,2)");
      
      EXPECT_NO_ERRORS("1:10 %>% {} %>% print");
      
      EXPECT_NO_ERRORS("y ~ s(x, bs = 'cs')");
      
      EXPECT_NO_ERRORS("y ~ (1)");
      
      EXPECT_NO_ERRORS("{x()\n{}}");
      EXPECT_NO_ERRORS("{a <- 1\n~ x + 1}\n");
      
      EXPECT_NO_ERRORS("(~ map())");
      EXPECT_NO_ERRORS("quote(1)\n~ apple");
      
      EXPECT_NO_LINT("foo(!! abc)");
      EXPECT_NO_LINT("foo(!!! abc)");
      
      EXPECT_NO_LINT("function() { i <- 1; function() { data[i] } }");
      
      EXPECT_ERRORS("{\nx\n<- 1\n}");
      
      EXPECT_ERRORS("%a\nb%");
      
      EXPECT_ERRORS("local({ if (TRUE) })");

      EXPECT_NO_ERRORS("phi = function(`arg 1`) 1 + 1\nph(`arg 1` = 1)");
      EXPECT_NO_ERRORS("'a\nb' <- 1");
      EXPECT_NO_ERRORS("`a\nb` <- 1");
      
      EXPECT_NO_ERRORS("mtcars |> data => lm(mpg ~ cyl, data = data)");
      
      EXPECT_NO_LINT("x <- { 1 + 1 }");
      EXPECT_NO_LINT("x <- ( 1 + 1 )");
      EXPECT_NO_LINT("x <- {1}");
      EXPECT_NO_LINT("x <- (1)");
      
      EXPECT_NO_LINT("apples <- 42; glue(\"{apples} and {bananas}\", bananas = 24)");
      EXPECT_NO_LINT("mtcars %>% stats::lm(mpg ~ cyl, data = .)");
      
      EXPECT_NO_LINT("r'())'");

      EXPECT_ERRORS("if (x = 1) {}");
      EXPECT_ERRORS("while (x = 42) {}");
      EXPECT_ERRORS("for (x = 1:5) {}");
      
      EXPECT_NO_LINT("{ apple <- banana <- 42; apple + banana }");
      EXPECT_NO_LINT("{ .[apple, banana] <- c(1, 2); apple + banana }");

      EXPECT_NO_ERRORS("c(warning = function() {}); warning(42)");
   }
   
   test_that("RStudio files can be successfully linted")
   {
      lintRStudioRFiles();
   }
}

} // namespace linter
} // namespace modules
} // namespace session
} // namespace rstudio
