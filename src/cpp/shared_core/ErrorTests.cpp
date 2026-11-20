/*
 * ErrorTests.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant to the terms of a commercial license agreement
 * with Posit, then this program is licensed to you under the following terms:
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

#include <gtest/gtest.h>

#include <cerrno>
#include <string>

#include <boost/system/error_code.hpp>

#include <shared_core/Error.hpp>

#ifdef _WIN32
#include <windows.h>
#endif

namespace rstudio {
namespace core {
namespace {

using boost::system::errc::errc_t;

void expectGenericError(const Error& in_error, errc_t in_errc)
{
   EXPECT_EQ(std::string(boost::system::generic_category().name()), in_error.getName());
   EXPECT_EQ(static_cast<int>(in_errc), in_error.getCode());
   EXPECT_EQ(boost::system::generic_category().message(in_errc), in_error.getMessage());
}

} // anonymous namespace

TEST(ErrorTest, ErrcErrorsUseTheGenericCategory)
{
   // 21 is EISDIR as an errno value, but ERROR_NOT_READY as a Win32 code
   errc_t errc = boost::system::errc::is_a_directory;
   Error cause = systemError(boost::system::errc::io_error, ErrorLocation());

   expectGenericError(systemError(errc, ErrorLocation()), errc);
   expectGenericError(systemError(errc, cause, ErrorLocation()), errc);
   expectGenericError(systemError(errc, "description", ErrorLocation()), errc);
   expectGenericError(systemError(errc, "description", cause, ErrorLocation()), errc);

   Error described = systemError(errc, "description", cause, ErrorLocation());
   EXPECT_EQ("description", described.getProperty("description"));
   EXPECT_EQ(cause, described.getCause());
}

TEST(ErrorTest, ErrcErrorsMatchTheSameErrcBuiltAnyWay)
{
   errc_t errc = boost::system::errc::no_such_file_or_directory;
   Error error = systemError(errc, ErrorLocation());

   EXPECT_EQ(Error(errc, ErrorLocation()), error);
   EXPECT_EQ(systemError(std::make_error_code(std::errc::no_such_file_or_directory), ErrorLocation()), error);
   EXPECT_TRUE(error == boost::system::error_code(errc, boost::system::generic_category()));
}

TEST(ErrorTest, ErrcErrorsMatchEquivalentSystemErrors)
{
   errc_t errc = boost::system::errc::no_such_file_or_directory;
   Error error = systemError(errc, ErrorLocation());

#ifdef _WIN32
   // Windows reports a missing file and a missing folder with different codes
   // for the same condition
   Error fileNotFound = systemError(ERROR_FILE_NOT_FOUND, ErrorLocation());
   Error pathNotFound = systemError(ERROR_PATH_NOT_FOUND, ErrorLocation());
   EXPECT_EQ(pathNotFound, error);
   EXPECT_EQ(error, pathNotFound);
   EXPECT_TRUE(error == boost::system::error_code(ERROR_PATH_NOT_FOUND, boost::system::system_category()));

   // ... but system codes match only themselves
   EXPECT_NE(fileNotFound, pathNotFound);
#else
   Error fileNotFound = systemError(ENOENT, ErrorLocation());
#endif

   EXPECT_EQ(fileNotFound, error);
   EXPECT_EQ(error, fileNotFound);
   EXPECT_TRUE(fileNotFound == boost::system::error_code(errc, boost::system::generic_category()));
   EXPECT_TRUE(error == boost::system::error_code(fileNotFound.getCode(), boost::system::system_category()));
}

TEST(ErrorTest, ErrcErrorsDoNotMatchUnrelatedErrors)
{
   errc_t errc = boost::system::errc::is_a_directory;
   Error error = systemError(errc, ErrorLocation());

#ifdef _WIN32
   // the same number in the system category is ERROR_NOT_READY
   Error sameNumber = systemError(static_cast<int>(errc), ErrorLocation());
   EXPECT_NE(sameNumber, error);
   EXPECT_NE(error, sameNumber);
   EXPECT_FALSE(error == boost::system::error_code(errc, boost::system::system_category()));
#endif

   EXPECT_NE(systemError(boost::system::errc::not_a_directory, ErrorLocation()), error);
   EXPECT_NE(Error("OtherCategory", static_cast<int>(errc), ErrorLocation()), error);
   EXPECT_NE(error, Error("OtherCategory", static_cast<int>(errc), ErrorLocation()));
   EXPECT_NE(Success(), error);
   EXPECT_NE(error, Success());
}

} // namespace core
} // namespace rstudio
