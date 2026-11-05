/*
 * SessionStartupWarning.hpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef SESSION_STARTUP_WARNING_HPP
#define SESSION_STARTUP_WARNING_HPP

#include <string>

#include <core/Settings.hpp>

namespace rstudio {
namespace session {

// Remember the last warning for a condition across suspend/resume and Restart R.
// A new session starts with no remembered warning, even if old settings remain.
class StartupWarning
{
public:
   explicit StartupWarning(const std::string& stateKey) : stateKey_(stateKey) {}

   bool shouldShow(const std::string& message)
   {
      bool show = !message.empty() && message != lastMessage_;
      // Clearing the condition allows the same warning to be shown if it recurs.
      lastMessage_ = message;
      return show;
   }

   void onSuspend(core::Settings* pSettings) const
   {
      pSettings->set(stateKey_, lastMessage_);
   }

   void onResume(const core::Settings& settings)
   {
      lastMessage_ = settings.get(stateKey_);
   }

private:
   std::string stateKey_;
   std::string lastMessage_;
};

} // namespace session
} // namespace rstudio

#endif // SESSION_STARTUP_WARNING_HPP
