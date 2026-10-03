/*
 * SessionStan.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

#include "SessionStan.hpp"

#include <core/Exec.hpp>
#include <session/SessionModuleContext.hpp>

using namespace rstudio;
using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace stan {

Error initialize()
{
   using namespace module_context;
   return sourceModuleRFileOnDemand(
      "SessionStan.R",
      {"rpc.stan_get_completions", "rpc.stan_get_arguments", "rpc.stan_run_diagnostics",
       "stan.getCompletions", "stan.getArguments", "stan.runDiagnostics",
       "stan.extractFromNamespace", "stan.keywords", "stan.types", "stan.blocks",
       "stan.rosetta", "stan.copySourceDatabaseToTempfile"});
}

} // end namespace stan
} // end namespace modules
} // end namespace session
} // end namespace rstudio
