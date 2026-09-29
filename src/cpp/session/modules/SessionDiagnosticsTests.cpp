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

#include <gtest/gtest.h>

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
      EXPECT_TRUE(results.lint().hasErrors());                                 \
   } while (0)

#define EXPECT_NO_ERRORS(__STRING__)                                           \
   do                                                                          \
   {                                                                           \
      ParseResults results = parse(__STRING__, s_parseOptions);                \
      if (results.lint().hasErrors())                                          \
         results.lint().dump();                                                \
      EXPECT_FALSE(results.lint().hasErrors());                                \
   } while (0)

#define EXPECT_LINT(__STRING__)                                                \
   do                                                                          \
   {                                                                           \
      ParseResults results = parse(__STRING__, s_parseOptions);                \
      EXPECT_FALSE(results.lint().get().empty());                              \
   } while (0)

#define EXPECT_NO_LINT(__STRING__)                                             \
   do                                                                          \
   {                                                                           \
      ParseResults results = parse(__STRING__, s_parseOptions);                \
      EXPECT_TRUE(results.lint().get().empty());                               \
   } while (0)

bool hasLintContaining(const ParseResults& results, const std::string& needle)
{
   for (const LintItem& item : results.lint().get())
      if (item.message.find(needle) != std::string::npos)
         return true;
   return false;
}

#define EXPECT_LINT_MESSAGE(__STRING__, __MESSAGE__)                           \
   do                                                                          \
   {                                                                           \
      ParseResults results = parse(__STRING__, s_parseOptions);                \
      if (!hasLintContaining(results, __MESSAGE__))                            \
         results.lint().dump();                                                \
      EXPECT_TRUE(hasLintContaining(results, __MESSAGE__));                    \
   } while (0)

#define EXPECT_NO_LINT_MESSAGE(__STRING__, __MESSAGE__)                        \
   do                                                                          \
   {                                                                           \
      ParseResults results = parse(__STRING__, s_parseOptions);                \
      if (hasLintContaining(results, __MESSAGE__))                             \
         results.lint().dump();                                                \
      EXPECT_FALSE(hasLintContaining(results, __MESSAGE__));                   \
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
         GTEST_FAIL() << "Lint errors: '" << info.absolutePath() << "'";
      }
   }
}

void lintRStudioRFiles()
{
   lintRFilesInSubdirectory(options().coreRSourcePath());
   lintRFilesInSubdirectory(options().modulesRSourcePath());
}

TEST(DiagnosticsTest, ValidExpressionsGenerateNoLint) {
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
   
   EXPECT_NO_ERRORS("c(a=function()a)");
   EXPECT_LINT_MESSAGE("c(a=function()a,)", "empty trailing argument in call to 'c'");
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
   EXPECT_NO_LINT("x <- 1; glue(\"{ x + \\\"hello\\\" }\")"); // escaped quotes in glue expression
   EXPECT_NO_LINT("mtcars %>% stats::lm(mpg ~ cyl, data = .)");
   
   EXPECT_NO_LINT("r'())'");

   EXPECT_ERRORS("if (x = 1) {}");
   EXPECT_ERRORS("while (x = 42) {}");
   EXPECT_ERRORS("for (x = 1:5) {}");
   
   EXPECT_NO_LINT("{ apple <- banana <- 42; apple + banana }");
   EXPECT_NO_LINT("{ .[apple, banana] <- c(1, 2); apple + banana }");

   EXPECT_NO_ERRORS("c(warning = function() {}); warning(42)");

   // single bracket argument lists
   EXPECT_NO_ERRORS("x[]");
   EXPECT_NO_ERRORS("x[1]");
   EXPECT_NO_ERRORS("x[1, 2]");
   EXPECT_NO_ERRORS("x[, 1]");
   EXPECT_NO_ERRORS("x[1, ]");

   // numeric literals can be indexed, and even 'called'
   // https://github.com/rstudio/rstudio/issues/18717
   EXPECT_NO_ERRORS("1[TRUE]");
   EXPECT_NO_ERRORS("1[[1]]");
   EXPECT_NO_ERRORS("quote(1[kg])");
   EXPECT_NO_ERRORS("1.5e3[2]");
   EXPECT_NO_ERRORS("0x10[[1]]");
   EXPECT_NO_ERRORS("foo(1[2])");
   EXPECT_NO_ERRORS("1(2)");

   // hex literals with uppercase prefixes, binary exponents, fractions
   // https://github.com/rstudio/rstudio/issues/14363
   EXPECT_NO_ERRORS("0XFF");
   EXPECT_NO_ERRORS("x <- 0x1p3");
   EXPECT_NO_ERRORS("0xAp-2 + 0X2P2");
   EXPECT_NO_ERRORS("0x1.8p3");
   EXPECT_NO_ERRORS("0xFi");
   EXPECT_NO_ERRORS("0xFL");

   // semicolons can end the document
   EXPECT_NO_ERRORS("1;");
   EXPECT_NO_ERRORS("x <- 1;");
   EXPECT_NO_ERRORS("f(); g();");
   EXPECT_NO_ERRORS("1; ");
   EXPECT_NO_ERRORS("1;\n");
   EXPECT_NO_ERRORS("f <- function() 1;");
   EXPECT_ERRORS("1;;");
}

