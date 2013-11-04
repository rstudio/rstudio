/*
 * MainFrameMenu.h
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


#import <Foundation/NSObject.h>

@interface MainFrameMenu : NSObject {
   NSMenu* mainMenu_;
   NSMutableArray* menuStack_;

   // Stores a list of the commands that were added as menu items. Each NSMenuItem has
   // a "tag" property that is an NSInt, and the value of the tag is the index into
   // this array. In other words, to get the tag for a menu item you would do
   // [commands_ objectAtIndex: [item tag]]
   NSMutableArray* commands_;
}

- (BOOL) validateMenuItem: (NSMenuItem *) item;

@end