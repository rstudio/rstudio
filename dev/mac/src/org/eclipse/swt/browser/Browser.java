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
package org.eclipse.swt.browser;

import org.eclipse.swt.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.internal.Callback;
import org.eclipse.swt.internal.carbon.*;
import org.eclipse.swt.widgets.*;

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
	
	/* Package Name */
	static final String PACKAGE_PREFIX = "org.eclipse.swt.browser."; //$NON-NLS-1$
	static final String ADD_WIDGET_KEY = "org.eclipse.swt.internal.addWidget"; //$NON-NLS-1$
	// GOOGLE: patched in from https://bugs.eclipse.org/bugs/show_bug.cgi?id=161259
	static final String CLEAR_GRAB_BIT = "org.eclipse.swt.internal.carbon.clearGrabBit"; //$NON-NLS-1$
	static final String BROWSER_WINDOW = "org.eclipse.swt.browser.Browser.Window"; //$NON-NLS-1$
	static final int MAX_PROGRESS = 100;
	
	/* External Listener management */
	CloseWindowListener[] closeWindowListeners = new CloseWindowListener[0];
	LocationListener[] locationListeners = new LocationListener[0];
	OpenWindowListener[] openWindowListeners = new OpenWindowListener[0];
	ProgressListener[] progressListeners = new ProgressListener[0];
	StatusTextListener[] statusTextListeners = new StatusTextListener[0];
	TitleListener[] titleListeners = new TitleListener[0];
	VisibilityWindowListener[] visibilityWindowListeners = new VisibilityWindowListener[0];
	
	static Callback Callback3, Callback7;

	/* Objective-C WebView delegate */
	int delegate;
	
	/* Carbon HIView handle */
	int webViewHandle;
	
	boolean changingLocation;
	String html;
	int identifier;
	int resourceCount;
	String url = "";
	Point location;
	Point size;
	boolean statusBar = true, toolBar = true, ignoreDispose;
	//TEMPORARY CODE
//	boolean doit;

	static final int MIN_SIZE = 16;

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
 * @see Widget#getStyle
 * 
 * @since 3.0
 */
