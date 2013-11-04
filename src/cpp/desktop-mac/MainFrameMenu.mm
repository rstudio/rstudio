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

@implementation MainFrameMenu

- (id)init
{
   if (self = [super init])
   {
      menuStack_ = [[NSMutableArray alloc] init];
      commands_ = [[NSMutableArray alloc] init];
   }
   return self;
}

- (void) dealloc
{
   [mainMenu_ release];
   [menuStack_ release];
   [commands_ release];
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

   [menuItem setTag: [commands_ count]];
   [commands_ addObject: commandId];
   [menuItem setTarget: self];
   [menuItem setAction: @selector(invoke:)];
   
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

- (void) invoke: (id) sender {
   NSString* command = [commands_ objectAtIndex: [sender tag]];
   [[MainFrameController instance] invokeCommand: command];
}

- (BOOL) validateMenuItem: (NSMenuItem *) item {
   NSString* command = [commands_ objectAtIndex: [item tag]];

   NSString* labelJs = [NSString stringWithFormat: @"window.desktopHooks.getCommandLabel(\"%@\");", command];
   [item setTitleWithMnemonic: [[MainFrameController instance] evaluateJavaScript: labelJs]];

   NSString* checkedJs = [NSString stringWithFormat: @"window.desktopHooks.isCommandChecked(\"%@\");", command];
   if ([[[MainFrameController instance] evaluateJavaScript: checkedJs] boolValue])
      [item setState: NSOnState];
   else
      [item setState: NSOffState];

   NSString* visibleJs = [NSString stringWithFormat: @"window.desktopHooks.isCommandVisible(\"%@\");", command];
   [item setHidden: ![[[MainFrameController instance] evaluateJavaScript: visibleJs] boolValue]];

   // Suppress any unnecessary separators. This code will run once per menu item which seems more
   // effort than necessary, but there's no guarantee that I know of that validateMenuItem will be
   // called from top to bottom, and it's fast anyway.
   NSMenu* menu = [item menu];
   bool suppressSep = TRUE; // When TRUE, we don't need any more seps at this point in the menu.
   NSMenuItem* trailingSep = Nil; // If non-null when we're done looping, an extraneous trailing sep.
   for (NSMenuItem* i in [menu itemArray]) {
      if ([i isSeparatorItem]) {
         [i setHidden: suppressSep];
         if (!suppressSep) {
            trailingSep = i;
            suppressSep = TRUE;
         }
      } else if (![i isHidden]) {
         // We've encountered a non-hidden, non-sep menu entry; the next sep should be shown.
         suppressSep = FALSE;
         trailingSep = Nil;
      }
   }
   if (trailingSep != Nil)
      [trailingSep setHidden: YES];

   NSString* enabledJs = [NSString stringWithFormat: @"window.desktopHooks.isCommandEnabled(\"%@\");", command];
   if ([[[MainFrameController instance] evaluateJavaScript: enabledJs] boolValue])
      return YES;
   else
      return NO;
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

