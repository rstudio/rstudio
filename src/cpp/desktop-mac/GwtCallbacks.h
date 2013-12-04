

#import <Foundation/NSObject.h>

#import "AppDelegate.h"

// An enumeration of message types used by the client (passed to showMessageBox)
enum MessageType
{
   MSG_POPUP_BLOCKED = 0,
   MSG_INFO = 1,
   MSG_WARNING = 2,
   MSG_ERROR = 3,
   MSG_QUESTION = 4
};

@protocol GwtCallbacksUIDelegate
-(NSWindow*) uiWindow;
@end

@interface GwtCallbacks : NSObject {
   id<GwtCallbacksUIDelegate> uiDelegate_;
}

// designated initializer
- (id) initWithUIDelegate: (id<GwtCallbacksUIDelegate>) uiDelegate;


@end