public Browser(Composite parent, int style) {
	super(parent, style);

	/*
	* Note.  Loading the webkit bundle on Jaguar causes a crash.
	* The workaround is to detect any OS prior to 10.30 and fail
	* without crashing.
	*/
	if (OS.VERSION < 0x1030) {
		dispose();
		SWT.error(SWT.ERROR_NO_HANDLES);
	}
	int outControl[] = new int[1];
	try {
		WebKit.HIWebViewCreate(outControl);
	} catch (UnsatisfiedLinkError e) {
		dispose();
		SWT.error(SWT.ERROR_NO_HANDLES);
	}
	webViewHandle = outControl[0];
	if (webViewHandle == 0) {
		dispose();
		SWT.error(SWT.ERROR_NO_HANDLES);		
	}
	Display display = getDisplay();
	display.setData(ADD_WIDGET_KEY, new Object[] {new Integer(webViewHandle), this});
	// GOOGLE: patched in from https://bugs.eclipse.org/bugs/show_bug.cgi?id=161259
	setData(CLEAR_GRAB_BIT, null);

	/*
	* Bug in Safari.  For some reason, every application must contain
	* a visible window that has never had a WebView or mouse move events
	* are not delivered.  This seems to happen after a browser has been
	* either hidden or disposed in any window.  The fix is to create a
	* single transparent overlay window that is disposed when the display
	* is disposed.
	*/
	if (display.getData(BROWSER_WINDOW) == null) {
		Rect bounds = new Rect ();
		OS.SetRect (bounds, (short) 0, (short) 0, (short) 1, (short) 1);
		final int[] outWindow = new int[1];
		OS.CreateNewWindow(OS.kOverlayWindowClass, 0, bounds, outWindow);
		OS.ShowWindow(outWindow[0]);
		display.disposeExec(new Runnable() {
			public void run() {
				if (outWindow[0] != 0) {
					OS.DisposeWindow(outWindow[0]);
				}
				outWindow[0] = 0;
			}
		});
		display.setData(BROWSER_WINDOW, outWindow);
	}
	
	/*
	 * Bug in Safari. The WebView does not receive mouse and key events when it is added
	 * to a visible top window.  It is assumed that Safari hooks its own event listener
	 * when the top window emits the kEventWindowShown event. The workaround is to send a
	 * fake kEventWindowShown event to the top window after the WebView has been added
	 * to the HIView (after the top window is visible) to give Safari a chance to hook
	 * events.
	 */
	int window = OS.GetControlOwner(handle);
	if (OS.HIVIEW) {
		int[] contentView = new int[1];
		OS.HIViewFindByID(OS.HIViewGetRoot(window), OS.kHIViewWindowContentID(), contentView);
		OS.HIViewAddSubview(contentView[0], webViewHandle);
		OS.HIViewChangeFeatures(webViewHandle, OS.kHIViewFeatureIsOpaque, 0);
	} else {
		OS.HIViewAddSubview(handle, webViewHandle);
	}
	OS.HIViewSetVisible(webViewHandle, true);	
	if (getShell().isVisible()) {
		int[] showEvent = new int[1];
		OS.CreateEvent(0, OS.kEventClassWindow, OS.kEventWindowShown, 0.0, OS.kEventAttributeUserEvent, showEvent);
		OS.SetEventParameter(showEvent[0], OS.kEventParamDirectObject, OS.typeWindowRef, 4, new int[] {OS.GetControlOwner(handle)});
		OS.SendEventToEventTarget(showEvent[0], OS.GetWindowEventTarget(window));
		if (showEvent[0] != 0) OS.ReleaseEvent(showEvent[0]);
	}

	final int webView = WebKit.HIWebViewGetWebView(webViewHandle);
	/*
	* This code is intentionally commented. Setting a group name is the right thing
	* to do in order to avoid multiple open window requests. For some reason, Safari
	* crashes when requested to reopen the same window if that window was previously
	* closed. This may be because that window was not correctly closed. 
	*/	
//	String groupName = "MyDocument"; //$NON-NLS-1$
//	int length = groupName.length();
//	char[] buffer = new char[length];
//	groupName.getChars(0, length, buffer, 0);
//	int groupNameString = OS.CFStringCreateWithCharacters(0, buffer, length);
//	// [webView setGroupName:@"MyDocument"];
//	WebKit.objc_msgSend(webView, WebKit.S_setGroupName, groupNameString);
//	OS.CFRelease(groupNameString);
	
	final int notificationCenter = WebKit.objc_msgSend(WebKit.C_NSNotificationCenter, WebKit.S_defaultCenter);

	Listener listener = new Listener() {
		public void handleEvent(Event e) {
			switch (e.type) {
				case SWT.Dispose: {
					/* make this handler run after other dispose listeners */
					if (ignoreDispose) {
						ignoreDispose = false;
						break;
					}
					ignoreDispose = true;
					notifyListeners (e.type, e);
					e.type = SWT.NONE;

					Shell shell = getShell();
					shell.removeListener(SWT.Resize, this);
					shell.removeListener(SWT.Show, this);
					shell.removeListener(SWT.Hide, this);
					Control c = Browser.this;
					do {
						c.removeListener(SWT.Show, this);
						c.removeListener(SWT.Hide, this);
						c = c.getParent();
					} while (c != shell);

					e.display.setData(ADD_WIDGET_KEY, new Object[] {new Integer(webViewHandle), null});

					WebKit.objc_msgSend(webView, WebKit.S_setFrameLoadDelegate, 0);
					WebKit.objc_msgSend(webView, WebKit.S_setResourceLoadDelegate, 0);
					WebKit.objc_msgSend(webView, WebKit.S_setUIDelegate, 0);
					WebKit.objc_msgSend(webView, WebKit.S_setPolicyDelegate, 0);
					WebKit.objc_msgSend(notificationCenter, WebKit.S_removeObserver, delegate);
					
					WebKit.objc_msgSend(delegate, WebKit.S_release);
					if (OS.HIVIEW) OS.DisposeControl(webViewHandle);
					html = null;
					break;
				}
				case SWT.Hide: {
					/*
					* Bug on Safari. The web view cannot be obscured by other views above it.
					* This problem is specified in the apple documentation for HiWebViewCreate.
					* The workaround is to hook Hide and Show events on the browser's parents
					* and set its size to 0 in Hide and to restore its size in Show.
					*/
					CGRect bounds = new CGRect();
					bounds.x = bounds.y = -MIN_SIZE;
					bounds.width = bounds.height = MIN_SIZE;
					OS.HIViewSetFrame(webViewHandle, bounds);
					break;
				}
				case SWT.Show: {
					/*
					* Do not update size when it is not visible. Note that isVisible()
					* cannot be used because SWT.Show is sent before the widget is
					* actually visible. 
					*/
					Shell shell = getShell();
					Composite parent = Browser.this;
					while (parent != shell && (parent.getVisible() || parent == e.widget)) {
						parent = parent.getParent();
					}
					if (!(parent.getVisible() || parent == e.widget)) return;

					/*
					* Bug on Safari. The web view cannot be obscured by other views above it.
					* This problem is specified in the apple documentation for HiWebViewCreate.
					* The workaround is to hook Hide and Show events on the browser's parents
					* and set its size to 0 in Hide and to restore its size in Show.
					*/
					CGRect bounds = new CGRect();
					if (OS.HIVIEW) {
						OS.HIViewGetBounds(handle, bounds);
						int[] contentView = new int[1];
						OS.HIViewFindByID(OS.HIViewGetRoot(OS.GetControlOwner(handle)), OS.kHIViewWindowContentID(), contentView);
						OS.HIViewConvertRect(bounds, handle, contentView[0]);
					} else {
						OS.HIViewGetFrame(handle, bounds);
					}
					/* 
					* Bug in Safari.  For some reason, the web view will display incorrectly or
					* blank depending on its contents, if its size is set to a value smaller than
					* MIN_SIZE. It will not display properly even after the size is made larger.
					* The fix is to avoid setting sizes smaller than MIN_SIZE. 
					*/
					if (bounds.width <= MIN_SIZE) bounds.width = MIN_SIZE;
					if (bounds.height <= MIN_SIZE) bounds.height = MIN_SIZE;
					OS.HIViewSetFrame(webViewHandle, bounds);
					break;
				}
				case SWT.Resize: {
					/* Do not update size when it is not visible */
					if (!isVisible()) return;
					/*
					* Bug on Safari. Resizing the height of a Shell containing a Browser at
					* a fixed location causes the Browser to redraw at a wrong location.
					* The web view is a HIView container that internally hosts
					* a Cocoa NSView that uses a coordinates system with the origin at the
					* bottom left corner of a window instead of the coordinates system used
					* in Carbon that starts at the top left corner. The workaround is to
					* reposition the web view every time the Shell of the Browser is resized.
					*/
					CGRect bounds = new CGRect();
					if (OS.HIVIEW) {
						OS.HIViewGetBounds(handle, bounds);
						int[] contentView = new int[1];
						OS.HIViewFindByID(OS.HIViewGetRoot(OS.GetControlOwner(handle)), OS.kHIViewWindowContentID(), contentView);
						OS.HIViewConvertRect(bounds, handle, contentView[0]);
					} else {
						OS.HIViewGetFrame(handle, bounds);
					}
					/* 
					* Bug in Safari.  For some reason, the web view will display incorrectly or
					* blank depending on its contents, if its size is set to a value smaller than
					* MIN_SIZE. It will not display properly even after the size is made larger.
					* The fix is to avoid setting sizes smaller than MIN_SIZE. 
					*/
					if (bounds.width <= MIN_SIZE) bounds.width = MIN_SIZE;
					if (bounds.height <= MIN_SIZE) bounds.height = MIN_SIZE;
					if (e.widget == getShell()) {
						bounds.x++;
						/* Note that the bounds needs to change */
						OS.HIViewSetFrame(webViewHandle, bounds);
						bounds.x--;
					}
					OS.HIViewSetFrame(webViewHandle, bounds);
					break;
				}
			}
		}
	};
	addListener(SWT.Dispose, listener);
	addListener(SWT.Resize, listener);
	Shell shell = getShell();
	shell.addListener(SWT.Resize, listener);
	shell.addListener(SWT.Show, listener);
	shell.addListener(SWT.Hide, listener);
	Control c = this;
	do {
		c.addListener(SWT.Show, listener);
		c.addListener(SWT.Hide, listener);
		c = c.getParent();
	} while (c != shell);
	
	if (Callback3 == null) Callback3 = new Callback(this.getClass(), "eventProc3", 3); //$NON-NLS-1$
	int callback3Address = Callback3.getAddress();
	if (callback3Address == 0) SWT.error(SWT.ERROR_NO_MORE_CALLBACKS);

	int[] mask = new int[] {
		OS.kEventClassKeyboard, OS.kEventRawKeyDown,
		OS.kEventClassControl, OS.kEventControlDraw,
		OS.kEventClassTextInput, OS.kEventTextInputUnicodeForKeyEvent,
	};
	int controlTarget = OS.GetControlEventTarget(webViewHandle);
	OS.InstallEventHandler(controlTarget, callback3Address, mask.length / 2, mask, webViewHandle, null);

	if (Callback7 == null) Callback7 = new Callback(this.getClass(), "eventProc7", 7); //$NON-NLS-1$
	int callback7Address = Callback7.getAddress();
	if (callback7Address == 0) SWT.error(SWT.ERROR_NO_MORE_CALLBACKS);
	
	// delegate = [[WebResourceLoadDelegate alloc] init eventProc];
	delegate = WebKit.objc_msgSend(WebKit.C_WebKitDelegate, WebKit.S_alloc);
	delegate = WebKit.objc_msgSend(delegate, WebKit.S_initWithProc, callback7Address, webViewHandle);
				
	// [webView setFrameLoadDelegate:delegate];
	WebKit.objc_msgSend(webView, WebKit.S_setFrameLoadDelegate, delegate);
		
	// [webView setResourceLoadDelegate:delegate];
	WebKit.objc_msgSend(webView, WebKit.S_setResourceLoadDelegate, delegate);

	// [webView setUIDelegate:delegate];
	WebKit.objc_msgSend(webView, WebKit.S_setUIDelegate, delegate);
	
	/* register delegate for all notifications sent out from webview */
	WebKit.objc_msgSend(notificationCenter, WebKit.S_addObserver_selector_name_object, delegate, WebKit.S_handleNotification, 0, webView);
	
	// [webView setPolicyDelegate:delegate];
	WebKit.objc_msgSend(webView, WebKit.S_setPolicyDelegate, delegate);

	// [webView setDownloadDelegate:delegate];
	WebKit.objc_msgSend(webView, WebKit.S_setDownloadDelegate, delegate);
}

