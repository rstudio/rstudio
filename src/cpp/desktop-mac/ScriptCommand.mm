#import <Foundation/Foundation.h>
#import "ScriptCommand.h"
#import "MainFrameController.h"


@implementation evaluateRScriptCommand

-(id) performDefaultImplementation
{
   NSString *script = [self directParameter];
   if (!script || [script isEqualToString:@""])
      return [NSNumber numberWithBool:NO];

   [[MainFrameController instance] evaluateRCommand: script];
   
   return [NSNumber numberWithBool:YES];
}

@end
