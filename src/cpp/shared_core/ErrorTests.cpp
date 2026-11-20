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
#include <type_traits>
#include <utility>

#ifdef _WIN32
#include <windows.h>
#endif

#include <shared_core/Error.hpp>

namespace rstudio {
namespace core {
namespace error_tests {

// An unscoped error code enum with its own category, like json::errc. Being unscoped, it would also
// promote to int.
enum test_errc
{
   TestFailure = 7
};

class TestCategory : public boost::system::error_category
{
public:
   const char* name() const noexcept override
   {
      return "rstudio-test";
   }

   std::string message(int) const override
   {
      return "test failure";
   }
};

inline const boost::system::error_category& testCategory()
{
   static TestCategory category;
   return category;
}

inline boost::system::error_code make_error_code(test_errc e)
{
   // an int, so that this doesn't ask whether test_errc is registered before it is
   return boost::system::error_code(static_cast<int>(e), testCategory());
}

enum unregistered_errc
{
   UnregisteredFailure = 2
};

} // namespace error_tests
} // namespace core
} // namespace rstudio

namespace RSTUDIO_BOOST_NAMESPACE {
namespace system {

template <>
struct is_error_code_enum<rstudio::core::error_tests::test_errc> : std::true_type
{
};

} // namespace system
} // namespace RSTUDIO_BOOST_NAMESPACE