/**
 * Clears all session cookies from all current Browser instances.
 * 
 * @since 3.2
 */
public static void clearSessions () {
	int storage = WebKit.objc_msgSend (WebKit.C_NSHTTPCookieStorage, WebKit.S_sharedHTTPCookieStorage);
	int cookies = WebKit.objc_msgSend (storage, WebKit.S_cookies);
	int count = WebKit.objc_msgSend (cookies, WebKit.S_count);
	for (int i = 0; i < count; i++) {
		int cookie = WebKit.objc_msgSend (cookies, WebKit.S_objectAtIndex, i);
		boolean isSession = WebKit.objc_msgSend (cookie, WebKit.S_isSessionOnly) != 0;
		if (isSession) {
			WebKit.objc_msgSend (storage, WebKit.S_deleteCookie, cookie);
		}
	}
}

static int eventProc3(int nextHandler, int theEvent, int userData) {
	Widget widget = Display.getCurrent().findWidget(userData);
	if (widget instanceof Browser)
		return ((Browser)widget).handleCallback(nextHandler, theEvent);
	return OS.eventNotHandledErr;
}

static int eventProc7(int webview, int userData, int selector, int arg0, int arg1, int arg2, int arg3) {
	Widget widget = Display.getCurrent().findWidget(userData);
	if (widget instanceof Browser)
		return ((Browser)widget).handleCallback(selector, arg0, arg1, arg2, arg3);
	return 0;
}

/**	 
 * Adds the listener to the collection of listeners who will be
 * notified when the window hosting the receiver should be closed.
 * <p>
 * This notification occurs when a javascript command such as
 * <code>window.close</code> gets executed by a <code>Browser</code>.
 * </p>
 *
 * @param listener the listener which should be notified
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
 * Adds the listener to the collection of listeners who will be
 * notified when the current location has changed or is about to change.
 * <p>
 * This notification typically occurs when the application navigates
 * to a new location with {@link #setUrl(String)} or when the user
 * activates a hyperlink.
 * </p>
 *
 * @param listener the listener which should be notified
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
 * Adds the listener to the collection of listeners who will be
 * notified when a new window needs to be created.
 * <p>
 * This notification occurs when a javascript command such as
 * <code>window.open</code> gets executed by a <code>Browser</code>.
 * </p>
 *
 * @param listener the listener which should be notified
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
 * Adds the listener to the collection of listeners who will be
 * notified when a progress is made during the loading of the current 
 * URL or when the loading of the current URL has been completed.
 *
 * @param listener the listener which should be notified
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
 * Adds the listener to the collection of listeners who will be
 * notified when the status text is changed.
 * <p>
 * The status text is typically displayed in the status bar of
 * a browser application.
 * </p>
 *
 * @param listener the listener which should be notified
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
 * Adds the listener to the collection of listeners who will be
 * notified when the title of the current document is available
 * or has changed.
 *
 * @param listener the listener which should be notified
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
 * Adds the listener to the collection of listeners who will be
 * notified when a window hosting the receiver needs to be displayed
 * or hidden.
 *
 * @param listener the listener which should be notified
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
	html = null;
	int webView = WebKit.HIWebViewGetWebView(webViewHandle);
	return WebKit.objc_msgSend(webView, WebKit.S_goBack) != 0;
}

protected void checkSubclass () {
	String name = getClass().getName();
	int index = name.lastIndexOf('.');
	if (!name.substring(0, index + 1).equals(PACKAGE_PREFIX)) {
		SWT.error(SWT.ERROR_INVALID_SUBCLASS);
	}
}

/**
 * Execute the specified script.
 *
 * <p>
 * Execute a script containing javascript commands in the context of the current document. 
 * 
 * @param script the script with javascript commands
 *  
 * @return <code>true</code> if the operation was successful and <code>false</code> otherwise
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the script is null</li>
 * </ul>
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS when called from the wrong thread</li>
 *    <li>ERROR_WIDGET_DISPOSED when the widget has been disposed</li>
 * </ul>
 *
 * @since 3.1
 */
public boolean execute(String script) {
	checkWidget();
	if (script == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);

	int length = script.length();
	char[] buffer = new char[length];
	script.getChars(0, length, buffer, 0);
	int string = OS.CFStringCreateWithCharacters(0, buffer, length);

	int webView = WebKit.HIWebViewGetWebView(webViewHandle);
	int value = WebKit.objc_msgSend(webView, WebKit.S_stringByEvaluatingJavaScriptFromString, string);
	OS.CFRelease(string);
	return value != 0;
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
	html = null;
	int webView = WebKit.HIWebViewGetWebView(webViewHandle);
	return WebKit.objc_msgSend(webView, WebKit.S_goForward) != 0;
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
	return url;
}

