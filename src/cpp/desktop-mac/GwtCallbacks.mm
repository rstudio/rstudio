

#include <boost/algorithm/string/predicate.hpp>

#include <core/FilePath.hpp>
#include <core/system/System.hpp>
#include <core/system/Environment.hpp>

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
   
NSString* createAliasedPath(NSString* path)
{
   if (path == nil || [path length] == 0)
      return @"";
   
   std::string aliased = FilePath::createAliasedPath(
                  FilePath([path UTF8String]), userHomePath());
   
   return [NSString stringWithUTF8String: aliased.c_str()];
}


NSString* resolveAliasedPath(NSString* path)
{
   if (path == nil)
      path = @"";
   
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
   [panel beginSheetModalForWindow: [[MainFrameController instance] window]
                completionHandler: nil];
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

- (NSString*) getOpenFileName: (NSString*) caption
              dir: (NSString*) dir
              filter: (NSString*) filter
{
   dir = resolveAliasedPath(dir);
   
   NSOpenPanel *open = [NSOpenPanel openPanel];
   [open setTitle: caption];
   [open setDirectoryURL: [NSURL fileURLWithPath:
                           [dir stringByStandardizingPath]]];
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
      [open setAllowedFileTypes: [NSArray arrayWithObject: fromExt]];
   }
   return [self runSheetFileDialog: open];
}

- (NSString*) getSaveFileName: (NSString*) caption
              dir: (NSString* ) dir
              defaultExtension: (NSString*) defaultExtension
              forceDefaultExtension: (Boolean) forceDefaultExtension
{
   dir = resolveAliasedPath(dir);
   
   NSURL *pathAndFile = [NSURL fileURLWithPath:
                         [dir stringByStandardizingPath]];
   NSSavePanel *save = [NSSavePanel savePanel];
   
   BOOL hasDefaultExtension = defaultExtension != nil &&
                              [defaultExtension length] > 0;
   if (hasDefaultExtension)
   {
      // The method is invoked with an extension like ".R", but NSSavePanel
      // expects extensions to look like "R" (i.e. no leading period).
      NSArray *extensions = [NSArray arrayWithObject:
                                [defaultExtension substringFromIndex: 1]];
  
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
   }

   [save setTitle: caption];
   [save setDirectoryURL: pathAndFile];
   return [self runSheetFileDialog: save];
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
   return [self runSheetFileDialog: open];
}

- (void) undo
{
   if ([NSApp mainWindow] == [[MainFrameController instance] window])
   {
      // It appears that using the webView's undoManager doesn't work (for what we want it to do).
      // It doesn't do anything in the main window when we use the menu to invoke.
      // However the native handling of Cmd+Z seems to do the right thing.
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
      // undoManager works just fine on secondary windows, and sending Cmd+Z sends us into an
      // endless loop of Cmd+Z-ing.
      WebViewController* webViewController = (WebViewController*)[[NSApp mainWindow] delegate];
      [[[webViewController webView] undoManager] undo];
   }
}

- (void) redo
{
   if ([NSApp mainWindow] == [[MainFrameController instance] window])
   {
      // It appears that using the webView's undoManager doesn't work (for what we want it to do).
      // It doesn't do anything in the main window when we use the menu to invoke.
      // However the native handling of Cmd+Shift+Z seems to do the right thing.
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
      // undoManager works just fine on secondary windows, and sending Cmd+Z sends us into an
      // endless loop of Cmd+Shift+Z-ing.
      WebViewController* webViewController = (WebViewController*)[[NSApp mainWindow] delegate];
      [[[webViewController webView] undoManager] redo];
   }
}

- (void) clipboardCut
{
   [[[[NSApp mainWindow] windowController] webView] cut: self];
}

- (void) clipboardCopy
{
   [[[[NSApp mainWindow] windowController] webView] copy: self];
}

- (void) clipboardPaste
{
   [[[[NSApp mainWindow] windowController] webView] paste: self];
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
   // Unlike the Qt implementation, the Cocoa implementation relies on the
   // webpage having selected the desired image first.
   [[[MainFrameController instance] webView] copy: self];
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
   for (NSString* buttonText in dialogButtons)
   {
      [alert addButtonWithTitle: buttonText];
   }

   // Make Enter invoke the default button, and ESC the cancel button.
   [[[alert buttons] objectAtIndex:defaultButton] setKeyEquivalent: @"\r"];
   [[[alert buttons] objectAtIndex:cancelButton] setKeyEquivalent: @"\033"];
   [alert beginSheetModalForWindow: [[MainFrameController instance] window]
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

- (void) openProjectInNewWindow: (NSString*) projectFilePath
{
   projectFilePath = resolveAliasedPath(projectFilePath);
   
   NSString* exePath = [NSString stringWithUTF8String:
               desktop::options().executablePath().absolutePath().c_str()];
   NSArray* args = [NSArray arrayWithObject: projectFilePath];
   
   [NSTask launchedTaskWithLaunchPath: exePath arguments: args];
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
   // reset the zoom level
   desktop::options().setZoomLevel(0);
   [[MainFrameController instance] syncZoomLevel];
}


- (void) macZoomIn
{
   // increment the current zoom level
   desktop::options().setZoomLevel(desktop::options().zoomLevel() + 1);
   [[MainFrameController instance] syncZoomLevel];
}

- (void) macZoomOut
{
   // decrement the current zoom level
   desktop::options().setZoomLevel(desktop::options().zoomLevel() - 1);
   [[MainFrameController instance] syncZoomLevel];
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

