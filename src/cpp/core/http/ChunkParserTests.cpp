/*
 * ChunkParserTests.cpp
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

#include <ios>
#include <sstream>
#include <deque>

#include <core/http/ChunkParser.hpp>

#include <gtest/gtest.h>

namespace rstudio {
namespace core {
namespace http {
namespace tests {

const std::string chunkEnd = "0\r\n\r\n";

TEST(HttpTest, CanParseOneSimpleChunkWithNoChunkEnd)
{
   std::string chunk = "Hello, world!";
   std::stringstream payload;
   payload << std::hex << chunk.size() << "\r\n" << chunk << "\r\n";
   std::string payloadStr = payload.str();

   ChunkParser chunkParser;
   std::deque<boost::shared_ptr<std::string> > chunks;

   EXPECT_FALSE(chunkParser.parse(payloadStr.c_str(), payloadStr.size(), &chunks));
   EXPECT_TRUE(chunks.size() == 1);
   EXPECT_TRUE(*(chunks.at(0)) == chunk);
}

TEST(HttpTest, CanParseOnSimpleChunkWithChunkEnd)
{
   std::string chunk = "Hello, world!";

   std::stringstream payload;
   payload << std::hex << chunk.size() << "\r\n" << chunk << "\r\n" << chunkEnd;
   std::string payloadStr = payload.str();

   ChunkParser chunkParser;
   std::deque<boost::shared_ptr<std::string> > chunks;

   EXPECT_TRUE(chunkParser.parse(payloadStr.c_str(), payloadStr.size(), &chunks));
   EXPECT_EQ(chunks.size(), 1u);
   EXPECT_TRUE(*(chunks.at(0)) == chunk);
}

TEST(HttpTest, CanParseMultipleSimpleChunksWithChunkEnd)
{
   std::string chunk = "Hello, world!";
   std::string chunk2 = "This is a second chunk";
   std::string chunk3 = "This is a third chunk";

   std::stringstream payload;
   payload << std::hex << chunk.size() << "\r\n" << chunk << "\r\n";
   payload << std::hex << chunk2.size() << "\r\n" << chunk2 << "\r\n";
   payload << std::hex << chunk3.size() << "\r\n" << chunk3 << "\r\n";
   payload << chunkEnd;

   std::string payloadStr = payload.str();

   ChunkParser chunkParser;
   std::deque<boost::shared_ptr<std::string> > chunks;

   EXPECT_TRUE(chunkParser.parse(payloadStr.c_str(), payloadStr.size(), &chunks));
   EXPECT_EQ(chunks.size(), 3u);
   EXPECT_TRUE(*(chunks.at(0)) == chunk);
   EXPECT_TRUE(*(chunks.at(1)) == chunk2);
   EXPECT_TRUE(*(chunks.at(2)) == chunk3);
}


TEST(HttpTest, CanParsePiecewiseChunks)
{
   std::string chunk1 = "Hello, world!";
   std::string chunk2 = "This is a second chunk";
   std::string chunk3 = "This is a third chunk";

   std::stringstream piece1;
   piece1 << std::hex << chunk1.size() << "\r";
   std::string piece1Str = piece1.str();

   std::stringstream piece2;
   piece2 << "\n" << chunk1.substr(0, 10);
   std::string piece2Str = piece2.str();

   std::stringstream piece3;
   piece3 << chunk1.substr(10);
   std::string piece3Str = piece3.str();

   std::stringstream piece4;
   piece4 << "\r\n" << std::hex << chunk2.size() << "\r\n" << chunk2 << "\r\n" << std::hex << chunk3.size();
   std::string piece4Str = piece4.str();

   std::stringstream piece5;
   piece5 << "\r\n" << chunk3 << "\r\n" << chunkEnd;
   std::string piece5Str = piece5.str();

   ChunkParser chunkParser;
   std::deque<boost::shared_ptr<std::string> > chunks;

   EXPECT_FALSE(chunkParser.parse(piece1Str.c_str(), piece1Str.size(), &chunks));
   EXPECT_EQ(0u, chunks.size());

   EXPECT_FALSE(chunkParser.parse(piece2Str.c_str(), piece2Str.size(), &chunks));
   EXPECT_EQ(0u, chunks.size());

   EXPECT_FALSE(chunkParser.parse(piece3Str.c_str(), piece3Str.size(), &chunks));
   EXPECT_EQ(1u, chunks.size());
   EXPECT_EQ(chunk1, *(chunks.at(0)));

   EXPECT_FALSE(chunkParser.parse(piece4Str.c_str(), piece4Str.size(), &chunks));
   EXPECT_EQ(2u, chunks.size());
   EXPECT_EQ(chunk2, *(chunks.at(1)));

   EXPECT_TRUE(chunkParser.parse(piece5Str.c_str(), piece5Str.size(), &chunks));
   EXPECT_EQ(3u, chunks.size());
   EXPECT_EQ(chunk3, *(chunks.at(2)));
}

} // namespace tests
} // namespace http
} // namespace core
} // namespace rstudio