TEST(DiagnosticsTest, SymbolRangesDoNotLeakAcrossParseOperations) {
   // Parse a first expression that would populate the parse tree
   ParseResults results1 = parse("{ x <- 1; y <- 2; x + y }", s_parseOptions);
   EXPECT_TRUE(results1.lint().get().empty());

   // A second parse should not see symbols from the first
   ParseResults results2 = parse("{ x + y }", s_parseOptions);

   // The symbols from the first parse should not leak into the second.
   // Verify that x and y are unresolved in the second parse.
   std::vector<ParseItem> unresolved;
   results2.parseTree()->findAllUnresolvedSymbols(&unresolved);
   EXPECT_FALSE(unresolved.empty());
}

TEST(DiagnosticsTest, IsSymbolDefinedButNotUsed) {
   // Single definition, one self-reference: defined but not used
   ParseResults results1 = parse("function() { x <- 1 }", s_parseOptions);
   ASSERT_FALSE(results1.parseTree()->getChildren().empty());
   ParseNode* fnNode1 = results1.parseTree()->getChildren()[0].get();
   EXPECT_TRUE(fnNode1->isSymbolDefinedButNotUsed("x", false, false));

   // Single definition with a real usage: not "defined but not used"
   ParseResults results2 = parse("function() { x <- 1; print(x) }", s_parseOptions);
   ASSERT_FALSE(results2.parseTree()->getChildren().empty());
   ParseNode* fnNode2 = results2.parseTree()->getChildren()[0].get();
   EXPECT_FALSE(fnNode2->isSymbolDefinedButNotUsed("x", false, false));

   // Multiple definitions, no real usage: defined but not used
   ParseResults results3 = parse("function() { x <- 1; x <- 2 }", s_parseOptions);
   ASSERT_FALSE(results3.parseTree()->getChildren().empty());
   ParseNode* fnNode3 = results3.parseTree()->getChildren()[0].get();
   EXPECT_TRUE(fnNode3->isSymbolDefinedButNotUsed("x", false, false));
}

TEST(DiagnosticsTest, ParentAssignmentWalksDefinitions) {
   // x is defined in outer scope; <<- should find it
   EXPECT_NO_LINT("{ x <- 1; f <- function() { x <<- 2 }; x }");
}

TEST(DiagnosticsTest, HandleElseTokenDoesNotCorruptState) {
   // else following if inside braces with subsequent statements
   EXPECT_NO_ERRORS("{if(1) function(){} else 2; 3}");
   EXPECT_NO_ERRORS("{if(1) function() function(){} else 2; 3}");

   // nested control flow before else
   EXPECT_NO_ERRORS("{if(1) while(2) for(i in 3) 4 else 5; 6}");

   // stray else without matching if
   EXPECT_ERRORS("{1 else 2}");
}

TEST(DiagnosticsTest, RepeatedFormalArguments) {
   EXPECT_LINT_MESSAGE("function(x, x) {}", "repeated formal argument 'x'");
   EXPECT_LINT_MESSAGE("function(x = 1, y, x) {}", "repeated formal argument 'x'");
   EXPECT_LINT_MESSAGE("\\(a, b, a) a + b", "repeated formal argument 'a'");
   EXPECT_NO_LINT_MESSAGE("function(x, y) { x <- 1; y }", "repeated formal");
   EXPECT_NO_LINT_MESSAGE("function(x, ...) x", "repeated formal");

   // formals of a nested function definition live in their own scope
   EXPECT_NO_LINT_MESSAGE("function(x) function(x) x", "repeated formal");
   EXPECT_NO_LINT_MESSAGE("function(x, f = function(x) x) x", "repeated formal");
}

