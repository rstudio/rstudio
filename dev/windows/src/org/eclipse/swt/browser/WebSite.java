/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
// Modified by Google
package org.eclipse.swt.browser;

import org.eclipse.swt.*;
import org.eclipse.swt.internal.ole.win32.*;
import org.eclipse.swt.ole.win32.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.internal.win32.*;

class WebSite extends OleControlSite {
	COMObject iDocHostUIHandler;
	COMObject iDocHostShowUI;
	COMObject iServiceProvider;
	COMObject iInternetSecurityManager;
	COMObject iOleCommandTarget;

	static final int OLECMDID_SHOWSCRIPTERROR = 40;

public WebSite(Composite parent, int style, String progId) {
	super(parent, style, progId);
}

// GOOGLE: attempt to load Gears
/**
 * Load the Google Gears BHO if possible.
 * 
 * @return true if Gears was successfully loaded and initialized
 */
public boolean startGears() {
  // Get the classID of the Gears BHO.
  GUID appClsid = null;
  try {
    appClsid = getClassID("gears.BHO");
    if (appClsid == null) {
      return false;
    }
  } catch(SWTException e) {
    return false;
  }
  
  // Create an instance of the Gears BHO.
  int[] address = new int[1];
  if (COM.CoCreateInstance(appClsid, 0, COM.CLSCTX_INPROC_SERVER,
      COM.IIDIUnknown, address) != COM.S_OK) {
    return false;
  }
  
  // Get the IObjectWithSite interface and call SetSite. 
  IUnknown obj = new IUnknown(address[0]);
  int[] ppvObject = new int[1];
  if (obj.QueryInterface(COM.IIDIObjectWithSite, ppvObject) == COM.S_OK) {
    IObjectWithSite objectWithSite = new IObjectWithSite(ppvObject[0]);
    /*
     * TODO: Gears currently does not check the parameter passed other than
     * to see if it is non-null.  If Gears ever changes to actually use the
     * passed object, we will need to make sure that this works as expected.
     */
    objectWithSite.SetSite(objIUnknown);
    objectWithSite.Release();
    return true;
  }
  return false;
}

protected void createCOMInterfaces () {
	super.createCOMInterfaces();
	iDocHostUIHandler = new COMObject(new int[]{2, 0, 0, 4, 1, 5, 0, 0, 1, 1, 1, 3, 3, 2, 2, 1, 3, 2}){
		public int method0(int[] args) {return QueryInterface(args[0], args[1]);}
		public int method1(int[] args) {return AddRef();}
		public int method2(int[] args) {return Release();}
		public int method3(int[] args) {return ShowContextMenu(args[0], args[1], args[2], args[3]);}
		public int method4(int[] args) {return GetHostInfo(args[0]);}
		public int method5(int[] args) {return ShowUI(args[0], args[1], args[2], args[3], args[4]);}
		public int method6(int[] args) {return HideUI();}
		public int method7(int[] args) {return UpdateUI();}
		public int method8(int[] args) {return EnableModeless(args[0]);}
		public int method9(int[] args) {return OnDocWindowActivate(args[0]);}
		public int method10(int[] args) {return OnFrameWindowActivate(args[0]);}
		public int method11(int[] args) {return ResizeBorder(args[0], args[1], args[2]);}
		public int method12(int[] args) {return TranslateAccelerator(args[0], args[1], args[2]);}
		public int method13(int[] args) {return GetOptionKeyPath(args[0], args[1]);}
		public int method14(int[] args) {return GetDropTarget(args[0], args[1]);}
		public int method15(int[] args) {return GetExternal(args[0]);}
		public int method16(int[] args) {return TranslateUrl(args[0], args[1], args[2]);}		
		public int method17(int[] args) {return FilterDataObject(args[0], args[1]);}
	};
	iDocHostShowUI = new COMObject(new int[]{2, 0, 0, 7, 7}){
		public int method0(int[] args) {return QueryInterface(args[0], args[1]);}
		public int method1(int[] args) {return AddRef();}
		public int method2(int[] args) {return Release();}
		public int method3(int[] args) {return ShowMessage(args[0], args[1], args[2], args[3], args[4], args[5], args[6]);}
		public int method4(int[] args) {return ShowHelp(args[0], args[1], args[2], args[3], args[4], args[5], args[6]);}
	};
	iServiceProvider = new COMObject(new int[]{2, 0, 0, 3}){
		public int method0(int[] args) {return QueryInterface(args[0], args[1]);}
		public int method1(int[] args) {return AddRef();}
		public int method2(int[] args) {return Release();}
		public int method3(int[] args) {return QueryService(args[0], args[1], args[2]);}
	};
	iInternetSecurityManager = new COMObject(new int[]{2, 0, 0, 1, 1, 3, 4, 8, 7, 3, 3}){
		public int method0(int[] args) {return QueryInterface(args[0], args[1]);}
		public int method1(int[] args) {return AddRef();}
		public int method2(int[] args) {return Release();}
		public int method3(int[] args) {return SetSecuritySite(args[0]);}
		public int method4(int[] args) {return GetSecuritySite(args[0]);}
		public int method5(int[] args) {return MapUrlToZone(args[0], args[1], args[2]);}
		public int method6(int[] args) {return GetSecurityId(args[0], args[1], args[2], args[3]);}
		public int method7(int[] args) {return ProcessUrlAction(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7]);}
		public int method8(int[] args) {return QueryCustomPolicy(args[0], args[1], args[2], args[3], args[4], args[5], args[6]);}
		public int method9(int[] args) {return SetZoneMapping(args[0], args[1], args[2]);}
		public int method10(int[] args) {return GetZoneMappings(args[0], args[1], args[2]);}
	};
	iOleCommandTarget = new COMObject(new int[]{2, 0, 0, 4, 5}) {
		public int method0(int[] args) {return QueryInterface(args[0], args[1]);}
		public int method1(int[] args) {return AddRef();}
		public int method2(int[] args) {return Release();}		
		public int method3(int[] args) {return QueryStatus(args[0], args[1], args[2], args[3]);}		
		public int method4(int[] args) {return Exec(args[0], args[1], args[2], args[3], args[4]);}
	};
}

