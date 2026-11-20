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
#include <system_error>
#include <type_traits>
#include <utility>

#include <boost/system/error_code.hpp>

#include <shared_core/Error.hpp>

#ifdef _WIN32
#include <windows.h>
#endif

namespace rstudio {
namespace core {
namespace {

// an error code enum with its own category, like boost::asio::error::misc_errors; unscoped (as those and
// json::errc are), so it would also convert to int
enum TestErrc
{
   kTestFailure = 21
};

class TestCategory : public boost::system::error_category
{
public:
   const char* name() const noexcept override
   {
      return "test";
   }

   std::string message(int) const override
   {
      return "test failure";
   }
};

const boost::system::error_category& testCategory()
{
   static TestCategory category;
   return category;
}

boost::system::error_code make_error_code(TestErrc in_code)
{
   return boost::system::error_code(static_cast<int>(in_code), testCategory());
}

// an unscoped error code enum known only to std, so it would convert to int
enum StdOnlyErrc
{
   kStdOnlyFailure = 21
};

} // anonymous namespace
} // namespace core
} // namespace rstudio

namespace std {

template <>
struct is_error_code_enum<rstudio::core::StdOnlyErrc> : true_type
{
};

} // namespace std

namespace RSTUDIO_BOOST_NAMESPACE {
namespace system {

template <>
struct is_error_code_enum<rstudio::core::TestErrc>
{
   static const bool value = true;
};

} // namespace system
} // namespace RSTUDIO_BOOST_NAMESPACE

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

void expectTestError(const Error& in_error)
{
   EXPECT_EQ("test", in_error.getName());
   EXPECT_EQ(static_cast<int>(kTestFailure), in_error.getCode());
   EXPECT_EQ("test failure", in_error.getMessage());
}

// Check the public overloads without requiring an intentionally failing build.
template <typename E, typename... Args>
auto canMakeSystemError(int) -> decltype(systemError(
   std::declval<E>(), std::declval<Args>()..., std::declval<const ErrorLocation&>()), std::true_type{});

template <typename E, typename... Args>
std::false_type canMakeSystemError(...);

template <typename E, typename... Args>
using CanMakeSystemError = decltype(canMakeSystemError<E, Args...>(0));

template <typename E, typename... Args>
auto canMakeSystemCallError(int) -> decltype(systemCallError(
   std::declval<const std::string&>(), std::declval<E>(), std::declval<Args>()...,
   std::declval<const ErrorLocation&>()), std::true_type{});

template <typename E, typename... Args>
std::false_type canMakeSystemCallError(...);

template <typename E, typename... Args>
using CanMakeSystemCallError = decltype(canMakeSystemCallError<E, Args...>(0));

template <typename T>
constexpr bool rejectsRawCode()
{
   return !CanMakeSystemError<T>::value &&
          !CanMakeSystemError<T, Error>::value &&
          !CanMakeSystemError<T, std::string>::value &&
          !CanMakeSystemError<T, std::string, Error>::value &&
          !CanMakeSystemCallError<T>::value &&
          !CanMakeSystemCallError<T, std::string>::value;
}

enum PlainEnum
{
   kPlainValue = 21
};

static_assert(rejectsRawCode<int>(), "an errno value needs an explicit category");
static_assert(rejectsRawCode<unsigned long>(), "a raw Win32 code needs an explicit category");
static_assert(CanMakeSystemError<errc_t>::value, "an errc is a generic error");
static_assert(CanMakeSystemError<TestErrc>::value, "an error code enum has its own category");
static_assert(CanMakeSystemError<std::io_errc>::value, "a scoped std error code enum converts to std::error_code");
static_assert(!CanMakeSystemError<PlainEnum>::value, "a plain enum has no category");
static_assert(!CanMakeSystemError<StdOnlyErrc>::value, "an unscoped std error code enum needs std::error_code(value)");
static_assert(!CanMakeSystemError<std::errc>::value, "a std::errc needs std::make_error_code()");

// whether genericError(E, ErrorLocation) compiles
template <typename E, typename = void>
struct CanMakeGenericError : std::false_type
{
};

template <typename E>
struct CanMakeGenericError<E, decltype(void(genericError(std::declval<E>(), std::declval<const ErrorLocation&>())))>
   : std::true_type
{
};

// whether genericError(E, description, ErrorLocation) compiles
template <typename E, typename = void>
struct CanMakeDescribedGenericError : std::false_type
{
};

template <typename E>
struct CanMakeDescribedGenericError<
   E,
   decltype(void(genericError(
      std::declval<E>(),
      std::declval<const std::string&>(),
      std::declval<const ErrorLocation&>())))> : std::true_type
{
};

static_assert(CanMakeGenericError<int>::value, "an errno value is a generic error");
static_assert(!CanMakeGenericError<errc_t>::value, "an errc goes to systemError()");
static_assert(!CanMakeGenericError<PlainEnum>::value, "a plain enum isn't an errno value");
static_assert(CanMakeDescribedGenericError<int>::value, "an errno value is a generic error");
static_assert(!CanMakeDescribedGenericError<errc_t>::value, "an errc goes to systemError()");
static_assert(!CanMakeDescribedGenericError<PlainEnum>::value, "a plain enum isn't an errno value");

static_assert(CanMakeSystemCallError<boost::system::error_code>::value, "a named call accepts a categorized code");
static_assert(!CanMakeSystemCallError<errc_t>::value, "an errc isn't a system error code");
static_assert(!CanMakeSystemCallError<PlainEnum>::value, "a plain enum isn't a system error code");

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
   ASSERT_TRUE(described.hasCause());
   EXPECT_EQ(cause, described.getCause());
}

TEST(ErrorTest, CategorizedCodesKeepTheirCategory)
{
   const auto code = make_error_code(kTestFailure);
   const Error cause = systemError(boost::system::errc::io_error, ErrorLocation());
   expectTestError(systemError(code, ErrorLocation()));
   expectTestError(systemError(code, cause, ErrorLocation()));
   expectTestError(systemError(code, "details", ErrorLocation()));
   const Error described = systemError(code, "details", cause, ErrorLocation());
   expectTestError(described);
   EXPECT_EQ("details", described.getProperty("description"));
   ASSERT_TRUE(described.hasCause());
   EXPECT_EQ(cause, described.getCause());

   const Error named = systemCallError("read", code, ErrorLocation());
   EXPECT_TRUE(named == code);
   EXPECT_EQ("read: test failure", named.getMessage());
   const Error custom = systemCallError("read", code, "details", ErrorLocation());
   EXPECT_TRUE(custom == code);
   EXPECT_EQ("read: details", custom.getMessage());
}

TEST(ErrorTest, LastSystemErrorCapturesTheNativeCodeAndCallSite)
{
#ifdef _WIN32
   const int value = ERROR_ACCESS_DENIED;
   const auto previous = ::GetLastError();
   ::SetLastError(value);
   const Error error = LAST_SYSTEM_ERROR();
   ::SetLastError(value);
   const Error named = LAST_SYSTEM_CALL_ERROR("open");
   ::SetLastError(previous);
#else
   const int value = EACCES;
   const int previous = errno;
   errno = value;
   const Error error = LAST_SYSTEM_ERROR();
   errno = value;
   const Error named = LAST_SYSTEM_CALL_ERROR("open");
   errno = previous;
#endif

   const boost::system::error_code native(value, boost::system::system_category());
   EXPECT_TRUE(error == native);
   EXPECT_EQ(native.message(), error.getMessage());
   EXPECT_TRUE(named == native);
   EXPECT_EQ("open: " + native.message(), named.getMessage());
   EXPECT_EQ(BOOST_CURRENT_FUNCTION, error.getLocation().getFunction());
   EXPECT_EQ(BOOST_CURRENT_FUNCTION, named.getLocation().getFunction());
}

TEST(ErrorTest, GenericErrorsUseTheGenericCategory)
{
   // 21 is EISDIR as an errno value, but ERROR_NOT_READY as a Win32 code
   expectGenericError(genericError(EISDIR, ErrorLocation()), boost::system::errc::is_a_directory);

   Error described = genericError(EISDIR, "description", ErrorLocation());
   expectGenericError(described, boost::system::errc::is_a_directory);
   EXPECT_EQ("description", described.getProperty("description"));

   EXPECT_EQ(systemError(boost::system::errc::is_a_directory, ErrorLocation()), genericError(EISDIR, ErrorLocation()));
}

