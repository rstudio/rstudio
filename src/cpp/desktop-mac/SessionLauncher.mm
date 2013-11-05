/*
 * SessionLauncher.mm
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


#include <iostream>

#include <boost/bind.hpp>


#include <boost/thread.hpp>
#include <core/FileSerializer.hpp>
#include <core/system/Environment.hpp>

#import "SessionLauncher.hpp"

#import "MainFrameController.h"

#import "Options.hpp"
#import "Utils.hpp"

#define RUN_DIAGNOSTICS_LOG(message) if (desktop::options().runDiagnostics()) \
                   std::cout << (message) << std::endl;


using namespace core;

namespace desktop {
   
namespace {
         
Error launchProcess(
       std::string absPath,
       std::vector<std::string> argList,
       boost::function<void(const core::system::ProcessResult&)> onCompleted)
{
   core::system::ProcessOptions options;
   return utils::processSupervisor().runProgram(absPath,
                                                argList,
                                                "",
                                                options,
                                                onCompleted);
}

FilePath abendLogPath()
{
   return desktop::utils::userLogPath().complete("rsession_abort_msg.log");
}

void logEnvVar(const std::string& name)
{
   std::string value = core::system::getenv(name);
   if (!value.empty())
      RUN_DIAGNOSTICS_LOG("  " + name + "=" + value);
}
   
   
} // anonymous namespace
   
SessionLauncher& sessionLauncher()
{
   static SessionLauncher instance;
   return instance;
}
   
void SessionLauncher::init(const core::FilePath& sessionPath,
                           const core::FilePath& confPath)
{
   sessionPath_ = sessionPath;
   confPath_ = confPath;
}
   
Error SessionLauncher::launchFirstSession(const std::string& filename)
{
   // build a new new launch context
   std::string host, port, appUrl;
   std::vector<std::string> argList;
   buildLaunchContext(&host, &port, &argList, &appUrl);
   
   RUN_DIAGNOSTICS_LOG("\nAttempting to launch R session...");
   logEnvVar("RSTUDIO_WHICH_R");
   logEnvVar("R_HOME");
   logEnvVar("R_DOC_DIR");
   logEnvVar("R_INCLUDE_DIR");
   logEnvVar("R_SHARE_DIR");
   logEnvVar("R_LIBS");
   logEnvVar("R_LIBS_USER");
   logEnvVar("DYLD_LIBRARY_PATH");
   logEnvVar("LD_LIBRARY_PATH");
   logEnvVar("PATH");
   logEnvVar("HOME");
   logEnvVar("R_USER");

   // launch the session
   Error error = launchSession(argList);
   if (error)
      return error;


   // load the main window if we aren't running diagnostics
   if (!desktop::options().runDiagnostics())
   {
      NSString* openFile = [NSString stringWithUTF8String: filename.c_str()];
      NSString* url = [NSString stringWithUTF8String: appUrl.c_str()];
      [[MainFrameController alloc] initWithURL: [NSURL URLWithString: url]
                                      openFile: openFile];

      // activate the app
      [NSApp activateIgnoringOtherApps: YES];
   }
   
   return Success();
}
   
void SessionLauncher::launchNextSession(bool reload)
{   
   // build a new launch context -- re-use the same port if we aren't reloading
   std::string port = !reload ? options().portNumber() : "";
   std::string host, url;
   std::vector<std::string> argList;
   buildLaunchContext(&host, &port, &argList, &url);
   
   // launch the process
   Error error = launchSession(argList);
   if (!error)
   {
      // reload if necessary
      if (reload)
      {
         NSURL* nsurl = [NSURL URLWithString:
                           [NSString stringWithUTF8String: url.c_str()]];
         [[MainFrameController instance] loadURL: nsurl];
      }
   }
   else
   {
      LOG_ERROR(error);

      std::string errMsg = launchFailedErrorMessage();
      utils::showMessageBox(NSCriticalAlertStyle,
                            @"RStudio",
                            [NSString stringWithUTF8String: errMsg.c_str()]);

      [[MainFrameController instance] quit];
   }
}

   
std::string SessionLauncher::launchFailedErrorMessage()
{
   // check for abend log -- bail if there is none
   std::string abendLog = collectAbendLogMessage();
   if (abendLog.empty())
      return std::string();
   
   // build message
   std::string errMsg = "The R session had a fatal error.";
   
   // check for R version mismatch
   if (abendLog.find("arguments passed to .Internal") != std::string::npos)
   {
      errMsg.append("\n\nThis error was very likely caused "
                    "by R attempting to load packages from a different "
                    "incompatible version of R on your system. Please remove "
                    "other versions of R and/or remove environment variables "
                    "that reference libraries from other versions of R "
                    "before proceeding.");
   }
   
   errMsg.append("\n\n" + abendLog);
   
   // check for stderr
   if (!sessionStderr_.empty())
       errMsg.append("\n\n" + sessionStderr_);
       
   return errMsg;
}
 

void SessionLauncher::cleanupAtExit()
{
   // currently does nothing
}
   
void SessionLauncher::setPendingQuit(PendingQuit pendingQuit)
{
   pendingQuit_= pendingQuit;
}
   
PendingQuit SessionLauncher::collectPendingQuit()
{
   if (pendingQuit_ != PendingQuitNone)
   {
      PendingQuit pendingQuit = pendingQuit_;
      pendingQuit_ = PendingQuitNone;
      return pendingQuit;
   }
   else
   {
      return PendingQuitNone;
   }
}
   
void SessionLauncher::onRSessionExited(
                              const core::system::ProcessResult& result)
{
   // set flag indicating a process is no longer active
   sessionProcessActive_ = false;
   
   // write output to stdout and quit if we were running diagnostics
   if (desktop::options().runDiagnostics())
   {
      std::cout << result.stdOut << std::endl << result.stdErr << std::endl;
      [NSApp terminate: nil];
      return;
   }
   
   // collect stderr
   sessionStderr_ = result.stdErr;
   
   // get pending quit status
   int pendingQuit = collectPendingQuit();
   
   // if there was no pending quit set then this is a crash
   if (pendingQuit == PendingQuitNone)
   {
      closeAllWindows();
      
      [[MainFrameController instance]
         evaluateJavaScript: @"window.desktopHooks.notifyRCrashed()"];
      
      if (abendLogPath().exists())
      {
         std::string errMsg = collectAbendLogMessage();
         utils::showMessageBox(NSCriticalAlertStyle,
                               @"RStudio",
                               [NSString stringWithUTF8String: errMsg.c_str()]);
      }

   }
   
   // quit and exit means close the main window
   else if (pendingQuit == PendingQuitAndExit)
   {
      [[MainFrameController instance] quit];
   }
   
   // otherwise this is a restart so we need to launch the next session
   else
   {
      // close all satellite windows if we are reloading
      bool reload = (pendingQuit == PendingQuitRestartAndReload);
      if (reload)
         closeAllWindows();
      
      // launch next session
      launchNextSession(reload);
   }
}
   
void SessionLauncher::buildLaunchContext(std::string* pHost,
                                         std::string* pPort,
                                         std::vector<std::string>* pArgList,
                                         std::string* pUrl) const
{
   *pHost = "127.0.0.1";
   if (pPort->empty())
      *pPort = desktop::options().newPortNumber();
   *pUrl = "http://" + *pHost + ":" + *pPort + "/";
           
   
   pArgList->push_back("--config-file");
   if (!confPath_.empty())
   {
      pArgList->push_back(confPath_.absolutePath());
   }
   else
   {
      // explicitly pass "none" so that rsession doesn't read an
      // /etc/rstudio/rsession.conf file which may be sitting around
      // from a previous configuratin or install
      pArgList->push_back("none");
   }
   
   pArgList->push_back("--program-mode");
   pArgList->push_back("desktop");

   pArgList->push_back("--www-port");
   pArgList->push_back(*pPort);
   
   if (desktop::options().runDiagnostics())
   {
      pArgList->push_back("--verify-installation");
      pArgList->push_back("1");
   }
}
   
Error SessionLauncher::launchSession(std::vector<std::string> args)
{
   // always reset the abend log path and stderr before launching
   Error error = abendLogPath().removeIfExists();
   if (error)
      LOG_ERROR(error);
   sessionStderr_.clear();
   
   boost::function<void(const core::system::ProcessResult&)> onCompleted =
                  boost::bind(&SessionLauncher::onRSessionExited, this, _1);
   
   error = launchProcess(sessionPath_.absolutePath(), args, onCompleted);
   if (error)
      return error;
   
   // session process is active
   sessionProcessActive_ = true;
   
   // wait a bit to allow the socket to bind
   boost::this_thread::sleep(boost::posix_time::milliseconds(100));
   
   // success!
   return Success();
}

   
std::string SessionLauncher::collectAbendLogMessage()
{
   std::string contents;
   FilePath abendLog = abendLogPath();
   if (abendLog.exists())
   {
      Error error = core::readStringFromFile(abendLog, &contents);
      if (error)
         LOG_ERROR(error);
      
      error = abendLog.removeIfExists();
      if (error)
         LOG_ERROR(error);
   }
   
   return contents;
}
   
void SessionLauncher::closeAllWindows()
{
   NSWindow* mainWindow = [[MainFrameController instance] window];
   NSArray* windows = [NSApp windows];
   for (NSWindow* window in windows)
   {
      if (window != mainWindow)
         [window close];
   }
}



} // namespace desktop

