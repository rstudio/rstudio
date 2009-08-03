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

// ExceptionCatcher.h : Declaration of the CExceptionCatcher

#pragma once
#include "resource.h"       // main symbols
#include "comutil.h"
#include "dispex.h"
#include "oophm_i.h"


#if defined(_WIN32_WCE) && !defined(_CE_DCOM) && !defined(_CE_ALLOW_SINGLE_THREADED_OBJECTS_IN_MTA)
#error "Single-threaded COM objects are not properly supported on Windows CE platform, such as the Windows Mobile platforms that do not include full DCOM support. Define _CE_ALLOW_SINGLE_THREADED_OBJECTS_IN_MTA to force ATL to support creating single-thread COM object's and allow use of it's single-threaded COM object implementations. The threading model in your rgs file was set to 'Free' as that is the only threading model supported in non DCOM Windows CE platforms."
#endif



// CExceptionCatcher

class ATL_NO_VTABLE CExceptionCatcher :
	public CComObjectRootEx<CComSingleThreadModel>,
	public CComCoClass<CExceptionCatcher, &CLSID_ExceptionCatcher>,
  public ICanHandleException,
  public IServiceProvider,
	public IDispatchImpl<IExceptionCatcher, &IID_IExceptionCatcher, &LIBID_oophmLib, /*wMajor =*/ 1, /*wMinor =*/ 0>
{
public:
	CExceptionCatcher()
	{
	}

DECLARE_REGISTRY_RESOURCEID(IDR_EXCEPTIONCATCHER)


BEGIN_COM_MAP(CExceptionCatcher)
  COM_INTERFACE_ENTRY(ICanHandleException)
  COM_INTERFACE_ENTRY(IServiceProvider)
	COM_INTERFACE_ENTRY(IExceptionCatcher)
	COM_INTERFACE_ENTRY(IDispatch)
END_COM_MAP()



	DECLARE_PROTECT_FINAL_CONSTRUCT()

	HRESULT FinalConstruct()
	{
    hasCaughtException = false;
		return S_OK;
	}

	void FinalRelease()
	{
	}

public:
  STDMETHOD(getException)(VARIANT* retVal);
  STDMETHOD(hasSeenException)(BOOL* ret);
  STDMETHOD(CanHandleException)(EXCEPINFO* exInfo, VARIANT* value);
  STDMETHOD(QueryService)(const GUID& guidService, const IID& riid, void** ret);
private:
  _variant_t caughtException;
  bool hasCaughtException;
};

OBJECT_ENTRY_AUTO(__uuidof(ExceptionCatcher), CExceptionCatcher)
