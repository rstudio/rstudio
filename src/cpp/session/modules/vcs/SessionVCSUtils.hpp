/*
 * SessionVCSUtils.hpp
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

#ifndef SESSION_VCS_UTILS_HPP
#define SESSION_VCS_UTILS_HPP

#include <boost/noncopyable.hpp>

#include <core/json/Json.hpp>
#include <core/system/Process.hpp>

namespace session {
namespace modules {
namespace vcs_utils {

void enqueRefreshEventWithDelay(int delay);
void enqueueRefreshEvent();

core::json::Object processResultToJson(
      const core::system::ProcessResult& result);

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

#endif // SESSION_VCS_UTILS_HPP

