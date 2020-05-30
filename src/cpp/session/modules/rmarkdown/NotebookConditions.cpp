/*
 * NotebookConditions.cpp
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

#include "SessionRmdNotebook.hpp"
#include "NotebookConditions.hpp"

#include <shared_core/SafeConvert.hpp>

#include <r/RExec.hpp>
#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {
namespace {

SEXP rs_signalNotebookCondition(SEXP condition, SEXP message)
{
   // extract message (make sure we got one)
   std::string msg = r::sexp::safeAsString(message, "");
   if (msg.empty())
      return R_NilValue;

   // broadcast signaled condition to notebook exec context
   events().onCondition(
      static_cast<Condition>(r::sexp::asInteger(condition)), msg);

   return R_NilValue;
}

} // anonymous namespace

void ConditionCapture::connect()
{
   // create a parsed expression which will disable messages; this is 
   // effectively identical to suppressConditions() but we need to evaluate it
   // at the top level so we can influence the global handler stack.   
   r::sexp::Protect protect;
   SEXP retSEXP = R_NilValue;
   Error error = r::exec::executeStringUnsafe(
         ".Internal(.addCondHands(c(\"warning\", \"message\"), "
         " list(warning = function(m) { "
         "   .Call(\"rs_signalNotebookCondition\", " + 
                      safe_convert::numberToString(ConditionWarning) + "L, "
         "          m$message, PACKAGE = '(embedding)'); "
         "   invokeRestart(\"muffleWarning\") "
         " }, message = function(m) { "
         "   .Call(\"rs_signalNotebookCondition\", " + 
                      safe_convert::numberToString(ConditionMessage) + "L, "
         "          m$message, PACKAGE = '(embedding)'); "
         "   invokeRestart(\"muffleMessage\") "
         " } "
         "), .GlobalEnv, NULL, TRUE))", R_BaseNamespace, &retSEXP, &protect);
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

void ConditionCapture::disconnect()
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
            R_BaseNamespace, &retSEXP, &protect);
      if (error)
         LOG_ERROR(error);

      // clean up the variable holding the previous handler stack
      error = r::exec::executeStringUnsafe(
            "rm(\"_rs_handlerStack\", "
            "   envir = as.environment(\"tools:rstudio\"))",
            R_BaseNamespace, &retSEXP, &protect);
      if (error)
         LOG_ERROR(error);
   }
   NotebookCapture::disconnect();
}

core::Error initConditions()
{
   RS_REGISTER_CALL_METHOD(rs_signalNotebookCondition, 2);
   return Success();
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio
