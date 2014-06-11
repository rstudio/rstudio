/*
 * SessionPackrat.cpp
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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

#include "SessionPackrat.hpp"

#include <core/Exec.hpp>
#include <core/FileSerializer.hpp>
#include <core/Hash.hpp>
#include <core/system/FileMonitor.hpp>
#include <core/RecursionGuard.hpp>

#include <r/RExec.hpp>
#include <r/RJson.hpp>
#include <r/session/RClientState.hpp>
#include <r/RRoutines.hpp>

#include <session/projects/SessionProjects.hpp>
#include <session/SessionAsyncRProcess.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/SessionUserSettings.hpp>

#include "SessionPackages.hpp"
#include "session-config.h"

using namespace core;

#ifdef TRACE_PACKRAT_OUTPUT
#define PACKRAT_TRACE(x) \
   std::cerr << "(packrat) " << x << std::endl;
#else
#define PACKRAT_TRACE(x) 
#endif

#define PACKRAT_FOLDER "packrat/"
#define PACKRAT_LOCKFILE "packrat.lock"
#define PACKRAT_LIB_PATH PACKRAT_FOLDER "lib"
#define PACKRAT_LOCKFILE_PATH PACKRAT_FOLDER PACKRAT_LOCKFILE


namespace session {

namespace {

bool isRequiredPackratInstalled()
{
   return module_context::isPackageVersionInstalled("packrat", "0.2.0.108");
}

} // anonymous namespace

namespace modules { 
namespace packrat {

namespace {

// Current Packrat actions and state -----------------------------------------

enum PackratActionType 
{
   PACKRAT_ACTION_NONE = 0,
   PACKRAT_ACTION_SNAPSHOT = 1,
   PACKRAT_ACTION_RESTORE = 2,
   PACKRAT_ACTION_CLEAN = 3,
   PACKRAT_ACTION_UNKNOWN = 4
};

PackratActionType packratAction(const std::string& str)
{
   if (str == "snapshot") 
      return PACKRAT_ACTION_SNAPSHOT;
   else if (str == "restore")
      return PACKRAT_ACTION_RESTORE;
   else if (str == "clean")
      return PACKRAT_ACTION_CLEAN;
   else 
      return PACKRAT_ACTION_UNKNOWN;
}

static PackratActionType s_runningPackratAction = PACKRAT_ACTION_NONE;

// Library and lockfile hashing and comparison -------------------------------

enum PackratHashType
{
   HASH_TYPE_LOCKFILE = 0,
   HASH_TYPE_LIBRARY = 1
};

enum PendingSnapshotAction
{
   SET_PENDING_SNAPSHOT = 0,
   COMPLETE_SNAPSHOT = 1
};

std::string keyOfHashType(PackratHashType hashType)
{
   return hashType == HASH_TYPE_LOCKFILE ?
      "packratLockfileHash" : 
      "packratLibraryHash";
}

std::string getStoredHash(PackratHashType hashType)
{
   json::Value hash = 
      r::session::clientState().getProjectPersistent("packrat",
                                                     keyOfHashType(hashType));
   if (hash.type() == json::StringType) 
      return hash.get_str();
   else
      return "";
}

void setStoredHash(PackratHashType hashType, const std::string& hash)
{
   PACKRAT_TRACE("updating " << keyOfHashType(hashType) << " -> " <<  hash);
   r::session::clientState().putProjectPersistent(
         "packrat", 
         keyOfHashType(hashType), 
         hash);
}

// adds content from the given file to the given file if it's a 
// DESCRIPTION file (used to summarize library content for hashing)
bool addDescContent(int level, const FilePath& path, std::string* pDescContent)
{
   std::string newDescContent;
   if (path.filename() == "DESCRIPTION") 
   {
      Error error = readStringFromFile(path, &newDescContent);
      pDescContent->append(newDescContent);
   }
   return true;
}

// computes a hash of the content of all DESCRIPTION files in the Packrat
// private library
std::string computeLibraryHash()
{
   FilePath libraryPath = 
      projects::projectContext().directory().complete(PACKRAT_LIB_PATH);

   // find all DESCRIPTION files in the library and concatenate them to form
   // a hashable state
   std::string descFileContent;
   libraryPath.childrenRecursive(
         boost::bind(addDescContent, _1, _2, &descFileContent));

   if (descFileContent.empty())
      return "";

   return hash::crc32HexHash(descFileContent);
}

// computes the hash of the current project's lockfile
std::string computeLockfileHash()
{
   FilePath lockFilePath = 
      projects::projectContext().directory().complete(PACKRAT_LOCKFILE_PATH);

   if (!lockFilePath.exists()) 
      return "";

   std::string lockFileContent;
   Error error = readStringFromFile(lockFilePath, &lockFileContent);
   if (error)
   {
      LOG_ERROR(error);
      return "";
   }
   
   return hash::crc32HexHash(lockFileContent);
}

std::string getComputedHash(PackratHashType hashType)
{
   if (hashType == HASH_TYPE_LOCKFILE)
      return computeLockfileHash();
   else
      return computeLibraryHash();
}

void checkHashes(
      PackratHashType primary, 
      PackratHashType secondary, 
      boost::function<void(const std::string&, const std::string&)> onPrimaryMismatch)
{
   // if a request to check hashes comes in while we're already checking hashes,
   // drop it: it's very likely that the file monitor has discovered a change
   // to a file we've already hashed.
   DROP_RECURSIVE_CALLS;

   std::string oldHash = getStoredHash(primary);
   std::string newHash = getComputedHash(primary);

   // hashes match, no work needed
   if (oldHash == newHash)
      return;

   // primary hashes mismatch, secondary hashes match
   else if (getStoredHash(secondary) == getComputedHash(secondary)) 
   {
      onPrimaryMismatch(oldHash, newHash);
   }

   // primary and secondary hashes mismatch
   else 
   {
      // TODO: don't do this until the user has resolved any conflicts that
      // may exist, and packrat::status() is clean
      setStoredHash(primary, newHash);
      setStoredHash(secondary, getComputedHash(secondary));
   }
}

// Auto-snapshot -------------------------------------------------------------

// forward declarations
void performAutoSnapshot(const std::string& targetHash);
void pendingSnapshot(PendingSnapshotAction action);

class AutoSnapshot: public async_r::AsyncRProcess
{
public:
   static boost::shared_ptr<AutoSnapshot> create(
         const FilePath& projectDir, 
         const std::string& targetHash)
   {
      boost::shared_ptr<AutoSnapshot> pSnapshot(new AutoSnapshot());
      std::string snapshotCmd;
      Error error = r::exec::RFunction(
            ".rs.getAutoSnapshotCmd",
            projectDir.absolutePath()).call(&snapshotCmd);
      if (error)
         LOG_ERROR(error); // will also be reported in the console

      PACKRAT_TRACE("starting auto snapshot, R command: " << snapshotCmd);
      pSnapshot->setTargetHash(targetHash);
      pSnapshot->start(snapshotCmd.c_str(), projectDir);
      return pSnapshot;
   }

   std::string getTargetHash()
   {
      return targetHash_;
   }
  
private:
   void setTargetHash(const std::string& targetHash)
   {
      targetHash_ = targetHash;
   }

   void onStderr(const std::string& output)
   {
      PACKRAT_TRACE("(auto snapshot) " << output);
   }

   void onStdout(const std::string& output)
   {
      PACKRAT_TRACE("(auto snapshot) " << output);
   }
   
   void onCompleted(int exitStatus)
   {
      PACKRAT_TRACE("finished auto snapshot, exit status = " << exitStatus);
      if (exitStatus != 0)
         return;
      pendingSnapshot(COMPLETE_SNAPSHOT);
   }

   std::string targetHash_;
};

void pendingSnapshot(PendingSnapshotAction action)
{
   static int pendingSnapshots = 0;
   if (action == SET_PENDING_SNAPSHOT)
   {
      pendingSnapshots++;
      PACKRAT_TRACE("snapshot requested while running, queueing ("
                    << pendingSnapshots << ")");
      return;
   }
   else if (action == COMPLETE_SNAPSHOT)
   {
      if (pendingSnapshots > 0)
      {
         PACKRAT_TRACE("executing pending snapshot");
         pendingSnapshots = 0;
         performAutoSnapshot(computeLibraryHash());
      }
      else
      {
         // library and lockfile are now in sync
         setStoredHash(HASH_TYPE_LOCKFILE, computeLockfileHash());
         setStoredHash(HASH_TYPE_LIBRARY, computeLibraryHash());

         // let the client know that it needs to refresh the list of packages
         // (this will also fetch the newly snapshotted status from packrat)
         ClientEvent event(client_events::kInstalledPackagesChanged);
         module_context::enqueClientEvent(event);
      }
   }
}

void performAutoSnapshot(const std::string& newHash)
{
   static boost::shared_ptr<AutoSnapshot> pAutoSnapshot;
   if (pAutoSnapshot && 
       pAutoSnapshot->isRunning())
   {
      // is the requested snapshot for the same state we're already 
      // snapshotting? if it is, ignore the request
      if (pAutoSnapshot->getTargetHash() == newHash)
      {
         PACKRAT_TRACE("snapshot already running (" << newHash << ")");
         return;
      }
      else
      {
         pendingSnapshot(SET_PENDING_SNAPSHOT);
         return;
      }
   }

   // start a new auto-snapshot
   pAutoSnapshot = AutoSnapshot::create(
         projects::projectContext().directory(),
         newHash);
}

// Library and lockfile monitoring -------------------------------------------

// checks to see whether a restore is needed; if no restore is needed, updates
// the stored library hash. optionally notifies the client if a restore is
// necessary.
void checkRestoreNeeded(const std::string& lockfileHash, bool notifyClient)
{
   // check to see if there are any restore actions pending 
   SEXP actions;
   r::sexp::Protect protect;
   Error error = r::exec::RFunction(".rs.pendingRestoreActions", 
         projects::projectContext().directory().absolutePath())
         .call(&actions, &protect);

   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   if (r::sexp::length(actions) == 0)
   {
      PACKRAT_TRACE("no pending restore actions found, updating hash");
      setStoredHash(HASH_TYPE_LOCKFILE, lockfileHash);
   }
   else if (notifyClient)
   {
      PACKRAT_TRACE("found pending restore actions, alerting client");
      json::Value restoreActions;
      r::json::jsonValueFromObject(actions, &restoreActions);
      ClientEvent event(client_events::kPackratRestoreNeeded, restoreActions);
      module_context::enqueClientEvent(event);
   }
}

void onLockfileUpdate(const std::string& oldHash, const std::string& newHash)
{
   checkRestoreNeeded(newHash, true);
}

void onLibraryUpdate(const std::string& oldHash, const std::string& newHash)
{
   performAutoSnapshot(newHash);
}

void onFileChanged(FilePath sourceFilePath)
{
   // ignore file changes while Packrat is running
   if (s_runningPackratAction != PACKRAT_ACTION_NONE)
      return;
   
   // we only care about mutations to files in the Packrat library directory
   // (and packrat.lock)
   FilePath libraryPath = 
      projects::projectContext().directory().complete(PACKRAT_LIB_PATH);

   if (sourceFilePath.filename() == PACKRAT_LOCKFILE)
   {
      PACKRAT_TRACE("detected change to lockfile " << sourceFilePath);
      checkHashes(HASH_TYPE_LOCKFILE, HASH_TYPE_LIBRARY, onLockfileUpdate);
   }
   else if (sourceFilePath.isWithin(libraryPath) && 
            (sourceFilePath.isDirectory() || 
             sourceFilePath.filename() == "DESCRIPTION"))
   {
      // ignore changes in the RStudio-managed manipulate and rstudio 
      // directories and the files within them
      if (sourceFilePath.filename() == "manipulate" ||
          sourceFilePath.filename() == "rstudio" ||
          sourceFilePath.parent().filename() == "manipulate" || 
          sourceFilePath.parent().filename() == "rstudio")
      {
         return;
      }
      PACKRAT_TRACE("detected change to library file " << sourceFilePath);
      checkHashes(HASH_TYPE_LIBRARY, HASH_TYPE_LOCKFILE, onLibraryUpdate);
   }
}

void onPackageLibraryMutated()
{
   // ignore library changes while Packrat is running
   if (s_runningPackratAction != PACKRAT_ACTION_NONE)
      return;
   
   // make sure a Packrat library exists (we don't care about monitoring 
   // mutations to other libraries)
   FilePath libraryPath = 
      projects::projectContext().directory().complete(PACKRAT_LIB_PATH);
   if (libraryPath.exists())
   {
      PACKRAT_TRACE("detected user modification to library");
      checkHashes(HASH_TYPE_LIBRARY, HASH_TYPE_LOCKFILE, onLibraryUpdate);
   }
}

void onFilesChanged(const std::vector<core::system::FileChangeEvent>& changes)
{
   BOOST_FOREACH(const core::system::FileChangeEvent& fileChange, changes)
   {
      FilePath changedFilePath(fileChange.fileInfo().absolutePath());
      onFileChanged(changedFilePath);
   }
}

// RPC -----------------------------------------------------------------------

Error installPackrat(const json::JsonRpcRequest& request,
                    json::JsonRpcResponse* pResponse)
{
   Error error = module_context::installEmbeddedPackage("packrat");
   if (error)
   {
      std::string desc = error.getProperty("description");
      if (desc.empty())
         desc = error.summary();

      module_context::consoleWriteError(desc + "\n");

      LOG_ERROR(error);
   }

   pResponse->setResult(!error);

   return Success();
}

Error getPackratPrerequisites(const json::JsonRpcRequest& request,
                              json::JsonRpcResponse* pResponse)
{
   json::Object prereqJson;
   prereqJson["build_tools_available"] = module_context::canBuildCpp();
   prereqJson["package_available"] = isRequiredPackratInstalled();
   pResponse->setResult(prereqJson);
   return Success();
}


Error getPackratContext(const json::JsonRpcRequest& request,
                        json::JsonRpcResponse* pResponse)
{
   pResponse->setResult(module_context::packratContextAsJson());
   return Success();
}

Error packratBootstrap(const json::JsonRpcRequest& request,
                       json::JsonRpcResponse* pResponse)
{
   // read params
   std::string dir;
   bool enter = false;
   Error error = json::readParams(request.params, &dir, &enter);
   if (error)
      return error;

   // convert to file path then to system encoding
   FilePath dirPath = module_context::resolveAliasedPath(dir);
   dir = string_utils::utf8ToSystem(dirPath.absolutePath());

   // bootstrap
   r::exec::RFunction bootstrap("packrat:::bootstrap");
   bootstrap.addParam("project", dir);
   bootstrap.addParam("enter", enter);
   bootstrap.addParam("restart", false);

   error = bootstrap.call();
   if (error)
      LOG_ERROR(error); // will also be reported in the console

   // return status
   return Success();
}

Error initPackratMonitoring()
{
   FilePath lockfilePath = 
      projects::projectContext().directory().complete(PACKRAT_LOCKFILE_PATH);

   // if there's no lockfile, presume that this isn't a Packrat project
   if (!lockfilePath.exists())
      return Success();

   // listen for changes to the project files 
   PACKRAT_TRACE("found " << lockfilePath.absolutePath() << 
                 ", init monitoring");

   session::projects::FileMonitorCallbacks cb;
   cb.onFilesChanged = onFilesChanged;
   projects::projectContext().subscribeToFileMonitor("Packrat", cb);
   module_context::events().onSourceEditorFileSaved.connect(onFileChanged);
   module_context::events().onPackageLibraryMutated.connect(
         onPackageLibraryMutated);

   return Success();
}

// Notification that a packrat action has either started or
// stopped (indicated by the "running" flag). Possible values for
// action are: "snapshot", "restore", and "clean"
void onPackratAction(const std::string& project,
                     const std::string& action,
                     bool running)
{
   // if this doesn't apply to the current project then skip it
   if (!core::system::realPathsEqual(
          projects::projectContext().directory(), FilePath(project)))
   {
      return;
   }

   static std::string preLibraryHash;
   static std::string preLockfileHash;

   if (running && (s_runningPackratAction != PACKRAT_ACTION_NONE))
      PACKRAT_TRACE("warning: '" << action << "' executed while action " << 
                    s_runningPackratAction << " was already running");

   PACKRAT_TRACE("packrat action '" << action << "' " <<
                 (running ? "started" : "finished"));
   // action started, cache it and return
   if (running) 
   {
      preLibraryHash = computeLibraryHash();
      preLockfileHash = computeLockfileHash();
      s_runningPackratAction = packratAction(action);
      return;
   }

   PackratActionType completedAction = s_runningPackratAction;
   s_runningPackratAction = PACKRAT_ACTION_NONE;
   std::string postLockfileHash = computeLockfileHash();

   // action ended, update hashes accordingly
   switch (completedAction)
   {
      case PACKRAT_ACTION_RESTORE:
         // when a restore completes, check the list of pending restore actions
         // to ensure the command completed successfully. if it did, mark
         // the lockfile as clean.
         checkRestoreNeeded(postLockfileHash, false);
         break;
      case PACKRAT_ACTION_SNAPSHOT:
         // when a snapshot completes, check to see if it mutated the lockfile.
         // if it did, mark the lockfile and library as clean, and have the
         // client refresh the list of installed packages. 
         if (preLockfileHash != postLockfileHash)
         {
            setStoredHash(HASH_TYPE_LOCKFILE, postLockfileHash);
            setStoredHash(HASH_TYPE_LIBRARY, computeLibraryHash());
            ClientEvent event(client_events::kInstalledPackagesChanged);
            module_context::enqueClientEvent(event);
         }
         break;
      default:
         break;
   }
}


SEXP rs_onPackratAction(SEXP projectSEXP, SEXP actionSEXP, SEXP runningSEXP)
{
   std::string project = r::sexp::safeAsString(projectSEXP);
   std::string action = r::sexp::safeAsString(actionSEXP);
   bool running = r::sexp::asLogical(runningSEXP);

   onPackratAction(project, action, running);

   return R_NilValue;
}

} // anonymous namespace

json::Object contextAsJson(const module_context::PackratContext& context)
{
   json::Object contextJson;
   contextJson["available"] = context.available;
   contextJson["applicable"] = context.applicable;
   contextJson["packified"] = context.packified;
   contextJson["mode_on"] = context.modeOn;
   return contextJson;
}

json::Object contextAsJson()
{
   module_context::PackratContext context = module_context::packratContext();
   return contextAsJson(context);
}

Error initialize()
{
   // register packrat action hook
   R_CallMethodDef onPackratActionMethodDef ;
   onPackratActionMethodDef.name = "rs_onPackratAction" ;
   onPackratActionMethodDef.fun = (DL_FUNC) rs_onPackratAction ;
   onPackratActionMethodDef.numArgs = 3;
   r::routines::addCallMethod(onPackratActionMethodDef);

   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "install_packrat", installPackrat))
      (bind(registerRpcMethod, "get_packrat_prerequisites", getPackratPrerequisites))
      (bind(registerRpcMethod, "get_packrat_context", getPackratContext))
      (bind(registerRpcMethod, "packrat_bootstrap", packratBootstrap))
      (bind(sourceModuleRFile, "SessionPackrat.R"));
   Error error = initBlock.execute();
   if (error)
      return error;

   // additional stuff if we are in packrat mode
   if (packratContext().modeOn)
   {
      Error error = r::exec::RFunction(".rs.installPackratActionHook").call();
      if (error)
         LOG_ERROR(error);

      initPackratMonitoring();
   }

   return Success();
}

} // namespace packrat
} // namespace modules

namespace module_context {

PackratContext packratContext()
{
   PackratContext context;

   // NOTE: when we switch to auto-installing packrat we need to update
   // this check to look for R >= whatever packrat requires (we don't
   // need to look for R >= 3.0 as we do for rmarkdown/shiny because
   // build tools will be installed prior to attempting to auto-install
   // the embedded version of packrat

   context.available = isRequiredPackratInstalled();

   context.applicable = context.available &&
                        projects::projectContext().hasProject();

   if (context.applicable)
   {
      FilePath projectDir = projects::projectContext().directory();
      Error error = r::exec::RFunction(
                           "packrat:::checkPackified",
                           /* project = */ projectDir.absolutePath(),
                           /* silent = */ true).call(&context.packified);
      if (error)
         LOG_ERROR(error);

      if (context.packified)
      {
         error = r::exec::RFunction(
                            "packrat:::isPackratModeOn",
                            projectDir.absolutePath()).call(&context.modeOn);
         if (error)
            LOG_ERROR(error);
      }
   }

   return context;
}


json::Object packratContextAsJson()
{
   return modules::packrat::contextAsJson();
}

namespace {

void copyOption(SEXP optionsSEXP, const std::string& listName,
                json::Object* pOptionsJson, const std::string& jsonName,
                bool defaultValue)
{
   bool value = defaultValue;
   Error error = r::sexp::getNamedListElement(optionsSEXP,
                                              listName,
                                              &value,
                                              defaultValue);
   if (error)
   {
      error.addProperty("option", listName);
      LOG_ERROR(error);
   }

   (*pOptionsJson)[jsonName] = value;
}

json::Object defaultPackratOptions()
{
   json::Object optionsJson;
   optionsJson["auto_snapshot"] = true;
   optionsJson["vcs_ignore_lib"] = true;
   optionsJson["vcs_ignore_src"] = false;
   return optionsJson;
}

} // anonymous namespace

json::Object packratOptionsAsJson()
{
   PackratContext context = packratContext();
   if (context.packified)
   {
      // create options to return
      json::Object optionsJson;

      // get the options from packrat
      FilePath projectDir = projects::projectContext().directory();
      r::exec::RFunction getOpts("packrat:::get_opts");
      getOpts.addParam("simplify", false);
      getOpts.addParam("project", module_context::createAliasedPath(
                                                            projectDir));
      r::sexp::Protect rProtect;
      SEXP optionsSEXP;
      Error error = getOpts.call(&optionsSEXP, &rProtect);
      if (error)
      {
         LOG_ERROR(error);
         return defaultPackratOptions();
      }

      // copy the options into json
      copyOption(optionsSEXP, "auto.snapshot",
                 &optionsJson, "auto_snapshot", true);

      copyOption(optionsSEXP, "vcs.ignore.lib",
                 &optionsJson, "vcs_ignore_lib", true);

      copyOption(optionsSEXP, "vcs.ignore.src",
                 &optionsJson, "vcs_ignore_src", false);

      return optionsJson;
   }
   else
   {
      return defaultPackratOptions();
   }
}



} // namespace module_context
} // namespace session

