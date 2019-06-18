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


using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace module_context {

core::json::Object renvContextAsJson()
{
   SEXP resultSEXP = R_NilValue;
   r::sexp::Protect protect;

   Error error =
         r::exec::RFunction(".rs.renv.context")
         .call(&resultSEXP, &protect);

   if (error)
   {
      LOG_ERROR(error);
      return json::Object();
   }

   json::Value resultJson;
   error = r::json::jsonValueFromObject(resultSEXP, &resultJson);
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

   return resultJson.get_obj();
}

} // end namespace module_context
} // end namespace session
} // end namespace rstudio

namespace rstudio {
namespace session {
namespace modules {
namespace renv {

void afterSessionInitHook(bool newSession)
{
   // TODO: initialize if using renv
}

Error initialize()
{
   using namespace module_context;

   // initialize renv after session init (need to make sure
   // all other RStudio startup code runs first)
   events().afterSessionInitHook.connect(afterSessionInitHook);

   using boost::bind;
   ExecBlock initBlock;
   initBlock.addFunctions()
         (bind(sourceModuleRFile, "SessionPackrat.R"));

   return initBlock.execute();
}

} // end namespace renv
} // end namespace modules
} // end namespace session
} // end namespace rstudio
