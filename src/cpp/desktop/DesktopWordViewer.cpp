/*
 * DesktopWordViewer.cpp
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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

#include <windows.h>
#include <oleauto.h>

#include <boost/utility.hpp>

#include <core/Error.hpp>
#include <core/system/System.hpp>
#include <core/system/Environment.hpp>

#include "DesktopWordViewer.hpp"

using namespace core;
namespace desktop {

// Convenience to convert an HRSULT to our own error type and bail out on
// failure.
#define VERIFY_HRESULT(x) hr = (x); \
      if (FAILED(hr)) { \
         errorHR = Error( \
            boost::system::error_code(hr, boost::system::generic_category()), \
            ERROR_LOCATION); \
         goto LErrExit; \
      }

WordViewer::WordViewer():
   idispWord_(NULL),
   idispDocs_(NULL),
   idispCurrentDoc_(NULL)
{
}

WordViewer::~WordViewer()
{
   if (idispWord_)
      idispWord_->Release();
   if (idispDocs_)
      idispDocs_->Release();
   if (idispCurrentDoc_)
      idispDocs_->Release();
}

Error WordViewer::showDocument(QString& path)
{
   Error errorHR = Success();
   HRESULT hr = S_OK;

   // if there's already a doc open, close it--we only maintain state for
   // one doc at a time
   if (idispCurrentDoc_ != NULL)
   {
      errorHR = closeActiveDocument();
      if (errorHR)
         return errorHR;
   }

   // Get an IDispatch for the Word Application root object
   if (idispWord_ == NULL)
   {
      CLSID clsid;
      LPCOLESTR progId = L"Word.Application";
      VERIFY_HRESULT(CLSIDFromProgID(progId, &clsid));
      VERIFY_HRESULT(CoCreateInstance(clsid, NULL, CLSCTX_LOCAL_SERVER,
                                      IID_IDispatch,
                                      reinterpret_cast<void**>(&idispWord_)));
      idispWord_->AddRef();
   }

   // Get an IDispatch for the Documents collection
   if (idispDocs_ == NULL)
   {
      const WCHAR *strDocuments = L"Documents";
      DISPID dispidDocuments;
      DISPPARAMS dparams = { NULL, NULL, 0, 0 };
      VARIANT result;
      VERIFY_HRESULT(idispWord_->GetIDsOfNames(
                        IID_NULL, const_cast<WCHAR**>(&strDocuments), 1,
                        LOCALE_USER_DEFAULT, &dispidDocuments));
      VariantInit(&result);
      VERIFY_HRESULT(idispWord_->Invoke(dispidDocuments, IID_NULL,
                        LOCALE_SYSTEM_DEFAULT, DISPATCH_PROPERTYGET, &dparams,
                        &result, NULL, NULL));
      idispDocs_ = result.pdispVal;
      idispDocs_->AddRef();
   }

   // Make Word visible
   errorHR = showWord();
   if (errorHR)
      return errorHR;

   // Open the documenet
   return openDocument(path);

LErrExit:
   return errorHR;
}

Error WordViewer::showWord()
{
   Error errorHR = Success();
   HRESULT hr = S_OK;

   const WCHAR *strVisible = L"Visible";
   DISPID dispidVisible;
   DISPID propPut = DISPID_PROPERTYPUT;
   DISPPARAMS visibleParams;
   VARIANT visible;
   VARIANT result;
   VariantInit(&result);
   visible.vt = VT_BOOL;
   visible.boolVal = true;
   visibleParams.rgvarg = &visible;
   visibleParams.rgdispidNamedArgs = &propPut;
   visibleParams.cArgs = 1;
   visibleParams.cNamedArgs = 1;
   VERIFY_HRESULT(idispWord_->GetIDsOfNames(
                     IID_NULL, const_cast<WCHAR**>(&strVisible), 1,
                     LOCALE_USER_DEFAULT, &dispidVisible));
   VERIFY_HRESULT(idispWord_->Invoke(
                     dispidVisible, IID_NULL, LOCALE_SYSTEM_DEFAULT,
                     DISPATCH_PROPERTYPUT, &visibleParams, &result,
                     NULL, NULL));
LErrExit:
   return errorHR;
}

Error WordViewer::openDocument(QString& path)
{
   Error errorHR = Success();
   HRESULT hr = S_OK;

   const WCHAR *strOpen = L"Open";
   DISPID dispidOpen;
   BSTR bstrFileName =
         SysAllocString(reinterpret_cast<const WCHAR*>(
                           path.replace(QChar(L'/'), QChar(L'\\')).utf16()));

   // COM requires that arguments are specified in reverse order
   DISPID argPos[2] = { 2, 0 };
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
   VERIFY_HRESULT(idispDocs_->GetIDsOfNames(
                     IID_NULL, const_cast<WCHAR**>(&strOpen),  1,
                     LOCALE_USER_DEFAULT, &dispidOpen));
   VERIFY_HRESULT(idispDocs_->Invoke(
                     dispidOpen, IID_NULL, LOCALE_SYSTEM_DEFAULT,
                     DISPATCH_METHOD, &dparams, &result, NULL, NULL));

   // Release the current document if we have one
   if (idispCurrentDoc_ != NULL)
      idispCurrentDoc_->Release();

   idispCurrentDoc_ = result.pdispVal;
   idispCurrentDoc_->AddRef();

LErrExit:

   // Release resources
   SysFreeString(bstrFileName);
   return errorHR;
}

Error WordViewer::closeActiveDocument()
{
   if (idispCurrentDoc_ == NULL)
      return Success();

   Error errorHR = Success();
   HRESULT hr = S_OK;
   DISPID dispidClose;
   DISPPARAMS dparams = { NULL, NULL, 0, 0 };

   // Consider: should we pick up the document's scroll position here, so
   // we can apply it later?

   const WCHAR *strClose = L"Close";
   VERIFY_HRESULT(idispCurrentDoc_->GetIDsOfNames(
                     IID_NULL, const_cast<WCHAR**>(&strClose), 1,
                     LOCALE_USER_DEFAULT, &dispidClose));
   VERIFY_HRESULT(idispCurrentDoc_->Invoke(
                     dispidClose, IID_NULL, LOCALE_SYSTEM_DEFAULT,
                     DISPATCH_METHOD, &dparams, NULL, NULL, NULL));
   idispCurrentDoc_->Release();
   idispCurrentDoc_ = NULL;

LErrExit:
   return errorHR;
}

} // namespace desktop
