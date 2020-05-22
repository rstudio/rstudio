/*
 * Base64.hpp
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

#ifndef CORE_SYSTEM_BASE64_HPP
#define CORE_SYSTEM_BASE64_HPP

#include <string>

namespace rstudio {
namespace core {

class Error;
class FilePath;

namespace base64 {
      

Error encode(const char* pData, std::size_t n, std::string* pOutput);
Error encode(const std::string& input, std::string* pOutput);
Error encode(const FilePath& inputFile, std::string* pOutput);

Error decode(const char* pData, std::size_t n, std::string* pOutput);
Error decode(const std::string& input, std::string* pOutput);

         
} // namespace base64
} // namespace core
} // namespace rstudio

#endif // CORE_SYSTEM_BASE64_HPP

