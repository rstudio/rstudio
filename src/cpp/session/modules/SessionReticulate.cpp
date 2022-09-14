/*
 * SessionReticulate.cpp
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

#include "SessionReticulate.hpp"

#include "SessionThemes.hpp"

#include <boost/bind/bind.hpp>

#include <shared_core/Error.hpp>
#include <core/Exec.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;
using namespace boost::placeholders;

namespace rstudio {
namespace session {
namespace modules {
namespace reticulate {

namespace {

// has the Python session been initialized by reticulate yet?
bool s_pythonInitialized = false;

std::string s_reticulatePython;
bool s_reticulatePythonInited = false;

void updateReticulatePython(bool forInit)
{
   if (!forInit && s_reticulatePythonInited)
      return;

   if (!ASSERT_MAIN_THREAD())
   {
      return;
   }

   s_reticulatePython = core::system::getenv("RETICULATE_PYTHON");
   if (s_reticulatePython.empty())
   {
      // Will check if RETICULATE_PYTHON_FALLBACK is set,
      // unless higher priority Python config has already been found
      Error error = r::exec::RFunction(".rs.inferReticulatePython")
            .call(&s_reticulatePython);

      if (error)
         LOG_ERROR(error);
   }

   s_reticulatePythonInited = true;
}

SEXP rs_reticulateInitialized()
{
   // set initialized flag
   s_pythonInitialized = true;
   
   // Python will register its own console control handler,
   // which also blocks signals from reaching any previously
   // defined handlers (including RStudio's own). re-initialize
   // RStudio's console control handler here to ensure that
   // interrupts are still handled by R as expected
   module_context::initializeConsoleCtrlHandler();

   // cache the final path to python for offline use
   updateReticulatePython(true);

   return R_NilValue;
}

void onDeferredInit(bool)
{
   Error error = r::exec::RFunction(".rs.reticulate.initialize").call();
   if (error)
      LOG_ERROR(error);

   // update python path after all R init scripts
   updateReticulatePython(false);
}

} // end anonymous namespace

bool isPythonInitialized()
{
   return s_pythonInitialized;
}

bool isReplActive()
{
   bool active = false;
   Error error = r::exec::RFunction(".rs.reticulate.replIsActive").call(&active);
   if (error)
      LOG_ERROR(error);
   return active;
}


std::string reticulatePython()
{
   updateReticulatePython(false);
   return s_reticulatePython;
}

Error initialize()
{
   using namespace module_context;
   
   events().onDeferredInit.connect(onDeferredInit);

   RS_REGISTER_CALL_METHOD(rs_reticulateInitialized);

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionReticulate.R"));
   
   return initBlock.execute();
}

} // end namespace reticulate
} // end namespace modules

namespace module_context {

bool isPythonReplActive()
{
   return modules::reticulate::isReplActive();
}

} // end namespace module_context

} // end namespace session
} // end namespace rstudio
