
#ifdef __clang__
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wdeprecated-declarations"
#endif

#include <iostream>

#include <boost/algorithm/string/predicate.hpp>

#include <core/FilePath.hpp>
#include <core/system/System.hpp>
#include <core/system/Environment.hpp>

#include <core/r_util/RUserData.hpp>

#import "GwtCallbacks.h"
#import "Options.hpp"

#import <Foundation/NSString.h>
#import <AppKit/NSBitmapImageRep.h>

#include "SessionLauncher.hpp"
#include "Utils.hpp"

#import "MainFrameController.h"

#define kMinimalSuffix @"_minimal"

using namespace rstudio;
using namespace rstudio::core;
using namespace rstudio::desktop;

namespace {
   
FilePath userHomePath()
{
   return core::system::userHomePath("R_USER|HOME");
}
   
NSString* createAliasedPath(NSString* path)
{
   if (path == nil || [path length] == 0)
      return @"";
   
   std::string aliased = FilePath::createAliasedPath(
                  FilePath([path UTF8String]), userHomePath());
   
   return [NSString stringWithUTF8String: aliased.c_str()];
}

NSString* getNewWindowGeometry()
{
   NSRect frame = [[NSApp mainWindow] frame];
   frame.origin = [[NSApp mainWindow] cascadeTopLeftFromPoint: frame.origin];
   return [NSString stringWithFormat: @"%d,%d,%d,%d", (int)frame.origin.x,
           (int)frame.origin.y, (int)frame.size.height, (int)frame.size.width];
}

NSString* resolveAliasedPath(NSString* path)
{
   if (path == nil)
      path = @"";
   
   FilePath resolved = FilePath::resolveAliasedPath([path UTF8String],
                                                    userHomePath());
   return [NSString stringWithUTF8String: resolved.absolutePath().c_str()];
}
   
class CFAutoRelease : boost::noncopyable
{
public:
   explicit CFAutoRelease(CFTypeRef ref) : ref_(ref) {}
   ~CFAutoRelease() { CFRelease(ref_); }
private:
   CFTypeRef ref_;
};

} // anonymous namespace

@implementation GwtCallbacks

- (id) initWithUIDelegate: (id<GwtCallbacksUIDelegate>) uiDelegate
{
   if (self = [super init])
   {
      uiDelegate_ = uiDelegate;
      busyActivity_ = nil;
   }
   return self;
}

// sentinel function for gwt deferred binding
- (Boolean) isCocoa
{
   return true;
}

- (NSString*) proportionalFont
{
   return [NSString stringWithUTF8String: options().proportionalFont().c_str()];
}

- (NSString*) fixedWidthFont
{
   return [NSString stringWithUTF8String: options().fixedWidthFont().c_str()];
}

- (void) browseUrl: (NSString*) url
{
   NSURL* nsurl = [NSURL URLWithString: url];
   desktop::utils::browseURL(nsurl);
}

- (NSString*) runSheetFileDialog: (NSSavePanel*) panel
{
   NSString* path = @"";
   [panel beginSheetModalForWindow: [uiDelegate_ uiWindow]
                 completionHandler: ^(NSInteger result) {}];
   long int result = [panel runModal];
   @try
   {
      if (result == NSOKButton)
      {
         path = [[panel URL] path];
      }
   }
   @catch (NSException* e)
   {
      throw e;
   }
   @finally
   {
      [NSApp endSheet:panel];
   }
   return createAliasedPath(path);
}

- (NSString*) runFileDialog: (NSSavePanel*) panel
{
    NSString* path = @"";
    long int result = [panel runModal];
    @try
    {
        if (result == NSOKButton)
        {
            path = [[panel URL] path];
        }
    }
    @catch (NSException* e)
    {
        throw e;
    }
    return createAliasedPath(path);
}


- (NSString*) getOpenFileName: (NSString*) caption
                          dir: (NSString*) dir
                       filter: (NSString*) filter
         canChooseDirectories: (Boolean) canChooseDirectories
{
   dir = resolveAliasedPath(dir);
   
   NSOpenPanel *open = [NSOpenPanel openPanel];
   [open setTitle: caption];
   [open setDirectoryURL: [NSURL fileURLWithPath:
                           [dir stringByStandardizingPath]]];
   [open setCanChooseDirectories: canChooseDirectories];
   
   // If the filter was specified and looks like a filter string
   // (i.e. "R Projects (*.RProj)"), extract just the extension ("RProj") to
   // pass to the open dialog.
   if ([filter length] > 0 &&
       [filter rangeOfString: @"*."].location != NSNotFound)
   {
      NSString* toExt = [filter substringFromIndex:
                         [filter rangeOfString: @"*."].location + 2];
      NSString* fromExt = [toExt substringToIndex:
                           [toExt rangeOfString: @")"].location];
      
      // If we have the extension equal to 'Rproj', then use the
      // Uniform Type Identifier (UTI) associated with it
      if ([[fromExt lowercaseString] isEqualTo: @"rproj"])
      {
         fromExt = @"dyn.ah62d4rv4ge81e6dwr7za";
      }
      
      [open setAllowedFileTypes: [NSArray arrayWithObject: fromExt]];
   }
   return [self runFileDialog: open];
}

