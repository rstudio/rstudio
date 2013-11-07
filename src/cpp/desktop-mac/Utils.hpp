
/*
 * Utils.hpp
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

#ifndef DESKTOP_UTILS_HPP
#define DESKTOP_UTILS_HPP

#include <core/system/Process.hpp>

#import <AppKit/NSAlert.h>

namespace core {
   class FilePath;
}


namespace desktop {
namespace utils {

void initializeLang();

core::FilePath userLogPath();
   
void showMessageBox(NSAlertStyle style, NSString* title, NSString* message);
   
void browseURL(NSURL* url);
   
core::system::ProcessSupervisor& processSupervisor();
   
bool supportsFullscreenMode(NSWindow* window);
void enableFullscreenMode(NSWindow* window, bool primary);
void toggleFullscreenMode(NSWindow* window);

float titleBarHeight();
   
NSData *base64Decode(NSString *input);
   
} // namespace utils
} // namespace desktop

#endif // DESKTOP_UTILS_HPP