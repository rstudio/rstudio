/*
 * NotebookErrors.cpp
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

void ErrorCapture::connect()
{
   // store old handler
   sexpErrHandler_.set(r::options::getOption("error"));

   // set new handler
   Error error = r::exec::RFunction(".rs.registerNotebookErrHandler")
                                  .callUnsafe();
   if (error)
      LOG_ERROR(error);

   NotebookCapture::connect();
}

void ErrorCapture::disconnect()
{
   if (connected())
      r::options::setErrorOption(sexpErrHandler_.get());

   NotebookCapture::disconnect();
}


SEXP rs_recordNotebookError(SEXP errData)
{
   json::Value jsonErr;
   Error error = r::json::jsonValueFromList(errData, &jsonErr);
   if (error)
      LOG_ERROR(error);
   if (!jsonErr.isObject())
      return R_NilValue;
   events().onErrorOutput(jsonErr.getValue<json::Object>());
   return R_NilValue;
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
