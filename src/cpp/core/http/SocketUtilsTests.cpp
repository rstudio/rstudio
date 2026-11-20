/*
 * SocketUtilsTests.cpp
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

#include <core/http/SocketUtils.hpp>

#include <gtest/gtest.h>

namespace rstudio {
namespace core {
namespace http {
namespace tests {

using boost::system::errc::errc_t;

namespace {

// the same condition, as asio reports it (native, system category) and as our
// own code builds it (generic category)
template <typename AsioCode>
void expectBothForms(bool (*classify)(const Error&), AsioCode asioCode, errc_t condition)
{
   EXPECT_TRUE(classify(Error(boost::asio::error::make_error_code(asioCode), ERROR_LOCATION)));
   EXPECT_TRUE(classify(systemError(condition, ERROR_LOCATION)));
}

} // anonymous namespace

TEST(SocketUtils, ConnectionTerminatedErrorsMatchEitherCategory)
{
   // no using-directive for asio::error: its 'eof' is ambiguous with the
   // Windows CRT's global ::eof(int) from <io.h>
   namespace asio_error = boost::asio::error;
   namespace errc = boost::system::errc;

   expectBothForms(isConnectionTerminatedError, asio_error::timed_out, errc::timed_out);
   expectBothForms(isConnectionTerminatedError, asio_error::connection_reset, errc::connection_reset);
   expectBothForms(isConnectionTerminatedError, asio_error::bad_descriptor, errc::bad_file_descriptor);
   expectBothForms(isConnectionTerminatedError, asio_error::broken_pipe, errc::broken_pipe);

   EXPECT_TRUE(isConnectionTerminatedError(Error(asio_error::make_error_code(asio_error::eof), ERROR_LOCATION)));
   EXPECT_TRUE(isConnectionTerminatedError(systemError(errc::no_such_file_or_directory, ERROR_LOCATION)));
   EXPECT_FALSE(isConnectionTerminatedError(systemError(errc::permission_denied, ERROR_LOCATION)));
   EXPECT_FALSE(isConnectionTerminatedError(Success()));

#ifdef _WIN32
   // see #19756 for whether ERROR_PATH_NOT_FOUND should count too
   EXPECT_TRUE(isConnectionTerminatedError(systemError(ERROR_FILE_NOT_FOUND, ERROR_LOCATION)));
   EXPECT_FALSE(isConnectionTerminatedError(systemError(ERROR_PATH_NOT_FOUND, ERROR_LOCATION)));
#endif
}

TEST(SocketUtils, ConnectionUnavailableErrorsMatchEitherCategory)
{
   namespace errc = boost::system::errc;

   expectBothForms(isConnectionUnavailableError, boost::asio::error::connection_refused, errc::connection_refused);

   EXPECT_TRUE(isConnectionUnavailableError(systemError(errc::no_such_file_or_directory, ERROR_LOCATION)));
   EXPECT_FALSE(isConnectionUnavailableError(systemError(errc::timed_out, ERROR_LOCATION)));
   EXPECT_FALSE(isConnectionUnavailableError(Success()));

#ifdef _WIN32
   // a missing pipe is retried; a bad, malformed or unreachable path is
   // permanent, though Boost maps them all to no_such_file_or_directory
   EXPECT_TRUE(isConnectionUnavailableError(systemError(ERROR_FILE_NOT_FOUND, ERROR_LOCATION)));
   EXPECT_FALSE(isConnectionUnavailableError(systemError(ERROR_PATH_NOT_FOUND, ERROR_LOCATION)));
   EXPECT_FALSE(isConnectionUnavailableError(systemError(ERROR_INVALID_NAME, ERROR_LOCATION)));
   EXPECT_FALSE(isConnectionUnavailableError(systemError(ERROR_BAD_NETPATH, ERROR_LOCATION)));
#endif
}

} // namespace tests
} // namespace http
} // namespace core
} // namespace rstudio
