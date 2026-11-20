/*
 * StreamWriterTests.cpp
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

#include <cstddef>
#include <utility>

#include <boost/asio/error.hpp>
#include <boost/asio/io_context.hpp>
#include <boost/asio/post.hpp>
#include <boost/make_shared.hpp>

#include <core/http/Response.hpp>
#include <core/http/StreamWriter.hpp>

#include <gtest/gtest.h>

namespace rstudio {
namespace core {
namespace http {
namespace tests {

namespace {

// an AsyncWriteStream whose every write fails with the given error
class FailingStream
{
public:
   typedef boost::asio::io_context::executor_type executor_type;

   FailingStream(boost::asio::io_context& ioContext, const boost::system::error_code& error)
      : ioContext_(ioContext),
        error_(error)
   {
   }

   executor_type get_executor()
   {
      return ioContext_.get_executor();
   }

   template <typename ConstBufferSequence, typename WriteHandler>
   void async_write_some(const ConstBufferSequence&, WriteHandler&& handler)
   {
      boost::system::error_code error = error_;
      boost::asio::post(
         ioContext_,
         [handler = std::forward<WriteHandler>(handler), error]() mutable {
            handler(error, 0);
         });
   }

private:
   boost::asio::io_context& ioContext_;
   boost::system::error_code error_;
};

} // anonymous namespace

// The error handler gets the write's error code itself, category included: an
// Error keeps only its category's name, so a code rebuilt from one (as
// AsyncConnectionImpl::handleStreamError once did) lands in the wrong category.
TEST(StreamWriter, ErrorHandlerGetsTheWritesErrorCode)
{
   // an error outside the system category, which isn't a closed connection
   boost::system::error_code writeError =
      boost::asio::error::make_error_code(boost::asio::error::not_found);

   boost::asio::io_context ioContext;
   FailingStream stream(ioContext, writeError);

   bool completed = false;
   boost::system::error_code reported;
   auto pWriter = boost::make_shared<StreamWriter<FailingStream>>(
      stream,
      Response(),
      [&completed]() { completed = true; },
      [&reported](const boost::system::error_code& ec) { reported = ec; });

   pWriter->write();
   ioContext.run();

   // error_code equality compares the category as well as the value
   EXPECT_FALSE(completed);
   EXPECT_EQ(writeError, reported);
}

} // namespace tests
} // namespace http
} // namespace core
} // namespace rstudio
