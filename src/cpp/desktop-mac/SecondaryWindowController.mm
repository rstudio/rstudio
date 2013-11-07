/*
 * SecondaryWindowController.mm
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

#import "SecondaryWindowController.h"

#define kBackCommand    @"Back"
#define kForwardCommand @"Foward"
#define kRefreshCommand @"Refresh"
#define kPrintCommand   @"Print"

@implementation SecondaryWindowController


- (id)initWithURLRequest: (NSURLRequest*) request
                    name: (NSString*) name
{
   if (self = [super initWithURLRequest: request name: name])
   {
      toolbarItems_ = [[NSArray alloc] initWithObjects: kBackCommand,
                                                        kForwardCommand,
                                                        kRefreshCommand,
                                                        kPrintCommand,
                                                        nil];
      
      NSToolbar *toolbar = [[[NSToolbar alloc]
                  initWithIdentifier:@"PreferencesToolbar"] autorelease];
      [toolbar setSizeMode: NSToolbarSizeModeSmall];
      [toolbar setDisplayMode: NSToolbarDisplayModeIconOnly];
      [toolbar setAllowsUserCustomization: NO];
      [toolbar setAutosavesConfiguration: NO];
      
      [toolbar setDelegate: self];
      
      [[self window] setToolbar:toolbar];
   }
   return self;
}

- (void) dealloc
{
   [toolbarItems_ release];
   [super dealloc];
}

- (NSToolbarItem *)toolbar: (NSToolbar *) toolbar
     itemForItemIdentifier: (NSString *) identifier
 willBeInsertedIntoToolbar: (BOOL) flag
{
   NSToolbarItem *toolbarItem =
     [[[NSToolbarItem alloc] initWithItemIdentifier: identifier] autorelease];
   
 
   if ([identifier isEqual: kBackCommand])
   {
      [toolbarItem setLabel: kBackCommand];
      [toolbarItem setImage: [NSImage imageNamed: @"back_mac"]];
      [toolbarItem setTarget: [self webView]];
      [toolbarItem setAction: @selector(goBack:)];
   }
   else if ([identifier isEqual: kForwardCommand])
   {
      [toolbarItem setLabel: kForwardCommand];
      [toolbarItem setImage: [NSImage imageNamed: @"forward_mac"]];
      [toolbarItem setTarget: [self webView]];
      [toolbarItem setAction: @selector(goForward:)];
   }
   else if ([identifier isEqual: kRefreshCommand])
   {
      [toolbarItem setLabel: kRefreshCommand];
      [toolbarItem setImage: [NSImage imageNamed: @"reload_mac"]];
      [toolbarItem setTarget: [self webView]];
      [toolbarItem setAction: @selector(reload:)];
   }
   else if ([identifier isEqual: kPrintCommand])
   {
      [toolbarItem setLabel: kPrintCommand];
      [toolbarItem setImage: [NSImage imageNamed: @"print_mac"]];
      [toolbarItem setTarget: self];
      [toolbarItem setAction: @selector(printMainFrame)];
   }
   
   return toolbarItem;
}

- (void) printMainFrame
{
   [self printFrameView: self.webView.mainFrame.frameView];
}

- (NSArray*) toolbarAllowedItemIdentifiers: (NSToolbar*) toolbar
{
   return toolbarItems_;
}

-(NSArray *) toolbarDefaultItemIdentifiers:(NSToolbar*) toolbar
{
   return toolbarItems_;
}


@end