- (NSString*) getSaveFileName: (NSString*) caption
              dir: (NSString* ) dir
              defaultExtension: (NSString*) defaultExtension
              forceDefaultExtension: (Boolean) forceDefaultExtension
{
   dir = resolveAliasedPath(dir);
   
   NSSavePanel *save = [NSSavePanel savePanel];
   
   BOOL hasDefaultExtension = defaultExtension != nil &&
                              [defaultExtension length] > 0;
   if (hasDefaultExtension)
   {
      NSArray* extensions;
      if ([defaultExtension isEqualToString: @".cpp"])
      {
         extensions = @[@"cpp", @"c", @"hpp", @"h"];
      }
      else
      {
         // The method is invoked with an extension like ".R", but NSSavePanel
         // expects extensions to look like "R" (i.e. no leading period).
         extensions = [NSArray arrayWithObject:
                       [defaultExtension substringFromIndex: 1]];
      }
      
      [save setAllowedFileTypes: extensions];
      [save setAllowsOtherFileTypes: !forceDefaultExtension];
   }
   
   // determine the default filename
   FilePath filePath([dir UTF8String]);
   if (!filePath.isDirectory())
   {
      std::string filename;
      if (hasDefaultExtension)
         filename = filePath.stem();
      else
         filename = filePath.filename();
      [save setNameFieldStringValue:
                  [NSString stringWithUTF8String: filename.c_str()]];

      // In OSX 10.6, leaving the filename as part of the directory (in the
      // argument to setDirectoryURL below) causes the file to be treated as
      // though it were a directory itself.  Remove it to avoid confusion.
      NSRange idx = [dir rangeOfString: @"/"
                               options: NSBackwardsSearch];
      if (idx.location != NSNotFound)
         dir = [dir substringToIndex: idx.location];
   }

   NSURL *path = [NSURL fileURLWithPath:
                  [dir stringByStandardizingPath]];

   [save setTitle: caption];
   [save setDirectoryURL: path];
   return [self runFileDialog: save];
}

- (NSString*) getExistingDirectory: (NSString*) caption dir: (NSString*) dir
{
   dir = resolveAliasedPath(dir);
   NSOpenPanel *open = [NSOpenPanel openPanel];
   [open setTitle: caption];
   [open setDirectoryURL: [NSURL fileURLWithPath:
                           [dir stringByStandardizingPath]]];
   [open setCanChooseFiles: false];
   [open setCanChooseDirectories: true];
   [open setCanCreateDirectories: true];
   return [self runFileDialog: open];
}

- (void) undo: (bool) forAce
{
   if (forAce)
   {
      // in the ACE editor, synthesize a literal Cmd+Z for Ace to handle
      CGEventRef event1, event2, event3, event4;
      event1 = CGEventCreateKeyboardEvent (NULL, (CGKeyCode)55, true);
      event2 = CGEventCreateKeyboardEvent (NULL, (CGKeyCode)6, true);
      event3 = CGEventCreateKeyboardEvent (NULL, (CGKeyCode)6, false);
      event4 = CGEventCreateKeyboardEvent (NULL, (CGKeyCode)55, false);
      CGEventSetFlags(event2, kCGEventFlagMaskCommand);
      CGEventSetFlags(event3, kCGEventFlagMaskCommand);
      CGEventPost(kCGHIDEventTap, event1);
      CGEventPost(kCGHIDEventTap, event2);
      CGEventPost(kCGHIDEventTap, event3);
      CGEventPost(kCGHIDEventTap, event4);
   }
   else
   {
      // elsewhere, let the webview handle it natively
      WebViewController* webViewController = (WebViewController*)[[NSApp mainWindow] delegate];
      [[[webViewController webView] undoManager] undo];
   }
}

- (void) redo: (bool) forAce
{
   if (forAce)
   {
      // in the ACE editor, synthesize a literal Cmd+Shift+Z for Ace to handle
      CGEventRef event1, event2, event3, event4, event5, event6;
      event1 = CGEventCreateKeyboardEvent (NULL, (CGKeyCode)55, true);
      event2 = CGEventCreateKeyboardEvent (NULL, (CGKeyCode)56, true);
      event3 = CGEventCreateKeyboardEvent (NULL, (CGKeyCode)6, true);
      event4 = CGEventCreateKeyboardEvent (NULL, (CGKeyCode)6, false);
      event5 = CGEventCreateKeyboardEvent (NULL, (CGKeyCode)56, false);
      event6 = CGEventCreateKeyboardEvent (NULL, (CGKeyCode)55, false);
      CGEventSetFlags(event3, kCGEventFlagMaskCommand | kCGEventFlagMaskShift);
      CGEventSetFlags(event4, kCGEventFlagMaskCommand | kCGEventFlagMaskShift);
      CGEventPost(kCGHIDEventTap, event1);
      CGEventPost(kCGHIDEventTap, event2);
      CGEventPost(kCGHIDEventTap, event3);
      CGEventPost(kCGHIDEventTap, event4);
      CGEventPost(kCGHIDEventTap, event5);
      CGEventPost(kCGHIDEventTap, event6);
   }
   else
   {
      // elsewhere, let the webview handle it natively
      WebViewController* webViewController = (WebViewController*)[[NSApp mainWindow] delegate];
      [[[webViewController webView] undoManager] redo];
   }
}