TEST(DiagnosticsTest, ArgumentsMatchedMultipleTimes) {
   EXPECT_LINT_MESSAGE("rnorm(n = 1, n = 2)", "formal argument 'n' matched by multiple");
   EXPECT_LINT_MESSAGE("rnorm(1, mean = 0, mean = 1)", "formal argument 'mean' matched by multiple");
   EXPECT_LINT_MESSAGE("paste(sep = ',', sep = ' ')", "formal argument 'sep' matched by multiple");
   EXPECT_LINT_MESSAGE("f <- function(x, y) x; f(x = 1, x = 2)", "formal argument 'x' matched by multiple");
   EXPECT_NO_LINT_MESSAGE("rnorm(n = 1, mean = 2)", "matched by multiple");

   // names absorbed by '...' may legitimately repeat
   EXPECT_NO_LINT_MESSAGE("c(a = 1, a = 2)", "matched by multiple");
   EXPECT_NO_LINT_MESSAGE("list(a = 1, a = 2)", "matched by multiple");
   EXPECT_NO_LINT_MESSAGE("paste(a = 1, a = 2)", "matched by multiple");

   // nested calls are inspected on their own
   EXPECT_NO_LINT_MESSAGE("rnorm(n = 1, mean = rnorm(n = 1))", "matched by multiple");
}

TEST(DiagnosticsTest, InvalidAssignmentTargets) {
   EXPECT_LINT_MESSAGE("TRUE <- 1", "invalid assignment target 'TRUE'");
   EXPECT_LINT_MESSAGE("NULL <- 1", "invalid assignment target 'NULL'");
   EXPECT_LINT_MESSAGE("NA <- 1", "invalid assignment target 'NA'");
   EXPECT_LINT_MESSAGE("1 <- x", "invalid assignment target '1'");
   EXPECT_LINT_MESSAGE("2L <<- x", "invalid assignment target '2L'");
   EXPECT_LINT_MESSAGE("x -> 1", "invalid assignment target '1'");
   EXPECT_LINT_MESSAGE("x ->> FALSE", "invalid assignment target 'FALSE'");
   EXPECT_LINT_MESSAGE("f <- function() { Inf = 1 }", "invalid assignment target 'Inf'");

   // strings and symbols are fine, as are constants on the value side
   EXPECT_NO_LINT_MESSAGE("\"x\" <- 1", "invalid assignment target");
   EXPECT_NO_LINT_MESSAGE("x <- TRUE", "invalid assignment target");
   EXPECT_NO_LINT_MESSAGE("NULL -> x", "invalid assignment target");
   EXPECT_NO_LINT_MESSAGE("T <- 1", "invalid assignment target");
   EXPECT_NO_LINT_MESSAGE("x[1] <- 2", "invalid assignment target");
   EXPECT_NO_LINT_MESSAGE("f(x = 1, NA)", "invalid assignment target");
   EXPECT_NO_LINT_MESSAGE("list(TRUE = 1)", "invalid assignment target");
}

TEST(DiagnosticsTest, BareReturn) {
   EXPECT_LINT_MESSAGE("f <- function(x) { if (x) return; 1 }", "'return' used without parentheses");
   EXPECT_LINT_MESSAGE("f <- function(x) {\n  return\n}", "'return' used without parentheses");
   EXPECT_NO_LINT_MESSAGE("f <- function(x) { if (x) return() else return(1) }", "'return' used without");
   EXPECT_NO_LINT_MESSAGE("f <- function(x) { return (x) }", "'return' used without");
   EXPECT_NO_LINT_MESSAGE("x$return", "'return' used without");
   EXPECT_NO_LINT_MESSAGE("base::return(1)", "'return' used without");
   EXPECT_NO_LINT_MESSAGE("list(return = 1)", "'return' used without");
   EXPECT_NO_LINT_MESSAGE("'return'", "'return' used without");

   // 'return' can be an ordinary variable (grid has a formal named 'return')
   EXPECT_NO_LINT_MESSAGE("f <- function(x, return = FALSE) { if (return) x }", "'return' used without");
   EXPECT_NO_LINT_MESSAGE("f <- function(x, return = FALSE) list(return = return)", "'return' used without");

   // references to the function itself are not statements
   EXPECT_NO_LINT_MESSAGE("identical(e[[1]], quote(return))", "'return' used without");
   EXPECT_NO_LINT_MESSAGE("body(f)[[1]] == quote(return)", "'return' used without");
   EXPECT_NO_LINT_MESSAGE("lapply(x, return)", "'return' used without");
   EXPECT_NO_LINT_MESSAGE("ret <- return", "'return' used without");
   EXPECT_NO_LINT_MESSAGE("f <- function(x) { identical(x, return) }", "'return' used without");

   // statements within a block passed to a function still are
   EXPECT_LINT_MESSAGE("local({ if (x) return; 1 })", "'return' used without parentheses");
   EXPECT_LINT_MESSAGE("f <- function(x) if (x) return else 1", "'return' used without parentheses");
   EXPECT_LINT_MESSAGE("f <- function(x) {\n  if (x) return\n  -1\n}", "'return' used without parentheses");
}

