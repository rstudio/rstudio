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
#include <core/system/Environment.hpp>
#include <core/system/ParentProcessMonitor.hpp>

#import "SessionLauncher.hpp"

#import "MainFrameController.h"

#import "Options.hpp"
#import "Utils.hpp"

#define RUN_DIAGNOSTICS_LOG(message) if (desktop::options().runDiagnostics()) \
                   std::cout << (message) << std::endl;


using namespace core;

namespace desktop {
   
namespace {
         
void launchProcess(
       std::string absPath,
       std::vector<std::string> argList,
       boost::function<void(const core::system::ProcessResult&)> onCompleted)
{
   core::system::ProcessOptions options;
   Error error = utils::processSupervisor().runProgram(absPath,
                                                       argList,
                                                       "", options,
                                                       onCompleted);
   if (error)
      LOG_ERROR(error);
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
      NSString* url = [NSString stringWithUTF8String: appUrl.c_str()];
      [[MainFrameController alloc] initWithURL: [NSURL URLWithString: url]];

      // activate the app
      [NSApp activateIgnoringOtherApps: YES];
   }
   
   return Success();
}
   
std::string SessionLauncher::launchFailedErrorMessage()
{
   return std::string();
}
   
void SessionLauncher::cleanupAtExit()
{
      
}
   
void SessionLauncher::onRSessionExited(
                              const core::system::ProcessResult& result)
{
   // write output to stdout and quit if we were running diagnostics
   if (desktop::options().runDiagnostics())
   {
      std::cout << result.stdOut << std::endl << result.stdErr << std::endl;
      [NSApp stop: nil];
      return;
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
   // always remove the abend log path before launching
   Error error = abendLogPath().removeIfExists();
   if (error)
      LOG_ERROR(error);
   
   boost::function<void(const core::system::ProcessResult&)> onCompleted =
                  boost::bind(&SessionLauncher::onRSessionExited, this, _1);
   
   
   // TODO: wait for parent termination isn't working
   
   return parent_process_monitor::wrapFork(boost::bind(launchProcess,
                                             sessionPath_.absolutePath(),
                                             args,
                                             onCompleted));
   
   // wait a bit to allow the socket to bind
   boost::this_thread::sleep(boost::posix_time::milliseconds(100));
}



} // namespace desktop

