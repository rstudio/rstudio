/*
 * Hash.hpp
 * 
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant to the terms of a commercial license agreement
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

#include <shared_core/Hash.hpp>

#include <sstream>
#include <iomanip>

#include <boost/crc.hpp>
#include <boost/lexical_cast.hpp>

#include <shared_core/SafeConvert.hpp>

namespace rstudio {
namespace core {
namespace hash {   

std::string crc32Hash(const std::string& content)
{
   boost::crc_32_type result;
   result.process_bytes(content.data(), content.length());
   return safe_convert::numberToString(result.checksum());
}

std::string crc32HexHash(const std::string& content)
{
   // compute checksum
   boost::crc_32_type result;
   result.process_bytes(content.data(), content.length());

   // return hex representation; ensure padded to 8 characters
   std::ostringstream output;
   output << std::uppercase << std::setw(8) << std::setfill('0') 
          << std::hex << result.checksum();
   return output.str();
}

} // namespace hash
} // namespace core
} // namespace rstudio