namespace rstudio {
namespace core {
namespace {

using namespace error_tests;
using boost::system::errc::errc_t;

// Whether systemError(T, location) and systemError(T, description, location) compile. A deleted or
// ambiguous overload is a substitution failure here.
template <typename T, typename = void>
struct CanBuildSystemError : std::false_type
{
};

template <typename T>
struct CanBuildSystemError<
   T,
   decltype(void(systemError(std::declval<T>(), std::declval<const ErrorLocation&>())),
            void(systemError(std::declval<T>(), std::declval<std::string>(), std::declval<const ErrorLocation&>())))>
   : std::true_type
{
};

template <typename T, typename = void>
struct CanBuildGenericError : std::false_type
{
};

template <typename T>
struct CanBuildGenericError<T, decltype(void(genericError(std::declval<T>(), std::declval<const ErrorLocation&>())))>
   : std::true_type
{
};

// portable codes, registered enums and categorized codes are accepted everywhere
static_assert(CanBuildSystemError<errc_t>::value, "errc");
static_assert(CanBuildSystemError<test_errc>::value, "registered enum");
static_assert(CanBuildSystemError<std::error_code>::value, "std::error_code");

// codes whose category systemError() can't know
static_assert(!CanBuildSystemError<unregistered_errc>::value, "unregistered enum");
static_assert(!CanBuildSystemError<unsigned int>::value, "unsigned int");
static_assert(!CanBuildSystemError<short>::value, "short");
static_assert(!CanBuildSystemError<long long>::value, "long long");

#ifdef _WIN32
// an int may hold an errno value or a Win32 code
static_assert(!CanBuildSystemError<int>::value, "int on Windows");
static_assert(CanBuildSystemError<DWORD>::value, "DWORD");
static_assert(CanBuildSystemError<LONG>::value, "LONG");
static_assert(CanBuildSystemError<decltype(ERROR_ACCESS_DENIED)>::value, "ERROR_* constant");
#else
static_assert(CanBuildSystemError<int>::value, "int on POSIX");
static_assert(!CanBuildSystemError<long>::value, "long on POSIX");
static_assert(!CanBuildSystemError<unsigned long>::value, "unsigned long on POSIX");
#endif

// genericError() takes errno, which is an int
static_assert(CanBuildGenericError<int>::value, "int");
static_assert(!CanBuildGenericError<unsigned long>::value, "unsigned long, e.g. a DWORD");
static_assert(!CanBuildGenericError<long>::value, "long, e.g. a LONG");
static_assert(!CanBuildGenericError<errc_t>::value, "errc");

const std::string kGeneric = boost::system::generic_category().name();
const std::string kSystem = boost::system::system_category().name();

} // anonymous namespace

TEST(ErrorTest, ErrcErrorsUseTheGenericCategory)
{
   using namespace boost::system;

   const Error cause = systemError(errc::permission_denied, ERROR_LOCATION);
   const Error errors[] = {
      systemError(errc::is_a_directory, ERROR_LOCATION),
      systemError(errc::is_a_directory, cause, ERROR_LOCATION),
      systemError(errc::is_a_directory, "loading history", ERROR_LOCATION),
      systemError(errc::is_a_directory, "loading history", cause, ERROR_LOCATION),
   };

   for (const Error& error : errors)
   {
      EXPECT_EQ(kGeneric, error.getName());
      EXPECT_EQ(errc::is_a_directory, error.getCode());
      EXPECT_EQ(generic_category().message(errc::is_a_directory), error.getMessage());
   }

   EXPECT_EQ("loading history", errors[2].getProperty("description"));
   EXPECT_EQ(cause, errors[1].getCause());
   EXPECT_EQ(cause, errors[3].getCause());
}

TEST(ErrorTest, GenericErrorsUseTheGenericCategory)
{
   using namespace boost::system;

   Error error = genericError(EINVAL, ERROR_LOCATION);
   EXPECT_EQ(kGeneric, error.getName());
   EXPECT_EQ(EINVAL, error.getCode());
   EXPECT_EQ(generic_category().message(EINVAL), error.getMessage());

   Error described = genericError(EINVAL, "parsing options", ERROR_LOCATION);
   EXPECT_EQ(error, described);
   EXPECT_EQ("parsing options", described.getProperty("description"));

   // the same error, whichever way it's spelled
   EXPECT_EQ(error, systemError(errc::invalid_argument, ERROR_LOCATION));
   EXPECT_EQ(error, systemError(std::make_error_code(std::errc::invalid_argument), ERROR_LOCATION));
}

TEST(ErrorTest, ErrorCodeEnumsUseTheirOwnCategory)
{
   const Error cause = systemError(boost::system::errc::io_error, ERROR_LOCATION);
   const Error errors[] = {
      systemError(TestFailure, ERROR_LOCATION),
      systemError(TestFailure, cause, ERROR_LOCATION),
      systemError(TestFailure, "running the test", ERROR_LOCATION),
      systemError(TestFailure, "running the test", cause, ERROR_LOCATION),
   };

   for (const Error& error : errors)
   {
      EXPECT_EQ("rstudio-test", error.getName());
      EXPECT_EQ(TestFailure, error.getCode());
      EXPECT_EQ("test failure", error.getMessage());
   }

   EXPECT_EQ("running the test", errors[2].getProperty("description"));
   EXPECT_EQ(cause, errors[3].getCause());
}

#ifndef _WIN32

TEST(ErrorTest, ErrnoValuesUseTheGenericCategoryOnPosix)
{
   const int savedErrno = ENOENT;
   Error error = systemError(savedErrno, ERROR_LOCATION);

   EXPECT_EQ(kGeneric, error.getName());
   EXPECT_EQ(genericError(ENOENT, ERROR_LOCATION), error);
   EXPECT_EQ(systemError(boost::system::errc::no_such_file_or_directory, ERROR_LOCATION), error);

   Error callError = systemCallError("open", ENOENT, ERROR_LOCATION);
   EXPECT_EQ(kGeneric, callError.getName());
   EXPECT_EQ("open: " + boost::system::generic_category().message(ENOENT), callError.getMessage());
}

TEST(ErrorTest, LastSystemErrorReadsErrnoOnPosix)
{
   errno = EACCES;
   Error error = LAST_SYSTEM_ERROR();

   EXPECT_EQ(kGeneric, error.getName());
   EXPECT_EQ(EACCES, error.getCode());
}

#else

TEST(ErrorTest, Win32CodesUseTheSystemCategory)
{
   const DWORD lastError = ERROR_ACCESS_DENIED;
   const LONG regResult = ERROR_FILE_NOT_FOUND;

   Error fromDword = systemError(lastError, ERROR_LOCATION);
   EXPECT_EQ(kSystem, fromDword.getName());
   EXPECT_EQ(ERROR_ACCESS_DENIED, fromDword.getCode());
   EXPECT_EQ(boost::system::system_category().message(ERROR_ACCESS_DENIED), fromDword.getMessage());

   Error fromLong = systemError(regResult, "opening a registry key", ERROR_LOCATION);
   EXPECT_EQ(kSystem, fromLong.getName());
   EXPECT_EQ(ERROR_FILE_NOT_FOUND, fromLong.getCode());
   EXPECT_EQ("opening a registry key", fromLong.getProperty("description"));

   EXPECT_EQ(fromDword, systemError(ERROR_ACCESS_DENIED, ERROR_LOCATION));
}

TEST(ErrorTest, LastSystemErrorReadsGetLastErrorOnWindows)
{
   ::SetLastError(ERROR_SHARING_VIOLATION);
   Error error = LAST_SYSTEM_ERROR();

   EXPECT_EQ(kSystem, error.getName());
   EXPECT_EQ(ERROR_SHARING_VIOLATION, error.getCode());
}

#endif

TEST(ErrorTest, MatchesTestsForAPortableCondition)
{
   using namespace boost::system;

   EXPECT_TRUE(systemError(errc::no_such_file_or_directory, ERROR_LOCATION).matches(errc::no_such_file_or_directory));
   EXPECT_FALSE(systemError(errc::no_such_file_or_directory, ERROR_LOCATION).matches(errc::permission_denied));

   EXPECT_TRUE(Success().matches(errc::success));
   EXPECT_FALSE(Success().matches(errc::no_such_file_or_directory));

   // another category with the same number isn't the condition
   Error other = systemError(TestFailure, ERROR_LOCATION);
   EXPECT_FALSE(other.matches(static_cast<errc_t>(TestFailure)));

#ifndef _WIN32
   // Boost reports errno values in the system category; that's a different error, but the same condition
   Error native(error_code(ENOENT, system_category()), ERROR_LOCATION);
   EXPECT_EQ(kSystem, native.getName());
   EXPECT_NE(native, systemError(errc::no_such_file_or_directory, ERROR_LOCATION));
   EXPECT_TRUE(native.matches(errc::no_such_file_or_directory));
#else
   // both Win32 not-found codes are the portable condition, while staying distinct errors
   Error fileNotFound = systemError(ERROR_FILE_NOT_FOUND, ERROR_LOCATION);
   Error pathNotFound = systemError(ERROR_PATH_NOT_FOUND, ERROR_LOCATION);
   EXPECT_TRUE(fileNotFound.matches(errc::no_such_file_or_directory));
   EXPECT_TRUE(pathNotFound.matches(errc::no_such_file_or_directory));
   EXPECT_NE(fileNotFound, pathNotFound);

   // ERROR_NOT_READY shares is_a_directory's number (21), not its meaning
   EXPECT_FALSE(systemError(ERROR_NOT_READY, ERROR_LOCATION).matches(errc::is_a_directory));
#endif
}

} // namespace core
} // namespace rstudio
