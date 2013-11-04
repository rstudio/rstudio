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
#import <Cocoa/Cocoa.h>

#import "MainFrameController.h"

#import "MainFrameMenu.h"
#import "CommandInvoker.h"

@implementation MainFrameMenu

- (id)init
{
   if (self = [super init])
   {
      menuStack_ = [[NSMutableArray alloc] init];
   }
   return self;
}

- (void) dealloc
{
   [mainMenu_ release];
   [menuStack_ release];
   [super dealloc];
}

- (void) beginMainMenu
{
   // create main menu
   mainMenu_ = [[NSMenu alloc] initWithTitle: @"MainMenu"];
   NSMenuItem* appMenuItem = [[NSMenuItem new] autorelease];
   [mainMenu_ addItem: appMenuItem];
   [NSApp setMainMenu: mainMenu_];
   
   // create app menu (currently just has quit)
   NSMenu* appMenu = [[NSMenu new] autorelease];
   NSMenuItem* quitMenuItem = [[[NSMenuItem alloc]
                                initWithTitle: @"Quit RStudio"
                                action: @selector(initiateQuit)
                                keyEquivalent:@"q"] autorelease];
   [quitMenuItem setTarget: [MainFrameController instance]];
   [appMenu addItem: quitMenuItem];
   [appMenuItem setSubmenu: appMenu];
}

- (void) beginMenu: (NSString*) menuName
{
   // remove ampersand
   menuName = [menuName stringByReplacingOccurrencesOfString:@"&"
                                                  withString:@""];
      
   // create the menu item and add it to the target
   NSMenuItem* menuItem = [[NSMenuItem new] autorelease];
   [menuItem setTitle: menuName];
   [[self currentTargetMenu] addItem: menuItem];
   
   // create the menu and associate it with the menu item. we also
   // turn off "autoenable" so we can manage command states explicitly
   NSMenu* menu = [[[NSMenu alloc] initWithTitle: menuName] autorelease];
   [menu setAutoenablesItems: NO];
   [[self currentTargetMenu] setSubmenu: menu forItem: menuItem];
   
   // update the menu stack
   [menuStack_ addObject: menu];
}

- (void) addCommand: (NSString*) commandId
              label: (NSString*) label
            tooltip: (NSString*) tooltip
           shortcut: (NSString*) shortcut
        isCheckable: (Boolean) isCheckable
{
   // placeholder text for empty labels (can happen for MRU entries)
   if ([label length] == 0)
      label = @"Placeholder";
   
   // create menu item
   NSMenuItem* menuItem  = [[NSMenuItem new] autorelease];
   [menuItem setTitleWithMnemonic: label];
   [menuItem setToolTip: tooltip];

   id invoker = [[[CommandInvoker alloc] init: commandId] autorelease];
   [menuItem setRepresentedObject: invoker]; // for retain purposes
   [menuItem setTarget: invoker];
   [menuItem setAction: @selector(invoke)];
   
   // TODO: reflect other menu state/behavior
   
   // add it to the menu
   [[self currentTargetMenu] addItem: menuItem];
}

- (void) addSeparator
{
   [[self currentTargetMenu] addItem: [NSMenuItem separatorItem]];
}

- (void) endMenu
{
   [menuStack_ removeLastObject];
}

- (void) endMainMenu
{
   [NSApp setMainMenu: mainMenu_];
}

- (NSMenu*) currentTargetMenu
{
   if ([menuStack_ count] == 0)
      return mainMenu_;
   else
      return [menuStack_ lastObject];
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
   if (sel == @selector(currentTargetMenu))
      return YES;
   else
      return NO;
}

@end

