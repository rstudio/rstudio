/*
 * BrowserUtils.cpp
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

#include <boost/algorithm/string/predicate.hpp>

#include <core/BrowserUtils.hpp>
#include <core/RegexUtils.hpp>
#include <shared_core/SafeConvert.hpp>

using namespace boost::algorithm;

namespace rstudio {
namespace core {
namespace browser_utils {

namespace {

bool hasRequiredBrowserVersion(const std::string& userAgent,
                               const boost::regex& versionRegEx,
                               double requiredVersion)
{
   double detectedVersion = requiredVersion;
   boost::smatch match;
   if (regex_utils::search(userAgent, match, versionRegEx))
   {
      std::string versionString = match[1];
      detectedVersion = safe_convert::stringTo<double>(versionString,
                                                       detectedVersion);
      if (detectedVersion < requiredVersion)
         return false;
   }

   return true;
}

} // anonymous namespace


bool isChrome(const std::string& userAgent)
{
   return contains(userAgent, "Chrome");
}

bool isChromeOlderThan(const std::string& userAgent, double version)
{
   if (isChrome(userAgent))
   {
      boost::regex chromeRegEx("(?:Chrome)/(\\d{1,4})");
      return !hasRequiredBrowserVersion(userAgent, chromeRegEx, version);
   }
   else
   {
      return false;
   }

}

bool isFirefox(const std::string& userAgent)
{
   return contains(userAgent, "Firefox");
}

bool isFirefoxOlderThan(const std::string& userAgent, double version)
{
   if (isFirefox(userAgent))
   {
      boost::regex ffRegEx("Firefox/(\\d{1,4})");
      return !hasRequiredBrowserVersion(userAgent, ffRegEx, version);
   }
   else
   {
      return false;
   }
}

bool isSafari(const std::string& userAgent)
{
   return (contains(userAgent, "Safari") ||
           contains(userAgent, "AppleWebKit")) &&
          !contains(userAgent, "Chrome");
}

bool isQt(const std::string& userAgent)
{
   return (contains(userAgent, "AppleWebKit")) &&
           contains(userAgent, "Qt");
}

bool isSafariOlderThan(const std::string& userAgent, double version)
{
   if (isSafari(userAgent))
   {
      boost::regex safariRegEx("Version/(\\d{1,4}\\.\\d)");
      return !hasRequiredBrowserVersion(userAgent, safariRegEx, version);
   }
   else
   {
      return false;
   }
}

bool isTrident(const std::string& userAgent)
{
   return contains(userAgent, "Trident");
}

bool isTridentOlderThan(const std::string& userAgent, double version)
{
   if (isTrident(userAgent))
   {
      boost::regex tridentRegEx("Trident/(\\d{1,4})");
      return !hasRequiredBrowserVersion(userAgent, tridentRegEx, version);
   }
   else
   {
      return false;
   }
}


bool hasRequiredBrowser(const std::string& userAgent)
{
   if (isChromeOlderThan(userAgent, 69))
   {
      // Chrome user agent based on oldest supported Chrome release.
      // Ideally this should be version 71, but our QT browser in use for RDP is pinned at version 69.
      // See: https://endoflife.software/applications/browsers/google-chrome
      return false;
   }
   else if (isFirefoxOlderThan(userAgent, 68))
   {
      // Firefox user agent based on oldest ESR release. See:
      // https://support.mozilla.org/en-US/kb/firefox-esr-release-cycle
      return false;
   }
   else if (isSafariOlderThan(userAgent, 12.1))
   {
      // Safari user agent based on the Safari version on the oldest supported version of macOS.
      // See:
      // https://en.wikipedia.org/wiki/Safari_version_history
      return false;
   }
   else if (isTridentOlderThan(userAgent, 7.0))
   {
      // Trident user agent based on IE 11, the last version of IE (and the only one we support
      // since IE 10 is EOL and no further IE releases based on Trident are expected)
      return false;
   }
   else
   {
      return isChrome(userAgent) ||
             isFirefox(userAgent) ||
             isSafari(userAgent) ||
             isTrident(userAgent);
   }
}


} // namespace browser_utils
} // namespace core 
} // namespace rstudio



