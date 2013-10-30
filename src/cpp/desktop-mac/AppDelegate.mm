
#include <iostream>

#include <core/FilePath.hpp>
#include <core/system/System.hpp>
#include <core/system/Environment.hpp>

#include <core/r_util/RProjectFile.hpp>
#include <core/r_util/REnvironment.hpp>

#import <AppKit/AppKit.h>

#import <Foundation/NSTask.h>

#import "AppDelegate.h"
#import "Options.hpp"
#import "SessionLauncher.hpp"
#import "Utils.hpp"
#import "MainFrameController.h"

using namespace core;
using namespace desktop;

NSString* executablePath()
{
   FilePath exePath;
   Error error = core::system::executablePath(NULL, &exePath);
   if (error)
      LOG_ERROR(error);
   return [NSString stringWithUTF8String: exePath.absolutePath().c_str()];
}

BOOL isProjectFilename(NSString* filename)
{
   if (!filename)
      return NO;
   
   FilePath filePath([filename UTF8String]);
   return filePath.exists() && filePath.extensionLowerCase() == ".rproj";
}

NSString* openFileCommandLineArgument()
{
   NSArray *arguments = [[NSProcessInfo processInfo] arguments];
   int count = [arguments count];
   if (count > 1) // executable name doesn't count as an argument
   {
      for (int i=(count-1); i>0; --i)
      {
         NSString* arg = [arguments objectAtIndex: i];
         if (![arg hasPrefix: @"-psn"] && // avoid process serial number arg
             ![arg isEqualToString: @"--run-diagnostics"])
         {
            return arg;
         }
      }
   }
   
   return nil;
}

// PORT: From DesktopMain.cpp
NSString* verifyAndNormalizeFilename(NSString* filename)
{
   if (filename)
   {
      // resolve relative path
      std::string path([filename UTF8String]);
      if (!FilePath::isRootPath(path))
      {
         path = FilePath::safeCurrentPath(
                           FilePath("/")).childPath(path).absolutePath();
      }
      
      if (FilePath(path).exists())
         return [NSString stringWithUTF8String: path.c_str()];
      else
         return nil;
   }
   else
   {
      return nil;
   }
}

// PORT: from DesktopMain.cpp
std::string s_sharedSecret;

void initializeSharedSecret()
{
   s_sharedSecret = core::system::generateUuid();
   core::system::setenv("RS_SHARED_SECRET", s_sharedSecret);
}


// PORT: from DesktopMain.cpp
void initializeWorkingDirectory(const std::string& filename)
{
   // calculate what our initial working directory should be
   std::string workingDir;
   
   // if there is a filename passed to us then use it's path
   if (!filename.empty())
   {
      FilePath filePath(filename);
      if (filePath.exists())
      {
         if (filePath.isDirectory())
            workingDir = filePath.absolutePath();
         else
            workingDir = filePath.parent().absolutePath();
      }
   }
   
   // do additinal detection if necessary
   if (workingDir.empty())
   {
      // get current path
      FilePath currentPath = FilePath::safeCurrentPath(
                                                core::system::userHomePath());
      
      // detect whether we were launched from the system application menu
      // (e.g. Dock, Program File icon, etc.). we do this by checking
      // whether the executable path is within the current path. if we
      // weren't launched from the system app menu that set the initial
      // wd to the current path
      NSString* exePathStr = executablePath();
      FilePath exePath([exePathStr UTF8String]);
      if (!exePath.isWithin(currentPath))
         workingDir = currentPath.absolutePath();
   }
   
   // set the working dir if we have one
   if (!workingDir.empty())
      core::system::setenv("RS_INITIAL_WD", workingDir);
}

// PORT: from DesktopMain.cpp
void setInitialProject(const FilePath& projectFile, std::string* pFilename)
{
   core::system::setenv("RS_INITIAL_PROJECT", projectFile.absolutePath());
   pFilename->clear();
}

// PORT: from DesktopMain.cpp
void initializeStartupEnvironment(std::string* pFilename)
{
   // if the filename ends with .RData or .rda then this is an
   // environment file. if it ends with .Rproj then it is
   // a project file. we handle both cases by setting an environment
   // var and then resetting the pFilename so it isn't processed
   // using the standard open file logic
   FilePath filePath(*pFilename);
   if (filePath.exists())
   {
      std::string ext = filePath.extensionLowerCase();
      
      // if it is a directory or just an .rdata file then we can see
      // whether there is a project file we can automatically attach to
      if (filePath.isDirectory())
      {
         FilePath projectFile = r_util::projectFromDirectory(filePath);
         if (!projectFile.empty())
         {
            setInitialProject(projectFile, pFilename);
         }
      }
      else if (ext == ".rproj")
      {
         setInitialProject(filePath, pFilename);
      }
      else if (ext == ".rdata" || ext == ".rda")
      {
         core::system::setenv("RS_INITIAL_ENV", filePath.absolutePath());
         pFilename->clear();
      }
      
   }
}

