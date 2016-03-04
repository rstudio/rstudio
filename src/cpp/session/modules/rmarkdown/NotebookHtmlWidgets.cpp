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

#include "NotebookHtmlWidgets.hpp"

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
   return R_NilValue;
}

} // anonymous namespace

core::Error beginWidgetCapture(
              const core::FilePath& outputFolder,
              const core::FilePath& libraryFolder,
              boost::function<void(const core::FilePath&)> widgetCaptured)
{
   Error error = r::exec::RFunction(".rs.initHtmlCapture", 
         outputFolder.absolutePath(),
         libraryFolder.absolutePath()).call();
   if (error)
      return error;
   return Success();
}

core::Error initHtmlWidgets()
{
   RS_REGISTER_CALL_METHOD(rs_recordHtmlWidget, 1);

   return Success();
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

