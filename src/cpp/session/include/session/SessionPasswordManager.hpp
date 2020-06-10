/*
 * SessionPasswordManager.hpp
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
#ifndef SESSION_PASSWORD_MANAGER_HPP
#define SESSION_PASSWORD_MANAGER_HPP

#include <deque>

#include <boost/regex.hpp>
#include <session/SessionConsoleProcess.hpp>

namespace rstudio {
namespace session {
namespace console_process {

class PasswordManager : boost::noncopyable
{
public:
   typedef boost::function<bool(const std::string&, bool, std::string*, bool*)>
                                                               PromptHandler;

   explicit PasswordManager(const boost::regex& promptPattern,
                            const PromptHandler& promptHandler)
      : promptPattern_(promptPattern), promptHandler_(promptHandler)
   {
   }
   virtual ~PasswordManager() {}

   // COPYING: boost::noncopyable

public:
   // NOTE: if you don't showRememberOption then passwords from that
   // interaction will NOT be remembered after the parent console
   // process exits
   void attach(boost::shared_ptr<ConsoleProcess> pCP,
               bool showRememberOption = true);


private:
   bool handlePrompt(const std::string& cpHandle,
                     const std::string& prompt,
                     bool showRememberOption,
                     ConsoleProcess::Input* pInput);

   void onExit(const std::string& cpHandle, int exitCode);

   struct CachedPassword
   {
      CachedPassword() : remember(false) {}
      std::string cpHandle;
      std::string prompt;
      std::string password;
      bool remember;
   };

   static bool hasPrompt(const CachedPassword& cachedPassword,
                         const std::string& prompt);

   static bool hasHandle(const CachedPassword& cachedPassword,
                         const std::string& cpHandle);

   static bool forgetOnExit(const CachedPassword& cachedPassword,
                            const std::string& cpHandle);

private:
   boost::regex promptPattern_;
   PromptHandler promptHandler_;
   std::vector<CachedPassword> passwords_;
};

} // namespace console_process
} // namespace session
} // namespace rstudio

#endif // SESSION_PASSWORD_MANAGER_HPP
