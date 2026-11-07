/*
 * desktop.cc
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

// NOTE: Whenever adding new methods to be exported here, be sure to update
// 'desktop.node.d.ts', to ensure that the JavaScript type definitions are
// visible for usage on the Electron side.

#include <napi.h>

#include <algorithm>
#include <chrono>
#include <iomanip>
#include <iostream>
#include <set>
#include <sstream>
#include <string>
#include <vector>

#ifdef __APPLE__
# include <CoreFoundation/CoreFoundation.h>
# include <Carbon/Carbon.h>
# include <CoreText/CoreText.h>
#endif

#ifdef _WIN32
# include <Windows.h>
# include <shlobj.h>
#endif

bool s_loggingEnabled = false;

namespace {

std::string timestamp()
{
  auto now = std::chrono::system_clock::now();
  auto itt = std::chrono::system_clock::to_time_t(now);

  std::ostringstream ss;
  ss << std::put_time(gmtime(&itt), "%FT%TZ");
  return ss.str();

}

[[maybe_unused]] void logDebug(const std::string& message)
{
   if (s_loggingEnabled && message.length())
   {
      std::cerr << timestamp() << " DEBUG " << message;
      if (message[message.length() - 1] != '\n')
         std::cerr << std::endl;
   }
}


// Convert between wide strings and UTF-8 on every platform. wchar_t holds
// UTF-16 on Windows and UTF-32 elsewhere, and both are handled here so the
// callers need no platform-specific code. Malformed input becomes U+FFFD.
void appendUtf8(char32_t codePoint, std::string* pResult)
{
   if (codePoint > 0x10FFFF || (codePoint >= 0xD800 && codePoint <= 0xDFFF))
      codePoint = 0xFFFD;

   if (codePoint < 0x80)
   {
      pResult->push_back(static_cast<char>(codePoint));
   }
   else if (codePoint < 0x800)
   {
      pResult->push_back(static_cast<char>(0xC0 | (codePoint >> 6)));
      pResult->push_back(static_cast<char>(0x80 | (codePoint & 0x3F)));
   }
   else if (codePoint < 0x10000)
   {
      pResult->push_back(static_cast<char>(0xE0 | (codePoint >> 12)));
      pResult->push_back(static_cast<char>(0x80 | ((codePoint >> 6) & 0x3F)));
      pResult->push_back(static_cast<char>(0x80 | (codePoint & 0x3F)));
   }
   else
   {
      pResult->push_back(static_cast<char>(0xF0 | (codePoint >> 18)));
      pResult->push_back(static_cast<char>(0x80 | ((codePoint >> 12) & 0x3F)));
      pResult->push_back(static_cast<char>(0x80 | ((codePoint >> 6) & 0x3F)));
      pResult->push_back(static_cast<char>(0x80 | (codePoint & 0x3F)));
   }
}

std::string wideToUtf8(const std::wstring& value)
{
   std::string result;
   result.reserve(value.size());

   for (size_t i = 0, n = value.size(); i < n; i++)
   {
      char32_t codePoint = static_cast<char32_t>(value[i]);

      // UTF-16: combine a surrogate pair into one code point
      if (sizeof(wchar_t) == 2 && codePoint >= 0xD800 && codePoint <= 0xDBFF && i + 1 < n)
      {
         char32_t low = static_cast<char32_t>(value[i + 1]);
         if (low >= 0xDC00 && low <= 0xDFFF)
         {
            codePoint = 0x10000 + ((codePoint - 0xD800) << 10) + (low - 0xDC00);
            i++;
         }
      }

      appendUtf8(codePoint, &result);
   }

   return result;
}

std::wstring utf8ToWide(const std::string& value)
{
   std::wstring result;
   result.reserve(value.size());

   for (size_t i = 0, n = value.size(); i < n; )
   {
      unsigned char lead = static_cast<unsigned char>(value[i]);
      size_t length;
      char32_t codePoint;
      if (lead < 0x80)
      {
         length = 1;
         codePoint = lead;
      }
      else if ((lead & 0xE0) == 0xC0)
      {
         length = 2;
         codePoint = lead & 0x1F;
      }
      else if ((lead & 0xF0) == 0xE0)
      {
         length = 3;
         codePoint = lead & 0x0F;
      }
      else if ((lead & 0xF8) == 0xF0)
      {
         length = 4;
         codePoint = lead & 0x07;
      }
      else
      {
         length = 1;
         codePoint = 0xFFFD;
      }

      bool valid = i + length <= n;
      for (size_t j = 1; valid && j < length; j++)
      {
         unsigned char trail = static_cast<unsigned char>(value[i + j]);
         if ((trail & 0xC0) != 0x80)
            valid = false;
         else
            codePoint = (codePoint << 6) | (trail & 0x3F);
      }

      if (!valid)
      {
         // consume only the lead byte so the bytes after it are decoded on their own
         length = 1;
         codePoint = 0xFFFD;
      }
      else if (codePoint > 0x10FFFF ||
               (codePoint >= 0xD800 && codePoint <= 0xDFFF) ||
               (length == 2 && codePoint < 0x80) ||
               (length == 3 && codePoint < 0x800) ||
               (length == 4 && codePoint < 0x10000))
      {
         // out of range, encoded surrogate, or overlong
         codePoint = 0xFFFD;
      }

      i += length;

      // UTF-16: split supplementary code points into a surrogate pair
      if (sizeof(wchar_t) == 2 && codePoint >= 0x10000)
      {
         codePoint -= 0x10000;
         result.push_back(static_cast<wchar_t>(0xD800 + (codePoint >> 10)));
         result.push_back(static_cast<wchar_t>(0xDC00 + (codePoint & 0x3FF)));
      }
      else
      {
         result.push_back(static_cast<wchar_t>(codePoint));
      }
   }

   return result;
}

} // end anonymous namespace

#define DLOG(__X__)            \
   do                          \
   {                           \
      if (s_loggingEnabled)    \
      {                        \
         std::stringstream ss; \
         ss << __X__;          \
         logDebug(ss.str());   \
      }                        \
   } while (0)

#define RS_EXPORT_FUNCTION(__NAME__, __FUNCTION__) \
  exports.Set(                                     \
    Napi::String::New(env, __NAME__),              \
    Napi::Function::New(env, __FUNCTION__)         \
  )


#ifdef _WIN32

namespace {

std::string registryKeyToString(HKEY hKey)
{
   if (hKey == HKEY_CLASSES_ROOT)
   {
      return "HKEY_CLASSES_ROOT";
   }
   else if (hKey == HKEY_CURRENT_USER)
   {
      return "HKEY_CURRENT_USER";
   }
   else if (hKey == HKEY_LOCAL_MACHINE)
   {
      return "HKEY_LOCAL_MACHINE";
   }
   else if (hKey == HKEY_USERS)
   {
      return "HKEY_USERS";
   }
   else if (hKey == HKEY_CURRENT_CONFIG)
   {
      return "HKEY_CURRENT_CONFIG";
   }
   else
   {
      std::stringstream ss;
      ss << "<" << hKey << ">";
      return ss.str();
   }
}

std::string getErrorMessage(DWORD error)
{
   LPVOID lpMsgBuf;
   DWORD length = ::FormatMessage(
      FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS,
      nullptr,
      error,
      MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
      (LPTSTR) &lpMsgBuf,
      0,
      nullptr
   );

   if (length == 0)
      return "Unknown error";

   std::string msg((LPTSTR)lpMsgBuf);
   LocalFree(lpMsgBuf);
   return msg;
}

} // end anonymous namespace

namespace rstudio {
namespace core {
namespace system {

bool expandEnvironmentVariables(const std::wstring& value,
                                std::wstring* pResult)
{
   if (value.empty())
   {
      *pResult = value;
      return true;
   }

   DWORD sizeRequired = ::ExpandEnvironmentStringsW(value.c_str(), nullptr, 0);
   if (!sizeRequired)
   {
      DLOG("Error expanding environment strings: couldn't determine required size");
      return false;
   }

   std::vector<wchar_t> buffer(sizeRequired);
   DWORD result = ::ExpandEnvironmentStringsW(
       value.c_str(),
       &buffer[0],
       static_cast<DWORD>(buffer.size()));

   if (!result || result > buffer.size())
   {
      DWORD error = ::GetLastError();
      std::string message = getErrorMessage(error);
      DLOG("Error " << result << " expanding environment strings: " << message);
      return false;
   }

   *pResult = std::wstring(&buffer[0]);
   return true;
}

class RegistryKey {
public:

   RegistryKey()
      : hKey_(nullptr)
   {
   }

   ~RegistryKey()
   {
      if (hKey_ != nullptr)
      {
         ::RegCloseKey(hKey_);
         hKey_ = nullptr;
      }
   }

   bool open(HKEY hKey, const std::string& subKey, REGSAM samDesired)
   {
      LONG error = ::RegOpenKeyExW(hKey, utf8ToWide(subKey).c_str(), 0, samDesired, &hKey_);
      if (error != ERROR_SUCCESS)
      {
         std::string message = getErrorMessage(error);
         std::string fullKey = registryKeyToString(hKey) + "\\" + subKey;
         DLOG("Error " << error << " opening registry key '" << fullKey << "': " << message);
         hKey_ = nullptr;
         return false;
      }

      return true;
   }

   bool isOpen()
   {
      return hKey_ != nullptr;
   }

   HKEY handle()
   {
      return hKey_;
   }

   std::string getStringValue(const std::string& name,
                              const std::string& defaultValue)
   {
      std::string value;
      return getStringValue(name, &value) ? value : defaultValue;
   }

   bool getStringValue(const std::string& name,
                       std::string* pValue)
   {
      std::wstring wideName = utf8ToWide(name);
      std::vector<wchar_t> buffer(256);
      while (true)
      {
         DWORD type;
         DWORD size = static_cast<DWORD>(buffer.size() * sizeof(wchar_t));

         LONG result = ::RegQueryValueExW(
             hKey_,
             wideName.c_str(),
             nullptr,
             &type,
             reinterpret_cast<LPBYTE>(&buffer[0]),
             &size);

         switch (result)
         {
         case ERROR_SUCCESS:
         {
            if (type != REG_SZ && type != REG_EXPAND_SZ)
            {
               DLOG("Error getting registry value: unexpected type '" << type << "'");
               return false;
            }

            // 'size' is in bytes. REG_SZ and friends may or may not be
            // null-terminated, so trim the string at the first null, if any.
            std::wstring value(&buffer[0], size / sizeof(wchar_t));
            size_t idxNull = value.find(L'\0');
            if (idxNull != std::wstring::npos)
               value.resize(idxNull);

            if (type == REG_EXPAND_SZ)
               expandEnvironmentVariables(value, &value);

            *pValue = wideToUtf8(value);
            return true;
         }

         case ERROR_MORE_DATA:
            buffer.resize(size / sizeof(wchar_t) + 1);
            continue;

         default:
            return false;
         }
      }
   }

   std::vector<std::string> keyNames()
   {
      DWORD subKeys = 0, maxLen = 0;
      LONG result = ::RegQueryInfoKeyW(hKey_, nullptr, nullptr, nullptr, &subKeys, &maxLen,
                                       nullptr, nullptr, nullptr, nullptr, nullptr, nullptr);
      if (result != ERROR_SUCCESS)
      {
         DLOG("Error " << result << " querying registry key info: " << getErrorMessage(result));
         return std::vector<std::string>();
      }

      // maxLen is in characters and excludes the terminating null
      std::vector<wchar_t> nameBuffer(maxLen + 1);
      std::vector<std::string> results;
      results.reserve(subKeys);

      for (DWORD i = 0; ; i++)
      {
         DWORD size = static_cast<DWORD>(nameBuffer.size());
         LONG result = ::RegEnumKeyExW(hKey_,
                                       i,
                                       &nameBuffer[0],
                                       &size,
                                       nullptr, nullptr, nullptr, nullptr);
         switch (result)
         {
         case ERROR_SUCCESS:
         {
            results.push_back(wideToUtf8(std::wstring(&nameBuffer[0], size)));
            break;
         }
         case ERROR_NO_MORE_ITEMS:
         {
            return results;
         }
         default:
         {
            break;
         }
         }

      }

      return results;
   }

private:
   HKEY hKey_;

};

} // namespace system
} // namespace core
} // namespace rstudio

#endif

namespace rstudio {
namespace desktop {

namespace {

#ifdef __APPLE__

template <typename TValue>
class CFReleaseHandle
{
public:
   CFReleaseHandle(TValue value = nullptr)
      : value_(value)
   {
   }

   ~CFReleaseHandle()
   {
      if (value_)
         CFRelease(value_);
   }

   TValue& value()
   {
      return value_;
   }

   operator TValue() const
   {
      return value_;
   }

   TValue* operator&()
   {
      return &value_;
   }

private:
   TValue value_;
};

// Convert a CFStringRef to UTF-8, preserving embedded NULs. Returns false if
// cfStr is NULL or contains characters that cannot be represented in UTF-8
// (e.g. an unpaired surrogate); a valid but empty string returns true.
bool cfStringToUtf8(CFStringRef cfStr, std::string& result)
{
   result.clear();
   if (!cfStr)
      return false;

   CFIndex length = CFStringGetLength(cfStr);
   if (length == 0)
      return true;

   CFIndex maxSize = CFStringGetMaximumSizeForEncoding(length, kCFStringEncodingUTF8);
   std::vector<UInt8> buffer(maxSize);

   // CFStringGetBytes returns the number of characters it managed to encode;
   // fewer than 'length' means some character could not be represented.
   CFIndex usedSize = 0;
   CFIndex converted = CFStringGetBytes(
       cfStr, CFRangeMake(0, length), kCFStringEncodingUTF8,
       0 /* no loss byte */, false /* no BOM */, buffer.data(), maxSize, &usedSize);

   if (converted < length)
      return false;

   result.assign((const char*) buffer.data(), usedSize);
   return true;
}

