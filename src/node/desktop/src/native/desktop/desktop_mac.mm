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

Napi::Value macOSDefaultApplicationForFileExtension(const Napi::CallbackInfo& info)
{
   Napi::Env env = info.Env();
   std::string extension = info[0].As<Napi::String>().Utf8Value();

   std::string bundleIdentifier;
   bool isCurrentApplication = false;

   @autoreleasepool
   {
      UTType* type = [UTType typeWithFilenameExtension:toNSString(extension)];
      NSURL* url = type ? [[NSWorkspace sharedWorkspace] URLForApplicationToOpenContentType:type] : nil;
      NSBundle* bundle = url ? [NSBundle bundleWithURL:url] : nil;

      // any copy of this application counts, e.g. an older RStudio alongside this one
      bundleIdentifier = toStdString([bundle bundleIdentifier]);
      isCurrentApplication = bundle != nil && [[bundle bundleIdentifier] isEqualToString:[[NSBundle mainBundle] bundleIdentifier]];
   }

   Napi::Object result = Napi::Object::New(env);
   result.Set("bundleIdentifier", Napi::String::New(env, bundleIdentifier));
   result.Set("isCurrentApplication", Napi::Boolean::New(env, isCurrentApplication));
   return result;
}

Napi::Value macOSSetDefaultApplicationForFileExtension(const Napi::CallbackInfo& info)
{
   Napi::Env env = info.Env();
   std::string extension = info[0].As<Napi::String>().Utf8Value();
   Napi::Promise::Deferred deferred = Napi::Promise::Deferred::New(env);

   @autoreleasepool
   {
      UTType* type = [UTType typeWithFilenameExtension:toNSString(extension)];
      if (type == nil)
      {
         deferred.Reject(Napi::Error::New(env, "No file type is registered for extension '" + extension + "'").Value());
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
         SetDefaultResult* result = new SetDefaultResult{deferred, toStdString([error localizedDescription])};
         tsfn.BlockingCall(result, settleSetDefault);
         tsfn.Release();
      }];
   }

   return deferred.Promise();
}

} // end namespace desktop
} // end namespace rstudio
