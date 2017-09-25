/*
 * SecureKeyFile.hpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

#ifndef CORE_SECURE_KEY_FILE_HPP
#define CORE_SECURE_KEY_FILE_HPP

#include <string>

namespace rstudio {
namespace core {
class Error;
}
}

namespace rstudio {
namespace core {
namespace key_file {
  
core::Error readSecureKeyFile(const std::string& file, std::string* pContents);

} // namespace key_file
} // namespace server
} // namespace rstudio

#endif // CORE_SECURE_KEY_FILE_HPP

