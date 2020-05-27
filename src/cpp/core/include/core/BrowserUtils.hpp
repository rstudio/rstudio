/*
 * BrowserUtils.hpp
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

#ifndef CORE_BROWSER_UTILS_HPP
#define CORE_BROWSER_UTILS_HPP

#include <string>
#include <boost/regex.hpp>

namespace rstudio {
namespace core {

class Error;
class FilePath;

namespace browser_utils {
   
bool isChrome(const std::string& userAgent);
bool isFirefox(const std::string& userAgent);
bool isSafari(const std::string& userAgent);
bool isTrident(const std::string& userAgent);
bool isQt(const std::string& userAgent);

bool isChromeOlderThan(const std::string& userAgent, double version);
bool isFirefoxOlderThan(const std::string& userAgent, double version);
bool isSafariOlderThan(const std::string& userAgent, double version);
bool isTridentOlderThan(const std::string& userAgent, double version);

bool hasRequiredBrowser(const std::string& userAgent);

} // namespace browser_utils
} // namespace core 
} // namespace rstudio


#endif // CORE_BROWSER_UTILS_HPP

