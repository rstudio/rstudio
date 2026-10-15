/*
 * RGraphicsQuartzImage.cpp
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
#include <string>

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

// R's Quartz bitmap devices write their files through ImageIO with no
// properties: they don't record the resolution they drew at
// (https://bugs.r-project.org/show_bug.cgi?id=19076), so applications such as
// Word insert those images at the wrong physical size, and they ignore the
// compression and quality arguments of tiff() and jpeg(). The functions here
// finish those files through ImageIO. They are no-ops for the other devices.

int tiffCompressionTag(const std::string& compression)
{
   if (compression == "none")
      return 1;
   else if (compression == "lzw" || compression == "lzw+p")
      return 5;
   else if (compression == "zip" || compression == "zip+p")
      return 8;
   else
      return 0;
}

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

CFNumberRef createNumber(double value)
{
   return ::CFNumberCreate(kCFAllocatorDefault, kCFNumberDoubleType, &value);
}

CFNumberRef createNumber(int value)
{
   return ::CFNumberCreate(kCFAllocatorDefault, kCFNumberIntType, &value);
}

Error imageIOError(const std::string& description, const FilePath& imagePath)
{
   Error error = systemError(boost::system::errc::io_error, description, ERROR_LOCATION);
   error.addProperty("path", imagePath);
   return error;
}

Error openImage(const FilePath& imagePath, CGImageSourceRef* pSource)
{
   CFURLRef url = createFileUrl(imagePath);
   if (url == nullptr)
      return imageIOError("Invalid image path", imagePath);
   CFRefScope urlScope(url);

   *pSource = ::CGImageSourceCreateWithURL(url, nullptr);
   if (*pSource == nullptr)
      return imageIOError("Unable to read image", imagePath);

   return Success();
}

double readNumber(CFDictionaryRef properties, CFStringRef key)
{
   double value = 0;
   CFNumberRef number = static_cast<CFNumberRef>(::CFDictionaryGetValue(properties, key));
   if (number != nullptr)
      ::CFNumberGetValue(number, kCFNumberDoubleType, &value);
   return value;
}

// Reads the resolution (in DPI) and the compression tag recorded in an image;
// each 0 when there is none.
void readImageProperties(CGImageSourceRef source, double* pDpi, int* pTiffCompression)
{
   *pDpi = 0;
   *pTiffCompression = 0;

   CFDictionaryRef properties = ::CGImageSourceCopyPropertiesAtIndex(source, 0, nullptr);
   if (properties == nullptr)
      return;
   CFRefScope propertiesScope(properties);

   *pDpi = readNumber(properties, kCGImagePropertyDPIWidth);

   CFDictionaryRef tiff = static_cast<CFDictionaryRef>(
            ::CFDictionaryGetValue(properties, kCGImagePropertyTIFFDictionary));
   if (tiff != nullptr)
      *pTiffCompression = static_cast<int>(readNumber(tiff, kCGImagePropertyTIFFCompression));
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

// Writes the first image of a source to a file of a type (a UTI) through
// ImageIO, recording the resolution (when dpi is not 0), compressing a TIFF
// (when tiffCompression, a compression tag value, is not 0), and encoding a
// lossy format at a quality from 0 to 1.
Error writeImage(CGImageSourceRef source,
                 const FilePath& targetPath,
                 CFStringRef type,
                 int dpi,
                 int tiffCompression,
                 double quality)
{
   CFURLRef url = createFileUrl(targetPath);
   if (url == nullptr)
      return imageIOError("Invalid image path", targetPath);
   CFRefScope urlScope(url);

   CGImageDestinationRef destination = ::CGImageDestinationCreateWithURL(url, type, 1, nullptr);
   if (destination == nullptr)
      return imageIOError("Unable to write image", targetPath);
   CFRefScope destinationScope(destination);

   CFMutableDictionaryRef options = ::CFDictionaryCreateMutable(
            kCFAllocatorDefault,
            0,
            &kCFTypeDictionaryKeyCallBacks,
            &kCFTypeDictionaryValueCallBacks);
   CFRefScope optionsScope(options);

   if (dpi > 0)
   {
      CFNumberRef dpiNumber = createNumber(static_cast<double>(dpi));
      CFRefScope dpiNumberScope(dpiNumber);
      ::CFDictionarySetValue(options, kCGImagePropertyDPIWidth, dpiNumber);
      ::CFDictionarySetValue(options, kCGImagePropertyDPIHeight, dpiNumber);
   }

   CFNumberRef qualityNumber = createNumber(quality);
   CFRefScope qualityNumberScope(qualityNumber);
   ::CFDictionarySetValue(options, kCGImageDestinationLossyCompressionQuality, qualityNumber);

   if (tiffCompression > 0)
   {
      CFNumberRef compressionNumber = createNumber(tiffCompression);
      CFRefScope compressionNumberScope(compressionNumber);
      CFMutableDictionaryRef tiff = ::CFDictionaryCreateMutable(
               kCFAllocatorDefault,
               0,
               &kCFTypeDictionaryKeyCallBacks,
               &kCFTypeDictionaryValueCallBacks);
      CFRefScope tiffScope(tiff);
      ::CFDictionarySetValue(tiff, kCGImagePropertyTIFFCompression, compressionNumber);
      ::CFDictionarySetValue(options, kCGImagePropertyTIFFDictionary, tiff);
   }

   ::CGImageDestinationAddImageFromSource(destination, source, 0, options);
   if (!::CGImageDestinationFinalize(destination))
   {
      Error error = targetPath.removeIfExists();
      if (error)
         LOG_ERROR(error);
      return imageIOError("Unable to write image", targetPath);
   }

   return Success();
}

// Writes an image again next to the original, then replaces it.
Error rewriteImage(CGImageSourceRef source,
                   const FilePath& imagePath,
                   int dpi,
                   int tiffCompression)
{
   FilePath tempPath = imagePath.getParent().completeChildPath(
            "." + imagePath.getFilename() + ".rewrite");

   // lossy formats (JPEG) are re-encoded, so keep their quality
   Error error = writeImage(source, tempPath, ::CGImageSourceGetType(source), dpi, tiffCompression, 1.0);
   if (error)
      return error;

   // ImageIO writes a JPEG's resolution as an aspect ratio, as above
   if (dpi > 0)
   {
      bool patched = false;
      error = setJfifDensity(tempPath, dpi, &patched);
      if (error)
         LOG_ERROR(error);
   }

   error = tempPath.move(imagePath, FilePath::MoveDirect, true);
   if (error)
   {
      Error removeError = tempPath.removeIfExists();
      if (removeError)
         LOG_ERROR(removeError);
   }

   return error;
}

} // anonymous namespace

Error finishQuartzImage(const FilePath& imagePath,
                        int dpi,
                        const std::string& tiffCompression)
{
   CGImageSourceRef source = nullptr;
   Error error = openImage(imagePath, &source);
   if (error)
      return error;
   CFRefScope sourceScope(source);

   double fileDpi = 0;
   int fileCompression = 0;
   readImageProperties(source, &fileDpi, &fileCompression);

   bool recordResolution = dpi > 0 && std::fabs(fileDpi - dpi) >= 0.5;
   int compression = tiffCompressionTag(tiffCompression);
   bool compress = compression > 0 && compression != fileCompression;
   if (!recordResolution && !compress)
      return Success();

   // a JPEG with a JFIF header is patched in place
   if (!compress)
   {
      bool patched = false;
      error = setJfifDensity(imagePath, dpi, &patched);
      if (error || patched)
         return error;
   }

   return rewriteImage(source, imagePath, dpi, compression);
}

Error writeJpegFromPng(const FilePath& pngPath,
                       const FilePath& jpegPath,
                       int quality,
                       int dpi)
{
   CGImageSourceRef source = nullptr;
   Error error = openImage(pngPath, &source);
   if (error)
      return error;
   CFRefScope sourceScope(source);

   error = writeImage(source, jpegPath, CFSTR("public.jpeg"), dpi, 0, quality / 100.0);
   if (error)
      return error;

   // ImageIO writes the resolution as an aspect ratio (see setJfifDensity)
   if (dpi > 0)
   {
      bool patched = false;
      error = setJfifDensity(jpegPath, dpi, &patched);
      if (error)
         LOG_ERROR(error);
   }

   return Success();
}

#else

Error finishQuartzImage(const FilePath& imagePath,
                        int dpi,
                        const std::string& tiffCompression)
{
   return Success();
}

Error writeJpegFromPng(const FilePath& pngPath,
                       const FilePath& jpegPath,
                       int quality,
                       int dpi)
{
   return systemError(boost::system::errc::not_supported, ERROR_LOCATION);
}

#endif

} // namespace graphics
} // namespace session
} // namespace r
} // namespace rstudio
