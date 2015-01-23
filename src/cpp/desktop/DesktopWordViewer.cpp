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

#include <iostream>

#include <windows.h>
#include <winuser.h>
#include <oleauto.h>

#include <boost/utility.hpp>
#include <boost/scoped_array.hpp>

#include <core/Error.hpp>
#include <core/system/System.hpp>
#include <core/system/Environment.hpp>

#include "DesktopWordViewer.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace desktop {
namespace {

// Convenience to convert an HRSULT to our own error type and bail out on
// failure.
#define VERIFY_HRESULT(x) hr = (x); \
      if (FAILED(hr)) { \
         errorHR = Error( \
            boost::system::error_code(hr, boost::system::generic_category()), \
            ERROR_LOCATION); \
         goto LErrExit; \
      }

// Invoke a method or property by name on the given IDispatch interface.
// Adapted from the standard automation AutoWrap helper function
// (see http://support.microsoft.com/kb/238393)
HRESULT invokeDispatch (int dispatchType, VARIANT *pvResult,
                        IDispatch *pDisp, const std::wstring& strName,
                        int cArgs...)
{
   va_list marker;
   va_start(marker, cArgs);

   DISPPARAMS dp = { NULL, NULL, 0, 0 };
   DISPID dispidNamed = DISPID_PROPERTYPUT;
   DISPID dispID;
   HRESULT hr;

   LPOLESTR wstrName = const_cast<WCHAR*>(strName.c_str());
   hr = pDisp->GetIDsOfNames(IID_NULL, &wstrName, 1, LOCALE_USER_DEFAULT,
                             &dispID);
   if (FAILED(hr))
      return hr;

   // Create the argument list
   boost::scoped_array<VARIANT> spArgs(new VARIANT[cArgs + 1]);
   for (int i = 0; i < cArgs; i++)
   {
      spArgs[i] = va_arg(marker, VARIANT);
   }

   dp.cArgs = cArgs;
   dp.rgvarg = spArgs.get();

   // Handle special case: for property put, we need to use named args
   if (dispatchType & DISPATCH_PROPERTYPUT)
   {
      dp.cNamedArgs = 1;
      dp.rgdispidNamedArgs = &dispidNamed;
   }

   hr = pDisp->Invoke(dispID, IID_NULL, LOCALE_SYSTEM_DEFAULT, dispatchType,
                      &dp, pvResult, NULL, NULL);
   va_end(marker);
   return hr;
}

// Given an IDispatch pointer and a name, return an IDispatch pointer to the
// object property with the given name
HRESULT getIDispatchProp(IDispatch* idispIn, const std::wstring& strName,
                         IDispatch** pidispOut)
{
   VARIANT result;
   VariantInit(&result);
   HRESULT hr = invokeDispatch(DISPATCH_PROPERTYGET, &result,
                               idispIn, strName, 0);
   if (FAILED(hr))
      return hr;
   *pidispOut = result.pdispVal;
   return hr;
}

HRESULT getIntProp(IDispatch* idisp, const std::wstring& strName, int* pOut)
{
   VARIANT result;
   VariantInit(&result);
   HRESULT hr = invokeDispatch(DISPATCH_PROPERTYGET, &result,
                               idisp, strName, 0);
   if (FAILED(hr))
      return hr;
   *pOut = result.intVal;
   return hr;
}

} // anonymous namespace

WordViewer::WordViewer():
   idispWord_(NULL),
   docScrollX_(0),
   docScrollY_(0)
{
}

WordViewer::~WordViewer()
{
   try
   {
      if (idispWord_)
         idispWord_->Release();
   }
   catch (...)
   {
      // Ignore exceptions during teardown
   }
}

