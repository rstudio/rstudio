/*
 * desktop_mac.mm
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

// NOTE: Whenever adding new methods to be exported here, be sure to update
// 'desktop.node.d.ts', to ensure that the JavaScript type definitions are
// visible for usage on the Electron side.

#include "desktop_mac.h"

#import <Cocoa/Cocoa.h>
#import <UniformTypeIdentifiers/UniformTypeIdentifiers.h>

#include <string>

namespace rstudio {
namespace desktop {

namespace {

NSString* toNSString(const std::string& value)
{
   return [NSString stringWithUTF8String:value.c_str()];
}

std::string toStdString(NSString* value)
{
   return value == nil ? std::string() : std::string([value UTF8String]);
}

// Declining the confirmation completes with an error: NSCocoaErrorDomain 256
// wrapping userCanceledErr (-128). The user answered, so it is not a failure.
bool isUserCancelled(NSError* error)
{
   for (NSError* current = error; current != nil; current = current.userInfo[NSUnderlyingErrorKey])
   {
      if ([current.domain isEqualToString:NSOSStatusErrorDomain] && current.code == userCanceledErr)
         return true;
      if ([current.domain isEqualToString:NSCocoaErrorDomain] && current.code == NSUserCancelledError)
         return true;
   }
   return false;
}

// The outcome of a set-default request, carried from the completion handler
// (which may run on any thread) back to the JavaScript thread.
struct SetDefaultResult
{
   Napi::Promise::Deferred deferred;
   std::string error;
};

void settleSetDefault(Napi::Env env, Napi::Function, SetDefaultResult* result)
{
   if (result->error.empty())
      result->deferred.Resolve(env.Undefined());
   else
      result->deferred.Reject(Napi::Error::New(env, result->error).Value());

   delete result;
}

} // end anonymous namespace

Napi::Value macOSIsDefaultApplicationForFileExtension(const Napi::CallbackInfo& info)
{
   Napi::Env env = info.Env();
   std::string extension = info[0].As<Napi::String>().Utf8Value();

   bool isCurrentApplication = false;

   @autoreleasepool
   {
      UTType* type = [UTType typeWithFilenameExtension:toNSString(extension)];
      NSURL* url = type ? [[NSWorkspace sharedWorkspace] URLForApplicationToOpenContentType:type] : nil;
      NSBundle* bundle = url ? [NSBundle bundleWithURL:url] : nil;

      // any copy of this application counts, e.g. an older RStudio alongside this one
      isCurrentApplication = bundle != nil && [[bundle bundleIdentifier] isEqualToString:[[NSBundle mainBundle] bundleIdentifier]];
   }

   return Napi::Boolean::New(env, isCurrentApplication);
}

Napi::Value macOSSetDefaultApplicationForFileExtension(const Napi::CallbackInfo& info)
{
   Napi::Env env = info.Env();
   std::string extension = info[0].As<Napi::String>().Utf8Value();
   Napi::Promise::Deferred deferred = Napi::Promise::Deferred::New(env);

   @autoreleasepool
   {
      // an extension no application declares still gets a dynamic (dyn.*) type, as
      // do most of RStudio's own, which it claims by extension; nil means the
      // extension itself is invalid, e.g. empty
      UTType* type = [UTType typeWithFilenameExtension:toNSString(extension)];
      if (type == nil)
      {
         deferred.Reject(Napi::Error::New(env, "'" + extension + "' is not a valid file extension").Value());
         return deferred.Promise();
      }

      // macOS may show a confirmation dialog and reports the outcome asynchronously;
      // the thread-safe function keeps the event loop alive until it does
      Napi::ThreadSafeFunction tsfn = Napi::ThreadSafeFunction::New(
               env,
               Napi::Function::New(env, [](const Napi::CallbackInfo&) {}),
               "macOSSetDefaultApplicationForFileExtension",
               0,
               1);

      NSURL* applicationUrl = [[NSBundle mainBundle] bundleURL];
      [[NSWorkspace sharedWorkspace] setDefaultApplicationAtURL:applicationUrl
                                             toOpenContentType:type
                                             completionHandler:^(NSError* error) {
         std::string message = isUserCancelled(error) ? std::string() : toStdString([error localizedDescription]);
         SetDefaultResult* result = new SetDefaultResult{deferred, message};
         tsfn.BlockingCall(result, settleSetDefault);
         tsfn.Release();
      }];
   }

   return deferred.Promise();
}

} // end namespace desktop
} // end namespace rstudio
