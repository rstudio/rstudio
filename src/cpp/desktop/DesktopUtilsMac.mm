/*
 * DesktopUtilsMac.mm
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

#include "DesktopUtilsMac.hpp"

#include <boost/algorithm/string/predicate.hpp>

#include <core/StringUtils.hpp>
#include <core/system/Environment.hpp>

#import <Foundation/NSString.h>
#import <AppKit/NSFontManager.h>
#import <AppKit/NSView.h>
#import <AppKit/NSWindow.h>
#import <AppKit/NSOpenPanel.h>

#include "DockMenu.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace desktop {

QString getFixedWidthFontList()
{
   NSArray* fonts = [[NSFontManager sharedFontManager]
                         availableFontNamesWithTraits: NSFixedPitchFontMask];
   return QString::fromNSString([fonts componentsJoinedByString: @"\n"]);
}

namespace {

NSWindow* nsWindowForMainWindow(QMainWindow* pMainWindow)
{
   NSView *nsview = (NSView *) pMainWindow->winId();
   return [nsview window];
}

static DockMenu* s_pDockMenu;

void initializeSystemPrefs()
{
   NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
   [defaults setBool:NO forKey: @"NSFunctionBarAPIEnabled"];

   // Remove (disable) the "Start Dictation..." and "Emoji & Symbols" menu items from the "Edit" menu
   [defaults setBool:YES forKey:@"NSDisabledDictationMenuItem"];
   [defaults setBool:YES forKey:@"NSDisabledCharacterPaletteMenuItem"];

   // Remove the "Enter Full Screen" menu item from the "View" menu
   [defaults setBool:NO forKey:@"NSFullScreenMenuItemEverywhere"];
}

} // anonymous namespace

double devicePixelRatio(QMainWindow* pMainWindow)
{
   NSWindow* pWindow = nsWindowForMainWindow(pMainWindow);

   if ([pWindow respondsToSelector:@selector(backingScaleFactor)])
   {
      return [pWindow backingScaleFactor];
   }
   else
   {
      return 1.0;
   }
}

bool isMacOS()
{
   return true;
}

bool isCentOS()
{
   return false;
}

namespace {

bool supportsFullscreenMode(NSWindow* pWindow)
{
   return [pWindow respondsToSelector:@selector(toggleFullScreen:)];
}

} // anonymous namespace


bool supportsFullscreenMode(QMainWindow* pMainWindow)
{
   NSWindow* pWindow = nsWindowForMainWindow(pMainWindow);
   return supportsFullscreenMode(pWindow);
}

// see: https://bugreports.qt-project.org/browse/QTBUG-21607
// see: https://developer.apple.com/library/mac/#documentation/General/Conceptual/MOSXAppProgrammingGuide/FullScreenApp/FullScreenApp.html
void enableFullscreenMode(QMainWindow* pMainWindow, bool primary)
{
   NSWindow* pWindow = nsWindowForMainWindow(pMainWindow);

   if (supportsFullscreenMode(pWindow))
   {
      NSWindowCollectionBehavior behavior = [pWindow collectionBehavior];
      behavior = behavior | (primary ?
                             NSWindowCollectionBehaviorFullScreenPrimary :
                             NSWindowCollectionBehaviorFullScreenAuxiliary);
      [pWindow setCollectionBehavior:behavior];
   }
}

void toggleFullscreenMode(QMainWindow* pMainWindow)
{
   NSWindow* pWindow = nsWindowForMainWindow(pMainWindow);
   if (supportsFullscreenMode(pWindow))
      [pWindow toggleFullScreen:nil];
}

namespace {

NSString* readSystemLocale()
{
   using namespace core;
   using namespace core::system;
   Error error;

   // First, read all available locales so we can validate whether we've received
   // a valid locale.
   ProcessResult localeResult;
   error = runCommand("/usr/bin/locale -a", ProcessOptions(), &localeResult);
   if (error)
      LOG_ERROR(error);

   std::string allLocales = localeResult.stdOut;

   // Now, try looking for the active locale using NSLocale.
   std::string localeIdentifier = [[[NSLocale currentLocale] localeIdentifier] UTF8String];

   // Remove trailing @ components (macOS uses @ suffix to append locale overrides)
   auto idx = localeIdentifier.find('@');
   if (idx != std::string::npos)
      localeIdentifier = localeIdentifier.substr(0, idx);

   // Enforce a UTF-8 locale.
   localeIdentifier += ".UTF-8";

   if (allLocales.find(localeIdentifier) != std::string::npos)
      return [NSString stringWithCString: localeIdentifier.c_str()];

   // If that failed, fall back to reading the defaults value. Note that Mojave
   // (at least with 10.14) reports the wrong locale above and so we rely on this
   // as a fallback.
   ProcessResult defaultsResult;
   error = runCommand("defaults read NSGlobalDomain AppleLocale", ProcessOptions(), &defaultsResult);
   if (error)
      LOG_ERROR(error);

   std::string defaultsLocale = string_utils::trimWhitespace(defaultsResult.stdOut);

   // Remove trailing @ components (macOS uses @ suffix to append locale overrides)
   idx = defaultsLocale.find('@');
   if (idx != std::string::npos)
      defaultsLocale = defaultsLocale.substr(0, idx);

   // Enforce a UTF-8 locale.
   defaultsLocale += ".UTF-8";

   if (allLocales.find(defaultsLocale) != std::string::npos)
      return [NSString stringWithUTF8String: defaultsLocale.c_str()];

   return nullptr;
}

} // end anonymous namespace

void initializeLang()
{
   // Not sure what the memory management rules are here, i.e. whether an
   // autorelease pool is active. Just let it leak, since we're only calling
   // this once (at the time of this writing).

   // We try to simulate the behavior of R.app.

   NSString* lang = nil;

   // Highest precedence: force.LANG. If it has a value, use it.
   NSUserDefaults* defaults = [NSUserDefaults standardUserDefaults];
   [defaults addSuiteNamed:@"org.R-project.R"];
   lang = [defaults stringForKey:@"force.LANG"];
   if (lang && ![lang length])
   {
      // If force.LANG is present but empty, don't touch LANG at all.
      return;
   }
   
   // Next highest precedence: ignore.system.locale. If it has a value,
   // hardcode to en_US.UTF-8.
   if (!lang && [defaults boolForKey:@"ignore.system.locale"])
   {
      lang = @"en_US.UTF-8";
   }

   // Next highest precedence: LANG environment variable.
   if (!lang)
   {
      std::string envLang = core::system::getenv("LANG");
      if (!envLang.empty())
      {
         lang = [NSString stringWithCString:envLang.c_str()
                          encoding:NSASCIIStringEncoding];
      }
   }

   // Next highest precedence: Try to figure out language from the current
   // locale.
   if (!lang)
   {
      lang = readSystemLocale();
   }

   // None of the above worked. Just hard code it.
   if (!lang)
   {
      lang = @"en_US.UTF-8";
   }

   const char* clang = [lang cStringUsingEncoding:NSASCIIStringEncoding];
   core::system::setenv("LANG", clang);
   core::system::setenv("LC_CTYPE", clang);

   initializeSystemPrefs();
}

void finalPlatformInitialize(MainWindow* pMainWindow)
{
   // https://bugreports.qt.io/browse/QTBUG-61707
   [NSWindow setAllowsAutomaticWindowTabbing: NO];

   if (!s_pDockMenu)
   {
      s_pDockMenu = new DockMenu(pMainWindow);
   }
   else
   {
      s_pDockMenu->setMainWindow(pMainWindow);
   }
}

NSString* createAliasedPath(NSString* path)
{
   if (path == nil || [path length] == 0)
      return @"";

   std::string aliased = FilePath::createAliasedPath(
      FilePath([path UTF8String]),
      userHomePath());

   return [NSString stringWithUTF8String: aliased.c_str()];
}

NSString* resolveAliasedPath(NSString* path)
{
   if (path == nil)
      path = @"";

   FilePath resolved = FilePath::resolveAliasedPath(
      [path UTF8String],
      userHomePath());

   return [NSString stringWithUTF8String: resolved.getAbsolutePath().c_str()];
}

QString runFileDialog(NSSavePanel* panel)
{
   NSString* path = @"";
   long int result = [panel runModal];
   @try
   {
      if (result == NSOKButton)
      {
         path = [[panel URL] path];
      }
   }
   @catch (NSException* e)
   {
      throw e;
   }

   return QString::fromNSString(createAliasedPath(path));
}

QString browseDirectory(const QString& qCaption,
                        const QString& qLabel,
                        const QString& qDir,
                        QWidget* pOwner)
{
   NSString* caption = qCaption.toNSString();
   NSString* label = qLabel.toNSString();
   NSString* dir = qDir.toNSString();

   dir = resolveAliasedPath(dir);

   NSOpenPanel* panel = [NSOpenPanel openPanel];
   [panel setTitle: caption];
   [panel setPrompt: label];
   [panel setDirectoryURL: [NSURL fileURLWithPath:
                           [dir stringByStandardizingPath]]];
   [panel setCanChooseFiles: false];
   [panel setCanChooseDirectories: true];
   [panel setCanCreateDirectories: true];

   return runFileDialog(panel);
}

} // namespace desktop
} // namespace rstudio
