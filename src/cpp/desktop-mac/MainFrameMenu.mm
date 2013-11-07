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

NSString* charToStr(unichar c) {
   return [[NSString stringWithCharacters: &c length: 1] autorelease];
}

- (id)init
{
   if (self = [super init])
   {
      menuStack_ = [[NSMutableArray alloc] init];
      commands_ = [[NSMutableArray alloc] init];
      [commands_ addObject: @""]; // Make sure index 0 is not taken

      shortcutMap_ = [[NSDictionary alloc] initWithObjectsAndKeys:
                      @"\uF700", @"Up",
                      @"\uF701", @"Down",
                      @"\uF702", @"Left",
                      @"\uF703", @"Right",
                      @"\uF704", @"F1",
                      @"\uF705", @"F2",
                      @"\uF706", @"F3",
                      @"\uF707", @"F4",
                      @"\uF708", @"F5",
                      @"\uF709", @"F6",
                      @"\uF70A", @"F7",
                      @"\uF70B", @"F8",
                      @"\uF70C", @"F9",
                      @"\uF70D", @"F10",
                      @"\uF70E", @"F11",
                      @"\uF70F", @"F12",
                      @"\uF72C", @"PageUp",
                      @"\uF72D", @"PageDown",
                      @"\n",     @"Enter",
                      @"\t",     @"Tab",
                      @"\b",     @"Backspace",
                      nil];
      
      customShortcuts_ = [[NSDictionary alloc] initWithObjectsAndKeys:
                          @"Meta+0", @"zoomActualSize",
                          @"Meta+=", @"zoomIn",
                          @"Meta+-", @"zoomOut",
                          @"Meta+Z", @"undoDummy",
                          @"Meta+Shift+Z", @"redoDummy",
                          nil];
      // Undo/Redo can't have normal keyboard shortcuts assigned--their keystrokes need to be
      // handled by WebKit itself. But we still want it to show up in the menu. So just special
      // case them.

   }
   return self;
}

- (void) dealloc
{
   [mainMenu_ release];
   [menuStack_ release];
   [commands_ release];
   [shortcutMap_ release];
   [customShortcuts_ release];
   [super dealloc];
}

- (void) beginMainMenu
{
   // create main menu
   mainMenu_ = [[NSMenu alloc] initWithTitle: @"MainMenu"];
   [self addAppMenu];
}

