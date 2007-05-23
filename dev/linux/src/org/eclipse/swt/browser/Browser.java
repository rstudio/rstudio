/*******************************************************************************
 * Copyright (c) 2003, 2006 IBM Corporation and others.
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

import com.google.gwt.dev.shell.moz.LowLevelMoz; // GOOGLE

import java.io.*;
import java.util.*;
import org.eclipse.swt.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.internal.*;
import org.eclipse.swt.internal.gtk.*;
import org.eclipse.swt.internal.mozilla.*;
import org.eclipse.swt.layout.*;

/**
 * Instances of this class implement the browser user interface
 * metaphor.  It allows the user to visualize and navigate through
 * HTML documents.
 * <p>
 * Note that although this class is a subclass of <code>Composite</code>,
 * it does not make sense to set a layout on it.
 * </p><p>
 * IMPORTANT: This class is <em>not</em> intended to be subclassed.
 * </p>
 * 
 * @since 3.0
 */
public class Browser extends Composite {
	int /*long*/ embedHandle;
	int /*long*/ mozillaHandle;
	nsIWebBrowser webBrowser;

	/* Interfaces for this Mozilla embedding notification */
	XPCOMObject supports;
	XPCOMObject weakReference;
	XPCOMObject webProgressListener;
	XPCOMObject	webBrowserChrome;
	XPCOMObject webBrowserChromeFocus;
	XPCOMObject embeddingSiteWindow;
	XPCOMObject interfaceRequestor;
	XPCOMObject supportsWeakReference;
	XPCOMObject contextMenuListener;	
	XPCOMObject uriContentListener;
	XPCOMObject tooltipListener;
	int chromeFlags = nsIWebBrowserChrome.CHROME_DEFAULT;
	int refCount = 0;
	int /*long*/ request;
	Point location;
	Point size;
	boolean addressBar, menuBar, statusBar, toolBar;
	boolean visible;
	Shell tip = null;

	/* External Listener management */
	CloseWindowListener[] closeWindowListeners = new CloseWindowListener[0];
	LocationListener[] locationListeners = new LocationListener[0];
	OpenWindowListener[] openWindowListeners = new OpenWindowListener[0];
	ProgressListener[] progressListeners = new ProgressListener[0];
	StatusTextListener[] statusTextListeners = new StatusTextListener[0];
	TitleListener[] titleListeners = new TitleListener[0];
	VisibilityWindowListener[] visibilityWindowListeners = new VisibilityWindowListener[0];

	static nsIAppShell AppShell;
	static WindowCreator WindowCreator;
	static int BrowserCount;
	static boolean mozilla, ignoreDispose, usingProfile;
	static Callback eventCallback;
	static int /*long*/ eventProc;

