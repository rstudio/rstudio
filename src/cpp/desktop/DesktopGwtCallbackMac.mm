/*
 * DesktopGwtCallbackMac.mm
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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
#include "DesktopUtils.hpp"

#import <AppKit/NSApplication.h>
#import <AppKit/NSAlert.h>
#import <AppKit/NSOpenPanel.h>
#import <AppKit/NSSavePanel.h>
#import <AppKit/NSButton.h>
#import <AppKit/NSImage.h>

#include <core/FilePath.hpp>
#include <core/Macros.hpp>

using namespace rstudio;
using namespace rstudio::core;
using namespace rstudio::desktop;

RS_BEGIN_NAMESPACE(rstudio)
RS_BEGIN_NAMESPACE(desktop)

RS_BEGIN_NAMESPACE()

static const char* const s_openWordDocumentFormatString = 1 + R"EOF(
tell application "Microsoft Word"
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

static const char* const s_openPptPresFormatString = 1 + R"EOF(
tell application "Microsoft PowerPoint"
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

FilePath userHomePath()
{
   return core::system::userHomePath("R_USER|HOME");
}

NSString* createAliasedPath(NSString* path)
{
   if (path == nil || [path length] == 0)
      return @"";
   
   std::string aliased = FilePath::createAliasedPath(
      FilePath([path UTF8String]),
      userHomePath());
   
   return [NSString stringWithUTF8String: aliased.c_str()];
}

NSString* resolveAliasedPath(NSString* path)
{
   if (path == nil)
      path = @"";
   
   FilePath resolved = FilePath::resolveAliasedPath(
      [path UTF8String],
      userHomePath());
   
   return [NSString stringWithUTF8String: resolved.absolutePath().c_str()];
}

QString runFileDialog(NSSavePanel* panel)
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
   
   return QString::fromNSString(createAliasedPath(path));
}

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
      OSStatus status = LSOpenURLsWithRole(docArr, kLSRolesViewer, NULL, NULL, NULL, 0);
      if (status != noErr)
      {
         return false;
      }
   }

   return true;
}

RS_END_NAMESPACE()

void GwtCallback::initialize()
{
   [NSEvent addLocalMonitorForEventsMatchingMask: NSKeyDownMask
                                         handler: ^(NSEvent* event)
    {
       // detect attempts to run Command + Shift + /, and let our own
       // reflow comment code run instead
       if (event.keyCode == 44 &&
           (event.modifierFlags & NSEventModifierFlagShift) != 0 &&
           (event.modifierFlags & NSEventModifierFlagCommand) != 0 &&
           (event.modifierFlags & NSEventModifierFlagControl) == 0 &&
           (event.modifierFlags & NSEventModifierFlagOption) == 0 &&
           (event.modifierFlags & NSEventModifierFlagFunction) == 0)
       {
          invokeReflowComment();
          return (NSEvent*) nil;
       }
       
       return event;
    }];

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
   // If there's only one button, make sure Enter is the button used to
   // dismiss the dialog. If there's multiple dialogs, accommodate the
   // case where the default button may be the cancel button.
   if ([dialogButtons count] == 1)
   {
      [[[alert buttons] objectAtIndex: defaultButton] setKeyEquivalent: @"\r"];
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
         filename = filePath.stem();
      else
         filename = filePath.filename();
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
                                          bool focusOwner)
{
   NSString* caption = qCaption.toNSString();
   NSString* label = qLabel.toNSString();
   NSString* dir = qDir.toNSString();
   
   dir = resolveAliasedPath(dir);
   
   NSOpenPanel* panel = [NSOpenPanel openPanel];
   [panel setTitle: caption];
   [panel setPrompt: label];
   [panel setDirectoryURL: [NSURL fileURLWithPath:
                           [dir stringByStandardizingPath]]];
   [panel setCanChooseFiles: false];
   [panel setCanChooseDirectories: true];
   [panel setCanCreateDirectories: true];
   
   return runFileDialog(panel);
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


RS_END_NAMESPACE(desktop)
RS_END_NAMESPACE(rstudio)

