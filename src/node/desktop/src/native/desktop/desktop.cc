/*
 * desktop.cc
 *
 * Copyright (C) 2022 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
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
   if (stripHtml)
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

wchar_t* currentCSIDLPersonalHomePathImpl()
{
   #ifdef _WIN32
   // query for My Documents directory
   const DWORD SHGFP_TYPE_CURRENT = 0;
   wchar_t homePath[MAX_PATH];
   HRESULT hr = ::SHGetFolderPathW(nullptr,
                                   CSIDL_PERSONAL,
                                   nullptr,
                                   SHGFP_TYPE_CURRENT,
                                   homePath);
   if (SUCCEEDED(hr))
      return homePath;
   #endif
   return homePath;
}
} // end anonymous namespace

Napi::Value currentCSIDLPersonalHomePath(const Napi::CallbackInfo& info)
{
   wchar_t* value = currentCSIDLPersonalHomePathImpl();
   // wide to UTF-8
    std::wstring_convert<std::codecvt_utf8<wchar_t>> conv1;
    std::string u8str = conv1.to_bytes(value);
   return Napi::String::New(info.Env(), u8str);
}

namespace {

wchar_t* defaultCSIDLPersonalHomePathImpl()
{
   #ifdef _WIN32
   // query for default and force creation (works around situations
   // where redirected path is not available)
   const DWORD SHGFP_TYPE_DEFAULT = 1;
   wchar_t homePath[MAX_PATH];
   HRESULT hr = ::SHGetFolderPathW(nullptr,
                                   CSIDL_PERSONAL|CSIDL_FLAG_CREATE,
                                   nullptr,
                                   SHGFP_TYPE_DEFAULT,
                                   homePath);
   if (SUCCEEDED(hr))
      return homePath;
   #endif
   return homePath;
}
} // end anonymous namespace

Napi::Value defaultCSIDLPersonalHomePath(const Napi::CallbackInfo& info)
{
   wchar_t* value = defaultCSIDLPersonalHomePathImpl();
      // wide to UTF-8
    std::wstring_convert<std::codecvt_utf8<wchar_t>> conv1;
    std::string u8str = conv1.to_bytes(value);
   return Napi::String::New(info.Env(), u8str);
}

} // end namespace desktop
} // end namespace rstudio

Napi::Object Init(Napi::Env env, Napi::Object exports) {

  RS_EXPORT_FUNCTION("cleanClipboard", rstudio::desktop::cleanClipboard);
  RS_EXPORT_FUNCTION("isCtrlKeyDown", rstudio::desktop::isCtrlKeyDown);
  RS_EXPORT_FUNCTION("currentCSIDLPersonalHomePath", rstudio::desktop::currentCSIDLPersonalHomePath);
  RS_EXPORT_FUNCTION("defaultCSIDLPersonalHomePath", rstudio::desktop::defaultCSIDLPersonalHomePath);

  return exports;

}

NODE_API_MODULE(rstudio, Init)
