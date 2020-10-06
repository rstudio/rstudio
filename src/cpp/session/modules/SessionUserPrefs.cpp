/*
 * SessionUserPrefs.cpp
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

#include "SessionUserPrefs.hpp"
#include "SessionApiPrefs.hpp"
#include "SessionUserPrefsMigration.hpp"

#include <boost/bind/bind.hpp>

#include <core/Exec.hpp>

#include <core/system/Xdg.hpp>

#include <session/prefs/UserPrefs.hpp>
#include <session/prefs/UserState.hpp>
#include <session/SessionModuleContext.hpp>

#include <r/RExec.hpp>
#include <r/RSexp.hpp>
#include <r/ROptions.hpp>
#include <r/RRoutines.hpp>
#include <r/RJson.hpp>

using namespace rstudio::core;
using namespace rstudio::session::prefs;

namespace rstudio {
namespace session {
namespace modules {
namespace prefs {
namespace {

ApiPrefs& apiPrefs()
{
   static ApiPrefs instance;
   return instance;
}

Error setPreferences(const json::JsonRpcRequest& request,
                     json::JsonRpcResponse* pResponse)
{
   json::Value val;
   Error error = json::readParams(request.params, &val);
   if (error)
      return error;

   return userPrefs().writeLayer(PREF_LAYER_USER, val.getObject());
}

Error setState(const json::JsonRpcRequest& request,
               json::JsonRpcResponse* pResponse)
{
   json::Value val;
   Error error = json::readParams(request.params, &val);
   if (error)
      return error;

   userState().writeLayer(STATE_LAYER_USER, val.getObject());

   module_context::events().onPreferencesSaved();

   return Success();
}

Error editPreferences(const json::JsonRpcRequest& ,
                      json::JsonRpcResponse*)
{
   // Invoke an editor on the user-level config file
   r::exec::RFunction editor(".rs.editor");
   editor.addParam("file",
         core::system::xdg::userConfigDir().completePath(kUserPrefsFile).getAbsolutePath());
   return editor.call();
}

Error clearPreferences(const json::JsonRpcRequest& ,
                       json::JsonRpcResponse* pResponse)
{
   json::Value result;
   FilePath prefsFile = 
      core::system::xdg::userConfigDir().completePath(kUserPrefsFile);
   if (!prefsFile.exists())
   {
      // No prefs file = no work to do
      pResponse->setResult(result);
      return Success();
   }

   // Create a backup path for the old prefs so they can be restored
   FilePath backup;
   Error error = FilePath::uniqueFilePath(prefsFile.getParent().getAbsolutePath(), ".json", backup);
   if (error)
   {
      pResponse->setResult(result);
      return error;
   }

   // Move the prefs to the backup location
   error = prefsFile.move(backup);
   if (error)
   {
      pResponse->setResult(result);
      return error;
   }

   // Return the backup filename to the client
   result = backup.getAbsolutePath();
   pResponse->setResult(result);
   return Success();
}

Error viewPreferences(const json::JsonRpcRequest&,
                       json::JsonRpcResponse*)
{
    return r::exec::executeString("View(.rs.allPrefs())");
}

bool writePref(Preferences& prefs, SEXP prefName, SEXP value)
{
   json::Value prefValue = json::Value();

   // extract name of preference to write
   std::string pref = r::sexp::safeAsString(prefName, "");
   if (pref.empty())
      return false;

   // extract value to write
   Error error = r::json::jsonValueFromObject(value, &prefValue);
   if (error)
   {
      r::exec::error("Unexpected value: " + error.getSummary());
      return false;
   }

   // if this corresponds to an existing preference, ensure that we're not 
   // changing its data type
   boost::optional<json::Value> previous = prefs.readValue(pref);
   if (previous)
   {
      if ((*previous).getType() != prefValue.getType())
      {
         r::exec::error("Type mismatch: expected " + 
                  json::typeAsString((*previous).getType()) + "; got " +
                  json::typeAsString(prefValue.getType()));
         return false;
      }
   }
   
   // write new pref value
   error = prefs.writeValue(pref, prefValue);
   if (error)
   {
      r::exec::error("Could not save preferences: " + error.asString());
      return false;
   }

   // let other modules know we've updated the prefs
   module_context::events().onPreferencesSaved();
   
   return true;
}

SEXP rs_removePref(SEXP prefName)
{
   // extract name of preference to remove
   std::string pref = r::sexp::safeAsString(prefName, "");
   if (pref.empty())
      return R_NilValue;
   
   Error error = userPrefs().clearValue(pref);
   if (error)
   {
      r::exec::error("Could not save preferences: " + error.asString());
   }

   module_context::events().onPreferencesSaved();

   return R_NilValue;
}

SEXP rs_readPref(Preferences& prefs, SEXP prefName)
{
   r::sexp::Protect protect;

   // extract name of preference to read
   std::string pref = r::sexp::safeAsString(prefName, "");
   if (pref.empty())
      return R_NilValue;

   auto prefValue = prefs.readValue(pref);
   if (prefValue)
   {
      // convert to SEXP and return
      return r::sexp::create(*prefValue, &protect);
   }

   // No preference found with this name
   return R_NilValue;
}

SEXP rs_readUserPref(SEXP prefName)
{
   return rs_readPref(userPrefs(), prefName);
}

SEXP rs_writeUserPref(SEXP prefName, SEXP value)
{
   if (writePref(userPrefs(), prefName, value))
   {
      userPrefs().notifyClient(kUserPrefsUserLayer, r::sexp::safeAsString(prefName));
   }

   return R_NilValue;
}

SEXP rs_readApiPref(SEXP prefName)
{
   return rs_readPref(apiPrefs(), prefName);
}

SEXP rs_writeApiPref(SEXP prefName, SEXP value)
{
   writePref(apiPrefs(), prefName, value);
   return R_NilValue;
}

SEXP rs_readUserState(SEXP stateName)
{
   return rs_readPref(userState(), stateName);
}

SEXP rs_writeUserState(SEXP stateName, SEXP value)
{
   if (writePref(userState(), stateName, value))
   {
      userState().notifyClient(kUserStateUserLayer, r::sexp::safeAsString(stateName));
   }

   return R_NilValue;
}

SEXP rs_allPrefs()
{
   r::sexp::Protect protect;

   // Create table of all preferences
   std::vector<std::string> keys = userPrefs().allKeys();
   std::vector<std::string> sources;
   std::vector<std::string> values;

   // Sort preference keys alphabetically for convenience
   std::sort(keys.begin(), keys.end());

   for (const auto& key : keys) 
   {
      std::string layer;
      auto val = userPrefs().readValue(key, &layer);
      if (val)
      {
         sources.push_back(layer);
         values.push_back(val->write());
      }
      else
      {
         sources.push_back("none");
         values.push_back("");
      }
   }
   
   // Convert to data frame
   std::vector<std::string> names({"Preference", "Source", "Value"});
   SEXP list = r::sexp::createList(names, &protect);
   SET_VECTOR_ELT(list, 0, r::sexp::create(keys, &protect));
   SET_VECTOR_ELT(list, 1, r::sexp::create(sources, &protect));
   SET_VECTOR_ELT(list, 2, r::sexp::create(values, &protect));
   r::exec::RFunction asDataFrame("as.data.frame");
   asDataFrame.addParam("x", list);
   asDataFrame.addParam("stringsAsFactors", false);
   SEXP frame = R_NilValue;
   Error error = asDataFrame.call(&frame, &protect);
   if (error)
   {
      r::exec::error(error.asString());
   }
   else
   {
      return frame;
   }

   return list;
}

/**
 * One-time migration of user preferences from the user-settings file used in RStudio 1.2 and below
 * to the formal preferences system in RStudio 1.3.
 */