- (void) performClipboardAction: (SEL) selector
{
   // we use a macro just to avoid having to name the return type here
#define RS_WEB_VIEW ([[[NSApp mainWindow] windowController] webView])
   if (RS_WEB_VIEW == nil) {
      NSString* errorMsg = [NSString stringWithFormat: @"nil webView on clipboard action %@", NSStringFromSelector(selector)];
      LOG_ERROR_MESSAGE([errorMsg UTF8String]);
      return;
   }

   if ([RS_WEB_VIEW respondsToSelector: selector]) {
      [RS_WEB_VIEW performSelector: selector withObject: RS_WEB_VIEW];
   } else {
      NSString* errorMsg = [NSString stringWithFormat: @"@webView does not respond to selector %@", NSStringFromSelector(selector)];
      LOG_ERROR_MESSAGE([errorMsg UTF8String]);
      return;
   }
#undef RS_WEB_VIEW
}

- (void) clipboardCut
{
   [self performClipboardAction: @selector(cut:)];
}

- (void) clipboardCopy
{
   [self performClipboardAction: @selector(copy:)];
}

- (void) clipboardPaste
{
   [self performClipboardAction: @selector(paste:)];
}

- (NSString*) getUriForPath: (NSString*) path
{
   NSURL* url = [NSURL fileURLWithPath: resolveAliasedPath(path)];
   return [url absoluteString];
}


- (void) onWorkbenchInitialized: (NSString*) scratchPath
{
   [[MainFrameController instance] onWorkbenchInitialized];
}

- (void) showFolder: (NSString*) path
{
   if (path == nil || [path length] == 0)
      return;
   
   path = resolveAliasedPath(path);
   
   [[NSWorkspace sharedWorkspace] openFile: path];
}

- (void) showFile: (NSString*) path
{
   if (path == nil || [path length] == 0)
      return;
   
   path = resolveAliasedPath(path);
   
   // force preview for pdfs
   if ([path hasSuffix: @".pdf"])
   {
      [[NSWorkspace sharedWorkspace] openFile: path
                              withApplication: @"Preview"];
   }
   else
   {
      [[NSWorkspace sharedWorkspace] openFile: path];
   }
}

- (void) showWordDoc: (NSString*) path
{
   if (path == nil || [path length] == 0)
      return;
   
   bool opened = false;
   
   // create the structure describing the doc to open
   path = resolveAliasedPath(path);
   
   // figure out if word is installed
   if ([[NSWorkspace sharedWorkspace] fullPathForApplication:@"Microsoft Word"]!= nil)
   {
       // looks like Word is installed. try to reopen this Word document if it's
       // already open, while preserving its scroll position; if it isn't already
       // open, open it.
       NSString *openDocScript = [NSString stringWithFormat:
            @"tell application \"Microsoft Word\"\n"
            "  activate\n"
            "  set reopened to false\n"
            "  repeat with i from 1 to (count of documents)\n"
            "     set docPath to full name of document i\n"
            "     if POSIX path of docPath is equal to \"%@\" then\n"
            "        set w to active window of document i\n"
            "        set h to horizontal percent scrolled of w\n"
            "        set v to vertical percent scrolled of w\n"
            "        close document i\n"
            "        set d to open file name docPath with read only\n"
            "        set reopened to true\n"
            "        set w to active window of d\n"
            "        set horizontal percent scrolled of w to h\n"
            "        set vertical percent scrolled of w to v\n"
            "        exit repeat\n"
            "     end if\n"
            "  end repeat\n"
            "  if not reopened then open file name POSIX file \"%@\" with read only\n"
            "end tell\n" , path, path];
         
       NSAppleScript *openDoc =
           [[[NSAppleScript alloc] initWithSource: openDocScript] autorelease];
         
       if ([openDoc executeAndReturnError: nil] != nil)
       {
           opened = true;
       }
   }
   
   if (!opened)
   {
      // the AppleScript failed (or Word wasn't found), so try an alternate
      // method of opening the document.
      CFURLRef urls[1];
      urls[0] = (CFURLRef)[NSURL fileURLWithPath: path];
      CFArrayRef docArr =
      CFArrayCreate(kCFAllocatorDefault, (const void**)&urls, 1,
                    &kCFTypeArrayCallBacks);
      
      // ask the OS to open the doc for us in an appropriate viewer
      OSStatus status = LSOpenURLsWithRole(docArr, kLSRolesViewer, NULL, NULL, NULL, 0);
      if (status != noErr)
      {
         // if we failed to open in the viewer role, just invoke the default
         // opener
         [self showFile: path];
      }
   }
}

- (void) showPDF: (NSString*) path pdfPage: (int) pdfPage
{
   [self showFile: path];
}


- (double) devicePixelRatio
{
   NSWindow* mainWindow = [[MainFrameController instance] window];
   if ([mainWindow respondsToSelector:@selector(backingScaleFactor)])
   {
      return [mainWindow backingScaleFactor];
   }
   else
   {
      return 1.0;
   }
}