// Helper to convert CFStringRef to std::string (UTF-8), returning an empty
// string if the value is NULL or cannot be encoded.
std::string cfStringToStdString(CFStringRef cfStr)
{
   std::string result;
   cfStringToUtf8(cfStr, result);
   return result;
}

// Single-pass font enumeration that populates both monospace and proportional lists
// Enumerates system fonts, returning:
// - monospaceFonts: PostScript names (e.g., "Menlo-Regular") for individual font faces
// - proportionalFonts: Family names (e.g., "Lucida Grande") for UI font matching
void macOSListFontsImpl(std::vector<std::string>& monospaceFonts,
                        std::vector<std::string>& proportionalFonts)
{
   std::set<std::string> monospaceSet;
   std::set<std::string> proportionalSet;

   // Create a font collection containing all available fonts
   CFReleaseHandle<CTFontCollectionRef> collection = CTFontCollectionCreateFromAvailableFonts(nullptr);
   if (!collection.value())
      return;

   // Get all font descriptors from the collection
   CFReleaseHandle<CFArrayRef> descriptors = CTFontCollectionCreateMatchingFontDescriptors(collection);
   if (!descriptors.value())
      return;

   CFIndex count = CFArrayGetCount(descriptors);

   for (CFIndex i = 0; i < count; i++)
   {
      CTFontDescriptorRef descriptor = (CTFontDescriptorRef)CFArrayGetValueAtIndex(descriptors, i);
      if (!descriptor)
         continue;

      // Get PostScript name (individual face names like "Menlo-Regular")
      CFReleaseHandle<CFStringRef> postScriptName =
         (CFStringRef)CTFontDescriptorCopyAttribute(descriptor, kCTFontNameAttribute);
      if (!postScriptName.value())
         continue;

      std::string postScriptNameStr = cfStringToStdString(postScriptName);

      // Skip hidden fonts (starting with '.')
      if (!postScriptNameStr.empty() && postScriptNameStr[0] == '.')
         continue;

      // Skip vertically-oriented fonts (starting with '@')
      if (!postScriptNameStr.empty() && postScriptNameStr[0] == '@')
         continue;

      // Get symbolic traits to check font characteristics
      CFReleaseHandle<CFDictionaryRef> traits =
         (CFDictionaryRef)CTFontDescriptorCopyAttribute(descriptor, kCTFontTraitsAttribute);

      CTFontSymbolicTraits symbolicTraits = 0;
      if (traits.value())
      {
         CFNumberRef symbolicTraitsNum =
            (CFNumberRef)CFDictionaryGetValue(traits, kCTFontSymbolicTrait);
         if (symbolicTraitsNum)
         {
            int32_t traitsValue = 0;
            CFNumberGetValue(symbolicTraitsNum, kCFNumberSInt32Type, &traitsValue);
            symbolicTraits = (CTFontSymbolicTraits)traitsValue;
         }
      }

      // Skip symbol/ornament fonts (like Wingdings)
      if (symbolicTraits & kCTFontClassMaskTrait)
      {
         CTFontStylisticClass fontClass = symbolicTraits & kCTFontClassMaskTrait;
         if (fontClass == kCTFontSymbolicClass || fontClass == kCTFontOrnamentalsClass)
            continue;
      }

      // Skip italic fonts - the old node-system-fonts behavior filtered these out
      // due to its matching algorithm penalizing italic mismatches
      if (symbolicTraits & kCTFontItalicTrait)
         continue;

      // Check if monospace and add to appropriate set
      if (symbolicTraits & kCTFontMonoSpaceTrait)
      {
         // Monospace fonts use PostScript names for the font picker
         monospaceSet.insert(postScriptNameStr);
      }
      else
      {
         // Proportional fonts use family names for UI font availability checking
         CFReleaseHandle<CFStringRef> familyName =
            (CFStringRef)CTFontDescriptorCopyAttribute(descriptor, kCTFontFamilyNameAttribute);
         if (familyName.value())
         {
            std::string familyNameStr = cfStringToStdString(familyName);
            if (!familyNameStr.empty())
               proportionalSet.insert(familyNameStr);
         }
      }
   }

   monospaceFonts = std::vector<std::string>(monospaceSet.begin(), monospaceSet.end());
   proportionalFonts = std::vector<std::string>(proportionalSet.begin(), proportionalSet.end());
}