	/* Package Name */
	static final String PACKAGE_PREFIX = "org.eclipse.swt.browser."; //$NON-NLS-1$
	static final String ADD_WIDGET_KEY = "org.eclipse.swt.internal.addWidget"; //$NON-NLS-1$
	static final String NO_INPUT_METHOD = "org.eclipse.swt.internal.gtk.noInputMethod"; //$NON-NLS-1$
	static final String URI_FROMMEMORY = "file:///"; //$NON-NLS-1$
	static final String ABOUT_BLANK = "about:blank"; //$NON-NLS-1$
	static final String PREFERENCE_LANGUAGES = "intl.accept_languages"; //$NON-NLS-1$
	static final String PREFERENCE_CHARSET = "intl.charset.default"; //$NON-NLS-1$
	static final String SEPARATOR_LOCALE = "-"; //$NON-NLS-1$
	static final String TOKENIZER_LOCALE = ","; //$NON-NLS-1$
	static final String PROFILE_DIR = "/eclipse"; //$NON-NLS-1$
	static final int STOP_PROPOGATE = 1;

/**
 * Constructs a new instance of this class given its parent
 * and a style value describing its behavior and appearance.
 * <p>
 * The style value is either one of the style constants defined in
 * class <code>SWT</code> which is applicable to instances of this
 * class, or must be built by <em>bitwise OR</em>'ing together 
 * (that is, using the <code>int</code> "|" operator) two or more
 * of those <code>SWT</code> style constants. The class description
 * lists the style constants that are applicable to the class.
 * Style bits are also inherited from superclasses.
 * </p>
 *
 * @param parent a widget which will be the parent of the new instance (cannot be null)
 * @param style the style of widget to construct
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the parent is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the parent</li>
 * </ul>
 * @exception SWTError <ul>
 *    <li>ERROR_NO_HANDLES if a handle could not be obtained for browser creation</li>
 * </ul>
 * 
 * @see #getStyle
 * 
 * @since 3.0
 */
public Browser(Composite parent, int style) {
	super(fixIM(parent), style);
	Display display = parent.getDisplay();	
	display.setData(NO_INPUT_METHOD, null);
	
	int /*long*/[] result = new int /*long*/[1];
	if (!mozilla) {
		/*
		* GOOGLE: We ship our own bundled version of Mozilla; ignore the
		* environment and just use ours.
		*/
		String mozillaPath = LowLevelMoz.getMozillaDirectory();
		int ptr;
		//String mozillaPath = null;
		//int /*long*/ ptr = OS.getenv(Converter.wcsToMbcs(null, XPCOM.MOZILLA_FIVE_HOME, true));
		//if (ptr != 0) {
		//	int length = OS.strlen(ptr);
		//	byte[] buffer = new byte[length];
		//	OS.memmove(buffer, ptr, length);
		//	mozillaPath = new String (Converter.mbcsToWcs (null, buffer));
		//}
		//if (mozillaPath == null) {
		//	dispose();
		//	SWT.error(SWT.ERROR_NO_HANDLES, null, " [Unknown Mozilla path (MOZILLA_FIVE_HOME not set)]"); //$NON-NLS-1$
		//}

		/*
		* Note.  Embedding a Mozilla GTK1.2 causes a crash.  The workaround
		* is to check the version of GTK used by Mozilla by looking for
		* the libwidget_gtk.so library used by Mozilla GTK1.2. Mozilla GTK2
		* uses the libwidget_gtk2.so library.   
		*/
		File file = new File(mozillaPath, "components/libwidget_gtk.so"); //$NON-NLS-1$
		if (file.exists()) {
			dispose();
			SWT.error(SWT.ERROR_NO_HANDLES, null, " [Mozilla GTK2 required (GTK1.2 detected)]"); //$NON-NLS-1$							
		}

		try {
			/*
			 * GOOGLE: Eclipse seems to always add /usr/lib/mozilla onto the
			 * java.library.path, which results in loading the system-supplied
			 * libraries to fulfill dependencies rather than the ones we built.
			 * So, to get around this, we load each library specifically.
			 */
			System.load(mozillaPath + "/libnspr4.so");
			System.load(mozillaPath + "/libplc4.so");
			System.load(mozillaPath + "/libplds4.so");

			/*
			* GOOGLE: For some reason, swt-mozilla won't load unless xpcom is
			* loaded first. I don't know why it fails for me, or why it
			* doesn't fail for the SWT guys, but this seems to fix it.
			*
			* This may in fact be related to the problem above.
			*/
			System.load (mozillaPath + "/libxpcom.so");
			System.load(mozillaPath + "/libmozz.so");
			System.load(mozillaPath + "/libmozjs.so");
			System.load(mozillaPath + "/libgkgfx.so");
			System.load(mozillaPath + "/components/libtypeaheadfind.so");
			System.load(mozillaPath + "/components/libembedcomponents.so");
			System.load(mozillaPath + "/components/libpref.so");
			System.load(mozillaPath + "/components/libsystem-pref.so");
			System.load(mozillaPath + "/components/libnecko.so");
			System.load(mozillaPath + "/components/libcaps.so");
			System.load(mozillaPath + "/components/libjsd.so");
			System.load(mozillaPath + "/libgtkembedmoz.so");
			Library.loadLibrary ("swt-mozilla"); //$NON-NLS-1$
		} catch (UnsatisfiedLinkError e) {
			try {
				/* 
				 * The initial loadLibrary attempt may have failed as a result of the user's
				 * system not having libstdc++.so.6 installed, so try to load the alternate
				 * swt mozilla library that depends on libswtc++.so.5 instead.
				 */
				Library.loadLibrary ("swt-mozilla-gcc3"); //$NON-NLS-1$
			} catch (UnsatisfiedLinkError ex) {
				dispose ();
				/*
				 * Print the error from the first failed attempt since at this point it's
				 * known that the failure was not due to the libstdc++.so.6 dependency.
				 */
				SWT.error (SWT.ERROR_NO_HANDLES, e);
			}
		}

		/*
		 * Try to load the various profile libraries until one is found that loads successfully:
		 * - mozilla14profile/mozilla14profile-gcc should succeed for mozilla 1.4 - 1.6
		 * - mozilla17profile/mozilla17profile-gcc should succeed for mozilla 1.7.x and firefox
		 * - mozilla18profile/mozilla18profile-gcc should succeed for mozilla 1.8.x (seamonkey)
		 */
		try {
			Library.loadLibrary ("swt-mozilla14-profile"); //$NON-NLS-1$
			usingProfile = true;
		} catch (UnsatisfiedLinkError e1) {
			try {
				Library.loadLibrary ("swt-mozilla17-profile"); //$NON-NLS-1$
				usingProfile = true;
			} catch (UnsatisfiedLinkError e2) {
				try {
					Library.loadLibrary ("swt-mozilla14-profile-gcc3"); //$NON-NLS-1$
					usingProfile = true;
				} catch (UnsatisfiedLinkError e3) {
					try {
						Library.loadLibrary ("swt-mozilla17-profile-gcc3"); //$NON-NLS-1$
						usingProfile = true;
					} catch (UnsatisfiedLinkError e4) {
						try {
							Library.loadLibrary ("swt-mozilla18-profile"); //$NON-NLS-1$
							usingProfile = true;
						} catch (UnsatisfiedLinkError e5) {
							try {
								Library.loadLibrary ("swt-mozilla18-profile-gcc3"); //$NON-NLS-1$
								usingProfile = true;
							} catch (UnsatisfiedLinkError e6) {
								/* 
								* fail silently, the Browser will still work without profile support
								* but will abort any attempts to navigate to HTTPS pages
								*/
							}
						}
					}
				}
			}
		}

		int /*long*/[] retVal = new int /*long*/[1];
		nsEmbedString pathString = new nsEmbedString(mozillaPath);
		int rc = XPCOM.NS_NewLocalFile(pathString.getAddress(), true, retVal);
		pathString.dispose();
		if (rc != XPCOM.NS_OK) error(rc);
		if (retVal[0] == 0) error(XPCOM.NS_ERROR_NULL_POINTER);
		
		nsILocalFile localFile = new nsILocalFile(retVal[0]);
		rc = XPCOM.NS_InitEmbedding(localFile.getAddress(), 0);
		localFile.Release();
		if (rc != XPCOM.NS_OK) {
			dispose();
			SWT.error(SWT.ERROR_NO_HANDLES, null, " [NS_InitEmbedding "+mozillaPath+" error "+rc+"]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		rc = XPCOM.NS_GetComponentManager(result);
		if (rc != XPCOM.NS_OK) error(rc);
		if (result[0] == 0) error(XPCOM.NS_NOINTERFACE);
		
		nsIComponentManager componentManager = new nsIComponentManager(result[0]);
		result[0] = 0;
		rc = componentManager.CreateInstance(XPCOM.NS_APPSHELL_CID, 0, nsIAppShell.NS_IAPPSHELL_IID, result);
		if (rc != XPCOM.NS_OK) error(rc);
		if (result[0] == 0) error(XPCOM.NS_NOINTERFACE);
		
		AppShell = new nsIAppShell(result[0]);
		rc = AppShell.Create(0, null);
		if (rc != XPCOM.NS_OK) error(rc);
		rc = AppShell.Spinup();
		if (rc != XPCOM.NS_OK) error(rc);
		
		WindowCreator = new WindowCreator();
		WindowCreator.AddRef();
		
		rc = XPCOM.NS_GetServiceManager(result);
		if (rc != XPCOM.NS_OK) error(rc);
		if (result[0] == 0) error(XPCOM.NS_NOINTERFACE);
		
		nsIServiceManager serviceManager = new nsIServiceManager(result[0]);
		result[0] = 0;		
		byte[] buffer = XPCOM.NS_WINDOWWATCHER_CONTRACTID.getBytes();
		byte[] aContractID = new byte[buffer.length + 1];
		System.arraycopy(buffer, 0, aContractID, 0, buffer.length);
		rc = serviceManager.GetServiceByContractID(aContractID, nsIWindowWatcher.NS_IWINDOWWATCHER_IID, result);
		if (rc != XPCOM.NS_OK) error(rc);
		if (result[0] == 0) error(XPCOM.NS_NOINTERFACE);		

		nsIWindowWatcher windowWatcher = new nsIWindowWatcher(result[0]);
		result[0] = 0;
		rc = windowWatcher.SetWindowCreator(WindowCreator.getAddress());
		if (rc != XPCOM.NS_OK) error(rc);
		windowWatcher.Release();

		/* specify the user profile directory */
		if (usingProfile) {
			buffer = Converter.wcsToMbcs(null, XPCOM.NS_DIRECTORYSERVICE_CONTRACTID, true);
			rc = serviceManager.GetServiceByContractID(buffer, nsIDirectoryService.NS_IDIRECTORYSERVICE_IID, result);
			if (rc != XPCOM.NS_OK) error(rc);
			if (result[0] == 0) error(XPCOM.NS_NOINTERFACE);

			nsIDirectoryService directoryService = new nsIDirectoryService(result[0]);
			result[0] = 0;
			rc = directoryService.QueryInterface(nsIProperties.NS_IPROPERTIES_IID, result);
			if (rc != XPCOM.NS_OK) error(rc);
			if (result[0] == 0) error(XPCOM.NS_NOINTERFACE);
			directoryService.Release();

			nsIProperties properties = new nsIProperties(result[0]);
			result[0] = 0;
			buffer = Converter.wcsToMbcs(null, XPCOM.NS_APP_APPLICATION_REGISTRY_DIR, true);
			rc = properties.Get(buffer, nsIFile.NS_IFILE_IID, result);
			if (rc != XPCOM.NS_OK) error(rc);
			if (result[0] == 0) error(XPCOM.NS_NOINTERFACE);
			properties.Release();

			nsIFile profileDir = new nsIFile(result[0]);
			result[0] = 0;
			int /*long*/ path = XPCOM.nsEmbedCString_new();
			rc = profileDir.GetNativePath(path);
			if (rc != XPCOM.NS_OK) error(rc);
			profileDir.Release(); //

			int length = XPCOM.nsEmbedCString_Length(path);
			ptr = XPCOM.nsEmbedCString_get(path);
			buffer = new byte [length];
			XPCOM.memmove(buffer, ptr, length);
			XPCOM.nsEmbedCString_delete(path);
			String string = new String(Converter.mbcsToWcs(null, buffer)) + PROFILE_DIR; 
			pathString = new nsEmbedString(string);
			rc = XPCOM.NS_NewLocalFile(pathString.getAddress(), true, result);
			if (rc != XPCOM.NS_OK) error(rc);
			if (result[0] == 0) error(XPCOM.NS_ERROR_NULL_POINTER);
			pathString.dispose(); //

			profileDir = new nsIFile(result[0]);
			result[0] = 0;

			rc = XPCOM_PROFILE.NS_NewProfileDirServiceProvider(true, result);
			if (rc != XPCOM.NS_OK) error(rc);
			if (result[0] == 0) error(XPCOM.NS_NOINTERFACE);

			final int /*long*/ dirServiceProvider = result[0];
			result[0] = 0;
			rc = XPCOM_PROFILE.ProfileDirServiceProvider_Register(dirServiceProvider);
			if (rc != XPCOM.NS_OK) error(rc);
			rc = XPCOM_PROFILE.ProfileDirServiceProvider_SetProfileDir(dirServiceProvider, profileDir.getAddress());
			if (rc != XPCOM.NS_OK) error(rc);

			getDisplay().addListener(SWT.Dispose, new Listener() {
				public void handleEvent(Event e) {
					XPCOM_PROFILE.ProfileDirServiceProvider_Shutdown(dirServiceProvider);
				}
			});
		}

		/*
		 * As a result of using a common profile (or none at all), the user cannot specify
		 * their locale and charset.  The fix for this is to set mozilla's locale and charset
		 * preference values according to the user's current locale and charset.
		 */
		buffer = XPCOM.NS_PREFSERVICE_CONTRACTID.getBytes();
		aContractID = new byte[buffer.length + 1];
		System.arraycopy(buffer, 0, aContractID, 0, buffer.length);
		rc = serviceManager.GetServiceByContractID(aContractID, nsIPrefService.NS_IPREFSERVICE_IID, result);
		serviceManager.Release();
		if (rc != XPCOM.NS_OK) error(rc);
		if (result[0] == 0) error(XPCOM.NS_NOINTERFACE);

		nsIPrefService prefService = new nsIPrefService(result[0]);
		result[0] = 0;
		buffer = new byte[1];
		rc = prefService.GetBranch(buffer, result);	/* empty buffer denotes root preference level */
		prefService.Release();
		if (rc != XPCOM.NS_OK) error(rc);
		if (result[0] == 0) error(XPCOM.NS_NOINTERFACE);

		nsIPrefBranch prefBranch = new nsIPrefBranch(result[0]);
		result[0] = 0;

		/* get Mozilla's current locale preference value */
		String prefLocales = null;
		nsIPrefLocalizedString localizedString = null;
		buffer = Converter.wcsToMbcs(null, PREFERENCE_LANGUAGES, true);
		rc = prefBranch.GetComplexValue(buffer, nsIPrefLocalizedString.NS_IPREFLOCALIZEDSTRING_IID, result);
		/* 
		 * Feature of Debian.  For some reason attempting to query for the current locale
		 * preference fails on Debian.  The workaround for this is to assume a value of
		 * "en-us,en" since this is typically the default value when mozilla is used without
		 * a profile.
		 */
		if (rc != XPCOM.NS_OK) {
			prefLocales = "en-us,en" + TOKENIZER_LOCALE;	//$NON-NLS-1$
		} else {
			if (result[0] == 0) error(XPCOM.NS_NOINTERFACE);
			localizedString = new nsIPrefLocalizedString(result[0]);
			result[0] = 0;
			rc = localizedString.ToString(result);
			if (rc != XPCOM.NS_OK) error(rc);
			if (result[0] == 0) error(XPCOM.NS_NOINTERFACE);
			int length = XPCOM.strlen_PRUnichar(result[0]);
			char[] dest = new char[length];
			XPCOM.memmove(dest, result[0], length * 2);
			prefLocales = new String(dest) + TOKENIZER_LOCALE;
		}
		result[0] = 0;

		/*
		 * construct the new locale preference value by prepending the
		 * user's current locale and language to the original value 
		 */
		Locale locale = Locale.getDefault();
		String language = locale.getLanguage();
		String country = locale.getCountry();
		StringBuffer stringBuffer = new StringBuffer (language);
		stringBuffer.append(SEPARATOR_LOCALE);
		stringBuffer.append(country.toLowerCase());
		stringBuffer.append(TOKENIZER_LOCALE);
		stringBuffer.append(language);
		stringBuffer.append(TOKENIZER_LOCALE);
		String newLocales = stringBuffer.toString();
		StringTokenizer tokenzier = new StringTokenizer(prefLocales, TOKENIZER_LOCALE);
		while (tokenzier.hasMoreTokens()) {
			String token = (tokenzier.nextToken() + TOKENIZER_LOCALE).trim();
			/* ensure that duplicate locale values are not added */
			if (newLocales.indexOf(token) == -1) {
				stringBuffer.append(token);
			}
		}
		newLocales = stringBuffer.toString();
		if (!newLocales.equals(prefLocales)) {
			/* write the new locale value */
			newLocales = newLocales.substring(0, newLocales.length() - TOKENIZER_LOCALE.length ()); /* remove trailing tokenizer */
			int length = newLocales.length();
			char[] charBuffer = new char[length + 1];
			newLocales.getChars(0, length, charBuffer, 0);
			if (localizedString == null) {
				byte[] contractID = Converter.wcsToMbcs(null, XPCOM.NS_PREFLOCALIZEDSTRING_CONTRACTID, true);
				rc = componentManager.CreateInstanceByContractID(contractID, 0, nsIPrefLocalizedString.NS_IPREFLOCALIZEDSTRING_IID, result);
				if (rc != XPCOM.NS_OK) error(rc);
				if (result[0] == 0) error(XPCOM.NS_NOINTERFACE);
				localizedString = new nsIPrefLocalizedString(result[0]);
				result[0] = 0;
			}
			localizedString.SetDataWithLength(length, charBuffer);
			rc = prefBranch.SetComplexValue(buffer, nsIPrefLocalizedString.NS_IPREFLOCALIZEDSTRING_IID, localizedString.getAddress());
		}
		if (localizedString != null) {
			localizedString.Release();
			localizedString = null;
		}

		/* get Mozilla's current charset preference value */
		String prefCharset = null;
		buffer = Converter.wcsToMbcs(null, PREFERENCE_CHARSET, true);
		rc = prefBranch.GetComplexValue(buffer, nsIPrefLocalizedString.NS_IPREFLOCALIZEDSTRING_IID, result);
		/* 
		 * Feature of Debian.  For some reason attempting to query for the current charset
		 * preference fails on Debian.  The workaround for this is to assume a value of
		 * "ISO-8859-1" since this is typically the default value when mozilla is used
		 * without a profile.
		 */
		if (rc != XPCOM.NS_OK) {
			prefCharset = "ISO-8859-1";	//$NON_NLS-1$
		} else {
			if (result[0] == 0) error(XPCOM.NS_NOINTERFACE);
			localizedString = new nsIPrefLocalizedString(result[0]);
			result[0] = 0;
			rc = localizedString.ToString(result);
			if (rc != XPCOM.NS_OK) error(rc);
			if (result[0] == 0) error(XPCOM.NS_NOINTERFACE);
			int length = XPCOM.strlen_PRUnichar(result[0]);
			char[] dest = new char[length];
			XPCOM.memmove(dest, result[0], length * 2);
			prefCharset = new String(dest);
		}
		result[0] = 0;

		String newCharset = System.getProperty("file.encoding");	// $NON-NLS-1$
		if (!newCharset.equals(prefCharset)) {
			/* write the new charset value */
			int length = newCharset.length();
			char[] charBuffer = new char[length + 1];
			newCharset.getChars(0, length, charBuffer, 0);
			if (localizedString == null) {
				byte[] contractID = Converter.wcsToMbcs(null, XPCOM.NS_PREFLOCALIZEDSTRING_CONTRACTID, true);
				rc = componentManager.CreateInstanceByContractID(contractID, 0, nsIPrefLocalizedString.NS_IPREFLOCALIZEDSTRING_IID, result);
				if (rc != XPCOM.NS_OK) error(rc);
				if (result[0] == 0) error(XPCOM.NS_NOINTERFACE);
				localizedString = new nsIPrefLocalizedString(result[0]);
				result[0] = 0;
			}
			localizedString.SetDataWithLength(length, charBuffer);
			rc = prefBranch.SetComplexValue(buffer, nsIPrefLocalizedString.NS_IPREFLOCALIZEDSTRING_IID, localizedString.getAddress());
		}
		if (localizedString != null) localizedString.Release();
		prefBranch.Release();

		PromptServiceFactory factory = new PromptServiceFactory();
		factory.AddRef();

		rc = componentManager.QueryInterface(nsIComponentRegistrar.NS_ICOMPONENTREGISTRAR_IID, result);
		if (rc != XPCOM.NS_OK) error(rc);
		if (result[0] == 0) error(XPCOM.NS_NOINTERFACE);
		
		nsIComponentRegistrar componentRegistrar = new nsIComponentRegistrar(result[0]);
		result[0] = 0;
		buffer = XPCOM.NS_PROMPTSERVICE_CONTRACTID.getBytes();
		aContractID = new byte[buffer.length + 1];
		System.arraycopy(buffer, 0, aContractID, 0, buffer.length);
		buffer = "Prompt Service".getBytes(); //$NON-NLS-1$
		byte[] aClassName = new byte[buffer.length + 1];
		System.arraycopy(buffer, 0, aClassName, 0, buffer.length);
		rc = componentRegistrar.RegisterFactory(XPCOM.NS_PROMPTSERVICE_CID, aClassName, aContractID, factory.getAddress());
		if (rc != XPCOM.NS_OK) error(rc);
		factory.Release();
		
		HelperAppLauncherDialogFactory dialogFactory = new HelperAppLauncherDialogFactory();
		dialogFactory.AddRef();
		
		buffer = XPCOM.NS_HELPERAPPLAUNCHERDIALOG_CONTRACTID.getBytes();
		aContractID = new byte[buffer.length + 1];
		System.arraycopy(buffer, 0, aContractID, 0, buffer.length);
		buffer = "Helper App Launcher Dialog".getBytes(); //$NON-NLS-1$
		aClassName = new byte[buffer.length + 1];
		System.arraycopy(buffer, 0, aClassName, 0, buffer.length);
		rc = componentRegistrar.RegisterFactory(XPCOM.NS_HELPERAPPLAUNCHERDIALOG_CID, aClassName, aContractID, dialogFactory.getAddress());
		if (rc != XPCOM.NS_OK) error(rc);
		dialogFactory.Release();
		
		DownloadFactory downloadFactory = new DownloadFactory();
		downloadFactory.AddRef();
		
		buffer = XPCOM.NS_DOWNLOAD_CONTRACTID.getBytes();
		aContractID = new byte[buffer.length + 1];
		System.arraycopy(buffer, 0, aContractID, 0, buffer.length);
		buffer = "Download".getBytes(); //$NON-NLS-1$
		aClassName = new byte[buffer.length + 1];
		System.arraycopy(buffer, 0, aClassName, 0, buffer.length);
		rc = componentRegistrar.RegisterFactory(XPCOM.NS_DOWNLOAD_CID, aClassName, aContractID, downloadFactory.getAddress());
		if (rc != XPCOM.NS_OK) error(rc);
		downloadFactory.Release();
		
		FilePickerFactory pickerFactory = new FilePickerFactory();
		pickerFactory.AddRef();

		buffer = XPCOM.NS_FILEPICKER_CONTRACTID.getBytes();
		aContractID = new byte[buffer.length + 1];
		System.arraycopy(buffer, 0, aContractID, 0, buffer.length);
		buffer = "FilePicker".getBytes(); //$NON-NLS-1$
		aClassName = new byte[buffer.length + 1];
		System.arraycopy(buffer, 0, aClassName, 0, buffer.length);
		rc = componentRegistrar.RegisterFactory(XPCOM.NS_FILEPICKER_CID, aClassName, aContractID, pickerFactory.getAddress());
		if (rc != XPCOM.NS_OK) error(rc);
		pickerFactory.Release();

		componentRegistrar.Release();
		componentManager.Release();
		mozilla = true;

		/*
		* GOOGLE: If we don't force LowLevel into existence during this narrow
		* window of opportunity, registering our "window.external" handler seems
		* to have no effect.
		*/
		LowLevelMoz.init();
	}
	BrowserCount++;
	int rc = XPCOM.NS_GetComponentManager(result);
	if (rc != XPCOM.NS_OK) error(rc);
	if (result[0] == 0) error(XPCOM.NS_NOINTERFACE);
	
	nsIComponentManager componentManager = new nsIComponentManager(result[0]);
	result[0] = 0;
	nsID NS_IWEBBROWSER_CID = new nsID("F1EAC761-87E9-11d3-AF80-00A024FFC08C"); //$NON-NLS-1$
	rc = componentManager.CreateInstance(NS_IWEBBROWSER_CID, 0, nsIWebBrowser.NS_IWEBBROWSER_IID, result);
	if (rc != XPCOM.NS_OK) error(rc);
	if (result[0] == 0) error(XPCOM.NS_NOINTERFACE);	
	componentManager.Release();
	
	webBrowser = new nsIWebBrowser(result[0]); 

	createCOMInterfaces();
	AddRef();

	rc = webBrowser.SetContainerWindow(webBrowserChrome.getAddress());
	if (rc != XPCOM.NS_OK) error(rc);
			
	rc = webBrowser.QueryInterface(nsIBaseWindow.NS_IBASEWINDOW_IID, result);
	if (rc != XPCOM.NS_OK) error(rc);
	if (result[0] == 0) error(XPCOM.NS_ERROR_NO_INTERFACE);
	
	nsIBaseWindow baseWindow = new nsIBaseWindow(result[0]);	
	Rectangle rect = getClientArea();
	if (rect.isEmpty()) {
		rect.width = 1;
		rect.height = 1;
	}

	/*
	* Bug in Mozilla Linux GTK.  Embedding Mozilla into a GtkFixed
	* handle causes problems with some Mozilla plug-ins.  For some
	* reason, the Flash plug-in causes the child of the GtkFixed
	* handle to be resized to 1 when the Flash document is loaded.
	* That could be due to gtk_container_resize_children being called
	* by Mozilla - or one of its plug-ins - on the GtkFixed handle,
	* causing the child of the GtkFixed handle to be resized to 1.
	* The workaround is to embed Mozilla into a GtkHBox handle.
	*/
	embedHandle = OS.gtk_hbox_new (false, 0);
	OS.gtk_container_add (handle, embedHandle);
	OS.gtk_widget_show (embedHandle);
	
	rc = baseWindow.InitWindow(embedHandle, 0, 0, 0, rect.width, rect.height);
	if (rc != XPCOM.NS_OK) error(XPCOM.NS_ERROR_FAILURE);
	rc = baseWindow.Create();
	if (rc != XPCOM.NS_OK) error(XPCOM.NS_ERROR_FAILURE);
	rc = baseWindow.SetVisibility(true);
	if (rc != XPCOM.NS_OK) error(XPCOM.NS_ERROR_FAILURE);
	baseWindow.Release();

	rc = webBrowser.AddWebBrowserListener(weakReference.getAddress(), nsIWebProgressListener.NS_IWEBPROGRESSLISTENER_IID);
	if (rc != XPCOM.NS_OK) error(rc);

	rc = webBrowser.SetParentURIContentListener(uriContentListener.getAddress());
	if (rc != XPCOM.NS_OK) error(rc);

	if (eventCallback == null) {
		eventCallback = new Callback(Browser.class, "eventProc", 3);
		eventProc = eventCallback.getAddress();
		if (eventProc == 0) error(SWT.ERROR_NO_MORE_CALLBACKS);
	}

	/*
	* Feature in Mozilla.  GtkEvents such as key down, key pressed may be consumed
	* by Mozilla and never be received by the parent embedder.  The workaround
	* is to find the top Mozilla gtk widget that receives all the Mozilla GtkEvents,
	* i.e. the first child of the parent embedder. Then hook event callbacks and
	* forward the event to the parent embedder before Mozilla received and consumed
	* them.
	*/
	int /*long*/ list = OS.gtk_container_get_children(embedHandle);
	if (list != 0) {
		mozillaHandle = OS.g_list_data(list);
		OS.g_list_free(list);
		
		if (mozillaHandle != 0) {			
			getDisplay().setData(ADD_WIDGET_KEY, new Object[] {new LONG(mozillaHandle), this});

			/* Note. Callback to get events before Mozilla receives and consumes them. */
			OS.g_signal_connect (mozillaHandle, OS.event, eventProc, 0);
			
			/* 
			* Note.  Callback to get the events not consumed by Mozilla - and to block 
			* them so that they don't get propagated to the parent handle twice.  
			* This hook is set after Mozilla and is therefore called after Mozilla's 
			* handler because GTK dispatches events in their order of registration.
			*/
			OS.g_signal_connect (mozillaHandle, OS.key_press_event, eventProc, STOP_PROPOGATE);
			OS.g_signal_connect (mozillaHandle, OS.key_release_event, eventProc, STOP_PROPOGATE);
		}
	}
	
	Listener listener = new Listener() {
		public void handleEvent(Event event) {
			switch (event.type) {
				case SWT.Dispose: {
					/* make this handler run after other dispose listeners */
					if (ignoreDispose) {
						ignoreDispose = false;
						break;
					}
					ignoreDispose = true;
					notifyListeners (event.type, event);
					event.type = SWT.NONE;
					onDispose(event.display);
					break;
				}
				case SWT.Resize: onResize(); break;
				case SWT.FocusIn: Activate(); break;
				case SWT.Deactivate: {
					Display display = event.display;
					if (Browser.this == display.getFocusControl()) Deactivate();
					break;
				}
				case SWT.Show: {
					/*
					* Feature on GTK Mozilla.  Mozilla does not show up when
					* its container (a GTK fixed handle) is made visible
					* after having been hidden.  The workaround is to reset
					* its size after the container has been made visible. 
					*/
					Display display = event.display;
					display.asyncExec(new Runnable() {
						public void run() {
							if (Browser.this.isDisposed()) return;
							onResize();
						}
					});
					break;
				}
			}
		}
	};	
	int[] folderEvents = new int[]{
		SWT.Dispose,
		SWT.Resize,  
		SWT.FocusIn, 
		SWT.Deactivate,
		SWT.Show
	};
	for (int i = 0; i < folderEvents.length; i++) {
		addListener(folderEvents[i], listener);
	}
}

public static void clearSessions () {
	if (!mozilla) return;
	int /*long*/[] result = new int /*long*/[1];
	int rc = XPCOM.NS_GetServiceManager (result);
	if (rc != XPCOM.NS_OK) error (rc);
	if (result [0] == 0) error (XPCOM.NS_NOINTERFACE);
	nsIServiceManager serviceManager = new nsIServiceManager (result [0]);
	result [0] = 0;
	byte[] buffer = XPCOM.NS_COOKIEMANAGER_CONTRACTID.getBytes ();
	byte[] aContractID = new byte [buffer.length + 1];
	System.arraycopy (buffer, 0, aContractID, 0, buffer.length);
	rc = serviceManager.GetServiceByContractID (aContractID, nsICookieManager.NS_ICOOKIEMANAGER_IID, result);
	if (rc != XPCOM.NS_OK) error (rc);
	if (result [0] == 0) error (XPCOM.NS_NOINTERFACE);
	serviceManager.Release ();

	nsICookieManager manager = new nsICookieManager (result [0]);
	result [0] = 0;
	rc = manager.GetEnumerator (result);
	if (rc != XPCOM.NS_OK) error (rc);
	manager.Release ();

	nsISimpleEnumerator enumerator = new nsISimpleEnumerator (result [0]);
	boolean[] moreElements = new boolean [1];
	rc = enumerator.HasMoreElements (moreElements);
	if (rc != XPCOM.NS_OK) error (rc);
	while (moreElements [0]) {
		result [0] = 0;
		rc = enumerator.GetNext (result);
		if (rc != XPCOM.NS_OK) error (rc);
		nsICookie cookie = new nsICookie (result [0]);
		long[] expires = new long [1];
		rc = cookie.GetExpires (expires);
		if (expires [0] == 0) {
			/* indicates a session cookie */
			int /*long*/ domain = XPCOM.nsEmbedCString_new ();
			int /*long*/ name = XPCOM.nsEmbedCString_new ();
			int /*long*/ path = XPCOM.nsEmbedCString_new ();
			cookie.GetHost (domain);
			cookie.GetName (name);
			cookie.GetPath (path);
			rc = manager.Remove (domain, name, path, false);
			XPCOM.nsEmbedCString_delete (domain);
			XPCOM.nsEmbedCString_delete (name);
			XPCOM.nsEmbedCString_delete (path);
			if (rc != XPCOM.NS_OK) error (rc);
		}
		cookie.Release ();
		rc = enumerator.HasMoreElements (moreElements);
		if (rc != XPCOM.NS_OK) error (rc);
	}
	enumerator.Release ();
}

static int /*long*/ eventProc (int /*long*/ handle, int /*long*/ gdkEvent, int /*long*/ pointer) {
	Widget widget = Display.getCurrent().findWidget(handle);
	if (widget != null && widget instanceof Browser) {
		return ((Browser)widget).gtk_event(handle, gdkEvent, pointer);
	}
	return 0;
}

static Composite fixIM(Composite parent) {
	/*
	* Note.  Mozilla provides all IM suport needed for text input in webpages.
	* If SWTcreates another input method context for the widget it will cause
	* undetermine results to happen (hungs and crashes). The fix is to prevent 
	* SWT from creating an input method context for the  Browser widget.
	*/
	if (parent != null && !parent.isDisposed()) {
		Display display = parent.getDisplay();
		if (display != null) {
			if (display.getThread() == Thread.currentThread ()) {
				display.setData (NO_INPUT_METHOD, "true");
			}
		}
	}
	return parent;
}

int /*long*/ gtk_event (int /*long*/ handle, int /*long*/ gdkEvent, int /*long*/ pointer) {
	/* 
	* Stop the propagation of events that are not consumed by Mozilla, before
	* they reach the parent embedder.  These event have already been received.
	*/
	if (pointer == STOP_PROPOGATE) return 1;

	GdkEvent event = new GdkEvent ();
	OS.memmove (event, gdkEvent, GdkEvent.sizeof);
	switch (event.type) {
		case OS.GDK_KEY_PRESS:
		case OS.GDK_KEY_RELEASE:
		case OS.GDK_BUTTON_PRESS:
		case OS.GDK_BUTTON_RELEASE: {
			/* 
			* Forward the event to the parent embedder before Mozilla receives it, 
			* as Mozilla may or may not consume it.
			*/
			OS.gtk_widget_event (this.handle, gdkEvent);
			break;
		}
	}
	return 0;
}

/**	 
 * Adds the listener to receive events.
 * <p>
 *
 * @param listener the listener
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS when called from the wrong thread</li>
 *    <li>ERROR_WIDGET_DISPOSED when the widget has been disposed</li>
 * </ul>
 *
 * @since 3.0
 */
public void addCloseWindowListener(CloseWindowListener listener) {
	checkWidget();
	if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);	
	CloseWindowListener[] newCloseWindowListeners = new CloseWindowListener[closeWindowListeners.length + 1];
	System.arraycopy(closeWindowListeners, 0, newCloseWindowListeners, 0, closeWindowListeners.length);
	closeWindowListeners = newCloseWindowListeners;
	closeWindowListeners[closeWindowListeners.length - 1] = listener;
}

/**	 
 * Adds the listener to receive events.
 * <p>
 *
 * @param listener the listener
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS when called from the wrong thread</li>
 *    <li>ERROR_WIDGET_DISPOSED when the widget has been disposed</li>
 * </ul>
 *
 * @since 3.0
 */
public void addLocationListener(LocationListener listener) {
	checkWidget();
	if (listener == null) SWT.error (SWT.ERROR_NULL_ARGUMENT);
	LocationListener[] newLocationListeners = new LocationListener[locationListeners.length + 1];
	System.arraycopy(locationListeners, 0, newLocationListeners, 0, locationListeners.length);
	locationListeners = newLocationListeners;
	locationListeners[locationListeners.length - 1] = listener;
}

/**	 
 * Adds the listener to receive events.
 * <p>
 *
 * @param listener the listener
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS when called from the wrong thread</li>
 *    <li>ERROR_WIDGET_DISPOSED when the widget has been disposed</li>
 * </ul>
 *
 * @since 3.0
 */
public void addOpenWindowListener(OpenWindowListener listener) {
	checkWidget();
	if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	OpenWindowListener[] newOpenWindowListeners = new OpenWindowListener[openWindowListeners.length + 1];
	System.arraycopy(openWindowListeners, 0, newOpenWindowListeners, 0, openWindowListeners.length);
	openWindowListeners = newOpenWindowListeners;
	openWindowListeners[openWindowListeners.length - 1] = listener;
}

/**	 
 * Adds the listener to receive events.
 * <p>
 *
 * @param listener the listener
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS when called from the wrong thread</li>
 *    <li>ERROR_WIDGET_DISPOSED when the widget has been disposed</li>
 * </ul>
 *
 * @since 3.0
 */
public void addProgressListener(ProgressListener listener) {
	checkWidget();
	if (listener == null) SWT.error (SWT.ERROR_NULL_ARGUMENT);
	ProgressListener[] newProgressListeners = new ProgressListener[progressListeners.length + 1];
	System.arraycopy(progressListeners, 0, newProgressListeners, 0, progressListeners.length);
	progressListeners = newProgressListeners;
	progressListeners[progressListeners.length - 1] = listener;
}

/**	 
 * Adds the listener to receive events.
 * <p>
 *
 * @param listener the listener
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS when called from the wrong thread</li>
 *    <li>ERROR_WIDGET_DISPOSED when the widget has been disposed</li>
 * </ul>
 *
 * @since 3.0
 */
public void addStatusTextListener(StatusTextListener listener) {
	checkWidget();
	if (listener == null) SWT.error (SWT.ERROR_NULL_ARGUMENT);
	StatusTextListener[] newStatusTextListeners = new StatusTextListener[statusTextListeners.length + 1];
	System.arraycopy(statusTextListeners, 0, newStatusTextListeners, 0, statusTextListeners.length);
	statusTextListeners = newStatusTextListeners;
	statusTextListeners[statusTextListeners.length - 1] = listener;
}

/**	 
 * Adds the listener to receive events.
 * <p>
 *
 * @param listener the listener
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS when called from the wrong thread</li>
 *    <li>ERROR_WIDGET_DISPOSED when the widget has been disposed</li>
 * </ul>
 *
 * @since 3.0
 */
public void addTitleListener(TitleListener listener) {
	checkWidget();
	if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	TitleListener[] newTitleListeners = new TitleListener[titleListeners.length + 1];
	System.arraycopy(titleListeners, 0, newTitleListeners, 0, titleListeners.length);
	titleListeners = newTitleListeners;
	titleListeners[titleListeners.length - 1] = listener;
}

/**	 
 * Adds the listener to receive events.
 * <p>
 *
 * @param listener the listener
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS when called from the wrong thread</li>
 *    <li>ERROR_WIDGET_DISPOSED when the widget has been disposed</li>
 * </ul>
 *
 * @since 3.0
 */
public void addVisibilityWindowListener(VisibilityWindowListener listener) {
	checkWidget();
	if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	VisibilityWindowListener[] newVisibilityWindowListeners = new VisibilityWindowListener[visibilityWindowListeners.length + 1];
	System.arraycopy(visibilityWindowListeners, 0, newVisibilityWindowListeners, 0, visibilityWindowListeners.length);
	visibilityWindowListeners = newVisibilityWindowListeners;
	visibilityWindowListeners[visibilityWindowListeners.length - 1] = listener;
}

/**
 * Navigate to the previous session history item.
 *
 * @return <code>true</code> if the operation was successful and <code>false</code> otherwise
 *
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS when called from the wrong thread</li>
 *    <li>ERROR_WIDGET_DISPOSED when the widget has been disposed</li>
 * </ul>
 *
 * @see #forward
 * 
 * @since 3.0
 */
public boolean back() {
	checkWidget();
	int /*long*/[] result = new int /*long*/[1];
	int rc = webBrowser.QueryInterface(nsIWebNavigation.NS_IWEBNAVIGATION_IID, result);
	if (rc != XPCOM.NS_OK) error(rc);
	if (result[0] == 0) error(XPCOM.NS_ERROR_NO_INTERFACE);
	
	nsIWebNavigation webNavigation = new nsIWebNavigation(result[0]);		 	
	rc = webNavigation.GoBack();	
	webNavigation.Release();
	
	return rc == XPCOM.NS_OK;
}

protected void checkSubclass() {
	String name = getClass().getName();
	int index = name.lastIndexOf('.');
	if (!name.substring(0, index + 1).equals(PACKAGE_PREFIX)) {
		SWT.error(SWT.ERROR_INVALID_SUBCLASS);
	}
}

void createCOMInterfaces() {
	// Create each of the interfaces that this object implements
	supports = new XPCOMObject(new int[]{2, 0, 0}){
		public int /*long*/ method0(int /*long*/[] args) {return QueryInterface(args[0], args[1]);}
		public int /*long*/ method1(int /*long*/[] args) {return AddRef();}
		public int /*long*/ method2(int /*long*/[] args) {return Release();}
	};
	
	weakReference = new XPCOMObject(new int[]{2, 0, 0, 2}){
		public int /*long*/ method0(int /*long*/[] args) {return QueryInterface(args[0], args[1]);}
		public int /*long*/ method1(int /*long*/[] args) {return AddRef();}
		public int /*long*/ method2(int /*long*/[] args) {return Release();}
		public int /*long*/ method3(int /*long*/[] args) {return QueryReferent(args[0], args[1]);}
	};

	webProgressListener = new XPCOMObject(new int[]{2, 0, 0, 4, 6, 3, 4, 3}){
		public int /*long*/ method0(int /*long*/[] args) {return QueryInterface(args[0], args[1]);}
		public int /*long*/ method1(int /*long*/[] args) {return AddRef();}
		public int /*long*/ method2(int /*long*/[] args) {return Release();}
		public int /*long*/ method3(int /*long*/[] args) {return OnStateChange(args[0], args[1], args[2],args[3]);}
		public int /*long*/ method4(int /*long*/[] args) {return OnProgressChange(args[0], args[1], args[2],args[3],args[4],args[5]);}
		public int /*long*/ method5(int /*long*/[] args) {return OnLocationChange(args[0], args[1], args[2]);}
		public int /*long*/ method6(int /*long*/[] args) {return OnStatusChange(args[0], args[1], args[2],args[3]);}
		public int /*long*/ method7(int /*long*/[] args) {return OnSecurityChange(args[0], args[1], args[2]);}
	};
	
	webBrowserChrome = new XPCOMObject(new int[]{2, 0, 0, 2, 1, 1, 1, 1, 0, 2, 0, 1, 1}){
		public int /*long*/ method0(int /*long*/[] args) {return QueryInterface(args[0], args[1]);}
		public int /*long*/ method1(int /*long*/[] args) {return AddRef();}
		public int /*long*/ method2(int /*long*/[] args) {return Release();}
		public int /*long*/ method3(int /*long*/[] args) {return SetStatus(args[0], args[1]);}
		public int /*long*/ method4(int /*long*/[] args) {return GetWebBrowser(args[0]);}
		public int /*long*/ method5(int /*long*/[] args) {return SetWebBrowser(args[0]);}
		public int /*long*/ method6(int /*long*/[] args) {return GetChromeFlags(args[0]);}
		public int /*long*/ method7(int /*long*/[] args) {return SetChromeFlags(args[0]);}
		public int /*long*/ method8(int /*long*/[] args) {return DestroyBrowserWindow();}
		public int /*long*/ method9(int /*long*/[] args) {return SizeBrowserTo(args[0], args[1]);}
		public int /*long*/ method10(int /*long*/[] args) {return ShowAsModal();}
		public int /*long*/ method11(int /*long*/[] args) {return IsWindowModal(args[0]);}
		public int /*long*/ method12(int /*long*/[] args) {return ExitModalEventLoop(args[0]);}
	};
	
	webBrowserChromeFocus = new XPCOMObject(new int[]{2, 0, 0, 0, 0}){
		public int /*long*/ method0(int /*long*/[] args) {return QueryInterface(args[0], args[1]);}
		public int /*long*/ method1(int /*long*/[] args) {return AddRef();}
		public int /*long*/ method2(int /*long*/[] args) {return Release();}
		public int /*long*/ method3(int /*long*/[] args) {return FocusNextElement();}
		public int /*long*/ method4(int /*long*/[] args) {return FocusPrevElement();}
	};
		
	embeddingSiteWindow = new XPCOMObject(new int[]{2, 0, 0, 5, 5, 0, 1, 1, 1, 1, 1}){
		public int /*long*/ method0(int /*long*/[] args) {return QueryInterface(args[0], args[1]);}
		public int /*long*/ method1(int /*long*/[] args) {return AddRef();}
		public int /*long*/ method2(int /*long*/[] args) {return Release();}
		public int /*long*/ method3(int /*long*/[] args) {return SetDimensions(args[0], args[1], args[2], args[3], args[4]);}
		public int /*long*/ method4(int /*long*/[] args) {return GetDimensions(args[0], args[1], args[2], args[3], args[4]);}
		public int /*long*/ method5(int /*long*/[] args) {return SetFocus();}
		public int /*long*/ method6(int /*long*/[] args) {return GetVisibility(args[0]);}
		public int /*long*/ method7(int /*long*/[] args) {return SetVisibility(args[0]);}
		public int /*long*/ method8(int /*long*/[] args) {return GetTitle(args[0]);}
		public int /*long*/ method9(int /*long*/[] args) {return SetTitle(args[0]);}
		public int /*long*/ method10(int /*long*/[] args) {return GetSiteWindow(args[0]);}
	};
	
	interfaceRequestor = new XPCOMObject(new int[]{2, 0, 0, 2}){
		public int /*long*/ method0(int /*long*/[] args) {return QueryInterface(args[0], args[1]);}
		public int /*long*/ method1(int /*long*/[] args) {return AddRef();}
		public int /*long*/ method2(int /*long*/[] args) {return Release();}
		public int /*long*/ method3(int /*long*/[] args) {return GetInterface(args[0], args[1]);}
	};
		
	supportsWeakReference = new XPCOMObject(new int[]{2, 0, 0, 1}){
		public int /*long*/ method0(int /*long*/[] args) {return QueryInterface(args[0], args[1]);}
		public int /*long*/ method1(int /*long*/[] args) {return AddRef();}
		public int /*long*/ method2(int /*long*/[] args) {return Release();}
		public int /*long*/ method3(int /*long*/[] args) {return GetWeakReference(args[0]);}
	};
	
	contextMenuListener = new XPCOMObject(new int[]{2, 0, 0, 3}){
		public int /*long*/ method0(int /*long*/[] args) {return QueryInterface(args[0], args[1]);}
		public int /*long*/ method1(int /*long*/[] args) {return AddRef();}
		public int /*long*/ method2(int /*long*/[] args) {return Release();}
		public int /*long*/ method3(int /*long*/[] args) {return OnShowContextMenu(args[0],args[1],args[2]);}
	};
	
	uriContentListener = new XPCOMObject(new int[]{2, 0, 0, 2, 5, 3, 4, 1, 1, 1, 1}) {
		public int /*long*/ method0(int /*long*/[] args) {return QueryInterface(args[0], args[1]);}
		public int /*long*/ method1(int /*long*/[] args) {return AddRef();}
		public int /*long*/ method2(int /*long*/[] args) {return Release();}
		public int /*long*/ method3(int /*long*/[] args) {return OnStartURIOpen(args[0], args[1]);}
		public int /*long*/ method4(int /*long*/[] args) {return DoContent(args[0], args[1], args[2], args[3], args[4]);}
		public int /*long*/ method5(int /*long*/[] args) {return IsPreferred(args[0], args[1], args[2]);}
		public int /*long*/ method6(int /*long*/[] args) {return CanHandleContent(args[0], args[1], args[2], args[3]);}
		public int /*long*/ method7(int /*long*/[] args) {return GetLoadCookie(args[0]);}
		public int /*long*/ method8(int /*long*/[] args) {return SetLoadCookie(args[0]);}
		public int /*long*/ method9(int /*long*/[] args) {return GetParentContentListener(args[0]);}
		public int /*long*/ method10(int /*long*/[] args) {return SetParentContentListener(args[0]);}		
	};
	
	tooltipListener = new XPCOMObject(new int[]{2, 0, 0, 3, 0}) {
		public int /*long*/ method0(int /*long*/[] args) {return QueryInterface(args[0], args[1]);}
		public int /*long*/ method1(int /*long*/[] args) {return AddRef();}
		public int /*long*/ method2(int /*long*/[] args) {return Release();}
		public int /*long*/ method3(int /*long*/[] args) {return OnShowTooltip(args[0], args[1], args[2]);}
		public int /*long*/ method4(int /*long*/[] args) {return OnHideTooltip();}		
	};
}

void disposeCOMInterfaces() {
	if (supports != null) {
		supports.dispose();
		supports = null;
	}	
	if (weakReference != null) {
		weakReference.dispose();
		weakReference = null;	
	}
	if (webProgressListener != null) {
		webProgressListener.dispose();
		webProgressListener = null;
	}
	if (webBrowserChrome != null) {
		webBrowserChrome.dispose();
		webBrowserChrome = null;
	}
	if (webBrowserChromeFocus != null) {
		webBrowserChromeFocus.dispose();
		webBrowserChromeFocus = null;
	}
	if (embeddingSiteWindow != null) {
		embeddingSiteWindow.dispose();
		embeddingSiteWindow = null;
	}
	if (interfaceRequestor != null) {
		interfaceRequestor.dispose();
		interfaceRequestor = null;
	}		
	if (supportsWeakReference != null) {
		supportsWeakReference.dispose();
		supportsWeakReference = null;
	}	
	if (contextMenuListener != null) {
		contextMenuListener.dispose();
		contextMenuListener = null;
	}
	if (uriContentListener != null) {
		uriContentListener.dispose();
		uriContentListener = null;
	}
	if (tooltipListener != null) {
		tooltipListener.dispose();
		tooltipListener = null;
	}
}

public boolean execute(String script) {
	checkWidget();
	if (script == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	String url = "javascript:"+script+";void(0);";//$NON-NLS-1$ //$NON-NLS-2$
	int /*long*/[] result = new int /*long*/[1];
	int rc = webBrowser.QueryInterface(nsIWebNavigation.NS_IWEBNAVIGATION_IID, result);
	if (rc != XPCOM.NS_OK) error(rc);
	if (result[0] == 0) error(XPCOM.NS_ERROR_NO_INTERFACE);

	nsIWebNavigation webNavigation = new nsIWebNavigation(result[0]);
    char[] arg = url.toCharArray(); 
    char[] c = new char[arg.length+1];
    System.arraycopy(arg,0,c,0,arg.length);
	rc = webNavigation.LoadURI(c, nsIWebNavigation.LOAD_FLAGS_NONE, 0, 0, 0);
	webNavigation.Release();
	return rc == XPCOM.NS_OK;
}

static Browser findBrowser(int /*long*/ handle) {
	/*
	* Note.  On GTK, Mozilla is embedded into a GtkHBox handle
	* and not directly into the parent Composite handle.
	*/
	int /*long*/ parent = OS.gtk_widget_get_parent(handle);
	Display display = Display.getCurrent();
	return (Browser)display.findWidget(parent); 
}

/**
 * Navigate to the next session history item.
 *
 * @return <code>true</code> if the operation was successful and <code>false</code> otherwise
 *
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS when called from the wrong thread</li>
 *    <li>ERROR_WIDGET_DISPOSED when the widget has been disposed</li>
 * </ul>
 * 
 * @see #back
 * 
 * @since 3.0
 */
public boolean forward() {
	checkWidget();
	int /*long*/[] result = new int /*long*/[1];
	int rc = webBrowser.QueryInterface(nsIWebNavigation.NS_IWEBNAVIGATION_IID, result);
	if (rc != XPCOM.NS_OK) error(rc);
	if (result[0] == 0) error(XPCOM.NS_ERROR_NO_INTERFACE);
	
	nsIWebNavigation webNavigation = new nsIWebNavigation(result[0]);
	rc = webNavigation.GoForward();
	webNavigation.Release();

	return rc == XPCOM.NS_OK;
}

/**
 * Returns the current URL.
 *
 * @return the current URL or an empty <code>String</code> if there is no current URL
 *
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS when called from the wrong thread</li>
 *    <li>ERROR_WIDGET_DISPOSED when the widget has been disposed</li>
 * </ul>
 *
 * @see #setUrl
 * 
 * @since 3.0
 */
public String getUrl() {
	checkWidget();
	int /*long*/[] result = new int /*long*/[1];
	int rc = webBrowser.QueryInterface(nsIWebNavigation.NS_IWEBNAVIGATION_IID, result);
	if (rc != XPCOM.NS_OK) error(rc);
	if (result[0] == 0) error(XPCOM.NS_ERROR_NO_INTERFACE);
	
	nsIWebNavigation webNavigation = new nsIWebNavigation(result[0]);
	int /*long*/[] aCurrentURI = new int /*long*/[1];
	rc = webNavigation.GetCurrentURI(aCurrentURI);
	if (rc != XPCOM.NS_OK) error(rc);
	/*
	 * This code is intentionally commented.  aCurrentURI is 0
	 * when no location has previously been set.
	 */
	//if (aCurrentURI[0] == 0) error(XPCOM.NS_ERROR_NO_INTERFACE);
	webNavigation.Release();
	
	byte[] dest = null;
	if (aCurrentURI[0] != 0) {
		nsIURI uri = new nsIURI(aCurrentURI[0]);
		int /*long*/ aSpec = XPCOM.nsEmbedCString_new();
		rc = uri.GetSpec(aSpec);
		if (rc != XPCOM.NS_OK) error(rc);
		int length = XPCOM.nsEmbedCString_Length(aSpec);
		int /*long*/ buffer = XPCOM.nsEmbedCString_get(aSpec);
		dest = new byte[length];
		XPCOM.memmove(dest, buffer, length);
		XPCOM.nsEmbedCString_delete(aSpec);
		uri.Release();
	}
	if (dest == null) return ""; //$NON-NLS-1$
	/*
	 * If the URI indicates that the current page is being rendered from
	 * memory (ie.- via setText()) then answer about:blank as the URL
	 * to be consistent with win32.
	 */
	String location = new String (dest);
	if (location.equals (URI_FROMMEMORY)) location = ABOUT_BLANK;
	return location;
}

/**
 * Returns <code>true</code> if the receiver can navigate to the 
 * previous session history item, and <code>false</code> otherwise.
 *
 * @return the receiver's back command enabled state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * 
 * @see #back
 */
public boolean isBackEnabled() {
	checkWidget();
	int /*long*/[] result = new int /*long*/[1];
	int rc = webBrowser.QueryInterface(nsIWebNavigation.NS_IWEBNAVIGATION_IID, result);
	if (rc != XPCOM.NS_OK) error(rc);
	if (result[0] == 0) error(XPCOM.NS_ERROR_NO_INTERFACE);
	
	nsIWebNavigation webNavigation = new nsIWebNavigation(result[0]);
	boolean[] aCanGoBack = new boolean[1];
	rc = webNavigation.GetCanGoBack(aCanGoBack);	
	webNavigation.Release();
	
	return aCanGoBack[0];
}

/**
 * Returns <code>true</code> if the receiver can navigate to the 
 * next session history item, and <code>false</code> otherwise.
 *
 * @return the receiver's forward command enabled state
 *
 * @exception SWTException <ul>
 *    <li>ERROR_WIDGET_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 * 
 * @see #forward
 */
public boolean isForwardEnabled() {
	checkWidget();
	int /*long*/[] result = new int /*long*/[1];
	int rc = webBrowser.QueryInterface(nsIWebNavigation.NS_IWEBNAVIGATION_IID, result);
	if (rc != XPCOM.NS_OK) error(rc);
	if (result[0] == 0) error(XPCOM.NS_ERROR_NO_INTERFACE);
	
	nsIWebNavigation webNavigation = new nsIWebNavigation(result[0]);
	boolean[] aCanGoForward = new boolean[1];
	rc = webNavigation.GetCanGoForward(aCanGoForward);
	webNavigation.Release();

	return aCanGoForward[0];
}

static String error(int code) {
	throw new SWTError("XPCOM error "+code); //$NON-NLS-1$
}

void onDispose(Display display) {
	display.setData(ADD_WIDGET_KEY, new Object[] {new LONG(mozillaHandle), null});

	int rc = webBrowser.RemoveWebBrowserListener(weakReference.getAddress(), nsIWebProgressListener.NS_IWEBPROGRESSLISTENER_IID);
	if (rc != XPCOM.NS_OK) error(rc);

	rc = webBrowser.SetParentURIContentListener(0);
	if (rc != XPCOM.NS_OK) error(rc);
	
	int /*long*/[] result = new int /*long*/[1];
	rc = webBrowser.QueryInterface(nsIBaseWindow.NS_IBASEWINDOW_IID, result);
	if (rc != XPCOM.NS_OK) error(rc);
	if (result[0] == 0) error(XPCOM.NS_ERROR_NO_INTERFACE);
	
	nsIBaseWindow baseWindow = new nsIBaseWindow(result[0]);
	rc = baseWindow.Destroy();
	if (rc != XPCOM.NS_OK) error(rc);
	baseWindow.Release();
	
	Release();
	webBrowser.Release();
	
	if (tip != null && !tip.isDisposed()) tip.dispose();
	tip = null;

	BrowserCount--;
	/*
	* This code is intentionally commented.  It is not possible to reinitialize
	* Mozilla once it has been terminated.  NS_InitEmbedding always fails after
	* NS_TermEmbedding has been called.  The workaround is to call NS_InitEmbedding
	* once and never call NS_TermEmbedding.
	*/
//	if (BrowserCount == 0) {
//		if (AppShell != null) {
//			// Shutdown the appshell service.
//			rc = AppShell.Spindown();
//			if (rc != XPCOM.NS_OK) error(rc);
//			AppShell.Release();
//			AppShell = null;
//		}
//		WindowCreator.Release();
//		WindowCreator = null;
//		PromptService.Release();
//		PromptService = null;
//		XPCOM.NS_TermEmbedding();
//		mozilla = false;
//	}
}

void Activate() {
	int /*long*/[] result = new int /*long*/[1];
	int rc = webBrowser.QueryInterface(nsIWebBrowserFocus.NS_IWEBBROWSERFOCUS_IID, result);
	if (rc != XPCOM.NS_OK) error(rc);
	if (result[0] == 0) error(XPCOM.NS_ERROR_NO_INTERFACE);
	
	nsIWebBrowserFocus webBrowserFocus = new nsIWebBrowserFocus(result[0]);
	rc = webBrowserFocus.Activate();
	if (rc != XPCOM.NS_OK) error(rc);
	webBrowserFocus.Release();
}
	
void Deactivate() {
	int /*long*/[] result = new int /*long*/[1];
	int rc = webBrowser.QueryInterface(nsIWebBrowserFocus.NS_IWEBBROWSERFOCUS_IID, result);
	if (rc != XPCOM.NS_OK) error(rc);
	if (result[0] == 0) error(XPCOM.NS_ERROR_NO_INTERFACE);
	
	nsIWebBrowserFocus webBrowserFocus = new nsIWebBrowserFocus(result[0]);
	rc = webBrowserFocus.Deactivate();
	if (rc != XPCOM.NS_OK) error(rc);
	webBrowserFocus.Release();
}

void SetFocusAtFirstElement() {
	int /*long*/[] result = new int /*long*/[1];
	int rc = webBrowser.QueryInterface(nsIWebBrowserFocus.NS_IWEBBROWSERFOCUS_IID, result);
	if (rc != XPCOM.NS_OK) error(rc);
	if (result[0] == 0) error(XPCOM.NS_ERROR_NO_INTERFACE);
	
	nsIWebBrowserFocus webBrowserFocus = new nsIWebBrowserFocus(result[0]);
	rc = webBrowserFocus.SetFocusAtFirstElement();
	if (rc != XPCOM.NS_OK) error(rc);
	webBrowserFocus.Release();
}

void onResize() {
	Rectangle rect = getClientArea();
	int /*long*/[] result = new int /*long*/[1];
	int rc = webBrowser.QueryInterface(nsIBaseWindow.NS_IBASEWINDOW_IID, result);
	if (rc != XPCOM.NS_OK) error(rc);
	if (result[0] == 0) error(XPCOM.NS_ERROR_NO_INTERFACE);

	OS.gtk_widget_set_size_request(embedHandle, rect.width, rect.height);
	nsIBaseWindow baseWindow = new nsIBaseWindow(result[0]);
	rc = baseWindow.SetPositionAndSize(rect.x, rect.y, rect.width, rect.height, true);
	if (rc != XPCOM.NS_OK) error(rc);
	baseWindow.Release();
}

/**
 * Refresh the current page.
 *
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS when called from the wrong thread</li>
 *    <li>ERROR_WIDGET_DISPOSED when the widget has been disposed</li>
 * </ul>
 *
 * @since 3.0
 */
public void refresh() {
	checkWidget();
	int /*long*/[] result = new int /*long*/[1];
	int rc = webBrowser.QueryInterface(nsIWebNavigation.NS_IWEBNAVIGATION_IID, result);
	if (rc != XPCOM.NS_OK) error(rc);
	if (result[0] == 0) error(XPCOM.NS_ERROR_NO_INTERFACE);
	
	nsIWebNavigation webNavigation = new nsIWebNavigation(result[0]);		 	
	rc = webNavigation.Reload(nsIWebNavigation.LOAD_FLAGS_NONE);
	webNavigation.Release();
	if (rc == XPCOM.NS_OK) return;
	/*
	* Feature in Mozilla.  Reload returns an error code NS_ERROR_INVALID_POINTER
	* when it is called immediately after a request to load a new document using
	* LoadURI.  The workaround is to ignore this error code.
	* 
	* Feature in Mozilla.  Attempting to reload a file that no longer exists
	* returns an error code of NS_ERROR_FILE_NOT_FOUND.  This is equivalent to
	* attempting to load a non-existent local url, which is not a Browser error,
	* so this error code should be ignored. 
	*/
	if (rc != XPCOM.NS_ERROR_INVALID_POINTER && rc != XPCOM.NS_ERROR_FILE_NOT_FOUND) error(rc);
}

/**	 
 * Removes the listener.
 *
 * @param listener the listener
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS when called from the wrong thread</li>
 *    <li>ERROR_WIDGET_DISPOSED when the widget has been disposed</li>
 * </ul>
 * 
 * @since 3.0
 */
public void removeCloseWindowListener(CloseWindowListener listener) {
	checkWidget();
	if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	if (closeWindowListeners.length == 0) return;
	int index = -1;
	for (int i = 0; i < closeWindowListeners.length; i++) {
		if (listener == closeWindowListeners[i]){
			index = i;
			break;
		}
	}
	if (index == -1) return;
	if (closeWindowListeners.length == 1) {
		closeWindowListeners = new CloseWindowListener[0];
		return;
	}
	CloseWindowListener[] newCloseWindowListeners = new CloseWindowListener[closeWindowListeners.length - 1];
	System.arraycopy(closeWindowListeners, 0, newCloseWindowListeners, 0, index);
	System.arraycopy(closeWindowListeners, index + 1, newCloseWindowListeners, index, closeWindowListeners.length - index - 1);
	closeWindowListeners = newCloseWindowListeners;
}

/**	 
 * Removes the listener.
 *
 * @param listener the listener
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS when called from the wrong thread</li>
 *    <li>ERROR_WIDGET_DISPOSED when the widget has been disposed</li>
 * </ul>
 * 
 * @since 3.0
 */
public void removeLocationListener(LocationListener listener) {
	checkWidget();
	if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	if (locationListeners.length == 0) return;
	int index = -1;
	for (int i = 0; i < locationListeners.length; i++) {
		if (listener == locationListeners[i]){
			index = i;
			break;
		}
	}
	if (index == -1) return;
	if (locationListeners.length == 1) {
		locationListeners = new LocationListener[0];
		return;
	}
	LocationListener[] newLocationListeners = new LocationListener[locationListeners.length - 1];
	System.arraycopy(locationListeners, 0, newLocationListeners, 0, index);
	System.arraycopy(locationListeners, index + 1, newLocationListeners, index, locationListeners.length - index - 1);
	locationListeners = newLocationListeners;
}

/**	 
 * Removes the listener.
 *
 * @param listener the listener
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS when called from the wrong thread</li>
 *    <li>ERROR_WIDGET_DISPOSED when the widget has been disposed</li>
 * </ul>
 * 
 * @since 3.0
 */
public void removeOpenWindowListener(OpenWindowListener listener) {
	checkWidget();
	if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	if (openWindowListeners.length == 0) return;
	int index = -1;
	for (int i = 0; i < openWindowListeners.length; i++) {
		if (listener == openWindowListeners[i]){
			index = i;
			break;
		}
	}
	if (index == -1) return;
	if (openWindowListeners.length == 1) {
		openWindowListeners = new OpenWindowListener[0];
		return;
	}
	OpenWindowListener[] newOpenWindowListeners = new OpenWindowListener[openWindowListeners.length - 1];
	System.arraycopy(openWindowListeners, 0, newOpenWindowListeners, 0, index);
	System.arraycopy(openWindowListeners, index + 1, newOpenWindowListeners, index, openWindowListeners.length - index - 1);
	openWindowListeners = newOpenWindowListeners;
}

/**	 
 * Removes the listener.
 *
 * @param listener the listener
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS when called from the wrong thread</li>
 *    <li>ERROR_WIDGET_DISPOSED when the widget has been disposed</li>
 * </ul>
 * 
 * @since 3.0
 */
public void removeProgressListener(ProgressListener listener) {
	checkWidget();
	if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	if (progressListeners.length == 0) return;
	int index = -1;
	for (int i = 0; i < progressListeners.length; i++) {
		if (listener == progressListeners[i]){
			index = i;
			break;
		}
	}
	if (index == -1) return;
	if (progressListeners.length == 1) {
		progressListeners = new ProgressListener[0];
		return;
	}
	ProgressListener[] newProgressListeners = new ProgressListener[progressListeners.length - 1];
	System.arraycopy(progressListeners, 0, newProgressListeners, 0, index);
	System.arraycopy(progressListeners, index + 1, newProgressListeners, index, progressListeners.length - index - 1);
	progressListeners = newProgressListeners;
}

/**	 
 * Removes the listener.
 *
 * @param listener the listener
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS when called from the wrong thread</li>
 *    <li>ERROR_WIDGET_DISPOSED when the widget has been disposed</li>
 * </ul>
 * 
 * @since 3.0
 */
public void removeStatusTextListener(StatusTextListener listener) {
	checkWidget();
	if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	if (statusTextListeners.length == 0) return;
	int index = -1;
	for (int i = 0; i < statusTextListeners.length; i++) {
		if (listener == statusTextListeners[i]){
			index = i;
			break;
		}
	}
	if (index == -1) return;
	if (statusTextListeners.length == 1) {
		statusTextListeners = new StatusTextListener[0];
		return;
	}
	StatusTextListener[] newStatusTextListeners = new StatusTextListener[statusTextListeners.length - 1];
	System.arraycopy(statusTextListeners, 0, newStatusTextListeners, 0, index);
	System.arraycopy(statusTextListeners, index + 1, newStatusTextListeners, index, statusTextListeners.length - index - 1);
	statusTextListeners = newStatusTextListeners;
}

/**	 
 * Removes the listener.
 *
 * @param listener the listener
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS when called from the wrong thread</li>
 *    <li>ERROR_WIDGET_DISPOSED when the widget has been disposed</li>
 * </ul>
 * 
 * @since 3.0
 */
public void removeTitleListener(TitleListener listener) {
	checkWidget();
	if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	if (titleListeners.length == 0) return;
	int index = -1;
	for (int i = 0; i < titleListeners.length; i++) {
		if (listener == titleListeners[i]){
			index = i;
			break;
		}
	}
	if (index == -1) return;
	if (titleListeners.length == 1) {
		titleListeners = new TitleListener[0];
		return;
	}
	TitleListener[] newTitleListeners = new TitleListener[titleListeners.length - 1];
	System.arraycopy(titleListeners, 0, newTitleListeners, 0, index);
	System.arraycopy(titleListeners, index + 1, newTitleListeners, index, titleListeners.length - index - 1);
	titleListeners = newTitleListeners;
}

/**	 
 * Removes the listener.
 *
 * @param listener the listener
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS when called from the wrong thread</li>
 *    <li>ERROR_WIDGET_DISPOSED when the widget has been disposed</li>
 * </ul>
 * 
 * @since 3.0
 */
public void removeVisibilityWindowListener(VisibilityWindowListener listener) {
	checkWidget();
	if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	if (visibilityWindowListeners.length == 0) return;
	int index = -1;
	for (int i = 0; i < visibilityWindowListeners.length; i++) {
		if (listener == visibilityWindowListeners[i]){
			index = i;
			break;
		}
	}
	if (index == -1) return;
	if (visibilityWindowListeners.length == 1) {
		visibilityWindowListeners = new VisibilityWindowListener[0];
		return;
	}
	VisibilityWindowListener[] newVisibilityWindowListeners = new VisibilityWindowListener[visibilityWindowListeners.length - 1];
	System.arraycopy(visibilityWindowListeners, 0, newVisibilityWindowListeners, 0, index);
	System.arraycopy(visibilityWindowListeners, index + 1, newVisibilityWindowListeners, index, visibilityWindowListeners.length - index - 1);
	visibilityWindowListeners = newVisibilityWindowListeners;
}

/**
 * Renders HTML.
 * 
 * @param html the HTML content to be rendered
 *
 * @return true if the operation was successful and false otherwise.
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the html is null</li>
 * </ul>
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS when called from the wrong thread</li>
 *    <li>ERROR_WIDGET_DISPOSED when the widget has been disposed</li>
 * </ul>
 *  
 * @see #setUrl
 * 
 * @since 3.0
 */
public boolean setText(String html) {
	checkWidget();
	if (html == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	
	/*
	*  Feature in Mozilla.  The focus memory of Mozilla must be 
	*  properly managed through the nsIWebBrowserFocus interface.
	*  In particular, nsIWebBrowserFocus.deactivate must be called
	*  when the focus moves from the browser (or one of its children
	*  managed by Mozilla to another widget.  We currently do not
	*  get notified when a widget takes focus away from the Browser.
	*  As a result, deactivate is not properly called. This causes
	*  Mozilla to retake focus the next time a document is loaded.
	*  This breaks the case where the HTML loaded in the Browser 
	*  varies while the user enters characters in a text widget. The text
	*  widget loses focus every time new content is loaded.
	*  The current workaround is to call deactivate everytime if 
	*  the browser currently does not have focus. A better workaround
	*  would be to have a mean to call deactivate when the Browser
	*  or one of its children loses focus.
	*/
	if (this != getDisplay().getFocusControl()) Deactivate();
	
	/*
	 * Convert the String containing HTML to an array of
	 * bytes with UTF-8 data.
	 */
	byte[] data = null;
	try {
		data = html.getBytes("UTF-8"); //$NON-NLS-1$
	} catch (UnsupportedEncodingException e) {
		return false;
	}

	int /*long*/[] result = new int /*long*/[1];
	int rc = XPCOM.NS_GetServiceManager(result);
	if (rc != XPCOM.NS_OK) error(rc);
	if (result[0] == 0) error(XPCOM.NS_NOINTERFACE);

	nsIServiceManager serviceManager = new nsIServiceManager(result[0]);
	result[0] = 0;
	rc = serviceManager.GetService(XPCOM.NS_IOSERVICE_CID, nsIIOService.NS_IIOSERVICE_IID, result);
	if (rc != XPCOM.NS_OK) error(rc);
	if (result[0] == 0) error(XPCOM.NS_NOINTERFACE);
	serviceManager.Release();

	nsIIOService ioService = new nsIIOService(result[0]);
	result[0] = 0;
	/*
	* Note.  Mozilla ignores LINK tags used to load CSS stylesheets
	* when the URI protocol for the nsInputStreamChannel
	* is about:blank.  The fix is to specify the file protocol.
	*/
	byte[] aString = URI_FROMMEMORY.getBytes();
	int /*long*/ aSpec = XPCOM.nsEmbedCString_new(aString, aString.length);
	rc = ioService.NewURI(aSpec, null, 0, result);
	XPCOM.nsEmbedCString_delete(aSpec);
	if (rc != XPCOM.NS_OK) error(rc);
	if (result[0] == 0) error(XPCOM.NS_NOINTERFACE);
	ioService.Release();
	
	nsIURI uri = new nsIURI(result[0]);
	result[0] = 0;

	/* aContentType */
	byte[] buffer = "text/html".getBytes(); //$NON-NLS-1$
	byte[] contentTypeBuffer = new byte[buffer.length + 1];
	System.arraycopy(buffer, 0, contentTypeBuffer, 0, buffer.length);
	int /*long*/ aContentType = XPCOM.nsEmbedCString_new(contentTypeBuffer, contentTypeBuffer.length);

	/*
	 * First try to use nsIWebBrowserStream to set the text into the Browser, since this
	 * interface is frozen.  However, this may fail because this interface was only introduced
	 * as of mozilla 1.8; if this interface is not found then use the pre-1.8 approach of
	 * utilizing nsIDocShell instead. 
	 */
	result[0] = 0;
	rc = webBrowser.QueryInterface(nsIWebBrowserStream.NS_IWEBBROWSERSTREAM_IID, result);
	if (rc == XPCOM.NS_OK) {
		if (result[0] == 0) error(XPCOM.NS_ERROR_NO_INTERFACE);
		nsIWebBrowserStream stream = new nsIWebBrowserStream(result[0]);
		rc = stream.OpenStream(uri.getAddress(), aContentType);
		if (rc != XPCOM.NS_OK) error(rc);
		int /*long*/ ptr = XPCOM.PR_Malloc(data.length);
		XPCOM.memmove(ptr, data, data.length);
		rc = stream.AppendToStream(ptr, data.length);
		if (rc != XPCOM.NS_OK) error(rc);
		rc = stream.CloseStream();
		if (rc != XPCOM.NS_OK) error(rc);
		XPCOM.PR_Free(ptr);
		stream.Release();
	} else {
		rc = webBrowser.QueryInterface(nsIInterfaceRequestor.NS_IINTERFACEREQUESTOR_IID, result);
		if (rc != XPCOM.NS_OK) error(rc);
		if (result[0] == 0) error(XPCOM.NS_ERROR_NO_INTERFACE);

		nsIInterfaceRequestor interfaceRequestor = new nsIInterfaceRequestor(result[0]);
		result[0] = 0;
		rc = interfaceRequestor.GetInterface(nsIDocShell.NS_IDOCSHELL_IID, result);				
		interfaceRequestor.Release();

		nsIDocShell docShell = new nsIDocShell(result[0]);
		result[0] = 0;
		buffer = "UTF-8".getBytes(); //$NON-NLS-1$
		byte[] contentCharsetBuffer = new byte[buffer.length + 1];
		System.arraycopy(buffer, 0, contentCharsetBuffer, 0, buffer.length);
		int /*long*/ aContentCharset = XPCOM.nsEmbedCString_new(contentCharsetBuffer, contentCharsetBuffer.length);

		/*
		* Feature in Mozilla. LoadStream invokes the nsIInputStream argument
		* through a different thread.  The callback mechanism must attach 
		* a non java thread to the JVM otherwise the nsIInputStream Read and
		* Close methods never get called.
		*/
		InputStream inputStream = new InputStream(data);
		inputStream.AddRef();
		rc = docShell.LoadStream(inputStream.getAddress(), uri.getAddress(), aContentType,  aContentCharset, 0);
		if (rc != XPCOM.NS_OK) error(rc);
		XPCOM.nsEmbedCString_delete(aContentCharset);
		inputStream.Release();
		docShell.Release();
	}

	XPCOM.nsEmbedCString_delete(aContentType);
	uri.Release();
	return true;
}

/**
 * Loads a URL.
 * 
 * @param url the URL to be loaded
 *
 * @return true if the operation was successful and false otherwise.
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the url is null</li>
 * </ul>
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS when called from the wrong thread</li>
 *    <li>ERROR_WIDGET_DISPOSED when the widget has been disposed</li>
 * </ul>
 *  
 * @see #getUrl
 * 
 * @since 3.0
 */
public boolean setUrl(String url) {
	checkWidget();
	if (url == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	int /*long*/[] result = new int /*long*/[1];
	int rc = webBrowser.QueryInterface(nsIWebNavigation.NS_IWEBNAVIGATION_IID, result);
	if (rc != XPCOM.NS_OK) error(rc);
	if (result[0] == 0) error(XPCOM.NS_ERROR_NO_INTERFACE);

	nsIWebNavigation webNavigation = new nsIWebNavigation(result[0]);
    char[] arg = url.toCharArray(); 
    char[] c = new char[arg.length+1];
    System.arraycopy(arg,0,c,0,arg.length);
	rc = webNavigation.LoadURI(c, nsIWebNavigation.LOAD_FLAGS_NONE, 0, 0, 0);
	webNavigation.Release();
	return rc == XPCOM.NS_OK;
}

/**
 * Stop any loading and rendering activity.
 *
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS when called from the wrong thread</li>
 *    <li>ERROR_WIDGET_DISPOSED when the widget has been disposed</li>
 * </ul>
 *
 * @since 3.0
 */
public void stop() {
	checkWidget();
	int /*long*/[] result = new int /*long*/[1];
	int rc = webBrowser.QueryInterface(nsIWebNavigation.NS_IWEBNAVIGATION_IID, result);
	if (rc != XPCOM.NS_OK) error(rc);
	if (result[0] == 0) error(XPCOM.NS_ERROR_NO_INTERFACE);
	
	nsIWebNavigation webNavigation = new nsIWebNavigation(result[0]);	 	
	rc = webNavigation.Stop(nsIWebNavigation.STOP_ALL);
	if (rc != XPCOM.NS_OK) error(rc);
	webNavigation.Release();
}

/* nsISupports */

int /*long*/ QueryInterface(int /*long*/ riid, int /*long*/ ppvObject) {
	if (riid == 0 || ppvObject == 0) return XPCOM.NS_ERROR_NO_INTERFACE;

	nsID guid = new nsID();
	XPCOM.memmove(guid, riid, nsID.sizeof);

	if (guid.Equals(nsISupports.NS_ISUPPORTS_IID)) {
		XPCOM.memmove(ppvObject, new int /*long*/[] {supports.getAddress()}, OS.PTR_SIZEOF);
		AddRef();
		return XPCOM.NS_OK;
	}
	if (guid.Equals(nsIWeakReference.NS_IWEAKREFERENCE_IID)) {
		XPCOM.memmove(ppvObject, new int /*long*/[] {weakReference.getAddress()}, OS.PTR_SIZEOF);
		AddRef();
		return XPCOM.NS_OK;
	}
	if (guid.Equals(nsIWebProgressListener.NS_IWEBPROGRESSLISTENER_IID)) {
		XPCOM.memmove(ppvObject, new int /*long*/[] {webProgressListener.getAddress()}, OS.PTR_SIZEOF);
		AddRef();
		return XPCOM.NS_OK;
	}
	if (guid.Equals(nsIWebBrowserChrome.NS_IWEBBROWSERCHROME_IID)) {
		XPCOM.memmove(ppvObject, new int /*long*/[] {webBrowserChrome.getAddress()}, OS.PTR_SIZEOF);
		AddRef();
		return XPCOM.NS_OK;
	}
	if (guid.Equals(nsIWebBrowserChromeFocus.NS_IWEBBROWSERCHROMEFOCUS_IID)) {
		XPCOM.memmove(ppvObject, new int /*long*/[] {webBrowserChromeFocus.getAddress()}, OS.PTR_SIZEOF);
		AddRef();
		return XPCOM.NS_OK;
	}
	if (guid.Equals(nsIEmbeddingSiteWindow.NS_IEMBEDDINGSITEWINDOW_IID)) {
		XPCOM.memmove(ppvObject, new int /*long*/[] {embeddingSiteWindow.getAddress()}, OS.PTR_SIZEOF);
		AddRef();
		return XPCOM.NS_OK;
	}
	if (guid.Equals(nsIInterfaceRequestor.NS_IINTERFACEREQUESTOR_IID)) {
		XPCOM.memmove(ppvObject, new int /*long*/[] {interfaceRequestor.getAddress()}, OS.PTR_SIZEOF);
		AddRef();
		return XPCOM.NS_OK;
	}
	if (guid.Equals(nsISupportsWeakReference.NS_ISUPPORTSWEAKREFERENCE_IID)) {
		XPCOM.memmove(ppvObject, new int /*long*/[] {supportsWeakReference.getAddress()}, OS.PTR_SIZEOF);
		AddRef();
		return XPCOM.NS_OK;
	}
	if (guid.Equals(nsIContextMenuListener.NS_ICONTEXTMENULISTENER_IID)) {
		XPCOM.memmove(ppvObject, new int /*long*/[] {contextMenuListener.getAddress()}, OS.PTR_SIZEOF);
		AddRef();
		return XPCOM.NS_OK;
	}
	if (guid.Equals(nsIURIContentListener.NS_IURICONTENTLISTENER_IID)) {
		XPCOM.memmove(ppvObject, new int /*long*/[] {uriContentListener.getAddress()}, OS.PTR_SIZEOF);
		AddRef();
		return XPCOM.NS_OK;
	}
	if (guid.Equals(nsITooltipListener.NS_ITOOLTIPLISTENER_IID)) {
		XPCOM.memmove(ppvObject, new int /*long*/[] {tooltipListener.getAddress()}, OS.PTR_SIZEOF);
		AddRef();
		return XPCOM.NS_OK;
	}
	XPCOM.memmove(ppvObject, new int /*long*/[] {0}, OS.PTR_SIZEOF);
	return XPCOM.NS_ERROR_NO_INTERFACE;
}

int /*long*/ AddRef() {
	refCount++;
	return refCount;
}

int /*long*/ Release() {
	refCount--;
	if (refCount == 0) disposeCOMInterfaces();
	return refCount;
}

/* nsIWeakReference */	
	
int /*long*/ QueryReferent(int /*long*/ riid, int /*long*/ ppvObject) {
	return QueryInterface(riid,ppvObject);
}

/* nsIInterfaceRequestor */

int /*long*/ GetInterface(int /*long*/ riid, int /*long*/ ppvObject) {
	if (riid == 0 || ppvObject == 0) return XPCOM.NS_ERROR_NO_INTERFACE;
	nsID guid = new nsID();
	XPCOM.memmove(guid, riid, nsID.sizeof);
	if (guid.Equals(nsIDOMWindow.NS_IDOMWINDOW_IID)) {
		int /*long*/[] aContentDOMWindow = new int /*long*/[1];
		int rc = webBrowser.GetContentDOMWindow(aContentDOMWindow);
		if (rc != XPCOM.NS_OK) error(rc);
		if (aContentDOMWindow[0] == 0) error(XPCOM.NS_ERROR_NO_INTERFACE);
		XPCOM.memmove(ppvObject, aContentDOMWindow, OS.PTR_SIZEOF);
		return rc;
	}
	return QueryInterface(riid,ppvObject);
}

int /*long*/ GetWeakReference(int /*long*/ ppvObject) {
	XPCOM.memmove(ppvObject, new int /*long*/[] {weakReference.getAddress()}, OS.PTR_SIZEOF);
	AddRef();
	return XPCOM.NS_OK;
}

/* nsIWebProgressListener */

int /*long*/ OnStateChange(int /*long*/ aWebProgress, int /*long*/ aRequest, int /*long*/ aStateFlags, int /*long*/ aStatus) {
	if ((aStateFlags & nsIWebProgressListener.STATE_IS_DOCUMENT) == 0) return XPCOM.NS_OK;
	if ((aStateFlags & nsIWebProgressListener.STATE_START) != 0) {
		if (request == 0) request = aRequest;
	} else if ((aStateFlags & nsIWebProgressListener.STATE_REDIRECTING) != 0) {
		if (request == aRequest) request = 0;
	} else if ((aStateFlags & nsIWebProgressListener.STATE_STOP) != 0) {
		/*
		* Feature on Mozilla.  When a request is redirected (STATE_REDIRECTING),
		* it never reaches the state STATE_STOP and it is replaced with a new request.
		* The new request is received when it is in the state STATE_STOP.
		* To handle this case,  the variable request is set to 0 when the corresponding
		* request is redirected. The following request received with the state STATE_STOP
		* - the new request resulting from the redirection - is used to send
		* the ProgressListener.completed event.
		*/
		if (request == aRequest || request == 0) {
			request = 0;
			StatusTextEvent event = new StatusTextEvent(this);
			event.display = getDisplay();
			event.widget = this;
			event.text = ""; //$NON-NLS-1$
			for (int i = 0; i < statusTextListeners.length; i++)
				statusTextListeners[i].changed(event);
			
			ProgressEvent event2 = new ProgressEvent(this);
			event2.display = getDisplay();
			event2.widget = this;
			for (int i = 0; i < progressListeners.length; i++)
				progressListeners[i].completed(event2);
		}
	}
	return XPCOM.NS_OK;
}	

int /*long*/ OnProgressChange(int /*long*/ aWebProgress, int /*long*/ aRequest, int /*long*/ aCurSelfProgress, int /*long*/ aMaxSelfProgress, int /*long*/ aCurTotalProgress, int /*long*/ aMaxTotalProgress) {
	if (progressListeners.length == 0) return XPCOM.NS_OK;
	
	int /*long*/ total = aMaxTotalProgress;
	if (total <= 0) total = Integer.MAX_VALUE;
	ProgressEvent event = new ProgressEvent(this);
	event.display = getDisplay();
	event.widget = this;
	event.current = (int)/*64*/aCurTotalProgress;
	event.total = (int)/*64*/aMaxTotalProgress;
	for (int i = 0; i < progressListeners.length; i++)
		progressListeners[i].changed(event);			
	return XPCOM.NS_OK;
}		

int /*long*/ OnLocationChange(int /*long*/ aWebProgress, int /*long*/ aRequest, int /*long*/ aLocation) {
	/*
	* Feature on Mozilla.  When a page is loaded via setText before a previous
	* setText page load has completed, the expected OnStateChange STATE_STOP for the
	* original setText never arrives because it gets replaced by the OnStateChange
	* STATE_STOP for the new request.  This results in the request field never being
	* cleared because the original request's OnStateChange STATE_STOP is still expected
	* (but never arrives).  To handle this case, the request field is updated to the new
	* overriding request since its OnStateChange STATE_STOP will be received next.
	*/
	if (request != 0 && request != aRequest) request = aRequest;

	if (locationListeners.length == 0) return XPCOM.NS_OK;

	nsIWebProgress webProgress = new nsIWebProgress(aWebProgress);
	int /*long*/[] aDOMWindow = new int /*long*/[1];
	int rc = webProgress.GetDOMWindow(aDOMWindow);
	if (rc != XPCOM.NS_OK) error(rc);
	if (aDOMWindow[0] == 0) error(XPCOM.NS_ERROR_NO_INTERFACE);
	
	nsIDOMWindow domWindow = new nsIDOMWindow(aDOMWindow[0]);
	int /*long*/[] aTop = new int /*long*/[1];
	rc = domWindow.GetTop(aTop);
	if (rc != XPCOM.NS_OK) error(rc);
	if (aTop[0] == 0) error(XPCOM.NS_ERROR_NO_INTERFACE);
	domWindow.Release();
	
	nsIDOMWindow topWindow = new nsIDOMWindow(aTop[0]);
	topWindow.Release();
	
	nsIURI location = new nsIURI(aLocation);
	int /*long*/ aSpec = XPCOM.nsEmbedCString_new();
	location.GetSpec(aSpec);
	int length = XPCOM.nsEmbedCString_Length(aSpec);
	int /*long*/ buffer = XPCOM.nsEmbedCString_get(aSpec);
	byte[] dest = new byte[length];
	XPCOM.memmove(dest, buffer, length);
	XPCOM.nsEmbedCString_delete(aSpec);

	LocationEvent event = new LocationEvent(this);
	event.display = getDisplay();
	event.widget = this;
	event.location = new String(dest);
	if (event.location.equals (URI_FROMMEMORY)) {
		/*
		 * If the URI indicates that the page is being rendered from memory
		 * (ie.- via setText()) then set the event location to about:blank
		 * to be consistent with win32.
		 */
		event.location = ABOUT_BLANK;
	}
	event.top = aTop[0] == aDOMWindow[0];
	for (int i = 0; i < locationListeners.length; i++)
		locationListeners[i].changed(event);
	return XPCOM.NS_OK;
}
  
int /*long*/ OnStatusChange(int /*long*/ aWebProgress, int /*long*/ aRequest, int /*long*/ aStatus, int /*long*/ aMessage) {
	/*
	* Feature in Mozilla.  Navigating to an HTTPS link without a user profile
	* set causes a crash.  The workaround is to abort attempts to navigate to
	* HTTPS pages if a profile is not being used.
	* 
	* Most navigation requests for HTTPS pages are handled in OnStartURIOpen.
	* However, https page requests that do not initially specify https as their
	* protocol will get past this check since they are resolved afterwards.
	* The workaround is to check the url whenever there is a status change, and
	* to abort any detected https requests if a profile is not being used.
	*/
	nsIRequest request = new nsIRequest(aRequest);
	int /*long*/ aName = XPCOM.nsEmbedCString_new();
	request.GetName(aName);
	int length = XPCOM.nsEmbedCString_Length(aName);
	int /*long*/ buffer = XPCOM.nsEmbedCString_get(aName);
	byte[] bytes = new byte[length];
	XPCOM.memmove(bytes, buffer, length);
	XPCOM.nsEmbedCString_delete(aName);
	String value = new String(bytes);
	if (!usingProfile && value.startsWith(XPCOM.HTTPS_PROTOCOL)) {
		request.Cancel(XPCOM.NS_BINDING_ABORTED);
		return XPCOM.NS_OK;
	}

	if (statusTextListeners.length == 0) return XPCOM.NS_OK;

	StatusTextEvent event = new StatusTextEvent(this);
	event.display = getDisplay();
	event.widget = this;
	length = XPCOM.strlen_PRUnichar(aMessage);
	char[] dest = new char[length];
	XPCOM.memmove(dest, aMessage, length * 2);
	event.text = new String(dest);
	for (int i = 0; i < statusTextListeners.length; i++)
		statusTextListeners[i].changed(event);

	return XPCOM.NS_OK;
}		

int /*long*/ OnSecurityChange(int /*long*/ aWebProgress, int /*long*/ aRequest, int /*long*/ state) {
	return XPCOM.NS_OK;
}

/* nsIWebBrowserChrome */

int /*long*/ SetStatus(int /*long*/ statusType, int /*long*/ status) {
	StatusTextEvent event = new StatusTextEvent(this);
	event.display = getDisplay();
	event.widget = this;
	int length = XPCOM.strlen_PRUnichar(status);
	char[] dest = new char[length];
	XPCOM.memmove(dest, status, length * 2);
	String string = new String(dest);
	if (string == null) string = ""; //$NON-NLS-1$
	event.text = string;
	for (int i = 0; i < statusTextListeners.length; i++)
		statusTextListeners[i].changed(event);	
	return XPCOM.NS_OK;
}		

int /*long*/ GetWebBrowser(int /*long*/ aWebBrowser) {
	int /*long*/[] ret = new int /*long*/[1];	
	if (webBrowser != null) {
		webBrowser.AddRef();
		ret[0] = webBrowser.getAddress();	
	}
	XPCOM.memmove(aWebBrowser, ret, OS.PTR_SIZEOF);
	return XPCOM.NS_OK;
}

int /*long*/ SetWebBrowser(int /*long*/ aWebBrowser) {
	if (webBrowser != null) webBrowser.Release();
	webBrowser = aWebBrowser != 0 ? new nsIWebBrowser(aWebBrowser) : null;  				
	return XPCOM.NS_OK;
}
   
int /*long*/ GetChromeFlags(int /*long*/ aChromeFlags) {
	int[] ret = new int[1];
	ret[0] = chromeFlags;
	/* aChromeFlags is a pointer to a type of size 4 */
	XPCOM.memmove(aChromeFlags, ret, 4);
	return XPCOM.NS_OK;
}

int /*long*/ SetChromeFlags(int /*long*/ aChromeFlags) {
	chromeFlags = (int)/*64*/aChromeFlags;
	return XPCOM.NS_OK;
}
   
int /*long*/ DestroyBrowserWindow() {
	WindowEvent newEvent = new WindowEvent(this);
	newEvent.display = getDisplay();
	newEvent.widget = this;
	for (int i = 0; i < closeWindowListeners.length; i++)
		closeWindowListeners[i].close(newEvent);
	/*
	* Note on Mozilla.  The DestroyBrowserWindow notification cannot be cancelled.
	* The browser widget cannot be used after this notification has been received.
	* The application is advised to close the window hosting the browser widget.
	* The browser widget must be disposed in all cases.
	*/
	dispose();
	return XPCOM.NS_OK;
}
   	
int /*long*/ SizeBrowserTo(int /*long*/ aCX, int /*long*/ aCY) {
	size = new Point((int)/*64*/aCX, (int)/*64*/aCY);
	return XPCOM.NS_OK;
}

int /*long*/ ShowAsModal() {
	return XPCOM.NS_ERROR_NOT_IMPLEMENTED;
}
   
int /*long*/ IsWindowModal(int /*long*/ retval) {
	// no modal loop
	/* Note. boolean remains of size 4 on 64 bit machine */
	XPCOM.memmove(retval, new int[] {0}, 4);
	return XPCOM.NS_OK;
}
   
int /*long*/ ExitModalEventLoop(int /*long*/ aStatus) {
	return XPCOM.NS_OK;
}

/* nsIEmbeddingSiteWindow */ 
   
int /*long*/ SetDimensions(int /*long*/ flags, int /*long*/ x, int /*long*/ y, int /*long*/ cx, int /*long*/ cy) {
	if (flags == nsIEmbeddingSiteWindow.DIM_FLAGS_POSITION) location = new Point((int)/*64*/x, (int)/*64*/y);
	return XPCOM.NS_OK;   	
}	

int /*long*/ GetDimensions(int /*long*/ flags, int /*long*/ x, int /*long*/ y, int /*long*/ cx, int /*long*/ cy) {
	return XPCOM.NS_OK;     	
}	

int /*long*/ SetFocus() {
	int /*long*/[] result = new int /*long*/[1];
	int rc = webBrowser.QueryInterface(nsIBaseWindow.NS_IBASEWINDOW_IID, result);
	if (rc != XPCOM.NS_OK) error(rc);
	if (result[0] == 0) error(XPCOM.NS_ERROR_NO_INTERFACE);
	
	nsIBaseWindow baseWindow = new nsIBaseWindow(result[0]);
	rc = baseWindow.SetFocus();
	if (rc != XPCOM.NS_OK) error(rc);
	baseWindow.Release();

	/*
	* Note. Mozilla notifies here that one of the children took
	* focus. This could or should be used to fire an SWT.FOCUS_IN
	* event on Browser focus listeners.
	*/
	return XPCOM.NS_OK;     	
}	

int /*long*/ GetVisibility(int /*long*/ aVisibility) {
	/* Note. boolean remains of size 4 on 64 bit machine */
	XPCOM.memmove(aVisibility, new int[] {isVisible() ? 1 : 0}, 4);
	return XPCOM.NS_OK; 
}
   
int /*long*/ SetVisibility(int /*long*/ aVisibility) {
	WindowEvent event = new WindowEvent(this);
	event.display = getDisplay();
	event.widget = this;
	if (aVisibility == 1) {
		/*
		* Bug in Mozilla.  When the JavaScript window.open is executed, Mozilla
		* fires multiple SetVisibility 1 notifications.  The workaround is
		* to ignore subsequent notifications. 
		*/
		if (!visible) {
			visible = true;
			event.location = location;
			event.size = size;
			event.addressBar = addressBar;
			event.menuBar = menuBar;
			event.statusBar = statusBar;
			event.toolBar = toolBar;
			for (int i = 0; i < visibilityWindowListeners.length; i++)
				visibilityWindowListeners[i].show(event);
			location = null;
			size = null;
		}
	} else {
		visible = false;
		for (int i = 0; i < visibilityWindowListeners.length; i++)
			visibilityWindowListeners[i].hide(event);
	}
	return XPCOM.NS_OK;     	
}

int /*long*/ GetTitle(int /*long*/ aTitle) {
	return XPCOM.NS_OK;     	
}
 
int /*long*/ SetTitle(int /*long*/ aTitle) {
	if (titleListeners.length == 0) return XPCOM.NS_OK;
	TitleEvent event = new TitleEvent(this);
	event.display = getDisplay();
	event.widget = this;
	int length = XPCOM.strlen_PRUnichar(aTitle);
	char[] dest = new char[length];
	XPCOM.memmove(dest, aTitle, length * 2);
	event.title = new String(dest);
	for (int i = 0; i < titleListeners.length; i++)
		titleListeners[i].changed(event);
	return XPCOM.NS_OK;     	
}

int /*long*/ GetSiteWindow(int /*long*/ aSiteWindow) {
	/*
	* Note.  The handle is expected to be an HWND on Windows and
	* a GtkWidget* on GTK.  This callback is invoked on Windows
	* when the javascript window.print is invoked and the print
	* dialog comes up. If no handle is returned, the print dialog
	* does not come up on this platform.  
	*/
	XPCOM.memmove(aSiteWindow, new int /*long*/[] {embedHandle}, OS.PTR_SIZEOF);
	return XPCOM.NS_OK;     	
}  
 
/* nsIWebBrowserChromeFocus */

int /*long*/ FocusNextElement() {
	/*
	* Bug in Mozilla embedding API.  Mozilla takes back the focus after sending
	* this event.  This prevents tabbing out of Mozilla. This behaviour can be reproduced
	* with the Mozilla application TestGtkEmbed.  The workaround is to
	* send the traversal notification after this callback returns.
	*/
	getDisplay().asyncExec(new Runnable() {
		public void run() {
			traverse(SWT.TRAVERSE_TAB_NEXT);
		}
	});
	return XPCOM.NS_OK;  
}

int /*long*/ FocusPrevElement() {
	/*
	* Bug in Mozilla embedding API.  Mozilla takes back the focus after sending
	* this event.  This prevents tabbing out of Mozilla. This behaviour can be reproduced
	* with the Mozilla application TestGtkEmbed.  The workaround is to
	* send the traversal notification after this callback returns.
	*/
	getDisplay().asyncExec(new Runnable() {
		public void run() {
			traverse(SWT.TRAVERSE_TAB_PREVIOUS);
		}
	});
	return XPCOM.NS_OK;     	
}

/* nsIContextMenuListener */

int /*long*/ OnShowContextMenu(int /*long*/ aContextFlags, int /*long*/ aEvent, int /*long*/ aNode) {
	nsIDOMEvent domEvent = new nsIDOMEvent(aEvent);
	int /*long*/[] result = new int /*long*/[1];
	int rc = domEvent.QueryInterface(nsIDOMMouseEvent.NS_IDOMMOUSEEVENT_IID, result);
	if (rc != XPCOM.NS_OK) error(rc);
	if (result[0] == 0) error(XPCOM.NS_NOINTERFACE);

	nsIDOMMouseEvent domMouseEvent = new nsIDOMMouseEvent(result[0]);
	int[] aScreenX = new int[1], aScreenY = new int[1];
	rc = domMouseEvent.GetScreenX(aScreenX);
	if (rc != XPCOM.NS_OK) error(rc);
	rc = domMouseEvent.GetScreenY(aScreenY);
	if (rc != XPCOM.NS_OK) error(rc);
	domMouseEvent.Release();
	
	Event event = new Event();
	event.x = aScreenX[0];
	event.y = aScreenY[0];
	notifyListeners(SWT.MenuDetect, event);
	if (!event.doit) return XPCOM.NS_OK;
	Menu menu = getMenu();
	if (menu != null && !menu.isDisposed ()) {
		if (aScreenX[0] != event.x || aScreenY[0] != event.y) {
			menu.setLocation (event.x, event.y);
		}
		menu.setVisible (true);
	}
	return XPCOM.NS_OK;     	
}

/* nsIURIContentListener */

int /*long*/ OnStartURIOpen(int /*long*/ aURI, int /*long*/ retval) {
	nsIURI location = new nsIURI(aURI);
	int /*long*/ aSpec = XPCOM.nsEmbedCString_new();
	location.GetSpec(aSpec);
	int length = XPCOM.nsEmbedCString_Length(aSpec);
	int /*long*/ buffer = XPCOM.nsEmbedCString_get(aSpec);
	buffer = XPCOM.nsEmbedCString_get(aSpec);
	byte[] dest = new byte[length];
	XPCOM.memmove(dest, buffer, length);
	XPCOM.nsEmbedCString_delete(aSpec);
	String value = new String(dest);
	/*
	* Feature in Mozilla.  Navigating to an HTTPS link without a user profile
	* set causes a crash.  The workaround is to abort attempts to navigate to
	* HTTPS pages if a profile is not being used.
	*/
	boolean isHttps = value.startsWith(XPCOM.HTTPS_PROTOCOL);
	if (locationListeners.length == 0) {
		XPCOM.memmove(retval, new int[] {isHttps && !usingProfile ? 1 : 0}, 4);
		return XPCOM.NS_OK;
	}
	boolean doit = !isHttps || usingProfile;
	if (request == 0) {
		LocationEvent event = new LocationEvent(this);
		event.display = getDisplay();
		event.widget = this;
		event.location = value;
		if (event.location.equals (URI_FROMMEMORY)) {
			/*
			 * If the URI indicates that the page is being rendered from memory
			 * (ie.- via setText()) then set the event location to about:blank
			 * to be consistent with win32.
			 */
			event.location = ABOUT_BLANK;
		}
		event.doit = doit;
		for (int i = 0; i < locationListeners.length; i++)
			locationListeners[i].changing(event);
		if (!isHttps || usingProfile) doit = event.doit;
	}
	/* Note. boolean remains of size 4 on 64 bit machine */
	XPCOM.memmove(retval, new int[] {doit ? 0 : 1}, 4);
	return XPCOM.NS_OK;
}

int /*long*/ DoContent(int /*long*/ aContentType, int /*long*/ aIsContentPreferred, int /*long*/ aRequest, int /*long*/ aContentHandler, int /*long*/ retval) {
	return XPCOM.NS_ERROR_NOT_IMPLEMENTED;
}

int /*long*/ IsPreferred(int /*long*/ aContentType, int /*long*/ aDesiredContentType, int /*long*/ retval) {
	boolean preferred = false;
	int size = XPCOM.strlen(aContentType);
	if (size > 0) {
		byte[] typeBytes = new byte[size + 1];
		XPCOM.memmove(typeBytes, aContentType, size);
		String contentType = new String(typeBytes);

		/* do not attempt to handle known problematic content types */
		if (!contentType.equals(XPCOM.CONTENT_MAYBETEXT) && !contentType.equals(XPCOM.CONTENT_MULTIPART)) {
			/* determine whether browser can handle the content type */
			int /*long*/[] result = new int /*long*/[1];
			int rc = XPCOM.NS_GetServiceManager(result);
			if (rc != XPCOM.NS_OK) error(rc);
			if (result[0] == 0) error(XPCOM.NS_NOINTERFACE);
			nsIServiceManager serviceManager = new nsIServiceManager(result[0]);
			result[0] = 0;
			rc = serviceManager.GetService(XPCOM.NS_CATEGORYMANAGER_CID, nsICategoryManager.NS_ICATEGORYMANAGER_IID, result);
			serviceManager.Release();
			if (rc != XPCOM.NS_OK) error(rc);
			if (result[0] == 0) error(XPCOM.NS_NOINTERFACE);

			nsICategoryManager categoryManager = new nsICategoryManager(result[0]);
			result[0] = 0;
			byte[] categoryBytes = Converter.wcsToMbcs(null, "Gecko-Content-Viewers", true);	//$NON-NLS-1$
			rc = categoryManager.GetCategoryEntry(categoryBytes, typeBytes, result);
			categoryManager.Release();
			/* if no viewer for the content type is registered then rc == XPCOM.NS_ERROR_NOT_AVAILABLE */
			preferred = rc == XPCOM.NS_OK;
		}
	}

	/* note that boolean remains of size 4 on 64 bit machines */
	XPCOM.memmove(retval, new int[] {preferred ? 1 : 0}, 4);
	return XPCOM.NS_OK;
}

int /*long*/ CanHandleContent(int /*long*/ aContentType, int /*long*/ aIsContentPreferred, int /*long*/ aDesiredContentType, int /*long*/ retval) {
	return XPCOM.NS_ERROR_NOT_IMPLEMENTED;
}

int /*long*/ GetLoadCookie(int /*long*/ aLoadCookie) {
	return XPCOM.NS_ERROR_NOT_IMPLEMENTED;
}

int /*long*/ SetLoadCookie(int /*long*/ aLoadCookie) {
	return XPCOM.NS_ERROR_NOT_IMPLEMENTED;
}

int /*long*/ GetParentContentListener(int /*long*/ aParentContentListener) {
	return XPCOM.NS_ERROR_NOT_IMPLEMENTED;
}
	
int /*long*/ SetParentContentListener(int /*long*/ aParentContentListener) {
	return XPCOM.NS_ERROR_NOT_IMPLEMENTED;
}

/* nsITooltipListener */

int /*long*/ OnShowTooltip(int /*long*/ aXCoords, int /*long*/ aYCoords, int /*long*/ aTipText) {
	int length = XPCOM.strlen_PRUnichar(aTipText);
	char[] dest = new char[length];
	XPCOM.memmove(dest, aTipText, length * 2);
	String text = new String(dest);
	if (tip != null && !tip.isDisposed()) tip.dispose();
	Display display = getDisplay();
	Shell parent = getShell();
	tip = new Shell(parent, SWT.ON_TOP);
	tip.setLayout(new FillLayout());
	Label label = new Label(tip, SWT.CENTER);
	label.setForeground(display.getSystemColor(SWT.COLOR_INFO_FOREGROUND));
	label.setBackground(display.getSystemColor(SWT.COLOR_INFO_BACKGROUND));
	label.setText(text);
	/*
	* Bug in Mozilla embedded API.  Tooltip coordinates are wrong for 
	* elements inside an inline frame (IFrame tag).  The workaround is 
	* to position the tooltip based on the mouse cursor location.
	*/
	Point point = display.getCursorLocation();
	/* Assuming cursor is 21x21 because this is the size of
	 * the arrow cursor on Windows
	 */ 
	point.y += 21;
	tip.setLocation(point);
	tip.pack();
	tip.setVisible(true);
	return XPCOM.NS_OK;
}

int /*long*/ OnHideTooltip() {
	if (tip != null && !tip.isDisposed()) tip.dispose();
	tip = null;
	return XPCOM.NS_OK;
}
}
