/*
 * desktop_mac.h
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

// macOS entry points implemented in Objective-C++ (desktop_mac.mm) and
// exported from desktop.cc, which provides stubs on other platforms.

#ifndef RSTUDIO_DESKTOP_MAC_H
#define RSTUDIO_DESKTOP_MAC_H

#include <napi.h>

namespace rstudio {
namespace desktop {

Napi::Value macOSIsDefaultApplicationForFileExtension(const Napi::CallbackInfo& info);
Napi::Value macOSSetDefaultApplicationForFileExtension(const Napi::CallbackInfo& info);

} // end namespace desktop
} // end namespace rstudio

#endif // RSTUDIO_DESKTOP_MAC_H