- (void) openMinimalWindow: (NSString*) name url: (NSString*) url
                     width: (int) width height: (int) height
{
   // adjust name to scope within minimal windows
   NSString* windowName = [name stringByAppendingString: kMinimalSuffix];
   
   // check for an existing window with this name
   WebViewController* controller = [WebViewController windowNamed: windowName];
   
   // create a new window if necessary
   if (!controller)
   {
      // self-freeing so don't auto-release
      controller = [[WebViewController alloc] initWithURLRequest:
                  [NSURLRequest requestWithURL: [NSURL URLWithString: url]]
                                          name: windowName
                                    clientName: name
                         allowExternalNavigate: false];
      
      if ([windowName isEqualToString: @"_rstudio_viewer_zoom_minimal"])
         [[controller window] setTitle: @"Viewer Zoom"];
   }
   
   // reset window size (adjust for title bar height)
   NSRect frame = [[controller window] frame];
   NSPoint origin = frame.origin;
   height += desktop::utils::titleBarHeight();
   frame = NSMakeRect(origin.x, origin.y, width, height);
   [[controller window] setFrame: frame display: NO];
   
   // load url
   NSURL* nsurl = [NSURL URLWithString: url];   
   [controller loadURL: nsurl];
  
   // bring to front
   [[controller window] makeKeyAndOrderFront: self];
}

- (void) activateSatelliteWindow: (NSString*) name
{
   [WebViewController activateNamedWindow: name];
}

- (void) activateMinimalWindow: (NSString*) name
{
   name = [name stringByAppendingString: kMinimalSuffix];
   [WebViewController activateNamedWindow: name];
}

- (void) prepareForSatelliteWindow: (NSString*) name
                                 x: (int) x
                                 y: (int) y
                             width: (int) width
                            height: (int) height
{
   [WebViewController prepareForSatelliteWindow: name
                                              x: x
                                              y: y
                                          width: width
                                         height: height];
}

- (void) prepareForNamedWindow: (NSString*) name
         allowExternalNavigate: (bool) allowExternalNavigate
{
   [WebViewController prepareForNamedWindow: name
                      allowExternalNavigate: allowExternalNavigate];
}

- (void) closeNamedWindow: (NSString*) name
{
   [WebViewController closeNamedWindow: name];
   [self bringMainFrameToFront];
}

- (void) copyImageToClipboard: (int) left top: (int) top
                        width: (int) width height: (int) height
{
   // Unlike the Qt implementation, the Cocoa implementation relies on the
   // webpage having selected the desired image first.
   [[[MainFrameController instance] webView] copy: self];
}



- (NSImage*) nsImageForPageRegion: (NSRect) regionRect
{
   // get the main web view
   NSView* view = [[MainFrameController instance] webView];
   
   // offset to determine the location of the view within it's window
   NSRect originRect = [view convertRect:[view bounds] toView:[[view window] contentView]];
   
   // determine the capture rect in screen coordinates (start with the full view)
   NSRect captureRect = originRect;
   captureRect.origin.x += [view window].frame.origin.x;
   captureRect.origin.y = [[view window] screen].frame.size.height -
                           [view window].frame.origin.y -
                           originRect.origin.y -
                           originRect.size.height;
   
   // offset for the passed region rect (subset of the view we are capturing)
   captureRect.origin.x += regionRect.origin.x;
   captureRect.origin.y += regionRect.origin.y;
   captureRect.size = regionRect.size;
   
   // perform the capture
   CGImageRef imageRef = CGWindowListCreateImage(captureRect,
                                                 kCGWindowListOptionIncludingWindow,
                                                 (CGWindowID)[[view window] windowNumber],
                                                 kCGWindowImageDefault);
   CFAutoRelease imageAutoRelease(imageRef);
   
   // create an NSImage
   NSImage* image = [[NSImage alloc] initWithCGImage: imageRef size: NSZeroSize];
   
   // downsample if this is a retina display
   if ([self devicePixelRatio] != 1.0)
   {
      // allocate the imageRep
      NSSize size = regionRect.size;
      NSBitmapImageRep *imageRep = [[NSBitmapImageRep alloc]
                                    initWithBitmapDataPlanes:NULL
                                    pixelsWide: size.width
                                    pixelsHigh: size.height
                                    bitsPerSample: (4 * [self devicePixelRatio])
                                    samplesPerPixel: 4
                                    hasAlpha: YES
                                    isPlanar: NO
                                    colorSpaceName: NSCalibratedRGBColorSpace
                                    bytesPerRow: 0
                                    bitsPerPixel: 0];
      [imageRep setSize: size];
      
      // draw the original into the imageRep
      [NSGraphicsContext saveGraphicsState];
      [NSGraphicsContext setCurrentContext:[NSGraphicsContext graphicsContextWithBitmapImageRep:imageRep]];
      [image drawInRect: NSMakeRect(0, 0, size.width, size.height)
               fromRect: NSZeroRect
              operation: NSCompositeCopy
               fraction: 1.0];
      [NSGraphicsContext restoreGraphicsState];
      
      // release the original image, create a new one with the imageRep, then release the imageRep
      [image release];
      image = [[NSImage alloc] initWithSize:[imageRep size]];
      [image addRepresentation: imageRep];
      [imageRep release];
   }

   // return the image
   return [image autorelease];
}



