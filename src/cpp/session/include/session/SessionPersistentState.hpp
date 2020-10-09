/*
 * SessionPersistentState.hpp
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

#ifndef SESSION_PERSISTENT_STATE_HPP
#define SESSION_PERSISTENT_STATE_HPP

#include <string>

#include <boost/utility.hpp>

#include <core/Settings.hpp>

namespace rstudio {
namespace session {

// singleton
class PersistentState;
PersistentState& persistentState();
   
class PersistentState : boost::noncopyable
{
private:
   PersistentState() : serverMode_(false) {}
   friend PersistentState& persistentState();
   
public:
   // COPYING: boost::noncopyable
   
   core::Error initialize();
   
   // active-client-id
   std::string activeClientId();
   std::string newActiveClientId();

   // browser-facing url of active client
   std::string activeClientUrl() const;
   void setActiveClientUrl(const std::string& url);
   
   // abend
   bool hadAbend();
   void setAbend(bool abend);

   // active environment
   std::string activeEnvironmentName() const;
   void setActiveEnvironmentName(std::string environmentName);

   // environment monitor state
   bool environmentMonitoring() const;
   void setEnvironmentMonitoring(bool monitoring);

   // resolved hashes (for Packrat libraries and lockfiles)
   std::string getStoredHash(const std::string& hashName) const;
   void setStoredHash(const std::string& hashName, 
                      const std::string& hashValue);

   // port scrambling token
   std::string portToken() const;
   void setPortToken(const std::string& token);

   // get underlying settings
   core::Settings& settings() { return settings_; }

private:
   bool serverMode_;
   std::string desktopClientId_;
   core::Settings settings_;
   core::Settings sessionSettings_;
};
   
} // namespace session
} // namespace rstudio

#endif // SESSION_PERSISTENT_STATE_HPP

