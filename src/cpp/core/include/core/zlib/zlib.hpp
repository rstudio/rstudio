/*
* zlib.hpp
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

#ifndef CORE_ZLIB_ZLIB_HPP
#define CORE_ZLIB_ZLIB_HPP

#include <string>

#include <shared_core/Error.hpp>

namespace rstudio {
namespace core {
namespace zlib {

Error compressString(const std::string& toCompress, std::vector<unsigned char>* compressedData);

Error decompressString(const std::vector<unsigned char>& compressedData, std::string* str);

} // namespace zlib
} // namespace core
} // namespace rstudio

#endif
