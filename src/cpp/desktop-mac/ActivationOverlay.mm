/*
 * ActivationOverlay.mm
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

#import "ActivationOverlay.h"

using namespace rstudio;
using namespace rstudio::core;

@implementation Activation

+ (id) sharedActivation
{
   static Activation* sharedActivation = nil;
   @synchronized(self)
   {
      if (sharedActivation == nil)
      {
         sharedActivation = [[self alloc] init];
      }
   }
   return sharedActivation;
}

- (BOOL) allowProductUsage
{
   return TRUE;
}

- (std::string) currentLicenseStateMessage
{
   return std::string();
}

- (std::string) licenseStatus
{
   return std::string();
}

- (BOOL) getInitialLicenseWithArguments : (NSArray*) arguments
                                   path : (rstudio::core::FilePath&) installPath
                                devMode : (BOOL) devMode;
{
   return TRUE;
}

- (void) showLicenseDialog
{
}

@end