TEST(DiagnosticsTest, InvalidCharacters) {
   EXPECT_LINT_MESSAGE("x <- \xE2\x80\x9C" "hello" "\xE2\x80\x9D", "smart quote");
   EXPECT_LINT_MESSAGE("x <- \xE2\x80\x98" "hello" "\xE2\x80\x99", "smart quote");
   EXPECT_ERRORS("x <- \xE2\x80\x9C" "hello" "\xE2\x80\x9D");
   EXPECT_NO_LINT_MESSAGE("x <- \"hello\"", "smart quote");
   EXPECT_NO_LINT_MESSAGE("x <- 'hello'", "smart quote");
   EXPECT_NO_LINT_MESSAGE("x <- \"\xE2\x80\x9C" "quoted" "\xE2\x80\x9D\"", "smart quote");
   EXPECT_NO_LINT_MESSAGE("# \xE2\x80\x9C" "comment" "\xE2\x80\x9D", "smart quote");

   EXPECT_LINT_MESSAGE("x %in y", "unterminated '%' operator");
   EXPECT_LINT_MESSAGE("x <- r\"(abc)", "unterminated raw string");
   EXPECT_LINT_MESSAGE("x <- 1 \xC2\xA7 2", "unexpected character '\xC2\xA7'");
   EXPECT_NO_LINT_MESSAGE("x %in% y", "unterminated");
   EXPECT_NO_LINT_MESSAGE("x <- r\"(abc)\"", "unterminated");
}

TEST(DiagnosticsTest, AssignmentToLiteralInConditional) {
   EXPECT_LINT_MESSAGE("if (x<-1) 2", "did you mean to use '< -'");
   EXPECT_LINT_MESSAGE("while (n<-10) n", "did you mean to use '< -'");
   EXPECT_LINT_MESSAGE("if (x<-1.5) 2", "did you mean to use '< -'");
   EXPECT_LINT_MESSAGE("if (y && x<-1) 2", "did you mean to use '< -'");

   // spaced out, non-numeric, or outside a condition: legitimate assignments
   EXPECT_NO_LINT_MESSAGE("if (x <- 1) 2", "did you mean to use '< -'");
   EXPECT_NO_LINT_MESSAGE("if (x<-foo()) 2", "did you mean to use '< -'");
   EXPECT_NO_LINT_MESSAGE("if (x < -1) 2", "did you mean to use '< -'");
   EXPECT_NO_LINT_MESSAGE("x<-1", "did you mean to use '< -'");
   EXPECT_NO_LINT_MESSAGE("if (any(x<-1)) 2", "did you mean to use '< -'");
   EXPECT_NO_LINT_MESSAGE("for (i in x<-1) 2", "did you mean to use '< -'");
}

TEST(DiagnosticsTest, TooManyArgumentsToZeroArgumentFunction) {
   EXPECT_LINT_MESSAGE("test0 <- function() {}\ntest0(1)", "too many arguments in call to 'test0'");
   EXPECT_LINT_MESSAGE("test0 <- function() {}\ntest0(1, 2)", "too many arguments in call to 'test0'");
   EXPECT_LINT_MESSAGE("Sys.time(1)", "too many arguments in call to 'Sys.time'");
   EXPECT_NO_LINT_MESSAGE("test0 <- function() {}\ntest0()", "too many arguments");
   EXPECT_NO_LINT_MESSAGE("Sys.time()", "too many arguments");

   // primitives without formals (language constructs) are left alone
   EXPECT_NO_ERRORS("f <- function(x) { return(x) }");
   EXPECT_NO_ERRORS("f <- function(x) { on.exit(x) }");

   // '.Internal()' calls target the internal entry point, not the wrapper
   EXPECT_NO_ERRORS("grepl <- function(pattern, x) .Internal(grepl(pattern, x, FALSE, FALSE))");
}