int handleCallback(int nextHandler, int theEvent) {
	int eventKind = OS.GetEventKind(theEvent);
	switch (eventKind) {
		case OS.kEventControlDraw: {          
			/*
			* Bug on Safari. The web view cannot be obscured by other views above it.
			* This problem is specified in the apple documentation for HiWebViewCreate.
			* The workaround is to don't draw the web view when it is not visible.
			*/
			if (!isVisible ()) return OS.noErr;
            
			/*
			* GOOGLE: HACK - SWT does not properly push repaint events back to
			* WebKit, so we actually naively tell webkit to repaint it's entire
			* client area.
			*/
			setNeedsDisplay(true);            
			
			break;
		}
		case OS.kEventRawKeyDown: {
			/*
			* Bug in Safari. The WebView blocks the propagation of certain Carbon events
			* such as kEventRawKeyDown. On the Mac, Carbon events propagate from the
			* Focus Target Handler to the Control Target Handler, Window Target and finally
			* the Application Target Handler. It is assumed that WebView hooks its events
			* on the Window Target and does not pass kEventRawKeyDown to the next handler.
			* Since kEventRawKeyDown events never make it to the Application Target Handler,
			* the Application Target Handler never gets to emit kEventTextInputUnicodeForKeyEvent
			* used by SWT to send a SWT.KeyDown event.
			* The workaround is to hook kEventRawKeyDown on the Control Target Handler which gets
			* called before the WebView hook on the Window Target Handler. Then, forward this event
			* directly to the Application Target Handler. Note that if in certain conditions Safari
			* does not block the kEventRawKeyDown, then multiple kEventTextInputUnicodeForKeyEvent
			* events might be generated as a result of this workaround.
			*/
			//TEMPORARY CODE
//			doit = false;
//			OS.SendEventToEventTarget(theEvent, OS.GetApplicationEventTarget());
//			if (!doit) return OS.noErr;
			break;
		}
		case OS.kEventTextInputUnicodeForKeyEvent: {
			/*
			* Note.  This event is received from the Window Target therefore after it was received
			* by the Focus Target. The SWT.KeyDown event is sent by SWT on the Focus Target. If it
			* is received here, then the SWT.KeyDown doit flag must have been left to the value
			* true.  For package visibility reasons we cannot access the doit flag directly.
			* 
			* Sequence of events when the user presses a key down
			* 
			* .Control Target - kEventRawKeyDown
			* 	.forward to ApplicationEventTarget
			* 		.Focus Target kEventTextInputUnicodeForKeyEvent - SWT emits SWT.KeyDown - 
			* 			blocks further propagation if doit false. Browser does not know directly about
			* 			the doit flag value.
			* 			.Window Target kEventTextInputUnicodeForKeyEvent - if received, Browser knows 
			* 			SWT.KeyDown is not blocked and event should be sent to WebKit
			*  Return from Control Target - kEventRawKeyDown: let the event go to WebKit if doit true 
			*  (eventNotHandledErr) or stop it (noErr).
			*/
			//TEMPORARY CODE
//			doit = true;
			break;
		}
	}
	return OS.eventNotHandledErr;
}

/* Here we dispatch all WebView upcalls. */
int handleCallback(int selector, int arg0, int arg1, int arg2, int arg3) {
	int ret = 0;
	// for meaning of selector see WebKitDelegate methods in webkit.c
	switch (selector) {
		case 1: didFailProvisionalLoadWithError(arg0, arg1); break;
		case 2: didFinishLoadForFrame(arg0); break;
		case 3: didReceiveTitle(arg0, arg1); break;
		case 4: didStartProvisionalLoadForFrame(arg0); break;
		case 5: didFinishLoadingFromDataSource(arg0, arg1); break;
		case 6: didFailLoadingWithError(arg0, arg1, arg2); break;
		case 7: ret = identifierForInitialRequest(arg0, arg1); break;
		case 8: ret = willSendRequest(arg0, arg1, arg2, arg3); break;
		case 9: handleNotification(arg0); break;
		case 10: didCommitLoadForFrame(arg0); break;
		case 11: ret = createWebViewWithRequest(arg0); break;
		case 12: webViewShow(arg0); break;
		case 13: setFrame(arg0); break;
		case 14: webViewClose(); break;
		case 15: ret = contextMenuItemsForElement(arg0, arg1); break;
		case 16: setStatusBarVisible(arg0); break;
		case 17: setResizable(arg0); break;
		case 18: setToolbarsVisible(arg0); break;
		case 19: decidePolicyForMIMEType(arg0, arg1, arg2, arg3); break;
		case 20: decidePolicyForNavigationAction(arg0, arg1, arg2, arg3); break;
		case 21: decidePolicyForNewWindowAction(arg0, arg1, arg2, arg3); break;
		case 22: unableToImplementPolicyWithError(arg0, arg1); break;
		case 23: setStatusText(arg0); break;
		case 24: webViewFocus(); break;
		case 25: webViewUnfocus(); break;
		case 26: runJavaScriptAlertPanelWithMessage(arg0); break;
		case 27: ret = runJavaScriptConfirmPanelWithMessage(arg0); break;
		case 28: runOpenPanelForFileButtonWithResultListener(arg0); break;
		case 29: decideDestinationWithSuggestedFilename(arg0, arg1); break;
        // GOOGLE
		case 99: fireWindowScriptObjectListeners(arg0); break;
	}
	return ret;
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
	int webView = WebKit.HIWebViewGetWebView(webViewHandle);
	return WebKit.objc_msgSend(webView, WebKit.S_canGoBack) != 0;
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
	int webView = WebKit.HIWebViewGetWebView(webViewHandle);
	return WebKit.objc_msgSend(webView, WebKit.S_canGoForward) != 0;
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
	int webView = WebKit.HIWebViewGetWebView(webViewHandle);
	WebKit.objc_msgSend(webView, WebKit.S_reload, 0);
}

/**	 
 * Removes the listener from the collection of listeners who will
 * be notified when the window hosting the receiver should be closed.
 *
 * @param listener the listener which should no longer be notified
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
 * Removes the listener from the collection of listeners who will
 * be notified when the current location is changed or about to be changed.
 *
 * @param listener the listener which should no longer be notified
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
 * Removes the listener from the collection of listeners who will
 * be notified when a new window needs to be created.
 *
 * @param listener the listener which should no longer be notified
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
 * Removes the listener from the collection of listeners who will
 * be notified when a progress is made during the loading of the current 
 * URL or when the loading of the current URL has been completed.
 *
 * @param listener the listener which should no longer be notified
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
 * Removes the listener from the collection of listeners who will
 * be notified when the status text is changed.
 *
 * @param listener the listener which should no longer be notified
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
 * Removes the listener from the collection of listeners who will
 * be notified when the title of the current document is available
 * or has changed.
 *
 * @param listener the listener which should no longer be notified
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
 * Removes the listener from the collection of listeners who will
 * be notified when a window hosting the receiver needs to be displayed
 * or hidden.
 *
 * @param listener the listener which should no longer be notified
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
 * <p>
 * The html parameter is Unicode encoded since it is a java <code>String</code>.
 * As a result, the HTML meta tag charset should not be set. The charset is implied
 * by the <code>String</code> itself.
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
	* Bug in Safari.  The web view segment faults in some circunstances
	* when the text changes during the location changing callback.  The
	* fix is to defer the work until the callback is done. 
	*/
	if (changingLocation) {
		this.html = html;
	} else {
		_setText(html);
	}
	return true;
}
	
