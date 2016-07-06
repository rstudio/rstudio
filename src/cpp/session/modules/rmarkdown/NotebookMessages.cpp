/*
 * NotebookMessages.cpp
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

#include "SessionRmdNotebook.hpp"
#include "NotebookMessages.hpp"

#include <r/RExec.hpp>
#include <r/RExec.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

void MessageCapture::connect()
{
   // create a parsed expression which will disable messages; this is 
   // effectively identical to suppressMessages() but we need to evaluate it
   // at the top level so we can influence the global handler stack.   
   r::sexp::Protect protect;
   SEXP retSEXP = R_NilValue;
   Error error = r::exec::executeStringUnsafe(
         ".Internal(.addCondHands(\"message\", list(message = function(m) { "
         "   invokeRestart(\"muffleMessage\") "
         "}), .GlobalEnv, NULL, TRUE))", &retSEXP, &protect);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   // we save the old handler stack into the tools environment for easy
   // restoration later
   r::exec::RFunction assign("assign");
   assign.addParam("x", "_rs_handlerStack");
   assign.addParam("value", retSEXP);
   assign.addParam("envir", r::sexp::asEnvironment("tools:rstudio"));
   error = assign.call();
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
   
   NotebookCapture::connect();
}

void MessageCapture::disconnect()
{
   if (connected())
   {
      // restore the previous handler stack by extracting from the tools
      // environment
      r::sexp::Protect protect;
      SEXP retSEXP = R_NilValue;
      Error error = r::exec::executeStringUnsafe(
            ".Internal(.resetCondHands(get(\"_rs_handlerStack\", "
            "   envir = as.environment(\"tools:rstudio\")))) ",
            &retSEXP, &protect);
      if (error)
         LOG_ERROR(error);

      // clean up the variable holding the previous handler stack
      error = r::exec::executeStringUnsafe(
            "rm(\"_rs_handlerStack\", "
            "   envir = as.environment(\"tools:rstudio\"))",
            &retSEXP, &protect);
      if (error)
         LOG_ERROR(error);
   }
   NotebookCapture::disconnect();
}


} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio
