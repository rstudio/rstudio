/*
 * SessionPackrat.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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
#include <r/RRoutines.hpp>
#include <r/ROptions.hpp>
#include <r/session/RSessionUtils.hpp>

#include <session/projects/SessionProjects.hpp>
#include <session/SessionAsyncRProcess.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/SessionPersistentState.hpp>

#include "SessionPackages.hpp"
#include "session-config.h"

using namespace rstudio::core;

#ifdef TRACE_PACKRAT_OUTPUT
#define PACKRAT_TRACE(x) \
   std::cerr << "(packrat) " << x << std::endl;
#else
#define PACKRAT_TRACE(x)
#endif

#define kPackratFolder "packrat/"
#define kPackratLockfile "packrat.lock"
#define kPackratLibPath kPackratFolder "lib"
#define kPackratLockfilePath kPackratFolder kPackratLockfile

#define kPackratActionRestore "restore"
#define kPackratActionClean "clean"
#define kPackratActionSnapshot "snapshot"

#define kAutoSnapshotName "auto.snapshot"
#define kAutoSnapshotDefault false

#define kInvalidHashValue "--------"

// aligned with a corresponding protocol version in Packrat (see
// getPackageRStudioProtocol), and bumped in Packrat to indicate breaks in
// compatibility with older versions of RStudio
#define kPackratRStudioProtocolVersion 1

namespace rstudio {
namespace session {

namespace modules { 
namespace packrat {

namespace {

// Current Packrat actions and state -----------------------------------------

enum PackratActionType 
{
   PACKRAT_ACTION_NONE     = 0,
   PACKRAT_ACTION_SNAPSHOT = 1,
   PACKRAT_ACTION_RESTORE  = 2,
   PACKRAT_ACTION_CLEAN    = 3,
   PACKRAT_ACTION_UNKNOWN  = 4,
   PACKRAT_ACTION_MAX      = PACKRAT_ACTION_UNKNOWN
};

enum PackratHashType
{
   HASH_TYPE_LOCKFILE = 0,
   HASH_TYPE_LIBRARY  = 1
};

// Hash states are used for two purposes:
// 1) To ascertain whether an object has undergone a meaningful change--for
//    instance, if the library state is different after an operation
// 2) To track the last-resolved state of an object, as an aid for discovering
//    what actions are appropriate on the object
//
// As an example, take the lockfile hash:
// HASH_STATE_COMPUTED != HASH_STATE_OBSERVED
//    The client's view reflects a different lockfile state. Refresh the client
//    view.
// HASH_STATE_OBSERVED != HASH_STATE_RESOLVED
//    The content in the lockfile has changed since the last time a snapshot or
//    restore was performed. The user should perform a 'restore'.
// HASH_STATE_COMPUTED == HASH_STATE_RESOLVED
//    The content of the lockfile is up-to-date and no action is needed.
//
enum PackratHashState
{
   HASH_STATE_RESOLVED = 0,  // The state last known to be consistent (stored)
   HASH_STATE_OBSERVED = 1,  // The state last viewed by the client (stored)
   HASH_STATE_COMPUTED = 2   // The current state (not stored)
};

enum PendingSnapshotAction
{
   SET_PENDING_SNAPSHOT   = 0,
   CLEAR_PENDING_SNAPSHOT = 1,
   EXEC_PENDING_SNAPSHOT  = 2,
   COMPLETE_SNAPSHOT      = 3
};

PackratActionType packratAction(const std::string& str)
{
   if (str == kPackratActionSnapshot) 
      return PACKRAT_ACTION_SNAPSHOT;
   else if (str == kPackratActionRestore)
      return PACKRAT_ACTION_RESTORE;
   else if (str == kPackratActionClean)
      return PACKRAT_ACTION_CLEAN;
   else 
      return PACKRAT_ACTION_UNKNOWN;
}

std::string packratActionName(PackratActionType action)
{
   switch (action) {
      case PACKRAT_ACTION_SNAPSHOT:
         return kPackratActionSnapshot;
         break;
      case PACKRAT_ACTION_RESTORE: 
         return kPackratActionRestore;
         break;
      case PACKRAT_ACTION_CLEAN:
         return kPackratActionClean;
         break;
      default:
         return "";
   }
}

static PackratActionType s_runningPackratAction = PACKRAT_ACTION_NONE;
static bool s_autoSnapshotPending = false;
static bool s_autoSnapshotRunning = false;
static bool s_packageStateChanged = false;
static bool s_pendingLibraryHash  = false;


// Forward declarations ------------------------------------------------------

void performAutoSnapshot(const std::string& targetHash, bool queue);
void pendingSnapshot(PendingSnapshotAction action);
bool getPendingActions(PackratActionType action, bool useCached,
                       const std::string& libraryHash,
                       const std::string& lockfileHash, json::Value* pActions);
bool resolveStateAfterAction(PackratActionType action, 
                             PackratHashType hashType);
std::string computeLockfileHash();
std::string computeLibraryHash();

// Library and lockfile hashing and comparison -------------------------------

// Returns the storage key for the given hash type and state
std::string keyOfHashType(PackratHashType hashType, PackratHashState hashState)
{
   std::string hashKey  = "packrat";
   hashKey.append(hashType == HASH_TYPE_LOCKFILE ? "Lockfile" : "Library");
   hashKey.append(hashState == HASH_STATE_OBSERVED ? "Observed" : "Resolved");
   return hashKey;
}

// Given the hash type and state, return the hash
std::string getHash(PackratHashType hashType, PackratHashState hashState)
{
   // For computed hashes, do the computation
   if (hashState == HASH_STATE_COMPUTED)
   {
      if (hashType == HASH_TYPE_LOCKFILE)
         return computeLockfileHash();
      else
         return computeLibraryHash();
   }
   else
      return persistentState().getStoredHash(keyOfHashType(hashType, 
                                                           hashState));
}

void setStoredHash(PackratHashType hashType, PackratHashState hashState,
                   const std::string& hashValue)
{
   PACKRAT_TRACE("updating " << keyOfHashType(hashType, hashState) << 
                 " -> " << hashValue);
   persistentState().setStoredHash(keyOfHashType(hashType, hashState), 
                                   hashValue);
}

std::string updateHash(PackratHashType hashType, PackratHashState hashState, 
                       const std::string& computedHash = std::string())
{
   // compute the hash if not already provided
   std::string newHash = computedHash.empty() ?
      getHash(hashType, HASH_STATE_COMPUTED) :
      computedHash;

   std::string oldHash = getHash(hashType, hashState);
   if (newHash != oldHash)
      setStoredHash(hashType, hashState, newHash);

   return newHash;
}

// adds content from the given file to the given file if it's a 
// DESCRIPTION file (used to summarize library content for hashing)
void addDescContent(const FilePath& path, std::string* pDescContent)
{
   std::string newDescContent;
   if (path.getFilename() == "DESCRIPTION")
   {
      Error error = readStringFromFile(path, &newDescContent);
      // include the path of the file; on Windows the DESCRIPTION file moves
      // inside the library post-installation
      pDescContent->append(path.getAbsolutePath());
      pDescContent->append(newDescContent);
   }
}

// computes a hash of the content of all DESCRIPTION files in the Packrat
// private library
std::string computeLibraryHash()
{
   // figure out what library paths are being used by Packrat
   std::string libraryPath;
   Error error = r::exec::RFunction("packrat:::libDir").call(&libraryPath);
   if (error)
   {
      LOG_ERROR(error);
      return "";
   }

   // find DESCRIPTION files for the packages in these libraries
   std::string descFileContent;

   std::vector<FilePath> pkgPaths;
   error = FilePath(libraryPath).getChildren(pkgPaths);
   if (error)
      LOG_ERROR(error);

   for (auto& pkgPath : pkgPaths)
   {
      FilePath descPath = pkgPath.completeChildPath("DESCRIPTION");
      if (descPath.exists())
         addDescContent(descPath, &descFileContent);
   }

   if (descFileContent.empty())
      return "";

   return hash::crc32HexHash(descFileContent);
}

// computes the hash of the current project's lockfile
std::string computeLockfileHash()
{
   FilePath lockFilePath =
      projects::projectContext().directory().completePath(kPackratLockfilePath);

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

void checkHashes(
      PackratHashType hashType, 
      PackratHashState hashState,
      boost::function<void(const std::string&, const std::string&)> onMismatch)
{
   // if a request to check hashes comes in while we're already checking hashes,
   // drop it: it's very likely that the file monitor has discovered a change
   // to a file we've already hashed.
   DROP_RECURSIVE_CALLS;

   std::string oldHash = getHash(hashType, hashState);
   std::string newHash = getHash(hashType, HASH_STATE_COMPUTED);

   // hashes match, no work needed
   if (oldHash == newHash)
      return;
   else 
      onMismatch(oldHash, newHash);
}

bool hashStatesMatch(PackratHashType hashType, PackratHashState state1, 
                     PackratHashState state2)
{
   std::string hash1 = getHash(hashType, state1);
   std::string hash2 = getHash(hashType, state2);
   if (hash1.empty() || hash2.empty())
      return true;
   return hash1 == hash2;
}

bool isHashUnresolved(PackratHashType hashType)
{
   return !hashStatesMatch(hashType, HASH_STATE_OBSERVED, 
                           HASH_STATE_RESOLVED);
}

// Auto-snapshot -------------------------------------------------------------

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
            projectDir.getAbsolutePath()).call(&snapshotCmd);
      if (error)
         LOG_ERROR(error); // will also be reported in the console

      PACKRAT_TRACE("starting auto snapshot, R command: " << snapshotCmd);
      pSnapshot->setTargetHash(targetHash);
      pSnapshot->start(snapshotCmd.c_str(), projectDir, 
                       async_r::R_PROCESS_VANILLA);
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
   switch (action)
   {
      case SET_PENDING_SNAPSHOT:
         if (!s_autoSnapshotPending)
         {
            PACKRAT_TRACE("queueing pending snapshot");
            s_autoSnapshotPending = true;
         }
         break;
      case CLEAR_PENDING_SNAPSHOT:
         s_autoSnapshotPending = false;
         break;
      case EXEC_PENDING_SNAPSHOT:
      case COMPLETE_SNAPSHOT:
      {
         // prefer execution of any pending snapshots in either case
         if (s_autoSnapshotPending)
         {
            PACKRAT_TRACE("executing pending snapshot");
            performAutoSnapshot(computeLibraryHash(), false);
         }
         // when a snapshot finishes, resolve the library state
         else if (action == COMPLETE_SNAPSHOT)
         {
            s_autoSnapshotRunning = false;
            // if there are remaining actions, re-emit the state to the client 
            if (!resolveStateAfterAction(PACKRAT_ACTION_SNAPSHOT, 
                                         HASH_TYPE_LOCKFILE))
            {
               s_packageStateChanged = true;
            }
         }
      }
   }
}


// Checks Packrat options to see whether auto-snapshotting is enabled 
bool isAutoSnapshotEnabled()
{
   bool enabled = kAutoSnapshotDefault;
   r::sexp::Protect rProtect;
   SEXP optionsSEXP;
   Error error = modules::packrat::getPackratOptions(&optionsSEXP, &rProtect);
   if (!error)
   {
      error = r::sexp::getNamedListElement(optionsSEXP,
                                           kAutoSnapshotName,
                                           &enabled,
                                           kAutoSnapshotDefault);
   }
   return enabled;
}

// Performs an automatic snapshot of the Packrat library, either immediately
// or later (if queue == false).  In either case, does not perform a snapshot
// if one is already running for the requested state, or if there are 
// unresolved changes in the lockfile.
void performAutoSnapshot(const std::string& newHash, bool queue)
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

   // make sure we have no unresolved lockfile changes
   if (isHashUnresolved(HASH_TYPE_LOCKFILE))
   {
      PACKRAT_TRACE("not performing automatic snapshot; resolve pending (" <<
                    getHash(HASH_TYPE_LOCKFILE, HASH_STATE_RESOLVED) << ", " <<
                    getHash(HASH_TYPE_LOCKFILE, HASH_STATE_COMPUTED) << ")");
      return;
   }

   if (queue && isAutoSnapshotEnabled())
   {
      pendingSnapshot(SET_PENDING_SNAPSHOT);
   }
   else
   {
      if (!isAutoSnapshotEnabled())
      {
         PACKRAT_TRACE("not performing automatic snapshot; automatic "
                       "snapshots currently disabled in options");
         return;
      }
      // start a new auto-snapshot
      pendingSnapshot(CLEAR_PENDING_SNAPSHOT);
      s_autoSnapshotRunning = true;
      pAutoSnapshot = AutoSnapshot::create(
            projects::projectContext().directory(),
            newHash);
   }
}

// Library and lockfile monitoring -------------------------------------------

// indicates whether there are any actions that would be performed if the given
// action were executed; if there are actions, they are returned in pActions.
bool getPendingActions(PackratActionType action, bool useCached,
                       const std::string& libraryHash,
                       const std::string& lockfileHash, json::Value* pActions)
{
   // checking for actions can be expensive--if this call is for the same 
   // action with the same library and lockfile states for which we previously
   // queried for that action, serve cached state
   static std::string cachedLibraryHash[PACKRAT_ACTION_MAX];
   static std::string cachedLockfileHash[PACKRAT_ACTION_MAX];
   static bool cachedResult[PACKRAT_ACTION_MAX];
   static json::Value cachedActions[PACKRAT_ACTION_MAX];
   if (libraryHash == cachedLibraryHash[action] &&
       lockfileHash == cachedLockfileHash[action] &&
       useCached)
   {
      PACKRAT_TRACE("using cached action list for action '" << 
                    packratActionName(action) << "' (" << libraryHash << ", " <<
                    lockfileHash << ")");
      if (pActions && !cachedActions[action].isNull())
         *pActions = cachedActions[action];
      return cachedResult[action];
   }

   PACKRAT_TRACE("caching action list for action '" << 
                 packratActionName(action) << "' (" << libraryHash << ", " <<
                 lockfileHash << ")");

   // record state for later service from cache
   cachedLibraryHash[action] = libraryHash;
   cachedLockfileHash[action] = lockfileHash;
   cachedActions[action] = json::Value();

   // get the list of actions from Packrat
   SEXP actions;
   r::sexp::Protect protect;
   Error error = r::exec::RFunction(".rs.pendingActions", 
         packratActionName(action),
                                    projects::projectContext().directory().getAbsolutePath())
         .call(&actions, &protect);

   // if an error occurs, presume that there are pending actions (i.e. don't
   // resolve the state) 
   if (error)
   {
      LOG_ERROR(error);
      return (cachedResult[action] = true);
   }

   // if an empty list comes back, we can savely resolve the state
   if (r::sexp::length(actions) == 0)
      return (cachedResult[action] = false);

   // convert the action list to JSON if needed
   error = r::json::jsonValueFromObject(actions, &(cachedActions[action]));
   if (pActions)
      *pActions = cachedActions[action];

   return (cachedResult[action] = !error);
}

void onLockfileUpdate(const std::string& oldHash, const std::string& newHash)
{
   // if the lockfile changed, refresh to show the new Packrat state
   s_packageStateChanged = true;
}

void onLibraryUpdate(const std::string& oldHash, const std::string& newHash)
{
   // perform an auto-snapshot if we don't have a pending restore
   if (!isHashUnresolved(HASH_TYPE_LOCKFILE)) 
   {
      performAutoSnapshot(newHash, true);
   }
   else 
   {
      PACKRAT_TRACE("lockfile observed hash " << 
                    getHash(HASH_TYPE_LOCKFILE, HASH_STATE_OBSERVED) << 
                    " doesn't match resolved hash " <<
                    getHash(HASH_TYPE_LOCKFILE, HASH_STATE_RESOLVED) <<
                    ", skipping auto snapshot");
   }

   // send the new state to the client if Packrat isn't busy
   if (s_runningPackratAction == PACKRAT_ACTION_NONE)
      s_packageStateChanged = true;
}

void onFileChanged(FilePath sourceFilePath)
{
   // ignore file changes while Packrat is running
   if (s_runningPackratAction != PACKRAT_ACTION_NONE)
      return;
   
   // we only care about mutations to files in the Packrat library directory
   // (and packrat.lock)
   FilePath libraryPath =
      projects::projectContext().directory().completePath(kPackratLibPath);

   if (sourceFilePath.getFilename() == kPackratLockfile)
   {
      PACKRAT_TRACE("detected change to lockfile " << sourceFilePath);
      checkHashes(HASH_TYPE_LOCKFILE, HASH_STATE_OBSERVED, onLockfileUpdate);
   }
   else if (sourceFilePath.isWithin(libraryPath) && 
            (sourceFilePath.isDirectory() || 
             sourceFilePath.getFilename() == "DESCRIPTION"))
   {
      // ignore changes in the RStudio-managed manipulate and rstudio 
      // directories and the files within them
      if (sourceFilePath.getFilename() == "manipulate" ||
          sourceFilePath.getFilename() == "rstudio" ||
          sourceFilePath.getParent().getFilename() == "manipulate" ||
          sourceFilePath.getParent().getFilename() == "rstudio")
      {
         return;
      }
      PACKRAT_TRACE("detected change to library file " << sourceFilePath);
      s_pendingLibraryHash = true;
   }
}

void onFilesChanged(const std::vector<core::system::FileChangeEvent>& changes)
{
   for (const core::system::FileChangeEvent& fileChange : changes)
   {
      FilePath changedFilePath(fileChange.fileInfo().absolutePath());
      onFileChanged(changedFilePath);
   }
}

void onConsolePrompt(const std::string& prompt)
{
   // Execute pending auto-snapshots if any exist. We don't execute these
   // immediately on detecting a change since a bulk change may be in-flight
   // (e.g. installing a package and several upon which it depends)
   pendingSnapshot(EXEC_PENDING_SNAPSHOT);
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
         desc = error.getSummary();

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
   using namespace module_context;
   prereqJson["build_tools_available"] = canBuildCpp();
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
   dir = string_utils::utf8ToSystem(dirPath.getAbsolutePath());

   // bootstrap
   r::exec::RFunction bootstrap("packrat:::init");
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
      projects::projectContext().directory().completePath(kPackratLockfilePath);

   // if there's no lockfile, presume that this isn't a Packrat project
   if (!lockfilePath.exists())
      return Success();

   // listen for changes to the project files 
   PACKRAT_TRACE("found " << lockfilePath.getAbsolutePath() <<
                          ", init monitoring");

   session::projects::FileMonitorCallbacks cb;
   cb.onFilesChanged = onFilesChanged;
   projects::projectContext().subscribeToFileMonitor("Packrat", cb);

   using namespace module_context;
   events().onSourceEditorFileSaved.connect(onFileChanged);
   events().onConsolePrompt.connect(onConsolePrompt);

   return Success();
}

// runs after an (auto) snapshot or restore; returns whether the state was
// resolved successfully 
bool resolveStateAfterAction(PackratActionType action, 
                             PackratHashType hashType)
{
   // compute the new library and lockfile states
   std::string newLibraryHash = 
      getHash(HASH_TYPE_LIBRARY, HASH_STATE_COMPUTED);
   std::string newLockfileHash = 
      getHash(HASH_TYPE_LOCKFILE, HASH_STATE_COMPUTED);

   // mark the library resolved if there are no pending snapshot actions
   bool hasPendingSnapshotActions = 
      getPendingActions(PACKRAT_ACTION_SNAPSHOT, true, newLibraryHash,
                        newLockfileHash, nullptr);
   if (!hasPendingSnapshotActions)
      updateHash(HASH_TYPE_LIBRARY, HASH_STATE_RESOLVED, newLibraryHash);

   // mark the lockfile resolved if there are no pending restore actions   
   bool hasPendingRestoreActions = 
      getPendingActions(PACKRAT_ACTION_RESTORE, true, newLibraryHash,
                        newLockfileHash, nullptr);
   if (hasPendingRestoreActions)
   {
      // if we just finished a snapshot and there are pending restore actions,
      // dirty the lockfile so they'll get applied 
      if (action == PACKRAT_ACTION_SNAPSHOT && !hasPendingSnapshotActions) 
         setStoredHash(HASH_TYPE_LOCKFILE, HASH_STATE_RESOLVED, 
                       kInvalidHashValue);
   }
   else
      updateHash(HASH_TYPE_LOCKFILE, HASH_STATE_RESOLVED, newLockfileHash);

   // if the action changed the underlying store, send the new state to the
   // client
   bool hashChangedState = 
      !hashStatesMatch(hashType, HASH_STATE_OBSERVED, HASH_STATE_COMPUTED);
   if (hashChangedState)
      s_packageStateChanged = true;

   return hashChangedState || !(hasPendingSnapshotActions ||
                                hasPendingRestoreActions);
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

   if (running && (s_runningPackratAction != PACKRAT_ACTION_NONE))
   {
      PACKRAT_TRACE("warning: '" << action << "' executed while action " << 
                    s_runningPackratAction << " was already running");
   }

   PACKRAT_TRACE("packrat action '" << action << "' " <<
                 (running ? "started" : "finished"));
   // action started, cache it and return
   if (running) 
   {
      s_runningPackratAction = packratAction(action);
      return;
   }

   PackratActionType completedAction = s_runningPackratAction;
   s_runningPackratAction = PACKRAT_ACTION_NONE;

   // action ended, update hashes accordingly
   switch (completedAction)
   {
      case PACKRAT_ACTION_RESTORE:
         resolveStateAfterAction(PACKRAT_ACTION_RESTORE, HASH_TYPE_LIBRARY);
         break;
      case PACKRAT_ACTION_SNAPSHOT:
         resolveStateAfterAction(PACKRAT_ACTION_SNAPSHOT, HASH_TYPE_LOCKFILE);
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


void detectReposChanges()
{
   static SEXP s_lastReposSEXP = R_UnboundValue;
   SEXP reposSEXP = r::options::getOption("repos");
   if (s_lastReposSEXP == R_UnboundValue)
   {
      s_lastReposSEXP = reposSEXP;
   }
   else if (reposSEXP != s_lastReposSEXP)
   {
      s_lastReposSEXP = reposSEXP;
      performAutoSnapshot(getHash(HASH_TYPE_LIBRARY, HASH_STATE_COMPUTED), 
                          false);
   }
}

void onDetectChanges(module_context::ChangeSource source)
{
   if (source == module_context::ChangeSourceREPL)
      detectReposChanges();

   // Update hashes.
   if (s_pendingLibraryHash)
   {
      s_pendingLibraryHash = false;
      checkHashes(HASH_TYPE_LIBRARY, HASH_STATE_OBSERVED, onLibraryUpdate);
   }

   // If the package state has changed, report those changes to the client.
   if (s_packageStateChanged)
   {
      s_packageStateChanged = false;
      packages::enquePackageStateChanged();
   }
}

void activatePackagesIfPendingActions()
{
   // activate the packages pane if the library or lockfile states are 
   // unresolved (i.e. there is a pending snapshot or restore)
   if (!(hashStatesMatch(HASH_TYPE_LOCKFILE, HASH_STATE_COMPUTED, 
                         HASH_STATE_RESOLVED) &&
         hashStatesMatch(HASH_TYPE_LIBRARY, HASH_STATE_COMPUTED, 
                         HASH_STATE_RESOLVED)))
   {
      module_context::activatePane("packages");
   }
}

void afterSessionInitHook(bool newSession)
{
   // additional stuff if we are in packrat mode
   if (module_context::packratContext().modeOn)
   {
      Error error = r::exec::RFunction(".rs.installPackratActionHook").call();
      if (error)
         LOG_ERROR(error);

      error = initPackratMonitoring();
      if (error)
         LOG_ERROR(error);

      module_context::events().onDetectChanges.connect(onDetectChanges);

      // check whether there are pending actions and if there are then
      // ensure that the packages pane is activated. we do this on a
      // delayed basis to allow all of the other IDE initialization
      // RPC calls (list files, list environment, etc.) to occur before
      // we begin the (more latent) listing of packages + actions
      if (newSession)
      {
         activatePackagesIfPendingActions();
      }
   }
}

} // anonymous namespace

Error getPackratOptions(SEXP* pOptionsSEXP, r::sexp::Protect* pRProtect)
{
   FilePath projectDir = projects::projectContext().directory();
   r::exec::RFunction getOpts("packrat:::get_opts");
   getOpts.addParam("simplify", false);
   getOpts.addParam("project", module_context::createAliasedPath(projectDir));

   return getOpts.call(pOptionsSEXP, pRProtect);
}

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

Error getPackratActions(const json::JsonRpcRequest& request,
                        json::JsonRpcResponse* pResponse)
{
   json::Value restoreActions;
   json::Value snapshotActions;
   json::Object json;

   std::string oldLibraryHash = 
      getHash(HASH_TYPE_LIBRARY, HASH_STATE_OBSERVED);

   // compute new hashes and mark them observed
   std::string newLibraryHash = 
      updateHash(HASH_TYPE_LIBRARY, HASH_STATE_OBSERVED);
   std::string newLockfileHash = 
      updateHash(HASH_TYPE_LOCKFILE, HASH_STATE_OBSERVED);

   // take an auto-snapshot if the library hashes don't match: it's necessary
   // to do this here in case we've observed the new hash before the file
   // monitor had a chance to see it
   if (oldLibraryHash != newLibraryHash)
      performAutoSnapshot(newLibraryHash, false);

   // if we're waiting for an auto snapshot or Packrat is doing work, don't
   // bug the user with a list of actions until that work is finished. 
   if (!s_autoSnapshotPending && !s_autoSnapshotRunning && 
       s_runningPackratAction == PACKRAT_ACTION_NONE) 
   {
      // check for pending restore and snapshot actions
      bool hasPendingSnapshotActions = 
         getPendingActions(PACKRAT_ACTION_SNAPSHOT, false, newLibraryHash,
                           newLockfileHash, &snapshotActions);
      bool hasPendingRestoreActions = 
         getPendingActions(PACKRAT_ACTION_RESTORE, false, newLibraryHash,
                           newLockfileHash, &restoreActions);
      
      // if the state could be interpreted as either a pending restore or a
      // pending snapsot, try to guess which is appropriate
      if (hasPendingRestoreActions && hasPendingSnapshotActions)
      {
         bool libraryDirty = 
            newLibraryHash != getHash(HASH_TYPE_LIBRARY, HASH_STATE_RESOLVED);
         bool lockfileDirty = 
            newLockfileHash != getHash(HASH_TYPE_LOCKFILE, HASH_STATE_RESOLVED);

         // hide the list of pending restore actions if we think a snapshot is
         // appropriate, and vice versa
         if (libraryDirty && !lockfileDirty)
            restoreActions = json::Value();
         if (!libraryDirty && lockfileDirty)
            snapshotActions = json::Value();
      }
   }
      
   json["restore_actions"] = restoreActions;
   json["snapshot_actions"] = snapshotActions;
    
   pResponse->setResult(json);
   return Success();
}

Error initialize()
{
   // register deferred init (since we need to call into the packrat package
   // we need to wait until all other modules initialize and all R routines
   // are initialized -- otherwise the package load hook attempts to call
   // rs_packageLoaded and can't find it
   //
   // we want this to occur _after_ packrat has done its own initialization,
   // so we ensure that the package hooks are run before this
   module_context::events().afterSessionInitHook.connect(afterSessionInitHook);

   // register packrat action hook
   RS_REGISTER_CALL_METHOD(rs_onPackratAction);

   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "install_packrat", installPackrat))
      (bind(registerRpcMethod, "get_packrat_prerequisites", getPackratPrerequisites))
      (bind(registerRpcMethod, "get_packrat_context", getPackratContext))
      (bind(registerRpcMethod, "packrat_bootstrap", packratBootstrap))
      (bind(registerRpcMethod, "get_packrat_actions", getPackratActions))
      (bind(sourceModuleRFile, "SessionPackrat.R"));
   return initBlock.execute();
}

} // namespace packrat
} // namespace modules

namespace module_context {

bool isRequiredPackratInstalled()
{
   return getPackageCompatStatus("packrat", "0.4.6",
                                  kPackratRStudioProtocolVersion) == COMPAT_OK;
}

PackratContext packratContext()
{
   PackratContext context;

   // packrat is available in R >= 3.0
   context.available = r::session::utils::isR3();

   context.applicable = context.available &&
                        projects::projectContext().hasProject();

   // if it's applicable and installed then check packrat status
   if (context.applicable && isRequiredPackratInstalled())
   {
      FilePath projectDir = projects::projectContext().directory();
      std::string projectPath =
         string_utils::utf8ToSystem(projectDir.getAbsolutePath());
      
      // check and see if the project has been packified
      Error error = r::exec::RFunction(".rs.isPackified")
            .addParam(projectPath)
            .call(&context.packified);
      if (error)
         LOG_ERROR(error);

      if (context.packified)
      {
         Error error = r::exec::RFunction(
                  ".rs.isPackratModeOn",
                  projectPath).call(&context.modeOn);
         if (error)
            LOG_ERROR(error);

         // cache results in project directory to make packrat status information available on session start
         persistentState().settings().set("packratEnabled", context.modeOn);
      }
   }
   return context;
}


json::Object packratContextAsJson()
{
   return modules::packrat::contextAsJson();
}

namespace {

template <typename T>
void copyOption(SEXP optionsSEXP, const std::string& listName,
                json::Object* pOptionsJson, const std::string& jsonName,
                T defaultValue)
{
   T value = defaultValue;
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

void copyOptionArrayString(SEXP optionsSEXP, const std::string& listName,
                           json::Object* pOptionsJson, const std::string& jsonName,
                           std::vector<std::string> defaultValue)
{
   std::vector<std::string> value = defaultValue;
   Error error = r::sexp::getNamedListElement(optionsSEXP,
                                              listName,
                                              &value,
                                              defaultValue);

   if (error)
   {
      error.addProperty("option", listName);
      LOG_ERROR(error);
   }

   (*pOptionsJson)[jsonName] = json::toJsonArray(value);
}

json::Object defaultPackratOptions()
{
   json::Object optionsJson;
   optionsJson["use_packrat"] = false;
   optionsJson["auto_snapshot"] = kAutoSnapshotDefault;
   optionsJson["vcs_ignore_lib"] = true;
   optionsJson["vcs_ignore_src"] = false;
   optionsJson["use_cache"] = false;
   optionsJson["external_packages"] = json::toJsonArray(std::vector<std::string>());
   optionsJson["local_repos"] = json::toJsonArray(std::vector<std::string>());
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

      optionsJson["use_packrat"] = true;

      r::sexp::Protect rProtect;
      SEXP optionsSEXP;
      Error error = modules::packrat::getPackratOptions(&optionsSEXP, 
                                                        &rProtect);
      if (error)
      {
         LOG_ERROR(error);
         return defaultPackratOptions();
      }

      // copy the options into json
      copyOption(optionsSEXP, kAutoSnapshotName,
                 &optionsJson, "auto_snapshot", true);

      copyOption(optionsSEXP, "vcs.ignore.lib",
                 &optionsJson, "vcs_ignore_lib", true);

      copyOption(optionsSEXP, "vcs.ignore.src",
                 &optionsJson, "vcs_ignore_src", false);

      copyOption(optionsSEXP, "use.cache",
                 &optionsJson, "use_cache", false);

      copyOptionArrayString(optionsSEXP, "external.packages",
                            &optionsJson, "external_packages",
                            std::vector<std::string>());

      copyOptionArrayString(optionsSEXP, "local.repos",
                            &optionsJson, "local_repos",
                            std::vector<std::string>());


      return optionsJson;
   }
   else
   {
      return defaultPackratOptions();
   }
}



} // namespace module_context
} // namespace session
} // namespace rstudio