protected void disposeCOMInterfaces() {
	super.disposeCOMInterfaces();
	if (iDocHostUIHandler != null) {
		iDocHostUIHandler.dispose();
		iDocHostUIHandler = null;
	}
	if (iDocHostShowUI != null) {
		iDocHostShowUI.dispose();
		iDocHostShowUI = null;
	}
	if (iServiceProvider != null) {
		iServiceProvider.dispose();
		iServiceProvider = null;
	}
	if (iInternetSecurityManager != null) {
		iInternetSecurityManager.dispose();
		iInternetSecurityManager = null;
	}
	if (iOleCommandTarget != null) {
		iOleCommandTarget.dispose();
		iOleCommandTarget = null;
	}
}

protected int AddRef() {
	/* Workaround for javac 1.1.8 bug */
	return super.AddRef();
}

protected int QueryInterface(int riid, int ppvObject) {
	int result = super.QueryInterface(riid, ppvObject);
	if (result == COM.S_OK) return result;
	if (riid == 0 || ppvObject == 0) return COM.E_INVALIDARG;
	GUID guid = new GUID();
	COM.MoveMemory(guid, riid, GUID.sizeof);
	if (COM.IsEqualGUID(guid, COM.IIDIDocHostUIHandler)) {
		COM.MoveMemory(ppvObject, new int[] {iDocHostUIHandler.getAddress()}, 4);
		AddRef();
		return COM.S_OK;
	}
	if (COM.IsEqualGUID(guid, COM.IIDIDocHostShowUI)) {
		COM.MoveMemory(ppvObject, new int[] {iDocHostShowUI.getAddress()}, 4);
		AddRef();
		return COM.S_OK;
	}
	if (COM.IsEqualGUID(guid, COM.IIDIServiceProvider)) {
		COM.MoveMemory(ppvObject, new int[] {iServiceProvider.getAddress()}, 4);
		AddRef();
		return COM.S_OK;
	}
    if (COM.IsEqualGUID(guid, COM.IIDIInternetSecurityManager)) {
        COM.MoveMemory(ppvObject, new int[] {iInternetSecurityManager.getAddress()}, 4);
        AddRef();
        return COM.S_OK;
    }
	if (COM.IsEqualGUID(guid, COM.IIDIOleCommandTarget)) {
		COM.MoveMemory(ppvObject, new int[] {iOleCommandTarget.getAddress()}, 4);
		AddRef();
		return COM.S_OK;
	}
	COM.MoveMemory(ppvObject, new int[] {0}, 4);
	return COM.E_NOINTERFACE;
}

/* IDocHostUIHandler */

int EnableModeless(int EnableModeless) {
	return COM.E_NOTIMPL;
}

int FilterDataObject(int pDO, int ppDORet) {
	return COM.E_NOTIMPL;
}

int GetDropTarget(int pDropTarget, int ppDropTarget) {
	return COM.E_NOTIMPL;
}

