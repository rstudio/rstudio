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
#import <Cocoa/Cocoa.h>

// Implementation based on https://github.com/jessesquires/JSCustomBadge

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
   
   // draw the icon
   NSImage* icon = [NSImage imageNamed:@"NSApplicationIcon"];
   [icon setSize:bounds.size];
   [icon drawAtPoint:NSZeroPoint fromRect:NSZeroRect
           operation:NSCompositeCopy fraction:1.0];
   
   // draw the label if needed
   if (label_ != nil)
   {
      // sizes all based on the containing bounds
      const CGFloat kBaseSize = 128;
      const CGFloat kHeightFactor = 28 / kBaseSize;
      CGFloat height = kHeightFactor * bounds.size.height;
      const CGFloat kFontSizeFactor = 16 / kBaseSize;
      CGFloat fontSize = kFontSizeFactor * bounds.size.height;
      const CGFloat kInsetFactor = 5 / kBaseSize;
      CGFloat inset = kInsetFactor * bounds.size.width;
      
      
      // get the graphics context
      NSGraphicsContext* nsGraphicsContext = [NSGraphicsContext currentContext];
      CGContextRef  context = (CGContextRef) [nsGraphicsContext graphicsPort];
      
      // draw the badge rounded rect
      NSRect badgeRect = NSMakeRect(0, 0, bounds.size.width, height);
      [self drawRoundedRectWithContext: context inRect: badgeRect];
      [self drawShineWithContext: context inRect: badgeRect];
      [self drawFrameWithContext: context inRect: badgeRect];
      
      
      NSMutableParagraphStyle *paraStyle=[[NSMutableParagraphStyle alloc] init];
      [paraStyle setAlignment:NSCenterTextAlignment];
      
      NSDictionary *attributes = [NSDictionary dictionaryWithObjectsAndKeys:
                     [NSColor whiteColor], NSForegroundColorAttributeName,
                     [NSFont systemFontOfSize: fontSize], NSFontAttributeName,
                     paraStyle, NSParagraphStyleAttributeName,
                     nil];
      
      NSMutableAttributedString *as = [[NSMutableAttributedString alloc]
                                 initWithString: label_
                                 attributes:attributes];
      
      NSRect textRect = NSMakeRect(badgeRect.origin.x + inset,
                                   badgeRect.origin.y - (.25 * fontSize),
                                   badgeRect.size.width - (2 * inset),
                                   badgeRect.size.height);
      [as drawInRect: textRect];
   }
}


#define kBadgeCornerRoundness 0.25f

- (void)drawRoundedRectWithContext:(CGContextRef)context inRect:(CGRect)rect
{
   CGContextSaveGState(context);
   
   CGFloat radius = CGRectGetMaxY(rect) * kBadgeCornerRoundness;
   CGFloat puffer = CGRectGetMaxY(rect) * 0.1f;
   CGFloat maxX = CGRectGetMaxX(rect) - puffer;
   CGFloat maxY = CGRectGetMaxY(rect) - puffer;
   CGFloat minX = CGRectGetMinX(rect) + puffer;
   CGFloat minY = CGRectGetMinY(rect) + puffer;
   
   CGContextBeginPath(context);
   NSColor* fillNsColor = [NSColor colorWithCalibratedRed: 1.0f  // iOS badge
                                                  green: 0.22f
                                                   blue: 0.22f
                                                  alpha: 1.0f];
   CGColorRef fillColor = [self CGColorFromNSColor: fillNsColor];
   CGContextSetFillColorWithColor(context, fillColor);
   CGContextAddArc(context, maxX-radius, minY+radius, radius,
                   M_PI+(M_PI/2.0f), 0.0f, 0.0f);
   CGContextAddArc(context, maxX-radius, maxY-radius, radius,
                   0.0f, M_PI/2.0f, 0.0f);
   CGContextAddArc(context, minX+radius, maxY-radius, radius,
                   M_PI/2.0f, M_PI, 0.0f);
   CGContextAddArc(context, minX+radius, minY+radius, radius,
                   M_PI, M_PI+M_PI/2.0f, 0.0f);
   
   
   // draw shadow
   NSColor* shadowNsColor = [NSColor colorWithCalibratedWhite:0.0f alpha:0.75f];
   CGColorRef shadowColor = [self CGColorFromNSColor: shadowNsColor];
   CGContextSetShadowWithColor(context,
                               CGSizeMake(0.0f, 1.0f),
                               2.0f,
                               shadowColor);
   
   
   CGContextFillPath(context);
   
   CGContextRestoreGState(context);
   
   CGColorRelease(fillColor);
   CGColorRelease(shadowColor);
}

