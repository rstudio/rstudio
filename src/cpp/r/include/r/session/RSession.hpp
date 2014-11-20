/*
 * RSession.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include <core/FilePath.hpp>

#include <R_ext/RStartup.h>
#include <r/session/RSessionUtils.hpp>

#define EX_CONTINUE 100

namespace rscore {
	class Error ;
   class Settings;
} 

namespace r {
namespace session {
   
struct RClientMetrics
{   
   RClientMetrics() 
      : consoleWidth(0), graphicsWidth(0), graphicsHeight(0)
   {
   }
   int consoleWidth ;
   int graphicsWidth ;
   int graphicsHeight;
};
   
struct ROptions
{
   ROptions() :
         useInternet2(true),
         rCompatibleGraphicsEngineVersion(9),
         serverMode(false),
         autoReloadSource(false),
         restoreWorkspace(true),
         saveWorkspace(SA_SAVEASK),
         rProfileOnResume(false)
   {
   }
   rscore::FilePath userHomePath;
   rscore::FilePath userScratchPath;
   rscore::FilePath scopedScratchPath;
   rscore::FilePath logPath;
   rscore::FilePath startupEnvironmentFilePath;
   std::string sessionPort;
   boost::function<rscore::Settings&()> persistentState;
   boost::function<rscore::FilePath()> rEnvironmentDir;
   boost::function<rscore::FilePath()> rHistoryDir;
   boost::function<bool()> alwaysSaveHistory;
   rscore::FilePath rSourcePath;
   std::string rLibsUser;
   std::string rCRANRepos;
   bool useInternet2;
   int rCompatibleGraphicsEngineVersion;
   bool serverMode;
   bool autoReloadSource ;
   bool restoreWorkspace;
   SA_TYPE saveWorkspace;
   bool rProfileOnResume;
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
   RConsoleInput() : cancel(true) {}
   RConsoleInput(const std::string& text) : cancel(false), text(text) {}
   bool cancel ;
   std::string text;
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
   boost::function<rscore::Error(const RInitInfo&)> init ;
   boost::function<bool(const std::string&,bool,RConsoleInput*)> consoleRead;
   boost::function<void(const std::string&)> browseURL;
   boost::function<void(const rscore::FilePath&)> browseFile;
   boost::function<void(const std::string&)> showHelp;
   boost::function<void(const std::string&, rscore::FilePath&, bool)> showFile;
   boost::function<void(const std::string&, int)> consoleWrite;
   boost::function<void()> consoleHistoryReset;
   boost::function<bool(double*,double*)> locator;
   boost::function<rscore::FilePath(bool)> chooseFile;
   boost::function<int(const std::string&)> editFile;
   boost::function<void(const std::string&)> showMessage ;
   boost::function<void(bool)> busy;
   boost::function<void(bool)> deferredInit;
   boost::function<void(const r::session::RSuspendOptions& options)> suspended;
   boost::function<void()> resumed;
   boost::function<bool()> handleUnsavedChanges;
   boost::function<void()> quit;
   boost::function<void(const std::string&)> suicide;
   boost::function<void(bool)> cleanup;
   boost::function<void(int,const rscore::FilePath&)> serialization;
};

// run the session   
rscore::Error run(const ROptions& options, const RCallbacks& callbacks);
   
// deferred deserialization of the session
void ensureDeserialized();
      
// set client metrics 
void setClientMetrics(const RClientMetrics& metrics);

// report a warning to the user and also log it
void reportAndLogWarning(const std::string& warning);

// suspend/resume
bool isSuspendable(const std::string& prompt);
bool suspend(bool force);

struct RSuspendOptions
{
   RSuspendOptions()
      : saveMinimal(false), saveWorkspace(false), excludePackages(false)
   {
   }
   bool saveMinimal;
   bool saveWorkspace;
   bool excludePackages;
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

#endif // R_RSESSION_HPP

