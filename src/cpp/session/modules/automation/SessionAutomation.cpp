/*
 * SessionAutomation.cpp
 *
 * Copyright (C) 2024 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "SessionAutomation.hpp"

#include <boost/bind.hpp>

#include <shared_core/Error.hpp>

#include <core/Exec.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>
#include <r/RSexp.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio;
using namespace rstudio::core;
using namespace boost::placeholders;

namespace rstudio {
namespace session {
namespace modules {
namespace automation {

namespace {

SEXP rs_automationReportFile()
{
   FilePath reportFile = session::options().automationReportFile();
   if (reportFile.isEmpty())
      return R_NilValue;
   
   r::sexp::Protect protect;
   return r::sexp::create(reportFile.getAbsolutePath(), &protect);
}

} // end anonymous namespace

Error initialize()
{
   using namespace module_context;
   
   RS_REGISTER_CALL_METHOD(rs_automationReportFile);
   
   ExecBlock initBlock;
   initBlock.addFunctions()
      (boost::bind(sourceModuleRFile, "SessionAutomation.R"))
      (boost::bind(sourceModuleRFile, "SessionAutomationClient.R"))
      (boost::bind(sourceModuleRFile, "SessionAutomationConstants.R"))
      (boost::bind(sourceModuleRFile, "SessionAutomationRemote.R"))
      (boost::bind(sourceModuleRFile, "SessionAutomationRemoteObject.R"))
      (boost::bind(sourceModuleRFile, "SessionAutomationTools.R"))
      (boost::bind(sourceModuleRFile, "SessionAutomationToolsConsole.R"))
      (boost::bind(sourceModuleRFile, "SessionAutomationToolsProjects.R"));
   
   Error error = initBlock.execute();
   if (error)
      LOG_ERROR(error);
   
   return Success();
   
}

} // end namespace automation
} // end namespace modules
} // end namespace session
} // end namespace rstudio
