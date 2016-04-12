/*
 * NotebookErrors.cpp
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
#include "NotebookErrors.hpp"

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>
#include <r/ROptions.hpp>
#include <r/RJson.hpp>

#include <core/Exec.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {
namespace {

class ErrorState
{
public:
   ErrorState():
      connected_(false)
   { }

   ~ErrorState()
   {
      if (connected_)
         disconnect();
   }

   void connect()
   {
      // store old handler
      sexpErrHandler_.set(r::options::getOption("error"));

      // set new handler
      Error error = r::exec::RFunction(".rs.registerNotebookErrHandler")
                                     .callUnsafe();
      if (error)
         LOG_ERROR(error);

      // mark as connected
      connected_ = true;
   }

   void onError(SEXP sexpErr)
   {
      json::Value jsonErr; 
      Error error = r::json::jsonValueFromList(sexpErr, &jsonErr);
      if (error)
         LOG_ERROR(error);
      if (jsonErr.type() != json::ObjectType)
         return;
      events().onErrorOutput(jsonErr.get_obj());
   }

   void disconnect()
   {
      if (connected_)
      {
         r::options::setErrorOption(sexpErrHandler_.get());
         connected_ = false;
      }
   }

private:
   bool connected_;
   r::sexp::PreservedSEXP sexpErrHandler_;
};

boost::shared_ptr<ErrorState> s_pErrorState;

SEXP rs_recordNotebookError(SEXP errData)
{
   if (s_pErrorState)
      s_pErrorState->onError(errData);

   return R_NilValue;
}

} // anonymous namespace

core::Error beginErrorCapture()
{
   return Success();
}

core::Error initErrors()
{
   RS_REGISTER_CALL_METHOD(rs_recordNotebookError, 1);

   ExecBlock initBlock;
   initBlock.addFunctions()
      (boost::bind(module_context::sourceModuleRFile, "NotebookErrors.R"));

   return initBlock.execute();
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio
