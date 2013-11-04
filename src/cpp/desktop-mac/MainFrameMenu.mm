/*
 * MenuCallbacks.mm
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


#import <Foundation/NSString.h>

#import "MainFrameMenu.h"

@implementation MainFrameMenu

- (id)init
{
   return [super init];
}

- (void) beginMainMenu
{
   
}

- (void) beginMenu: (NSString*) menu
{
   
}

- (void) addCommand: (NSString*) commandId
              label: (NSString*) label
            tooltip: (NSString*) tooltip
           shortcut: (NSString*) shortcut
        isCheckable: (Boolean) isCheckable
{
   
}

- (void) addSeparator
{
   
}

- (void) endMenu
{
   
}

- (void) endMainMenu
{
   
}

+ (NSString *) webScriptNameForSelector: (SEL) sel
{
   if (sel == @selector(beginMenu:))
      return @"beginMenu";
   else if (sel == @selector(addCommand:label:tooltip:shortcut:isCheckable:))
      return @"addCommand";
     
   return nil;
}

+ (BOOL)isSelectorExcludedFromWebScript: (SEL) sel
{
   return NO;
}

@end