- (void) copyPageRegionToClipboard: (int) left top: (int) top
                             width: (int) width height: (int) height
{
   // get an image for the specified region
   NSRect regionRect = NSMakeRect(left, top, width, height);
   NSImage* image = [self nsImageForPageRegion: regionRect];
   
   // copy it to the pasteboard
   NSPasteboard *pboard = [NSPasteboard generalPasteboard];
   [pboard clearContents];
   NSArray *copiedObjects = [NSArray arrayWithObject:image];
   [pboard writeObjects: copiedObjects];
}


- (void) exportPageRegionToFile: (NSString*) targetPath
                         format: (NSString*) format
                           left: (int) left
                            top: (int) top
                          width: (int) width
                         height: (int) height
{
   // resolve path
   targetPath = resolveAliasedPath(targetPath);
   
   // get an image for the specified region
   NSRect regionRect = NSMakeRect(left, top, width, height);
   NSImage* image = [self nsImageForPageRegion: regionRect];
   
   // determine format and properties for writing file
   NSBitmapImageFileType imageFileType;
   NSDictionary* properties = nil;
   if ([format isEqualToString: @"png"])
   {
      imageFileType = NSPNGFileType;
   }
   else if ([format isEqualToString: @"jpeg"])
   {
      imageFileType = NSJPEGFileType;
      [properties setValue: [NSNumber numberWithDouble: 1.0]
                    forKey: NSImageCompressionFactor];
   }
   else if ([format isEqualToString: @"tiff"])
   {
      imageFileType = NSTIFFFileType;
      [properties setValue: [NSNumber numberWithInteger: NSTIFFCompressionNone]
                    forKey: NSImageCompressionMethod];
   }
   else // keep compiler happy
   {
      imageFileType = NSPNGFileType;
   }
   
   // write to file
   NSBitmapImageRep *imageRep = (NSBitmapImageRep*) [[image representations] objectAtIndex: 0];
   NSData *data = [imageRep representationUsingType: imageFileType properties: properties];
   if (![data writeToFile: targetPath atomically: NO])
   {
      Error error = systemError(boost::system::errc::io_error, ERROR_LOCATION);
      error.addProperty("target-file", [targetPath UTF8String]);
      LOG_ERROR(error);
   }
}


- (Boolean) supportsClipboardMetafile
{
   return false;
}

- (void) modalAlertDidEnd: (void *) alert
               returnCode: (int) returnCode
              contextInfo: (int *) contextInfo
{
   [NSApp stopModalWithCode: returnCode];
}

- (int) showMessageBox: (int) type
               caption: (NSString*) caption
               message: (NSString*) message
               buttons: (NSString*) buttons // Pipe-delimited
         defaultButton: (int) defaultButton
          cancelButton: (int) cancelButton
{
   NSArray *dialogButtons = [buttons componentsSeparatedByString: @"|"];
   NSAlert *alert = [[[NSAlert alloc] init] autorelease];

   // Translate the message type requested by the client to the appropriate
   // type of NSAlert
   NSAlertStyle style = NSInformationalAlertStyle;
   if (type == MSG_WARNING || type == MSG_ERROR)
      style = NSWarningAlertStyle;
   
   // Choose an image type appropriate to the alert
   NSString* imageName = @"";
   if (type == MSG_POPUP_BLOCKED)
      imageName = @"dialog_popup_blocked";
   else if (type == MSG_INFO)
      imageName = @"dialog_info";
   else if (type == MSG_WARNING)
      imageName = @"dialog_warning";
   else if (type == MSG_ERROR)
      imageName = @"dialog_error";
   else if (type == MSG_QUESTION)
      imageName = @"dialog_question";
   
   if ([imageName length] > 0)
   {
      [alert setIcon: [NSImage imageNamed: imageName]];
   }
      
   [alert setMessageText:caption];
   [alert setInformativeText:message];
   [alert setAlertStyle: style];
   
   for (NSString* buttonText in dialogButtons)
   {
      [alert addButtonWithTitle: buttonText];
   }

   // Make Enter invoke the default button, and ESC the cancel button.
   [[[alert buttons] objectAtIndex:defaultButton] setKeyEquivalent: @"\r"];
   [[[alert buttons] objectAtIndex:cancelButton] setKeyEquivalent: @"\033"];
   [alert beginSheetModalForWindow: [uiDelegate_ uiWindow]
                     modalDelegate: self
                    didEndSelector: @selector(modalAlertDidEnd:returnCode:contextInfo:)
                       contextInfo: nil];
   // Run the dialog and translate the result
   int clicked = [NSApp runModalForWindow: [alert window]];
   switch(clicked)
   {
      case NSAlertFirstButtonReturn:
         return 0;
      case NSAlertSecondButtonReturn:
         return 1;
   }
   return (clicked - NSAlertThirdButtonReturn) + 2;
}

- (void) showAboutDialog
{
   [NSApp orderFrontStandardAboutPanel: self];
}

