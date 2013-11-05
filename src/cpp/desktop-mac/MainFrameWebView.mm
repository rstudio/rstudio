/*
 * MainFrameWebView.mm
 *
 * Copyright (C) 2009-13 by RStudio, Inc.
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

#import "MainFrameWebView.h"
#import "MainFrameController.h"

@implementation MainFrameWebView

- (id) initWithFrame:(NSRect)frame
{
    self = [super initWithFrame:frame];
    if (self) {
        // Initialization code here.
    }
    return self;
}

- (BOOL) performKeyEquivalent: (NSEvent *) theEvent
{
   if ([[theEvent charactersIgnoringModifiers] isEqualToString: @"w"]
       && ([theEvent modifierFlags] & NSDeviceIndependentModifierFlagsMask) == NSCommandKeyMask)
   {
      if ([[MainFrameController instance] isCommandEnabled: @"closeSourceDoc"])
      {
         [[MainFrameController instance] invokeCommand: @"closeSourceDoc"];
      }
      else
      {
         [[self window] performClose: self];
      }
      return YES;
   }
   return [super performKeyEquivalent: theEvent];
}

@end
