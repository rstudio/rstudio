/*
 * RSession.hpp
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

#ifndef R_RSESSION_HPP
#define R_RSESSION_HPP

#include <string>

#include <boost/function.hpp>

#include <shared_core/json/Json.hpp>
#include <shared_core/FilePath.hpp>

#include <core/r_util/RSessionContext.hpp>

#include <R_ext/RStartup.h>
#include <r/session/RSessionUtils.hpp>

typedef struct SEXPREC* SEXP;

#define kConsoleInputCancel 1
#define kConsoleInputEof    2
#define kConsoleInputNoEcho 4

#define EX_CONTINUE                         100
#define EX_FORCE                            101
#define EX_SUSPEND_RESTART_LAUNCHER_SESSION 102

namespace rstudio {
namespace core {
   class Error;
   class Settings;
} 
}

namespace rstudio {
namespace r {
namespace session {
   
struct RClientMetrics
{   
   RClientMetrics() 
      : consoleWidth(0), buildConsoleWidth(0), graphicsWidth(0),
        graphicsHeight(0), devicePixelRatio(1.0)
   {
   }
   int consoleWidth;
   int buildConsoleWidth;
   int graphicsWidth;
   int graphicsHeight;
   double devicePixelRatio;
};
   
struct ROptions
{
   ROptions() :
         rCompatibleGraphicsEngineVersion(17),
         serverMode(false),
         autoReloadSource(false),
         restoreWorkspace(true),
         saveWorkspace(SA_SAVEASK),
         disableRProfileOnStart(false),
         rProfileOnResume(false),
         restoreEnvironmentOnResume(true),
         packratEnabled(false),
         suspendOnIncompleteStatement(false),
         rMaxConnections(0)
   {
   }
   
   core::FilePath projectPath;
   core::FilePath userHomePath;
   core::FilePath userScratchPath;
   core::FilePath scopedScratchPath;
   core::FilePath sessionScratchPath;
   core::FilePath logPath;
   core::FilePath startupEnvironmentFilePath;
   std::string sessionPort;
   boost::function<core::FilePath()> rEnvironmentDir;
   boost::function<core::FilePath()> rHistoryDir;
   boost::function<bool()> alwaysSaveHistory;
   core::FilePath rSourcePath;
   std::string rLibsUser;
   std::string rCRANUrl;
   std::string rCRANSecondary;
   std::string runScript;
   int rCompatibleGraphicsEngineVersion;
   bool serverMode;
   bool autoReloadSource;
   bool restoreWorkspace;
   SA_TYPE saveWorkspace;
   bool disableRProfileOnStart;
   bool rProfileOnResume;
   bool restoreEnvironmentOnResume;
   core::r_util::SessionScope sessionScope;
   bool packratEnabled;
   bool suspendOnIncompleteStatement;
   int rMaxConnections;
};
      
struct RInitInfo
{
   RInitInfo()
      : resumed(false)
   {
   }
   RInitInfo(bool resumed)
      : resumed(resumed) 
   {
   }
   bool resumed;
};
      
struct RConsoleInput
{
   explicit RConsoleInput()
      : flags(0)
   {
   }
   
   explicit RConsoleInput(int flags)
      : flags(flags)
   {
   }
   
   explicit RConsoleInput(const std::string& text,
                          const std::string& console = "",
                          int flags = 0)
      : text(text),
        console(console),
        flags(flags)
   {
   }
   
   bool isCancel()
   {
      return (flags & kConsoleInputCancel) != 0;
   }
   
   bool isEof()
   {
      return (flags & kConsoleInputEof) != 0;
   }

   bool isNoEcho()
   {
      return (flags & kConsoleInputNoEcho) != 0;
   }
   
   // typically used for hand-constructed RPC requests
   core::json::Array toJsonArray()
   {
      core::json::Array jsonArray;
      jsonArray.push_back(text);
      jsonArray.push_back(console);
      jsonArray.push_back(flags);
      return jsonArray;
   }
   
   std::string text;
   std::string console;
   int flags;
};

// forward declare DisplayState
namespace graphics {
   struct DisplayState;
}
   
// serialization actions
extern const int kSerializationActionSaveDefaultWorkspace;
extern const int kSerializationActionLoadDefaultWorkspace;
extern const int kSerializationActionSuspendSession;
extern const int kSerializationActionResumeSession;
extern const int kSerializationActionCompleted;

struct RSuspendOptions;
struct RCallbacks
{
   boost::function<core::Error(const RInitInfo&)> init;
   boost::function<void()> initComplete;
   boost::function<bool(const std::string&, bool, RConsoleInput*)> consoleRead;
   boost::function<void(const std::string&)> browseURL;
   boost::function<void(const core::FilePath&)> browseFile;
   boost::function<void(const std::string&)> showHelp;
   boost::function<void(const std::string&, core::FilePath&, bool)> showFile;
   boost::function<void(const std::string&, int)> consoleWrite;
   boost::function<void()> consoleHistoryReset;
   boost::function<void()> consoleReset;
   boost::function<bool(double*, double*)> locator;
   boost::function<core::FilePath(bool)> chooseFile;
   boost::function<int(const std::string&)> editFile;
   boost::function<void(const std::string&)> showMessage;
   boost::function<void(bool)> busy;
   boost::function<void(bool)> deferredInit;
   boost::function<void(const r::session::RSuspendOptions& options)> suspended;
   boost::function<void()> resumed;
   boost::function<bool()> handleUnsavedChanges;
   boost::function<void()> quit;
   boost::function<void(const std::string&)> suicide;
   boost::function<void(bool)> cleanup;
   boost::function<void(int, const core::FilePath&)> serialization;
   boost::function<void()> runTests;
   boost::function<void()> runAutomation;
};

// run the session   
core::Error run(const ROptions& options, const RCallbacks& callbacks);
   
// deferred deserialization of the session
void ensureDeserialized();

// returns false if there is a pending deferred deserialization of session data
bool isSessionRestored();
      
// set client metrics 
void setClientMetrics(const RClientMetrics& metrics);

// report a warning to the user
void reportWarningToConsole(const std::string& warning);

// report a warning to the user and also log it
void reportAndLogWarning(const std::string& warning);

// suspend/resume
bool isSuspendable(const std::string& prompt);
bool suspend(bool force, int status, const std::string& ephemeralEnvVars);

struct RSuspendOptions
{
   RSuspendOptions(int exitStatus)
      : status(exitStatus)
   {
   }

   RSuspendOptions(int exitStatus, const std::string& ephemeral) 
      : status(exitStatus),
        ephemeralEnvVars(ephemeral)
   {
   }
   
   int status;
   bool saveMinimal = false;
   bool saveWorkspace = false;
   bool excludePackages = false;
   std::string ephemeralEnvVars;
   std::string afterRestartCommand;
   std::string builtPackagePath;
};

void suspendForRestart(const RSuspendOptions& options);
   
// set save action
extern const int kSaveActionNoSave;
extern const int kSaveActionSave;
extern const int kSaveActionAsk;
void setSaveAction(int saveAction);

// image dirty state
void setImageDirty(bool imageDirty);
bool imageIsDirty();

// prompt state -- set from RReadConsole. true when R is at the top-level
// REPL (either default "> " or continuation "+ " prompt), false during
// browse prompts, readline(), scan(), or other sub-prompts.
void setAtTopLevelPrompt(bool atPrompt);
bool atTopLevelPrompt();

// browser context state -- set from RReadConsole based on the prompt
void setBrowserActive(bool active);
bool browserContextActive();

// browser environment -- captured by .rs.captureCurrentEnvironment()
// which is injected into the browser REPL before user input.
void setBrowserEnv(SEXP env);
SEXP browserEnv();

// Returns true when R is at the top-level prompt with no evaluation contexts
// on the stack.
bool isAtTopLevel();

// Returns true when we are in a "browse" debugging state: at a browse prompt
// with the browser environment inside a function (not the global env).
bool isBrowseActive();

// Find the function context associated with the browser, or at a given depth.
// Returns false if no matching context was found.
bool getFunctionContext(int depth, bool browsing, int* pDepth, SEXP* pEnv);

// Check if the topmost function on the stack is a debugger-internal
// function (has the "hideFromDebugger" attribute).
bool inDebugHiddenContext();

// quit
void quit(bool saveWorkspace, int status = EXIT_SUCCESS);

void setResumeCallbacks(boost::function<void()> before, boost::function<void()> after);

} // namespace session
} // namespace r
} // namespace rstudio

#endif // R_RSESSION_HPP

