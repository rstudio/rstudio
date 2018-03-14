/*
 * GwtCallbacks.mm
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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
#include "DesktopUtils.hpp"

#import <AppKit/NSApplication.h>
#import <AppKit/NSAlert.h>
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

enum MessageType
{
   MSG_POPUP_BLOCKED = 0,
   MSG_INFO = 1,
   MSG_WARNING = 2,
   MSG_ERROR = 3,
   MSG_QUESTION = 4
};

RS_END_NAMESPACE()

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

   [alert beginSheetModalForWindow: [[NSApplication sharedApplication] mainWindow] completionHandler: ^(NSModalResponse response) {
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

RS_END_NAMESPACE(desktop)
RS_END_NAMESPACE(rstudio)

