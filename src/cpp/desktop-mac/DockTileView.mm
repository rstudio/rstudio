/*
 * DockTileView.mm
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

#import "DockTileView.h"

@implementation DockTileView

- (void) setLabel: (NSString*) label
{
   if(label != label_)
   {
      [label retain];
      [label_ release];
      label_ = label;
   }
}

- (void) drawRect: (NSRect)rect
{
   // get the current bounds
   NSRect bounds = [self bounds];
   
   //draw the icon
   NSImage* icon = [NSImage imageNamed:@"NSApplicationIcon"];
   [icon setSize:bounds.size];
   [icon drawAtPoint:NSZeroPoint fromRect:NSZeroRect
           operation:NSCompositeCopy fraction:1.0];
   
   if (label_ != nil)
   {
      NSDictionary *attributes = [NSDictionary dictionaryWithObjectsAndKeys:
                        [NSColor blackColor], NSForegroundColorAttributeName,
                        [NSFont systemFontOfSize:16], NSFontAttributeName,
                         nil];
      
      NSMutableAttributedString *as = [[NSMutableAttributedString alloc]
                                 initWithString: label_
                                 attributes:attributes];
      
      [as drawInRect:rect];
   }
}

@end