void _setText(String html) {	
	int length = html.length();
	char[] buffer = new char[length];
	html.getChars(0, length, buffer, 0);
	int string = OS.CFStringCreateWithCharacters(0, buffer, length);

	String baseURL = "about:blank"; //$NON-NLS-1$
	length = baseURL.length();
	buffer = new char[length];
	baseURL.getChars(0, length, buffer, 0);
	int URLString = OS.CFStringCreateWithCharacters(0, buffer, length);
	
	/*
	* Note.  URLWithString uses autorelease.  The resulting URL
	* does not need to be released.
	* URL = [NSURL URLWithString:(NSString *)URLString]
	*/	
	int URL = WebKit.objc_msgSend(WebKit.C_NSURL, WebKit.S_URLWithString, URLString);
	OS.CFRelease(URLString);

	int webView = WebKit.HIWebViewGetWebView(webViewHandle);
	
	//mainFrame = [webView mainFrame];
	int mainFrame = WebKit.objc_msgSend(webView, WebKit.S_mainFrame);
	
	//[mainFrame loadHTMLString:(NSString *) string baseURL:(NSURL *)URL];
	WebKit.objc_msgSend(mainFrame, WebKit.S_loadHTMLStringbaseURL, string, URL);
	OS.CFRelease(string);
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

	html = null;

	StringBuffer buffer = new StringBuffer();
	if (url.indexOf('/') == 0) buffer.append("file://"); //$NON-NLS-1$  //$NON-NLS-2$
	else if (url.indexOf(':') == -1) buffer.append("http://");	 //$NON-NLS-1$
	for (int i = 0; i < url.length(); i++) {
		char c = url.charAt(i);
		if (c == ' ') buffer.append("%20"); //$NON-NLS-1$  //$NON-NLS-2$
		else buffer.append(c);
	}
	
	int length = buffer.length();
	char[] chars = new char[length];
	buffer.getChars(0, length, chars, 0);
	int sHandle = OS.CFStringCreateWithCharacters(0, chars, length);

	/*
	* Note.  URLWithString uses autorelease.  The resulting URL
	* does not need to be released.
	* inURL = [NSURL URLWithString:(NSString *)sHandle]
	*/	
	int inURL= WebKit.objc_msgSend(WebKit.C_NSURL, WebKit.S_URLWithString, sHandle);
	OS.CFRelease(sHandle);
		
	//request = [NSURLRequest requestWithURL:(NSURL*)inURL];
	int request= WebKit.objc_msgSend(WebKit.C_NSURLRequest, WebKit.S_requestWithURL, inURL);
	
	int webView = WebKit.HIWebViewGetWebView(webViewHandle);
	
	//mainFrame = [webView mainFrame];
	int mainFrame= WebKit.objc_msgSend(webView, WebKit.S_mainFrame);

	//[mainFrame loadRequest:request];
	WebKit.objc_msgSend(mainFrame, WebKit.S_loadRequest, request);

	return true;
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
	html = null;
	int webView = WebKit.HIWebViewGetWebView(webViewHandle);
	WebKit.objc_msgSend(webView, WebKit.S_stopLoading, 0);
}

/* WebFrameLoadDelegate */
  
void didFailProvisionalLoadWithError(int error, int frame) {
	int webView = WebKit.HIWebViewGetWebView(webViewHandle);
	if (frame == WebKit.objc_msgSend(webView, WebKit.S_mainFrame)) {
		/*
		* Feature on Safari.  The identifier is used here as a marker for the events 
		* related to the top frame and the URL changes related to that top frame as 
		* they should appear on the location bar of a browser.  It is expected to reset
		* the identifier to 0 when the event didFinishLoadingFromDataSource related to 
		* the identifierForInitialRequest event is received.  Howeever, Safari fires
		* the didFinishLoadingFromDataSource event before the entire content of the
		* top frame is loaded.  It is possible to receive multiple willSendRequest 
		* events in this interval, causing the Browser widget to send unwanted
		* Location.changing events.  For this reason, the identifier is reset to 0
		* when the top frame has either finished loading (didFinishLoadForFrame
		* event) or failed (didFailProvisionalLoadWithError).
		*/
		identifier = 0;
	}
}

void didFinishLoadForFrame(int frame) {
	int webView = WebKit.HIWebViewGetWebView(webViewHandle);
	if (frame == WebKit.objc_msgSend(webView, WebKit.S_mainFrame)) {
		final Display display= getDisplay();
		final ProgressEvent progress = new ProgressEvent(this);
		progress.display = getDisplay();
		progress.widget = this;
		progress.current = MAX_PROGRESS;
		progress.total = MAX_PROGRESS;
		for (int i = 0; i < progressListeners.length; i++) {
			final ProgressListener listener = progressListeners[i];
			/*
			* Note on WebKit.  Running the event loop from a Browser
			* delegate callback breaks the WebKit (stop loading or
			* crash).  The widget ProgressBar currently touches the
			* event loop every time the method setSelection is called.  
			* The workaround is to invoke Display.asyncexec so that
			* the Browser does not crash when the user updates the 
			* selection of the ProgressBar.
			*/
			display.asyncExec(
				new Runnable() {
					public void run() {
						if (!display.isDisposed() && !isDisposed())
							listener.completed(progress);
					}
				}
			);
		}
		/*
		* Feature on Safari.  The identifier is used here as a marker for the events 
		* related to the top frame and the URL changes related to that top frame as 
		* they should appear on the location bar of a browser.  It is expected to reset
		* the identifier to 0 when the event didFinishLoadingFromDataSource related to 
		* the identifierForInitialRequest event is received.  Howeever, Safari fires
		* the didFinishLoadingFromDataSource event before the entire content of the
		* top frame is loaded.  It is possible to receive multiple willSendRequest 
		* events in this interval, causing the Browser widget to send unwanted
		* Location.changing events.  For this reason, the identifier is reset to 0
		* when the top frame has either finished loading (didFinishLoadForFrame
		* event) or failed (didFailProvisionalLoadWithError).
		*/
		identifier = 0;
	}
}

void didReceiveTitle(int title, int frame) {
	int webView = WebKit.HIWebViewGetWebView(webViewHandle);
	if (frame == WebKit.objc_msgSend(webView, WebKit.S_mainFrame)) {
		int length = OS.CFStringGetLength(title);
		char[] buffer = new char[length];
		CFRange range = new CFRange();
		range.length = length;
		OS.CFStringGetCharacters(title, range, buffer);
		String newTitle = new String(buffer);
		TitleEvent newEvent = new TitleEvent(Browser.this);
		newEvent.display = getDisplay();
		newEvent.widget = this;
		newEvent.title = newTitle;
		for (int i = 0; i < titleListeners.length; i++)
			titleListeners[i].changed(newEvent);
	}
}

