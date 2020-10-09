/*
 * RSession.hpp
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

#ifndef R_RSESSION_HPP
#define R_RSESSION_HPP

#include <string>

#include <boost/function.hpp>

#include <shared_core/FilePath.hpp>

#include <core/r_util/RSessionContext.hpp>

#include <R_ext/RStartup.h>
#include <r/session/RSessionUtils.hpp>

#define EX_CONTINUE 100
#define EX_FORCE    101

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
         useInternet2(true),
         rCompatibleGraphicsEngineVersion(13),
         serverMode(false),
         autoReloadSource(false),
         restoreWorkspace(true),
         saveWorkspace(SA_SAVEASK),
         disableRProfileOnStart(false),
         rProfileOnResume(false),
         restoreEnvironmentOnResume(true),
         packratEnabled(false)
   {
   }
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
   bool useInternet2;
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
   explicit RConsoleInput(const std::string& console) : cancel(true), console(console) {}
   explicit RConsoleInput(const std::string& text, const std::string& console) : 
                          cancel(false), text(text), console(console) {}
   bool cancel;
   std::string text;
   std::string console;
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
   boost::function<bool(const std::string&,bool,RConsoleInput*)> consoleRead;
   boost::function<void(const std::string&)> browseURL;
   boost::function<void(const core::FilePath&)> browseFile;
   boost::function<void(const std::string&)> showHelp;
   boost::function<void(const std::string&, core::FilePath&, bool)> showFile;
   boost::function<void(const std::string&, int)> consoleWrite;
   boost::function<void()> consoleHistoryReset;
   boost::function<bool(double*,double*)> locator;
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
   boost::function<void(int,const core::FilePath&)> serialization;
};

// run the session   
core::Error run(const ROptions& options, const RCallbacks& callbacks);
   
// deferred deserialization of the session
void ensureDeserialized();
      
// set client metrics 
void setClientMetrics(const RClientMetrics& metrics);

// report a warning to the user and also log it
void reportAndLogWarning(const std::string& warning);

// suspend/resume
bool isSuspendable(const std::string& prompt);
bool suspend(bool force, int status, const std::string& envVarSaveBlacklist);

struct RSuspendOptions
{
   RSuspendOptions(int exitStatus)
      : status(exitStatus)
   {
   }

   RSuspendOptions(int exitStatus, const std::string& blacklist) 
      : status(exitStatus),
        envVarSaveBlacklist(blacklist)
   {
   }
   int status;
   bool saveMinimal { false };
   bool saveWorkspace { false };
   bool excludePackages { false };
   std::string envVarSaveBlacklist;
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

// check whether there is a browser context active
bool browserContextActive();

// quit
void quit(bool saveWorkspace, int status = EXIT_SUCCESS);

} // namespace session
} // namespace r
} // namespace rstudio

#endif // R_RSESSION_HPP

