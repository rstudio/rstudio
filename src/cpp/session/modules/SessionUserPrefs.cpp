/*
 * SessionUserPrefs.cpp
 *
 * Copyright (C) 2009-19 by RStudio, Inc.
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

#include <boost/bind/bind.hpp>

#include <core/Exec.hpp>
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

   return userPrefs().writeLayer(PREF_LAYER_USER, val.get_obj()); 
}

Error setState(const json::JsonRpcRequest& request,
               json::JsonRpcResponse* pResponse)
{
   json::Value val;
   Error error = json::readParams(request.params, &val);
   if (error)
      return error;

   userState().writeLayer(STATE_LAYER_USER, val.get_obj()); 

   module_context::events().onPreferencesSaved();

   return Success();
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
      r::exec::error("Unexpected value: " + error.summary());
      return false;
   }

   // if this corresponds to an existing preference, ensure that we're not 
   // changing its data type
   boost::optional<json::Value> previous = prefs.readValue(pref);
   if (previous)
   {
      if ((*previous).type() != prefValue.type())
      {
         r::exec::error("Type mismatch: expected " + 
                  json::typeAsString((*previous).type()) + "; got " +
                  json::typeAsString(prefValue.type()));
         return false;
      }
   }
   
   // write new pref value
   error = prefs.writeValue(pref, prefValue);
   if (error)
   {
      r::exec::error("Could not save preferences: " + error.description());
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
      r::exec::error("Could not save preferences: " + error.description());
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
   for (const auto key: keys) 
   {
      std::string layer;
      auto val = userPrefs().readValue(key, &layer);
      if (val)
      {
         sources.push_back(layer);

         std::ostringstream oss;
         json::write(*val, oss);
         values.push_back(oss.str());
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
      r::exec::error(error.description());
   }
   else
   {
      return frame;
   }

   return list;
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
      (bind(registerRpcMethod, "set_user_state", setState));
   return initBlock.execute();
}

} // namespace prefs
} // namespace modules
} // namespace session
} // namespace rstudio

