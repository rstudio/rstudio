/*
 * SessionWorkspace.cpp
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

Error getSaveAction(const json::JsonRpcRequest&,
                    json::JsonRpcResponse* pResponse)
{  
   json::Object saveAction;
   if (r::session::imageIsDirty())
   {
      saveAction["action"] = userSettings().saveAction();
   }
   else
   {
      saveAction["action"] = r::session::kSaveActionNoSave;
   }

   pResponse->setResult(saveAction);

   return Success();
}

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
   SEXP globalVar = findVar(name);
   if (globalVar != R_UnboundValue)
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
}
 
void onDetectChanges(module_context::ChangeSource source)
{
   s_globalEnvironmentMonitor.checkForChanges();
}

} // anonymous namespace
 
Error initialize()
{         
   // subscribe to events
   using boost::bind;
   using namespace session::module_context;
   events().onClientInit.connect(bind(onClientInit));
   events().onDetectChanges.connect(bind(onDetectChanges, _1));
   
   // register handlers
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "get_save_action", getSaveAction))
      (bind(registerRBrowseFileHandler, handleRBrowseEnv))
      (bind(sourceModuleRFile, "SessionWorkspace.R"));
   return initBlock.execute();
}
   

} // namepsace workspace
} // namespace modules
} // namesapce session