- (void) bringMainFrameToFront
{
   [NSApp activateIgnoringOtherApps: YES];
   [[[MainFrameController instance] window] makeKeyAndOrderFront: self];
}

- (void) bringMainFrameBehindActive
{
   NSWindow* keyWindow = [NSApp keyWindow];
   NSWindow* mainWindow =  [[MainFrameController instance] window];
   [mainWindow orderWindow: NSWindowBelow
                relativeTo: [keyWindow windowNumber]];
}

- (void) cleanClipboard: (Boolean) stripHtml
{
   // Remove all but plain-text and (optionally) HTML data from the pasteboard.
   
   NSPasteboard* pasteboard = [NSPasteboard generalPasteboard];
   if ([[pasteboard pasteboardItems] count] == 0)
      return;
   
   NSString* data = [pasteboard stringForType: NSStringPboardType];
   if (data == nil)
      return;
   
   NSString* htmlData = nil;
   if (!stripHtml)
      htmlData = [pasteboard stringForType: NSHTMLPboardType];
   
   [pasteboard clearContents];
   
   [pasteboard setString: data forType: NSStringPboardType];
   if (htmlData != nil)
      [pasteboard setString: htmlData forType: NSHTMLPboardType];
}

- (void) setPendingQuit: (int) pendingQuit
{
   sessionLauncher().setPendingQuit((PendingQuit)pendingQuit);
}

- (void) openInNewWindow: (NSArray*) args
{
   NSString* exePath = [NSString stringWithUTF8String:
               desktop::options().executablePath().absolutePath().c_str()];
   [NSTask launchedTaskWithLaunchPath: exePath arguments: args];
}

- (void) openProjectInNewWindow: (NSString*) projectFilePath
{
   projectFilePath = resolveAliasedPath(projectFilePath);
   NSArray* args = [NSArray arrayWithObjects: projectFilePath,
                                              kInitialGeometryArg,
                                              getNewWindowGeometry(), nil];
   [self openInNewWindow: args];
}

- (void) openProjectInOverlaidNewWindow: (NSString*) projectFilePath
{
   if (![projectFilePath isEqualToString: @"none"])
      projectFilePath = resolveAliasedPath(projectFilePath);
   NSArray* args = [NSArray arrayWithObjects: projectFilePath, nil];
   [self openInNewWindow: args];
}

- (void) openSessionInNewWindow: (NSString*) workingDirectoryPath
{
   workingDirectoryPath = resolveAliasedPath(workingDirectoryPath);   
   core::system::setenv(kRStudioInitialWorkingDir, [workingDirectoryPath UTF8String]);
   NSArray* args = [NSArray arrayWithObjects: kInitialGeometryArg,
                   getNewWindowGeometry(), nil];
   [self openInNewWindow: args];
}



- (void) openTerminal: (NSString*) terminalPath
         workingDirectory: (NSString*) workingDirectory
         extraPathEntries: (NSString*) extraPathEntries
{
   // append extra path entries to our path before launching
   if ([extraPathEntries length] > 0)
   {
      std::string path = core::system::getenv("PATH");
      std::string previousPath = path;
      core::system::addToPath(&path, [extraPathEntries UTF8String]);
      core::system::setenv("PATH", path);
   }
   
   // call Terminal.app with an applescript that navigates it
   // to the specified directory. note we don't reference the
   // passed terminalPath because this setting isn't respected
   // on the Mac (we always use Terminal.app)
   FilePath macTermScriptFilePath =
            desktop::options().scriptsPath().complete("mac-terminal");
   NSString* exePath = [NSString stringWithUTF8String:
                              macTermScriptFilePath.absolutePath().c_str()];
   workingDirectory = resolveAliasedPath(workingDirectory);
   NSArray* args = [NSArray arrayWithObject: workingDirectory];
   [NSTask launchedTaskWithLaunchPath: exePath arguments: args];
}

- (NSString*) getFixedWidthFontList
{
   NSArray* fonts = [[NSFontManager sharedFontManager]
                         availableFontNamesWithTraits: NSFixedPitchFontMask];
   return [fonts componentsJoinedByString: @"\n"];
}

- (NSString*) getFixedWidthFont
{
   return [NSString stringWithUTF8String:
                              desktop::options().fixedWidthFont().c_str()];
}

- (void) setFixedWidthFont: (NSString*) font
{
   desktop::options().setFixedWidthFont([font UTF8String]);
}

- (void) macZoomActualSize
{
   [self macZoomDelta: 0];
}

- (void) macZoomIn
{
   [self macZoomDelta: 1];
}

- (void) macZoomOut
{
   [self macZoomDelta: -1];
}

- (void) macZoomDelta: (int) delta
{
   WebViewController* controller = [WebViewController activeDesktopController];
   if ([controller isMemberOfClass: [MainFrameController class]])
   {
      // if this is the main frame, save its zoom level and sync
      int newZoomLevel = delta == 0 ?
                                  0 : desktop::options().zoomLevel() + delta;
      desktop::options().setZoomLevel(newZoomLevel);
      [controller syncZoomLevel];
   }
   else
   {
      // not the main frame, zoom it independently
      [controller adjustZoomLevel: delta];
   }
}


