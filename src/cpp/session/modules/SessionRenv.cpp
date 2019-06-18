/*
 * SessionRenv.cpp
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

#include "SessionRenv.hpp"

#include <core/Error.hpp>
#include <core/Exec.hpp>

#include <r/RExec.hpp>
#include <r/RJson.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/projects/SessionProjects.hpp>


using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace module_context {

core::json::Value renvContextAsJson()
{
   json::Value resultJson;
   Error error =
         r::exec::RFunction(".rs.renv.context")
         .call(&resultJson);

   if (error)
   {
      LOG_ERROR(error);
      return json::Object();
   }

   if (resultJson.type() != json::ObjectType)
   {
      error = systemError(boost::system::errc::invalid_argument, ERROR_LOCATION);
      LOG_ERROR(error);
      return json::Object();
   }

   return resultJson;
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
   Error error = r::exec::RFunction(".rs.renv.refresh")
         .call();

   if (error)
      LOG_ERROR(error);
}

void afterSessionInitHook(bool /* newSession */)
{
   // use RENV_PROJECT environment variable to detect if renv active
   std::string renvProject = core::system::getenv("RENV_PROJECT");
   if (renvProject.empty())
      return;

   // renv is active -- initialize other infrastructure
   using namespace module_context;
   events().onConsolePrompt.connect(onConsolePrompt);
}

} // end anonymous namespace

Error initialize()
{
   using namespace module_context;

   // initialize renv after session init (need to make sure
   // all other RStudio startup code runs first)
   events().afterSessionInitHook.connect(afterSessionInitHook);

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
