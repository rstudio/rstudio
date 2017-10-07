/*
 * GwtCallbacks.h
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

#import <Foundation/NSObject.h>

#import "AppDelegate.h"

// An enumeration of message types used by the client (passed to showMessageBox)
enum MessageType
{
   MSG_POPUP_BLOCKED = 0,
   MSG_INFO = 1,
   MSG_WARNING = 2,
   MSG_ERROR = 3,
   MSG_QUESTION = 4
};

@protocol GwtCallbacksUIDelegate
-(NSWindow*) uiWindow;
@end

@interface GwtCallbacks : NSObject {
   id<GwtCallbacksUIDelegate> uiDelegate_;
   id<NSObject> busyActivity_;
}

// designated initializer
- (id) initWithUIDelegate: (id<GwtCallbacksUIDelegate>) uiDelegate;

- (void) openProjectInOverlaidNewWindow: (NSString*) projectFilePath;

@end
