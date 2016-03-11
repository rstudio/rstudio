/*
 * NotebookHtmlWidgets.cpp
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
#include "NotebookHtmlWidgets.hpp"
#include "NotebookOutput.hpp"

#include <iostream>

#include <boost/foreach.hpp>
#include <boost/format.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>

#include <core/Exec.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {

namespace {

SEXP rs_recordHtmlWidget(SEXP fileSEXP)
{
   std::string file = r::sexp::safeAsString(fileSEXP);
   events().onHtmlOutput(FilePath(file));
   return R_NilValue;
}

void onConsolePrompt(const std::string&)
{
   // stop capturing HTML widgets when the prompt returns
   Error error = r::exec::RFunction(".rs.releaseHtmlCapture").call();
   if (error)
      LOG_ERROR(error);

   module_context::events().onConsolePrompt.disconnect(onConsolePrompt);
}


} // anonymous namespace

core::Error beginWidgetCapture(
              const core::FilePath& outputFolder,
              const core::FilePath& libraryFolder)
{
   Error error = r::exec::RFunction(".rs.initHtmlCapture", 
         outputFolder.absolutePath(),
         outputFolder.complete(kChunkLibDir).absolutePath()).call();
   if (error)
      return error;


   module_context::events().onConsolePrompt.connect(onConsolePrompt);

   return Success();
}

core::Error initHtmlWidgets()
{
   RS_REGISTER_CALL_METHOD(rs_recordHtmlWidget, 1);

   ExecBlock initBlock;
   initBlock.addFunctions()
      (boost::bind(module_context::sourceModuleRFile, "NotebookHtmlWidgets.R"));

   return initBlock.execute();
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

