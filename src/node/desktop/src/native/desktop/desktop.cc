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

#include <chrono>
#include <codecvt>
#include <iomanip>
#include <iostream>
#include <locale>
#include <set>
#include <sstream>
#include <string>
#include <vector>

#ifdef __APPLE__
# include <CoreFoundation/CoreFoundation.h>
# include <Carbon/Carbon.h>
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

void logDebug(const std::string& message)
{
   if (s_loggingEnabled && message.length())
   {
      std::cerr << timestamp() << " DEBUG " << message;
      if (message[message.length() - 1] != '\n')
         std::cerr << std::endl;
   }
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

bool expandEnvironmentVariables(const std::string& value,
                                std::string* pResult)
{
   if (value.empty())
   {
      *pResult = value;
      return true;
   }

   DWORD sizeRequired = ::ExpandEnvironmentStrings(value.c_str(), nullptr, 0);
   if (!sizeRequired)
   {
      DLOG("Error expanding environment strings: couldn't determine required size");
      return false;
   }

   std::vector<char> buffer(sizeRequired);
   auto result = ::ExpandEnvironmentStrings(
       value.c_str(),
       &buffer[0],
       static_cast<DWORD>(buffer.capacity()));

   if (!result || result > buffer.capacity())
   {
      DWORD error = ::GetLastError();
      std::string message = getErrorMessage(error);
      DLOG("Error " << result << " expanding environment strings: " << message);
      return false;
   }

   *pResult = std::string(&buffer[0]);
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
      LONG error = ::RegOpenKeyEx(hKey, subKey.c_str(), 0, samDesired, &hKey_);
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
      std::vector<char> buffer(256);
      while (true)
      {
         DWORD type;
         DWORD size = static_cast<DWORD>(buffer.capacity());

         LONG result = ::RegQueryValueEx(
             hKey_,
             name.c_str(),
             nullptr,
             &type,
             (LPBYTE)(&buffer[0]),
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

            *pValue = std::string(&buffer[0], buffer.capacity());

            // REG_SZ and friends may or may not be null-terminated.
            // So trim the string at the first null, if any.
            size_t idxNull = pValue->find('\0');
            if (idxNull != std::string::npos)
               pValue->resize(idxNull);

            if (type == REG_EXPAND_SZ)
               expandEnvironmentVariables(*pValue, pValue);

            return true;
         }

         case ERROR_MORE_DATA:
            buffer.reserve(size);
            continue;

         default:
            return false;
         }
      }
   }

   std::vector<std::string> keyNames()
   {
      LONG result;

      DWORD subKeys, maxLen;
      result = ::RegQueryInfoKey(hKey_, nullptr, nullptr, nullptr, &subKeys, &maxLen,
                                 nullptr, nullptr, nullptr, nullptr, nullptr, nullptr);


      std::vector<char> nameBuffer(maxLen + 2);
      std::vector<std::string> results;
      results.reserve(subKeys);

      for (DWORD i = 0; ; i++)
      {
         DWORD size = static_cast<DWORD>(nameBuffer.capacity());
         LONG result = ::RegEnumKeyEx(hKey_,
                                      i,
                                      &nameBuffer[0],
                                      &size,
                                      nullptr, nullptr, nullptr, nullptr);
         switch (result)
         {
         case ERROR_SUCCESS:
         {
            results.push_back(std::string(&nameBuffer[0], size));
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

   // clear the clipboard
   if (::PasteboardClear(clipboard))
      return;

   // if we had some UTF-16 data on the clipboard, convert it to UTF-8
   // and write that to the clipboard instead
   if (utf16Data.value())
   {
      // read the clipboard data (as bytes)
      // include extra byte for null terminator, just in case?
      auto length = ::CFDataGetLength(utf16Data);
      std::vector<UInt8> buffer(length);
      ::CFDataGetBytes(utf16Data, CFRangeMake(0, length), (UInt8*) buffer.data());

      // convert those bytes from UTF-16 to UTF-8
      char16_t* pBytes = (char16_t*) &buffer[0];
      std::wstring_convert<std::codecvt_utf8_utf16<char16_t>, char16_t> converter;
      std::string utf8Text = converter.to_bytes(pBytes, pBytes + length / 2);

      // convert '\r' line endings to '\n' -- not sure why these sneak in?
      std::replace(utf8Text.begin(), utf8Text.end(), '\r', '\n');

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
   auto value = currentCSIDLPersonalHomePathImpl();

   // convert wide to UTF-8
   std::wstring_convert<std::codecvt_utf8<wchar_t>> converter;
   std::string u8str = converter.to_bytes(value);
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
   auto value = defaultCSIDLPersonalHomePathImpl();

   // convert wide to UTF-8
   std::wstring_convert<std::codecvt_utf8<wchar_t>> converter;
   std::string u8str = converter.to_bytes(value);
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

std::vector<std::string> win32ListMonospaceFontsImpl()
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

   return exports;

}

NODE_API_MODULE(rstudio, Init)
