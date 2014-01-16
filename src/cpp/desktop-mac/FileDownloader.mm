/*
 * FileDownloader.mm
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

#import "FileDownloader.h"

#include <core/Error.hpp>
#include <core/FilePath.hpp>

#include "Utils.hpp"


@interface FileDownloader : NSObject<NSURLDownloadDelegate> {
   NSString* targetPath_;
}

@end

@implementation FileDownloader

- (id) init: (NSURLRequest*) request
{
   if (self = [super init])
   {
      // create a temp directory
      core::FilePath tempPath;
      core::Error error = core::FilePath::tempFilePath(&tempPath);
      if (error)
      {
         LOG_ERROR(error);
         tempPath = core::FilePath("/tmp");
      }
      error = tempPath.ensureDirectory();
      if (error)
      {
         LOG_ERROR(error);
         tempPath = core::FilePath("/tmp");
      }
      NSString* tempDir = [NSString stringWithUTF8String:
                                    tempPath.absolutePath().c_str()];
      
      // initialize the download and set it's path
      NSURLDownload *download = [[NSURLDownload alloc] initWithRequest: request
                                                              delegate: self];
      if (download)
      {
         NSString* filename = [[request URL] lastPathComponent];
         targetPath_ = [[tempDir stringByAppendingPathComponent: filename] retain];
         
         [download setDestination: targetPath_ allowOverwrite: YES];
      }
      else
      {
         NSString* message = [NSString stringWithFormat:
                              @"Unable to Download %@",
                              [[request URL] absoluteURL]];
         
         desktop::utils::showMessageBox(NSCriticalAlertStyle,
                                        @"File Download Failed",
                                        message);
      }
   }
   
   return self;
}

- (void) dealloc
{
   [targetPath_ release];
   [super dealloc];
}
 
- (void)download:(NSURLDownload *) download didFailWithError: (NSError *) error
{
   NSString* message = [NSString stringWithFormat: @"Error - %@ %@",
                        [error localizedDescription],
                        [[error userInfo] objectForKey:NSURLErrorFailingURLStringErrorKey]];
   
   desktop::utils::showMessageBox(NSCriticalAlertStyle,
                                  @"File Download Failed",
                                  message);
   [download release];
   [self release];
}

- (void)downloadDidFinish: (NSURLDownload *) download
{
   if ([targetPath_ hasSuffix: @".pdf"])
   {
      [[NSWorkspace sharedWorkspace] openFile: targetPath_
                              withApplication: @"Preview"];
   }
   else
   {
      [[NSWorkspace sharedWorkspace] openFile: targetPath_];
   }
   
   [download release];
   [self release];
}

@end


namespace desktop {

void downloadAndShowFile(NSURLRequest* request)
{
   [[FileDownloader alloc] init: request]; // self-freeing
}

} // namespace desktop