void didStartProvisionalLoadForFrame(int frame) {
	/* 
	* This code is intentionally commented.  WebFrameLoadDelegate:didStartProvisionalLoadForFrame is
	* called before WebResourceLoadDelegate:willSendRequest and
	* WebFrameLoadDelegate:didCommitLoadForFrame.  The resource count is reset when didCommitLoadForFrame
	* is received for the top frame.
	*/
//	int webView = WebKit.HIWebViewGetWebView(webViewHandle);
//	if (frame == WebKit.objc_msgSend(webView, WebKit.S_mainFrame)) {
//		/* reset resource status variables */
//		resourceCount= 0;
//	}
}

void didCommitLoadForFrame(int frame) {
	int webView = WebKit.HIWebViewGetWebView(webViewHandle);
	//id url= [[[[frame provisionalDataSource] request] URL] absoluteString];
	int dataSource = WebKit.objc_msgSend(frame, WebKit.S_dataSource);
	int request = WebKit.objc_msgSend(dataSource, WebKit.S_request);
	int url = WebKit.objc_msgSend(request, WebKit.S_URL);
	int s = WebKit.objc_msgSend(url, WebKit.S_absoluteString);	
	int length = OS.CFStringGetLength(s);
	if (length == 0) return;
	char[] buffer = new char[length];
	CFRange range = new CFRange();
	range.length = length;
	OS.CFStringGetCharacters(s, range, buffer);
	String url2 = new String(buffer);
	final Display display = getDisplay();

	boolean top = frame == WebKit.objc_msgSend(webView, WebKit.S_mainFrame);
	if (top) {
		/* reset resource status variables */
		resourceCount = 0;		
		this.url = url2;
		
		final ProgressEvent progress = new ProgressEvent(this);
		progress.display = display;
		progress.widget = this;
		progress.current = 1;
		progress.total = MAX_PROGRESS;
		for (int i = 0; i < progressListeners.length; i++) {
			final ProgressListener listener = progressListeners[i];
			/*
			* Note on WebKit.  Running the event loop from a Browser
			* delegate callback breaks the WebKit (stop loading or
			* crash).  The widget ProgressBar currently touches the
			* event loop every time the method setSelection is called.  
			* The workaround is to invoke Display.asyncexec so that
			* the Browser does not crash when the user updates the 
			* selection of the ProgressBar.
			*/
			display.asyncExec(
				new Runnable() {
					public void run() {
						if (!display.isDisposed() && !isDisposed())
							listener.changed(progress);
					}
				}
			);
		}
		
		StatusTextEvent statusText = new StatusTextEvent(this);
		statusText.display = display;
		statusText.widget = this;
		statusText.text = url2;
		for (int i = 0; i < statusTextListeners.length; i++)
			statusTextListeners[i].changed(statusText);
	}
	LocationEvent location = new LocationEvent(Browser.this);
	location.display = display;
	location.widget = this;
	location.location = url2;
	location.top = top;
	for (int i = 0; i < locationListeners.length; i++)
		locationListeners[i].changed(location);
}

/* WebResourceLoadDelegate */

void didFinishLoadingFromDataSource(int identifier, int dataSource) {
	/*
	* Feature on Safari.  The identifier is used here as a marker for the events 
	* related to the top frame and the URL changes related to that top frame as 
	* they should appear on the location bar of a browser.  It is expected to reset
	* the identifier to 0 when the event didFinishLoadingFromDataSource related to 
	* the identifierForInitialRequest event is received.  Howeever, Safari fires
	* the didFinishLoadingFromDataSource event before the entire content of the
	* top frame is loaded.  It is possible to receive multiple willSendRequest 
	* events in this interval, causing the Browser widget to send unwanted
	* Location.changing events.  For this reason, the identifier is reset to 0
	* when the top frame has either finished loading (didFinishLoadForFrame
	* event) or failed (didFailProvisionalLoadWithError).
	*/
	// this code is intentionally commented
	//if (this.identifier == identifier) this.identifier = 0;
}

void didFailLoadingWithError(int identifier, int error, int dataSource) {
	/*
	* Feature on Safari.  The identifier is used here as a marker for the events 
	* related to the top frame and the URL changes related to that top frame as 
	* they should appear on the location bar of a browser.  It is expected to reset
	* the identifier to 0 when the event didFinishLoadingFromDataSource related to 
	* the identifierForInitialRequest event is received.  Howeever, Safari fires
	* the didFinishLoadingFromDataSource event before the entire content of the
	* top frame is loaded.  It is possible to receive multiple willSendRequest 
	* events in this interval, causing the Browser widget to send unwanted
	* Location.changing events.  For this reason, the identifier is reset to 0
	* when the top frame has either finished loading (didFinishLoadForFrame
	* event) or failed (didFailProvisionalLoadWithError).
	*/
	// this code is intentionally commented
	//if (this.identifier == identifier) this.identifier = 0;
}

int identifierForInitialRequest(int request, int dataSource) {
	final Display display = getDisplay();
	final ProgressEvent progress = new ProgressEvent(this);
	progress.display = display;
	progress.widget = this;
	progress.current = resourceCount;
	progress.total = Math.max(resourceCount, MAX_PROGRESS);
	for (int i = 0; i < progressListeners.length; i++) {
		final ProgressListener listener = progressListeners[i];
		/*
		* Note on WebKit.  Running the event loop from a Browser
		* delegate callback breaks the WebKit (stop loading or
		* crash).  The widget ProgressBar currently touches the
		* event loop every time the method setSelection is called.  
		* The workaround is to invoke Display.asyncexec so that
		* the Browser does not crash when the user updates the 
		* selection of the ProgressBar.
		*/
		display.asyncExec(
			new Runnable() {
				public void run() {
					if (!display.isDisposed() && !isDisposed())
						listener.changed(progress);
				}
			}
		);
	}

	/*
	* Note.  numberWithInt uses autorelease.  The resulting object
	* does not need to be released.
	* identifier = [NSNumber numberWithInt: resourceCount++]
	*/	
	int identifier = WebKit.objc_msgSend(WebKit.C_NSNumber, WebKit.S_numberWithInt, resourceCount++);
		
	if (this.identifier == 0) {
		int webView = WebKit.HIWebViewGetWebView(webViewHandle);
		int frame = WebKit.objc_msgSend(dataSource, WebKit.S_webFrame);
		if (frame == WebKit.objc_msgSend(webView, WebKit.S_mainFrame)) this.identifier = identifier;
	}
	return identifier;
		
}

int willSendRequest(int identifier, int request, int redirectResponse, int dataSource) {
	return request;
}

/* handleNotification */

void handleNotification(int notification) {	
}

