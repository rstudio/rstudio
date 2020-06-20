/*
 * RClientState.cpp
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

#include <r/session/RClientState.hpp>

#include <algorithm>

#include <boost/bind.hpp>
#include <boost/function.hpp>

#include <core/Log.hpp>
#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <core/FileSerializer.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace r {
namespace session {
    
namespace {
  
const char * const kTemporaryExt = ".temporary";
const char * const kPersistentExt = ".persistent";
const char * const kProjPersistentExt = ".pper";
   
void putState(const std::string& scope, 
              const std::string& entryName,
              const json::Value& entryValue,
              json::Object* pStateContainer)
{
   // get the scope object (create if it doesn't exist)
   json::Object::Iterator pos = pStateContainer->find(scope);
   if (pos == pStateContainer->end())
   {
      json::Object newScopeObject;
      pStateContainer->insert(scope, newScopeObject);
   }
   const json::Value& scopeValue = pStateContainer->operator[](scope);
   json::Object scopeObject = scopeValue.getObject();
   
   // insert the value into the scope
   scopeObject.insert(entryName, entryValue);
}

void putState(const std::string& scope,
              const json::Object::Member& entry,
              json::Object* pStateContainer)
{
   putState(scope, entry.getName(), entry.getValue(), pStateContainer);
}
   
void mergeStateScope(const json::Object::Member& scopeMember,
                     json::Object* pTargetState)
{
   const std::string& scope = scopeMember.getName();
   const json::Value& value = scopeMember.getValue();
   if ( value.isObject() )
   {
      const json::Object& stateObject = value.getObject();
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
   for (const json::Object::Member& member : stateContainer)
   {
      // generate json
      std::ostringstream ostr;
      member.getValue().writeFormatted(ostr);
      
      // write to file
      FilePath stateFile = stateDir.completePath(member.getName() + fileExt);
      Error error = writeStringToFile(stateFile, ostr.str());
      if (error)
         LOG_ERROR(error);
   }
}
   
void restoreState(const core::FilePath& stateFilePath,
                  json::Object* pStateContainer)
{
   // read the contents of the file
   std::string contents;
   Error error = readStringFromFile(stateFilePath, &contents);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
   
   // parse the json
   json::Value value;
   if ( value.parse(contents) )
   {
      LOG_ERROR_MESSAGE("Error parsing client state");
      return;
   }
   
   // write to the container 
   pStateContainer->insert(stateFilePath.getStem(), value);
}

Error removeAndRecreateStateDir(const FilePath& stateDir)
{
   Error error = stateDir.removeIfExists();
   if (error)
      return error;
   return stateDir.ensureDirectory();
}

Error restoreStateFiles(const FilePath& sourceDir,
                        boost::function<void(const FilePath&)> restoreFunc)
{
   // ignore if the directory doesn't exist
   if (!sourceDir.exists())
      return Success();

   // list the files
   std::vector<FilePath> childPaths;
   Error error = sourceDir.getChildren(childPaths);
   if (error)
      return error;

   // restore files
   std::for_each(childPaths.begin(), childPaths.end(), restoreFunc);
   return Success();
}

void appendAndValidateState(const json::Object& sourceState,
                            json::Object* pTargetState)
{
   // append (log warning if there are dupes)
   for (const json::Object::Member& member : sourceState)
   {
      if (pTargetState->find(member.getName()) != pTargetState->end())
         LOG_WARNING_MESSAGE("duplicate state key: " + member.getName());
      else
         pTargetState->insert(member);
   }
}


} // anonymous namespace
   
// singleton
ClientState& clientState()
{
   static ClientState instance;
   return instance;
}

   
ClientState::ClientState()
{
}

void ClientState::restoreGlobalState(const FilePath& stateFile)
{
   if (stateFile.getExtension() == kTemporaryExt)
      restoreState(stateFile, &temporaryState_);
   else if (stateFile.getExtension() == kPersistentExt)
      restoreState(stateFile, &persistentState_);
}

void ClientState::restoreProjectState(const FilePath& stateFile)
{
   if (stateFile.getExtension() == kProjPersistentExt)
      restoreState(stateFile, &projectPersistentState_);
}
   
void ClientState::clear()  
{
   temporaryState_.clear();
   persistentState_.clear();
   projectPersistentState_.clear();
}
 
void ClientState::putTemporary(const std::string& scope, 
                               const std::string& name,
                               const json::Value& value)
{
   json::Object stateContainer;
   putState(scope, name, value, &stateContainer);
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
   putState(scope, name, value, &stateContainer);
   putPersistent(stateContainer);
}

void ClientState::putPersistent(const json::Object& persistentState)
{
   mergeState(persistentState, &persistentState_);
}

void ClientState::putProjectPersistent(const std::string& scope,
                                       const std::string& name,
                                       const json::Value& value)
{
   json::Object stateContainer;
   putState(scope, name, value, &stateContainer);
   putProjectPersistent(stateContainer);
}

json::Value ClientState::getProjectPersistent(std::string scope,
                                              std::string name)
{
   json::Object::Iterator i = projectPersistentState_.find(scope);
   if (i == projectPersistentState_.end())
   {
      return json::Value();
   }
   else
   {
      if (!json::isType<core::json::Object>((*i).getValue()))
         return json::Value();
      json::Object scopeObject = (*i).getValue().getObject();
      return scopeObject[name].clone();
   }
}

void ClientState::putProjectPersistent(
                              const json::Object& projectPersistentState)
{
   mergeState(projectPersistentState, &projectPersistentState_);
}


Error ClientState::commit(ClientStateCommitType commitType, 
                          const core::FilePath& stateDir,
                          const core::FilePath& projectStateDir)
{
   // remove and re-create the stateDirs
   Error error = removeAndRecreateStateDir(stateDir);
   if (error)
      return error;
   error = removeAndRecreateStateDir(projectStateDir);
   if (error)
      return error;

   // always commit persistent state
   commitState(persistentState_, kPersistentExt, stateDir);
   commitState(projectPersistentState_, kProjPersistentExt, projectStateDir);
  
   // commit all state if requested
   if (commitType == ClientStateCommitAll)
      commitState(temporaryState_, kTemporaryExt, stateDir);
   else
      temporaryState_.clear();
   
   return Success();
}
   
Error ClientState::restore(const FilePath& stateDir,
                           const FilePath& projectStateDir)
{
   // clear existing values
   clear();
   
   // restore global state
   Error error = restoreStateFiles(
                  stateDir,
                  boost::bind(&ClientState::restoreGlobalState, this, _1));
   if (error)
      return error;

   // restore project state
   return restoreStateFiles(
                  projectStateDir,
                  boost::bind(&ClientState::restoreProjectState, this, _1));
}

// generate current state by merging temporary and persistent states
void ClientState::currentState(json::Object* pCurrentState) const
{
   // start with copy of persistent state
   pCurrentState->clear();
   *pCurrentState = persistentState_;
   
   // add and validate other state collections
   appendAndValidateState(projectPersistentState_, pCurrentState);
   appendAndValidateState(temporaryState_, pCurrentState);
}

} // namespace session
} // namespace r
} // namespace rstudio

