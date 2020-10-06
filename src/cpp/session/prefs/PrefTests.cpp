/*
 * UserPrefTests.cpp
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

#include "UserStateDefaultLayer.hpp"
#include "UserPrefsDefaultLayer.hpp"
#include <session/prefs/UserState.hpp>
#include <session/prefs/UserPrefs.hpp>

#include <session/SessionOptions.hpp>

#include <core/FileSerializer.hpp>

#include <tests/TestThat.hpp>

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

test_context("default validation")
{
   test_that("all user preferences have defaults")
   {
      std::string err = findMissingDefaults(
         options().rResourcesPath().completePath("schema").completePath(kUserPrefsSchemaFile));
      expect_equal(err, "");
   }
 
   test_that("all user state values have defaults")
   {
      std::string err = findMissingDefaults(
         options().rResourcesPath().completePath("schema").completePath(kUserStateSchemaFile));
      expect_equal(err, "");
   }
 
   test_that("user preference defaults are valid according to their schema")
   {
      UserPrefsDefaultLayer defaults;
      Error error = defaults.readPrefs();
      expect_true(!error);

      error = defaults.validatePrefsFromSchema(
         options().rResourcesPath().completePath("schema").completePath(kUserPrefsSchemaFile));
      INFO(error.asString());
      expect_true(!error);
   }

   test_that("user state defaults are valid according to their schema")
   {
      UserStateDefaultLayer defaults;
      Error error = defaults.readPrefs();
      expect_true(!error);

      error = defaults.validatePrefsFromSchema(
         options().rResourcesPath().completePath("schema").completePath(kUserStateSchemaFile));
      INFO(error.asString());
      expect_true(!error);
   }
}

} // namespace tests
} // namespace prefs
} // namespace session
} // namespace rstudio

