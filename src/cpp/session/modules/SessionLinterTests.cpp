/*
 * SessionLinterTests.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <tests/TestThat.hpp>

#include "SessionLinter.hpp"

#include <iostream>

#include <core/collection/Tree.hpp>
#include <core/FilePath.hpp>
#include <core/system/FileScanner.hpp>
#include <core/FileUtils.hpp>

#include <boost/algorithm/string.hpp>
#include <boost/bind.hpp>
#include <boost/foreach.hpp>

#include <session/SessionOptions.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace linter {

using namespace rparser;

static const ParseOptions s_parseOptions(true);

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
      
      std::string content = file_utils::readFile(core::toFilePath(info));
      ParseResults results = parse(content);
      
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

context("Linter")
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
      EXPECT_NO_ERRORS("if (foo) {()} else {()}");
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
      
      EXPECT_NO_ERRORS("c(a=function()a,)");
      EXPECT_NO_ERRORS("function(a) a");
      EXPECT_NO_ERRORS("function(a)\nwhile (1) 1\n");
      EXPECT_NO_ERRORS("function(a)\nfor (i in 1) 1\n");

      EXPECT_NO_ERRORS("{if(!(a)){};if(b){}}");

      EXPECT_NO_ERRORS("lapply(x, `[[`, 1)");

      EXPECT_NO_ERRORS("a((function() {})())");
      EXPECT_NO_ERRORS("function(a=1,b=2) {}");

      EXPECT_ERRORS("for {i in 1:10}");
      EXPECT_ERRORS("((()})");
      
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
   }
   
   lintRStudioRFiles();
}

} // namespace linter
} // namespace modules
} // namespace session
} // namespace rstudio
