/*
 * DesktopComUtils.cpp
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

#include <windows.h>
#include <winuser.h>
#include <oleauto.h>

#include <boost/utility.hpp>
#include <boost/scoped_array.hpp>

#include "DesktopComUtils.hpp"

namespace rstudio {
namespace desktop {

// Invoke a method or property by name on the given IDispatch interface.
// Adapted from the standard automation AutoWrap helper function
// (see http://support.microsoft.com/kb/238393)
HRESULT invokeDispatch (int dispatchType, VARIANT *pvResult,
                        IDispatch *pDisp, const std::wstring& strName,
                        int cArgs...)
{
   va_list marker;
   va_start(marker, cArgs);

   DISPPARAMS dp = { nullptr, nullptr, 0, 0 };
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
      VARIANT varArg = va_arg(marker, VARIANT);
      spArgs[i] = varArg;
   }

   dp.cArgs = cArgs;
   dp.rgvarg = spArgs.get();

   // Handle special case: for property put, we need to use named args
   if (dispatchType & DISPATCH_PROPERTYPUT)
   {
      dp.cNamedArgs = 1;
      dp.rgdispidNamedArgs = &dispidNamed;
   }

   EXCEPINFO excep;
   hr = pDisp->Invoke(dispID, IID_NULL, LOCALE_SYSTEM_DEFAULT, dispatchType,
                      &dp, pvResult, &excep, nullptr);
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

} // namespace desktop
} // namespace rstudio