- (void) beginMenu: (NSString*) menuName
{
   // remove ampersand
   menuName = [menuName stringByReplacingOccurrencesOfString:@"&"
                                                  withString:@""];

   if ([menuName isEqualToString: @"Help"]) {
      [self addWindowMenu];
   }

   // create the menu item and add it to the target
   NSMenuItem* menuItem = [[NSMenuItem new] autorelease];
   [menuItem setTitleWithMnemonic: menuName];
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
   
   if ([customShortcuts_ objectForKey: commandId] != nil)
      shortcut = [customShortcuts_ objectForKey: commandId];
   
   [self assignShortcut: shortcut toMenuItem: menuItem];

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

- (void) assignShortcut: (NSString*) shortcut toMenuItem: (NSMenuItem*) menuItem {
   static NSRegularExpression* re = [[NSRegularExpression alloc] initWithPattern: @"^[a-zA-Z0-9.+\\-/`=]$"
                                                                         options: NSRegularExpressionCaseInsensitive
                                                                           error: NULL];

   if ([shortcut length] == 0)
      return;

   NSArray* parts = [shortcut componentsSeparatedByString: @"+"];
   NSUInteger modifiers = 0;
   for (NSUInteger i = 0; i < [parts count] - 1; i++) {
      NSString* mod = [parts objectAtIndex: i];
      if ([mod isEqualToString: @"Ctrl"])
         modifiers |= NSControlKeyMask;
      else if ([mod isEqualToString: @"Shift"])
         modifiers |= NSShiftKeyMask;
      else if ([mod isEqualToString: @"Alt"])
         modifiers |= NSAlternateKeyMask;
      else if ([mod isEqualToString: @"Meta"])
         modifiers |= NSCommandKeyMask;
   }
   NSString* key = [parts lastObject];
   [menuItem setKeyEquivalentModifierMask: modifiers];
   if ([re numberOfMatchesInString: key
                           options: NSMatchingAnchored
                             range: NSMakeRange(0, [key length])] > 0) {
      [menuItem setKeyEquivalent: [key lowercaseString]];
   } else {
      NSString* keyEquiv = [shortcutMap_ objectForKey: key];
      assert(keyEquiv != Nil);
      [menuItem setKeyEquivalent: keyEquiv];
   }
}

- (BOOL) validateMenuItem: (NSMenuItem *) item {
   if ([item tag] == 0) {
      return YES;
   }

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

   if ([[MainFrameController instance] isCommandEnabled: command])
      return YES;
   else
      return NO;
}

- (void) addAppMenu {
   NSMenuItem* appMenuItem = [[NSMenuItem new] autorelease];
   [mainMenu_ addItem: appMenuItem];
   [NSApp setMainMenu: mainMenu_];

   // create app menu (currently just has quit)
   NSMenu* appMenu = [[NSMenu new] autorelease];
   [appMenuItem setSubmenu: appMenu];

   // "About RStudio"
   NSMenuItem* aboutMenuItem = [[NSMenuItem alloc]
                                initWithTitle: @"About RStudio"
                                action: @selector(showAbout:)
                                keyEquivalent: @""];
   [aboutMenuItem setTarget: self];
   [appMenu addItem: aboutMenuItem];

   [appMenu addItem: [NSMenuItem separatorItem]];

   // "Preferences..."
   NSMenuItem* prefsMenuItem = [[NSMenuItem alloc]
                                initWithTitle: @"Preferences\u2026"
                                action: @selector(showPrefs:)
                                keyEquivalent: @","];
   [prefsMenuItem setTarget: self];
   [prefsMenuItem setKeyEquivalentModifierMask: NSCommandKeyMask];
   [appMenu addItem: prefsMenuItem];

   [appMenu addItem: [NSMenuItem separatorItem]];

   /*
   // "Services"
   // These don't currently appear to actually work correctly; text in the console input
   // area and in Ace aren't picked up (though console output works fine).
   NSMenuItem* servicesMenuItem = [[NSMenuItem new] autorelease];
   [servicesMenuItem setTitle: @"Services"];
   [appMenu addItem: servicesMenuItem];
   NSMenu* servicesMenu = [[NSMenu new] autorelease];
   [NSApp setServicesMenu: servicesMenu];
   [servicesMenuItem setSubmenu: servicesMenu];

   [appMenu addItem: [NSMenuItem separatorItem]];
   */

   // "Hide RStudio"
   NSMenuItem* hideMenuItem = [[[NSMenuItem alloc] initWithTitle: @"Hide RStudio"
                                                          action: @selector(hide:)
                                                   keyEquivalent: @"h"] autorelease];
   [hideMenuItem setTarget: NSApp];
   [hideMenuItem setKeyEquivalentModifierMask: NSCommandKeyMask];
   [appMenu addItem: hideMenuItem];

   // "Hide Others"
   NSMenuItem* hideOthersMenuItem = [[[NSMenuItem alloc] initWithTitle: @"Hide Others"
                                                                action: @selector(hideOtherApplications:)
                                                         keyEquivalent: @"h"] autorelease];
   [hideOthersMenuItem setTarget: NSApp];
   [hideOthersMenuItem setKeyEquivalentModifierMask: NSCommandKeyMask | NSAlternateKeyMask];
   [appMenu addItem: hideOthersMenuItem];

   // "Show All"
   NSMenuItem* showAllMenuItem = [[[NSMenuItem alloc] initWithTitle: @"Show All"
                                                             action: @selector(unhideAllApplications:)
                                                      keyEquivalent: @""] autorelease];
   [showAllMenuItem setTarget: NSApp];
   [appMenu addItem: showAllMenuItem];

   [appMenu addItem: [NSMenuItem separatorItem]];

   // "Quit RStudio"
   NSMenuItem* quitMenuItem = [[[NSMenuItem alloc]
                                initWithTitle: @"Quit RStudio"
                                action: @selector(initiateQuit)
                                keyEquivalent:@"q"] autorelease];
   [quitMenuItem setTarget: [MainFrameController instance]];
   [appMenu addItem: quitMenuItem];
}

- (void) addWindowMenu {
   NSMenuItem* windowMenuItem = [[NSMenuItem new] autorelease];
   [windowMenuItem setTitleWithMnemonic: @"Window"];
   [[self currentTargetMenu] addItem: windowMenuItem];

   NSMenu* windowMenu = [[[NSMenu alloc] initWithTitle: @"Window"] autorelease];
   [[self currentTargetMenu] setSubmenu: windowMenu forItem: windowMenuItem];

   NSMenuItem* minimize = [[NSMenuItem new] autorelease];
   [minimize setTitle: @"Minimize"];
   [minimize setTarget: self];
   [minimize setAction: @selector(minimize:)];
   [minimize setKeyEquivalent: @"m"];
   [minimize setKeyEquivalentModifierMask: NSCommandKeyMask];
   [minimize setAlternate: NO];
   [minimize setTag: 0];
   [windowMenu addItem: minimize];

   NSMenuItem* minimizeAll = [[NSMenuItem new] autorelease];
   [minimizeAll setTitle: @"Minimize All"];
   [minimizeAll setTarget: NSApp];
   [minimizeAll setAction: @selector(miniaturizeAll:)];
   [minimizeAll setKeyEquivalent: @"m"];
   [minimizeAll setKeyEquivalentModifierMask: NSCommandKeyMask | NSAlternateKeyMask];
   [minimizeAll setAlternate: YES];
   [minimizeAll setTag: 0];
   [windowMenu addItem: minimizeAll];

   NSMenuItem* zoom = [[NSMenuItem new] autorelease];
   [zoom setTitle: @"Zoom"];
   [zoom setTarget: self];
   [zoom setAction: @selector(zoom:)];
   [zoom setAlternate: NO];
   [zoom setTag: 0];
   [windowMenu addItem: zoom];

   NSMenuItem* zoomAll = [[NSMenuItem new] autorelease];
   [zoomAll setTitle: @"Zoom All"];
   [zoomAll setTarget: NSApp];
   [zoomAll setAction: @selector(zoomAll:)];
   [zoomAll setKeyEquivalentModifierMask: NSAlternateKeyMask];
   [zoomAll setAlternate: YES];
   [zoomAll setTag: 0];
   [windowMenu addItem: zoomAll];

   [windowMenu addItem: [NSMenuItem separatorItem]];

   NSMenuItem* bringAllToFront = [[NSMenuItem new] autorelease];
   [bringAllToFront setTitle: @"Bring All to Front"];
   [bringAllToFront setTarget: self];
   [bringAllToFront setAction: @selector(bringAllToFront:)];
   [bringAllToFront setKeyEquivalentModifierMask: NSAlternateKeyMask];
   [bringAllToFront setAlternate: YES];
   [bringAllToFront setTag: 0];
   [windowMenu addItem: bringAllToFront];

   [windowMenu addItem: [NSMenuItem separatorItem]];

   [NSApp setWindowsMenu: windowMenu];
}

- (void) minimize: (id) sender {
   [[NSApp keyWindow] performMiniaturize: sender];
}

- (void) zoom: (id) sender {
   [[NSApp keyWindow] performZoom: sender];
}

- (void) bringAllToFront: (id) sender {
   for (NSWindow* window in [NSApp windows]) {
      [window orderFront: self];
   }
   [[NSApp mainWindow] makeKeyAndOrderFront: self];
}

- (void) showAbout: (id) sender {
   [[MainFrameController instance] invokeCommand: @"showAboutDialog"];
}

- (void) showPrefs: (id) sender {
   [[MainFrameController instance] invokeCommand: @"showOptions"];
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
   if (sel == @selector(beginMainMenu) ||
       sel == @selector(beginMenu:) ||
       sel == @selector(addCommand:label:tooltip:shortcut:isCheckable:) ||
       sel == @selector(addSeparator) ||
       sel == @selector(endMenu) ||
       sel == @selector(endMainMenu)) {
      return NO;
   }
   else {
      return YES;
   }
}

@end

