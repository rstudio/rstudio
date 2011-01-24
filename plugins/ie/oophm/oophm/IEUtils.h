/*
* Copyright 2008 Google Inc.
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy of
* the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations under
* the License.
*/

#pragma once
#include "stdafx.h"
#include "comutil.h"
#include "dispex.h"
#include "SessionData.h"

#define SYSLOGERROR(MSG,FMT,...) \
    LPCWSTR msgArr[3] = {NULL, NULL, NULL}; \
    msgArr[0] = MSG; \
    WCHAR buffer1[512]; \
    swprintf(buffer1, sizeof(buffer1)/sizeof(WCHAR), FMT, __VA_ARGS__); \
    msgArr[1] = buffer1; \
    WCHAR buffer2[512]; \
    swprintf(buffer2, sizeof(buffer2)/sizeof(WCHAR), L"function: %S, file: %S, line: %d", __FUNCTION__, __FILE__, __LINE__); \
    msgArr[2] = buffer2; \
    IEUtils::WriteToLog((LPCWSTR*)msgArr, 3);


typedef HRESULT (*PFNRESOLVENAME)(IDispatch*, LPOLESTR, DISPID*);

//
// This class is a collection of helper methods specific to IE
// It finds the appropriate implementation that resolves javascript
// names regardless of the specific documentMode that browser is 
// running. 
//
class IEUtils
{
    static HANDLE hEventLog;
    static LPWSTR logSourceName;
    static PFNRESOLVENAME pfnResolveName;

    //
    // finds which IDispatch interface is capable of
    // of 'resolving' names.
    //
    static PFNRESOLVENAME getResolveNameFunction(IDispatch* obj)
    {
        _variant_t retVal;
        std::string probeScript("function _FN3E9738B048214100A6D6B750F2230A34() { return null; }");
        CComQIPtr<IHTMLWindow2> spWindow2(obj);
        if (!spWindow2) {
            return &IEUtils::internalResolveNameEx;
        }
        LPOLESTR functionName = L"_FN3E9738B048214100A6D6B750F2230A34";
        HRESULT hr = spWindow2->execScript(UTF8ToBSTR(probeScript.length(), probeScript.c_str()),
            UTF8ToBSTR(10, "JavaScript"), retVal.GetAddress());
        if (SUCCEEDED(hr)) {
            DISPID dispId;
            hr = internalResolveName(spWindow2, functionName, &dispId);
            if (SUCCEEDED(hr)) {
                return &IEUtils::internalResolveName;
            } else {
                hr = internalResolveNameEx(spWindow2, functionName, &dispId);
                if (SUCCEEDED(hr)) {
                    return &IEUtils::internalResolveNameEx;
                } else {
                    SYSLOGERROR(L"Failed to find a IDispatch Implementation able to resolve names",
                        L"hr=0x%08x", hr);
                }
            }
        }
        return &IEUtils::internalResolveNameEx;
    }

    //
    // resolves 'name' using default IDispatch interface
    //
    static HRESULT internalResolveName(IDispatch* obj, LPOLESTR name, DISPID *dispID)
    {
        assert(obj != NULL);
        return obj->GetIDsOfNames(IID_NULL, &name, 1, LOCALE_SYSTEM_DEFAULT, dispID);
    }

    //
    // resolves 'name' using IDispatchEx interface
    //
    static HRESULT internalResolveNameEx(IDispatch* obj, LPOLESTR name, DISPID *dispID)
    {
        assert(obj != NULL);
        CComQIPtr<IDispatchEx> spDispEx(obj);
        if (!spDispEx) {
            return E_FAIL;
        }
        return spDispEx->GetIDsOfNames(IID_NULL, &name, 1, LOCALE_SYSTEM_DEFAULT, dispID);
    }

public:

    static void InitEventLog() {
        if (NULL == hEventLog) {
            hEventLog = OpenEventLog(NULL, IEUtils::logSourceName);
        }
    }

    static void WriteToLog(LPCWSTR* rgMsg, INT size) {
        if (NULL != hEventLog) {
            ReportEvent(hEventLog, EVENTLOG_ERROR_TYPE, 0, 0, NULL, size, 0, rgMsg, NULL);
        }
    }

    static void CloseEventLog()
    {
        if (NULL != hEventLog) {
            ::CloseEventLog(hEventLog);
        }
    }

    static WCHAR* GetSysErrorMessage(DWORD dwErrorCode)
    {
        WCHAR * pMsgBuf = NULL;
        DWORD dwSize = FormatMessage(FORMAT_MESSAGE_FROM_SYSTEM |
            FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_IGNORE_INSERTS,
            NULL, dwErrorCode, 0, (LPTSTR) &pMsgBuf, 0, NULL);
        if (dwSize) {
            return pMsgBuf;
        }
        return NULL;
    }

    static HRESULT resolveName(IDispatch* obj, LPOLESTR name, DISPID *dispID)
    {
        if (NULL == pfnResolveName) {
            pfnResolveName = getResolveNameFunction(obj);
        }
        assert(NULL != pfnResolveName);
        return pfnResolveName(obj, name, dispID);
    }

    static HRESULT resolveName(IDispatch* obj, std::string name, DISPID *dispID)
    {
        return resolveName(obj, UTF8ToBSTR(name.length(), name.c_str()), dispID);
    }

    static void resetResolver()
    {
        pfnResolveName = NULL;
    }

    static HRESULT Invoke(IUnknown* obj,
        DISPID id,
        WORD wFlags,
        DISPPARAMS *pdp,
        VARIANT *pvarRes,
        EXCEPINFO *pei,
        UINT *puArgErr)
    {
        HRESULT hr = S_OK;
        CComQIPtr<IDispatchEx> spDispEx(obj);
        if (!spDispEx) {
            return E_FAIL;
        }
        hr = spDispEx->Invoke(id, IID_NULL, LOCALE_SYSTEM_DEFAULT, wFlags, pdp, pvarRes, pei, puArgErr);
        return hr;
    }
};

__declspec(selectany) HANDLE IEUtils::hEventLog;
__declspec(selectany) LPWSTR IEUtils::logSourceName = L"GWT Developer Mode Plugin";
__declspec(selectany) HRESULT (*IEUtils::pfnResolveName)(IDispatch* object, LPOLESTR name, DISPID *dispID);


