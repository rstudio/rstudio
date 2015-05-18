/*
 * SessionPersistentState.cpp
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

#include <session/SessionPersistentState.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>
#include <core/system/System.hpp>

#include <session/SessionOptions.hpp>
#include <session/SessionModuleContext.hpp>

using namespace rstudio::core ;

namespace rstudio {
namespace session {  

namespace {
const char * const kActiveClientId = "active-client-id";
const char * const kAbend = "abend";
}
   
PersistentState& persistentState()
{
   static PersistentState instance ;
   return instance;
}
   
Error PersistentState::initialize()
{
   serverMode_ = (session::options().programMode() ==
                  kSessionProgramModeServer);

   // always the same so that we can supporrt a restart of
   // the session without reloading the client page
   desktopClientId_ = "33e600bb-c1b1-46bf-b562-ab5cba070b0e";

   FilePath scratchPath = module_context::scopedScratchPath();
   activeClientIdPath_ = scratchPath.childPath(kActiveClientId);
   FilePath statePath = scratchPath.complete("persistent-state");
   return settings_.initialize(statePath);
}

std::string PersistentState::activeClientId()
{
   if (serverMode_)
   {
      std::string activeClientId;
      if (activeClientIdPath_.exists())
      {
         Error error = core::readStringFromFile(activeClientIdPath_,
                                                &activeClientId);
         if (error)
            LOG_ERROR(error);

         boost::algorithm::trim(activeClientId);
      }

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
      Error error = core::writeStringToFile(activeClientIdPath_, newId);
      if (error)
         LOG_ERROR(error);
      return newId;
   }
   else
   {
      return desktopClientId_;
   }
}

// abend tracking only applies to server mode

bool PersistentState::hadAbend() 
{ 
   if (serverMode_)
   {
      return settings_.getInt(kAbend, false);
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
      settings_.set(kAbend, abend);
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

std::string PersistentState::getStoredHash(const std::string& hashName) const
{
   return settings_.get(hashName + "Hash", "");
}

void PersistentState::setStoredHash(const std::string& hashName, 
                                    const std::string& hashValue)
{
   settings_.set(hashName + "Hash", hashValue);
}

} // namespace session
} // namespace rstudio
