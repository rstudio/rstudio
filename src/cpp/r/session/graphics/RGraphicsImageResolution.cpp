/*
 * RGraphicsImageResolution.cpp
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

#include "RGraphicsUtils.hpp"

#include <cmath>
#include <cstring>
#include <fstream>

#include <boost/noncopyable.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <core/Log.hpp>

#ifdef __APPLE__
#include <ApplicationServices/ApplicationServices.h>
#endif

using namespace rstudio::core;

namespace rstudio {
namespace r {
namespace session {
namespace graphics {

#ifdef __APPLE__

namespace {

class CFRefScope : boost::noncopyable
{
public:
   explicit CFRefScope(CFTypeRef ref)
      : ref_(ref)
   {
   }

   ~CFRefScope()
   {
      if (ref_ != nullptr)
         ::CFRelease(ref_);
   }

private:
   CFTypeRef ref_;
};

CFURLRef createFileUrl(const FilePath& filePath)
{
   std::string path = filePath.getAbsolutePath();
   return ::CFURLCreateFromFileSystemRepresentation(
            kCFAllocatorDefault,
            reinterpret_cast<const UInt8*>(path.c_str()),
            static_cast<CFIndex>(path.length()),
            false);
}

double readResolution(CGImageSourceRef source)
{
   CFDictionaryRef properties = ::CGImageSourceCopyPropertiesAtIndex(source, 0, nullptr);
   if (properties == nullptr)
      return 0;
   CFRefScope propertiesScope(properties);

   double dpi = 0;
   CFNumberRef dpiNumber = static_cast<CFNumberRef>(
            ::CFDictionaryGetValue(properties, kCGImagePropertyDPIWidth));
   if (dpiNumber != nullptr)
      ::CFNumberGetValue(dpiNumber, kCFNumberDoubleType, &dpi);
   return dpi;
}

// Records the resolution in a JPEG's JFIF header, in dots per inch, when the
// file has one; *pPatched says whether it did. Quartz and ImageIO store an
// aspect ratio there (density unit 0), so applications that read only the JFIF
// header fall back to a default resolution. Patching the header in place
// avoids re-encoding the (lossy) image.
Error setJfifDensity(const FilePath& imagePath, int dpi, bool* pPatched)
{
   // SOI, then an APP0 segment: FF E0, length (2), "JFIF\0", version (2),
   // density unit (1), X density (2), Y density (2)
   const std::streamoff kUnitsOffset = 13;
   const char kJfifSignature[] = { '\xFF', '\xD8', '\xFF', '\xE0' };

   *pPatched = false;

   std::fstream stream(imagePath.getAbsolutePath(), std::ios::in | std::ios::out | std::ios::binary);
   if (!stream)
      return systemError(boost::system::errc::io_error, ERROR_LOCATION);

   char header[kUnitsOffset];
   if (!stream.read(header, sizeof(header)))
      return Success();

   bool isJfif =
         std::memcmp(header, kJfifSignature, sizeof(kJfifSignature)) == 0 &&
         std::memcmp(header + 6, "JFIF", 5) == 0;
   if (!isJfif)
      return Success();

   char highByte = static_cast<char>((dpi >> 8) & 0xFF);
   char lowByte = static_cast<char>(dpi & 0xFF);
   const char density[] = { 1, highByte, lowByte, highByte, lowByte };
   stream.seekp(kUnitsOffset);
   stream.write(density, sizeof(density));
   stream.flush();
   if (!stream)
      return systemError(boost::system::errc::io_error, ERROR_LOCATION);

   *pPatched = true;
   return Success();
}

Error imageIOError(const std::string& description, const FilePath& imagePath)
{
   Error error = systemError(boost::system::errc::io_error, description, ERROR_LOCATION);
   error.addProperty("path", imagePath);
   return error;
}

} // anonymous namespace

// R's Quartz bitmap devices don't record the resolution they drew at in the
// files they write (https://bugs.r-project.org/show_bug.cgi?id=19076), so
// applications such as Word insert those images at the wrong physical size.
// Record the resolution when it's missing or wrong; this is a no-op for the
// other devices, and for Quartz once R records it.
Error ensureImageResolution(const FilePath& imagePath, int dpi)
{
   CFURLRef url = createFileUrl(imagePath);
   if (url == nullptr)
      return imageIOError("Invalid image path", imagePath);
   CFRefScope urlScope(url);

   CGImageSourceRef source = ::CGImageSourceCreateWithURL(url, nullptr);
   if (source == nullptr)
      return imageIOError("Unable to read image", imagePath);
   CFRefScope sourceScope(source);

   if (std::fabs(readResolution(source) - dpi) < 0.5)
      return Success();

   // a JPEG with a JFIF header is patched in place
   bool patched = false;
   Error error = setJfifDensity(imagePath, dpi, &patched);
   if (error || patched)
      return error;

   // otherwise, write the image next to the original, then replace it
   FilePath tempPath = imagePath.getParent().completeChildPath(
            "." + imagePath.getFilename() + ".resolution");
   CFURLRef tempUrl = createFileUrl(tempPath);
   if (tempUrl == nullptr)
      return imageIOError("Invalid image path", tempPath);
   CFRefScope tempUrlScope(tempUrl);

   CGImageDestinationRef destination = ::CGImageDestinationCreateWithURL(
            tempUrl, ::CGImageSourceGetType(source), 1, nullptr);
   if (destination == nullptr)
      return imageIOError("Unable to write image", tempPath);
   CFRefScope destinationScope(destination);

   double dpiValue = dpi;
   double quality = 1.0;
   CFNumberRef dpiNumber = ::CFNumberCreate(kCFAllocatorDefault, kCFNumberDoubleType, &dpiValue);
   CFRefScope dpiNumberScope(dpiNumber);
   CFNumberRef qualityNumber = ::CFNumberCreate(kCFAllocatorDefault, kCFNumberDoubleType, &quality);
   CFRefScope qualityNumberScope(qualityNumber);

   // lossy formats (JPEG) are re-encoded, so keep their quality
   const void* keys[] = {
      kCGImagePropertyDPIWidth,
      kCGImagePropertyDPIHeight,
      kCGImageDestinationLossyCompressionQuality
   };
   const void* values[] = { dpiNumber, dpiNumber, qualityNumber };
   CFDictionaryRef options = ::CFDictionaryCreate(
            kCFAllocatorDefault,
            keys,
            values,
            3,
            &kCFTypeDictionaryKeyCallBacks,
            &kCFTypeDictionaryValueCallBacks);
   CFRefScope optionsScope(options);

   ::CGImageDestinationAddImageFromSource(destination, source, 0, options);
   if (!::CGImageDestinationFinalize(destination))
   {
      Error error = tempPath.removeIfExists();
      if (error)
         LOG_ERROR(error);
      return imageIOError("Unable to write image", tempPath);
   }

   // ImageIO writes a JPEG's resolution as an aspect ratio, as above
   error = setJfifDensity(tempPath, dpi, &patched);
   if (error)
      LOG_ERROR(error);

   error = tempPath.move(imagePath, FilePath::MoveDirect, true);
   if (error)
   {
      Error removeError = tempPath.removeIfExists();
      if (removeError)
         LOG_ERROR(removeError);
   }

   return error;
}

#else

Error ensureImageResolution(const FilePath& imagePath, int dpi)
{
   return Success();
}

#endif

} // namespace graphics
} // namespace session
} // namespace r
} // namespace rstudio
