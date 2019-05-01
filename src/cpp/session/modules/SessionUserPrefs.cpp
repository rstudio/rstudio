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

#include <core/system/Xdg.hpp>
#include <core/FileSerializer.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/json/rapidjson/schema.h>

#include <core/Exec.hpp>

#include <session/SessionOptions.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/projects/SessionProjects.hpp>

#include "SessionUserPrefs.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace prefs {
namespace {

static boost::shared_ptr<json::Array> s_pLayers;
static core::FilePath s_userPrefsFile;

Error setPreference(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   /*
   std::string key;
   json::Value val;
   Error error = json::readParams(request.params, &key, &val);
   if (error)
      return error;

   auto it = s_pUserPrefs->find(key);
   if (it != s_pUserPrefs->end() && (*it).value() == val)
   {
      // no work necessary if preference value is unchanged
      return Success();
   }

   // save preference value
   (*s_pUserPrefs)[key] = val;
   */

   // TODO: need to write changes. how to compute which changes to write? don't want to write
   // defaults. maybe need a json method to compute intersection? sort of like merge... open
   // question: do we want to diff against the defaults or against the system+defaults?

   return Success();
}

Error getSchemaContents(std::string *contents)
{
   FilePath schemaFile = 
      options().rResourcesPath().complete("schema").complete("user-prefs-schema.json");
   return readStringFromFile(schemaFile, contents);
}

Error getPrefDefaults(json::Object* layer)
{
   std::string schemaContents;
   Error error = getSchemaContents(&schemaContents);
   if (error)
      return error;

   return json::getSchemaDefaults(schemaContents, layer);
}

Error loadPrefsFromFile(const FilePath& prefsFile, json::Object* layer)
{
   std::string schemaContents;
   Error error = getSchemaContents(&schemaContents);
   if (error)
      LOG_ERROR(error);

   std::string globalPrefsContents;
   error = readStringFromFile(globalPrefsFile, &globalPrefsContents);
   if (error)
   {
      return error;
   }
   else
   {
      // We have a global preferences file; ensure it's valid.
      json::Value globalPrefs;
      if (schemaContents.empty())
      {
         // Shouldn't happen, but if we couldn't read the schema, we can still parse without it.
         error = json::parse(globalPrefsContents, ERROR_LOCATION, &globalPrefs);
      }
      else
      {
         // Parse and validate against the schema.
         error = json::parseAndValidate(globalPrefsContents, schemaContents, ERROR_LOCATION, 
               &globalPrefs);
      }
      if (error)
      {
         return error;
      }
      else if (globalPrefs.type() == json::ObjectType)
      {
         *layer = globalPrefs.get_obj();
      }
   }
   return Success();
}

} // anonymous namespace


json::Object getLayer(PrefLayer layer)
{
   // Is this layer already cached?
   if (s_pLayers->size() > layer)
      return (*s_pLayers)[layer].get_obj();

   json::Object result;
   Error error;
   switch(layer)
   {
      case LAYER_DEFAULT:
         error = getPrefDefaults(&result);
         break;

      case LAYER_SYSTEM:
         error = loadPrefsFromFile(
               core::system::xdg::systemConfigDir().complete(kUserPrefsFile),
               &result);
         break;

      case LAYER_USER:
         error = loadPrefsFromFile(
               core::system::xdg::userConfigDir().complete(kUserPrefsFile),
               &result);
         break;

      case LAYER_PROJECT:
         if (projects::projectContext().hasProject())
         {
            result = projects::projectContext().uiPrefs();
         }
         break;
   }
   
   // Ensure there's space for this layer
   if (s_pLayers->size())
   
}

Error initialize()
{
   using namespace module_context;

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "set_preference", setPreference));
   Error error = initBlock.execute();
   if (error)
      return error;

   // Load schema for validation
   FilePath schemaFile = 
      options().rResourcesPath().complete("schema").complete("user-prefs-schema.json");
   std::string schemaContents;
   error = readStringFromFile(schemaFile, &schemaContents);
   if (error)
      return error;

   s_pLayers = boost::make_shared<json::Array>();

   // Layer 0: Defaults -------------------------------------------------------
   //
   // Extract default values from schema
   json::Object defaults;
   error = json::getSchemaDefaults(schemaContents, &defaults);
   if (error)
      return error;

   s_pLayers->push_back(defaults);

   // Layer 1: System ---------------------------------------------------------
   //
   // If there's a system-wide configuration file, load that first.
   FilePath globalPrefsFile = core::system::xdg::systemConfigDir().complete(kUserPrefsFile);
   if (globalPrefsFile.exists())
   {
      std::string globalPrefsContents;
      error = readStringFromFile(globalPrefsFile, &globalPrefsContents);
      if (error)
      {
         // Non-fatal; we will proceed with defaults and/or user prefs.
         LOG_ERROR(error);
      }
      else
      {
         // We have a global preferences file; ensure it's valid.
         json::Value globalPrefs;
         error = json::parseAndValidate(globalPrefsContents, schemaContents, ERROR_LOCATION, 
               &globalPrefs);
         if (error)
         {
            LOG_ERROR(error);
         }
         else if (globalPrefs.type() == json::ObjectType)
         {
            s_pLayers->push_back(globalPrefs);
         }
      }
   }

   // If we didn't get a system-wide config, push an empty layer.
   if (s_pLayers->size() == LAYER_SYSTEM)
   {
      s_pLayers->push_back(json::Object());
   }

   // Layer 2: User -----------------------------------------------------------
   //
   // Load the user prefs file.
   FilePath prefsFile = core::system::xdg::userConfigDir().complete(kUserPrefsFile);
   if (prefsFile.exists())
   {
      std::string prefsContents;
      error = readStringFromFile(prefsFile, &prefsContents);
      if (error)
      {
         // Don't fail here since it will cause startup to fail, we'll just live with no prefs.
         LOG_ERROR(error);
      }
      else
      {
         json::Value prefs;
         error = json::parseAndValidate(prefsContents, schemaContents, ERROR_LOCATION, &prefs);
         if (error)
         {
            // Invalid or non-conforming user prefs; use defaults.
            LOG_ERROR(error);
         }
         else
         {
            s_pLayers->push_back(prefs);
         }
      }
   }

   if (s_pLayers->size() <= LAYER_USER)
   {
      s_pLayers->push_back(json::Object());
   }

   // Layer 2: User -----------------------------------------------------------
   //

   return Success();
}

} // namespace prefs
} // namespace modules
} // namespace session
} // namespace rstudio
