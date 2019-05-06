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
   error = readStringFromFile(prefsFile, &globalPrefsContents);
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
   // Ensure we have a layer cache; add empty slots for each
   if (!s_pLayers)
   {
      s_pLayers = boost::make_shared<json::Array>();
      for (unsigned i = 0; i < LAYER_MAX; i++)
      {
         s_pLayers->push_back(json::Value());
      }
   }

   // Is this layer already cached?
   else if ((*s_pLayers)[layer].type() == json::ObjectType)
   {
      return (*s_pLayers)[layer].get_obj();
   }

   // Compute requested layer
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
   
   // Cache result
   (*s_pLayers)[layer] = result; 

   return result;
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

   return Success();
}

} // namespace prefs
} // namespace modules
} // namespace session
} // namespace rstudio