- (Boolean) supportsFullscreenMode
{
   NSWindow* mainWindow = [[MainFrameController instance] window];
   return desktop::utils::supportsFullscreenMode(mainWindow);
}

- (void) toggleFullscreenMode
{
   NSWindow* mainWindow = [[MainFrameController instance] window];
   desktop::utils::toggleFullscreenMode(mainWindow);
}

- (void) showKeyboardShortcutHelp
{
   FilePath keyboardHelpPath = options().wwwDocsPath().complete("keyboard.htm");
   NSURL* url = [NSURL fileURLWithPath:
      [NSString stringWithUTF8String: keyboardHelpPath.absolutePath().c_str()]];
   [[NSWorkspace sharedWorkspace] openURL: url];
}

- (void) launchSession: (Boolean) reload
{
   sessionLauncher().launchNextSession(reload);
}

- (void) reloadZoomWindow
{
   WebViewController* controller =
            [WebViewController windowNamed: @"_rstudio_zoom_minimal"];
   if (controller)
      [[[controller webView] mainFrame] reload];
}

- (void) setViewerUrl: (NSString*) url
{
   [[MainFrameController instance] setViewerURL: url];
}

- (void) setShinyDialogUrl: (NSString*) url
{
   [[MainFrameController instance] setShinyDialogURL: url];
}

- (void) reloadViewerZoomWindow: (NSString*) url
{
   WebViewController* controller =
      [WebViewController windowNamed: @"_rstudio_viewer_zoom_minimal"];
   if (controller) {
      [[[controller webView] mainFrame] loadRequest:[NSURLRequest requestWithURL:[NSURL URLWithString:url]]];
   }
}

- (NSString*) getScrollingCompensationType
{
   return @"None";
}

- (Boolean) isOSXMavericks
{
   NSDictionary *systemVersionDictionary =
   [NSDictionary dictionaryWithContentsOfFile:
    @"/System/Library/CoreServices/SystemVersion.plist"];
   
   NSString *systemVersion =
   [systemVersionDictionary objectForKey:@"ProductVersion"];
   
   std::string version(
                       [systemVersion cStringUsingEncoding:NSASCIIStringEncoding]);
   
   return boost::algorithm::starts_with(version, "10.9") ||
          boost::algorithm::starts_with(version, "10.10");
}

- (Boolean) isCentOS
{
   return NO;
}

// On Mavericks we need to tell the OS that we are busy so that
// AppNap doesn't kick in. Declare a local version of NSActivityOptions
// so we can build this on non-Mavericks systems
enum RS_NSActivityOptions : uint64_t
{
   RS_NSActivityIdleDisplaySleepDisabled = (1ULL << 40),
   RS_NSActivityIdleSystemSleepDisabled = (1ULL << 20),
   RS_NSActivitySuddenTerminationDisabled = (1ULL << 14),
   RS_NSActivityAutomaticTerminationDisabled = (1ULL << 15),
   RS_NSActivityUserInitiated = (0x00FFFFFFULL | RS_NSActivityIdleSystemSleepDisabled),
   RS_NSActivityUserInitiatedAllowingIdleSystemSleep = (RS_NSActivityUserInitiated & ~RS_NSActivityIdleSystemSleepDisabled),
   RS_NSActivityBackground = 0x000000FFULL,
   RS_NSActivityLatencyCritical = 0xFF00000000ULL,
};

- (void) setBusy: (Boolean) busy
{
   id pi = [NSProcessInfo processInfo];
   if ([pi respondsToSelector: @selector(beginActivityWithOptions:reason:)])
   {
      if (busy && busyActivity_ == nil)
      {
         busyActivity_ = [[pi performSelector: @selector(beginActivityWithOptions:reason:)
                  withObject: [NSNumber numberWithInt:
                         RS_NSActivityUserInitiatedAllowingIdleSystemSleep]
                  withObject: @"R Computation"] retain];
      }
      else if (!busy && busyActivity_ != nil)
      {
         [pi performSelector: @selector(endActivity:) withObject: busyActivity_];
         [busyActivity_ release];
         busyActivity_ = nil;
      }
   }
}

- (void) setWindowTitle: (NSString*) title
{
   [[MainFrameController instance] setWindowTitle: title];
}

- (void) setPendingProject: (NSString*) projectPath
{
   [self setPendingQuit: 1];
   [[MainFrameController instance] setPendingProject: projectPath];
}
 
- (NSString*) filterText: (NSString*) text
{
   // Normalize NFD Unicode text. I couldn't reproduce the behavior that made this
   // necessary in the first place but just in case, and for symmetry with the Qt
   // code, do the normalization anyway.
   return [text precomposedStringWithCanonicalMapping];
}


// R version methods are only implemented for front-ends that
// enable the user to choose from multiple R versions

- (NSString*) getRVersion
{
   return @"";
}

- (NSString*) chooseRVersion
{
   return @"";
}

- (Boolean) canChooseRVersion
{
   return false;
}


// No desktop synctex on the Mac

- (NSString*) getDesktopSynctexViewer
{
   return @"";
}

- (void) externalSynctexPreview: (NSString*) pdfPath page: (int) page
{
}

