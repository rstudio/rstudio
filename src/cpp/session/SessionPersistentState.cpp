/*
 * SessionPersistentState.cpp
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

#include <session/SessionPersistentState.hpp>

#include <core/Log.hpp>
#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <core/FileSerializer.hpp>
#include <core/system/System.hpp>

#include <session/SessionOptions.hpp>
#include <session/SessionModuleContext.hpp>

#include "session-config.h"

#ifdef RSTUDIO_SERVER
#include <server_core/UrlPorts.hpp>
#endif

using namespace rstudio::core;

namespace rstudio {
namespace session {  

namespace {
const char * const kActiveClientId = "active-client-id";
const char * const kAbend = "abend";
}
   
PersistentState& persistentState()
{
   static PersistentState instance;
   return instance;
}
   
Error PersistentState::initialize()
{
   serverMode_ = (session::options().programMode() ==
                  kSessionProgramModeServer);

   // always the same so that we can supporrt a restart of
   // the session without reloading the client page
   desktopClientId_ = "33e600bb-c1b1-46bf-b562-ab5cba070b0e";

   // scoped/project settings
   FilePath scratchPath = module_context::scopedScratchPath();
   FilePath statePath = scratchPath.completePath("persistent-state");
   Error error = settings_.initialize(statePath);
   if (error)
      return error;

   // session settings
   scratchPath = module_context::sessionScratchPath();
   statePath = scratchPath.completePath("session-persistent-state");
   return sessionSettings_.initialize(statePath);
}

std::string PersistentState::activeClientId()
{
   if (serverMode_)
   {
      std::string activeClientId = sessionSettings_.get(kActiveClientId);
      if (!activeClientId.empty())
         return activeClientId;
      else
         return newActiveClientId();
   }
   else
   {
      return desktopClientId_;
   }
}

std::string PersistentState::newActiveClientId() 
{
   if (serverMode_)
   {
      std::string newId = core::system::generateUuid();
      sessionSettings_.set(kActiveClientId, newId);
      return newId;
   }
   else
   {
      return desktopClientId_;
   }
}

std::string PersistentState::activeClientUrl() const
{
   return settings_.get("activeClientUrl", "");
}

void PersistentState::setActiveClientUrl(const std::string& url)
{
   settings_.set("activeClientUrl", url);
}

// abend tracking only applies to server mode

bool PersistentState::hadAbend() 
{ 
   if (serverMode_)
   {
      return sessionSettings_.getInt(kAbend, false);
   }
   else
   {
      return false;
   }
}
   
void PersistentState::setAbend(bool abend) 
{ 
   if (serverMode_)
   {
      sessionSettings_.set(kAbend, abend);
   }
}

std::string PersistentState::activeEnvironmentName() const
{
   return settings_.get("activeEnvironmentName", "R_GlobalEnv");
}

void PersistentState::setActiveEnvironmentName(std::string environmentName)
{
   settings_.set("activeEnvironmentName", environmentName);
}


std::string PersistentState::portToken() const
{
   return settings_.get("portToken", 
#ifdef RSTUDIO_SERVER
   // on RStudio Server, we have a fallback default so that we're guaranteed to have a port token to
   // work with (better a predictable obfuscated value than a raw or busted one)
   kDefaultPortToken
#else
   // Desktop doesn't use port tokens
   ""
#endif
   );
}

void PersistentState::setPortToken(const std::string& token)
{
   settings_.set("portToken", token);
}

std::string PersistentState::getStoredHash(const std::string& hashName) const
{
   return settings_.get(hashName + "Hash", "");
}

void PersistentState::setStoredHash(const std::string& hashName, 
                                    const std::string& hashValue)
{
   settings_.set(hashName + "Hash", hashValue);
}

bool PersistentState::environmentMonitoring() const
{
   return settings_.getBool("environmentMonitoring", true);
}

void PersistentState::setEnvironmentMonitoring(bool monitoring)
{
   return settings_.set("environmentMonitoring", monitoring);
}

} // namespace session
} // namespace rstudio