#endif

void cleanClipboardImpl(bool stripHtml)
{
#ifdef __APPLE__

   // forward-declare
   OSStatus err;

   // get reference to clipboard
   CFReleaseHandle<PasteboardRef> clipboard;
   if (::PasteboardCreate(kPasteboardClipboard, &clipboard))
      return;

   // synchronize clipboard
   ::PasteboardSynchronize(clipboard);

   // check for items on the pasteboard
   ItemCount itemCount;
   if (::PasteboardGetItemCount(clipboard, &itemCount))
      return;

   // if we don't have anything on the clipboard, bail
   if (itemCount < 1)
      return;

   // get the item identifier
   PasteboardItemID itemId;
   if (::PasteboardGetItemIdentifier(clipboard, 1, &itemId))
      return;

   // read UTF-16 data
   CFReleaseHandle<CFDataRef> utf16Data;
   err = ::PasteboardCopyItemFlavorData(clipboard,
                                        itemId,
                                        CFSTR("public.utf16-plain-text"),
                                        &utf16Data);

   if (err && err != badPasteboardFlavorErr)
      return;

   // read HTML data
   CFReleaseHandle<CFDataRef> htmlData;
   if (!stripHtml)
   {
      err = ::PasteboardCopyItemFlavorData(clipboard, itemId, CFSTR("public.html"), &htmlData);
      if (err && err != badPasteboardFlavorErr)
         return;
   }

   // Convert any UTF-16 clipboard data to UTF-8 *before* clearing the
   // clipboard, so that a decoding failure leaves the user's clipboard intact.
   std::string utf8Text;
   if (utf16Data.value())
   {
      // copy the UTF-16 bytes out of the pasteboard data
      CFIndex length = ::CFDataGetLength(utf16Data);
      std::vector<UInt8> buffer(length);
      ::CFDataGetBytes(utf16Data, CFRangeMake(0, length), buffer.data());

      // Decode as little-endian UTF-16: macOS runs only on little-endian CPUs
      // and producers write this flavor in native byte order, so decoding as LE
      // preserves the previous host-order behavior.
      CFReleaseHandle<CFStringRef> utf16Text = CFStringCreateWithBytes(
          nullptr, buffer.data(), length, kCFStringEncodingUTF16LE, false);

      // If non-empty UTF-16 data cannot be converted to UTF-8 (malformed bytes,
      // e.g. an unpaired surrogate), leave the clipboard untouched rather than
      // clearing it and replacing the user's data with an empty string.
      if (length > 0 && !cfStringToUtf8(utf16Text, utf8Text))
         return;

      // convert '\r' line endings to '\n' -- not sure why these sneak in?
      std::replace(utf8Text.begin(), utf8Text.end(), '\r', '\n');
   }

   // clear the clipboard
   if (::PasteboardClear(clipboard))
      return;

   // write the converted UTF-8 text back to the clipboard
   if (utf16Data.value())
   {
      CFReleaseHandle<CFDataRef> utf8TextRef = CFDataCreate(nullptr, (UInt8*) utf8Text.data(), utf8Text.size());
      if (utf8TextRef && utf8TextRef.value())
         ::PasteboardPutItemFlavor(clipboard, (PasteboardItemID) 1, CFSTR("public.utf8-plain-text"), utf8TextRef, 0);
   }

   if (htmlData && htmlData.value())
      ::PasteboardPutItemFlavor(clipboard, (PasteboardItemID) 1, CFSTR("public.html"), htmlData, 0);

#endif // __APPLE__
}

} // end anonymous namespace