- (void) externalSynctexView: (NSString*) pdfFile
                     srcFile: (NSString*) srcFile
                        line: (int) line
                      column: (int) column
{
}

// Custom zoom implementation on the Mac

- (NSString*) getZoomLevels
{
    return @"";
}

- (double) getZoomLevel
{
    return 1.0;
}

- (void) setZoomLevel: (double) zoomLevel
{
}


// We allow WebTextInput to handle prompt for text in the Cocoa port

- (NSString*) promptForText: (NSString*) title
                    caption: (NSString*) caption
               defaultValue: (NSString*) defaultValue
            usePasswordMask: (Boolean) usePasswordMask
     rememberPasswordPrompt: (NSString*) rememberPasswordPrompt
          rememberByDefault: (Boolean) rememberByDefault
                numbersOnly: (Boolean) numbersOnly
             selectionStart: (int) selectionStart
            selectionLength: (int) selectionLength
{
   return @"";
}


+ (NSString *) webScriptNameForSelector: (SEL) sel
{
   if (sel == @selector(browseUrl:))
      return @"browseUrl";
   else if (sel == @selector(getOpenFileName:dir:filter:canChooseDirectories:))
      return @"getOpenFileName";
   else if (sel == @selector(getSaveFileName:dir:defaultExtension:forceDefaultExtension:))
      return @"getSaveFileName";
   else if (sel == @selector(getExistingDirectory:dir:))
      return @"getExistingDirectory";
   else if (sel == @selector(getUriForPath:))
      return @"getUriForPath";
   else if (sel == @selector(onWorkbenchInitialized:))
      return @"onWorkbenchInitialized";
   else if (sel == @selector(showFolder:))
      return @"showFolder";
   else if (sel == @selector(showFile:))
      return @"showFile";
   else if (sel == @selector(showWordDoc:))
      return @"showWordDoc";
   else if (sel == @selector(showPDF:pdfPage:))
      return @"showPDF";
   else if (sel == @selector(openMinimalWindow:url:width:height:))
      return @"openMinimalWindow";
   else if (sel == @selector(activateMinimalWindow:))
      return @"activateMinimalWindow";
   else if (sel == @selector(activateSatelliteWindow:))
      return @"activateSatelliteWindow";
   else if (sel == @selector(prepareForSatelliteWindow:x:y:width:height:))
      return @"prepareForSatelliteWindow";
   else if (sel == @selector(copyImageToClipboard:top:width:height:))
      return @"copyImageToClipboard";
   else if (sel == @selector(copyPageRegionToClipboard:top:width:height:))
      return @"copyPageRegionToClipboard";
   else if (sel == @selector(exportPageRegionToFile:format:left:top:width:height:))
      return @"exportPageRegionToFile";
   else if (sel == @selector(showMessageBox:caption:message:buttons:defaultButton:cancelButton:))
      return @"showMessageBox";
   else if (sel == @selector(promptForText:caption:defaultValue:usePasswordMask:rememberPasswordPrompt:rememberByDefault:numbersOnly:selectionStart:selectionLength:))
      return @"promptForText";
   else if (sel == @selector(cleanClipboard:))
      return @"cleanClipboard";
   else if (sel == @selector(setPendingQuit:))
      return @"setPendingQuit";
   else if (sel == @selector(openProjectInNewWindow:))
      return @"openProjectInNewWindow";
   else if (sel == @selector(openSessionInNewWindow:))
      return @"openSessionInNewWindow";
   else if (sel == @selector(openTerminal:workingDirectory:extraPathEntries:))
      return @"openTerminal";
   else if (sel == @selector(setFixedWidthFont:))
      return @"setFixedWidthFont";
   else if (sel == @selector(setZoomLevel:))
      return @"setZoomLevel";
   else if (sel == @selector(externalSynctexPreview:page:))
      return @"externalSynctexPreview";
   else if (sel == @selector(externalSynctexView:srcFile:line:column:))
      return @"externalSynctexView";
   else if (sel == @selector(launchSession:))
      return @"launchSession";
   else if (sel == @selector(setViewerUrl:))
      return @"setViewerUrl";
   else if (sel == @selector(setShinyDialogUrl:))
      return @"setShinyDialogUrl";
   else if (sel == @selector(filterText:))
      return @"filterText";
   else if (sel == @selector(setBusy:))
      return @"setBusy";
   else if (sel == @selector(setWindowTitle:))
      return @"setWindowTitle";
   else if (sel == @selector(reloadViewerZoomWindow:))
      return @"reloadViewerZoomWindow";
   else if (sel == @selector(prepareForNamedWindow:allowExternalNavigate:))
      return @"prepareForNamedWindow";
   else if (sel == @selector(closeNamedWindow:))
      return @"closeNamedWindow";
   else if (sel == @selector(undo:))
      return @"undo";
   else if (sel == @selector(redo:))
      return @"redo";
   else if (sel == @selector(setPendingProject:))
      return @"setPendingProject";
      
   return nil;
}

+ (BOOL)isSelectorExcludedFromWebScript: (SEL) sel
{
   if (sel == @selector(setUIDelegate:))
      return YES;
   else
      return NO;
}

@end

#ifdef __clang__
#pragma clang diagnostic pop
#endif

