/*
 * GwtFileHandler.hpp
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

#ifndef CORE_GWT_FILE_HANDLER_HPP
#define CORE_GWT_FILE_HANDLER_HPP

#include <core/http/UriHandler.hpp>

namespace rstudio {
namespace core {
namespace gwt {
      
http::UriHandlerFunction fileHandlerFunction(
      const std::string& wwwLocalPath,
      const std::string& baseUri = std::string(),
      http::UriFilterFunction mainPageFilter = http::UriFilterFunction(),
      const std::string& initJs = std::string(),
      const std::string& gwtPrefix = std::string(),
      bool useEmulatedStack = false,
      const std::string& frameOptions = std::string());
   
} // namespace gwt
} // namespace core
} // namespace rstudio

#endif // CORE_GWT_FILE_HANDLER_HPP

