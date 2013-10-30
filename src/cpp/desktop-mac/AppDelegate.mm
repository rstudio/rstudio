

#include <core/FilePath.hpp>
#include <core/system/System.hpp>
#include <core/system/Environment.hpp>

#include <core/r_util/RProjectFile.hpp>

#import <AppKit/AppKit.h>

#import <Foundation/NSTask.h>

#import "AppDelegate.h"
#import "Options.hpp"
#import "Utils.hpp"
#import "WebViewController.h"

using namespace core;

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
      
      NSAlert *alert = [[[NSAlert alloc] init] autorelease];
      [alert setMessageText: [@"Open Existing:" stringByAppendingString: filename]];
      [alert runModal];
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
   
   //
   // TODO - more option proessing
   //
   //
   
   // initialize startup environment
   initializeSharedSecret();
   initializeWorkingDirectory(filename);
   initializeStartupEnvironment(&filename);
   
  
   
   
   
   // create menubar
   id menubar = [[NSMenu new] autorelease];
   id appMenuItem = [[NSMenuItem new] autorelease];
   [menubar addItem: appMenuItem];
   [NSApp setMainMenu: menubar];
   id appMenu = [[NSMenu new] autorelease];
   id appName = [[NSProcessInfo processInfo] processName];
   id quitTitle = [@"Quit " stringByAppendingString:appName];
   id quitMenuItem = [[[NSMenuItem alloc] initWithTitle:quitTitle
                                                 action:@selector(terminate:)
                                          keyEquivalent:@"q"] autorelease];
   [appMenu addItem: quitMenuItem];
   [appMenuItem setSubmenu: appMenu];
   
   
   // load the main window
   NSString *urlAddress = @"http://localhost:8787";
   NSURL *url = [NSURL URLWithString: urlAddress];
   NSURLRequest *request = [NSURLRequest requestWithURL: url];
   WebViewController * windowController =
   [[WebViewController alloc] initWithURLRequest: request];
   
   // remember size and position accross sessions
   [windowController setWindowFrameAutosaveName: @"RStudio"];
   
   // activate the app
   [NSApp activateIgnoringOtherApps: YES];
}

- (void) applicationWillTerminate: (NSNotification *) notification
{

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
