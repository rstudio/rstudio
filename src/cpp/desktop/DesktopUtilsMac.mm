/*
 * DesktopUtilsMac.mm
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "DesktopUtils.hpp"

#include <QWidget>

#include <core/system/Environment.hpp>

namespace desktop {

bool isRetina(QMainWindow* pMainWindow)
{
   OSWindowRef macWindow = qt_mac_window_for(pMainWindow);
   NSWindow* pWindow = (NSWindow*)macWindow;

   if ([pWindow respondsToSelector:@selector(backingScaleFactor)])
   {
      double scaleFactor = [pWindow backingScaleFactor];
      return scaleFactor == 2.0;
   }
   else
   {
      return false;
   }
}

namespace {

NSWindow* nsWindowForMainWindow(QMainWindow* pMainWindow)
{
   OSWindowRef macWindow = qt_mac_window_for(pMainWindow);
   NSWindow* pWindow = (NSWindow*)macWindow;
   return pWindow;
}

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
      NSString* lcid = [[NSLocale currentLocale] localeIdentifier];
      if (lcid)
      {
         // Eliminate trailing @ components (OS X uses the @ suffix to append
         // locale overrides like alternate currency formats)
         std::string localeId = std::string([lcid UTF8String]);
         std::size_t atLoc = localeId.find('@');
         if (atLoc != std::string::npos)
         {
            localeId = localeId.substr(0, atLoc);
            lcid = [NSString stringWithUTF8String: localeId.c_str()];
         }

         lang = [lcid stringByAppendingString:@".UTF-8"];
      }
   }

   // None of the above worked. Just hard code it.
   if (!lang)
   {
      lang = @"en_US.UTF-8";
   }

   const char* clang = [lang cStringUsingEncoding:NSASCIIStringEncoding];
   core::system::setenv("LANG", clang);
   core::system::setenv("LC_CTYPE", clang);
}

} // namespace desktop
