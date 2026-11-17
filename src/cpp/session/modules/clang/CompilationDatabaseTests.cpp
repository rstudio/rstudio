/*
 * CompilationDatabaseTests.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

#include <boost/scope_exit.hpp>

#include <shared_core/FilePath.hpp>
#include <shared_core/Hash.hpp>
#include <core/Algorithm.hpp>
#include <core/FileSerializer.hpp>
#include <core/libclang/LibClang.hpp>

#include "CompilationDatabase.hpp"

namespace rstudio {
namespace session {
namespace modules {
namespace clang {
namespace {

using namespace core;

class CompileFlagsTest : public ::testing::Test
{
protected:
   void SetUp() override
   {
      // <root>/pkg/src, with 'pkg' standing in for the project directory
      FilePath::tempFilePath(root_);
      pkg_ = root_.completeChildPath("pkg");
      src_ = pkg_.completeChildPath("src");
      src_.ensureDirectory();
   }

   void TearDown() override
   {
      root_.removeIfExists();
   }

   FilePath flagsFile(const FilePath& dir, const std::string& contents = "-DFOO\n")
   {
      FilePath file = dir.completeChildPath("compile_flags.txt");
      writeStringToFile(file, contents);
      return file;
   }

   FilePath root_;
   FilePath pkg_;
   FilePath src_;
};

// The entries filterCompileFlags() rejects, as they're reported
std::vector<std::string> rejectedFlags(const std::vector<std::string>& flags)
{
   std::vector<RejectedCompileFlag> rejected;
   filterCompileFlags(flags, &rejected);

   std::vector<std::string> result;
   for (const RejectedCompileFlag& entry : rejected)
      result.push_back(entry.flag);
   return result;
}

TEST_F(CompileFlagsTest, FindsFileInSourceDirectory)
{
   FilePath file = flagsFile(src_);
   EXPECT_EQ(findCompileFlagsFile(src_, src_), file);
   EXPECT_EQ(findCompileFlagsFile(src_, pkg_), file);
}

TEST_F(CompileFlagsTest, FindsFileInParentUpToStopDirectory)
{
   FilePath file = flagsFile(pkg_);
   EXPECT_EQ(findCompileFlagsFile(src_, pkg_), file);
}

TEST_F(CompileFlagsTest, NearestFileWins)
{
   flagsFile(pkg_);
   FilePath nearest = flagsFile(src_);
   EXPECT_EQ(findCompileFlagsFile(src_, pkg_), nearest);
}

TEST_F(CompileFlagsTest, DoesNotLookAboveStopDirectory)
{
   flagsFile(root_);
   EXPECT_TRUE(findCompileFlagsFile(src_, pkg_).isEmpty());

   // and not beyond the source directory when that is the stop directory
   flagsFile(pkg_);
   EXPECT_TRUE(findCompileFlagsFile(src_, src_).isEmpty());
}

TEST_F(CompileFlagsTest, StopDirectoryMayHaveTrailingSeparator)
{
   flagsFile(root_);
   FilePath stopDir(pkg_.getAbsolutePath() + "/");
   EXPECT_TRUE(findCompileFlagsFile(src_, stopDir).isEmpty());
}

TEST_F(CompileFlagsTest, ReturnsEmptyWhenThereIsNoFile)
{
   EXPECT_TRUE(findCompileFlagsFile(src_, pkg_).isEmpty());
}

TEST_F(CompileFlagsTest, ReadsOneFlagPerLine)
{
   FilePath file = flagsFile(src_, "\r\n-ferror-limit=5\r\n\r\n  -Wall  \r\n-DFOO=1");
   std::vector<std::string> expected = {"-ferror-limit=5", "-Wall", "-DFOO=1"};
   EXPECT_EQ(readCompileFlags(file), expected);
}

TEST_F(CompileFlagsTest, ReadsNothingFromBlankFile)
{
   EXPECT_TRUE(readCompileFlags(flagsFile(src_, "")).empty());
   EXPECT_TRUE(readCompileFlags(flagsFile(src_, "\n  \n\r\n")).empty());
}

TEST_F(CompileFlagsTest, ResolvesRelativePathsAgainstDirectory)
{
   std::string inc = pkg_.completePath("inc").getAbsolutePath();
   std::string header = pkg_.completePath("../pre.h").getAbsolutePath();

   // joined and separate operands both come out joined
   std::vector<std::string> flags = {"-Iinc", "-I", "inc", "-isystem", "inc", "-include", "../pre.h"};
   std::vector<std::string> expected = {"-I" + inc, "-I" + inc, "-isystem" + inc, "-include" + header};
   EXPECT_EQ(resolveCompileFlagPaths(flags, pkg_), expected);
}

TEST_F(CompileFlagsTest, ResolvesLongOptionSpellings)
{
   std::string inc = pkg_.completePath("inc").getAbsolutePath();
   std::string sys = pkg_.completePath("sys").getAbsolutePath();

   // '-isystem-after' is its own option, not '-isystem' with a '-after...' operand
   std::vector<std::string> flags = {"-isystem-afterinc", "-isystem-after", "inc"};
   std::vector<std::string> expected = {"-isystem-after" + inc, "-isystem-after" + inc};
   EXPECT_EQ(resolveCompileFlagPaths(flags, pkg_), expected);

   // the long include spellings, with or without the '=', come out as the
   // short options they alias (the form header completion reads)
   flags = {"--include-directory=inc", "--include-directory", "inc", "--include-directory-after=inc"};
   expected = {"-I" + inc, "-I" + inc, "-idirafter" + inc};
   EXPECT_EQ(resolveCompileFlagPaths(flags, pkg_), expected);

   // other '=' options keep their spelling
   flags = {"--sysroot=sys", "--sysroot", "sys"};
   expected = {"--sysroot=" + sys, "--sysroot=" + sys};
   EXPECT_EQ(resolveCompileFlagPaths(flags, pkg_), expected);

   // something that merely starts like an '=' option is not one
   flags = {"--sysrootx"};
   EXPECT_EQ(resolveCompileFlagPaths(flags, pkg_), flags);
}

TEST_F(CompileFlagsTest, ResolvesLessCommonPathOptions)
{
   std::string inc = pkg_.completePath("inc").getAbsolutePath();
   std::string file = pkg_.completePath("x.yaml").getAbsolutePath();

   std::vector<std::string> flags = {
      "-iframeworkinc", "-cxx-isystem", "inc", "-ivfsoverlay", "x.yaml",
      "-fmodule-map-file=x.yaml", "--gcc-toolchain=inc"
   };
   std::vector<std::string> expected = {
      "-iframework" + inc, "-cxx-isystem" + inc, "-ivfsoverlay" + file,
      "-fmodule-map-file=" + file, "--gcc-toolchain=" + inc
   };
   EXPECT_EQ(resolveCompileFlagPaths(flags, pkg_), expected);
}

TEST_F(CompileFlagsTest, LeavesAbsolutePathsAndOtherFlagsAlone)
{
   std::string abs = root_.getAbsolutePath();
   std::vector<std::string> flags = {"-DFOO", "-I" + abs, "-ferror-limit=5", "-fPIC", "-Wall"};
   EXPECT_EQ(resolveCompileFlagPaths(flags, pkg_), flags);
}

TEST_F(CompileFlagsTest, LeavesUnsupportedAndDanglingOptionsAlone)
{
   // '-include-pch' is not '-include' with a '-pch' path, and an option
   // whose next argument looks like an option doesn't take it as its path
   std::vector<std::string> flags = {"-include-pch", "x.pch", "-I", "-DFOO", "-isystem"};
   EXPECT_EQ(resolveCompileFlagPaths(flags, pkg_), flags);
}

TEST_F(CompileFlagsTest, AllowsFlagsThatShapeTheParse)
{
   std::vector<std::string> flags = {
      "-DFOO", "-D", "BAR=1", "-UNDEBUG", "-Iinc", "-I", "inc", "-isystem-afterinc",
      "-iquote", "inc", "--include-directory=inc", "--sysroot", "sys", "-include", "pre.h",
      "-fmodule-map-file=x.modulemap", "--gcc-toolchain=gcc",
      "-std=c++20", "-stdlib=libc++", "-xc++", "-x", "c++-header",
      "--target=x86_64-pc-linux-gnu", "-target", "aarch64-linux-gnu", "-arch", "arm64",
      "-march=haswell", "-mavx2", "-mno-sse4.2", "-msse4", "-mno-sse4", "-m64",
      "-W", "-Wall", "-Wno-unused-parameter", "-Werror=format", "-Wframe-larger-than=4096", "-w",
      "-pedantic", "-ferror-limit=50", "-fopenmp", "-fopenmp=libomp", "-fno-exceptions",
      "-fsanitize=address,undefined", "-fno-sanitize=alignment",
      "-fPIC", "-fvisibility=hidden", "-O2", "-g", "-pthread",
   };

   std::vector<RejectedCompileFlag> rejected;
   EXPECT_EQ(filterCompileFlags(flags, &rejected), flags);
   EXPECT_TRUE(rejected.empty());
}

TEST_F(CompileFlagsTest, RejectsNegatedFeaturesThatArentListed)
{
   // '-fno-' and '-mno-' only allow the names their positive forms do
   std::vector<std::string> flags = {"-fno-foo", "-mno-foo", "-ffoo", "-mfoo"};
   EXPECT_EQ(rejectedFlags(flags), flags);
}

TEST_F(CompileFlagsTest, RejectsFlagsThatReachBeyondTheParse)
{
   // plugins and arguments passed on to clang's internals, LLVM, or other
   // tools; files to read further flags from, or to write; and an
   // option's value goes with it into the report
   std::vector<std::string> flags = {
      "-Xclang", "-load", "-Xclang", "plugin.so", "-fplugin=plugin.so", "-fpass-plugin=plugin.so",
      "-mllvm", "-debug", "-mllvm=-debug", "@more-flags.txt", "--config=x.cfg",
      "-Wp,-MD,deps.d", "-Wl,-rpath,lib", "-o", "out.o", "-MD", "-MF", "deps.d",
      "-MJ", "db.json", "-module-dependency-dir", "deps", "-fmodules-cache-path=cache",
      "-ftime-trace", "-include-pch", "x.pch", "-iframeworkwithsysroot", "fw",
      "-x", "ast", "-xast", "-fmodule-map-file", "x.modulemap", "--gcc-toolchain", "gcc",
      "other.cpp",
   };
   std::vector<std::string> expected = {
      "-Xclang", "-load", "-Xclang plugin.so", "-fplugin=plugin.so", "-fpass-plugin=plugin.so",
      "-mllvm", "-debug", "-mllvm=-debug", "@more-flags.txt", "--config=x.cfg",
      "-Wp,-MD,deps.d", "-Wl,-rpath,lib", "-o out.o", "-MD", "-MF deps.d",
      "-MJ db.json", "-module-dependency-dir deps", "-fmodules-cache-path=cache",
      "-ftime-trace", "-include-pch x.pch", "-iframeworkwithsysroot fw",
      "-x ast", "-xast", "-fmodule-map-file x.modulemap", "--gcc-toolchain gcc",
      "other.cpp",
   };

   EXPECT_TRUE(filterCompileFlags(flags).empty());
   EXPECT_EQ(rejectedFlags(flags), expected);
}

TEST_F(CompileFlagsTest, ReportsValueOnlyWithAnOptionThatTakesOne)
{
   // neither takes the argument after it, which is reported on its own
   std::vector<std::string> flags = {"-ftime-trace", "other.cpp", "-fplugin=x.so", "src/extra.c"};
   EXPECT_EQ(rejectedFlags(flags), flags);

   std::vector<RejectedCompileFlag> rejected;
   filterCompileFlags(flags, &rejected);
   ASSERT_EQ(rejected.size(), 4u);
   EXPECT_EQ(rejected[0].reason, "only flags that affect how files are parsed are supported");
   EXPECT_EQ(rejected[1].reason, "it is neither a flag nor the value of a supported option");
}

TEST_F(CompileFlagsTest, SaysWhyASupportedOptionWasRejected)
{
   std::vector<std::string> flags = {
      "-I", "-DFOO", "-x", "ast", "-std", "c++17", "-std=", "--gcc-toolchain", "gcc",
      "--sysroot=", "-I-", "-D",
   };

   std::vector<RejectedCompileFlag> rejected;
   EXPECT_EQ(filterCompileFlags(flags, &rejected), std::vector<std::string>{"-DFOO"});

   std::vector<std::pair<std::string, std::string>> actual;
   for (const RejectedCompileFlag& entry : rejected)
      actual.emplace_back(entry.flag, entry.reason);

   std::vector<std::pair<std::string, std::string>> expected = {
      {"-I", "'-I' needs a value; one on the next line can't begin with '-' or '@'"},
      {"-x ast", "'-x' doesn't support the value 'ast'"},
      {"-std c++17", "'-std' needs a value after an '=', as in '-std=<value>'"},
      {"-std=", "'-std' needs a value after an '=', as in '-std=<value>'"},
      {"--gcc-toolchain gcc", "'--gcc-toolchain' needs a value after an '=', as in '--gcc-toolchain=<value>'"},
      {"--sysroot=", "'--sysroot' needs a value after an '=', as in '--sysroot=<value>'"},
      {"-I-", "clang doesn't support '-I-'; use '-iquote' instead"},
      {"-D", "'-D' needs a value"},
   };
   EXPECT_EQ(actual, expected);
}

TEST_F(CompileFlagsTest, DoesNotResolveIDashAsAPath)
{
   std::vector<std::string> flags = {"-I-"};
   EXPECT_EQ(resolveCompileFlagPaths(flags, pkg_), flags);
}

TEST_F(CompileFlagsTest, KeepsAllowedFlagsAroundRejectedOnes)
{
   std::vector<std::string> flags = {"-DFOO", "-o", "out.o", "-Wall"};
   std::vector<std::string> expected = {"-DFOO", "-Wall"};
   EXPECT_EQ(filterCompileFlags(flags), expected);
   EXPECT_EQ(rejectedFlags(flags), std::vector<std::string>{"-o out.o"});
}

TEST_F(CompileFlagsTest, ValueInNextArgumentMustNotLookLikeAnOption)
{
   // clang would take '-fplugin=...' as the path, but we don't, so that
   // nothing we pass on is an option we haven't checked
   std::vector<std::string> flags = {"-I", "-fplugin=plugin.so"};
   EXPECT_TRUE(filterCompileFlags(flags).empty());
   EXPECT_EQ(rejectedFlags(flags), flags);

   // nor a response file, nor nothing at all
   flags = {"-include", "@more-flags.txt", "-D"};
   EXPECT_TRUE(filterCompileFlags(flags).empty());
   EXPECT_EQ(rejectedFlags(flags), flags);
}

TEST_F(CompileFlagsTest, LeavesSysrootRelativePathsAlone)
{
   std::vector<std::string> flags = {"-I=/usr/include/foo", "-isystem", "$SYSROOT/include", "-iquote=lib"};
   std::vector<std::string> expected = {"-I=/usr/include/foo", "-isystem$SYSROOT/include", "-iquote=lib"};
   EXPECT_EQ(resolveCompileFlagPaths(flags, pkg_), expected);
}

TEST_F(CompileFlagsTest, RemovesUserFlagsPchOnceItsFlagsFileIsGone)
{
   FilePath file = flagsFile(src_, "-DFOO\n");
   FilePath source = src_.completeChildPath("a.cpp");
   std::vector<std::string> flags = compileFlagsForSource(source);
   ASSERT_EQ(flags, std::vector<std::string>{"-DFOO"});

   // the PCH directory holds the default PCH and the variant for those flags
   FilePath platform = root_.completeChildPath("pch");
   platform.ensureDirectory();
   std::string variant = "Rcpp-std=gnu++17-flags-" + hash::crc32HexHash(algorithm::join(flags, "\n"));
   std::vector<std::string> names = {"Rcpp-std=gnu++17.pch", "Rcpp-std=gnu++17.cpp", variant + ".pch", variant + ".cpp"};
   for (const std::string& name : names)
      writeStringToFile(platform.completeChildPath(name), "");

   // the file is still there, so nothing happens
   removeOrphanedUserFlagsPchs(platform, "Rcpp");
   EXPECT_TRUE(platform.completeChildPath(variant + ".pch").exists());

   file.remove();
   removeOrphanedUserFlagsPchs(platform, "Rcpp");
   EXPECT_FALSE(platform.completeChildPath(variant + ".pch").exists());
   EXPECT_FALSE(platform.completeChildPath(variant + ".cpp").exists());
   EXPECT_TRUE(platform.completeChildPath("Rcpp-std=gnu++17.pch").exists());
   EXPECT_TRUE(platform.completeChildPath("Rcpp-std=gnu++17.cpp").exists());
}

TEST_F(CompileFlagsTest, RemovesUserFlagsPchOnceNoFlagIsAllowed)
{
   FilePath file = flagsFile(src_, "-DFOO\n");
   std::vector<std::string> flags = compileFlagsForSource(src_.completeChildPath("a.cpp"));
   ASSERT_EQ(flags, std::vector<std::string>{"-DFOO"});

   FilePath platform = root_.completeChildPath("pch");
   platform.ensureDirectory();
   std::string variant = "Rcpp-std=gnu++17-flags-" + hash::crc32HexHash(algorithm::join(flags, "\n"));
   FilePath pch = platform.completeChildPath(variant + ".pch");
   writeStringToFile(pch, "");

   // a file left with only flags we drop supplies no flags
   writeStringToFile(file, "-o\nout.o\n");
   removeOrphanedUserFlagsPchs(platform, "Rcpp");
   EXPECT_FALSE(pch.exists());
}

TEST_F(CompileFlagsTest, SystemIncludeArgsAreSearchedInReportedOrder)
{
   core::libclang::LibClang& lib = core::libclang::clang();
   if (!lib.isLoaded())
      GTEST_SKIP() << "libclang is not available";

   // 'std' stands in for the C++ standard library, whose <stddef.h> wraps
   // the one in 'builtin', the compiler's own headers
   FilePath stdDir = root_.completeChildPath("std");
   FilePath builtinDir = root_.completeChildPath("builtin");
   stdDir.ensureDirectory();
   builtinDir.ensureDirectory();
   writeStringToFile(stdDir.completeChildPath("stddef.h"), R"(
#define STD_STDDEF_SEEN 1
#include_next <stddef.h>
)");
   writeStringToFile(builtinDir.completeChildPath("stddef.h"), R"(
#define BUILTIN_STDDEF_SEEN 1
)");

   FilePath source = root_.completeChildPath("probe.c");
   writeStringToFile(source, R"(
#include <stddef.h>
#ifndef STD_STDDEF_SEEN
#error builtin stddef.h was found ahead of the standard library one
#endif
#ifndef BUILTIN_STDDEF_SEEN
#error builtin stddef.h was not found
#endif
)");

   // the trailing '-isystem' makes 'std' one of libclang's system
   // directories too, as the standard library's directory is in practice;
   // that is what gets a '-I' for the same directory dropped
   std::vector<std::string> args = {"-nostdinc"};
   std::vector<std::string> includeArgs = systemIncludeArgs({stdDir.getAbsolutePath(), builtinDir.getAbsolutePath()});
   args.insert(args.end(), includeArgs.begin(), includeArgs.end());
   args.push_back("-isystem" + stdDir.getAbsolutePath());

   std::vector<const char*> argv;
   for (const std::string& arg : args)
      argv.push_back(arg.c_str());

   CXIndex index = lib.createIndex(0, 0);
   CXTranslationUnit tu = lib.parseTranslationUnit(
      index,
      source.getAbsolutePath().c_str(),
      argv.data(),
      static_cast<int>(argv.size()),
      nullptr,
      0,
      CXTranslationUnit_None);

   // runs when an assertion returns early, too
   BOOST_SCOPE_EXIT(&lib, &index, &tu)
   {
      if (tu != nullptr)
         lib.disposeTranslationUnit(tu);

      lib.disposeIndex(index);
   }
   BOOST_SCOPE_EXIT_END

   ASSERT_NE(tu, nullptr);

   // list the diagnostics, so that a failure says which '#error' fired (or
   // what else this libclang had to report)
   std::string diagnostics;
   unsigned numDiagnostics = lib.getNumDiagnostics(tu);
   for (unsigned i = 0; i < numDiagnostics; i++)
      diagnostics += libclang::Diagnostic(lib.getDiagnostic(tu, i)).format() + "\n";

   EXPECT_EQ(numDiagnostics, 0u) << diagnostics;
}

} // anonymous namespace
} // namespace clang
} // namespace modules
} // namespace session
} // namespace rstudio
