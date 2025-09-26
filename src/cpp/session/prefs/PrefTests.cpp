/*
 * UserPrefTests.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "UserStateDefaultLayer.hpp"
#include "UserPrefsDefaultLayer.hpp"
#include <session/prefs/UserState.hpp>
#include <session/prefs/UserPrefs.hpp>

#include <session/SessionOptions.hpp>

#include <core/FileSerializer.hpp>

#include <gtest/gtest.h>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace prefs {
namespace tests {

std::string findMissingDefaults(const FilePath& schemaFile)
{
   json::Value value;
   std::string schemaContents;

   // Load schema 
   Error error = core::readStringFromFile(schemaFile, &schemaContents);
   if (error)
      return error.asString();
   error = value.parse(schemaContents);
   if (error)
      return error.asString();

   // Find properties (prefs)
   json::Object schema = value.getObject();
   json::Object::Iterator objProperties = schema.find("properties");
   if (objProperties == schema.end() || !(*objProperties).getValue().isObject())
   {
      return "No properties found";
   }
   const json::Object& properties = (*objProperties).getValue().getObject();

   // Ensure each property has a default value
   for (auto prop: properties)
   {
      const json::Object& definition = prop.getValue().getObject();
      json::Object::Iterator type = definition.find("type");
      if (type != definition.end())
      {
         if ((*type).getValue().getString() == "object")
         {
            // Exempt object types from the requirement for a default (only scalars need defaults)
            continue;
         }
      }
      json::Object::Iterator def = definition.find("default");
      if (def == definition.end())
      {
         return "Pref named '" + prop.getName() + "' does not have a default.";
      }
   }

   return "";
}

TEST(SessionTest, UserPrefsHaveDefaults) {
   std::string err = findMissingDefaults(
      options().rResourcesPath().completePath("schema").completePath(kUserPrefsSchemaFile));
   ASSERT_EQ("", err);
}

TEST(SessionTest, UserStateHaveDefaults) {
   std::string err = findMissingDefaults(
      options().rResourcesPath().completePath("schema").completePath(kUserStateSchemaFile));
   ASSERT_EQ("", err);
}

TEST(SessionTest, UserPrefsDefaultsMatchSchema) {
   UserPrefsDefaultLayer defaults;
   Error error = defaults.readPrefs();
   ASSERT_FALSE(error);

   error = defaults.validatePrefsFromSchema(
      options().rResourcesPath().completePath("schema").completePath(kUserPrefsSchemaFile));
   if (error) {
      std::cerr << error.asString() << std::endl;
   }
   ASSERT_FALSE(error);
}

TEST(SessionTest, UserStateDefaultsMatchSchema) {
   UserStateDefaultLayer defaults;
   Error error = defaults.readPrefs();
   ASSERT_FALSE(error);

   error = defaults.validatePrefsFromSchema(
      options().rResourcesPath().completePath("schema").completePath(kUserStateSchemaFile));
   if (error) {
      std::cerr << error.asString() << std::endl;
   }
   ASSERT_FALSE(error);
}

} // namespace tests
} // namespace prefs
} // namespace session
} // namespace rstudio