TEST(DiagnosticsTest, EmptyTrailingArguments) {
   EXPECT_LINT_MESSAGE("c(1,)", "empty trailing argument in call to 'c'");
   EXPECT_LINT_MESSAGE("c(\n  1,\n  2,\n)", "empty trailing argument in call to 'c'");
   EXPECT_LINT_MESSAGE("list(a = 1, )", "empty trailing argument in call to 'list'");
   EXPECT_LINT_MESSAGE("sum(1, )", "empty trailing argument in call to 'sum'");
   EXPECT_LINT_MESSAGE("paste('a', )", "empty trailing argument in call to 'paste'");
   EXPECT_LINT_MESSAGE("data.frame(a = 1, )", "empty trailing argument in call to 'data.frame'");
   EXPECT_LINT_MESSAGE("length(x, )", "empty trailing argument in call to 'length'");

   // functions that tolerate a trailing empty argument
   EXPECT_NO_LINT_MESSAGE("c(1, 2)", "empty trailing argument");
   EXPECT_NO_LINT_MESSAGE("switch(x, a = 1, )", "empty trailing argument");
   EXPECT_NO_LINT_MESSAGE("on.exit(NULL, )", "empty trailing argument");
   EXPECT_NO_LINT_MESSAGE("mean(x, )", "empty trailing argument");
   EXPECT_NO_LINT_MESSAGE("x[1, ]", "empty trailing argument");
   EXPECT_NO_LINT_MESSAGE("f <- function(...) list2(...); f(1, )", "empty trailing argument");
}

TEST(DiagnosticsTest, PackageNotInstalled) {
   EXPECT_LINT_MESSAGE("library(rstudioNoSuchPackage)", "package 'rstudioNoSuchPackage' is not installed");
   EXPECT_LINT_MESSAGE("library('rstudioNoSuchPackage')", "package 'rstudioNoSuchPackage' is not installed");
   EXPECT_LINT_MESSAGE("require(rstudioNoSuchPackage)", "package 'rstudioNoSuchPackage' is not installed");
   EXPECT_LINT_MESSAGE("suppressPackageStartupMessages(library(rstudioNoSuchPackage))", "is not installed");

   // availability checks and string-taking loaders are left alone
   EXPECT_NO_LINT_MESSAGE("requireNamespace(\"rstudioNoSuchPackage\", quietly = TRUE)", "is not installed");
   EXPECT_NO_LINT_MESSAGE("loadNamespace(pkg)", "is not installed");
   EXPECT_NO_LINT_MESSAGE("library(stats)", "is not installed");
   EXPECT_NO_LINT_MESSAGE("library(\"utils\")", "is not installed");
   EXPECT_NO_LINT_MESSAGE("pkg <- 'rstudioNoSuchPackage'; library(pkg, character.only = TRUE)", "is not installed");
   EXPECT_NO_LINT_MESSAGE("library(rstudioNoSuchPackage, character.only = TRUE)", "is not installed");
   EXPECT_NO_LINT_MESSAGE("library()", "is not installed");
   EXPECT_NO_LINT_MESSAGE("x$library(rstudioNoSuchPackage)", "is not installed");

   // 'require()' is an availability check whenever its result is used
   EXPECT_NO_LINT_MESSAGE("if (!require(rstudioNoSuchPackage)) install.packages('rstudioNoSuchPackage')", "is not installed");
   EXPECT_NO_LINT_MESSAGE("if (require(rstudioNoSuchPackage)) 1 else 2", "is not installed");
   EXPECT_NO_LINT_MESSAGE("ok <- require(rstudioNoSuchPackage)", "is not installed");
   EXPECT_NO_LINT_MESSAGE("require(rstudioNoSuchPackage) || stop('unavailable')", "is not installed");
   EXPECT_NO_LINT_MESSAGE("stopifnot(require(rstudioNoSuchPackage))", "is not installed");
   EXPECT_LINT_MESSAGE("f <- function() {\n  require(rstudioNoSuchPackage)\n  1\n}", "is not installed");
   EXPECT_LINT_MESSAGE("if (x) library(rstudioNoSuchPackage)", "is not installed");

   // the package may live in a library that isn't on the library paths
   EXPECT_NO_LINT_MESSAGE("library(rstudioNoSuchPackage, lib.loc = '~/mylib')", "is not installed");
   EXPECT_NO_LINT_MESSAGE("require(rstudioNoSuchPackage, lib.loc = lib)", "is not installed");
}

TEST(DiagnosticsTest, RStudioFilesCanBeSuccessfullyLinted) {
   lintRStudioRFiles();
}

} // namespace diagnostics
} // namespace modules 
} // namespace session
} // namespace rstudio
