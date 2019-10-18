/*
 * SessionDependencyList.cpp
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

#include "SessionDependencyList.hpp"

#include <core/FileSerializer.hpp>

#include <core/json/JsonRpc.hpp>

#include <session/SessionOptions.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace dependency_list {

Error getDependencyList(json::Object *pList)
{
   // Get the contents of the dependency list JSON database (shipped in our resources folder)
   std::string contents;
   Error error = readStringFromFile(
         options().rResourcesPath().complete("dependencies").complete("r-packages.json"),
         &contents);
   if (error)
      return error;

   // Parse and ensure we got an object
   json::Value val;
   error = json::parse(contents, ERROR_LOCATION, &val);
   if (error)
      return error;

   if (val.type() != json::ObjectType)
   {
      // It's unlikely to parse successfully and not get an object, but avoid throwing by bailing if
      // this occurs.
      return Error(json::errc::ParseError, ERROR_LOCATION);
   }

   // Return the parsed dependency list.
   *pList = val.get_obj();
   return Success();
}

} // namespace dependency_list
} // namespace modules
} // namespace session
} // namespace rstudio

