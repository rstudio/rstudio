/*
 * RSessionUtils.hpp
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

#ifndef R_SESSION_UTILS_HPP
#define R_SESSION_UTILS_HPP

#include <string>

namespace core {
   class Error;
   class FilePath;
}

namespace r {
namespace session {
namespace utils {
      
// user home path
const core::FilePath& userHomePath();  

core::FilePath safeCurrentPath();
   
core::FilePath tempFile(const std::string& prefix, 
                        const std::string& extension);

core::FilePath tempDir();
     
// suppress output in scope
class SuppressOutputInScope
{
public:
   SuppressOutputInScope();
   ~SuppressOutputInScope();
};

} // namespace utils
} // namespace session
} // namespace r

#endif // R_SESSION_UTILS_HPP