int GetExternal(int ppDispatch) {
	OS.MoveMemory(ppDispatch, new int[] {0}, 4);
	return COM.S_FALSE;
}

int GetHostInfo(int pInfo) {
	Browser browser = (Browser)getParent().getParent();
	OS.MoveMemory(pInfo + 4, new int[] {browser.info}, 4);
	return COM.S_OK;
}

int GetOptionKeyPath(int pchKey, int dw) {
	return COM.E_NOTIMPL;
}

int HideUI() {
	return COM.E_NOTIMPL;
}

int OnDocWindowActivate(int fActivate) {
	return COM.E_NOTIMPL;
}

int OnFrameWindowActivate(int fActivate) {
	return COM.E_NOTIMPL;
}

protected int Release() {
	/* Workaround for javac 1.1.8 bug */
	return super.Release();
}

int ResizeBorder(int prcBorder, int pUIWindow, int fFrameWindow) {
	return COM.E_NOTIMPL;
}

int ShowContextMenu(int dwID, int ppt, int pcmdtReserved, int pdispReserved) {
	Browser browser = (Browser)getParent().getParent();
	Event event = new Event();
	POINT pt = new POINT();
	OS.MoveMemory(pt, ppt, POINT.sizeof);
	event.x = pt.x;
	event.y = pt.y;
	browser.notifyListeners(SWT.MenuDetect, event);
	if (!event.doit) return COM.S_OK;
	Menu menu = browser.getMenu();
	if (menu != null && !menu.isDisposed ()) {
		if (pt.x != event.x || pt.y != event.y) {
			menu.setLocation (event.x, event.y);
		}
		menu.setVisible (true);
		return COM.S_OK;
	}
	/* Show default IE popup menu */
	return COM.S_FALSE;
}

int ShowUI(int dwID, int pActiveObject, int pCommandTarget, int pFrame, int pDoc) {
	return COM.E_NOTIMPL;
}

int TranslateAccelerator(int lpMsg, int pguidCmdGroup, int nCmdID) {
	/*
	* Feature on Internet Explorer.  By default the embedded Internet Explorer control runs
	* the Internet Explorer shortcuts (e.g. F5 for refresh).  This overrides the shortcuts
	* defined by SWT.  The workaround is to forward the accelerator keys to the parent window
	* and have Internet Explorer ignore the ones handled by the parent window.
	*/
	Menu menubar = getShell().getMenuBar();
	if (menubar != null && !menubar.isDisposed() && menubar.isEnabled()) {
		Shell shell = menubar.getShell();
		int hwnd = shell.handle;
		int hAccel = OS.SendMessage(hwnd, OS.WM_APP+1, 0, 0);
		if (hAccel != 0) {
			MSG msg = new MSG();
			OS.MoveMemory(msg, lpMsg, MSG.sizeof);
			if (OS.TranslateAccelerator(hwnd, hAccel, msg) != 0) return COM.S_OK;
		}
	}
	/*
	* Feature on Internet Explorer.  By default the embedded Internet Explorer control runs
	* the Internet Explorer shortcuts.  F5 causes refresh.  CTRL-N opens a standalone Internet 
	* Explorer.  These behaviours are undesired when rendering HTML in memory.
	* The workaround is to block the default CTRL-N and F5 handling by IE when the URL is about:blank.
	*/
	OleAutomation auto = new OleAutomation(this);
	int[] rgdispid = auto.getIDsOfNames(new String[] { "LocationURL" }); //$NON-NLS-1$
	Variant pVarResult = auto.getProperty(rgdispid[0]);
	auto.dispose();
	int result = COM.S_FALSE;
	if (pVarResult != null) {
		if (pVarResult.getType() == OLE.VT_BSTR) {
			String url = pVarResult.getString();
			if (url.equals(Browser.ABOUT_BLANK)) {
				MSG msg = new MSG();
				OS.MoveMemory(msg, lpMsg, MSG.sizeof);
				if (msg.message == OS.WM_KEYDOWN && msg.wParam == OS.VK_F5) result = COM.S_OK;
				if (msg.message == OS.WM_KEYDOWN && msg.wParam == OS.VK_N && OS.GetKeyState (OS.VK_CONTROL) < 0) result = COM.S_OK;
			}
		}
		pVarResult.dispose();
	}
	return result;
}

int TranslateUrl(int dwTranslate, int pchURLIn, int ppchURLOut) {
	return COM.E_NOTIMPL;
}

int UpdateUI() {
	return COM.E_NOTIMPL;
}

