/*
 * SessionDependencyList.cpp
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

#include "SessionDependencyList.hpp"

#include <core/FileSerializer.hpp>

#include <core/json/JsonRpc.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionOptions.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace dependency_list {
namespace {

// Return a list of all the packages RStudio depends on
SEXP rs_packageDependencies()
{
   r::sexp::Protect protect;
   json::Object packageList;
   Error error = getDependencyList(&packageList);
   if (error)
   {
      r::exec::error(error.getSummary());
      return R_NilValue;
   }

   // Process the JSON object into columns
   std::vector<std::string> name, version, location;
   std::vector<bool> source;
   try
   {
      // Read each field; we ship this JSON file so can reasonably expect it will be well-formed
      // (we will just bail generically below if it isn't)
      for (const auto& it: packageList["packages"].getObject())
      {
          // The map key is the name of the package
          name.push_back(it.getName());

          // The value object forms the rest of the package metadata
          json::Object pkg = it.getValue().getObject();
          version.push_back(pkg["version"].getString());
          location.push_back(pkg["location"].getString());
          source.push_back(pkg["source"].getBool());
      }
   }
   catch (...)
   {
      r::exec::error("Could not process dependency information.");
      return R_NilValue;
   }

   // Assemble the metadata into a data frame
   r::exec::RFunction frame("data.frame");
   frame.addParam("name", r::sexp::create(name, &protect));
   frame.addParam("version", r::sexp::create(version, &protect));
   frame.addParam("location", r::sexp::create(location, &protect));
   frame.addParam("source", r::sexp::create(source, &protect));
   SEXP packageFrame = R_NilValue;
   error = frame.call(&packageFrame, &protect);
   if (error)
   {
       r::exec::error(error.getSummary());
   }

   return packageFrame;
}

} // anonymous namespace

Error getDependencyList(json::Object *pList)
{
   // Get the contents of the dependency list JSON database (shipped in our resources folder)
   std::string contents;
   Error error = readStringFromFile(
         options().rResourcesPath().completeChildPath("dependencies")
                                   .completeChildPath("r-packages.json"),
         &contents);
   if (error)
      return error;

   // Parse and ensure we got an object
   json::Value val;
   error = val.parse(contents);
   if (error)
      return error;

   if (val.getType() != json::Type::OBJECT)
   {
      // It's unlikely to parse successfully and not get an object, but avoid throwing by bailing if
      // this occurs.
      return Error(json::errc::ParseError, ERROR_LOCATION);
   }

   // Return the parsed dependency list.
   *pList = val.getObject();
   return Success();
}

Error initialize()
{         
   RS_REGISTER_CALL_METHOD(rs_packageDependencies);

   return Success();
}


} // namespace dependency_list
} // namespace modules
} // namespace session
} // namespace rstudio

