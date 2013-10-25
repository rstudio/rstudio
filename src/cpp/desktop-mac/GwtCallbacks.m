

#import <Cocoa/Cocoa.h>

#include "GwtCallbacks.h"

// https://developer.apple.com/library/mac/documentation/AppleApplications/Conceptual/SafariJSProgTopics/Tasks/ObjCFromJavaScript.html
// https://developer.apple.com/library/mac/samplecode/CallJS/Introduction/Intro.html#//apple_ref/doc/uid/DTS10004241=

@implementation GwtCallbacks

- (id)init
{
   if (self = [super init])
   {
      theValue_ = 14;
      return self;
   }
   else
   {
      return nil;
   }
}

- (void) setTheValue:(int)value
{
   theValue_ = value;
}

- (int)getTheValue
{
   return theValue_;
}

+ (NSString *) webScriptNameForSelector:(SEL)sel
{
   if (sel == @selector(setTheValue:))
      return @"setTheValue";
   
   return nil;
}

+ (BOOL)isSelectorExcludedFromWebScript:(SEL)sel
{
   return NO;
}

@end