/* IDocHostShowUI */

int ShowMessage(int hwnd, int lpstrText, int lpstrCaption, int dwType, int lpstrHelpFile, int dwHelpContext, int plResult) {
	/*
	* Feature on IE.  When IE navigates to a website that contains an ActiveX that is prevented from
	* being executed, IE displays a message "Your current security settings prohibit running ActiveX 
	* controls on this page ...".  The workaround is to selectively block this alert as indicated
	* in the MSDN article "WebBrowser customization".
	*/
	/* resource identifier in shdoclc.dll for window caption "Your current security settings prohibit 
	 * running ActiveX controls on this page ..." 
	 */
	int IDS_MESSAGE_BOX_CAPTION = 8033;
		if (lpstrText != 0) {
		TCHAR lpLibFileName = new TCHAR (0, "SHDOCLC.DLL", true); //$NON-NLS-1$
		int hModule = OS.LoadLibrary(lpLibFileName);
		if (hModule != 0) {
			/* 
			* Note.  lpstrText is a LPOLESTR, i.e. a null terminated unicode string LPWSTR, i.e. a WCHAR*.
			* It is not a BSTR.  A BSTR is a null terminated unicode string that contains its length
			* at the beginning. 
			*/
			int cnt = OS.wcslen(lpstrText);
			char[] buffer = new char[cnt];
			/* 
			* Note.  lpstrText is unicode on both unicode and ansi platforms.
			* The nbr of chars is multiplied by the constant 2 and not by TCHAR.sizeof since
			* TCHAR.sizeof returns 1 on ansi platforms.
			*/
			OS.MoveMemory(buffer, lpstrText, cnt * 2);
			String text = new String(buffer);
			/* provide a buffer large enough to hold the string to compare to and a null terminated character */
			int length = (OS.IsUnicode ? cnt : OS.WideCharToMultiByte (OS.CP_ACP, 0, buffer, cnt, 0, 0, null, null)) + 1;

			TCHAR lpBuffer = new TCHAR(0, length);
			int result = OS.LoadString(hModule, IDS_MESSAGE_BOX_CAPTION, lpBuffer, length);
			OS.FreeLibrary(hModule);
			return result > 0 && text.equals(lpBuffer.toString(0, result)) ? COM.S_OK : COM.S_FALSE;
		}
	}
	return COM.S_FALSE;
}

/* Note.  One of the arguments of ShowHelp is a POINT struct and not a pointer to a POINT struct. Because
 * of the way Callback gets int parameters from a va_list of C arguments 2 integer arguments must be declared,
 * ptMouse_x and ptMouse_y. Otherwise the Browser crashes when the user presses F1 to invoke
 * the help.
 */
int ShowHelp(int hwnd, int pszHelpFile, int uCommand, int dwData, int ptMouse_x, int ptMouse_y, int pDispatchObjectHit) {
	Browser browser = (Browser)getParent().getParent();
	Event event = new Event();
	event.type = SWT.Help;
	event.display = getDisplay();
	event.widget = browser;
	Shell shell = browser.getShell();
	Control control = browser;
	do {
		if (control.isListening(SWT.Help)) {
			control.notifyListeners(SWT.Help, event);
			break;
		}
		if (control == shell) break;
		control = control.getParent();
	} while (true);
	return COM.S_OK;
}

/* IServiceProvider */

int QueryService(int guidService, int riid, int ppvObject) {
	if (riid == 0 || ppvObject == 0) return COM.E_INVALIDARG;
	GUID guid = new GUID();
	COM.MoveMemory(guid, riid, GUID.sizeof);
	if (COM.IsEqualGUID(guid, COM.IIDIInternetSecurityManager)) {
		COM.MoveMemory(ppvObject, new int[] {iInternetSecurityManager.getAddress()}, 4);
		AddRef();
		return COM.S_OK;
	}
	COM.MoveMemory(ppvObject, new int[] {0}, 4);
	return COM.E_NOINTERFACE;
}

/* IInternetSecurityManager */

int SetSecuritySite(int pSite) {
	return Browser.INET_E_DEFAULT_ACTION;
}

int GetSecuritySite(int ppSite) {
	return Browser.INET_E_DEFAULT_ACTION;
}

int MapUrlToZone(int pwszUrl, int pdwZone, int dwFlags) {	
	/*
	* Feature in IE 6 sp1.  HTML rendered in memory
	* does not enable local links but the exact same
	* HTML document loaded through a local file is
	* permitted to follow local links.  The workaround is
	* to return URLZONE_INTRANET instead of the default
	* value URLZONE_LOCAL_MACHINE.
	*/	
	COM.MoveMemory(pdwZone, new int[] {Browser.URLZONE_INTRANET}, 4);
	return COM.S_OK;
}

