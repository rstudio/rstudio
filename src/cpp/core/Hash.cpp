/*
 * Hash.cpp
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

#include <core/Hash.hpp>

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



