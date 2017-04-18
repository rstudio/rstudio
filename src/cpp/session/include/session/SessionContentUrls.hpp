/*
 * SessionContentUrls.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#ifndef SESSION_SESSION_CONTENT_URLS_HPP
#define SESSION_SESSION_CONTENT_URLS_HPP

#include <string>

namespace rstudio {
namespace core {
   class Error;
   class FilePath;
}
}
 
namespace rstudio {
namespace session {    
namespace content_urls {

std::string provision(const std::string& title, const core::FilePath& filePath);

std::string provision(const std::string& title,
                      const std::string& content,
                      const std::string& extension);

core::Error initialize();
   
} // namespace content_urls
} // namespace session
} // namespace rstudio

#endif // SESSION_SESSION_CONTENT_URLS_HPP