Napi::Value cleanClipboard(const Napi::CallbackInfo& info)
{
   // unpack arguments
   bool stripHtml = info[0].As<Napi::Boolean>().Value();

   // call method
   cleanClipboardImpl(stripHtml);

   // return value
   return Napi::Value();
}

Napi::Value shortPathName(const Napi::CallbackInfo& info)
{
   Napi::String path = info[0].As<Napi::String>();

#ifdef _WIN32

   // Get path as UTF-16 text.
   // Note that, on Windows, std::wstring and std::u16string are interchangable.
   // With the exception that char16_t is unsigned, and wchar_t is signed.
   std::u16string wPath = info[0].As<Napi::String>().Utf16Value();

   // Convert to a short path.
   //
   // https://learn.microsoft.com/en-us/windows/win32/api/fileapi/nf-fileapi-getshortpathnamew
   //
   // If the lpszShortPath buffer is too small to contain the path, the
   // return value is the size of the buffer, _in TCHARs_, that is required to
   // hold the path _and the terminating null character_.
   int wShortPathSize = ::GetShortPathNameW((LPCWSTR) wPath.data(), nullptr, 0);
   if (wShortPathSize == 0) {
      return path;
   }

   // Allocate a vector for the short path string.
   std::vector<WCHAR> wShortPath(wShortPathSize, 0);

   // Copy into that buffer.
   int numBytesWritten = ::GetShortPathNameW((LPCWSTR) wPath.data(), &wShortPath[0], wShortPathSize);
   if (numBytesWritten == 0) {
      return path;
   }

   // Set the resulting path.
   path = Napi::String::From(info.Env(), (const char16_t*) &wShortPath[0]);

#endif

   return path;
}