Error WordViewer::showDocument(QString& path)
{
   Error errorHR = Success();
   HRESULT hr = S_OK;

   // Allow Word to become the foreground window. CoAllowSetForegroundWindow
   // would be preferable here, since we'd be able to restrict activation to
   // only the process we started, but it is not exposed by MinGW headers.
   // Note that AllowSetForegroundWindow already limits activation to processes
   // initiated by the foreground process, and self-expires on user input.
   AllowSetForegroundWindow(ASFW_ANY);

   // If we have an active IDispatch pointer to Word, check to see whether
   // it has been closed
   if (idispWord_ != NULL)
   {
      // Test the interface by looking up a known DISPID
      const WCHAR* wstrQuit = L"Quit";
      DISPID dispid;
      hr = idispWord_->GetIDsOfNames(IID_NULL, const_cast<WCHAR**>(&wstrQuit),
                                     1, LOCALE_USER_DEFAULT, &dispid);

      // If the lookup fails, release this IDispatch pointer--it's stale.
      // We'll CoCreate a new instance of Word below.
      if (FAILED(hr) &&
          SCODE_CODE(hr) == RPC_S_SERVER_UNAVAILABLE)
      {
         idispWord_->Release();
         idispWord_ = NULL;
      }
   }

   // Get an IDispatch for the Word Application root object
   if (idispWord_ == NULL)
   {
      CLSID clsid;
      LPCOLESTR progId = L"Word.Application";
      CoInitialize(NULL);
      VERIFY_HRESULT(CLSIDFromProgID(progId, &clsid));
      VERIFY_HRESULT(CoCreateInstance(clsid, NULL, CLSCTX_LOCAL_SERVER,
                                      IID_IDispatch,
                                      reinterpret_cast<void**>(&idispWord_)));
      idispWord_->AddRef();
   }

   // Make Word visible
   errorHR = showWord();
   if (errorHR)
      return errorHR;

   IDispatch* idispDocs;
   IDispatch* idispDoc;
   VERIFY_HRESULT(getIDispatchProp(idispWord_, L"Documents", &idispDocs));

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
   VERIFY_HRESULT(invokeDispatch(DISPATCH_METHOD, NULL, idispWord_,
                                 L"Activate", 0));

LErrExit:
   return errorHR;
}

Error WordViewer::showWord()
{
   Error errorHR = Success();
   HRESULT hr = S_OK;

   VARIANT visible;
   visible.vt = VT_BOOL;
   visible.boolVal = true;
   VERIFY_HRESULT(invokeDispatch(DISPATCH_PROPERTYPUT, NULL, idispWord_,
                                 L"Visible", 1, visible));
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
                     DISPATCH_METHOD, &dparams, &result, NULL, NULL));
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

   if (idispWord_ == NULL)
      return Success();

   // Find the open document corresponding to the one we last rendered. If we
   // find it, close it; if we can't find it, do nothing.
   IDispatch* idispDoc = NULL;
   errorHR = getDocumentByPath(docPath_, &idispDoc);
   if (errorHR)
      return errorHR;
   if (idispDoc == NULL)
      return Success();

   errorHR = getDocumentPosition(idispDoc, &docScrollX_, &docScrollY_);
   if (errorHR)
      LOG_ERROR(errorHR);

   VERIFY_HRESULT(invokeDispatch(DISPATCH_METHOD, NULL, idispDoc, L"Close", 0));

LErrExit:
   return errorHR;
}

Error WordViewer::getDocumentPosition(IDispatch* idispDoc, int* pxPos,
                                      int* pyPos)
{
   Error errorHR = Success();
   HRESULT hr = S_OK;

   IDispatch* idispWindow = NULL;

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

   IDispatch* idispWindow = NULL;
   VARIANT varPos;
   varPos.vt = VT_INT;

   VERIFY_HRESULT(getIDispatchProp(idispDoc, L"ActiveWindow", &idispWindow));
   varPos.intVal = xPos;
   VERIFY_HRESULT(invokeDispatch(DISPATCH_PROPERTYPUT, NULL, idispWindow,
                                 L"HorizontalPercentScrolled", 1, varPos));
   varPos.intVal = yPos;
   VERIFY_HRESULT(invokeDispatch(DISPATCH_PROPERTYPUT, NULL, idispWindow,
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

   IDispatch* idispDocs = NULL;
   IDispatch* idispDoc = NULL;
   VARIANT varDocIdx;
   VARIANT varResult;
   int docCount = 0;

   *pidispDoc = NULL;

   VERIFY_HRESULT(getIDispatchProp(idispWord_, L"Documents", &idispDocs));
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
