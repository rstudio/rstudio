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
#include <session/SessionSuspend.hpp>
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

// Compare the version of R the project asks for with the running one. The
// client is told when they differ, so it can offer to switch, or when they
// match but the renv library is empty (as it is right after a switch), so
// it can offer to restore it.
void checkProjectRVersion()
{
   if (!prefs::userPrefs().checkProjectRVersion())
      return;

   if (!projects::projectContext().hasProject())
      return;

   // an explicit version in the project file wins over the lockfile's
   std::string projectVersion;
   const r_util::RVersionInfo& rVersion = projects::projectContext().config().rVersion;
   if (!rVersion.isDefault())
      projectVersion = rVersion.number;

   // switching (and installing) R is only possible on the desktop, where the
   // session can be relaunched with a different R; server installations are
   // managed by administrators, so there is no need to look for one there
   bool desktop = options().programMode() == kSessionProgramModeDesktop;

   json::Value resultJson;
   Error error = r::exec::RFunction(".rs.projectRVersionCheck")
         .addParam("project", projects::projectContext().directory().getAbsolutePath())
         .addParam("projectVersion", projectVersion)
         .addParam("minimum", RSTUDIO_R_VERSION_REQUIRED)
         .addParam("renvActive", module_context::isRenvActive())
         .addParam("findInstalled", desktop)
         .call(&resultJson);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   if (!resultJson.isObject())
      return;

   json::Object result = resultJson.getObject();

   std::string type;
   error = json::readObject(result, "type", type);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   if (type == "restore")
   {
      module_context::enqueClientEvent(ClientEvent(client_events::kRenvRestorePrompt, json::Object()));
      return;
   }

   if (type != "mismatch")
      return;

   std::string requested, current, source;
   bool supported = false;
   error = json::readObject(result,
                            "requested", requested,
                            "current", current,
                            "source", source,
                            "supported", supported);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   json::Value installed = result["installed"];

   json::Object data;
   data["requested_version"] = requested;
   data["current_version"] = current;
   data["source"] = source;
   data["supported"] = supported;
   data["installed"] = installed.isObject() ? installed : json::Value();
   data["can_install"] = desktop && supported;

   // lets a client that connects while R installs (e.g. after a refresh)
   // pick up where the one that started the installation left off
   data["installing_version"] = modules::rig::installInProgress();
   module_context::enqueClientEvent(ClientEvent(client_events::kProjectRVersionMismatch, data));
}

// whether the session has finished starting; a client initializing after
// that is one reconnecting to it (e.g. after a browser refresh)
bool s_sessionStarted = false;

// whether a check is waiting to run
bool s_checkScheduled = false;

void runScheduledCheck()
{
   s_checkScheduled = false;
   checkProjectRVersion();
}

// Looking for installed versions of R runs rig, which takes a moment, so the
// check runs once the session is idle rather than holding anything up.
void scheduleProjectRVersionCheck()
{
   if (s_checkScheduled)
      return;

   s_checkScheduled = true;
   module_context::scheduleDelayedWork(
            boost::posix_time::seconds(1),
            runScheduledCheck,
            true);
}

void onDeferredInit(bool newSession)
{
   s_sessionStarted = true;

   // check again when resumed from a suspend for restart (renv's own check
   // runs again then, and the project may have changed), but not after an
   // ordinary suspend (e.g. for inactivity): nothing changed, and the client
   // was already told
   if (!newSession && !suspend::sessionResumedForRestart())
      return;

   scheduleProjectRVersionCheck();
}

// A reconnecting client (e.g. after a browser refresh) starts without the
// warning the previous page showed.
void onClientInit()
{
   if (s_sessionStarted)
      scheduleProjectRVersionCheck();
}

} // end anonymous namespace

Error initialize()
{
   using namespace module_context;

   // initialize renv after session init (need to make sure
   // all other RStudio startup code runs first)
   events().onConsolePrompt.connect(onConsolePrompt);
   events().onDeferredInit.connect(onDeferredInit);
   events().onClientInit.connect(onClientInit);

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
