/*
 * SessionJulia.cpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

#include "SessionJulia.hpp"

#include <boost/bind/bind.hpp>

#include <shared_core/Error.hpp>
#include <core/Exec.hpp>

#include <r/RExec.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace julia {

bool isReplActive()
{
   bool active = false;
   Error error = r::exec::RFunction(".rs.julia.replIsActive").call(&active);
   if (error)
      LOG_ERROR(error);
   return active;
}

Error initialize()
{
   using boost::bind;
   using namespace module_context;

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionJulia.R"));

   return initBlock.execute();
}

} // end namespace julia
} // end namespace modules
} // end namespace session
} // end namespace rstudio
