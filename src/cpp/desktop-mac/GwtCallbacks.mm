

#include <boost/algorithm/string/predicate.hpp>

#include <core/FilePath.hpp>
#include <core/system/System.hpp>

#import "GwtCallbacks.h"
#import "Options.hpp"

#import <Foundation/NSString.h>

#include "SessionLauncher.hpp"
#include "Utils.hpp"

#import "MainFrameController.h"

using namespace core;
using namespace desktop;

namespace {
   
FilePath userHomePath()
{
   return core::system::userHomePath("R_USER|HOME");
}

NSString* resolveAliasedPath(NSString* path)
{
   FilePath resolved = FilePath::resolveAliasedPath([path UTF8String],
                                                    userHomePath());
   return [NSString stringWithUTF8String: resolved.absolutePath().c_str()];
}
   
} // anonymous namespace

@implementation GwtCallbacks


- (id)init
{
   if (self = [super init])
   {
      appDelegate_ = (AppDelegate*)[NSApp delegate];
   }
   return self;
}

// sentinel function for gwt deferred binding
- (void) isCocoa
{
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

- (NSString*) getOpenFileName: (NSString*) caption
              dir: (NSString*) dir
              filter: (NSString*) filter
{
   NSOpenPanel *open = [NSOpenPanel openPanel];
   [open setTitle: caption];
   [open setDirectoryURL: [NSURL fileURLWithPath:
                           [dir stringByStandardizingPath]]];
   if ([filter length] > 0)
   {
      // TODO: Extract the extension from the filter using string math
      // (i.e. "R Projects (*.RProj)" => "RProj" and apply it using
      // [open setAllowedFileTypes]
   }
   long int result = [open runModal];
   if (result == NSOKButton)
   {
      return [[open URL] path];
   }
   else
   {
      return @"";
   }
}

- (NSString*) getSaveFileName: (NSString*) caption
              dir: (NSString* ) dir
              defaultExtension: (NSString*) defaultExtension
              forceDefaultExtension: (Boolean) forceDefaultExtension
{
   // The method is invoked with an extension like ".R", but NSSavePanel
   // expects extensions to look like "R" (i.e. no leading period).
   NSArray *extensions = [NSArray arrayWithObject:
                          [defaultExtension substringFromIndex: 1]];
   NSURL *pathAndFile = [NSURL fileURLWithPath:
                         [dir stringByStandardizingPath]];
   NSSavePanel *save = [NSSavePanel savePanel];
   [save setAllowedFileTypes: extensions];
   [save setAllowsOtherFileTypes: !forceDefaultExtension];
   [save setTitle: caption];
   [save setDirectoryURL: pathAndFile];
   [save setNameFieldStringValue: [pathAndFile lastPathComponent]];
   long int result = [save runModal];
   if (result == NSOKButton)
   {
      NSString *filename = [[save URL] path];
      return filename;
   }
   else
   {
      return @"";
   }
}

- (NSString*) getExistingDirectory: (NSString*) caption dir: (NSString*) dir
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
   
   return @"getExistingDirectory";
}

- (void) undo
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
}

- (void) redo
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
}

- (void) clipboardCut
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
}

- (void) clipboardCopy
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
}

- (void) clipboardPaste
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
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

- (Boolean) isRetina
{
   NSWindow* mainWindow = [[MainFrameController instance] window];
   if ([mainWindow respondsToSelector:@selector(backingScaleFactor)])
   {
      double scaleFactor = [mainWindow backingScaleFactor];
      return scaleFactor == 2.0;
   }
   else
   {
      return false;
   }
}

