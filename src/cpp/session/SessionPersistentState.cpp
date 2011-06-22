/*
 * SessionPersistentState.cpp
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

#include "SessionPersistentState.hpp"

#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/system/System.hpp>

#include <session/SessionOptions.hpp>

using namespace core ;

namespace session {  

namespace {
const char * const kActiveClientId = "active-client-id";
const char * const kAbend = "abend";
const char * const kNextSessionProjectPath = "next-session-project-path";
}
   
PersistentState& persistentState()
{
   static PersistentState instance ;
   return instance;
}
   
Error PersistentState::initialize()
{
   FilePath scratchPath = session::options().userScratchPath();
   FilePath statePath = scratchPath.complete("persistent-state");
   return settings_.initialize(statePath);
}

std::string PersistentState::activeClientId()
{
   std::string activeClientId = settings_.get(kActiveClientId);
   if (!activeClientId.empty())
      return activeClientId;
   else
      return newActiveClientId();
}

std::string PersistentState::newActiveClientId() 
{
   std::string newId = core::system::generateUuid();
   settings_.set(kActiveClientId, newId);
   return newId;  
}

bool PersistentState::hadAbend() 
{ 
   return settings_.getInt(kAbend, false); 
}
   
void PersistentState::setAbend(bool abend) 
{ 
   settings_.set(kAbend, abend); 
}

FilePath PersistentState::nextSessionProjectPath() const
{
   std::string path = settings_.get(kNextSessionProjectPath);
   if (!path.empty())
      return FilePath(path);
   else
      return FilePath();
}

void PersistentState::setNextSessionProjectPath(
                           const FilePath& nextSessionProjectPath)
{
   settings_.set(kNextSessionProjectPath, nextSessionProjectPath.absolutePath());
}
   
} // namespace session