int GetSecurityId(int pwszUrl, int pbSecurityId, int pcbSecurityId, int dwReserved) {
	return Browser.INET_E_DEFAULT_ACTION;
}

int ProcessUrlAction(int pwszUrl, int dwAction, int pPolicy, int cbPolicy, int pContext, int cbContext, int dwFlags, int dwReserved) {
	/*
	* Feature in IE 6 sp1.  HTML rendered in memory
	* containing an OBJECT tag referring to a local file
	* brings up a warning dialog asking the user whether
	* it should proceed or not.  The workaround is to
	* set the policy to URLPOLICY_ALLOW in this case (dwAction
	* value of 0x1406).
	* 
	* Feature in IE. Security Patches and user settings
	* affect the way the embedded web control behaves.  The current
	* approach is to consider the content trusted and allow
	* all URLs by default.
	*/
	int policy = Browser.URLPOLICY_ALLOW;
	/*
	* Note. The URLACTION_JAVA flags refer to the applet tag that normally resolve to
	* the Microsoft VM, not to the java OBJECT tag that resolves to the
	* Sun plugin. Return URLPOLICY_JAVA_LOW to authorize applets instead of
	* URLPOLICY_ALLOW that is interpreted as URLPOLICY_JAVA_PROHIBIT in this
	* context. 
	*/
	if (dwAction >= Browser.URLACTION_JAVA_MIN && dwAction <= Browser.URLACTION_JAVA_MAX) {
		policy = Browser.URLPOLICY_JAVA_LOW;
	}
	/*
	* Note.  Some ActiveX plugins crash when executing
	* inside the embedded explorer itself running into
	* a JVM.  The current workaround is to detect when
	* such ActiveX is about to be started and refuse
	* to execute it.
	*/
	if (dwAction == Browser.URLACTION_ACTIVEX_RUN) {
		GUID guid = new GUID();
		COM.MoveMemory(guid, pContext, GUID.sizeof);
		if (COM.IsEqualGUID(guid, COM.IIDJavaBeansBridge) || COM.IsEqualGUID(guid, COM.IIDShockwaveActiveXControl)) {
			policy = Browser.URLPOLICY_DISALLOW;
		}
	}
	if (cbPolicy >= 4) COM.MoveMemory(pPolicy, new int[] {policy}, 4);
	return COM.S_OK;
}

int QueryCustomPolicy(int pwszUrl, int guidKey, int ppPolicy, int pcbPolicy, int pContext, int cbContext, int dwReserved) {
	return Browser.INET_E_DEFAULT_ACTION;
}

int SetZoneMapping(int dwZone, int lpszPattern, int dwFlags) {
	return Browser.INET_E_DEFAULT_ACTION;
}

int GetZoneMappings(int dwZone, int ppenumString, int dwFlags) {
	return COM.E_NOTIMPL;
}

/* IOleCommandTarget */
int QueryStatus(int pguidCmdGroup, int cCmds, int prgCmds, int pCmdText) {
	return COM.E_NOTSUPPORTED;
}

int Exec(int pguidCmdGroup, int nCmdID, int nCmdExecOpt, int pvaIn, int pvaOut) {
	if (pguidCmdGroup != 0) {
		GUID guid = new GUID();
		COM.MoveMemory(guid, pguidCmdGroup, GUID.sizeof);

		/*
		* If a javascript error occurred then suppress IE's default script error dialog.
		*/
		if (COM.IsEqualGUID(guid, COM.CGID_DocHostCommandHandler)) {
			if (nCmdID == OLECMDID_SHOWSCRIPTERROR) return COM.S_OK;
		}

		/*
		* Bug in Internet Explorer.  OnToolBar TRUE is also fired when any of the 
		* address bar or menu bar are requested but not the tool bar.  A workaround
		* has been posted by a Microsoft developer on the public webbrowser_ctl
		* newsgroup. The workaround is to implement the IOleCommandTarget interface
		* to test the argument of an undocumented command.
		*/
		if (nCmdID == 1 && COM.IsEqualGUID(guid, COM.CGID_Explorer) && ((nCmdExecOpt & 0xFFFF) == 0xA)) {
			Browser browser = (Browser)getParent().getParent();
			browser.toolBar = (nCmdExecOpt & 0xFFFF0000) != 0;
		}
	}
	return COM.E_NOTSUPPORTED;
}

}
