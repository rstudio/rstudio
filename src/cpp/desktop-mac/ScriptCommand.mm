#import <Foundation/Foundation.h>
#import "ScriptCommand.h"
#import "MainFrameController.h"


@implementation doRscriptCommand

-(id) performDefaultImplementation
{
    NSString *script = [self directParameter];
    if (!script || [script isEqualToString:@""])
        return [NSNumber numberWithBool:NO];
    script = [script stringByReplacingOccurrencesOfString:@"\\" withString:@"\\\\"];
    script = [script stringByReplacingOccurrencesOfString:@"\"" withString:@"\\\""];
    script = [script stringByReplacingOccurrencesOfString:@"\n" withString:@"\\n"];
    NSString *jscript = [NSString stringWithFormat:
      @"var input = document.getElementById('rstudio_console_input'); \
      var textarea = input.getElementsByTagName('textarea')[0]; \
      textarea.value += \"%@\"; \
      var e = document.createEvent('KeyboardEvent'); \
      e.initKeyboardEvent('input'); \
      textarea.dispatchEvent(e); \
      var e = document.createEvent('KeyboardEvent'); \
      e.initKeyboardEvent('keydown'); \
      Object.defineProperty(e, 'keyCode', {'value' : 13}); \
      input.dispatchEvent(e);", script];
   [[MainFrameController instance] evaluateJavaScript: jscript];

   return [NSNumber numberWithBool:YES];
}

@end