/* UIDelegate */
int createWebViewWithRequest(int request) {
	WindowEvent newEvent = new WindowEvent(Browser.this);
	newEvent.display = getDisplay();
	newEvent.widget = this;
	newEvent.required = true;
	if (openWindowListeners != null) {
		for (int i = 0; i < openWindowListeners.length; i++)
			openWindowListeners[i].open(newEvent);
	}
	int webView = 0;
	Browser browser = newEvent.browser;
	if (browser != null && !browser.isDisposed()) {
		webView = WebKit.HIWebViewGetWebView(browser.webViewHandle);
		
		if (request != 0) {
			//mainFrame = [webView mainFrame];
			int mainFrame= WebKit.objc_msgSend(webView, WebKit.S_mainFrame);

			//[mainFrame loadRequest:request];
			WebKit.objc_msgSend(mainFrame, WebKit.S_loadRequest, request);
		}
	}
	return webView;
}

void webViewShow(int sender) {
	/*
	* Feature on WebKit.  The Safari WebKit expects the application
	* to create a new Window using the Objective C Cocoa API in response
	* to UIDelegate.createWebViewWithRequest. The application is then
	* expected to use Objective C Cocoa API to make this window visible
	* when receiving the UIDelegate.webViewShow message.  For some reason,
	* a window created with the Carbon API hosting the new browser instance
	* does not redraw until it has been resized.  The fix is to increase the
	* size of the Shell and restore it to its initial size.
	*/
	Shell parent = getShell();
	Point pt = parent.getSize();
	parent.setSize(pt.x+1, pt.y);
	parent.setSize(pt.x, pt.y);
	WindowEvent newEvent = new WindowEvent(this);
	newEvent.display = getDisplay();
	newEvent.widget = this;
	if (location != null) newEvent.location = location;
	if (size != null) newEvent.size = size;
	/*
	* Feature in Safari.  Safari's tool bar contains
	* the address bar.  The address bar is displayed
	* if the tool bar is displayed. There is no separate
	* notification for the address bar.
	* Feature in Safari.  The menu bar is always
	* displayed. There is no notification to hide
	* the menu bar.
	*/
	newEvent.addressBar = toolBar;
	newEvent.menuBar = true;
	newEvent.statusBar = statusBar;
	newEvent.toolBar = toolBar;
	for (int i = 0; i < visibilityWindowListeners.length; i++)
		visibilityWindowListeners[i].show(newEvent);
	location = null;
	size = null;
}

void setFrame(int frame) {
	float[] dest = new float[4];
	OS.memcpy(dest, frame, 16);
	/* convert to SWT system coordinates */
	Rectangle bounds = getDisplay().getBounds();
	location = new Point((int)dest[0], bounds.height - (int)dest[1] - (int)dest[3]);
	size = new Point((int)dest[2], (int)dest[3]);
}

void webViewFocus() {
}

void webViewUnfocus() {
}

void runJavaScriptAlertPanelWithMessage(int message) {
	int length = OS.CFStringGetLength(message);
	char[] buffer = new char[length];
	CFRange range = new CFRange();
	range.length = length;
	OS.CFStringGetCharacters(message, range, buffer);
	String text = new String(buffer);

	MessageBox messageBox = new MessageBox(getShell(), SWT.OK | SWT.ICON_WARNING);
	messageBox.setText("Javascript");	//$NON-NLS-1$
	messageBox.setMessage(text);
	messageBox.open();
}

int runJavaScriptConfirmPanelWithMessage(int message) {
	int length = OS.CFStringGetLength(message);
	char[] buffer = new char[length];
	CFRange range = new CFRange();
	range.length = length;
	OS.CFStringGetCharacters(message, range, buffer);
	String text = new String(buffer);

	MessageBox messageBox = new MessageBox(getShell(), SWT.OK | SWT.CANCEL | SWT.ICON_QUESTION);
	messageBox.setText("Javascript");	//$NON-NLS-1$
	messageBox.setMessage(text);
	return messageBox.open() == SWT.OK ? 1 : 0;
}

void runOpenPanelForFileButtonWithResultListener(int resultListener) {
	FileDialog dialog = new FileDialog(getShell(), SWT.NONE);
	String result = dialog.open();
	if (result == null) {
		WebKit.objc_msgSend(resultListener, WebKit.S_cancel);
		return;
	}
	int length = result.length();
	char[] buffer = new char[length];
	result.getChars(0, length, buffer, 0);
	int filename = OS.CFStringCreateWithCharacters(0, buffer, length);
	WebKit.objc_msgSend(resultListener, WebKit.S_chooseFilename, filename);
	OS.CFRelease(filename);
}
void webViewClose() {
	Shell parent = getShell();
	WindowEvent newEvent = new WindowEvent(this);
	newEvent.display = getDisplay();
	newEvent.widget = this;
	for (int i = 0; i < closeWindowListeners.length; i++)
		closeWindowListeners[i].close(newEvent);	
	dispose();
	if (parent.isDisposed()) return;
	/*
	* Feature on WebKit.  The Safari WebKit expects the application
	* to create a new Window using the Objective C Cocoa API in response
	* to UIDelegate.createWebViewWithRequest. The application is then
	* expected to use Objective C Cocoa API to make this window visible
	* when receiving the UIDelegate.webViewShow message.  For some reason,
	* a window created with the Carbon API hosting the new browser instance
	* does not redraw until it has been resized.  The fix is to increase the
	* size of the Shell and restore it to its initial size.
	*/
	Point pt = parent.getSize();
	parent.setSize(pt.x+1, pt.y);
	parent.setSize(pt.x, pt.y);
}

int contextMenuItemsForElement(int element, int defaultMenuItems) {
	org.eclipse.swt.internal.carbon.Point pt = new org.eclipse.swt.internal.carbon.Point();
	OS.GetGlobalMouse(pt);
	Event event = new Event();
	event.x = pt.h;
	event.y = pt.v;
	notifyListeners(SWT.MenuDetect, event);
	Menu menu = getMenu();
	if (!event.doit) return 0;
	if (menu != null && !menu.isDisposed()) {
		if (event.x != pt.h || event.y != pt.v) {
			menu.setLocation(event.x, event.y);
		}
		menu.setVisible(true);
		return 0;
	}
	return defaultMenuItems;
}

void setStatusBarVisible(int visible) {
	/* Note.  Webkit only emits the notification when the status bar should be hidden. */
	statusBar = visible != 0;
}

void setStatusText(int text) {
	int length = OS.CFStringGetLength(text);
	if (length == 0) return;
	char[] buffer = new char[length];
	CFRange range = new CFRange();
	range.length = length;
	OS.CFStringGetCharacters(text, range, buffer);

	StatusTextEvent statusText = new StatusTextEvent(this);
	statusText.display = getDisplay();
	statusText.widget = this;
	statusText.text = new String(buffer);
	for (int i = 0; i < statusTextListeners.length; i++)
		statusTextListeners[i].changed(statusText);
}

void setResizable(int visible) {
}

void setToolbarsVisible(int visible) {
	/* Note.  Webkit only emits the notification when the tool bar should be hidden. */
	toolBar = visible != 0;
}

