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

// JavaObject.h : Declaration of the CJavaObject

#pragma once
#include "resource.h"       // main symbols
#include "SessionData.h"

#include "oophm_i.h"


#if defined(_WIN32_WCE) && !defined(_CE_DCOM) && !defined(_CE_ALLOW_SINGLE_THREADED_OBJECTS_IN_MTA)
#error "Single-threaded COM objects are not properly supported on Windows CE platform, such as the Windows Mobile platforms that do not include full DCOM support. Define _CE_ALLOW_SINGLE_THREADED_OBJECTS_IN_MTA to force ATL to support creating single-thread COM object's and allow use of it's single-threaded COM object implementations. The threading model in your rgs file was set to 'Free' as that is the only threading model supported in non DCOM Windows CE platforms."
#endif



// CJavaObject

class ATL_NO_VTABLE CJavaObject :
	public CComObjectRootEx<CComSingleThreadModel>,
	public CComCoClass<CJavaObject, &CLSID_JavaObject>,
	public IDispatchImpl<IJavaObject, &IID_IJavaObject, &LIBID_oophmLib, /*wMajor =*/ 1, /*wMinor =*/ 0>
{
public:
  // TODO How can the default constructor be gotten rid of?
  CJavaObject() : objId(-1) {
  }

	STDMETHOD(GetIDsOfNames)(REFIID riid, LPOLESTR* rgszNames, UINT cNames,
		LCID lcid, DISPID* rgdispid);

	STDMETHOD(Invoke)(DISPID dispidMember, REFIID riid,
		LCID lcid, WORD wFlags, DISPPARAMS* pdispparams, VARIANT* pvarResult,
		EXCEPINFO* pexcepinfo, UINT* puArgErr);

DECLARE_REGISTRY_RESOURCEID(IDR_JAVAOBJECT)


BEGIN_COM_MAP(CJavaObject)
	COM_INTERFACE_ENTRY(IJavaObject)
  COM_INTERFACE_ENTRY(IDispatchEx)
  COM_INTERFACE_ENTRY(IDispatch)
END_COM_MAP()



	DECLARE_PROTECT_FINAL_CONSTRUCT()

	HRESULT FinalConstruct()
	{
		return S_OK;
	}

	void FinalRelease()
	{
    Debug::log(Debug::Debugging) << "JavaObject " << objId << " released" << Debug::flush;
    if (sessionData) {
      // After shutdown, the session data will have been torn down
      sessionData->freeJavaObject(objId);
    }
	}

  unsigned int getObjectId() const {
    return objId;
  }

  STDMETHOD(GetDispID)(BSTR,DWORD,DISPID *);
  STDMETHOD(InvokeEx)(DISPID,LCID,WORD,DISPPARAMS *,VARIANT *,EXCEPINFO *,IServiceProvider *);
  STDMETHOD(DeleteMemberByName)(BSTR,DWORD);
  STDMETHOD(DeleteMemberByDispID)(DISPID);
  STDMETHOD(GetMemberProperties)(DISPID,DWORD,DWORD *);
  STDMETHOD(GetMemberName)(DISPID,BSTR *);
  STDMETHOD(GetNextDispID)(DWORD,DISPID,DISPID *);
  STDMETHOD(GetNameSpaceParent)(IUnknown **);

  void initialize(unsigned int objId, SessionDataRef sessionData) {
    this->objId = objId;
    this->sessionData = sessionData;
  }

  void shutdown() {
    sessionData = NULL;
  }

private:
  unsigned int objId;
  SessionDataRef sessionData;
};

OBJECT_ENTRY_AUTO(__uuidof(JavaObject), CJavaObject)