- (void)drawShineWithContext:(CGContextRef)context inRect:(CGRect)rect
{
   CGContextSaveGState(context);
   
   CGContextTranslateCTM(context, 0.0, rect.size.height);
   CGContextScaleCTM(context, 1.0, -1.0);
   
   CGFloat radius = CGRectGetMaxY(rect) * kBadgeCornerRoundness;
   CGFloat puffer = CGRectGetMaxY(rect) * 0.1f;
   CGFloat maxX = CGRectGetMaxX(rect) - puffer;
   CGFloat maxY = CGRectGetMaxY(rect) - puffer;
   CGFloat minX = CGRectGetMinX(rect) + puffer;
   CGFloat minY = CGRectGetMinY(rect) + puffer;
   
   CGContextBeginPath(context);
   CGContextAddArc(context, maxX-radius, minY+radius, radius,
                   M_PI+(M_PI/2.0f), 0.0f, 0.0f);
   CGContextAddArc(context, maxX-radius, maxY-radius, radius,
                   0.0f, M_PI/2.0f, 0.0f);
   CGContextAddArc(context, minX+radius, maxY-radius, radius,
                   M_PI/2.0f, M_PI, 0.0f);
   CGContextAddArc(context, minX+radius, minY+radius, radius,
                   M_PI, M_PI+M_PI/2.0f, 0.0f);
   CGContextClip(context);
   
   size_t num_locations = 2.0f;
   CGFloat locations[2] = { 0.0f, 0.4f };
   CGFloat components[8] = { 0.92f, 0.92f, 0.92f, 1.0f,
                             0.82f, 0.82f, 0.82f, 0.4f };
   
   CGColorSpaceRef cspace;
   CGGradientRef gradient;
   cspace = CGColorSpaceCreateDeviceRGB();
   gradient = CGGradientCreateWithColorComponents(cspace,
                                                  components,
                                                  locations,
                                                  num_locations);
   
   CGPoint sPoint, ePoint;
   sPoint.x = 0.0f;
   sPoint.y = 0.0f;
   ePoint.x = 0.0f;
   ePoint.y = maxY;
   CGContextDrawLinearGradient (context, gradient, sPoint, ePoint, 0.0f);
   
   CGColorSpaceRelease(cspace);
   CGGradientRelease(gradient);
   
   CGContextRestoreGState(context);
}


- (void)drawFrameWithContext:(CGContextRef)context inRect:(CGRect)rect
{
   CGFloat radius = CGRectGetMaxY(rect) * kBadgeCornerRoundness;
   CGFloat puffer = CGRectGetMaxY(rect) * 0.1f;
   
   CGFloat maxX = CGRectGetMaxX(rect) - puffer;
   CGFloat maxY = CGRectGetMaxY(rect) - puffer;
   CGFloat minX = CGRectGetMinX(rect) + puffer;
   CGFloat minY = CGRectGetMinY(rect) + puffer;
   
   CGContextBeginPath(context);
   CGFloat lineSize = 1.5f;
   
   CGContextSetLineWidth(context, lineSize);
   CGColorRef strokeColor = [self CGColorFromNSColor: [NSColor whiteColor]];
   CGContextSetStrokeColorWithColor(context, strokeColor);
   CGContextAddArc(context, maxX-radius, minY+radius, radius,
                   M_PI + (M_PI/2.0f), 0.0f, 0.0f);
   CGContextAddArc(context, maxX-radius, maxY-radius, radius, 0.0f,
                   M_PI/2.0f, 0.0f);
   CGContextAddArc(context, minX+radius, maxY-radius, radius,
                   M_PI/2.0f, M_PI, 0.0f);
   CGContextAddArc(context, minX+radius, minY+radius, radius,
                   M_PI, M_PI+M_PI/2.0f, 0.0f);
   
   CGContextClosePath(context);
   CGContextStrokePath(context);
   
   CGColorRelease(strokeColor);
}


- (CGColorRef) CGColorFromNSColor: (NSColor*) color
{
   NSColor* deviceColor = [color colorUsingColorSpaceName:NSDeviceRGBColorSpace];
   CGFloat red = [deviceColor redComponent];
   CGFloat green = [deviceColor greenComponent];
   CGFloat blue = [deviceColor blueComponent];
   CGFloat alpha = [deviceColor alphaComponent];
   const CGFloat components[4] = { red, green, blue, alpha };
   CGColorSpaceRef deviceRGBColorSpace = CGColorSpaceCreateDeviceRGB();
   CGColorRef cgColor = CGColorCreate(deviceRGBColorSpace, components);
   CGColorSpaceRelease(deviceRGBColorSpace);
   return cgColor;
}






@end