/*
 * ChatBackendStdoutTests.cpp
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

#include "ChatBackendStdout.hpp"

#include <gtest/gtest.h>

using namespace rstudio::session::modules::chat::backend;

namespace {

using Bodies = std::vector<std::string>;

std::string frame(const std::string& body)
{
   return "Content-Length: " + std::to_string(body.length()) + "\r\n\r\n" + body;
}

} // anonymous namespace

TEST(ChatBackendStdout, ReadsTheStartedBackend)
{
   StdoutReader reader;
   reader.start(1);
   EXPECT_EQ(reader.append(1, frame("{\"id\":1}")), Bodies{"{\"id\":1}"});
}

TEST(ChatBackendStdout, ReadsNothingBeforeAStart)
{
   StdoutReader reader;
   EXPECT_TRUE(reader.append(1, frame("{}")).empty());
}

TEST(ChatBackendStdout, DropsOutputAfterAStop)
{
   StdoutReader reader;
   reader.start(1);
   reader.stop();
   EXPECT_TRUE(reader.append(1, frame("{\"id\":1}")).empty());
}

TEST(ChatBackendStdout, DropsAReplacedBackendsOutput)
{
   StdoutReader reader;
   reader.start(1);
   reader.stop();
   reader.start(2);
   EXPECT_TRUE(reader.append(1, frame("{\"id\":1}")).empty());
   EXPECT_EQ(reader.append(2, frame("{\"id\":2}")), Bodies{"{\"id\":2}"});
}

TEST(ChatBackendStdout, DropsAPartialMessageAtAStop)
{
   StdoutReader reader;
   reader.start(1);
   EXPECT_TRUE(reader.append(1, "Content-Length: 500\r\n\r\n{\"id\":").empty());
   reader.stop();
   reader.start(2);
   EXPECT_EQ(reader.append(2, frame("{\"id\":2}")), Bodies{"{\"id\":2}"});
}

TEST(ChatBackendStdout, DropsAPartialMessageWhenStartedWithoutAStop)
{
   StdoutReader reader;
   reader.start(1);
   EXPECT_TRUE(reader.append(1, "Content-Length: 500\r\n\r\n{\"id\":").empty());
   reader.start(2);
   EXPECT_EQ(reader.append(2, frame("{\"id\":2}")), Bodies{"{\"id\":2}"});
}

TEST(ChatBackendStdout, DropsAPartialMessageFromAReplacedBackend)
{
   // If it were read, the old backend's header would swallow the new
   // backend's first message.
   StdoutReader reader;
   reader.start(1);
   reader.stop();
   reader.start(2);
   EXPECT_TRUE(reader.append(1, "Content-Length: 500\r\n\r\n").empty());
   EXPECT_EQ(reader.append(2, frame("{\"id\":2}")), Bodies{"{\"id\":2}"});
}