namespace {

bool isCtrlKeyDownImpl()
{
   bool result = false;

#ifdef _WIN32
   result = ::GetAsyncKeyState(VK_CONTROL) & ~1;
#endif

   return result;

}

} // end anonymous namespace

Napi::Value isCtrlKeyDown(const Napi::CallbackInfo& info)
{
   bool value = isCtrlKeyDownImpl();
   return Napi::Boolean::From(info.Env(), value);
}


namespace {

std::wstring currentCSIDLPersonalHomePathImpl()
{
#ifdef _WIN32

   wchar_t buffer[MAX_PATH] = {};

   // query for My Documents directory
   const DWORD SHGFP_TYPE_CURRENT = 0;
   HRESULT hr = ::SHGetFolderPathW(nullptr,
                                   CSIDL_PERSONAL,
                                   nullptr,
                                   SHGFP_TYPE_CURRENT,
                                   buffer);

   return std::wstring(buffer);

#else

   return std::wstring();

#endif
}

} // end anonymous namespace

Napi::Value currentCSIDLPersonalHomePath(const Napi::CallbackInfo& info)
{
   std::string u8str = wideToUtf8(currentCSIDLPersonalHomePathImpl());
   return Napi::String::New(info.Env(), u8str);
}

namespace {

std::wstring defaultCSIDLPersonalHomePathImpl()
{
#ifdef _WIN32
   wchar_t buffer[MAX_PATH];

   // query for default and force creation (works around situations
   // where redirected path is not available)
   const DWORD SHGFP_TYPE_DEFAULT = 1;
   HRESULT hr = ::SHGetFolderPathW(nullptr,
                                   CSIDL_PERSONAL | CSIDL_FLAG_CREATE,
                                   nullptr,
                                   SHGFP_TYPE_DEFAULT,
                                   buffer);

   return std::wstring(buffer);

#else

   return std::wstring();

#endif
}

} // end anonymous namespace

