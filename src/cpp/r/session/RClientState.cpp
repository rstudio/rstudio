/*
 * RClientState.cpp
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

#include <r/session/RClientState.hpp>

#include <algorithm>

#include <boost/bind.hpp>
#include <boost/function.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>

using namespace core;

namespace r {
namespace session {
    
namespace {
  
const char * const kTemporaryExt = ".temporary";
const char * const kPersistentExt = ".persistent";
   
void putState(const std::string& scope, 
              const json::Object::value_type& entry,
              json::Object* pStateContainer)
{
   // get the scope object (create if it doesn't exist)
   json::Object::iterator pos = pStateContainer->find(scope);
   if (pos == pStateContainer->end())
   {
      json::Object newScopeObject;
      pStateContainer->insert(std::make_pair(scope, newScopeObject));
   }
   json::Value& scopeValue = pStateContainer->operator[](scope); 
   json::Object& scopeObject = scopeValue.get_obj();
   
   // insert the value into the scope
   scopeObject[entry.first] = entry.second;
}
   
void mergeStateScope(const json::Object::value_type& scopePair,
                     json::Object* pTargetState)
{
   const std::string& scope = scopePair.first;
   const json::Value& value = scopePair.second;
   if ( value.type() == json::ObjectType )
   {
      const json::Object& stateObject = value.get_obj();
      std::for_each(stateObject.begin(),
                    stateObject.end(),
                    boost::bind(putState, scope, _1, pTargetState));
   }
   else
   {
      LOG_WARNING_MESSAGE("set_client_state call sent non json object data");
   }
}
                     
   
void mergeState(const json::Object& sourceState,
                json::Object* pTargetState)
{
   std::for_each(sourceState.begin(), 
                 sourceState.end(),
                 boost::bind(mergeStateScope, _1, pTargetState));
}

void commitState(const json::Object& stateContainer,
                 const std::string& fileExt,
                 const core::FilePath& stateDir)
{
   for (json::Object::const_iterator
        it = stateContainer.begin(); it != stateContainer.end(); ++it)
   {
      // generate json
      std::ostringstream ostr ;
      json::writeFormatted(it->second, ostr);
      
      // write to file
      FilePath stateFile = stateDir.complete(it->first + fileExt);
      Error error = writeStringToFile(stateFile, ostr.str());
      if (error)
         LOG_ERROR(error);   
   }
}
   
void restoreState(const core::FilePath& stateFilePath,
                  json::Object* pStateContainer)
{
   // read the contents of the file
   std::string contents ;
   Error error = readStringFromFile(stateFilePath, &contents);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
   
   // parse the json
   json::Value value;
   if ( !json::parse(contents, &value) )
   {
      LOG_ERROR_MESSAGE("Error parsing client state");
      return;
   }
   
   // write to the container 
   pStateContainer->insert(std::make_pair(stateFilePath.stem(), value));
}

}
   
// singleton
ClientState& clientState()
{
   static ClientState instance;
   return instance;
}

   
ClientState::ClientState()
{
}
   
void ClientState::clear()  
{
   temporaryState_.clear();
   persistentState_.clear();
}
 
void ClientState::putTemporary(const std::string& scope, 
                               const std::string& name,
                               const json::Value& value)
{
   json::Object stateContainer ;
   putState(scope, std::make_pair(name, value), &stateContainer);
   putTemporary(stateContainer);
}
   
void ClientState::putTemporary(const json::Object& temporaryState)
{
   mergeState(temporaryState, &temporaryState_);
}

void ClientState::putPersistent(const std::string& scope, 
                                const std::string& name,
                                const json::Value& value)
{
   json::Object stateContainer;
   putState(scope, std::make_pair(name, value), &stateContainer);
   putPersistent(stateContainer);
}

void ClientState::putPersistent(const json::Object& persistentState)
{
   mergeState(persistentState, &persistentState_);
}

Error ClientState::commit(ClientStateCommitType commitType, 
                          const core::FilePath& stateDir)
{
   // remote and re-create the stateDir
   Error error = stateDir.removeIfExists();
   if (error)
      return error;
   error = stateDir.ensureDirectory();
   if (error)
      return error;
   
   // always commit persistent state
   commitState(persistentState_, kPersistentExt, stateDir);
  
   // commit all state if requested
   if (commitType == ClientStateCommitAll)
      commitState(temporaryState_, kTemporaryExt, stateDir);
   else
      temporaryState_.clear();
   
   return Success();
}
   
   
Error ClientState::restore(const FilePath& stateDir)
{
   // clear existing values
   clear();
   
   // if the directory doesn't exist then simply exit
   if (!stateDir.exists())
      return Success();
   
   // list the files
   std::vector<FilePath> childPaths ;
   Error error = stateDir.children(&childPaths);
   if (error)
      return error ;
   
   // restore each file
   for (std::vector<FilePath>::const_iterator it = childPaths.begin();
        it != childPaths.end();
        it++)
   {
      if (it->extension() == kTemporaryExt)
         restoreState(*it, &temporaryState_);
      else if (it->extension() == kPersistentExt)
         restoreState(*it, &persistentState_);
      else
         LOG_WARNING_MESSAGE("unexpected client state extension: " +
                             it->extension());
   }
   
   return Success();
}
   
// generate current state by merging temporary and persistent states
void ClientState::currentState(json::Object* pCurrentState) const
{
   // start with copy of persistent state
   pCurrentState->clear();
   pCurrentState->insert(persistentState_.begin(), persistentState_.end());
   
   // add items from temporary state (log warning if there are dupes)
   for (json::Object::const_iterator it = temporaryState_.begin();
        it != temporaryState_.end();
        ++it)
   {
      if (pCurrentState->find(it->first) != pCurrentState->end())
         LOG_WARNING_MESSAGE("duplicate state key: " + it->first);
      else
         pCurrentState->insert(*it);
   }
}

   
   
      
} // namespace session
} // namespace r

