/*
 * RequestParserTestUtils.hpp
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

#ifndef CORE_HTTP_REQUEST_PARSER_TEST_UTILS_HPP
#define CORE_HTTP_REQUEST_PARSER_TEST_UTILS_HPP

#include <cstdint>

#include <core/http/RequestParser.hpp>

namespace rstudio {
namespace core {
namespace http {
namespace tests {

// the limit is process-wide, so put it back for the tests that follow
class ScopedMaxBufferedBodySize
{
public:
   explicit ScopedMaxBufferedBodySize(uintmax_t maxSize)
      : previous_(RequestParser::maxBufferedBodySize())
   {
      RequestParser::setMaxBufferedBodySize(maxSize);
   }

   ~ScopedMaxBufferedBodySize()
   {
      RequestParser::setMaxBufferedBodySize(previous_);
   }

   ScopedMaxBufferedBodySize(const ScopedMaxBufferedBodySize&) = delete;
   ScopedMaxBufferedBodySize& operator=(const ScopedMaxBufferedBodySize&) = delete;

private:
   uintmax_t previous_;
};

} // namespace tests
} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_REQUEST_PARSER_TEST_UTILS_HPP
