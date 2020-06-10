/*
 * SessionRenv.cpp
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

#include "SessionRenv.hpp"

#include <shared_core/Error.hpp>
#include <core/Exec.hpp>

#include <r/RExec.hpp>
#include <r/RJson.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/projects/SessionProjects.hpp>


using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace module_context {

bool isRequiredRenvInstalled()
{
   return isPackageVersionInstalled("renv", "0.9.2");
}

bool isRenvActive()
{
   return !core::system::getenv("RENV_PROJECT").empty();
}

namespace {

core::json::Value renvStateAsJson(const std::string method)
{
   json::Value resultJson;
   Error error =
         r::exec::RFunction(method)
         .call(&resultJson);

   if (error)
   {
      LOG_ERROR(error);
      return json::Object();
   }

   if (resultJson.getType() != json::Type::OBJECT)
   {
      error = systemError(boost::system::errc::invalid_argument, ERROR_LOCATION);
      LOG_ERROR(error);
      return json::Object();
   }

   return resultJson;

}

} // end anonymous namespace

core::json::Value renvOptionsAsJson()
{
   return renvStateAsJson(".rs.renv.options");
}

core::json::Value renvContextAsJson()
{
   return renvStateAsJson(".rs.renv.context");
}

} // end namespace module_context
} // end namespace session
} // end namespace rstudio

namespace rstudio {
namespace session {
namespace modules {
namespace renv {

namespace {

void onConsolePrompt(const std::string& /* prompt */)
{
   // use RENV_PROJECT environment variable to detect if renv active
   std::string renvProject = core::system::getenv("RENV_PROJECT");
   if (renvProject.empty())
      return;

   // validate that it matches the project currently open in RStudio
   // (we could consider relaxing this in the future)
   const FilePath& projDir = projects::projectContext().directory();
   if (!projDir.isEquivalentTo(FilePath(renvProject)))
      return;

   Error error = r::exec::RFunction(".rs.renv.refresh") .call();
   if (error)
      LOG_ERROR(error);
}

} // end anonymous namespace

Error initialize()
{
   using namespace module_context;

   // initialize renv after session init (need to make sure
   // all other RStudio startup code runs first)
   events().onConsolePrompt.connect(onConsolePrompt);

   using boost::bind;
   ExecBlock initBlock;
   initBlock.addFunctions()
         (bind(sourceModuleRFile, "SessionRenv.R"));

   return initBlock.execute();
}

} // end namespace renv
} // end namespace modules
} // end namespace session
} // end namespace rstudio
