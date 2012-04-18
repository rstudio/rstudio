/*
 * SessionPersistentState.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
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
#include <core/system/System.hpp>

#include <session/SessionOptions.hpp>
#include <session/SessionModuleContext.hpp>

using namespace core ;

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

   desktopClientId_ = core::system::generateUuid();

   FilePath scratchPath = module_context::scopedScratchPath();
   FilePath statePath = scratchPath.complete("persistent-state");
   return settings_.initialize(statePath);
}

std::string PersistentState::activeClientId()
{
   if (serverMode_)
   {
      std::string activeClientId = settings_.get(kActiveClientId);
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
      settings_.set(kActiveClientId, newId);
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

} // namespace session