// PORT: from DesktopPosixDetectRHome
bool prepareEnvironment(Options& options)
{
   // check for which R override
   FilePath rWhichRPath;
   std::string whichROverride = core::system::getenv("RSTUDIO_WHICH_R");
   if (!whichROverride.empty())
      rWhichRPath = FilePath(whichROverride);
   
   // determine rLdPaths script location
   FilePath supportingFilePath = options.supportingFilePath();
   FilePath rLdScriptPath = supportingFilePath.complete("bin/r-ldpath");
   if (!rLdScriptPath.exists())
      rLdScriptPath = supportingFilePath.complete("session/r-ldpath");
   
   // attempt to detect R environment
   std::string rScriptPath, errMsg;
   r_util::EnvironmentVars rEnvVars;
   bool success = r_util::detectREnvironment(rWhichRPath,
                                             rLdScriptPath,
                                             std::string(),
                                             &rScriptPath,
                                             &rEnvVars,
                                             &errMsg);
   if (!success)
   {
      [NSApp activateIgnoringOtherApps: YES];
      utils::showMessageBox(NSCriticalAlertStyle,
                            @"R Not Found",
                            [NSString stringWithUTF8String: errMsg.c_str()]);
      return false;
   }
   
   if (desktop::options().runDiagnostics())
   {
      std::cout << std::endl << "Using R script: " << rScriptPath
      << std::endl;
   }
   
   // set environment and return true
   r_util::setREnvironmentVars(rEnvVars);
   return true;
}



@implementation AppDelegate

- (void)dealloc
{
   [openFile_ release];
   [super dealloc];
}

- (BOOL) application: (NSApplication *) theApplication
            openFile:(NSString *) filename
{
   // open file and application together
   if (!openFile_)
   {
      openFile_ = [filename copy];
   }
   // attemping to open a project in an existing instance, force a new instance
   else if (isProjectFilename(filename))
   {
      NSArray* args = [NSArray arrayWithObject: filename];
      [NSTask launchedTaskWithLaunchPath: executablePath()
                               arguments: args];
      
   }
   // attempt to open a file in an existing instance
   else
   {
      // TODO: open file in existing instance
      
      
      desktop::utils::showMessageBox(
                  NSInformationalAlertStyle,
                  @"RStudio",
                  [@"Open Existing: " stringByAppendingString: filename]);
     
   }
   
   return YES;   
}

- (void) applicationDidFinishLaunching: (NSNotification *) aNotification
{
   // check for open file request (either apple event or command line)
   NSString* openFile = verifyAndNormalizeFilename(openFile_);
   if (!openFile)
      openFile = verifyAndNormalizeFilename(openFileCommandLineArgument());
   std::string filename;
   if (openFile)
      filename = [openFile UTF8String];
   
   // intialize options
   NSArray* arguments = [[NSProcessInfo processInfo] arguments];
   desktop::options().initFromCommandLine(arguments);
   
   // reset log if we are in run-diagnostics mode
   if (desktop::options().runDiagnostics())
      initializeStderrLog("rdesktop", core::system::kLogLevelWarning);
   
   // initialize startup environment
   initializeSharedSecret();
   initializeWorkingDirectory(filename);
   initializeStartupEnvironment(&filename);
   desktop::Options& options = desktop::options();
   if (!prepareEnvironment(options))
   {
      [NSApp terminate: self];
      return;
   }
       
   // get install path
   FilePath installPath;
   Error error = core::system::installPath("..", NULL, &installPath);
   if (error)
   {
      LOG_ERROR(error);
      [NSApp terminate: self];
      return;
   }
   
   // calculate paths to config file, rsession, and desktop scripts
   FilePath confPath, sessionPath, scriptsPath;
   
   // check for debug configuration
#ifndef NDEBUG
   FilePath currentPath = FilePath::safeCurrentPath(installPath);
   if (currentPath.complete("conf/rdesktop-dev.conf").exists())
   {
      confPath = currentPath.complete("conf/rdesktop-dev.conf");
      sessionPath = currentPath.complete("session/Debug/rsession");
      scriptsPath = currentPath.complete("desktop-mac");
   }
#endif
   
   // if there is no conf path then release mode
   if (confPath.empty())
   {
      // default paths (then tweak)
      sessionPath = installPath.complete("bin/rsession");
      scriptsPath = installPath.complete("bin");
      
      // check for running in a bundle
      if (installPath.complete("Info.plist").exists())
      {
         sessionPath = installPath.complete("MacOS/rsession");
         scriptsPath = installPath.complete("MacOS");
      }
   }
   
   // set the scripts path in options
   desktop::options().setScriptsPath(scriptsPath);
   
   // initailize the session launcher and launch the first session
   sessionLauncher().init(sessionPath, confPath);
   error = sessionLauncher().launchFirstSession(filename);
   if (error)
   {
      LOG_ERROR(error);
      
      std::string msg = sessionLauncher().launchFailedErrorMessage();
      
      [NSApp activateIgnoringOtherApps: YES];
      utils::showMessageBox(NSCriticalAlertStyle,
                            @"RStudio",
                            [NSString stringWithUTF8String: msg.c_str()]);
      [NSApp terminate: self];
   }
}

- (void) applicationWillTerminate: (NSNotification *) notification
{
   sessionLauncher().cleanupAtExit();
}

- (BOOL) canBecomeKeyWindow
{
    return YES;
}

- (BOOL) applicationShouldTerminateAfterLastWindowClosed: (NSApplication*) s
{
   return YES;
}

@end