Napi::Value defaultCSIDLPersonalHomePath(const Napi::CallbackInfo& info)
{
   std::string u8str = wideToUtf8(defaultCSIDLPersonalHomePathImpl());
   return Napi::String::New(info.Env(), u8str);
}

namespace {

std::vector<std::string> searchRegistryForInstallationsOfRImpl(const Napi::CallbackInfo& info)
{
   std::vector<std::string> result;

#ifdef _WIN32
   using namespace rstudio::core::system;

   // TODO: Should we check "SOFTWARE\\R-core\\R64" as well?
   // When using the most recent R installer (for R 4.2.3), it seems
   // like registry entries were populated for both R and R64.
   std::string rRootKeyName = "SOFTWARE\\R-core\\R";

   // search both 32-bit and 64-bit registry keys, just in case
   for (int flags : { KEY_WOW64_64KEY, KEY_WOW64_32KEY })
   {
      for (HKEY key : { HKEY_CURRENT_USER, HKEY_LOCAL_MACHINE })
      {
         // open registry key
         RegistryKey rootKey;
         if (!rootKey.open(key, rRootKeyName, KEY_READ | flags))
            continue;

         // get the sub-key names (these are specific versions of R)
         auto versionKeyNames = rootKey.keyNames();
         for (auto&& versionKeyName : versionKeyNames)
         {
            RegistryKey versionKey;
            if (!versionKey.open(key, rRootKeyName + "\\" + versionKeyName, KEY_READ | flags))
               continue;

            // read the installation path
            std::string installPath;
            if (!versionKey.getStringValue("InstallPath", &installPath))
               continue;

            // add it
            result.push_back(installPath);

         }
      }
   }
#endif

   return result;
}

} // end anonymous namespace

Napi::Value searchRegistryForInstallationsOfR(const Napi::CallbackInfo& info)
{
   auto installPaths = searchRegistryForInstallationsOfRImpl(info);
   auto result = Napi::Array::New(info.Env(), installPaths.size());
   for (int i = 0, n = installPaths.size(); i < n; i++)
      result[i] = Napi::String::From(info.Env(), installPaths[i]);
   return result;
}

