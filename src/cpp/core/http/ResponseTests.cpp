/*
 * ResponseTests.cpp
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

// Regression coverage for Response::setStreamFile(): when the underlying file
// stream cannot be opened, initialize() fails and setError() is called. The
// stream response used to be left set, so isStreamResponse() still returned
// true and the async HTTP server drove the StreamWriter into nextBuffer() on a
// stream whose file was never opened -- a null pointer dereference that
// crashed the session. setStreamFile() must clear the stream response on
// failure so the error body is sent instead.

#include <core/http/Response.hpp>
#include <core/http/Request.hpp>

#include <core/FileSerializer.hpp>

#include <shared_core/FilePath.hpp>

#ifndef _WIN32
#include <sys/stat.h>
#include <unistd.h>
#endif

#include <gtest/gtest.h>

namespace rstudio {
namespace core {
namespace http {
namespace tests {

TEST(ResponseStreamFile, MissingFileDoesNotLeaveAStreamResponse)
{
   // a path that is guaranteed not to exist: create a temp file, then remove it
   FilePath missingFile;
   ASSERT_FALSE(FilePath::tempFilePath(missingFile));
   ASSERT_FALSE(missingFile.remove());
   ASSERT_FALSE(missingFile.exists());

   // advertise gzip so setStreamFile() takes the compressed
   // (ZlibCompressionStreamResponse) path -- the path exercised in the field
   // on Linux/Unix (Request is noncopyable, so build it in place)
   Request request;
   request.setHeader("Accept-Encoding", "gzip");
   Response response;
   response.setStreamFile(missingFile, request);

   // initialize() failed, so the response must NOT be treated as a stream --
   // otherwise the HTTP server calls nextBuffer() on a null file stream
   EXPECT_FALSE(response.isStreamResponse());
   EXPECT_FALSE(response.getStreamResponse());
   EXPECT_EQ(response.statusCode(), status::InternalServerError);
}

#ifndef _WIN32
TEST(ResponseStreamFile, UnreadableFileDoesNotLeaveAStreamResponse)
{
   // the reported scenario: exporting a chmod 000 file. Skip when running as
   // root, which bypasses the permission check and can read the file anyway.
   if (::geteuid() == 0)
      GTEST_SKIP() << "running as root bypasses file permissions";

   FilePath unreadableFile;
   ASSERT_FALSE(FilePath::tempFilePath(unreadableFile));
   ASSERT_FALSE(writeStringToFile(unreadableFile, "test"));
   ASSERT_EQ(::chmod(unreadableFile.getAbsolutePath().c_str(), 0), 0);

   Request request;
   request.setHeader("Accept-Encoding", "gzip");
   Response response;
   response.setStreamFile(unreadableFile, request);

   EXPECT_FALSE(response.isStreamResponse());
   EXPECT_FALSE(response.getStreamResponse());
   EXPECT_EQ(response.statusCode(), status::InternalServerError);

   // restore permissions so the temp file can be cleaned up
   ::chmod(unreadableFile.getAbsolutePath().c_str(), 0600);
   unreadableFile.remove();
}
#endif

} // namespace tests
} // namespace http
} // namespace core
} // namespace rstudio
