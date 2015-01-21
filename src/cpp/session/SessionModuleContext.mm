/*
 * SessionModuleContext.mm
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#include <session/SessionModuleContext.hpp>

#include <boost/algorithm/string/predicate.hpp>

#include <core/system/Process.hpp>

#include <Foundation/NSString.h>
#include <Foundation/NSArray.h>
#include <Foundation/NSDictionary.h>
#include <Foundation/NSAutoreleasePool.h>

#include <AppKit/NSPasteboard.h>
#include <AppKit/NSImage.h>

using namespace rstudio;
using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace module_context {

bool isOSXMavericks()
{
   NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

   NSDictionary *systemVersionDictionary =
       [NSDictionary dictionaryWithContentsOfFile:
           @"/System/Library/CoreServices/SystemVersion.plist"];

   NSString *systemVersion =
       [systemVersionDictionary objectForKey:@"ProductVersion"];

   std::string version(
         [systemVersion cStringUsingEncoding:NSASCIIStringEncoding]);

   [pool release];

   return boost::algorithm::starts_with(version, "10.9") ||
          boost::algorithm::starts_with(version, "10.10");
}

bool hasOSXMavericksDeveloperTools()
{
   if (isOSXMavericks())
   {
      core::system::ProcessResult result;
      Error error = core::system::runCommand("xcode-select -p",
                                             core::system::ProcessOptions(),
                                             &result);
      if (!error && (result.exitStatus == EXIT_SUCCESS))
         return true;
      else
         return false;
   }
   else
   {
      return false;
   }
}

Error copyImageToCocoaPasteboard(const FilePath& imagePath)
{
   NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

   NSString* path = [NSString stringWithUTF8String:
                     imagePath.absolutePath().c_str()];
   
   NSImage* image = [[NSImage alloc] initWithContentsOfFile: path];
   
   NSPasteboard *pboard = [NSPasteboard generalPasteboard];
   [pboard clearContents];
   
   NSArray *copiedObjects = [NSArray arrayWithObject:image];
   [pboard writeObjects: copiedObjects];
   
   [image release];
   
   [pool release];

   return Success();
}

} // namespace module_context
} // namespace session
} // namespace rstudio
