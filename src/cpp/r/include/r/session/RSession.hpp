/*
 * RSession.hpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
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

namespace core {
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
         rCompatibleGraphicsEngineVersion(8),
         serverMode(false),
         autoReloadSource(false),
         shellEscape(false),
         restoreWorkspace(true),
         saveWorkspace(SA_SAVEASK),
         consoleHistorySize(250)
   {
   }
   core::FilePath userHomePath;
   core::FilePath userScratchPath;
   core::FilePath defaultWorkingDir;
   core::FilePath startupEnvironmentFilePath;
   boost::function<core::FilePath()> rEnvironmentDir;
   core::FilePath rSourcePath;
   core::FilePath rLibsExtra;
   std::string rLibsUser;
   std::string rCRANRepos;
   int rCompatibleGraphicsEngineVersion;
   bool serverMode;
   bool autoReloadSource ;
   bool shellEscape;
   bool restoreWorkspace;
   SA_TYPE saveWorkspace;
   int consoleHistorySize;
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

struct RCallbacks
{
   boost::function<core::Error(const RInitInfo&)> init ;
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
   boost::function<void(const std::string&)> showMessage ;
   boost::function<void(bool)> busy;
   boost::function<void()> suspended;
   boost::function<void()> resumed;
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
bool suspend(bool force);
   
// set save action
extern const int kSaveActionNoSave;
extern const int kSaveActionSave;
extern const int kSaveActionAsk;
void setSaveAction(int saveAction);

// image dirty state
void markImageClean();
bool imageIsDirty();

// quit
void quit(bool saveWorkspace);

} // namespace session
} // namespace r

#endif // R_RSESSION_HPP