- (void) openMinimalWindow: (NSString*) name url: (NSString*) url
                     width: (int) width height: (int) height
{
   // adjust name to scope within minimal windows
   name = [name stringByAppendingString: @"_minimal"];
   
   // check for an existing window with this name
   WebViewController* controller = [WebViewController windowNamed: name];
   
   // create a new window if necessary
   if (!controller)
   {
      // self-freeing so don't auto-release
      controller = [[WebViewController alloc] initWithURLRequest:
                  [NSURLRequest requestWithURL: [NSURL URLWithString: url]]
                                              name: name];
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
   [WebViewController activateSatelliteWindow: name];
}

- (void) prepareForSatelliteWindow: (NSString*) name
                             width: (int) width height: (int) height
{
   [WebViewController prepareForSatelliteWindow: name
                                          width: width
                                          height: height];
}

- (void) copyImageToClipboard: (int) left top: (int) top
                        width: (int) width height: (int) height
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
}

- (Boolean) supportsClipboardMetafile
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
   return false;
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
   if (type == MSG_WARNING)
      style = NSWarningAlertStyle;
   else if (type == MSG_ERROR)
      style = NSCriticalAlertStyle;
   [alert setAlertStyle: style];
   
   // TODO: Pick an image appropriate for each MessageType and attach it to the
   // dialog

   [alert setMessageText:caption];
   [alert setInformativeText:message];
   for (NSString* buttonText in dialogButtons)
   {
      [alert addButtonWithTitle: buttonText];
   }

   // Make Enter invoke the default button, and ESC the cancel button.
   [[[alert buttons] objectAtIndex:defaultButton] setKeyEquivalent: @"\r"];
   [[[alert buttons] objectAtIndex:cancelButton] setKeyEquivalent: @"\033"];
   
   // Run the dialog and translate the result
   int clicked = [alert runModal];
   switch(clicked)
   {
      case NSAlertFirstButtonReturn:
         return 0;
      case NSAlertSecondButtonReturn:
         return 1;
   }
   return (clicked - NSAlertThirdButtonReturn) + 2;
}



- (void) checkForUpdates
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
}

- (void) showAboutDialog
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
}

- (void) bringMainFrameToFront
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
}

- (void) cleanClipboard: (Boolean) stripHtml
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
}

- (void) setPendingQuit: (int) pendingQuit
{
   sessionLauncher().setPendingQuit((PendingQuit)pendingQuit);
}

- (void) openProjectInNewWindow: (NSString*) projectFilePath
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
}

- (void) openTerminal: (NSString*) terminalPath
         workingDirectory: (NSString*) workingDirectory
         extraPathEntries: (NSString*) extraPathEntries
{
   NSLog(@"%@", NSStringFromSelector(_cmd));

}

- (NSString*) getFontList: (Boolean) fixedWidthOnly
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
   return @"fontList";
}

- (NSString*) getFixedWidthFont
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
   return @"getFixedWidthFont";
}

- (void) setFixedWidthFont: (NSString*) font
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
}

- (NSString*) getZoomLevels
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
   return @"1.0\n1.1";
}

- (double) getZoomLevel
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
   return 1.0;
}

- (void) setZoomLevel: (double) zoomLevel
{
   NSLog(@"%@", NSStringFromSelector(_cmd));
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
   
   return boost::algorithm::starts_with(version, "10.9");
}

- (NSString*) filterText: (NSString*) text
{
   // TODO: normalize NFD Unicode text as we do in Qt version
   
   return text;
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
   else if (sel == @selector(getOpenFileName:dir:filter:))
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
   else if (sel == @selector(openMinimalWindow:url:width:height:))
      return @"openMinimalWindow";
   else if (sel == @selector(activateSatelliteWindow:))
      return @"activateSatelliteWindow";
   else if (sel == @selector(prepareForSatelliteWindow:width:height:))
      return @"prepareForSatelliteWindow";
   else if (sel == @selector(copyImageToClipboard:top:width:height:))
      return @"copyImageToClipboard";
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
   else if (sel == @selector(openTerminal:workingDirectory:extraPathEntries:))
      return @"openTerminal";
   else if (sel == @selector(getFontList:))
      return @"getFontList";
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
   else if (sel == @selector(filterText:))
      return @"filterText";
  
   return nil;
}

+ (BOOL)isSelectorExcludedFromWebScript: (SEL) sel
{
   return NO;
}

@end

