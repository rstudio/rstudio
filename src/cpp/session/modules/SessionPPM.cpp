/*
 * SessionPPM.cpp
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

#include <functional>

#include <shared_core/Error.hpp>

#include <core/Exec.hpp>

#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace ppm {

bool isPpmIntegrationEnabled()
{
   // primarily for testing
   std::string enabled = core::system::getenv("PWB_PPM_INTEGRATION_ENABLED");
   if (!enabled.empty())
      return string_utils::isTruthy(enabled);

   // otherwise, assume integration is enabled if a repository URL was provided
   std::string url = core::system::getenv("PWB_PPM_REPO_URL");
   return !url.empty();
}

namespace {

SEXP rs_ppmIntegrationEnabled()
{
   bool enabled = isPpmIntegrationEnabled();
   r::sexp::Protect protect;
   return r::sexp::create(enabled, &protect);
}

} // end anonymous namespace

Error initialize()
{
   using std::bind;
   using namespace module_context;

   RS_REGISTER_CALL_METHOD(rs_ppmIntegrationEnabled);

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionPPM.R"));

   return initBlock.execute();
}

} // namespace ppm
} // namespace modules
} // namespace session
} // namespace rstudio


