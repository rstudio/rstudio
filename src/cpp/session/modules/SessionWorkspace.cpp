/*
 * SessionWorkspace.cpp
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

#include "SessionWorkspace.hpp"

#include <algorithm>

#include <boost/bind.hpp>
#include <boost/format.hpp>
#include <boost/utility.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/Exec.hpp>
#include <core/FilePath.hpp>

#include <core/json/JsonRpc.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>
#include <r/RErrorCategory.hpp>
#include <r/session/RSession.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionUserSettings.hpp>

using namespace core ;
using namespace r::sexp;
using namespace r::exec;

namespace session {
namespace modules {
namespace workspace {

namespace {

bool handleRBrowseEnv(const core::FilePath& filePath)
{
   if (filePath.filename() == "wsbrowser.html")
   {
      module_context::showContent("R objects", filePath);
      return true;
   }
   else
   {
      return false;
   }
}

json::Value classOfGlobalVar(SEXP globalVar)
{
   std::string value;
   Error error = r::exec::RFunction(".rs.getSingleClass",
                                    globalVar).call(&value);
   if (error)
   {
      LOG_ERROR(error);
      return json::Value(); // return null
   }
   else
   {
      return value;
   }
}

json::Value valueOfGlobalVar(SEXP globalVar)
{
   std::string value;
   Error error = r::exec::RFunction(".rs.valueAsString",
                                    globalVar).call(&value);
   if (error)
   {
      LOG_ERROR(error);
      return json::Value(); // return null
   }
   else
   {
      return value;
   }
}

json::Value descriptionOfGlobalVar(SEXP globalVar)
{
   std::string value;
   Error error = r::exec::RFunction(".rs.valueDescription",
                                    globalVar).call(&value);
   if (error)
   {
      LOG_ERROR(error);
      return json::Value(); // return null
   }
   else
   {
      return value;
   }
}

json::Value jsonValueForGlobalVar(const std::string& name)
{
   json::Object jsonObject ;
   jsonObject["name"] = name;
   
   // get R alias to object and get its type and lengt
   //
   // NOTE: check for isLanguage is a temporary fix for error messages
   // that were printed at the console for a <- bquote(test()) -- this
   // was the result of errors being thrown from the .rs.valueDescription, etc.
   // calls above used to probe for object info. interestingly when these
   // same calls are made from .rs.rpc.list_objects no errors are thrown.
   // the practical impact of this workaround is that immediately after
   // assignment language expressions show up as "(unknown)" but then are
   // correctly displayed in refreshed listings of the workspace.
   //
   SEXP globalVar = findVar(name);
   if ((globalVar != R_UnboundValue) && !r::sexp::isLanguage(globalVar))
   {
      Protect rProtect(globalVar);
      jsonObject["type"] = classOfGlobalVar(globalVar);
      jsonObject["len"] = length(globalVar);
      jsonObject["value"] = valueOfGlobalVar(globalVar);
      jsonObject["extra"] = descriptionOfGlobalVar(globalVar);
   }
   else
   {
      jsonObject["type"] = std::string("<unknown>");
      jsonObject["len"] = (int)0;
      jsonObject["value"] = json::Value(); // null
      jsonObject["extra"] = json::Value(); // null
   }
   
   return jsonObject;
}

void enqueRefreshEvent()
{
   ClientEvent refreshEvent(client_events::kWorkspaceRefresh);
   module_context::enqueClientEvent(refreshEvent);
}

void enqueRemovedEvent(const r::sexp::Variable& variable)   
{
   ClientEvent removedEvent(client_events::kWorkspaceRemove, variable.first);
   module_context::enqueClientEvent(removedEvent);
}

void enqueAssignedEvent(const r::sexp::Variable& variable)
{   
   // get object info
   json::Value objInfo = jsonValueForGlobalVar(variable.first);
   
   // enque event
   ClientEvent assignedEvent(client_events::kWorkspaceAssign, objInfo);
   module_context::enqueClientEvent(assignedEvent);
}

// last save action.
// NOTE: we don't persist this (or the workspace dirty state) during suspends in
// server mode. this means that if you are ever suspended then you will always
// end up with a 'dirty' workspace. not a big deal considering how infrequently
// quit occurs in server mode.
// TODO: this now affects switching projects after a suspend. we should try
// to figure out how to preserve dirty state of the workspace accross suspend
int s_lastSaveAction = r::session::kSaveActionAsk;

const char * const kSaveActionState = "saveActionState";
const char * const kImageDirtyState = "imageDirtyState";

void enqueSaveActionChanged()
{
   json::Object saveAction;
   saveAction["action"] = s_lastSaveAction;
   ClientEvent event(client_events::kSaveActionChanged, saveAction);
   module_context::enqueClientEvent(event);
}

void checkForSaveActionChanged()
{
   // compute current save action
   int currentSaveAction = r::session::imageIsDirty() ?
                                 module_context::saveWorkspaceAction() :
                                 r::session::kSaveActionNoSave;

   // compare and fire event if necessary
   if (s_lastSaveAction != currentSaveAction)
   {
      s_lastSaveAction = currentSaveAction;
      enqueSaveActionChanged();
   }
}

void onSuspend(Settings* pSettings)
{
   pSettings->set(kSaveActionState, s_lastSaveAction);
   pSettings->set(kImageDirtyState, r::session::imageIsDirty());
}

void onResume(const Settings& settings)
{
   s_lastSaveAction = settings.getInt(kSaveActionState,
                                      r::session::kSaveActionAsk);

   r::session::setImageDirty(settings.getBool(kImageDirtyState, true));

   enqueSaveActionChanged();
}


// detect changes in the environment by inspecting the list of variable
// names as well as the SEXP pointers (a new pointer implies a mutation of
// an object)
class GlobalEnvironmentMonitor : boost::noncopyable
{
public:
   GlobalEnvironmentMonitor() 
      : initialized_(false) 
   {
   }
   
   void reset()
   {
      initialized_ = false;
      lastEnv_.clear();
   }
   
   void checkForChanges()
   {
      // get the current environment
      std::vector<r::sexp::Variable> currentEnv ;
      listEnvironment(&currentEnv);
      
      // force refresh event the first time
      if (!initialized_)
      {
         enqueRefreshEvent();
         initialized_ = true;
      }
      
      // if there are changes
      else if (currentEnv != lastEnv_)
      {      
         // optimize for empty currentEnv (user reset workspace) or empty 
         // lastEnv_ (startup) by just sending a single WorkspaceRefresh event
         if (currentEnv.empty() || lastEnv_.empty())
         {
            enqueRefreshEvent();
         }
         else
         {
            // find deletes (all variable names in the previous environment but
            // NOT in the current environment)
            std::vector<Variable> removedVars ;
            std::set_difference(lastEnv_.begin(), lastEnv_.end(),
                                currentEnv.begin(), currentEnv.end(),
                                std::back_inserter(removedVars),
                                boost::bind(
                                  &GlobalEnvironmentMonitor::compareVarName,
                                  this, _1, _2));
                                
            // fire removed event for deletes
            std::for_each(removedVars.begin(), 
                          removedVars.end(), 
                          enqueRemovedEvent);
            
            // find adds & assigns (all variable name/value combinations in the 
            // current environment but NOT in the previous environment)
            std::vector<Variable> addedVars ;
            std::set_difference(currentEnv.begin(), currentEnv.end(),
                                lastEnv_.begin(), lastEnv_.end(),
                                std::back_inserter(addedVars));
                                
            // fire assigned event for adds & assigns
            std::for_each(addedVars.begin(), 
                          addedVars.end(), 
                          enqueAssignedEvent);
         }
      }
      
      // set the "last environment" list to the current env
      // note that the SEXP values within the currentEnv are not protected
      // beyond the scope of this call. this is OK because we only reference
      // the pointer values not the underlying R objects. if we want to be
      // able to manipulate the SEXPs directly we'll need a static protection
      // context so the objects are guaranteed to survive until the next call
      lastEnv_ = currentEnv;
   }
   
private:
   
   void listEnvironment(std::vector<r::sexp::Variable>* pEnvironment)
   {
      // get the variables currently in the global environment (note this list
      // is guaranteed to be sorted based on the behavior of R_lsInternal)
      r::sexp::Protect rProtect;
      r::sexp::listEnvironment(R_GlobalEnv, false, &rProtect, pEnvironment);   
   }
   
   // helper to deterine whether two variables have the same name
   bool compareVarName(const Variable& var1, const Variable& var2)
   {
      return var1.first < var2.first ;
   }
   
private:
   std::vector<r::sexp::Variable> lastEnv_; 
   bool initialized_ ;
};

// global environment monitor
GlobalEnvironmentMonitor s_globalEnvironmentMonitor;
   
void onClientInit()
{
   // reset monitor and check for changes for brand new client
   s_globalEnvironmentMonitor.reset();
   s_globalEnvironmentMonitor.checkForChanges();

   // enque save action changed
   enqueSaveActionChanged();
}
 
void onDetectChanges(module_context::ChangeSource source)
{
   // check global environment
   s_globalEnvironmentMonitor.checkForChanges();

   // check for save action changed
   checkForSaveActionChanged();
}

} // anonymous namespace
 
Error initialize()
{         
   // add suspend handler
   using namespace session::module_context;
   addSuspendHandler(SuspendHandler(boost::bind(onSuspend, _2), onResume));

   // subscribe to events
   using boost::bind;
   events().onClientInit.connect(bind(onClientInit));
   events().onDetectChanges.connect(bind(onDetectChanges, _1));
   
   // register handlers
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRBrowseFileHandler, handleRBrowseEnv))
      (bind(sourceModuleRFile, "SessionWorkspace.R"));
   return initBlock.execute();
}
   

} // namepsace workspace
} // namespace modules
} // namesapce session