TEST(ErrorTest, ErrcErrorsMatchTheSameErrcBuiltAnyWay)
{
   errc_t errc = boost::system::errc::no_such_file_or_directory;
   Error error = systemError(errc, ErrorLocation());

   EXPECT_EQ(Error(errc, ErrorLocation()), error);
   EXPECT_EQ(systemError(std::make_error_code(std::errc::no_such_file_or_directory), ErrorLocation()), error);
   EXPECT_TRUE(error == boost::system::error_code(errc, boost::system::generic_category()));
}

TEST(ErrorTest, DistinctCodesCanMatchTheSameCondition)
{
   using boost::system::error_code;
   const errc_t missing = boost::system::errc::no_such_file_or_directory;
   Error generic = systemError(missing, ErrorLocation());

#ifdef _WIN32
   const error_code native(ERROR_FILE_NOT_FOUND, boost::system::system_category());
#else
   const error_code native(ENOENT, boost::system::system_category());
#endif
   Error file(native, ErrorLocation());
#ifdef _WIN32
   const error_code nativePath(ERROR_PATH_NOT_FOUND, boost::system::system_category());
   Error path(nativePath, ErrorLocation());
   EXPECT_TRUE(path.matches(missing));
   EXPECT_NE(path, generic);
   EXPECT_NE(path, file);
#endif
   EXPECT_TRUE(file.matches(missing));
   EXPECT_TRUE(generic.matches(missing));
   EXPECT_NE(file, generic);
   EXPECT_NE(generic, file);
   EXPECT_TRUE(file == native);
   EXPECT_FALSE(generic == native);
   EXPECT_FALSE(file == boost::system::errc::make_error_code(missing));
}

TEST(ErrorTest, ErrcErrorsDoNotMatchUnrelatedErrors)
{
   errc_t errc = boost::system::errc::is_a_directory;
   Error error = systemError(errc, ErrorLocation());

#ifdef _WIN32
   // the same number in the system category is ERROR_NOT_READY
   const boost::system::error_code native(ERROR_NOT_READY, boost::system::system_category());
   Error sameNumber(native, ErrorLocation());
   EXPECT_FALSE(sameNumber.matches(errc));
   EXPECT_NE(sameNumber, error);
   EXPECT_NE(error, sameNumber);
   EXPECT_FALSE(error == native);
#endif

   EXPECT_NE(systemError(boost::system::errc::not_a_directory, ErrorLocation()), error);
   EXPECT_NE(Error("OtherCategory", static_cast<int>(errc), ErrorLocation()), error);
   EXPECT_NE(error, Error("OtherCategory", static_cast<int>(errc), ErrorLocation()));
   EXPECT_NE(Success(), error);
   EXPECT_NE(error, Success());
   EXPECT_FALSE(error.matches(boost::system::errc::not_a_directory));
   EXPECT_FALSE(Error("OtherCategory", static_cast<int>(errc), ErrorLocation()).matches(errc));
   EXPECT_FALSE(Success().matches(errc));
   EXPECT_TRUE(Success().matches(boost::system::errc::success));
}

TEST(ErrorTest, ErrorCodeEnumsUseTheirOwnCategory)
{
   Error cause = systemError(boost::system::errc::io_error, ErrorLocation());

   expectTestError(systemError(kTestFailure, ErrorLocation()));
   expectTestError(systemError(kTestFailure, cause, ErrorLocation()));
   expectTestError(systemError(kTestFailure, "description", ErrorLocation()));
   expectTestError(systemError(kTestFailure, "description", cause, ErrorLocation()));

   Error described = systemError(kTestFailure, "description", cause, ErrorLocation());
   EXPECT_EQ("description", described.getProperty("description"));
   ASSERT_TRUE(described.hasCause());
   EXPECT_EQ(cause, described.getCause());

   // the category isn't system or generic, so the same number elsewhere doesn't match
   EXPECT_NE(systemError(boost::system::errc::is_a_directory, ErrorLocation()),
             systemError(kTestFailure, ErrorLocation()));
}

} // namespace core
} // namespace rstudio
