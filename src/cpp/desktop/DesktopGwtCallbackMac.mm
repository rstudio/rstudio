/*
 * DesktopGwtCallbackMac.mm
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "DesktopGwtCallback.hpp"
#include "DesktopGwtWindow.hpp"
#include "DesktopUtilsMac.hpp"

#import <AppKit/NSApplication.h>
#import <AppKit/NSAlert.h>
#import <AppKit/NSOpenPanel.h>
#import <AppKit/NSSavePanel.h>
#import <AppKit/NSButton.h>
#import <AppKit/NSImage.h>
#import <Cocoa/Cocoa.h>

#include <shared_core/FilePath.hpp>
#include <core/Macros.hpp>

using namespace rstudio;
using namespace rstudio::core;
using namespace rstudio::desktop;

RS_BEGIN_NAMESPACE(rstudio)
RS_BEGIN_NAMESPACE(desktop)

RS_BEGIN_NAMESPACE()

static const char* const s_openWordDocumentFormatString =
R"EOF(tell application "Microsoft Word"
   activate
   set reopened to false
   repeat with i from 1 to (count of documents)
      set docPath to full name of document i
      if POSIX path of docPath is equal to "%@" then
         set w to active window of document i
         set h to horizontal percent scrolled of w
         set v to vertical percent scrolled of w
         close document i
         set d to open file name docPath with read only
         set reopened to true
         set w to active window of d
         set horizontal percent scrolled of w to h
         set vertical percent scrolled of w to v
         exit repeat
      end if
   end repeat
   if not reopened then open file name POSIX file "%@" with read only
end tell
)EOF";

static const char* const s_openPptPresFormatString =
R"EOF(tell application "Microsoft PowerPoint"
	activate
	set reopened to false
	repeat with i from 1 to (count of presentations)
		set docPath to full name of presentation i
		if POSIX path of docPath is equal to "%@" then
			set s to slide number of slide range of selection of document window 1 of presentation i
			close presentation i
			open POSIX file "%@" with read only
			set reopened to true
			set v to view of active window
			go to slide v number s
			exit repeat
		end if
	end repeat
	if not reopened then open POSIX file "%@" with read only
end tell

)EOF";
enum MessageType
{
   MSG_POPUP_BLOCKED = 0,
   MSG_INFO = 1,
   MSG_WARNING = 2,
   MSG_ERROR = 3,
   MSG_QUESTION = 4
};

bool showOfficeDoc(NSString* path, NSString* appName, NSString* formatString)
{
   bool opened = false;
   
   // create the structure describing the doc to open
   path = resolveAliasedPath(path);
   
   // figure out if appropriate opener is installed
   if ([[NSWorkspace sharedWorkspace] fullPathForApplication:appName]!= nil)
   {
      NSString* openDocScript = [NSString stringWithFormat: formatString, path, path, path];
      NSAppleScript* openDoc = [[[NSAppleScript alloc] initWithSource: openDocScript] autorelease];
      
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
      OSStatus status = LSOpenURLsWithRole(docArr, kLSRolesViewer, nullptr, nullptr, nullptr, 0);
      if (status != noErr)
      {
         return false;
      }
   }

   return true;
}

NSArray* parseFileNameFilters(NSString* fileNameFilters)
{
    NSArray* splitArr = [fileNameFilters componentsSeparatedByString:@";;"];
    NSMutableArray* allFilters = [[NSMutableArray alloc] init];
    for (id splitItem in splitArr)
    {
        // Get inside the brackets
        NSRange start = [splitItem rangeOfString:@"("];
        NSRange end = [splitItem rangeOfString:@")"];
        NSArray* filterSubset =
        [[splitItem substringWithRange:NSMakeRange(start.location + 1, end.location - start.location - 1)]
         componentsSeparatedByString:@" "];
        for (id filter in filterSubset)
        {
            // Strip the *. from the start of the extension
            NSRange toStrip = [filter rangeOfString:@"*."];
            if (([filter length] > 0) && (toStrip.location != NSNotFound))
            {
                filter = [filter substringFromIndex:(toStrip.location + 2)];
                // If we have the extension equal to 'Rproj', then use the
                // Uniform Type Identifier (UTI) associated with it
                if ([[filter lowercaseString] isEqualTo: @"rproj"])
                {
                    filter = @"dyn.ah62d4rv4ge81e6dwr7za";
                }
                
                [allFilters addObject:filter];
            }
        }
    }
    
    return [allFilters copy];
}

RS_END_NAMESPACE()

void GwtCallback::initialize()
{
}

int GwtCallback::showMessageBox(int type,
                                QString qCaption,
                                QString qMessage,
                                QString qButtons,
                                int defaultButton,
                                int cancelButton)
{
   NSString* caption = qCaption.toNSString();
   NSString* message = qMessage.toNSString();
   NSString* buttons = qButtons.toNSString();
   
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
   // If there's only one button, allow both Enter and ESC to
   // dismiss the dialog. If there's multiple dialogs, accommodate the
   // case where the default button may be the cancel button.
   if ([dialogButtons count] == 1)
   {
      [[[alert buttons] objectAtIndex: defaultButton] setKeyEquivalent: @"\r"];

      // a single button can't have multiple key equivalents, so create a second
      // hidden button to respond to ESC
      NSButton* otherButton = [[NSButton alloc] initWithFrame:NSMakeRect(0, 0, 0, 0)];
      otherButton.target = alert.buttons.lastObject;
      otherButton.action = @selector(performClick:);
      otherButton.keyEquivalent = @"\e"; // Escape
      [alert setAccessoryView:otherButton];
   }
   else
   {
      [[[alert buttons] objectAtIndex: defaultButton] setKeyEquivalent: @"\r"];
      [[[alert buttons] objectAtIndex: cancelButton] setKeyEquivalent: @"\033"];
   }

   NSView* pView = reinterpret_cast<NSView*>(pOwner_->asWidget()->winId());
   [alert beginSheetModalForWindow: [pView window] completionHandler: ^(NSModalResponse response) {
      [[NSApplication sharedApplication] stopModalWithCode: response];
   }];
   
   // Run the dialog and translate the result
   int clicked = [NSApp runModalForWindow: [alert window]];
   switch (clicked)
   {
      case NSAlertFirstButtonReturn:
         return 0;
      case NSAlertSecondButtonReturn:
         return 1;
   }
   return (clicked - NSAlertThirdButtonReturn) + 2;
}

QString GwtCallback::getOpenFileName(const QString& qCaption,
                                     const QString& qLabel,
                                     const QString& qDir,
                                     const QString& qFilter,
                                     bool canChooseDirectories,
                                     bool focusOwner)
{
   NSString* caption = qCaption.toNSString();
   NSString* label   = qLabel.toNSString();
   NSString* dir     = qDir.toNSString();
   NSString* filter  = qFilter.toNSString();
   
   dir = resolveAliasedPath(dir);
   
   NSOpenPanel *open = [NSOpenPanel openPanel];
   [open setTitle: caption];
   [open setPrompt: label];
   [open setDirectoryURL: [NSURL fileURLWithPath:
                           [dir stringByStandardizingPath]]];
   [open setCanChooseDirectories: canChooseDirectories];
   [open setResolvesAliases: NO];
   
   // If the filter was specified and looks like a filter string
   // (i.e. "R Projects (*.RProj)"), extract just the extension ("RProj") to
   // pass to the open dialog.
   if ([filter length] > 0 &&
       [filter rangeOfString: @"*."].location != NSNotFound)
   {
      [open setAllowedFileTypes: parseFileNameFilters(filter)];
   }
   return runFileDialog(open);
}

QString GwtCallback::getSaveFileName(const QString& qCaption,
                                     const QString& qLabel,
                                     const QString& qDir,
                                     const QString& qDefaultExtension,
                                     bool forceDefaultExtension,
                                     bool focusOwner)
{
   NSString* caption          = qCaption.toNSString();
   NSString* label            = qLabel.toNSString();
   NSString* dir              = qDir.toNSString();
   NSString* defaultExtension = qDefaultExtension.toNSString();
   
   dir = resolveAliasedPath(dir);

   NSSavePanel* panel = [NSSavePanel savePanel];
   [panel setPrompt: label];
   
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
      
      [panel setAllowedFileTypes: extensions];
      [panel setAllowsOtherFileTypes: !forceDefaultExtension];
   }
   
   // determine the default filename
   FilePath filePath([dir UTF8String]);
   if (!filePath.isDirectory())
   {
      std::string filename;
      if (hasDefaultExtension)
         filename = filePath.getStem();
      else
         filename = filePath.getFilename();
      [panel setNameFieldStringValue:
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

   [panel setTitle: caption];
   [panel setDirectoryURL: path];
   
   return runFileDialog(panel);
}

QString GwtCallback::getExistingDirectory(const QString& qCaption,
                                          const QString& qLabel,
                                          const QString& qDir,
                                          bool /*focusOwner*/)
{
   return browseDirectory(qCaption, qLabel, qDir);
}

void GwtCallback::showWordDoc(QString qPath)
{
   if (qPath.isEmpty())
      return;
   NSString* path = qPath.toNSString();
   if (!showOfficeDoc(path, @"Microsoft Word", 
         [NSString stringWithUTF8String: s_openWordDocumentFormatString]))
   {
      showFile(qPath);
   };
}

void GwtCallback::showPptPresentation(QString qPath)
{
   if (qPath.isEmpty())
      return;
   NSString* path = qPath.toNSString();
   if (!showOfficeDoc(path, [NSString stringWithUTF8String: "Microsoft PowerPoint"], 
         [NSString stringWithUTF8String: s_openPptPresFormatString]))
   {
      showFile(qPath);
   }
}

void GwtCallback::changeTitleBarColor(int red, int green, int blue) {

   // attempts to mess with the window appearance (especially with
   // whether layer-backed views are used) cause rendering issues
   // with older versions of macOS, and also cause issues if the
   // requested appearance is incompatible with the current macOS
   // theme selected by the user, so make this a no-op for now.
   //
   // https://github.com/rstudio/rstudio/issues/4409
   // https://github.com/rstudio/rstudio/issues/4497

}


RS_END_NAMESPACE(desktop)
RS_END_NAMESPACE(rstudio)
