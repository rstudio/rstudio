/*
 * SessionModuleContextWin32.cpp
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

#include <session/SessionModuleContext.hpp>

#include <windows.h>
#include <objbase.h>
#include <shlobj.h>

#include <algorithm>
#include <string>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace module_context {

namespace {

// RAII release for COM interface pointers (same shape as the file-private
// AutoRelease in SessionGit.cpp)
template <typename T>
class AutoRelease
{
public:
   explicit AutoRelease(T* pUnk) : pUnk_(pUnk)
   {
   }

   ~AutoRelease()
   {
      if (pUnk_ != nullptr)
         pUnk_->Release();
   }

private:
   T* pUnk_;
};

// the shell APIs store and expect native (backslash) separators, while
// FilePath::getAbsolutePathW() returns generic (forward-slash) paths
std::wstring toNativeSeparators(std::wstring path)
{
   std::replace(path.begin(), path.end(), L'/', L'\\');
   return path;
}

// error shape mirrors resolveFinderAlias (SessionModuleContext.mm):
// systemError + diagnostic properties
Error shortcutError(HRESULT hr,
                    const FilePath& shortcutPath,
                    const ErrorLocation& location)
{
   Error error = systemError(boost::system::errc::no_such_file_or_directory,
                             location);
   error.addProperty("shortcut-path", shortcutPath);
   char hresult[16];
   ::snprintf(hresult, sizeof(hresult), "0x%08lX",
              static_cast<unsigned long>(hr));
   error.addProperty("hresult", hresult);
   return error;
}

} // anonymous namespace

bool isWindowsShortcut(const core::FilePath& filePath)
{
   // cheap extension gate first: this runs once per listing entry, and the
   // COM work in resolveWindowsShortcut is only paid for actual .lnk files.
   // Like Explorer, anything named *.lnk is treated as a shortcut; files
   // with non-shortcut content simply fail to resolve (IPersistFile::Load).
   if (filePath.getExtensionLowerCase() != ".lnk")
      return false;

   // shortcuts are regular files: excludes directories named *.lnk and
   // entries deleted since the listing was taken
   return filePath.isRegularFile();
}

Error resolveWindowsShortcut(const core::FilePath& shortcutPath,
                             core::FilePath* pTargetPath)
{
   // NOTE: rsession initializes COM (STA) eagerly on the main thread at
   // startup (SessionMain.cpp), and listings / file-monitor callbacks run
   // on the main thread, so no CoInitialize is needed here. If this is ever
   // called on a thread without COM, CoCreateInstance fails cleanly with
   // CO_E_NOTINITIALIZED rather than crashing.
   IShellLinkW* pShellLink = nullptr;
   HRESULT hr = ::CoCreateInstance(CLSID_ShellLink,
                                   nullptr,
                                   CLSCTX_INPROC_SERVER,
                                   IID_IShellLinkW,
                                   (void**) &pShellLink);
   if (FAILED(hr))
      return shortcutError(hr, shortcutPath, ERROR_LOCATION);
   AutoRelease<IShellLinkW> arShellLink(pShellLink);

   IPersistFile* pPersistFile = nullptr;
   hr = pShellLink->QueryInterface(IID_IPersistFile, (void**) &pPersistFile);
   if (FAILED(hr))
      return shortcutError(hr, shortcutPath, ERROR_LOCATION);
   AutoRelease<IPersistFile> arPersistFile(pPersistFile);

   std::wstring widePath = toNativeSeparators(shortcutPath.getAbsolutePathW());
   hr = pPersistFile->Load(widePath.c_str(), STGM_READ);
   if (FAILED(hr))
      return shortcutError(hr, shortcutPath, ERROR_LOCATION);

   // Deliberately NO IShellLink::Resolve() -- during a listing it could
   // search the disk or hit the network for moved/remote targets. GetPath
   // returns the shortcut's STORED target path even when that target no
   // longer exists; broken-shortcut detection is the caller's exists()
   // check on the result (createFileSystemItem does this, matching the
   // broken-alias handling).
   wchar_t targetPath[MAX_PATH];
   hr = pShellLink->GetPath(targetPath, MAX_PATH, nullptr, 0);

   // S_FALSE (or an empty path) means the shortcut has no file-system
   // target (virtual shortcuts, e.g. to Control Panel applets); report
   // those as unresolvable
   if (hr != S_OK || targetPath[0] == L'\0')
      return shortcutError(hr, shortcutPath, ERROR_LOCATION);

   *pTargetPath = FilePath(std::wstring(targetPath));
   return Success();
}

} // namespace module_context
} // namespace session
} // namespace rstudio