namespace {

std::string searchRegistryForDefaultInstallationOfRImpl(const std::string& versionKey)
{
   std::string installPath;

#ifdef _WIN32
   using namespace rstudio::core::system;

   std::string rCoreKey = "SOFTWARE\\R-core";
   std::string rKey = rCoreKey + "\\" + versionKey;

   // search both 32-bit and 64-bit registry keys, just in case
   for (int flags : { KEY_WOW64_64KEY, KEY_WOW64_32KEY })
   {
      for (HKEY key : { HKEY_CURRENT_USER, HKEY_LOCAL_MACHINE })
      {
         // open registry key
         RegistryKey registryKey;
         if (!registryKey.open(key, rKey, KEY_READ | flags))
            continue;

         // read the installation path
         if (!registryKey.getStringValue("InstallPath", &installPath))
            continue;

         DLOG("Found default version of R in registry: " << rKey << " (InstallPath = " << installPath << ")");
         return installPath;
      }
   }
#endif

   return installPath;
}

} // end anonymous namespace

Napi::Value searchRegistryForDefaultInstallationOfR(const Napi::CallbackInfo& info)
{
   std::string versionKey = info[0].As<Napi::String>().Utf8Value();
   std::string installPath = searchRegistryForDefaultInstallationOfRImpl(versionKey);
   return Napi::String::From(info.Env(), installPath);
}

Napi::Value openExternal(const Napi::CallbackInfo& info)
{

#ifdef _WIN32
   std::u16string uPath = info[0].As<Napi::String>().Utf16Value();
   CoInitializeEx(NULL, COINIT_APARTMENTTHREADED | COINIT_DISABLE_OLE1DDE);
   ShellExecuteW(NULL, L"open", (wchar_t*) &uPath[0], NULL, NULL, SW_SHOWNORMAL);
#endif

   return Napi::Value();

}

namespace {

#ifdef _WIN32

// Hook watching for dialogs created by the rsession process; see
// win32WatchSessionDialogs().
HWINEVENTHOOK s_sessionDialogHook = nullptr;

void CALLBACK onSessionDialogStart(HWINEVENTHOOK, DWORD, HWND hwnd, LONG, LONG, DWORD, DWORD)
{
   DLOG("Raising dialog created by watched process (hwnd " << hwnd << ")");
   if (!::BringWindowToTop(hwnd))
   {
      DWORD error = ::GetLastError();
      DLOG("Error " << error << " raising dialog (hwnd " << hwnd << "): " << getErrorMessage(error));
   }
}

void win32StopWatchingSessionDialogsImpl()
{
   if (s_sessionDialogHook != nullptr)
   {
      ::UnhookWinEvent(s_sessionDialogHook);
      s_sessionDialogHook = nullptr;
   }
}

#endif

} // end anonymous namespace

Napi::Value win32WatchSessionDialogs(const Napi::CallbackInfo& info)
{
#ifdef _WIN32
   DWORD processId = info[0].As<Napi::Number>().Uint32Value();

   win32StopWatchingSessionDialogsImpl();

   // a process id of 0 would tell SetWinEventHook to watch all processes
   // on the current desktop
   if (processId != 0)
   {
      // WINEVENT_OUTOFCONTEXT delivers events via this thread's message
      // loop (Electron's main thread), so no DLL injection is involved
      s_sessionDialogHook = ::SetWinEventHook(
          EVENT_SYSTEM_DIALOGSTART, EVENT_SYSTEM_DIALOGSTART,
          nullptr,
          onSessionDialogStart,
          processId,
          0,
          WINEVENT_OUTOFCONTEXT);

      if (s_sessionDialogHook == nullptr)
      {
         DWORD error = ::GetLastError();
         DLOG("Error " << error << " watching for dialogs from process " << processId << ": " << getErrorMessage(error));
      }
      else
      {
         DLOG("Watching for dialogs created by process " << processId);
      }
   }
#endif

   return Napi::Value();
}

Napi::Value win32StopWatchingSessionDialogs(const Napi::CallbackInfo& info)
{
#ifdef _WIN32
   win32StopWatchingSessionDialogsImpl();
#endif

   return Napi::Value();
}