Error migrateUserPrefs()
{
   // Check to see whether there's a preferences file at the new location
   FilePath prefsFile = core::system::xdg::userConfigDir().completePath(kUserPrefsFile);
   if (prefsFile.exists())
   {
      // We already have prefs; don't try to overwrite them
      return Success();
   }

   // Check to see whether there's a preferences file at the old location
   FilePath userSettings = module_context::userScratchPath()
      .completePath(kMonitoredPath)
      .completePath("user-settings")
      .completePath("user-settings");

   if (userSettings.exists())
   {
      // There are no new prefs, but there are old prefs. Migrate!
      return migratePrefs(userSettings);
   }

   // No work to do
   return Success();
}

void onShutdown(bool terminatedNormally)
{
   // Forcibly destroy pref layers when shutting down, since some shutdown processes (namely
   // unregistering the file monitor) should be performed before memory is automatically released
   userPrefs().destroyLayers();
}

void onSuspend(core::Settings*)
{
   // Treat suspends as a shutdown (destroy layers)
   onShutdown(true);
}

void onResume(const core::Settings&)
{
   // No action required here; stub exists since we always register suspend/resume as
   // a pair
}

} // anonymous namespace

core::Error initialize()
{
   // Initialize computed preference layers
   using namespace module_context;
   Error error = initializeSessionPrefs();
   if (error)
      return error;

   // Initialize prefs for the RStudio API
   error = apiPrefs().initialize();
   if (error)
      return error;

   // Migrate user preferences from older versions of RStudio if they exist (and we don't have prefs
   // yet)
   error = migrateUserPrefs();
   if (error)
   {
      // This error is non-fatal (we'll just start with clean prefs if we cannot migrate)
      LOG_ERROR(error);
   }
   
   // Register handlers for session suspend/shutdown
   events().onShutdown.connect(onShutdown);
   addSuspendHandler(SuspendHandler(boost::bind(onSuspend, _2), onResume));

   RS_REGISTER_CALL_METHOD(rs_readUserPref);
   RS_REGISTER_CALL_METHOD(rs_writeUserPref);
   RS_REGISTER_CALL_METHOD(rs_readApiPref);
   RS_REGISTER_CALL_METHOD(rs_writeApiPref);
   RS_REGISTER_CALL_METHOD(rs_readUserState);
   RS_REGISTER_CALL_METHOD(rs_writeUserState);
   RS_REGISTER_CALL_METHOD(rs_allPrefs);
   RS_REGISTER_CALL_METHOD(rs_removePref);

   // Ensure we have a context ID
   if (userState().contextId().empty())
      userState().setContextId(core::system::generateShortenedUuid());

   using boost::bind;

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "set_user_prefs", setPreferences))
      (bind(registerRpcMethod, "set_user_state", setState))
      (bind(registerRpcMethod, "edit_user_prefs", editPreferences))
      (bind(registerRpcMethod, "clear_user_prefs", clearPreferences))
      (bind(registerRpcMethod, "view_all_prefs", viewPreferences));
   return initBlock.execute();
}

} // namespace prefs
} // namespace modules
} // namespace session
} // namespace rstudio

