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

#include <codecvt>
#include <iostream>
#include <locale>
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

#define RS_EXPORT_FUNCTION(__NAME__, __FUNCTION__) \
  exports.Set(                                     \
    Napi::String::New(env, __NAME__),              \
    Napi::Function::New(env, __FUNCTION__)         \
  )


#ifdef _WIN32

namespace rstudio {
namespace core {
namespace system {

bool expandEnvironmentVariables(std::string value, std::string* pResult)
{
   if (value.empty())
   {
      *pResult = value;
      return true;
   }

   DWORD sizeRequired = ::ExpandEnvironmentStrings(value.c_str(), nullptr, 0);
   if (!sizeRequired)
      return false;

   std::vector<char> buffer(sizeRequired);
   auto result = ::ExpandEnvironmentStrings(
       value.c_str(),
       &buffer[0],
       static_cast<DWORD>(buffer.capacity()));

   if (!result || result > buffer.capacity())
      return false;

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
      ::RegCloseKey(hKey_);
      hKey_ = nullptr;
   }

   bool open(HKEY hKey, std::string subKey, REGSAM samDesired)
   {
      hKey_ = nullptr;
      LONG error = ::RegOpenKeyEx(hKey, subKey.c_str(), 0, samDesired, &hKey_);
      if (error != ERROR_SUCCESS)
      {
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
      bool ok = getStringValue(name, &value);
      if (ok)
         return value;
      return defaultValue;
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
                  return false;

               *pValue = std::string(&buffer[0], buffer.capacity());

               // REG_SZ and friends may or may not be null-terminated.
               // So trim the string at the first null, if any.
               size_t idxNull = pValue->find('\0');
               if (idxNull != std::string::npos)
                  pValue->resize(idxNull);

               if (type == REG_EXPAND_SZ)
               {
                  bool ok = expandEnvironmentVariables(*pValue, pValue);
                  if (!ok)
                     return false;
               }

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


      std::vector<char> nameBuffer(maxLen+2);
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
            results.push_back(std::string(&nameBuffer[0], size));
            break;
         case ERROR_NO_MORE_ITEMS:
            return results;
         default:
            break;
         }

      }

      return std::vector<std::string>();
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

   // search both 32-bit and 64-bit registry keys, just in case
   for (int flags : { KEY_WOW64_32KEY, KEY_WOW64_64KEY })
   {
      for (HKEY key : { HKEY_LOCAL_MACHINE, HKEY_CURRENT_USER })
      {
         // open registry key
         RegistryKey rootKey;
         bool ok = rootKey.open(key, "Software\\R-core\\R", KEY_READ | flags);
         if (!ok)
            continue;

         // get the sub-key names (these are specific versions of R)
         auto keyNames = rootKey.keyNames();
         for (auto&& keyName : keyNames)
         {
            RegistryKey versionKey;
            bool ok = versionKey.open(rootKey.handle(), keyName, KEY_READ | flags);
            if (!ok)
               continue;

            // read the installation path
            std::string installPath;
            if (!versionKey.getStringValue("InstallPath", &installPath))
               continue;

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
      result.Set(i, Napi::String::From(info.Env(), installPaths[i]));
   return result;
}

} // end namespace desktop
} // end namespace rstudio

Napi::Object Init(Napi::Env env, Napi::Object exports) {

  RS_EXPORT_FUNCTION("cleanClipboard", rstudio::desktop::cleanClipboard);
  RS_EXPORT_FUNCTION("isCtrlKeyDown", rstudio::desktop::isCtrlKeyDown);
  RS_EXPORT_FUNCTION("currentCSIDLPersonalHomePath", rstudio::desktop::currentCSIDLPersonalHomePath);
  RS_EXPORT_FUNCTION("defaultCSIDLPersonalHomePath", rstudio::desktop::defaultCSIDLPersonalHomePath);
  RS_EXPORT_FUNCTION("searchRegistryForInstallationsOfR", rstudio::desktop::searchRegistryForInstallationsOfR);

  return exports;

}

NODE_API_MODULE(rstudio, Init)
