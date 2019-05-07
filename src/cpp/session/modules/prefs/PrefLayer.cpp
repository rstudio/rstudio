/*
 * PrefLayer.cpp
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

#include "PrefLayer.hpp"

#include <core/FileSerializer.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace prefs {

PrefLayer::~PrefLayer()
{
   // No-op virtual destructor
}

core::json::Object PrefLayer::allPrefs()
{
   return *cache_;
}

core::Error PrefLayer::writePrefs(const core::json::Object &prefs)
{
   // Most preference layers aren't writable, so the default implementation throws an error.
   return systemError(boost::system::errc::function_not_supported, ERROR_LOCATION);
}

core::Error PrefLayer::loadPrefsFromFile(const core::FilePath &prefsFile)
{
   json::Value val;
   std::string contents;
   Error error = readStringFromFile(prefsFile, &contents);
   if (error)
      return error;

   error = json::parse(contents, ERROR_LOCATION, &val);
   if (error)
   {
      // Couldn't parse prefs JSON
      return error;
   }
   else if (val.type() == json::ObjectType)
   {
      // Successful parse of prefs object
      cache_ = boost::make_shared<json::Object>(val.get_obj());
   }
   else
   {
      // We parsed but got a non-object JSON value (this is exceedingly unlikely)
      return Error(rapidjson::kParseErrorValueInvalid, ERROR_LOCATION);
   }

   return Success();
}

core::Error PrefLayer::loadPrefsFromSchema(const core::FilePath &schemaFile)
{
   std::string contents;
   Error error = readStringFromFile(schemaFile, &contents);
   if (error)
      return error;

   cache_ = boost::make_shared<json::Object>();
   return json::getSchemaDefaults(contents, cache_.get());
}

core::Error PrefLayer::validatePrefsFromSchema(const core::FilePath &schemaFile)
{
   json::Value val;
   std::string contents;
   Error error = readStringFromFile(schemaFile, &contents);
   if (error)
      return error;

  return json::validate(*cache_, contents, ERROR_LOCATION);
}

core::Error PrefLayer::writePrefsToFile(const core::json::Object& prefs,
      const core::FilePath& prefsFile)
{
   *cache_ = prefs;

   std::ostringstream oss;
   json::writeFormatted(prefs, oss);
   return writeStringToFile(prefsFile, oss.str());
}


} // namespace prefs
} // namespace modules
} // namespace session
} // namespace rstudio

