/*
 * RGraphicsUtils.hpp
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

#ifndef R_SESSION_GRAPHICS_UTILS_HPP
#define R_SESSION_GRAPHICS_UTILS_HPP

#include <string>

#include <boost/shared_ptr.hpp>

namespace rstudio {
namespace core {
   class Error;
   class ErrorLocation;
   class FilePath;
}
}

namespace rstudio {
namespace r {
namespace session {
namespace graphics {

void setCompatibleEngineVersion(int version);
bool validateRequirements(std::string* pMessage = nullptr);

class RestorePreviousGraphicsDeviceScope
{
public:
   RestorePreviousGraphicsDeviceScope();
   virtual ~RestorePreviousGraphicsDeviceScope();
   
private:
   struct Impl;
   boost::shared_ptr<Impl> pImpl_;
};

void reportError(const core::Error& error);

// Whether the grDevices bitmap devices (png(), jpeg(), ...) draw with Quartz,
// given the graphics backend option; always false off macOS.
bool usesQuartzBitmapDevice();

// The value of a TIFF's compression tag for a compression as named by
// grDevices::tiff()'s compression argument, as the ImageIO rewrite applies
// it; 0 for one it can't apply (ImageIO has no predictor option, so that
// includes the predictor variants).
int tiffCompressionTag(const std::string& compression);

// Finishes a bitmap written by R's Quartz devices on macOS, which record no
// resolution and ignore tiff()'s compression argument: records the resolution
// (in DPI, when not 0), and compresses a TIFF as that argument names (when
// not empty). A no-op when the file already matches.
core::Error finishQuartzImage(const core::FilePath& imagePath,
                              int dpi,
                              const std::string& tiffCompression);

// Encodes a JPEG from a PNG, at a quality from 1 to 100, recording the
// resolution (in DPI, when not 0). R's Quartz jpeg() device ignores its
// quality argument, and its output can't be re-encoded without losing more.
core::Error writeJpegFromPng(const core::FilePath& pngPath,
                             const core::FilePath& jpegPath,
                             int quality,
                             int dpi);

void logAndReportError(const core::Error& error,
                       const core::ErrorLocation& location);

} // namespace graphics
} // namespace session
} // namespace r
} // namespace rstudio


#endif // R_SESSION_GRAPHICS_UTILS_HPP 

