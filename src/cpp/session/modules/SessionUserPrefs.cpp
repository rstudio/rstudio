/*
 * SessionUserPrefs.hpp
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

#include <session/SessionOptions.hpp>

#include "SessionUserPrefs.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace prefs {
namespace {

static boost::shared_ptr<json::Object> s_pUserPrefs;

}

json::Object userPrefs()
{
   if (s_pUserPrefs)
      return *s_pUserPrefs;
   return json::Object();
}

Error initialize()
{
   // Load schema for validation
   FilePath schemaFile = 
      options().rResourcesPath().complete("prefs").complete("user-prefs-schema.json");
   std::string schemaContents;
   Error error = readStringFromFile(schemaFile, &schemaContents);
   if (error)
      return error;
   rapidjson::Document sd;
   if (sd.Parse(schemaContents).HasParseError())
   {
      return Error(json::errc::ParseError, ERROR_LOCATION);
   }

   // TODO: check for version-specific file first
   FilePath prefsFile = core::system::xdg::userConfigDir().complete(kUserPrefsFile);
   if (!prefsFile.exists())
      return Success();

   // TODO: validate version stored in file
   std::string prefsContents;
   error = readStringFromFile(prefsFile, &prefsContents);
   if (error)
   {
      // don't fail here since it will cause startup to fail, we'll just live with no prefs
      LOG_ERROR(error);
      return Success();
   }

   // Parse the user preferences
   rapidjson::Document pd;
   if (pd.Parse(prefsContents).HasParseError())
   {
      return Error(json::errc::ParseError, ERROR_LOCATION);
   }

   // Validate the user prefs according to the schema
   rapidjson::SchemaDocument schema(sd);
   rapidjson::SchemaValidator validator(schema);
   if (!pd.Accept(validator))
   {
      rapidjson::StringBuffer sb;
      validator.GetInvalidSchemaPointer().StringifyUriFragment(sb);

      error = Error(json::errc::ParseError, ERROR_LOCATION);
      error.addProperty("schema", sb.GetString());
      error.addProperty("keyword", validator.GetInvalidSchemaKeyword());
      return error;
   }

   s_pUserPrefs = boost::make_shared<json::Object>();

   // Iterate over every known preference value (which are exhaustively enumerated in the schema
   // document) and read the value from the user prefs file if present
   /*
   for (auto & it: sd["properties"].GetObject())
   {
      // Read the name of the preference
      std::string prefName(it.name.GetString());

      // See if the preference has a user-defined value
      auto userPref = pd.FindMember(prefName);
      rapidjson::Value val;
      if (pd != pd.MemberEnd())
      {
         // It has a user-defined value; use it
         val = userPref->value;
      }
      else
      {
         // No user defined value; use the default from the schema if we can find it.
         auto pref = it.value;
         if (pref.HasMember("default"))
         {
            val = pref["default"];
         }
      }
      s_pUserPrefs[prefName] = val;
   }
   */

   return Success();
}

} // namespace prefs
} // namespace modules
} // namespace session
} // namespace rstudio
