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

#define kPwbPpmIntegrationEnabled  "PWB_PPM_INTEGRATION_ENABLED"
#define kPwbPpmRepoUrl             "PWB_PPM_REPO_URL"
#define kPwbPpmMetadataKey         "PWB_PPM_METADATA_KEY"
#define kPwbPpmMetadataColumnLabel "PWB_PPM_METADATA_COLUMN_LABEL"

namespace rstudio {
namespace session {
namespace modules {
namespace ppm {

bool isPpmIntegrationEnabled()
{
   // primarily for testing
   std::string enabled = core::system::getenv(kPwbPpmIntegrationEnabled);
   if (!enabled.empty())
      return string_utils::isTruthy(enabled);

   // otherwise, assume integration is enabled if a repository URL was provided
   std::string url = core::system::getenv(kPwbPpmRepoUrl);
   return !url.empty();
}

std::string getPpmRepositoryUrl()
{
   return core::system::getenv(kPwbPpmRepoUrl);
}

std::string getPpmMetadataKey()
{
   std::string key;

   // primarily for testing
   key = core::system::getenv(kPwbPpmMetadataKey);
   if (!key.empty())
      return key;

   // otherwise, read from session options
   return session::options().getOverlayOption("posit-package-manager-metadata-key");
}

std::string getPpmMetadataColumnLabel()
{
   std::string label;

   // primarily for testing
   label = core::system::getenv(kPwbPpmMetadataColumnLabel);
   if (!label.empty())
      return label;

   // otherwise, read from session options
   label = session::options().getOverlayOption("posit-package-manager-metadata-key-display-name");
   if (!label.empty())
      return label;

   // if nothing else provided, just use a default label
   return "Metadata";
}

namespace {

SEXP rs_ppmIntegrationEnabled()
{
   bool enabled = isPpmIntegrationEnabled();
   r::sexp::Protect protect;
   return r::sexp::create(enabled, &protect);
}

SEXP rs_ppmMetadataKey()
{
   r::sexp::Protect protect;
   return r::sexp::create(getPpmMetadataKey(), &protect);
}

} // end anonymous namespace

Error initialize()
{
   using std::bind;
   using namespace module_context;

   RS_REGISTER_CALL_METHOD(rs_ppmIntegrationEnabled);
   RS_REGISTER_CALL_METHOD(rs_ppmMetadataKey);

   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(sourceModuleRFile, "SessionPPM.R"));

   return initBlock.execute();
}

} // namespace ppm
} // namespace modules
} // namespace session
} // namespace rstudio


