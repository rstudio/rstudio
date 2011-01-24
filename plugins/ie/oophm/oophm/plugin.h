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

// plugin.h : Declaration of the Cplugin
#pragma once
#include "resource.h"       // main symbols
#include <atlctl.h>
#include "oophm_i.h"
#include "Debug.h"
#include "IESessionHandler.h"
#include "IEUtils.h"

#ifdef _WIN32_WCE
#error "ATL does not support HTML controls for Windows CE."
#endif
class ATL_NO_VTABLE CpluginUI :
    public IDispatchImpl<IpluginUI, &IID_IpluginUI, &LIBID_oophmLib, /*wMajor =*/ 1, /*wMinor =*/ 0>,
    public CComObjectRootEx<CComSingleThreadModel>
{
    BEGIN_COM_MAP(CpluginUI)
        COM_INTERFACE_ENTRY(IpluginUI)
        COM_INTERFACE_ENTRY(IDispatch)
    END_COM_MAP()
    // Iplugin
public:
    DECLARE_PROTECT_FINAL_CONSTRUCT()

    HRESULT FinalConstruct()
    {
        return S_OK;
    }

    void FinalRelease()
    {
    }

    // Example method called by the HTML to change the <BODY> background color
    STDMETHOD(OnClick)(IDispatch* pdispBody, VARIANT varColor)
    {
        CComQIPtr<IHTMLBodyElement> spBody(pdispBody);
        if (spBody != NULL)
            spBody->put_bgColor(varColor);
        return S_OK;
    }
};



// Cplugin
class ATL_NO_VTABLE Cplugin :
    public CComObjectRootEx<CComSingleThreadModel>,
    public IDispatchImpl<Iplugin, &IID_Iplugin, &LIBID_oophmLib, /*wMajor =*/ 1, /*wMinor =*/ 0>,
    public IPersistStreamInitImpl<Cplugin>,
    public IOleControlImpl<Cplugin>,
    public IOleObjectImpl<Cplugin>,
    public IOleInPlaceActiveObjectImpl<Cplugin>,
    public IViewObjectExImpl<Cplugin>,
    public IOleInPlaceObjectWindowlessImpl<Cplugin>,
    public ISupportErrorInfo,
    public IPersistStorageImpl<Cplugin>,
    public ISpecifyPropertyPagesImpl<Cplugin>,
    public IQuickActivateImpl<Cplugin>,
    public IObjectSafetyImpl<Cplugin, INTERFACESAFE_FOR_UNTRUSTED_CALLER | INTERFACESAFE_FOR_UNTRUSTED_DATA>,
#ifndef _WIN32_WCE
    public IDataObjectImpl<Cplugin>,
#endif
    public IProvideClassInfo2Impl<&CLSID_plugin, NULL, &LIBID_oophmLib>,
#ifdef _WIN32_WCE // IObjectSafety is required on Windows CE for the control to be loaded correctly
    public IObjectSafetyImpl<Cplugin, INTERFACESAFE_FOR_UNTRUSTED_CALLER>,
