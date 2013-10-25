

#import <Cocoa/Cocoa.h>


@interface GwtCallbacks : NSObject {
   int theValue_;
}
- (void) setTheValue:(int)value;
- (int)getTheValue;
@end