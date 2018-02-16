/*
 * SessionReticulate.cpp
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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

#include "SessionReticulate.hpp"

#include <boost/bind.hpp>

#include <core/Error.hpp>
#include <core/Exec.hpp>

#include <r/RExec.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace reticulate {

bool isReplActive()
{
   bool active = false;
   Error error = r::exec::RFunction("reticulate:::py_repl_active").call(&active);
   if (error)
      LOG_ERROR(error);
   return active;
}

Error initialize()
{
   using namespace module_context;
   
   ExecBlock initBlock;
   initBlock.addFunctions()
         (bind(sourceModuleRFile, "SessionReticulate.R"));
   
   return initBlock.execute();
}

} // end namespace reticulate
} // end namespace modules
} // end namespace session
} // end namespace rstudio
