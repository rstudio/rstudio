/*
 * Hash.cpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <core/Hash.hpp>

#include <sstream>

#include <boost/crc.hpp>
#include <boost/lexical_cast.hpp>

namespace core {
namespace hash {   

std::string crc32Hash(const std::string& content)
{
   boost::crc_32_type result;
   result.process_bytes(content.data(), content.length());
   return boost::lexical_cast<std::string>(result.checksum());
}
   
} // namespace hash
} // namespace core 