namespace {

#ifdef _WIN32

int CALLBACK win32ListMonospaceFontsProc(
    ENUMLOGFONTEX* lpelfe,
    NEWTEXTMETRICEX* lpntme,
    DWORD FontType,
    LPARAM lParam)
{
    if (lpelfe != nullptr)
    {
      // The pitch and family of the font. The two low-order bits specify the
      // pitch of the font and can be one of the following values.
      if (lpelfe->elfLogFont.lfPitchAndFamily & FIXED_PITCH)
      {
         std::set<std::string>* pFontSet = (std::set<std::string>*) lParam;
         CHAR* faceName = lpelfe->elfLogFont.lfFaceName;

         // Skip vertically-oriented fonts.
         // https://devblogs.microsoft.com/oldnewthing/20120719-00/?p=7093
         if (faceName[0] == L'@')
            return 1;

         pFontSet->insert(faceName);
      }
    }

    return 1; // Continue enumeration
}

#endif

[[maybe_unused]] std::vector<std::string> win32ListMonospaceFontsImpl()
{
    std::vector<std::string> fontList;

#ifdef _WIN32
    std::set<std::string> fontSet;
    HDC hdc = GetDC(NULL);
    LOGFONT logFont = { 0 };
    logFont.lfCharSet = DEFAULT_CHARSET;
    EnumFontFamiliesEx(hdc, &logFont, (FONTENUMPROC) win32ListMonospaceFontsProc, (LPARAM) &fontSet, 0);
    ReleaseDC(NULL, hdc);
    fontSet.erase("8514oem");
    fontList = std::vector<std::string>(fontSet.begin(), fontSet.end());
#endif

    return fontList;
}

} // end anonymous namespace

Napi::Value win32ListMonospaceFonts(const Napi::CallbackInfo& info)
{
   std::vector<std::string> fontList;

#ifdef _WIN32
   fontList = win32ListMonospaceFontsImpl();
#endif

   auto result = Napi::Array::New(info.Env(), fontList.size());
   for (int i = 0, n = fontList.size(); i < n; i++)
      result[i] = Napi::String::From(info.Env(), fontList[i]);
   return result;
}

Napi::Value macOSListFonts(const Napi::CallbackInfo& info)
{
   std::vector<std::string> monospaceFonts;
   std::vector<std::string> proportionalFonts;

#ifdef __APPLE__
   macOSListFontsImpl(monospaceFonts, proportionalFonts);
#endif

   auto monospaceArray = Napi::Array::New(info.Env(), monospaceFonts.size());
   for (size_t i = 0, n = monospaceFonts.size(); i < n; i++)
      monospaceArray[i] = Napi::String::From(info.Env(), monospaceFonts[i]);

   auto proportionalArray = Napi::Array::New(info.Env(), proportionalFonts.size());
   for (size_t i = 0, n = proportionalFonts.size(); i < n; i++)
      proportionalArray[i] = Napi::String::From(info.Env(), proportionalFonts[i]);

   auto result = Napi::Object::New(info.Env());
   result.Set("monospace", monospaceArray);
   result.Set("proportional", proportionalArray);
   return result;
}

} // end namespace desktop
} // end namespace rstudio

Napi::Object Init(Napi::Env env, Napi::Object exports) {

   // debug logging
   const char* logLevel = ::getenv("RS_LOG_LEVEL");
   if (logLevel)
   {
      s_loggingEnabled =
         strcmp(logLevel, "debug") == 0 ||
         strcmp(logLevel, "DEBUG") == 0;
   }

   RS_EXPORT_FUNCTION("cleanClipboard", rstudio::desktop::cleanClipboard);
   RS_EXPORT_FUNCTION("shortPathName", rstudio::desktop::shortPathName);
   RS_EXPORT_FUNCTION("isCtrlKeyDown", rstudio::desktop::isCtrlKeyDown);
   RS_EXPORT_FUNCTION("currentCSIDLPersonalHomePath", rstudio::desktop::currentCSIDLPersonalHomePath);
   RS_EXPORT_FUNCTION("defaultCSIDLPersonalHomePath", rstudio::desktop::defaultCSIDLPersonalHomePath);
   RS_EXPORT_FUNCTION("searchRegistryForInstallationsOfR", rstudio::desktop::searchRegistryForInstallationsOfR);
   RS_EXPORT_FUNCTION("searchRegistryForDefaultInstallationOfR", rstudio::desktop::searchRegistryForDefaultInstallationOfR);
   RS_EXPORT_FUNCTION("openExternal", rstudio::desktop::openExternal);
   RS_EXPORT_FUNCTION("win32ListMonospaceFonts", rstudio::desktop::win32ListMonospaceFonts);
   RS_EXPORT_FUNCTION("win32WatchSessionDialogs", rstudio::desktop::win32WatchSessionDialogs);
   RS_EXPORT_FUNCTION("win32StopWatchingSessionDialogs", rstudio::desktop::win32StopWatchingSessionDialogs);
   RS_EXPORT_FUNCTION("macOSListFonts", rstudio::desktop::macOSListFonts);

   return exports;

}

NODE_API_MODULE(rstudio, Init)
