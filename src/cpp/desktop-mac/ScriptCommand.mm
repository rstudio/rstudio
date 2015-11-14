#import <Foundation/Foundation.h>
#import "ScriptCommand.h"
#import "MainFrameController.h"


@implementation evaluateRScriptCommand

-(id) performDefaultImplementation
{
    NSString *script = [self directParameter];
    if (!script || [script isEqualToString:@""])
        return [NSNumber numberWithBool:NO];
    script = [script stringByReplacingOccurrencesOfString:@"\\" withString:@"\\\\"];
    script = [script stringByReplacingOccurrencesOfString:@"\"" withString:@"\\\""];
    script = [script stringByReplacingOccurrencesOfString:@"\n" withString:@"\\n"];
    NSString *jscript = [NSString stringWithFormat:
      @"if (typeof evaluateRscript == 'undefined') { \
         var evaluateRscript = function(){ \
              var queue = []; \
              var running = 0; \
              var run_queue = function(){ \
                   if (queue.length > 0 && running == 0){ \
                        running = 1; \
                        evaluate(queue[0]); \
                        queue.splice(0, 1); \
                        setTimeout(function(){ \
                             running = 0; \
                             run_queue(); \
                        }, 100); \
                   } \
              }; \
              var evaluate = function(str){ \
                   var input = document.getElementById('rstudio_console_input'); \
                   var textarea = input.getElementsByTagName('textarea')[0]; \
                   textarea.value += str; \
                   var e = document.createEvent('KeyboardEvent'); \
                   e.initKeyboardEvent('input'); \
                   textarea.dispatchEvent(e); \
                   var e = document.createEvent('KeyboardEvent'); \
                   e.initKeyboardEvent('keydown'); \
                   Object.defineProperty(e, 'keyCode', {'value' : 13}); \
                   input.dispatchEvent(e); \
              }; \
              return function(str) { \
                   queue.push(str); \
                   run_queue(); \
              } \
         }(); \
      }; \
      evaluateRscript(\"%@\");", script];
   [[MainFrameController instance] evaluateJavaScript: jscript];

   return [NSNumber numberWithBool:YES];
}

@end
