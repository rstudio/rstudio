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
   using namespace boost::asio::error;
   namespace errc = boost::system::errc;

   expectBothForms(isConnectionTerminatedError, timed_out, errc::timed_out);
   expectBothForms(isConnectionTerminatedError, connection_reset, errc::connection_reset);
   expectBothForms(isConnectionTerminatedError, bad_descriptor, errc::bad_file_descriptor);
   expectBothForms(isConnectionTerminatedError, broken_pipe, errc::broken_pipe);

   EXPECT_TRUE(isConnectionTerminatedError(Error(make_error_code(eof), ERROR_LOCATION)));
   EXPECT_FALSE(isConnectionTerminatedError(systemError(errc::permission_denied, ERROR_LOCATION)));
   EXPECT_FALSE(isConnectionTerminatedError(Success()));
}

TEST(SocketUtils, ConnectionUnavailableErrorsMatchEitherCategory)
{
   namespace errc = boost::system::errc;

   expectBothForms(isConnectionUnavailableError, boost::asio::error::connection_refused, errc::connection_refused);

   EXPECT_TRUE(isConnectionUnavailableError(systemError(errc::no_such_file_or_directory, ERROR_LOCATION)));
   EXPECT_FALSE(isConnectionUnavailableError(systemError(errc::timed_out, ERROR_LOCATION)));
   EXPECT_FALSE(isConnectionUnavailableError(Success()));

#ifdef _WIN32
   // a missing pipe is retried; a malformed or unreachable name is permanent,
   // though Boost maps both to no_such_file_or_directory
   EXPECT_TRUE(isConnectionUnavailableError(systemError(ERROR_FILE_NOT_FOUND, ERROR_LOCATION)));
   EXPECT_FALSE(isConnectionUnavailableError(systemError(ERROR_INVALID_NAME, ERROR_LOCATION)));
   EXPECT_FALSE(isConnectionUnavailableError(systemError(ERROR_BAD_NETPATH, ERROR_LOCATION)));
#endif
}

} // namespace tests
} // namespace http
} // namespace core
} // namespace rstudio