#endif
    public CComCoClass<Cplugin, &CLSID_plugin>,
    public CComControl<Cplugin>
{
public:


    Cplugin()
    {
        m_bWindowOnly = TRUE;
    }

    DECLARE_OLEMISC_STATUS(OLEMISC_RECOMPOSEONRESIZE |
    OLEMISC_CANTLINKINSIDE |
        OLEMISC_INSIDEOUT |
        OLEMISC_ACTIVATEWHENVISIBLE |
        OLEMISC_SETCLIENTSITEFIRST
        )

        DECLARE_REGISTRY_RESOURCEID(IDR_PLUGIN)


    BEGIN_COM_MAP(Cplugin)
        COM_INTERFACE_ENTRY(Iplugin)
        COM_INTERFACE_ENTRY(IDispatch)
        COM_INTERFACE_ENTRY(IViewObjectEx)
        COM_INTERFACE_ENTRY(IViewObject2)
        COM_INTERFACE_ENTRY(IViewObject)
        COM_INTERFACE_ENTRY(IOleInPlaceObjectWindowless)
        COM_INTERFACE_ENTRY(IOleInPlaceObject)
        COM_INTERFACE_ENTRY2(IOleWindow, IOleInPlaceObjectWindowless)
        COM_INTERFACE_ENTRY(IOleInPlaceActiveObject)
        COM_INTERFACE_ENTRY(IOleControl)
        COM_INTERFACE_ENTRY(IOleObject)
        COM_INTERFACE_ENTRY(IPersistStreamInit)
        COM_INTERFACE_ENTRY2(IPersist, IPersistStreamInit)
        COM_INTERFACE_ENTRY(ISupportErrorInfo)
        COM_INTERFACE_ENTRY(ISpecifyPropertyPages)
        COM_INTERFACE_ENTRY(IQuickActivate)
        COM_INTERFACE_ENTRY(IPersistStorage)
        COM_INTERFACE_ENTRY(IObjectSafety)
#ifndef _WIN32_WCE
        COM_INTERFACE_ENTRY(IDataObject)
#endif
        COM_INTERFACE_ENTRY(IProvideClassInfo)
        COM_INTERFACE_ENTRY(IProvideClassInfo2)
#ifdef _WIN32_WCE // IObjectSafety is required on Windows CE for the control to be loaded correctly
        COM_INTERFACE_ENTRY_IID(IID_IObjectSafety, IObjectSafety)
#endif
    END_COM_MAP()

    BEGIN_PROP_MAP(Cplugin)
        PROP_DATA_ENTRY("_cx", m_sizeExtent.cx, VT_UI4)
        PROP_DATA_ENTRY("_cy", m_sizeExtent.cy, VT_UI4)
        // Example entries
        // PROP_ENTRY_TYPE("Property Name", dispid, clsid, vtType)
        // PROP_PAGE(CLSID_StockColorPage)
    END_PROP_MAP()


    BEGIN_MSG_MAP(Cplugin)
        MESSAGE_HANDLER(WM_CREATE, OnCreate)
        CHAIN_MSG_MAP(CComControl<Cplugin>)
        DEFAULT_REFLECTION_HANDLER()
    END_MSG_MAP()
    // Handler prototypes:
    //  LRESULT MessageHandler(UINT uMsg, WPARAM wParam, LPARAM lParam, BOOL& bHandled);
    //  LRESULT CommandHandler(WORD wNotifyCode, WORD wID, HWND hWndCtl, BOOL& bHandled);
    //  LRESULT NotifyHandler(int idCtrl, LPNMHDR pnmh, BOOL& bHandled);

    // ISupportsErrorInfo
    STDMETHOD(InterfaceSupportsErrorInfo)(REFIID riid)
    {
        static const IID* arr[] =
        {
            &IID_Iplugin,
        };

        for (int i=0; i<sizeof(arr)/sizeof(arr[0]); i++)
        {
            if (InlineIsEqualGUID(*arr[i], riid))
                return S_OK;
        }
        return S_FALSE;
    }

    // IViewObjectEx
    DECLARE_VIEW_STATUS(VIEWSTATUS_SOLIDBKGND | VIEWSTATUS_OPAQUE)

    // Iplugin

    LRESULT OnCreate(UINT /*uMsg*/, WPARAM /*wParam*/, LPARAM /*lParam*/, BOOL& /*bHandled*/)
    {
        CAxWindow wnd(m_hWnd);
        wnd.ModifyStyle(0, WS_HSCROLL | WS_VSCROLL);
        HRESULT hr = wnd.CreateControl(IDH_PLUGIN);
        if (SUCCEEDED(hr))
        {
            CComObject<CpluginUI> *pObject = NULL;
            hr = CComObject<CpluginUI>::CreateInstance(&pObject);
            if (SUCCEEDED(hr) && pObject != NULL)
                hr = wnd.SetExternalDispatch(static_cast<IpluginUI*>(pObject));
        }
        if (SUCCEEDED(hr))
            hr = wnd.QueryControl(IID_IWebBrowser2, (void**)&m_spBrowser);
        return SUCCEEDED(hr) ? 0 : -1;
    }

    STDMETHOD(TranslateAccelerator)(LPMSG pMsg)
    {
        CComPtr<IOleInPlaceActiveObject> spIOleInPlaceActiveObject;

        HRESULT hr = m_spBrowser->QueryInterface(&spIOleInPlaceActiveObject);
        if (SUCCEEDED(hr))
            hr = spIOleInPlaceActiveObject->TranslateAccelerator(pMsg);
        if (hr != S_OK)
            hr = IOleInPlaceActiveObjectImpl<Cplugin>::TranslateAccelerator(pMsg);

        return hr;
    }
    CComPtr<IWebBrowser2> m_spBrowser;

    DECLARE_PROTECT_FINAL_CONSTRUCT()

    HRESULT FinalConstruct()
    {
        IEUtils::InitEventLog();
        return S_OK;
    }

    void FinalRelease()
    {
        IEUtils::CloseEventLog();
        Debug::log(Debug::Debugging) << "OOPHM plugin FinalRelease" << Debug::flush;
    }

    STDMETHOD(connect)(BSTR url, BSTR sessionKey, BSTR hostedServer,
        BSTR moduleName, BSTR hostedHtmlVersion, VARIANT_BOOL* ret);
    STDMETHOD(init)(IDispatch* jsniContext, VARIANT_BOOL* ret);
    STDMETHOD(testObject)(IDispatch** ret);
private:
    scoped_ptr<IESessionHandler> sessionHandler;
};

OBJECT_ENTRY_AUTO(__uuidof(plugin), Cplugin)
