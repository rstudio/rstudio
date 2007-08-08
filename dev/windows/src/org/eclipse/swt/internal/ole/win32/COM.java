/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
// Modified by Google
package org.eclipse.swt.internal.ole.win32;

import org.eclipse.swt.internal.win32.*;

public class COM extends OS {
	/** GUID Constants */
	public static final GUID IIDJavaBeansBridge = COM.IIDFromString("{8AD9C840-044E-11D1-B3E9-00805F499D93}"); //$NON-NLS-1$
	public static final GUID IIDShockwaveActiveXControl = COM.IIDFromString("{166B1BCA-3F9C-11CF-8075-444553540000}"); //$NON-NLS-1$
	public static final GUID IIDIEditorSiteTime = IIDFromString("{6BD2AEFE-7876-45e6-A6E7-3BFCDF6540AA}"); //$NON-NLS-1$
	public static final GUID IIDIEditorSiteProperty = IIDFromString("{D381A1F4-2326-4f3c-AFB9-B7537DB9E238}"); //$NON-NLS-1$
	public static final GUID IIDIEditorBaseProperty = IIDFromString("{61E55B0B-2647-47c4-8C89-E736EF15D636}"); //$NON-NLS-1$
	public static final GUID IIDIEditorSite = IIDFromString("{CDD88AB9-B01D-426E-B0F0-30973E9A074B}"); //$NON-NLS-1$
	public static final GUID IIDIEditorService = IIDFromString("{BEE283FE-7B42-4FF3-8232-0F07D43ABCF1}"); //$NON-NLS-1$
	public static final GUID IIDIEditorManager = IIDFromString("{EFDE08C4-BE87-4B1A-BF84-15FC30207180}"); //$NON-NLS-1$
	public static final GUID IIDIAccessible = IIDFromString("{618736E0-3C3D-11CF-810C-00AA00389B71}"); //$NON-NLS-1$
	//public static final GUID IIDIAccessibleHandler = IIDFromString("{03022430-ABC4-11D0-BDE2-00AA001A1953}"); //$NON-NLS-1$
	//public static final GUID IIDIAccessor = IIDFromString("{0C733A8C-2A1C-11CE-ADE5-00AA0044773D}"); //$NON-NLS-1$
	public static final GUID IIDIAdviseSink = IIDFromString("{0000010F-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	//public static final GUID IIDIAdviseSink2 = IIDFromString("{00000125-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	//public static final GUID IIDIBindCtx = IIDFromString("{0000000E-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	//public static final GUID IIDIClassFactory = IIDFromString("{00000001-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	public static final GUID IIDIClassFactory2 = IIDFromString("{B196B28F-BAB4-101A-B69C-00AA00341D07}"); //$NON-NLS-1$
	public static final GUID IIDIConnectionPoint = IIDFromString("{B196B286-BAB4-101A-B69C-00AA00341D07}"); //$NON-NLS-1$
	public static final GUID IIDIConnectionPointContainer = IIDFromString("{B196B284-BAB4-101A-B69C-00AA00341D07}"); //$NON-NLS-1$
	//public static final GUID IIDICreateErrorInfo = IIDFromString("{22F03340-547D-101B-8E65-08002B2BD119}"); //$NON-NLS-1$
	//public static final GUID IIDICreateTypeInfo = IIDFromString("{00020405-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	//public static final GUID IIDICreateTypeLib = IIDFromString("{00020406-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	//public static final GUID IIDIDataAdviseHolder = IIDFromString("{00000110-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	public static final GUID IIDIDataObject = IIDFromString("{0000010E-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	public static final GUID IIDIDispatch = IIDFromString("{00020400-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	public static final GUID IIDIDocHostUIHandler = IIDFromString("{BD3F23C0-D43E-11CF-893B-00AA00BDCE1A}"); //$NON-NLS-1$	
	public static final GUID IIDIDocHostShowUI = IIDFromString("{C4D244B0-D43E-11CF-893B-00AA00BDCE1A}"); //$NON-NLS-1$	
	public static final GUID IIDIDropSource = IIDFromString("{00000121-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	public static final GUID IIDIDropTarget = IIDFromString("{00000122-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	//public static final GUID IIDIEnumConnectionPoints = IIDFromString("{B196B285-BAB4-101A-B69C-00AA00341D07}"); //$NON-NLS-1$
	//public static final GUID IIDIEnumConnections = IIDFromString("{B196B287-BAB4-101A-B69C-00AA00341D07}"); //$NON-NLS-1$
	public static final GUID IIDIEnumFORMATETC = IIDFromString("{00000103-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	//public static final GUID IIDIEnumMoniker = IIDFromString("{00000102-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	//public static final GUID IIDIEnumOLEVERB = IIDFromString("{00000104-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	//public static final GUID IIDIEnumSTATDATA = IIDFromString("{00000105-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	//public static final GUID IIDIEnumSTATSTG = IIDFromString("{0000000D-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	//public static final GUID IIDIEnumString = IIDFromString("{00000101-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	//public static final GUID IIDIEnumUnknown = IIDFromString("{00000100-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	public static final GUID IIDIEnumVARIANT = IIDFromString("{00020404-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	//public static final GUID IIDIErrorInfo = IIDFromString("{1CF2B120-547D-101B-8E65-08002B2BD119}"); //$NON-NLS-1$
	//public static final GUID IIDIErrorLog = IIDFromString("{3127CA40-446E-11CE-8135-00AA004BB851}"); //$NON-NLS-1$
	//public static final GUID IIDIExternalConnection = IIDFromString("{00000019-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	public static final GUID IIDIFont = IIDFromString("{BEF6E002-A874-101A-8BBA-00AA00300CAB}"); //$NON-NLS-1$
	//public static final GUID IIDIFontDisp = IIDFromString("{BEF6E003-A874-101A-8BBA-00AA00300CAB}"); //$NON-NLS-1$
	public static final GUID IIDIInternetSecurityManager = IIDFromString("{79eac9ee-baf9-11ce-8c82-00aa004ba90b}"); //$NON-NLS-1$
	//public static final GUID IIDILockBytes = IIDFromString("{0000000A-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	//public static final GUID IIDIMalloc = IIDFromString("{00000002-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	//public static final GUID IIDIMallocSpy = IIDFromString("{0000001D-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	//public static final GUID IIDIMarshal = IIDFromString("{00000003-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	//public static final GUID IIDIMessageFilter = IIDFromString("{00000016-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	//public static final GUID IIDIMoniker = IIDFromString("{0000000F-0000-0000-C000-000000000046}"); //$NON-NLS-1$
    public static final GUID IIDIObjectWithSite = IIDFromString("{FC4801A3-2BA9-11CF-A229-00AA003D7352}"); //$NON-NLS-1$ // GOOGLE
	//public static final GUID IIDIOleAdviseHolder = IIDFromString("{00000111-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	//public static final GUID IIDIOleCache = IIDFromString("{0000011E-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	//public static final GUID IIDIOleCache2 = IIDFromString("{00000128-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	//public static final GUID IIDIOleCacheControl = IIDFromString("{00000129-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	public static final GUID IIDIOleClientSite = IIDFromString("{00000118-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	public static final GUID IIDIOleCommandTarget = IIDFromString("{B722BCCB-4E68-101B-A2BC-00AA00404770}"); //$NON-NLS-1$
	public static final GUID IIDIOleContainer = IIDFromString("{0000011B-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	public static final GUID IIDIOleControl = IIDFromString("{B196B288-BAB4-101A-B69C-00AA00341D07}"); //$NON-NLS-1$
	public static final GUID IIDIOleControlSite = IIDFromString("{B196B289-BAB4-101A-B69C-00AA00341D07}"); //$NON-NLS-1$
	public static final GUID IIDIOleDocument = IIDFromString("{B722BCC5-4E68-101B-A2BC-00AA00404770}"); //$NON-NLS-1$
	public static final GUID IIDIOleDocumentSite = IIDFromString("{B722BCC7-4E68-101B-A2BC-00AA00404770}"); //$NON-NLS-1$
	public static final GUID IIDIOleInPlaceActiveObject = IIDFromString("{00000117-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	public static final GUID IIDIOleInPlaceFrame = IIDFromString("{00000116-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	public static final GUID IIDIOleInPlaceObject = IIDFromString("{00000113-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	public static final GUID IIDIOleInPlaceSite = IIDFromString("{00000119-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	public static final GUID IIDIOleInPlaceUIWindow = IIDFromString("{00000115-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	//public static final GUID IIDIOleItemContainer = IIDFromString("{0000011C-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	public static final GUID IIDIOleLink = IIDFromString("{0000011D-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	public static final GUID IIDIOleObject = IIDFromString("{00000112-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	public static final GUID IIDIOleWindow = IIDFromString("{00000114-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	//public static final GUID IIDIParseDisplayName = IIDFromString("{0000011A-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	//public static final GUID IIDIPerPropertyBrowsing = IIDFromString("{376BD3AA-3845-101B-84ED-08002B2EC713}"); //$NON-NLS-1$
	public static final GUID IIDIPersist = IIDFromString("{0000010C-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	public static final GUID IIDIPersistFile = IIDFromString("{0000010B-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	//public static final GUID IIDIPersistMemory = IIDFromString("{BD1AE5E0-A6AE-11CE-BD37-504200C10000}"); //$NON-NLS-1$
	//public static final GUID IIDIPersistPropertyBag = IIDFromString("{37D84F60-42CB-11CE-8135-00AA004BB851}"); //$NON-NLS-1$
	public static final GUID IIDIPersistStorage = IIDFromString("{0000010A-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	public static final GUID IIDIPersistStream = IIDFromString("{00000109-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	public static final GUID IIDIPersistStreamInit = IIDFromString("{7FD52380-4E07-101B-AE2D-08002B2EC713}"); //$NON-NLS-1$
	//public static final GUID IIDIPicture = IIDFromString("{7BF80980-BF32-101A-8BBB-00AA00300CAB}"); //$NON-NLS-1$
	//public static final GUID IIDIPictureDisp = IIDFromString("{7BF80981-BF32-101A-8BBB-00AA00300CAB}"); //$NON-NLS-1$
	//public static final GUID IIDIPropertyBag = IIDFromString("{55272A00-42CB-11CE-8135-00AA004BB851}"); //$NON-NLS-1$
	public static final GUID IIDIPropertyNotifySink = IIDFromString("{9BFBBC02-EFF1-101A-84ED-00AA00341D07}"); //$NON-NLS-1$
	//public static final GUID IIDIPropertyPage = IIDFromString("{B196B28D-BAB4-101A-B69C-00AA00341D07}"); //$NON-NLS-1$
	//public static final GUID IIDIPropertyPage2 = IIDFromString("{01E44665-24AC-101B-84ED-08002B2EC713}"); //$NON-NLS-1$
	//public static final GUID IIDIPropertyPageSite = IIDFromString("{B196B28C-BAB4-101A-B69C-00AA00341D07}"); //$NON-NLS-1$
	public static final GUID IIDIProvideClassInfo = IIDFromString("{B196B283-BAB4-101A-B69C-00AA00341D07}"); //$NON-NLS-1$
	public static final GUID IIDIProvideClassInfo2 = IIDFromString("{A6BC3AC0-DBAA-11CE-9DE3-00AA004BB851}"); //$NON-NLS-1$
	//public static final GUID IIDIPSFactoryBuffer = IIDFromString("{D5F569D0-593B-101A-B569-08002B2DBF7A}"); //$NON-NLS-1$
	//public static final GUID IIDIRootStorage = IIDFromString("{00000012-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	//public static final GUID IIDIROTData = IIDFromString("{F29F6BC0-5021-11CE-AA15-00006901293F}"); //$NON-NLS-1$
	//public static final GUID IIDIRpcChannelBuffer = IIDFromString("{D5F56B60-593B-101A-B569-08002B2DBF7A}"); //$NON-NLS-1$
	//public static final GUID IIDIRpcProxyBuffer = IIDFromString("{D5F56A34-593B-101A-B569-08002B2DBF7A}"); //$NON-NLS-1$
	//public static final GUID IIDIRpcStubBuffer = IIDFromString("{D5F56AFC-593B-101A-B569-08002B2DBF7A}"); //$NON-NLS-1$
	//public static final GUID IIDIRunnableObject = IIDFromString("{00000126-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	//public static final GUID IIDIRunningObjectTable = IIDFromString("{00000010-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	//public static final GUID IIDISimpleFrameSite = IIDFromString("{742B0E01-14E6-101B-914E-00AA00300CAB}"); //$NON-NLS-1$
	public static final GUID IIDIServiceProvider = IIDFromString("{6d5140c1-7436-11ce-8034-00aa006009fa}"); //$NON-NLS-1$
	public static final GUID IIDISpecifyPropertyPages = IIDFromString("{B196B28B-BAB4-101A-B69C-00AA00341D07}"); //$NON-NLS-1$
	//public static final GUID IIDIStdMarshalInfo = IIDFromString("{00000018-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	public static final GUID IIDIStorage = IIDFromString("{0000000B-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	public static final GUID IIDIStream = IIDFromString("{0000000C-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	//public static final GUID IIDISupportErrorInfo = IIDFromString("{DF0B3D60-548F-101B-8E65-08002B2BD119}"); //$NON-NLS-1$
	//public static final GUID IIDITypeComp = IIDFromString("{00020403-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	//public static final GUID IIDITypeLib = IIDFromString("{00020402-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	public static final GUID IIDIUnknown = IIDFromString("{00000000-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	//public static final GUID IIDIViewObject = IIDFromString("{0000010D-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	public static final GUID IIDIViewObject2 = IIDFromString("{00000127-0000-0000-C000-000000000046}"); //$NON-NLS-1$
	public static final GUID CGID_DocHostCommandHandler = IIDFromString("{f38bc242-b950-11d1-8918-00c04fc2c836}"); //$NON-NLS-1$
	public static final GUID CGID_Explorer = IIDFromString("{000214D0-0000-0000-C000-000000000046}"); //$NON-NLS-1$


	/** Constants */
	//public static final int ADVF_DATAONSTOP = 64;
	//public static final int ADVF_NODATA = 1;
	//public static final int ADVF_ONLYONCE = 2;
	//public static final int ADVF_PRIMEFIRST = 4;
	//public static final int ADVFCACHE_FORCEBUILTIN = 16;
	//public static final int ADVFCACHE_NOHANDLER = 8;
	//public static final int ADVFCACHE_ONSAVE = 32;
	public static final int CF_TEXT = 1;
	public static final int CF_BITMAP = 2;
	public static final int CF_METAFILEPICT = 3;
	public static final int CF_SYLK = 4;
	public static final int CF_DIF = 5;
	public static final int CF_TIFF = 6;
	public static final int CF_OEMTEXT = 7;
	public static final int CF_DIB = 8;
	public static final int CF_PALETTE = 9;
	public static final int CF_PENDATA = 10;
	public static final int CF_RIFF = 11;
	public static final int CF_WAVE = 12;
	public static final int CF_UNICODETEXT = 13;
	public static final int CF_ENHMETAFILE = 14;
	public static final int CF_HDROP = 15;
	public static final int CF_LOCALE = 16;
	public static final int CF_MAX = 17;
	public static final int CLSCTX_INPROC_HANDLER = 2;
	public static final int CLSCTX_INPROC_SERVER = 1;
	public static final int CLSCTX_LOCAL_SERVER = 4;
	public static final int CLSCTX_REMOTE_SERVER = 16;
	public static final int CO_E_CLASSSTRING = -2147221005;
	//public static final int COINIT_APARTMENTTHREADED = 2;
	//public static final int COINIT_DISABLE_OLE1DDE = 4;
	//public static final int COINIT_MULTITHREADED = 0;
	//public static final int COINIT_SPEED_OVER_MEMORY = 8;
	public static final int DATADIR_GET = 1;
	public static final int DATADIR_SET = 2;
	public static final int DISP_E_EXCEPTION = 0x80020009;
	public static final int DISP_E_MEMBERNOTFOUND = -2147352573;
	public static final int DISP_E_UNKNOWNINTERFACE = 0x80020001;
	//public static final int DISPID_AMBIENT_APPEARANCE = -716;
	//public static final int DISPID_AMBIENT_AUTOCLIP = -715;
	public static final int DISPID_AMBIENT_BACKCOLOR = -701;
	//public static final int DISPID_AMBIENT_CHARSET = -727;
	//public static final int DISPID_AMBIENT_CODEPAGE = -725;
	//public static final int DISPID_AMBIENT_DISPLAYASDEFAULT = -713;
	//public static final int DISPID_AMBIENT_DISPLAYNAME = -702;
	public static final int DISPID_AMBIENT_FONT = -703;
	public static final int DISPID_AMBIENT_FORECOLOR = -704;
	public static final int DISPID_AMBIENT_LOCALEID = -705;
	public static final int DISPID_AMBIENT_MESSAGEREFLECT = -706;
	public static final int DISPID_AMBIENT_OFFLINEIFNOTCONNECTED = -5501;
	//public static final int DISPID_AMBIENT_PALETTE = -726;
	//public static final int DISPID_AMBIENT_RIGHTTOLEFT = -732;
	//public static final int DISPID_AMBIENT_SCALEUNITS = -707;
	public static final int DISPID_AMBIENT_SHOWGRABHANDLES = -711;
	public static final int DISPID_AMBIENT_SHOWHATCHING = -712;
	public static final int DISPID_AMBIENT_SILENT = -5502;
	public static final int DISPID_AMBIENT_SUPPORTSMNEMONICS = -714;
	//public static final int DISPID_AMBIENT_TEXTALIGN = -708;
	//public static final int DISPID_AMBIENT_TOPTOBOTTOM = -733;
	//public static final int DISPID_AMBIENT_TRANSFERPRIORITY = -728;
	public static final int DISPID_AMBIENT_UIDEAD = -710;
	public static final int DISPID_AMBIENT_USERMODE = -709;
	public static final int DISPID_BACKCOLOR = -501;
	public static final int DISPID_FONT = -512;
	public static final int DISPID_FONT_BOLD = 3;
	public static final int DISPID_FONT_CHARSET = 8; 
	public static final int DISPID_FONT_ITALIC = 4; 
	public static final int DISPID_FONT_NAME = 0;
	public static final int DISPID_FONT_SIZE = 2;
	public static final int DISPID_FONT_STRIKE = 6;
	public static final int DISPID_FONT_UNDER = 5;
	public static final int DISPID_FONT_WEIGHT = 7;
	public static final int DISPID_FORECOLOR = -513;
	//public static final int DISPID_READYSTATE = -525;
	//public static final int DISPID_READYSTATECHANGE = -609;
	public static final int DRAGDROP_S_DROP = 0x00040100;
	public static final int DRAGDROP_S_CANCEL = 0x00040101;
	public static final int DRAGDROP_S_USEDEFAULTCURSORS = 0x00040102;
	public static final int DROPEFFECT_NONE = 0; 
	public static final int DROPEFFECT_COPY = 1; 
	public static final int DROPEFFECT_MOVE = 2; 
	public static final int DROPEFFECT_LINK = 4; 
	public static final int DROPEFFECT_SCROLL = 0x80000000; 
	public static final int DV_E_FORMATETC = -2147221404;
	public static final int DV_E_STGMEDIUM = -2147221402;
	public static final int DV_E_TYMED = -2147221399;
	public static final int DVASPECT_CONTENT = 1;
	//public static final int DVASPECT_DOCPRINT = 8;
	//public static final int DVASPECT_ICON = 4;
	//public static final int DVASPECT_THUMBNAIL = 2;
	public static final int E_FAIL = -2147467259;
	public static final int E_INVALIDARG = -2147024809;
	public static final int E_NOINTERFACE = -2147467262;
	public static final int E_NOTIMPL = -2147467263;
	public static final int E_NOTSUPPORTED = 0x80040100;
	//public static final int E_NOTLICENSED = -2147221230;
	//public static final int E_OUTOFMEMORY = -2147024882;
	//public static final int E_POINTER = -2147467261;
	public static final int GMEM_FIXED = 0;
	//public static final int GMEM_MOVABLE = 2;
	//public static final int GMEM_NODISCARD = 32;
	public static final int GMEM_ZEROINIT = 64;
	public static final int GUIDKIND_DEFAULT_SOURCE_DISP_IID = 1;
	public static final int IMPLTYPEFLAG_FDEFAULT = 1;
	//public static final int IMPLTYPEFLAG_FDEFAULTVTABLE = 2048;
	public static final int IMPLTYPEFLAG_FRESTRICTED = 4;
	public static final int IMPLTYPEFLAG_FSOURCE = 2;
	public static final int LOCALE_SYSTEM_DEFAULT = 1024;
	public static final int LOCALE_USER_DEFAULT = 2048;
	//public static final int MEMCTX_TASK = 1;
	//public static final int OLEACTIVATEAUTO = 3; 
	//public static final int OLEACTIVATEDOUBLECLICK = 2; 
	//public static final int OLEACTIVATEGETFOCUS = 1; 
	//public static final int OLEACTIVATEMANUAL = 0; 
	//public static final int OLEAUTOMATIC = 0; 
	//public static final int OLECHANGED = 0; 
	public static final int OLECLOSE_NOSAVE = 1;
	//public static final int OLECLOSE_PROMPTSAVE = 2;
	public static final int OLECLOSE_SAVEIFDIRTY = 0;	
	//public static final int OLECLOSED = 2; 
	//public static final int OLECONTF_EMBEDDINGS = 1;
	//public static final int OLECONTF_LINKS = 2;
	//public static final int OLECONTF_ONLYIFRUNNING = 16;
	//public static final int OLECONTF_ONLYUSER = 8;
	//public static final int OLECONTF_OTHERS = 4;
	//public static final int OLEDEACTIVATEMANUAL = 1;
	//public static final int OLEDEACTIVATEONLOSEFOCUS = 0; 
	//public static final int OLEDECBORDER = 1;
	//public static final int OLEDECBORDERANDNIBS = 3;
	//public static final int OLEDECNIBS = 2;
	//public static final int OLEDECNONE = 0;
	//public static final int OLEDISPLAYCONTENT = 0; 
	//public static final int OLEDISPLAYICON = 1; 
	//public static final int OLEEITHER = 2; 
	public static final int OLEEMBEDDED = 1; 
	//public static final int OLEFROZEN = 1; 
	public static final int OLEIVERB_DISCARDUNDOSTATE = -6;
	//public static final int OLEIVERB_HIDE = -3;
	public static final int OLEIVERB_INPLACEACTIVATE = -5;	
	//public static final int OLEIVERB_OPEN = -2;
	public static final int OLEIVERB_PRIMARY = 0;
	//public static final int OLEIVERB_PROPERTIES = -7;
	//public static final int OLEIVERB_SHOW = -1;
	//public static final int OLEIVERB_UIACTIVATE = -4; 
	public static final int OLELINKED = 0; 
	//public static final int OLEMANUAL = 2; 
	//public static final int OLEMISC_ACTIVATEWHENVISIBLE = 256;
	//public static final int OLEMISC_ACTSLIKEBUTTON = 4096;
	//public static final int OLEMISC_ACTSLIKELABEL = 8192;
	//public static final int OLEMISC_ALIGNABLE = 32768;
	//public static final int OLEMISC_ALWAYSRUN = 2048;
	//public static final int OLEMISC_CANLINKBYOLE1 = 32;
	//public static final int OLEMISC_CANTLINKINSIDE = 16;
	//public static final int OLEMISC_IGNOREACTIVATEWHENVISIBLE = 524288;
	//public static final int OLEMISC_IMEMODE = 262144;
	//public static final int OLEMISC_INSERTNOTREPLACE = 4;
	//public static final int OLEMISC_INSIDEOUT = 128;
	//public static final int OLEMISC_INVISIBLEATRUNTIME = 1024;
	//public static final int OLEMISC_ISLINKOBJECT = 64;
	//public static final int OLEMISC_NOUIACTIVATE = 16384;
	//public static final int OLEMISC_ONLYICONIC = 2;
	//public static final int OLEMISC_RECOMPOSEONRESIZE = 1;
	//public static final int OLEMISC_RENDERINGISDEVICEINDEPENDENT = 512;
	//public static final int OLEMISC_SETCLIENTSITEFIRST = 131072;
	//public static final int OLEMISC_SIMPLEFRAME = 65536;
	//public static final int OLEMISC_STATIC = 8;
	//public static final int OLEMISC_SUPPORTSMULTILEVELUNDO = 2097152;
	//public static final int OLEMISC_WANTSTOMENUMERGE = 1048576;
	//public static final int OLENONE = 3; 
	//public static final int OLERENAMED = 3; 
	//public static final int OLERENDER_ASIS = 3;
	public static final int OLERENDER_DRAW = 1;
	//public static final int OLERENDER_FORMAT = 2;
	//public static final int OLERENDER_NONE = 0;
	//public static final int OLESAVED = 1; 
	//public static final int OLESIZEAUTOSIZE = 2; 
	//public static final int OLESIZECLIP = 0; 
	//public static final int OLESIZESTRETCH = 1; 
	//public static final int OLESIZEZOOM = 3; 
	//public static final int OLEWHICHMK_CONTAINER = 1;
	//public static final int OLEWHICHMK_OBJFULL = 3;
	//public static final int OLEWHICHMK_OBJREL = 2;
	public static final int S_FALSE = 1;
	public static final int S_OK = 0;
	public static final int STG_E_FILENOTFOUND = 0x80030002;
	public static final int STG_S_CONVERTED = 0x00030200;
	//public static final int STGC_CONSOLIDATE = 8;
	//public static final int STGC_DANGEROUSLYCOMMITMERELYTODISKCACHE = 4;
	public static final int STGC_DEFAULT = 0;
	//public static final int STGC_ONLYIFCURRENT = 2;
	//public static final int STGC_OVERWRITE = 1;
	public static final int STGM_CONVERT = 0x00020000;
	public static final int STGM_CREATE = 0x00001000;
	public static final int STGM_DELETEONRELEASE = 0x04000000;
	public static final int STGM_DIRECT = 0x00000000;
	public static final int STGM_DIRECT_SWMR = 0x00400000;
	public static final int STGM_FAILIFTHERE = 0x00000000;
	public static final int STGM_NOSCRATCH = 0x00100000;
	public static final int STGM_NOSNAPSHOT = 0x00200000;
	public static final int STGM_PRIORITY = 0x00040000;
	public static final int STGM_READ = 0x00000000;
	public static final int STGM_READWRITE = 0x00000002;
	public static final int STGM_SHARE_DENY_NONE = 0x00000040;
	public static final int STGM_SHARE_DENY_READ = 0x00000030;
	public static final int STGM_SHARE_DENY_WRITE = 0x00000020;
	public static final int STGM_SHARE_EXCLUSIVE = 0x00000010;
	public static final int STGM_SIMPLE = 0x08000000;
	public static final int STGM_TRANSACTED = 0x00010000;
	public static final int STGM_WRITE = 0x00000001;
	public static final int STGTY_STORAGE = 1;
	public static final int STGTY_STREAM = 2;
	public static final int STGTY_LOCKBYTES = 3;
	public static final int STGTY_PROPERTY = 4;
	//public static final int TYMED_ENHMF = 64;
	//public static final int TYMED_FILE = 2;
	//public static final int TYMED_GDI = 16;
	public static final int TYMED_HGLOBAL = 1;
	//public static final int TYMED_ISTORAGE = 8;
	//public static final int TYMED_ISTREAM = 4;
	//public static final int TYMED_MFPICT = 32;
	//public static final int TYMED_NULL = 0;
	public static final short DISPATCH_METHOD = 0x1;
	public static final short DISPATCH_PROPERTYGET = 0x2;
	public static final short DISPATCH_PROPERTYPUT = 0x4;
	public static final short DISPATCH_PROPERTYPUTREF = 0x8;
	//public static final short DISPID_CONSTRUCTOR = -6;
	//public static final short DISPID_DESTRUCTOR = -7;
	//public static final short DISPID_EVALUATE = -5;
	//public static final short DISPID_NEWENUM = -4;
	public static final short DISPID_PROPERTYPUT = -3;
	//public static final short DISPID_UNKNOWN = -1;
	//public static final short DISPID_VALUE = 0;
	public static final short VT_BOOL = 11;
	public static final short VT_BSTR = 8;
	public static final short VT_BYREF = 16384;
	public static final short VT_CY = 6;
	public static final short VT_DATE = 7;
	public static final short VT_DISPATCH = 9;
	public static final short VT_EMPTY = 0;
	public static final short VT_ERROR = 10;
	public static final short VT_I1 = 16;
	public static final short VT_I2 = 2;
	public static final short VT_I4 = 3;
	public static final short VT_I8 = 20;
	public static final short VT_NULL = 1;
	public static final short VT_R4 = 4;
	public static final short VT_R8 = 5;
	public static final short VT_UI1 = 17;
	public static final short VT_UI2 = 18;
	public static final short VT_UI4 = 19;
	public static final short VT_UNKNOWN = 13;
	public static final short VT_VARIANT = 12;
	/*
	 * GOOGLE: IE7 returns a variant of type 130 from the name field
	 * of a JavaScript exception, and we can't find documentation of it
	 * anywhere.  It appears to contain a pointer to a string constant
	 * that behaves like a VT_BSTR, so we are treating it the same way.
	 * Note that this is a gross hack using a totally undocumented
	 * feature, and Microsoft may break these assumptions at any time.
	 */
	public static final short VT_WEIRD_IE7_BSTR = 130;
	public static final short VARIANT_TRUE = -1;
	public static final short VARIANT_FALSE = 0;

private static GUID IIDFromString(String lpsz) {
	int length = lpsz.length();
	char[] buffer = new char[length + 1];
	lpsz.getChars(0, length, buffer, 0);
	GUID lpiid = new GUID();
	if (COM.IIDFromString(buffer, lpiid) == COM.S_OK) return lpiid;
	return null;
}

/** Natives */
public static final native int CLSIDFromProgID(char[] lpszProgID, GUID pclsid);
public static final native int CLSIDFromString(char[] lpsz, GUID pclsid);
public static final native int CoCreateInstance(GUID rclsid, int pUnkOuter, int dwClsContext, GUID riid, int[] ppv); 
public static final native void CoFreeUnusedLibraries();
public static final native int CoGetClassObject(GUID rclsid, int dwClsContext, int pServerInfo, GUID riid, int[] ppv);
public static final native int CoLockObjectExternal(int pUnk, boolean fLock, boolean fLastUnlockReleases);
public static final native int CoTaskMemAlloc(int cb);
public static final native void CoTaskMemFree(int pv);
public static final native int DoDragDrop(int pDataObject, int pDropSource, int dwOKEffect, int[] pdwEffect);
public static final native int GetClassFile(char[] szFileName, GUID clsid);
public static final native int IIDFromString(char[] lpsz, GUID lpiid);
public static final native boolean IsEqualGUID(GUID rguid1, GUID rguid2);
public static final native void MoveMemory(int Destination, FORMATETC Source, int Length);
public static final native void MoveMemory(int DestinationPtr, GUID Source, int Length);
public static final native void MoveMemory(int DestinationPtr, OLEINPLACEFRAMEINFO Source, int Length);
public static final native void MoveMemory(int Destination, STATSTG Source, int Length);
public static final native void MoveMemory(int Destination, STGMEDIUM Source, int Length);
public static final native void MoveMemory(STGMEDIUM Destination, int SourcePtr, int Length);
public static final native void MoveMemory(DISPPARAMS Destination, int SourcePtr, int Length);
public static final native void MoveMemory(FORMATETC Destination, int Source, int Length);
public static final native void MoveMemory(GUID Destination, int SourcePtr, int Length);
public static final native void MoveMemory(STATSTG Destination, int Source, int Length);
public static final native void MoveMemory(TYPEATTR Destination, int SourcePtr, int Length);
public static final native void MoveMemory(RECT Destination, int Source, int Length);
public static final native void MoveMemory(FUNCDESC Destination, int Source, int Length);
public static final native void MoveMemory(VARDESC Destination, int Source, int Length);
public static final native int OleCreate(GUID rclsid, GUID riid, int renderopt, FORMATETC pFormatEtc, int pClientSite, int pStg, int[] ppvObject);
public static final native int OleCreateFromFile(GUID rclsid, char[] lpszFileName, GUID riid, int renderopt, FORMATETC pFormatEtc, int pClientSite, int pStg, int[] ppvObj); 
public static final native int OleCreatePropertyFrame(int hwndOwner,int x, int y, char[] lpszCaption, int cObjects, int[] lplpUnk, int cPages, int lpPageClsID, int lcid, int dwReserved, int lpvReserved);
public static final native int OleDraw(int pUnk, int dwAspect, int hdcDraw, int lprcBounds);
public static final native int OleFlushClipboard();
public static final native int OleGetClipboard(int[] ppDataObject);
public static final native int OleIsCurrentClipboard(int pDataObject);
public static final native boolean OleIsRunning(int pObject);
public static final native int OleLoad(int pStg, GUID riid, int pClientSite, int[] ppvObj);
public static final native int OleRun(int pUnknown);
public static final native int OleSave(int pPS, int pStg,boolean fSameAsLoad);
public static final native int OleSetClipboard(int pDataObject);
public static final native int OleSetContainedObject(int pUnk, boolean fContained);
public static final native int OleSetMenuDescriptor(int holemenu, int hwndFrame, int hwndActiveObject, int lpFrame, int lpActiveObj);
public static final native int OleTranslateColor(int clr, int hpal, int[] pcolorref);
public static final native int ProgIDFromCLSID(GUID clsid, int[] lplpszProgID);
public static final native int RegisterDragDrop(int hwnd, int pDropTarget);
public static final native  void ReleaseStgMedium(int pmedium); 
public static final native int RevokeDragDrop(int hwnd);
public static final native int StgCreateDocfile(char[] pwcsName, int grfMode, int reserved, int[] ppstgOpen);
public static final native int StgIsStorageFile(char[] pwcsName);
public static final native int StgOpenStorage(char[] pwcsName, int pstgPriority, int grfMode, int snbExclude, int reserved, int[] ppstgOpen);
public static final native int StringFromCLSID(GUID rclsid, int[] ppsz); 
public static final native int SysAllocString(char [] sz);
public static final native int SysAllocStringLen(char [] sz, int len); // GOOGLE
public static final native void SysFreeString(int bstr);
public static final native int SysStringByteLen(int bstr);
public static final native int VariantChangeType(int pvargDest, int pvarSrc, short wFlags, short vt);
public static final native int VariantClear(int pvarg);
public static final native void VariantInit(int pvarg);
public static final native int VtblCall(int fnNumber, int ppVtbl, char[] arg0);
public static final native int VtblCall(int fnNumber, int ppVtbl, char[] arg0, char[] arg1);
public static final native int VtblCall(int fnNumber, int ppVtbl, char[] arg0, int arg1);
public static final native int VtblCall(int fnNumber, int ppVtbl, char[] arg0, int arg1, int arg2, int arg3, int[] arg4);
public static final native int VtblCall(int fnNumber, int ppVtbl, char[] arg0, int arg1, int arg2, int arg3, int arg4, int[] arg5);
public static final native int VtblCall(int fnNumber, int ppVtbl, int[] arg0);
public static final native int VtblCall(int fnNumber, int ppVtbl, int arg0, int[] arg1);
public static final native int VtblCall(int fnNumber, int ppVtbl, int arg0, int arg1);
public static final native int VtblCall(int fnNumber, int ppVtbl, int arg0, int arg1, int[] arg2);
public static final native int VtblCall(int fnNumber, int ppVtbl, int arg0, int arg1, int arg2);
public static final native int VtblCall(int fnNumber, int ppVtbl, int arg0, int arg1, DVTARGETDEVICE arg2, SIZE arg3);
public static final native int VtblCall(int fnNumber, int ppVtbl, int arg0, int arg1, GUID arg2, int arg3, int[] arg4);
public static final native int VtblCall(int fnNumber, int ppVtbl, int arg0, FORMATETC arg1, int[] arg2);
public static final native int VtblCall(int fnNumber, int ppVtbl, int arg0, GUID arg1);
public static final native int VtblCall(int fnNumber, int ppVtbl, int arg0, GUID arg1, int arg2, int arg3);
public static final native int VtblCall(int fnNumber, int ppVtbl, int arg0, GUID arg1, int arg2, int arg3, DISPPARAMS arg4, int arg5, EXCEPINFO arg6, int[] arg7);
public static final native int VtblCall(int fnNumber, int ppVtbl, int arg0, STATSTG arg1, int[] arg2);
public static final native int VtblCall(int fnNumber, int ppVtbl, MSG arg0);
public static final native int VtblCall(int fnNumber, int ppVtbl, int arg0, MSG arg1, int arg2, int arg3, int arg4, RECT arg5);
public static final native int VtblCall(int fnNumber, int ppVtbl, int arg0, SIZE arg1);
public static final native int VtblCall(int fnNumber, int ppVtbl, int arg0, boolean arg1);
public static final native int VtblCall(int fnNumber, int ppVtbl, CAUUID arg0);
public static final native int VtblCall(int fnNumber, int ppVtbl, CONTROLINFO arg0);
public static final native int VtblCall(int fnNumber, int ppVtbl, FORMATETC arg0);
public static final native int VtblCall(int fnNumber, int ppVtbl, FORMATETC arg0, STGMEDIUM arg1);
public static final native int VtblCall(int fnNumber, int ppVtbl, FORMATETC arg0, STGMEDIUM arg1, boolean arg2);
public static final native int VtblCall(int fnNumber, int ppVtbl, GUID arg0);
public static final native int VtblCall(int fnNumber, int ppVtbl, GUID arg0, int[] arg1);
public static final native int VtblCall(int fnNumber, int ppVtbl, GUID arg0, int arg1, int arg2, int arg3, int[] arg4);
public static final native int VtblCall(int fnNumber, int ppVtbl, GUID arg0, int arg1, int arg2, int arg3, int arg4);
public static final native int VtblCall(int fnNumber, int ppVtbl, GUID arg0, int arg1, OLECMD arg2, OLECMDTEXT arg3);
public static final native int VtblCall(int fnNumber, int ppVtbl, LICINFO arg0);
public static final native int VtblCall(int fnNumber, int ppVtbl, RECT arg0, int arg1, boolean arg2);
public static final native int VtblCall(int fnNumber, int ppVtbl, RECT arg0, RECT arg1);
public static final native int VtblCall(int fnNumber, int ppVtbl, RECT arg0);
public static final native int VtblCall(int fnNumber, int ppVtbl, int arg0, int[] arg1, int[] arg2, int[] arg3, int[] arg4);
public static final native int VtblCall(int fnNumber, int ppVtbl, int arg0, int[] arg1, int arg2, int[] arg3);
public static final native int WriteClassStg(int pStg, GUID rclsid);

/** Accessibility constants */
public static final int CHILDID_SELF = 0;
public static final int CO_E_OBJNOTCONNECTED = 0x800401FD;
//public static final int ROLE_SYSTEM_TITLEBAR = 0x1;
public static final int ROLE_SYSTEM_MENUBAR = 0x2;
public static final int ROLE_SYSTEM_SCROLLBAR = 0x3;
//public static final int ROLE_SYSTEM_GRIP = 0x4;
//public static final int ROLE_SYSTEM_SOUND = 0x5;
//public static final int ROLE_SYSTEM_CURSOR = 0x6;
//public static final int ROLE_SYSTEM_CARET = 0x7;
//public static final int ROLE_SYSTEM_ALERT = 0x8;
public static final int ROLE_SYSTEM_WINDOW = 0x9;
public static final int ROLE_SYSTEM_CLIENT = 0xa;
public static final int ROLE_SYSTEM_MENUPOPUP = 0xb;
public static final int ROLE_SYSTEM_MENUITEM = 0xc;
public static final int ROLE_SYSTEM_TOOLTIP = 0xd;
//public static final int ROLE_SYSTEM_APPLICATION = 0xe;
//public static final int ROLE_SYSTEM_DOCUMENT = 0xf;
//public static final int ROLE_SYSTEM_PANE = 0x10;
//public static final int ROLE_SYSTEM_CHART = 0x11;
public static final int ROLE_SYSTEM_DIALOG = 0x12;
//public static final int ROLE_SYSTEM_BORDER = 0x13;
//public static final int ROLE_SYSTEM_GROUPING = 0x14;
public static final int ROLE_SYSTEM_SEPARATOR = 0x15;
public static final int ROLE_SYSTEM_TOOLBAR = 0x16;
//public static final int ROLE_SYSTEM_STATUSBAR = 0x17;
public static final int ROLE_SYSTEM_TABLE = 0x18;
public static final int ROLE_SYSTEM_COLUMNHEADER = 0x19;
public static final int ROLE_SYSTEM_ROWHEADER = 0x1a;
//public static final int ROLE_SYSTEM_COLUMN = 0x1b;
//public static final int ROLE_SYSTEM_ROW = 0x1c;
public static final int ROLE_SYSTEM_CELL = 0x1d;
public static final int ROLE_SYSTEM_LINK = 0x1e;
//public static final int ROLE_SYSTEM_HELPBALLOON = 0x1f;
//public static final int ROLE_SYSTEM_CHARACTER = 0x20;
public static final int ROLE_SYSTEM_LIST = 0x21;
public static final int ROLE_SYSTEM_LISTITEM = 0x22;
public static final int ROLE_SYSTEM_OUTLINE = 0x23;
public static final int ROLE_SYSTEM_OUTLINEITEM = 0x24;
public static final int ROLE_SYSTEM_PAGETAB = 0x25;
//public static final int ROLE_SYSTEM_PROPERTYPAGE = 0x26;
//public static final int ROLE_SYSTEM_INDICATOR = 0x27;
//public static final int ROLE_SYSTEM_GRAPHIC = 0x28;
public static final int ROLE_SYSTEM_STATICTEXT = 0x29;
public static final int ROLE_SYSTEM_TEXT = 0x2a;
public static final int ROLE_SYSTEM_PUSHBUTTON = 0x2b;
public static final int ROLE_SYSTEM_CHECKBUTTON = 0x2c;
public static final int ROLE_SYSTEM_RADIOBUTTON = 0x2d;
public static final int ROLE_SYSTEM_COMBOBOX = 0x2e;
//public static final int ROLE_SYSTEM_DROPLIST = 0x2f;
public static final int ROLE_SYSTEM_PROGRESSBAR = 0x30;
//public static final int ROLE_SYSTEM_DIAL = 0x31;
//public static final int ROLE_SYSTEM_HOTKEYFIELD = 0x32;
public static final int ROLE_SYSTEM_SLIDER = 0x33;
//public static final int ROLE_SYSTEM_SPINBUTTON = 0x34;
//public static final int ROLE_SYSTEM_DIAGRAM = 0x35;
//public static final int ROLE_SYSTEM_ANIMATION = 0x36;
//public static final int ROLE_SYSTEM_EQUATION = 0x37;
//public static final int ROLE_SYSTEM_BUTTONDROPDOWN = 0x38;
//public static final int ROLE_SYSTEM_BUTTONMENU = 0x39;
//public static final int ROLE_SYSTEM_BUTTONDROPDOWNGRID = 0x3a;
//public static final int ROLE_SYSTEM_WHITESPACE = 0x3b;
public static final int ROLE_SYSTEM_PAGETABLIST = 0x3c;
//public static final int ROLE_SYSTEM_CLOCK = 0x3d;
public static final int STATE_SYSTEM_NORMAL = 0;
//public static final int STATE_SYSTEM_UNAVAILABLE = 0x1;
public static final int STATE_SYSTEM_SELECTED = 0x2;
public static final int STATE_SYSTEM_FOCUSED = 0x4;
public static final int STATE_SYSTEM_PRESSED = 0x8;
public static final int STATE_SYSTEM_CHECKED = 0x10;
//public static final int STATE_SYSTEM_MIXED = 0x20;
//public static final int STATE_SYSTEM_INDETERMINATE = STATE_SYSTEM_MIXED;
public static final int STATE_SYSTEM_READONLY = 0x40;
public static final int STATE_SYSTEM_HOTTRACKED = 0x80;
//public static final int STATE_SYSTEM_DEFAULT = 0x100;
public static final int STATE_SYSTEM_EXPANDED = 0x200;
public static final int STATE_SYSTEM_COLLAPSED = 0x400;
public static final int STATE_SYSTEM_BUSY = 0x800;
//public static final int STATE_SYSTEM_FLOATING = 0x1000;
//public static final int STATE_SYSTEM_MARQUEED = 0x2000;
//public static final int STATE_SYSTEM_ANIMATED = 0x4000;
public static final int STATE_SYSTEM_INVISIBLE = 0x8000;
public static final int STATE_SYSTEM_OFFSCREEN = 0x10000;
public static final int STATE_SYSTEM_SIZEABLE = 0x20000;
//public static final int STATE_SYSTEM_MOVEABLE = 0x40000;
//public static final int STATE_SYSTEM_SELFVOICING = 0x80000;
public static final int STATE_SYSTEM_FOCUSABLE = 0x100000;
public static final int STATE_SYSTEM_SELECTABLE = 0x200000;
public static final int STATE_SYSTEM_LINKED = 0x400000;
//public static final int STATE_SYSTEM_TRAVERSED = 0x800000;
public static final int STATE_SYSTEM_MULTISELECTABLE = 0x1000000;
//public static final int STATE_SYSTEM_EXTSELECTABLE = 0x2000000;
//public static final int STATE_SYSTEM_ALERT_LOW = 0x4000000;
//public static final int STATE_SYSTEM_ALERT_MEDIUM = 0x8000000;
//public static final int STATE_SYSTEM_ALERT_HIGH = 0x10000000;
//public static final int STATE_SYSTEM_PROTECTED = 0x20000000;
//public static final int STATE_SYSTEM_VALID = 0x3fffffff;

/** Accessibility natives */
public static final native int CreateStdAccessibleObject (int hwnd, int idObject, GUID riidInterface, int[] ppvObject);
public static final native int LresultFromObject (GUID riid, int wParam, int pAcc);
public static final native int VtblCall(int fnNumber, int ppVtbl, int arg0, int arg1, int arg2, int arg3);
public static final native int VtblCall(int fnNumber, int ppVtbl, int arg0, int arg1, int arg2, int arg3, int arg4);
public static final native int VtblCall(int fnNumber, int ppVtbl, int arg0, int arg1, int arg2, int arg3, int arg4, int arg5);
public static final native int VtblCall(int fnNumber, int ppVtbl, int arg0, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6, int arg7);

}
