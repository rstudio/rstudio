/*
 * ScriptCommand.mm
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

#import <Foundation/Foundation.h>
#import "ScriptCommand.h"
#include "DesktopMainWindow.hpp"
#include <boost/algorithm/string/replace.hpp>


std::string jsEscape(std::string str)
{
   boost::algorithm::replace_all(str, "\\", "\\\\");
   boost::algorithm::replace_all(str, "\"", "\\\"");
   boost::algorithm::replace_all(str, "\n", "\\n");
   return str;
}


// evaluate R command
void evaluateRCommand(std::string rCmd)
{
   rCmd = jsEscape(rCmd);
   std::string js = "window.desktopHooks.evaluateRCmd(\"" + rCmd + "\")";
   rstudio::desktop::MainWindow::getInstance()->webPage()->runJavaScript(QString::fromUtf8(js.c_str()));
}


@implementation evaluateRScriptCommand



-(id) performDefaultImplementation
{
   NSString *script = [self directParameter];
   if (!script || [script isEqualToString:@""])
      return [NSNumber numberWithBool:NO];

   evaluateRCommand([script UTF8String]);

   return [NSNumber numberWithBool:YES];
}

@end
