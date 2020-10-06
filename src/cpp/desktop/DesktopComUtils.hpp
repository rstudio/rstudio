/*
 * DesktopComUtils.hpp
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

#ifndef DESKTOP_COMUTILS_HPP
#define DESKTOP_COMUTILS_HPP

#include <string>

// Convenience to convert an HRSULT to our own error type and bail out on
// failure.
#define VERIFY_HRESULT(x) hr = (x); \
      if (FAILED(hr)) { \
         errorHR = Error( \
            boost::system::error_code(hr, boost::system::generic_category()), \
            ERROR_LOCATION); \
         goto LErrExit; \
      }

namespace rstudio {
namespace desktop {

HRESULT invokeDispatch (int dispatchType, VARIANT *pvResult,
                        IDispatch *pDisp, const std::wstring& strName,
                        int cArgs...);

HRESULT getIDispatchProp(IDispatch* idispIn, const std::wstring& strName,
                         IDispatch** pidispOut);

HRESULT getIntProp(IDispatch* idisp, const std::wstring& strName, int* pOut);

} // namespace desktop
} // namespace rstudio

#endif
