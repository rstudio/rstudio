/*
 * NotebookConditions.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
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

#include <core/Exec.hpp>

#include <r/RExec.hpp>
#include <r/RSexp.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>

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
   std::string msg = r::sexp::asUtf8String(message);
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
   r::sexp::Protect protect;
   
   SEXP connectCall = R_NilValue;
   Error error = r::exec::RFunction(".rs.notebookConditions.connectCall").call(&connectCall, &protect);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }
   
   SEXP resultSEXP = R_NilValue;
   error = r::exec::executeCallUnsafe(connectCall, R_GlobalEnv, &resultSEXP, &protect);
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
      r::sexp::Protect protect;
      
      SEXP disconnectCall = R_NilValue;
      Error error = r::exec::RFunction(".rs.notebookConditions.disconnectCall").call(&disconnectCall, &protect);
      if (error)
         LOG_ERROR(error);
      
      SEXP resultSEXP = R_NilValue;
      error = r::exec::executeCallUnsafe(disconnectCall, R_GlobalEnv, &resultSEXP, &protect);
      if (error)
         LOG_ERROR(error);
   }
   
   NotebookCapture::disconnect();
}

core::Error initConditions()
{
   RS_REGISTER_CALL_METHOD(rs_signalNotebookCondition);  
   
   ExecBlock initBlock;
   initBlock.addFunctions()
      (boost::bind(module_context::sourceModuleRFile, "NotebookConditions.R"));
   
   return initBlock.execute();
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio
