/*
 * UserPrefTests.cpp
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
      return error.description();
   error = json::parse(schemaContents, ERROR_LOCATION, &value);
   if (error)
      return error.description();

   // Find properties (prefs)
   json::Object schema = value.get_obj();
   json::Object::iterator objProperties = schema.find("properties");
   if (objProperties == schema.end() ||
       (*objProperties).value().type() != json::ObjectType)
   {
      return "No properties found";
   }
   const json::Object& properties = (*objProperties).value().get_obj();

   // Ensure each property has a default value
   for (auto prop: properties)
   {
      const json::Object& definition = prop.value().get_obj();
      json::Object::iterator type = definition.find("type");
      if (type != definition.end())
      {
         if ((*type).value().get_str() == "object")
         {
            // Exempt object types from the requirement for a default (only scalars need defaults)
            continue;
         }
      }
      json::Object::iterator def = definition.find("default");
      if (def == definition.end())
      {
         return "Pref named '" + prop.name() + "' does not have a default.";
      }
   }

   return "";
}

test_context("default validation")
{
   test_that("all user preferences have defaults")
   {
      std::string err = findMissingDefaults(
         options().rResourcesPath().complete("schema").complete(kUserPrefsSchemaFile));
      expect_equal(err, "");
   }
 
   test_that("all user state values have defaults")
   {
      std::string err = findMissingDefaults(
         options().rResourcesPath().complete("schema").complete(kUserStateSchemaFile));
      expect_equal(err, "");
   }
 
   test_that("user preference defaults are valid according to their schema")
   {
      UserPrefsDefaultLayer defaults;
      Error error = defaults.readPrefs();
      expect_true(error == Success());

      error = defaults.validatePrefsFromSchema(
         options().rResourcesPath().complete("schema").complete(kUserPrefsSchemaFile));
      INFO(error.description());
      expect_true(error == Success());
   }

   test_that("user state defaults are valid according to their schema")
   {
      UserStateDefaultLayer defaults;
      Error error = defaults.readPrefs();
      expect_true(error == Success());

      error = defaults.validatePrefsFromSchema(
         options().rResourcesPath().complete("schema").complete(kUserStateSchemaFile));
      INFO(error.description());
      expect_true(error == Success());
   }
}

} // namespace tests
} // namespace prefs
} // namespace session
} // namespace rstudio

