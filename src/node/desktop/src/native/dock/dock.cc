/*
 * dock.cc
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

#include <napi.h>
#include <string>

#define RS_EXPORT_FUNCTION(__NAME__, __FUNCTION__) \
  exports.Set(                                     \
    Napi::String::New(env, __NAME__),              \
    Napi::Function::New(env, __FUNCTION__)         \
  )

namespace rstudio {
namespace dock {
Napi::Value setDockLabel(const Napi::CallbackInfo& info)
{
   // currently only implemented for macOS in dock_mac.mm
   return Napi::Value();
}

} // end namespace dock
} // end namespace rstudio

Napi::Object Init(Napi::Env env, Napi::Object exports) {

  RS_EXPORT_FUNCTION("setDockLabel", rstudio::dock::setDockLabel);

  return exports;

}

NODE_API_MODULE(rstudio, Init)
