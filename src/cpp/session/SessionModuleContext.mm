/*
 * SessionModuleContext.mm
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
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
#include <Foundation/NSError.h>
#include <Foundation/NSURL.h>
#include <Foundation/NSValue.h>

#include <AppKit/NSPasteboard.h>
#include <AppKit/NSImage.h>

using namespace rstudio;
using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace module_context {

Error copyImageToCocoaPasteboard(const FilePath& imagePath)
{
   NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

   NSString* path = [NSString stringWithUTF8String:
                     imagePath.getAbsolutePath().c_str()];
   
   NSImage* image = [[NSImage alloc] initWithContentsOfFile: path];
   
   NSPasteboard *pboard = [NSPasteboard generalPasteboard];
   [pboard clearContents];
   
   NSArray *copiedObjects = [NSArray arrayWithObject:image];
   [pboard writeObjects: copiedObjects];
   
   [image release];
   
   [pool release];

   return Success();
}

bool isFinderAlias(const FilePath& filePath)
{
   @autoreleasepool
   {
      NSString* path = [NSString stringWithUTF8String:
                        filePath.getAbsolutePath().c_str()];
      if (path == nil)
         return false;

      NSURL* url = [NSURL fileURLWithPath: path];

      NSNumber* isAlias = nil;
      if (![url getResourceValue: &isAlias
                          forKey: NSURLIsAliasFileKey
                           error: nil] || ![isAlias boolValue])
      {
         return false;
      }

      // NSURLIsAliasFileKey is also true for symlinks; the filesystem
      // already follows those, so only report bookmark-file aliases
      NSNumber* isSymlink = nil;
      if ([url getResourceValue: &isSymlink
                         forKey: NSURLIsSymbolicLinkKey
                          error: nil] && [isSymlink boolValue])
      {
         return false;
      }

      return true;
   }
}

Error resolveFinderAlias(const FilePath& aliasPath, FilePath* pTargetPath)
{
   @autoreleasepool
   {
      NSString* path = [NSString stringWithUTF8String:
                        aliasPath.getAbsolutePath().c_str()];
      if (path == nil)
         return systemError(boost::system::errc::invalid_argument, ERROR_LOCATION);

      NSError* nsError = nil;
      NSURL* resolved = [NSURL URLByResolvingAliasFileAtURL: [NSURL fileURLWithPath: path]
                                                    options: (NSURLBookmarkResolutionWithoutUI |
                                                              NSURLBookmarkResolutionWithoutMounting)
                                                      error: &nsError];
      if (resolved == nil || ![resolved isFileURL])
      {
         Error error = systemError(boost::system::errc::no_such_file_or_directory, ERROR_LOCATION);
         error.addProperty("alias-path", aliasPath);
         if (nsError != nil)
            error.addProperty("description", [[nsError localizedDescription] UTF8String]);
         return error;
      }

      *pTargetPath = FilePath([[resolved path] UTF8String]);
      return Success();
   }
}

} // namespace module_context
} // namespace session
} // namespace rstudio
