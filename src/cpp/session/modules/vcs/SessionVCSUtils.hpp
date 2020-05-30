/*
 * SessionVCSUtils.hpp
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

#ifndef SESSION_VCS_UTILS_HPP
#define SESSION_VCS_UTILS_HPP

#include <boost/noncopyable.hpp>

#include <shared_core/json/Json.hpp>
#include <core/system/Process.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace vcs_utils {

void enqueRefreshEventWithDelay(int delay);
void enqueueRefreshEvent();

core::json::Object processResultToJson(
      const core::system::ProcessResult& result);

core::FilePath fileFilterPath(const core::json::Value& fileFilterJson);

void splitMessage(const std::string message,
                  std::string* pSubject,
                  std::string* pDescription);

// If no invalid byte ranges are encountered, then everything will be converted
// from project encoding to UTF-8, regardless of allowSubst value.
//
// If allowSubst is true, and invalid byte ranges are encountered, they will
// be replaced with ? and any valid byte ranges will be converted from project
// encoding to UTF-8.
//
// If allowSubst is false, and invalid byte ranges are encountered, the entire
// string is returned unchanged.
std::string convertToUtf8(const std::string& content, bool allowSubst);

std::string convertDiff(const std::string& diff,
                        const std::string& fromEncoding,
                        const std::string& toEncoding,
                        bool allowSubst,
                        bool* pSuccess=nullptr);

struct RefreshOnExit : public boost::noncopyable
{
   ~RefreshOnExit()
   {
      try
      {
         enqueueRefreshEvent();
      }
      catch(...)
      {
      }
   }
};

} // namespace vcs_utils
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_VCS_UTILS_HPP

