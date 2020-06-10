/*
 * ChunkParserTests.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
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

#include <tests/TestThat.hpp>

namespace rstudio {
namespace core {
namespace http {
namespace tests {

const std::string chunkEnd = "0\r\n\r\n";

test_context("ChunkParserTests")
{
   test_that("Can parse one simple chunk with no chunk end")
   {
      std::string chunk = "Hello, world!";
      std::stringstream payload;
      payload << std::hex << chunk.size() << "\r\n" << chunk << "\r\n";
      std::string payloadStr = payload.str();

      ChunkParser chunkParser;
      std::deque<boost::shared_ptr<std::string> > chunks;

      CHECK_FALSE(chunkParser.parse(payloadStr.c_str(), payloadStr.size(), &chunks));
      CHECK(chunks.size() == 1);
      CHECK(*(chunks.at(0)) == chunk);
   }

   test_that("Can parse on simple chunk with chunk end")
   {
      std::string chunk = "Hello, world!";

      std::stringstream payload;
      payload << std::hex << chunk.size() << "\r\n" << chunk << "\r\n" << chunkEnd;
      std::string payloadStr = payload.str();

      ChunkParser chunkParser;
      std::deque<boost::shared_ptr<std::string> > chunks;

      CHECK(chunkParser.parse(payloadStr.c_str(), payloadStr.size(), &chunks));
      CHECK(chunks.size() == 1);
      CHECK(*(chunks.at(0)) == chunk);
   }

   test_that("Can parse multiple simple chunks with chunk end")
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

      CHECK(chunkParser.parse(payloadStr.c_str(), payloadStr.size(), &chunks));
      CHECK(chunks.size() == 3);
      CHECK(*(chunks.at(0)) == chunk);
      CHECK(*(chunks.at(1)) == chunk2);
      CHECK(*(chunks.at(2)) == chunk3);
   }

   test_that("Can parse piecewise chunks")
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

      CHECK_FALSE(chunkParser.parse(piece1Str.c_str(), piece1Str.size(), &chunks));
      CHECK(chunks.size() == 0);

      CHECK_FALSE(chunkParser.parse(piece2Str.c_str(), piece2Str.size(), &chunks));
      CHECK(chunks.size() == 0);

      CHECK_FALSE(chunkParser.parse(piece3Str.c_str(), piece3Str.size(), &chunks));
      CHECK(chunks.size() == 1);
      CHECK(*(chunks.at(0)) == chunk1);

      CHECK_FALSE(chunkParser.parse(piece4Str.c_str(), piece4Str.size(), &chunks));
      CHECK(chunks.size() == 2);
      CHECK(*(chunks.at(1)) == chunk2);

      CHECK(chunkParser.parse(piece5Str.c_str(), piece5Str.size(), &chunks));
      CHECK(chunks.size() == 3);
      CHECK(*(chunks.at(2)) == chunk3);
   }
}

} // end namespace tests
} // end namespace http
} // end namespace core
} // end namespace rstudio