/* PolicyDelegate */

void decidePolicyForMIMEType(int type, int request, int frame, int listener) {
	boolean canShow = WebKit.objc_msgSend(WebKit.C_WebView, WebKit.S_canShowMIMEType, type) != 0;
	WebKit.objc_msgSend(listener, canShow ? WebKit.S_use : WebKit.S_download);
}

void decidePolicyForNavigationAction(int actionInformation, int request, int frame, int listener) {
	int url = WebKit.objc_msgSend(request, WebKit.S_URL);
	if (url == 0) {
		/* indicates that a URL with an invalid format was specified */
		WebKit.objc_msgSend(listener, WebKit.S_ignore);
		return;
	}
	int s = WebKit.objc_msgSend(url, WebKit.S_absoluteString);
	int length = OS.CFStringGetLength(s);
	char[] buffer = new char[length];
	CFRange range = new CFRange();
	range.length = length;
	OS.CFStringGetCharacters(s, range, buffer);
	String url2 = new String(buffer);

	LocationEvent newEvent = new LocationEvent(this);
	newEvent.display = getDisplay();
	newEvent.widget = this;
	newEvent.location = url2;
	newEvent.doit = true;
	if (locationListeners != null) {
		changingLocation = true;
		for (int i = 0; i < locationListeners.length; i++) 
			locationListeners[i].changing(newEvent);
		changingLocation = false;
	}

	WebKit.objc_msgSend(listener, newEvent.doit ? WebKit.S_use : WebKit.S_ignore);

	if (html != null && !isDisposed()) {
		String html = this.html;
		this.html = null;
		_setText(html);
	}
}

void decidePolicyForNewWindowAction(int actionInformation, int request, int frameName, int listener) {
	WebKit.objc_msgSend(listener, WebKit.S_use);
}

void unableToImplementPolicyWithError(int error, int frame) {
}

/* WebDownload */

void decideDestinationWithSuggestedFilename (int download, int filename) {
	int length = OS.CFStringGetLength(filename);
	char[] buffer = new char[length];
	CFRange range = new CFRange();
	range.length = length;
	OS.CFStringGetCharacters(filename, range, buffer);
	String name = new String(buffer);
	FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);
	dialog.setText(SWT.getMessage ("SWT_FileDownload")); //$NON-NLS-1$
	dialog.setFileName(name);
	String path = dialog.open();
	if (path == null) {
		/* cancel pressed */
		WebKit.objc_msgSend(download, WebKit.S_release);
		return;
	}
	length = path.length();
	char[] chars = new char[length];
	path.getChars(0, length, chars, 0);
	int result = OS.CFStringCreateWithCharacters(0, chars, length);
	WebKit.objc_msgSend(download, WebKit.S_setDestinationAllowOverwrite, result, 1);
	OS.CFRelease(result);
}

// GOOGLE: we need a notification when the window object is available so we can
// setup our own hooks before any JavaScript runs.
public interface WindowScriptObjectListener {
	public void windowScriptObjectAvailable(int windowScriptObject);
}

public void addWindowScriptObjectListener(WindowScriptObjectListener listener) {
	checkWidget();
	if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	WindowScriptObjectListener[] newWindowScriptObjectListeners = new WindowScriptObjectListener[windowScriptObjectListeners.length + 1];
	System.arraycopy(windowScriptObjectListeners, 0, newWindowScriptObjectListeners, 0, windowScriptObjectListeners.length);
	windowScriptObjectListeners = newWindowScriptObjectListeners;
	windowScriptObjectListeners[windowScriptObjectListeners.length - 1] = listener;
}

public void removeWindowScriptObjectListener(WindowScriptObjectListener listener) {
	checkWidget();
	if (listener == null) SWT.error(SWT.ERROR_NULL_ARGUMENT);
	if (windowScriptObjectListeners.length == 0) return;
	int index = -1;
	for (int i = 0; i < windowScriptObjectListeners.length; i++) {
		if (listener == windowScriptObjectListeners[i]) {
			index = i;
			break;
		}
	}
	if (index == -1) return;
	if (windowScriptObjectListeners.length == 1) {
		windowScriptObjectListeners = new WindowScriptObjectListener[0];
		return;
	}
	WindowScriptObjectListener[] newWindowScriptObjectListeners = new WindowScriptObjectListener[windowScriptObjectListeners.length - 1];
	System.arraycopy(windowScriptObjectListeners, 0, newWindowScriptObjectListeners, 0, index);
	System.arraycopy(windowScriptObjectListeners, index + 1, newWindowScriptObjectListeners, index, windowScriptObjectListeners.length - index - 1);
	windowScriptObjectListeners = newWindowScriptObjectListeners;
}

void fireWindowScriptObjectListeners(int windowScriptObject) {
	for (int i = 0; i < windowScriptObjectListeners.length; i++) {
		windowScriptObjectListeners[i].windowScriptObjectAvailable(windowScriptObject);
	}
}

public static void setWebInspectorEnabled(boolean isEnabled) {
	char[] name = "WebKitDeveloperExtras".toCharArray();
	int clsId = WebKit.objc_getClass("NSUserDefaults");
	if (clsId == 0) return;
	int sel_sud = WebKit.sel_registerName("standardUserDefaults");
	if (sel_sud == 0) return;
	int objId = WebKit.objc_msgSend(clsId, sel_sud);
	if (objId == 0) return;
	int sel_sbfk = WebKit.sel_registerName("setBool:forKey:");
	if (sel_sbfk == 0) return;
	int cfName = OS.CFStringCreateWithCharacters(0,name,name.length);
	if (cfName == 0) return;
	try {
		WebKit.objc_msgSend(objId, sel_sbfk, isEnabled ? 1 : 0, cfName);
	} finally {
		OS.CFRelease(cfName);
	}
}

public void setUserAgentApplicationName(String userAgent) {
	char[] chars = userAgent.toCharArray();
	int webView = WebKit.HIWebViewGetWebView(webViewHandle);
	if (webView == 0) return;
	int sel = WebKit.sel_registerName("setApplicationNameForUserAgent:");
	if (sel == 0) return;
	int userAgentStr = OS.CFStringCreateWithCharacters(0, chars, chars.length);
	if (userAgentStr == 0) return;
	try {
		WebKit.objc_msgSend(webView, sel, userAgentStr);
	} finally {
		OS.CFRelease(userAgentStr);
	}
}

public void setNeedsDisplay(boolean value) {
	int webView = WebKit.HIWebViewGetWebView(webViewHandle);
	if (webView == 0) return;
	int sel = WebKit.sel_registerName("setNeedsDisplay:");
	if (sel == 0) return;
	WebKit.objc_msgSend(webView, sel, (value)?1:0);
}

WindowScriptObjectListener[] windowScriptObjectListeners = new WindowScriptObjectListener[0];
// end GOOGLE
}
