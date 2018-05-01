/*
 * DesktopWordViewer.cpp
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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

#include <boost/utility.hpp>
#include <boost/scoped_array.hpp>

#include <core/Error.hpp>
#include <core/system/System.hpp>
#include <core/system/Environment.hpp>

#include "DesktopComUtils.hpp"
#include "DesktopWordViewer.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace desktop {

WordViewer::WordViewer():
   OfficeViewer(L"Word.Application"),
   docScrollX_(0),
   docScrollY_(0)
{
}

Error WordViewer::showDocument(QString& path)
{
   Error errorHR = Success();
   HRESULT hr = S_OK;

   errorHR = ensureInterface();
   if (errorHR)
       return errorHR;

   // Make Word visible
   errorHR = showApp();
   if (errorHR)
      return errorHR;

   IDispatch* idispDocs;
   IDispatch* idispDoc;
   VERIFY_HRESULT(getIDispatchProp(idispApp(), L"Documents", &idispDocs));

   // Open the documenet
   path = path.replace(QChar(L'/'), QChar(L'\\'));
   errorHR = openDocument(path, idispDocs, &idispDoc);
   if (errorHR)
      return errorHR;
   if (docPath_ == path)
   {
      // Reopening the last-opened doc: apply the scroll position if we have
      // one cached
      if (docScrollX_ > 0 || docScrollY_ > 0)
         setDocumentPosition(idispDoc, docScrollX_, docScrollY_);
   }
   else
   {
      // Opening a different doc: forget scroll position and save the doc name
      docScrollX_ = 0;
      docScrollY_ = 0;
      docPath_ = path;
   }

   // Bring Word to the foreground
   VERIFY_HRESULT(invokeDispatch(DISPATCH_METHOD, nullptr, idispApp(),
                                 L"Activate", 0));

LErrExit:
   return errorHR;
}

Error WordViewer::openDocument(QString& path, IDispatch* idispDocs,
                               IDispatch** pidispDoc)
{
   Error errorHR = Success();
   HRESULT hr = S_OK;

   const WCHAR *strOpen = L"Open";
   DISPID dispidOpen;

   BSTR bstrFileName =
         SysAllocString(reinterpret_cast<const WCHAR*>(path.utf16()));

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
   VERIFY_HRESULT(idispDocs->GetIDsOfNames(
                     IID_NULL, const_cast<WCHAR**>(&strOpen),  1,
                     LOCALE_USER_DEFAULT, &dispidOpen));
   VERIFY_HRESULT(idispDocs->Invoke(
                     dispidOpen, IID_NULL, LOCALE_SYSTEM_DEFAULT,
                     DISPATCH_METHOD, &dparams, &result, nullptr, nullptr));
   if (pidispDoc)
      *pidispDoc = result.pdispVal;

LErrExit:

   // Release resources
   SysFreeString(bstrFileName);
   return errorHR;
}

Error WordViewer::closeLastViewedDocument()
{
   Error errorHR = Success();
   HRESULT hr = S_OK;

   if (idispApp() == nullptr)
      return Success();

   // Find the open document corresponding to the one we last rendered. If we
   // find it, close it; if we can't find it, do nothing.
   IDispatch* idispDoc = nullptr;
   errorHR = getDocumentByPath(docPath_, &idispDoc);
   if (errorHR)
      return errorHR;
   if (idispDoc == nullptr)
      return Success();

   errorHR = getDocumentPosition(idispDoc, &docScrollX_, &docScrollY_);
   if (errorHR)
      LOG_ERROR(errorHR);

   VERIFY_HRESULT(invokeDispatch(DISPATCH_METHOD, nullptr, idispDoc, L"Close", 0));

LErrExit:
   return errorHR;
}

Error WordViewer::getDocumentPosition(IDispatch* idispDoc, int* pxPos,
                                      int* pyPos)
{
   Error errorHR = Success();
   HRESULT hr = S_OK;

   IDispatch* idispWindow = nullptr;

   VERIFY_HRESULT(getIDispatchProp(idispDoc, L"ActiveWindow", &idispWindow));
   VERIFY_HRESULT(getIntProp(idispWindow, L"HorizontalPercentScrolled", pxPos));
   VERIFY_HRESULT(getIntProp(idispWindow, L"VerticalPercentScrolled", pyPos));

LErrExit:
   return errorHR;
}

Error WordViewer::setDocumentPosition(IDispatch* idispDoc, int xPos, int yPos)
{
   Error errorHR = Success();
   HRESULT hr = S_OK;

   IDispatch* idispWindow = nullptr;
   VARIANT varPos;
   varPos.vt = VT_INT;

   VERIFY_HRESULT(getIDispatchProp(idispDoc, L"ActiveWindow", &idispWindow));
   varPos.intVal = xPos;
   VERIFY_HRESULT(invokeDispatch(DISPATCH_PROPERTYPUT, nullptr, idispWindow,
                                 L"HorizontalPercentScrolled", 1, varPos));
   varPos.intVal = yPos;
   VERIFY_HRESULT(invokeDispatch(DISPATCH_PROPERTYPUT, nullptr, idispWindow,
                                 L"VerticalPercentScrolled", 1, varPos));
LErrExit:
   return errorHR;
}

// Given a path, searches for the document in the Documents collection that
// has the path given. The out parameter is set to that document's IDispatch
// pointer, or NULL if no document with the path could be found.
Error WordViewer::getDocumentByPath(QString& path, IDispatch** pidispDoc)
{
   Error errorHR = Success();
   HRESULT hr = S_OK;

   IDispatch* idispDocs = nullptr;
   IDispatch* idispDoc = nullptr;
   VARIANT varDocIdx;
   VARIANT varResult;
   int docCount = 0;

   *pidispDoc = nullptr;

   VERIFY_HRESULT(getIDispatchProp(idispApp(), L"Documents", &idispDocs));
   VERIFY_HRESULT(getIntProp(idispDocs, L"Count", &docCount));

   varDocIdx.vt = VT_INT;
   for (int i = 1; i <= docCount; i++)
   {
      VariantInit(&varResult);
      varDocIdx.intVal = i;
      VERIFY_HRESULT(invokeDispatch(DISPATCH_METHOD, &varResult, idispDocs,
                                    L"Item", 1, varDocIdx));
      idispDoc = varResult.pdispVal;
      VERIFY_HRESULT(invokeDispatch(DISPATCH_PROPERTYGET, &varResult, idispDoc,
                                    L"FullName", 0));
      if (path.toStdWString() == varResult.bstrVal)
      {
         *pidispDoc = idispDoc;
         break;
      }
   }

LErrExit:
   return errorHR;
}

} // namespace desktop
} // namespace rstudio
