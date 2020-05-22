/*
 * DesktopOfficeViewer.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#include <iostream>

#include <windows.h>
#include <winuser.h>
#include <oleauto.h>

#include <boost/algorithm/string.hpp>

#include <shared_core/Error.hpp>
#include <core/system/System.hpp>
#include <core/system/Environment.hpp>

#include "DesktopComUtils.hpp"
#include "DesktopOfficeViewer.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace desktop {

OfficeViewer::OfficeViewer(const std::wstring& progId,
                           const std::wstring& collection,
                           int readOnlyPos):
   idisp_(nullptr),
   progId_(progId),
   collection_(collection),
   readOnlyPos_(readOnlyPos)
{
}

OfficeViewer::~OfficeViewer()
{
   try
   {
      if (idisp_)
         idisp_->Release();
   }
   catch (...)
   {
      // Ignore exceptions during teardown
   }
}

Error OfficeViewer::ensureInterface()
{
   HRESULT hr = S_OK;
   Error errorHR = Success();

   // If we have an active IDispatch pointer, check to see whether it has been
   // closed
   if (idisp_ != nullptr)
   {
      // Test the interface by looking up a known DISPID
      const WCHAR* wstrQuit = L"Quit";
      DISPID dispid;
      hr = idisp_->GetIDsOfNames(IID_NULL, const_cast<WCHAR**>(&wstrQuit),
                                     1, LOCALE_USER_DEFAULT, &dispid);

      // If the lookup fails, release this IDispatch pointer--it's stale.
      // We'll CoCreate a new instance below.
      if (FAILED(hr) &&
          SCODE_CODE(hr) == RPC_S_SERVER_UNAVAILABLE)
      {
         idisp_->Release();
         idisp_ = nullptr;
      }
   }

   // Get an IDispatch for the application's root object
   if (idisp_ == nullptr)
   {
      CLSID clsid;
      CoInitialize(nullptr);
      VERIFY_HRESULT(CLSIDFromProgID(progId_.c_str(), &clsid));
      VERIFY_HRESULT(CoCreateInstance(clsid, nullptr, CLSCTX_LOCAL_SERVER,
                                      IID_IDispatch,
                                      reinterpret_cast<void**>(&idisp_)));
      idisp_->AddRef();
   }

LErrExit:
   return errorHR;
}

Error OfficeViewer::showApp()
{
   // Allow the application to become the foreground window.
   // CoAllowSetForegroundWindow would be preferable here, since we'd be able
   // to restrict activation to only the process we started, but it is not
   // exposed by MinGW headers.  Note that AllowSetForegroundWindow already
   // limits activation to processes initiated by the foreground process, and
   // self-expires on user input.
   AllowSetForegroundWindow(ASFW_ANY);

   Error errorHR = Success();
   HRESULT hr = S_OK;

   VARIANT visible;
   visible.vt = VT_BOOL;
   visible.boolVal = true;
   VERIFY_HRESULT(invokeDispatch(DISPATCH_PROPERTYPUT, nullptr, idisp_,
                                 L"Visible", 1, visible));
LErrExit:
   return errorHR;
}

Error OfficeViewer::openFile(const std::wstring& path, IDispatch** pidispOut)
{
   Error errorHR = Success();
   HRESULT hr = S_OK;

   IDispatch *idispItems = nullptr;
   VERIFY_HRESULT(getIDispatchProp(idispApp(), collection_.c_str(), &idispItems));

   const WCHAR *strOpen = L"Open";
   DISPID dispidOpen;

   BSTR bstrFileName = SysAllocString(path.c_str());

   // COM requires that arguments are specified in reverse order
   DISPID argPos[2] = { readOnlyPos_, 0 };
   VARIANT args[2];
   VARIANT result;
   DISPPARAMS dparams;

   // ReadOnly argument
   args[0].vt = VT_BOOL;
   args[0].boolVal = true;

   // FileName argument
   args[1].vt = VT_BSTR;
   args[1].bstrVal = bstrFileName;

   VariantInit(&result);
   dparams.rgvarg = args;
   dparams.rgdispidNamedArgs = argPos;
   dparams.cArgs = 2;
   dparams.cNamedArgs = 2;
   VERIFY_HRESULT(idispItems->GetIDsOfNames(
                     IID_NULL, const_cast<WCHAR**>(&strOpen),  1,
                     LOCALE_USER_DEFAULT, &dispidOpen));
   VERIFY_HRESULT(idispItems->Invoke(
                     dispidOpen, IID_NULL, LOCALE_SYSTEM_DEFAULT,
                     DISPATCH_METHOD, &dparams, &result, nullptr, nullptr));
   if (pidispOut)
      *pidispOut = result.pdispVal;

LErrExit:

   // Release resources
   SysFreeString(bstrFileName);
   return errorHR;
}

IDispatch* OfficeViewer::idispApp()
{
   return idisp_;
}

// Given a path, searches for the item in the collection that
// has the path given. The out parameter is set to that item's IDispatch
// pointer, or NULL if no item with the path could be found.
Error OfficeViewer::getItemFromPath(const std::wstring& path, IDispatch** pidispOut)
{
   Error errorHR = Success();
   HRESULT hr = S_OK;

   IDispatch* idispItems = nullptr;
   IDispatch* idispDoc = nullptr;
   VARIANT varDocIdx;
   VARIANT varResult;
   int docCount = 0;

   *pidispOut = nullptr;

   // Count the number of items in the collection
   VERIFY_HRESULT(getIDispatchProp(idispApp(), collection_.c_str(), &idispItems));
   VERIFY_HRESULT(getIntProp(idispItems, L"Count", &docCount));

   VariantInit(&varDocIdx);
   varDocIdx.vt = VT_I4;
   for (int i = 1; i <= docCount; i++)
   {
      VariantInit(&varResult);
      varDocIdx.intVal = i;

      // Retrieve the next item
      VERIFY_HRESULT(invokeDispatch(DISPATCH_METHOD, &varResult, idispItems,
                                    L"Item", 1, varDocIdx));
      idispDoc = varResult.pdispVal;

      // Find its FullName (i.e. its path on disk)
      VERIFY_HRESULT(invokeDispatch(DISPATCH_PROPERTYGET, &varResult, idispDoc,
                                    L"FullName", 0));
      if (path.compare(varResult.bstrVal) == 0)
      {
         *pidispOut = idispDoc;
         break;
      }
   }

LErrExit:
   return errorHR;
}

std::wstring OfficeViewer::path()
{
   return path_;
}

void OfficeViewer::setPath(const std::wstring& path)
{
    path_ = path;
}

Error OfficeViewer::closeLastViewedItem()
{
   Error errorHR = Success();
   HRESULT hr = S_OK;

   if (idispApp() == nullptr)
      return Success();

   // Find the open item corresponding to the one we last rendered. If we find
   // it, close it; if we can't find it, do nothing.
   IDispatch* idispItem = nullptr;
   errorHR = getItemFromPath(path(), &idispItem);
   if (errorHR)
      return errorHR;
   if (idispItem == nullptr)
      return Success();

   errorHR = savePosition(idispItem);
   if (errorHR)
      LOG_ERROR(errorHR);

   // Close the item
   VERIFY_HRESULT(invokeDispatch(DISPATCH_METHOD, nullptr, idispItem, L"Close", 0));

LErrExit:
   return errorHR;
}

Error OfficeViewer::showItem(const std::wstring& item)
{
   HRESULT hr = S_OK;
   std::wstring itemPath = item;

   Error errorHR = ensureInterface();
   if (errorHR)
       return errorHR;

   // Make the application visible
   errorHR = showApp();
   if (errorHR)
      return errorHR;

   IDispatch* idispDoc;

   // Open the document
   std::replace(itemPath.begin(), itemPath.end(), '/', '\\');
   errorHR = openFile(itemPath, &idispDoc);
   if (errorHR)
      return errorHR;
   if (path() == itemPath)
   {
      // Reopening the last-opened doc: apply the position if we have one
      // cached
      if (hasPosition())
         restorePosition(idispDoc);
   }
   else
   {
      // Opening a different doc: forget scroll position and save the doc name
      resetPosition();
      setPath(itemPath);
   }

   // Bring the application to the foreground
   VERIFY_HRESULT(invokeDispatch(DISPATCH_METHOD, nullptr, idispApp(),
                                 L"Activate", 0));

LErrExit:
   return errorHR;
}
} // namespace rstudio
} // namespace desktop

