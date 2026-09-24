/*
 * SessionRenv.cpp
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

#include "SessionRenv.hpp"
#include "SessionRig.hpp"

#include <shared_core/Error.hpp>
#include <core/Exec.hpp>

#include <r/RExec.hpp>
#include <r/RJson.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionOptions.hpp>
#include <session/prefs/UserPrefs.hpp>
#include <session/projects/SessionProjects.hpp>

#include "session-config.h"


using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace module_context {

bool isRequiredRenvInstalled()
{
   return isPackageVersionInstalled("renv", "0.9.2");
}

bool isRenvActive()
{
   return !core::system::getenv("RENV_PROJECT").empty();
}

namespace {

core::json::Value renvStateAsJson(const std::string method)
{
   json::Value resultJson;
   Error error =
         r::exec::RFunction(method)
         .call(&resultJson);

   if (error)
   {
      LOG_ERROR(error);
      return json::Object();
   }

   if (resultJson.getType() != json::Type::OBJECT)
   {
      error = systemError(boost::system::errc::invalid_argument, ERROR_LOCATION);
      LOG_ERROR(error);
      return json::Object();
   }

   return resultJson;

}

} // end anonymous namespace

core::json::Value renvOptionsAsJson()
{
   return renvStateAsJson(".rs.renv.options");
}

core::json::Value renvContextAsJson()
{
   return renvStateAsJson(".rs.renv.context");
}

} // end namespace module_context
} // end namespace session
} // end namespace rstudio

namespace rstudio {
namespace session {
namespace modules {
namespace renv {

namespace {

void onConsolePrompt(const std::string& /* prompt */)
{
   // use RENV_PROJECT environment variable to detect if renv active
   std::string renvProject = core::system::getenv("RENV_PROJECT");
   if (renvProject.empty())
      return;

   // validate that it matches the project currently open in RStudio
   // (we could consider relaxing this in the future)
   const FilePath& projDir = projects::projectContext().directory();
   if (!projDir.isEquivalentTo(FilePath(renvProject)))
      return;

   Error error = r::exec::RFunction(".rs.renv.refresh") .call();
   if (error)
      LOG_ERROR(error);
}

// The version of R a project asks for: an explicit version in the project
// file wins, otherwise the version recorded in the renv lockfile.
std::string requestedRVersion(const FilePath& projectDir, std::string* pSource)
{
   const r_util::RVersionInfo& projectVersion = projects::projectContext().config().rVersion;
   if (!projectVersion.isDefault() && !projectVersion.number.empty())
   {
      *pSource = "project";
      return projectVersion.number;
   }

   std::string version;
   Error error = r::exec::RFunction(".rs.renv.lockfileRVersion")
         .addParam("project", projectDir.getAbsolutePath())
         .call(&version);
   if (error)
   {
      LOG_ERROR(error);
      return std::string();
   }

   *pSource = "lockfile";
   return version;
}

// Offer to restore the project library when the lockfile's R version is in
// use but the library is empty, e.g. right after switching to that R.
void promptForRestoreIfLibraryEmpty(const FilePath& projectDir)
{
   if (!module_context::isRenvActive())
      return;

   bool prompt = false;
   Error error = r::exec::RFunction(".rs.renv.shouldPromptRestore")
         .addParam("project", projectDir.getAbsolutePath())
         .call(&prompt);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   if (prompt)
      module_context::enqueClientEvent(ClientEvent(client_events::kRenvRestorePrompt, json::Object()));
}

// Compare the version of R the project asks for with the running one, and
// tell the client when they differ so it can offer to switch.
void checkProjectRVersion()
{
   if (!prefs::userPrefs().checkProjectRVersion())
      return;

   if (!projects::projectContext().hasProject())
      return;

   const FilePath& projectDir = projects::projectContext().directory();

   std::string source;
   std::string requested = requestedRVersion(projectDir, &source);
   if (requested.empty())
      return;

   json::Object status;
   Error error = r::exec::RFunction(".rs.rVersionStatus")
         .addParam("requested", requested)
         .addParam("minimum", RSTUDIO_R_VERSION_REQUIRED)
         .addParam("maximum", RSTUDIO_R_VERSION_MAXIMUM)
         .call(&status);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   bool matches = false, supported = false;
   std::string current;
   error = json::readObject(status,
                            "current", current,
                            "matches", matches,
                            "supported", supported);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   if (matches)
   {
      if (source == "lockfile")
         promptForRestoreIfLibraryEmpty(projectDir);
      return;
   }

   json::Value installed;
   error = modules::rig::findInstalledRVersion(requested, &installed);
   if (error)
      LOG_ERROR(error);

   // installing R is only offered on the desktop, where the session can be
   // relaunched with a different R; server installations are managed by
   // administrators
   bool desktop = options().programMode() == kSessionProgramModeDesktop;

   json::Object data;
   data["requested_version"] = requested;
   data["current_version"] = current;
   data["source"] = source;
   data["supported"] = supported;
   data["installed"] = installed;
   data["can_install"] = desktop && supported;
   module_context::enqueClientEvent(ClientEvent(client_events::kProjectRVersionMismatch, data));
}

void onDeferredInit(bool)
{
   checkProjectRVersion();
}

} // end anonymous namespace

Error initialize()
{
   using namespace module_context;

   // initialize renv after session init (need to make sure
   // all other RStudio startup code runs first)
   events().onConsolePrompt.connect(onConsolePrompt);
   events().onDeferredInit.connect(onDeferredInit);

   using boost::bind;
   ExecBlock initBlock;
   initBlock.addFunctions()
         (bind(sourceModuleRFile, "SessionRenv.R"));

   return initBlock.execute();
}

} // end namespace renv
} // end namespace modules
} // end namespace session
} // end namespace rstudio
