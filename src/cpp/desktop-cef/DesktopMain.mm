
#import <Cocoa/Cocoa.h>

int main(int argc, char* argv[])
{
   NSAlert *alert = [[[NSAlert alloc] init] autorelease];
   [alert setMessageText:@"Hello, World!"];
   [alert runModal];

   return 0;

}
