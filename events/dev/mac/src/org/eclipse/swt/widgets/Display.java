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
package org.eclipse.swt.widgets;


import org.eclipse.swt.internal.*;
import org.eclipse.swt.internal.carbon.CFRange;
import org.eclipse.swt.internal.carbon.OS;
import org.eclipse.swt.internal.carbon.CGPoint;
import org.eclipse.swt.internal.carbon.CGRect;
import org.eclipse.swt.internal.carbon.GDevice;
import org.eclipse.swt.internal.carbon.HICommand;
import org.eclipse.swt.internal.carbon.Rect;
import org.eclipse.swt.internal.carbon.RGBColor;

import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;

// GOOGLE: special hacks to allow Browser to function normally
import org.eclipse.swt.browser.Browser;

/**
 * Instances of this class are responsible for managing the
 * connection between SWT and the underlying operating
 * system. Their most important function is to implement
 * the SWT event loop in terms of the platform event model.
 * They also provide various methods for accessing information
 * about the operating system, and have overall control over
 * the operating system resources which SWT allocates.
 * <p>
 * Applications which are built with SWT will <em>almost always</em>
 * require only a single display. In particular, some platforms
 * which SWT supports will not allow more than one <em>active</em>
 * display. In other words, some platforms do not support
 * creating a new display if one already exists that has not been
 * sent the <code>dispose()</code> message.
 * <p>
 * In SWT, the thread which creates a <code>Display</code>
 * instance is distinguished as the <em>user-interface thread</em>
 * for that display.
 * </p>
 * The user-interface thread for a particular display has the
 * following special attributes:
 * <ul>
 * <li>
 * The event loop for that display must be run from the thread.
 * </li>
 * <li>
 * Some SWT API methods (notably, most of the public methods in
 * <code>Widget</code> and its subclasses), may only be called
 * from the thread. (To support multi-threaded user-interface
 * applications, class <code>Display</code> provides inter-thread
 * communication methods which allow threads other than the 
 * user-interface thread to request that it perform operations
 * on their behalf.)
 * </li>
 * <li>
 * The thread is not allowed to construct other 
 * <code>Display</code>s until that display has been disposed.
 * (Note that, this is in addition to the restriction mentioned
 * above concerning platform support for multiple displays. Thus,
 * the only way to have multiple simultaneously active displays,
 * even on platforms which support it, is to have multiple threads.)
 * </li>
 * </ul>
 * Enforcing these attributes allows SWT to be implemented directly
 * on the underlying operating system's event model. This has 
 * numerous benefits including smaller footprint, better use of 
 * resources, safer memory management, clearer program logic,
 * better performance, and fewer overall operating system threads
 * required. The down side however, is that care must be taken
 * (only) when constructing multi-threaded applications to use the
 * inter-thread communication mechanisms which this class provides
 * when required.
 * </p><p>
 * All SWT API methods which may only be called from the user-interface
 * thread are distinguished in their documentation by indicating that
 * they throw the "<code>ERROR_THREAD_INVALID_ACCESS</code>"
 * SWT exception.
 * </p>
 * <dl>
 * <dt><b>Styles:</b></dt>
 * <dd>(none)</dd>
 * <dt><b>Events:</b></dt>
 * <dd>Close, Dispose</dd>
 * </dl>
 * <p>
 * IMPORTANT: This class is <em>not</em> intended to be subclassed.
 * </p>
 * @see #syncExec
 * @see #asyncExec
 * @see #wake
 * @see #readAndDispatch
 * @see #sleep
 * @see Device#dispose
 */
public class Display extends Device {
	
	/* Windows and Events */
	static final int WAKE_CLASS = 'S' << 24 | 'W' << 16 | 'T' << 8 | '-';
	static final int WAKE_KIND = 1;
	Event [] eventQueue;
	Callback actionCallback, appleEventCallback, commandCallback, controlCallback, accessibilityCallback, appearanceCallback;
	Callback drawItemCallback, itemDataCallback, itemNotificationCallback, itemCompareCallback;
	Callback hitTestCallback, keyboardCallback, menuCallback, mouseHoverCallback, helpCallback;
	Callback mouseCallback, trackingCallback, windowCallback, colorCallback, textInputCallback;
	int actionProc, appleEventProc, commandProc, controlProc, appearanceProc, accessibilityProc;
	int drawItemProc, itemDataProc, itemNotificationProc, itemCompareProc, helpProc;
	int hitTestProc, keyboardProc, menuProc, mouseHoverProc;
	int mouseProc, trackingProc, windowProc, colorProc, textInputProc;
	EventTable eventTable, filterTable;
	int queue, lastModifiers;
	boolean closing;
	
	boolean inPaint, needsPaint;

	/* GC */
	int gcWindow;

	/* Deferred dispose window */
	int disposeWindow;
	int [] disposeWindowList;

	/* Sync/Async Widget Communication */
	Synchronizer synchronizer = new Synchronizer (this);
	Thread thread;
	
	/* Widget Table */
	int freeSlot;
	int [] indexTable, property;
	Widget [] widgetTable;
	static final int GROW_SIZE = 1024;
	static final int SWT0 = ('s'<<24) + ('w'<<16) + ('t'<<8) + '0';
	
	/* Focus */
	int focusEvent;
	Control focusControl;
	Combo focusCombo;
	boolean ignoreFocus;

	/* Menus */
	Menu menuBar;
	Menu [] menus, popups;
	static final int ID_TEMPORARY = 1000;
	static final int ID_START = 1001;
	
	/* Display Shutdown */
	Runnable [] disposeList;

	/* System Tray */
	Tray tray;
	
	/* Timers */
	int [] timerIds;
	Runnable [] timerList;
	Callback timerCallback;
	int timerProc;
	boolean allowTimers = true;
		
	/* Current caret */
	Caret currentCaret;
	Callback caretCallback;
	int caretID, caretProc;
	
	/* Grabs */
	Control grabControl, mouseUpControl;
	boolean grabbing;

	/* Hover Help */
	int helpString;
	Widget helpWidget;
	int lastHelpX, lastHelpY;
	
	/* Mouse DoubleClick */
	int clickCount, clickCountButton;
	
	/* Mouse Enter/Exit/Hover */
	Control currentControl;
	int mouseHoverID;
	org.eclipse.swt.internal.carbon.Point dragMouseStart = null;
	boolean dragging, mouseMoved;
	
	/* Insets */
	Rect buttonInset, tabFolderNorthInset, tabFolderSouthInset, comboInset, editTextInset;
	
	/* Fonts */
	boolean smallFonts;
	boolean noFocusRing;

	/* Keyboard */
	int kchrPtr;
	int [] kchrState = new int [1];

	/* System Resources */
	Image errorImage, infoImage, warningImage;
	Cursor [] cursors = new Cursor [SWT.CURSOR_HAND + 1];
	
	/* System Settings */
	boolean runSettings;
	RGBColor highlightColor;

	/* Dock icon */
	int dockImage, dockImageData;

	/* Key Mappings. */
	static int [] [] KeyTable = {

		/* Keyboard and Mouse Masks */
		{58,	SWT.ALT},
		{56,	SWT.SHIFT},
		{59,	SWT.CONTROL},
		{55,	SWT.COMMAND},

		/* Non-Numeric Keypad Keys */
		{126, SWT.ARROW_UP},
		{125, SWT.ARROW_DOWN},
		{123, SWT.ARROW_LEFT},
		{124, SWT.ARROW_RIGHT},
		{116, SWT.PAGE_UP},
		{121, SWT.PAGE_DOWN},
		{115, SWT.HOME},
		{119, SWT.END},
//		{??,	SWT.INSERT},

		/* Virtual and Ascii Keys */
		{51,	SWT.BS},
		{36,	SWT.CR},
		{117, SWT.DEL},
		{53,	SWT.ESC},
		{76,	SWT.LF},
		{48,	SWT.TAB},	
		
		/* Functions Keys */
		{122, SWT.F1},
		{120, SWT.F2},
		{99,	SWT.F3},
		{118, SWT.F4},
		{96,	SWT.F5},
		{97,	SWT.F6},
		{98,	SWT.F7},
		{100, SWT.F8},
		{101, SWT.F9},
		{109, SWT.F10},
		{103, SWT.F11},
		{111, SWT.F12},
		{105, SWT.F13},
		{107, SWT.F14},
		{113, SWT.F15},
		
		/* Numeric Keypad Keys */
		{67, SWT.KEYPAD_MULTIPLY},
		{69, SWT.KEYPAD_ADD},
		{76, SWT.KEYPAD_CR},
		{78, SWT.KEYPAD_SUBTRACT},
		{65, SWT.KEYPAD_DECIMAL},
		{75, SWT.KEYPAD_DIVIDE},
		{82, SWT.KEYPAD_0},
		{83, SWT.KEYPAD_1},
		{84, SWT.KEYPAD_2},
		{85, SWT.KEYPAD_3},
		{86, SWT.KEYPAD_4},
		{87, SWT.KEYPAD_5},
		{88, SWT.KEYPAD_6},
		{89, SWT.KEYPAD_7},
		{91, SWT.KEYPAD_8},
		{92, SWT.KEYPAD_9},
		{81, SWT.KEYPAD_EQUAL},

		/* Other keys */
//		{??,	SWT.CAPS_LOCK},
		{71,	SWT.NUM_LOCK},
//		{??,	SWT.SCROLL_LOCK},
//		{??,	SWT.PAUSE},
//		{??,	SWT.BREAK},
//		{??,	SWT.PRINT_SCREEN},
		{114, SWT.HELP},
		
	};

	static String APP_NAME = "SWT";
	static final String ADD_WIDGET_KEY = "org.eclipse.swt.internal.addWidget";

	/* Multiple Displays. */
	static Display Default;
	static Display [] Displays = new Display [4];
				
	/* Package Name */
	static final String PACKAGE_PREFIX = "org.eclipse.swt.widgets.";
			
	/* Display Data */
	Object data;
	String [] keys;
	Object [] values;
	
	/*
	* TEMPORARY CODE.  Install the runnable that
	* gets the current display. This code will
	* be removed in the future.
	*/
	static {
		DeviceFinder = new Runnable () {
			public void run () {
				Device device = getCurrent ();
				if (device == null) {
					device = getDefault ();
				}
				setDevice (device);
			}
		};
	}
	
/*
* TEMPORARY CODE.
*/
static void setDevice (Device device) {
	CurrentDevice = device;
}

static byte [] ascii (String name) {
	int length = name.length ();
	char [] chars = new char [length];
	name.getChars (0, length, chars, 0);
	byte [] buffer = new byte [length + 1];
	for (int i=0; i<length; i++) {
		buffer [i] = (byte) chars [i];
	}
	return buffer;
}

static int translateKey (int key) {
	for (int i=0; i<KeyTable.length; i++) {
		if (KeyTable [i] [0] == key) return KeyTable [i] [1];
	}
	return 0;
}

static int untranslateKey (int key) {
	for (int i=0; i<KeyTable.length; i++) {
		if (KeyTable [i] [1] == key) return KeyTable [i] [0];
	}
	return 0;
}

int actionProc (int theControl, int partCode) {
	Widget widget = getWidget (theControl);
	if (widget != null) return widget.actionProc (theControl, partCode);
	return OS.noErr;
}

int appearanceProc (int theAppleEvent, int reply, int handlerRefcon) {
	runSettings = true;
	wakeThread ();
	return OS.eventNotHandledErr;
}

int appleEventProc (int nextHandler, int theEvent, int userData) {
	int eventClass = OS.GetEventClass (theEvent);
	int eventKind = OS.GetEventKind (theEvent);
	switch (eventClass) {
		case OS.kEventClassApplication: 
			switch (eventKind) {
				case OS.kEventAppAvailableWindowBoundsChanged: {
					/* Reset the dock image in case the dock has been restarted */
					if (dockImage != 0) {
						int [] reason = new int [1];
						OS.GetEventParameter (theEvent, OS.kEventParamReason, OS.typeUInt32, null, 4, null, reason);
						if (reason [0] == OS.kAvailBoundsChangedForDock) {
							OS.SetApplicationDockTileImage (dockImage);
						}
					}
					break;
				}
			}
			break;
		case OS.kEventClassAppleEvent:
			int [] aeEventID = new int [1];
			if (OS.GetEventParameter (theEvent, OS.kEventParamAEEventID, OS.typeType, null, 4, null, aeEventID) == OS.noErr) {
				if (aeEventID [0] == OS.kAEQuitApplication) {
					Event event = new Event ();
					sendEvent (SWT.Close, event);
					if (event.doit) {
						/*
						* When the application is closing, no SWT program can continue
						* to run.  In order to avoid running code after the display has
						* been disposed, exit from Java.
						*/
						dispose ();
						System.exit (0);
					}
					return OS.userCanceledErr;
				}
			}
			break;
	}
	return OS.eventNotHandledErr;
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when an event of the given type occurs anywhere
 * in a widget. The event type is one of the event constants
 * defined in class <code>SWT</code>. When the event does occur,
 * the listener is notified by sending it the <code>handleEvent()</code>
 * message.
 * <p>
 * Setting the type of an event to <code>SWT.None</code> from
 * within the <code>handleEvent()</code> method can be used to
 * change the event type and stop subsequent Java listeners
 * from running. Because event filters run before other listeners,
 * event filters can both block other listeners and set arbitrary
 * fields within an event. For this reason, event filters are both
 * powerful and dangerous. They should generally be avoided for
 * performance, debugging and code maintenance reasons.
 * </p>
 * 
 * @param eventType the type of event to listen for
 * @param listener the listener which should be notified when the event occurs
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 *
 * @see Listener
 * @see SWT
 * @see #removeFilter
 * @see #removeListener
 * 
 * @since 3.0 
 */
public void addFilter (int eventType, Listener listener) {
	checkDevice ();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (filterTable == null) filterTable = new EventTable ();
	filterTable.hook (eventType, listener);
}

/**
 * Adds the listener to the collection of listeners who will
 * be notified when an event of the given type occurs. The event
 * type is one of the event constants defined in class <code>SWT</code>.
 * When the event does occur in the display, the listener is notified by
 * sending it the <code>handleEvent()</code> message.
 *
 * @param eventType the type of event to listen for
 * @param listener the listener which should be notified when the event occurs
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 *
 * @see Listener
 * @see SWT
 * @see #removeListener
 * 
 * @since 2.0 
 */
public void addListener (int eventType, Listener listener) {
	checkDevice ();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) eventTable = new EventTable ();
	eventTable.hook (eventType, listener);
}

void addMenu (Menu menu) {
	if (menus == null) menus = new Menu [12];
	for (int i=0; i<menus.length; i++) {
		if (menus [i] == null) {
			menu.id = (short)(ID_START + i);
			menus [i] = menu;
			return;
		}
	}
	Menu [] newMenus = new Menu [menus.length + 12];
	menu.id = (short)(ID_START + menus.length);
	newMenus [menus.length] = menu;
	System.arraycopy (menus, 0, newMenus, 0, menus.length);
	menus = newMenus;
}

void addPopup (Menu menu) {
	if (popups == null) popups = new Menu [4];
	int length = popups.length;
	for (int i=0; i<length; i++) {
		if (popups [i] == menu) return;
	}
	int index = 0;
	while (index < length) {
		if (popups [index] == null) break;
		index++;
	}
	if (index == length) {
		Menu [] newPopups = new Menu [length + 4];
		System.arraycopy (popups, 0, newPopups, 0, length);
		popups = newPopups;
	}
	popups [index] = menu;
}

void addToDisposeWindow (int control) {
	int [] root = new int [1];
	if (disposeWindow == 0) {
		int [] outWindow = new int [1];
		OS.CreateNewWindow (OS.kOverlayWindowClass, 0, new Rect(), outWindow);
		disposeWindow = outWindow [0];
		OS.CreateRootControl (disposeWindow, root);
	} else {
		OS.GetRootControl (disposeWindow, root);
	}
	OS.EmbedControl (control, root [0]);
}

void addWidget (int handle, Widget widget) {
	if (handle == 0) return;
	if (freeSlot == -1) {
		int length = (freeSlot = indexTable.length) + GROW_SIZE;
		int [] newIndexTable = new int [length];
		Widget [] newWidgetTable = new Widget [length];
		System.arraycopy (indexTable, 0, newIndexTable, 0, freeSlot);
		System.arraycopy (widgetTable, 0, newWidgetTable, 0, freeSlot);
		for (int i=freeSlot; i<length-1; i++) {
			newIndexTable [i] = i + 1;
		}
		newIndexTable [length - 1] = -1;
		indexTable = newIndexTable;
		widgetTable = newWidgetTable;
	}
	property [0] = freeSlot + 1;
	OS.SetControlProperty (handle, SWT0, SWT0, 4, property);
	int oldSlot = freeSlot;
	freeSlot = indexTable [oldSlot];
	indexTable [oldSlot] = -2;
	widgetTable [oldSlot] = widget;
}

void addDisposeWindow (int window) {
	if (disposeWindowList == null) disposeWindowList = new int [4];
	int length = disposeWindowList.length;
	for (int i=0; i<length; i++) {
		if (disposeWindowList [i] == window) return;
	}
	int index = 0;
	while (index < length) {
		if (disposeWindowList [index] == 0) break;
		index++;
	}
	if (index == length) {
		int [] newList = new int [length + 4];
		System.arraycopy (disposeWindowList, 0, newList, 0, length);
		disposeWindowList = newList;
	}
	disposeWindowList [index] = window;
}

/**
 * Causes the <code>run()</code> method of the runnable to
 * be invoked by the user-interface thread at the next 
 * reasonable opportunity. The caller of this method continues 
 * to run in parallel, and is not notified when the
 * runnable has completed.  Specifying <code>null</code> as the
 * runnable simply wakes the user-interface thread when run.
 * <p>
 * Note that at the time the runnable is invoked, widgets 
 * that have the receiver as their display may have been
 * disposed. Therefore, it is necessary to check for this
 * case inside the runnable before accessing the widget.
 * </p>
 *
 * @param runnable code to run on the user-interface thread or <code>null</code>
 *
 * @exception SWTException <ul>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 * 
 * @see #syncExec
 */
public void asyncExec (Runnable runnable) {
	if (isDisposed ()) error (SWT.ERROR_DEVICE_DISPOSED);
	synchronizer.asyncExec (runnable);
}

/**
 * Causes the system hardware to emit a short sound
 * (if it supports this capability).
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 */
public void beep () {
	checkDevice ();
	OS.SysBeep ((short) 100);
}

int caretProc (int id, int clientData) {
	if (currentCaret == null || currentCaret.isDisposed()) return 0;
	if (currentCaret.blinkCaret ()) {
		int blinkRate = currentCaret.blinkRate;
		if (blinkRate == 0) return 0;
		OS.SetEventLoopTimerNextFireTime (id, blinkRate / 1000.0);
	} else {
		currentCaret = null;
	}
	return 0;
}

protected void checkDevice () {
	if (thread == null) error (SWT.ERROR_WIDGET_DISPOSED);
	if (thread != Thread.currentThread ()) error (SWT.ERROR_THREAD_INVALID_ACCESS);
	if (isDisposed ()) error (SWT.ERROR_DEVICE_DISPOSED);
}

/**
 * Checks that this class can be subclassed.
 * <p>
 * IMPORTANT: See the comment in <code>Widget.checkSubclass()</code>.
 * </p>
 *
 * @exception SWTException <ul>
 *    <li>ERROR_INVALID_SUBCLASS - if this class is not an allowed subclass</li>
 * </ul>
 *
 * @see Widget#checkSubclass
 */
protected void checkSubclass () {
	if (!Display.isValidClass (getClass ())) error (SWT.ERROR_INVALID_SUBCLASS);
}

int[] createImage (int type) {
	int[] ref = new int [1];
	int result = OS.GetIconRef (OS.kOnSystemDisk, OS.kSystemIconsCreator, type, ref);
	if (result != OS.noErr) return null;
	int[] family = new int [1];
	result = OS.IconRefToIconFamily (ref [0], OS.kSelectorAlLAvailableData, family);
	OS.ReleaseIconRef (ref [0]);
	if (result != OS.noErr) return null;
	int[] image = createImageFromFamily (family [0], OS.kLarge32BitData, OS.kLarge8BitMask, 32, 32);
	OS.DisposeHandle (family [0]);
	return image;
}

int[] createImageFromFamily (int family, int type, int maskType, int width, int height) {
	int dataHandle = OS.NewHandle (0);
	int result = OS.GetIconFamilyData (family, type, dataHandle);
	if (result != OS.noErr) {
		OS.DisposeHandle (dataHandle);
		return null;
	}
	int maskHandle = OS.NewHandle (0);
	result = OS.GetIconFamilyData (family, maskType, maskHandle);
	if (result != OS.noErr) {
		OS.DisposeHandle (maskHandle);
		OS.DisposeHandle (dataHandle);
		return null;
	}
	int bpr = width * 4;
	int dataSize = OS.GetHandleSize (dataHandle);
	int data = OS.NewPtrClear (dataSize);
	if (data == 0)  {
		OS.DisposeHandle (maskHandle);
		OS.DisposeHandle (dataHandle);
		return null;
	}
	OS.HLock (dataHandle);
	OS.HLock (maskHandle);
	int[] iconPtr = new int [1];
	int[] maskPtr = new int [1];
	OS.memcpy (iconPtr, dataHandle, 4);
	OS.memcpy (maskPtr, maskHandle, 4);
	OS.memcpy (data, iconPtr [0], dataSize);
	int pixelCount = dataSize / 4;
	for (int i = 0; i < pixelCount; i++) {
		OS.memcpy (data + (i * 4), maskPtr [0] + i, 1);
	}
	OS.HUnlock (maskHandle);
	OS.HUnlock (dataHandle);
	OS.DisposeHandle (maskHandle);
	OS.DisposeHandle (dataHandle);

	int provider = OS.CGDataProviderCreateWithData (0, data, dataSize, 0);
	if (provider == 0) {
		OS.DisposePtr (data);
		return null;
	}
	int colorspace = OS.CGColorSpaceCreateDeviceRGB ();
	if (colorspace == 0) {
		OS.CGDataProviderRelease (provider);
		OS.DisposePtr (data);
		return null;
	}
	int cgImage = OS.CGImageCreate (width, height, 8, 32, bpr, colorspace, OS.kCGImageAlphaFirst, provider, null, true, 0);
	OS.CGColorSpaceRelease (colorspace);
	OS.CGDataProviderRelease (provider);
	
	return new int[] {cgImage, data};
}

int createOverlayWindow () {
	int gdevice = OS.GetMainDevice ();
	int [] ptr = new int [1];
	OS.memcpy (ptr, gdevice, 4);
	GDevice device = new GDevice ();
	OS.memcpy (device, ptr [0], GDevice.sizeof);
	Rect rect = new Rect ();	
	OS.SetRect (rect, device.left, device.top, device.right, device.bottom);
	int [] outWindow = new int [1];
	OS.CreateNewWindow (OS.kOverlayWindowClass, 0, rect, outWindow);
	if (outWindow [0] == 0) SWT.error (SWT.ERROR_NO_HANDLES);
	return outWindow [0];
}

/**
 * Constructs a new instance of this class.
 * <p>
 * Note: The resulting display is marked as the <em>current</em>
 * display. If this is the first display which has been 
 * constructed since the application started, it is also
 * marked as the <em>default</em> display.
 * </p>
 *
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if called from a thread that already created an existing display</li>
 *    <li>ERROR_INVALID_SUBCLASS - if this class is not an allowed subclass</li>
 * </ul>
 *
 * @see #getCurrent
 * @see #getDefault
 * @see Widget#checkSubclass
 * @see Shell
 */
public Display () {
	this (null);
}

/**
 * Constructs a new instance of this class using the parameter.
 * 
 * @param data the device data
 */
public Display (DeviceData data) {
	super (data);
}

static synchronized void checkDisplay (Thread thread, boolean multiple) {
	for (int i=0; i<Displays.length; i++) {
		if (Displays [i] != null) {
			if (!multiple) SWT.error (SWT.ERROR_NOT_IMPLEMENTED, null, " [multiple displays]");
			if (Displays [i].thread == thread) SWT.error (SWT.ERROR_THREAD_INVALID_ACCESS);
		}
	}
}

int colorProc (int inControl, int inMessage, int inDrawDepth, int inDrawInColor) {
	Widget widget = getWidget (inControl);
	if (widget != null) return widget.colorProc (inControl, inMessage, inDrawDepth, inDrawInColor);
	return OS.eventNotHandledErr;
}

int commandProc (int nextHandler, int theEvent, int userData) {
	int eventKind = OS.GetEventKind (theEvent);
	HICommand command = new HICommand ();
	OS.GetEventParameter (theEvent, OS.kEventParamDirectObject, OS.typeHICommand, null, HICommand.sizeof, null, command);
	switch (eventKind) {
		case OS.kEventProcessCommand: {
			if (command.commandID == OS.kAEQuitApplication) {
				if (!closing) close ();
				return OS.noErr;
			}
			if ((command.attributes & OS.kHICommandFromMenu) != 0) {
				if (userData != 0) {
					Widget widget = getWidget (userData);
					if (widget != null) return widget.commandProc (nextHandler, theEvent, userData);
				} else {
					int result = OS.eventNotHandledErr;
					int menuRef = command.menu_menuRef;
					short menuID = OS.GetMenuID (menuRef);
					Menu menu = getMenu (menuID);
					if (menu != null) {
						/*
						* Feature in the Macintosh.  When a menu item is selected by the
						* user, the Macintosh sends kEventMenuOpening, remembers the index
						* of the item the user selected, sends kEventMenuClosed and then
						* sends kEventProcessCommand.  If application code modifies the menu
						* inside of kEventMenuClosed by adding or removing items, the index
						* of the item that the user selected is invalid.  The fix is to detect
						* that a menu has been modified during kEventMenuClosed and use the
						* last target item remembered kEventMenuTargetItem.
						*/
						MenuItem item = null;
						if (menu.closed && menu.modified) {
							item = menu.lastTarget;
						} else {
							item = menu.getItem (command.menu_menuItemIndex - 1);
						}
						if (item != null) {
							result = item.kEventProcessCommand (nextHandler, theEvent, userData);
						}
					}
					OS.HiliteMenu ((short) 0);
					return result;
				}
			}
		}
	}
	return OS.eventNotHandledErr;
}

Rect computeInset (int control) {
	int tempRgn = OS.NewRgn ();
	Rect rect = new Rect ();
	OS.GetControlRegion (control, (short) OS.kControlStructureMetaPart, tempRgn);
	OS.GetControlBounds (control, rect);
	Rect rgnRect = new Rect ();
	OS.GetRegionBounds (tempRgn, rgnRect);
	OS.DisposeRgn (tempRgn);
	rect.left -= rgnRect.left;
	rect.top -= rgnRect.top;
	rect.right = (short) (rgnRect.right - rect.right);
	rect.bottom = (short) (rgnRect.bottom - rect.bottom);
	return rect; 
}

int controlProc (int nextHandler, int theEvent, int userData) {
	Widget widget = getWidget (userData);
	if (widget != null) return widget.controlProc (nextHandler, theEvent, userData);
	return OS.eventNotHandledErr;
}

int accessibilityProc (int nextHandler, int theEvent, int userData) {
	Widget widget = getWidget (userData);
	if (widget != null) return widget.accessibilityProc (nextHandler, theEvent, userData);
	return OS.eventNotHandledErr;
}

static String convertToLf(String text) {
	char Cr = '\r';
	char Lf = '\n';
	int length = text.length ();
	if (length == 0) return text;
	
	/* Check for an LF or CR/LF.  Assume the rest of the string 
	 * is formated that way.  This will not work if the string 
	 * contains mixed delimiters. */
	int i = text.indexOf (Lf, 0);
	if (i == -1 || i == 0) return text;
	if (text.charAt (i - 1) != Cr) return text;

	/* The string is formatted with CR/LF.
	 * Create a new string with the LF line delimiter. */
	i = 0;
	StringBuffer result = new StringBuffer ();
	while (i < length) {
		int j = text.indexOf (Cr, i);
		if (j == -1) j = length;
		String s = text.substring (i, j);
		result.append (s);
		i = j + 2;
		result.append (Lf);
	}
	return result.toString ();
}

void clearMenuFlags () {
	if (menus == null) return;
	for (int i=0; i<menus.length; i++) {
		Menu menu = menus [i];
		if (menu != null) {
			menu.modified = menu.closed = false;
			menu.lastTarget = null;
		}
	}
}

/**
 * Requests that the connection between SWT and the underlying
 * operating system be closed.
 *
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 *
 * @see Device#dispose
 * 
 * @since 2.0
 */
public void close () {
	checkDevice ();
	closing = true;
	Event event = new Event ();
	sendEvent (SWT.Close, event);
	if (event.doit) dispose ();
}

/**
 * Creates the device in the operating system.  If the device
 * does not have a handle, this method may do nothing depending
 * on the device.
 * <p>
 * This method is called before <code>init</code>.
 * </p>
 *
 * @param data the DeviceData which describes the receiver
 *
 * @see #init
 */
protected void create (DeviceData data) {
	checkSubclass ();
	checkDisplay (thread = Thread.currentThread (), false);
	createDisplay (data);
	register (this);
	if (Default == null) Default = this;
}

void createDisplay (DeviceData data) {
	/*
	* Feature in the Macintosh.  On OS 10.2, it is necessary
	* to explicitly check in with the Process Manager and set
	* the current process to be the front process in order for
	* windows to come to the front by default.  The fix is call
	* both GetCurrentProcess() and SetFrontProcess().
	* 
	* NOTE: It is not actually necessary to use the process
	* serial number returned by GetCurrentProcess() in the
	* call to SetFrontProcess() (ie. kCurrentProcess can be
	* used) but both functions must be called in order for
	* windows to come to the front.
	*/
	int [] psn = new int [2];
	if (OS.GetCurrentProcess (psn) == OS.noErr) {
		int pid = OS.getpid ();
		byte [] buffer = null;
		int ptr = OS.getenv (ascii ("APP_NAME_" + pid));
		if (ptr != 0) {
			buffer = new byte [OS.strlen (ptr) + 1];
			OS.memcpy (buffer, ptr, buffer.length);
		} else {
			if (APP_NAME != null) {
				char [] chars = new char [APP_NAME.length ()];
				APP_NAME.getChars (0, chars.length, chars, 0);
				int cfstring = OS.CFStringCreateWithCharacters (OS.kCFAllocatorDefault, chars, chars.length);
				if (cfstring != 0) {
					CFRange range = new CFRange ();
					range.length = chars.length;
					int encoding = OS.CFStringGetSystemEncoding ();
					int [] size = new int [1];
					int numChars = OS.CFStringGetBytes (cfstring, range, encoding, (byte) '?', true, null, 0, size);
					if (numChars != 0) {
						buffer = new byte [size [0] + 1];
						numChars = OS.CFStringGetBytes (cfstring, range, encoding, (byte) '?', true, buffer, size [0], size);
					}
					OS.CFRelease (cfstring);
				}
			}
		}
		if (buffer != null) OS.CPSSetProcessName (psn, buffer);	
		OS.CPSEnableForegroundOperation (psn, 0x03, 0x3C, 0x2C, 0x1103);
		OS.SetFrontProcess (psn);
		ptr = OS.getenv (ascii ("APP_ICON_" + pid));
		if (ptr != 0) {
			int [] image = readImageRef (ptr);
			if (image != null) {
				dockImage = image [0];
				dockImageData = image [1];
				OS.SetApplicationDockTileImage (dockImage);
			}
		}
	}
	/*
	* Feature in the Macintosh.  In order to get the standard
	* application menu to appear on the menu bar, an application
	* must manipulate the menu bar.  If the application does not
	* install a menu bar, the application menu will not appear.
	* The fix is to use ClearMenuBar() to manipulate the menu
	* bar before any application has had a chance install a menu
	* bar.
	*/
	OS.ClearMenuBar ();
	queue = OS.GetCurrentEventQueue ();
	OS.TXNInitTextension (0, 0, 0);
	
	/* Save the current highlight color */
	OS.RegisterAppearanceClient ();
	highlightColor = new RGBColor ();
	OS.GetThemeBrushAsColor ((short) OS.kThemeBrushPrimaryHighlightColor, (short) getDepth(), true, highlightColor);
}

synchronized static void deregister (Display display) {
	for (int i=0; i<Displays.length; i++) {
		if (display == Displays [i]) Displays [i] = null;
	}
}

/**
 * Destroys the device in the operating system and releases
 * the device's handle.  If the device does not have a handle,
 * this method may do nothing depending on the device.
 * <p>
 * This method is called after <code>release</code>.
 * </p>
 * @see Device#dispose
 * @see #release
 */
protected void destroy () {
	if (this == Default) Default = null;
	deregister (this);
	destroyDisplay ();
}

void destroyDisplay () {
}

/**
 * Causes the <code>run()</code> method of the runnable to
 * be invoked by the user-interface thread just before the
 * receiver is disposed.  Specifying a <code>null</code> runnable
 * is ignored.
 *
 * @param runnable code to run at dispose time.
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 */
public void disposeExec (Runnable runnable) {
	checkDevice ();
	if (disposeList == null) disposeList = new Runnable [4];
	for (int i=0; i<disposeList.length; i++) {
		if (disposeList [i] == null) {
			disposeList [i] = runnable;
			return;
		}
	}
	Runnable [] newDisposeList = new Runnable [disposeList.length + 4];
	System.arraycopy (disposeList, 0, newDisposeList, 0, disposeList.length);
	newDisposeList [disposeList.length] = runnable;
	disposeList = newDisposeList;
}

void dragDetect (Control control) {
	if (!dragging && control.hooks (SWT.DragDetect) && dragMouseStart != null) {
		if (OS.WaitMouseMoved (dragMouseStart)) {
			dragging = true;
			Rect rect = new Rect ();
			int window = OS.GetControlOwner (control.handle);
			int x, y;
			if (OS.HIVIEW) {
				CGPoint pt = new CGPoint ();
				pt.x = dragMouseStart.h;
				pt.y = dragMouseStart.v;
				OS.HIViewConvertPoint (pt, 0, control.handle);
				x = (int) pt.x;
				y = (int) pt.y;
				OS.GetWindowBounds (window, (short) OS.kWindowStructureRgn, rect);
			} else {
				OS.GetControlBounds (control.handle, rect);
				x = dragMouseStart.h - rect.left;
				y = dragMouseStart.v - rect.top;
				OS.GetWindowBounds (window, (short) OS.kWindowContentRgn, rect);
			}
			x -= rect.left;
			y -= rect.top;
			control.sendDragEvent (x, y);
			dragMouseStart = null;
		}
	}
}

int drawItemProc (int browser, int item, int property, int itemState, int theRect, int gdDepth, int colorDevice) {
	Widget widget = getWidget (browser);
	if (widget != null) return widget.drawItemProc (browser, item, property, itemState, theRect, gdDepth, colorDevice);
	return OS.noErr;
}

void disposeWindows () {
	if (disposeWindow != 0) {
		OS.DisposeWindow (disposeWindow);
		disposeWindow = 0;
	}
	if (disposeWindowList != null) {
		for (int i = 0; i < disposeWindowList.length; i++) {
			if (disposeWindowList [i] != 0) {
				OS.DisposeWindow (disposeWindowList [i]);
			}
		}
		disposeWindowList = null;
	}
}

void error (int code) {
	SWT.error(code);
}

boolean filterEvent (Event event) {
	if (filterTable != null) filterTable.sendEvent (event);
	return false;
}

boolean filters (int eventType) {
	if (filterTable == null) return false;
	return filterTable.hooks (eventType);
}

/**
 * Given the operating system handle for a widget, returns
 * the instance of the <code>Widget</code> subclass which
 * represents it in the currently running application, if
 * such exists, or null if no matching widget can be found.
 * <p>
 * <b>IMPORTANT:</b> This method should not be called from
 * application code. The arguments are platform-specific.
 * </p>
 *
 * @param handle the handle for the widget
 * @return the SWT widget that the handle represents
 *
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 */
public Widget findWidget (int handle) {
	checkDevice ();
	return getWidget (handle);
}

/**
 * Given the operating system handle for a widget,
 * and widget-specific id, returns the instance of
 * the <code>Widget</code> subclass which represents
 * the handle/id pair in the currently running application,
 * if such exists, or null if no matching widget can be found.
 * <p>
 * <b>IMPORTANT:</b> This method should not be called from
 * application code. The arguments are platform-specific.
 * </p>
 *
 * @param handle the handle for the widget
 * @param id the id for the subwidget (usually an item)
 * @return the SWT widget that the handle/id pair represents
 *
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 * 
 * @since 3.1
 */
public Widget findWidget (int handle, int id) {
	return null;
}

/**
 * Returns the display which the given thread is the
 * user-interface thread for, or null if the given thread
 * is not a user-interface thread for any display.  Specifying
 * <code>null</code> as the thread will return <code>null</code>
 * for the display. 
 *
 * @param thread the user-interface thread
 * @return the display for the given thread
 */
public static synchronized Display findDisplay (Thread thread) {
	for (int i=0; i<Displays.length; i++) {
		Display display = Displays [i];
		if (display != null && display.thread == thread) {
			return display;
		}
	}
	return null;
}

/**
 * Returns the currently active <code>Shell</code>, or null
 * if no shell belonging to the currently running application
 * is active.
 *
 * @return the active shell or null
 *
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 */
public Shell getActiveShell () {
	checkDevice ();
	for (int i=0; i<widgetTable.length; i++) {
		Widget widget = widgetTable [i];
		if (widget != null && widget instanceof Shell) {
			Shell shell = (Shell) widget;
			if (OS.IsWindowActive (shell.shellHandle)) return shell;
		}
	}
	return null;
}

/**
 * Returns a rectangle describing the receiver's size and location.
 *
 * @return the bounding rectangle
 *
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 */
public Rectangle getBounds () {
	checkDevice ();
	int gdevice = OS.GetDeviceList();
	if (gdevice == 0 || OS.GetNextDevice (gdevice) == 0) {
		return super.getBounds ();
	}
	Monitor [] monitors = getMonitors ();
	Rectangle rect = monitors [0].getBounds ();
	for (int i=1; i<monitors.length; i++) {
		rect = rect.union (monitors [i].getBounds ()); 
	}
	return rect;
}

/**
 * Returns the display which the currently running thread is
 * the user-interface thread for, or null if the currently
 * running thread is not a user-interface thread for any display.
 *
 * @return the current display
 */
public static synchronized Display getCurrent () {
	return findDisplay (Thread.currentThread ());
}

int getCaretBlinkTime () {
//	checkDevice ();
	return OS.GetCaretTime () * 1000 / 60;
}

/**
 * Returns a rectangle which describes the area of the
 * receiver which is capable of displaying data.
 * 
 * @return the client area
 *
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 *
 * @see #getBounds
 */
public Rectangle getClientArea () {
	checkDevice ();
	int gdevice = OS.GetDeviceList();
	if (gdevice == 0 || OS.GetNextDevice (gdevice) == 0) {
		return super.getClientArea ();
	}
	Monitor [] monitors = getMonitors ();
	Rectangle rect = monitors [0].getBounds ();
	for (int i=1; i<monitors.length; i++) {
		rect = rect.union (monitors [i].getBounds ()); 
	}
	return rect;
}

/**
 * Returns the control which the on-screen pointer is currently
 * over top of, or null if it is not currently over one of the
 * controls built by the currently running application.
 *
 * @return the control under the cursor
 *
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 */
public Control getCursorControl () {
	org.eclipse.swt.internal.carbon.Point where = new org.eclipse.swt.internal.carbon.Point ();
	OS.GetGlobalMouse (where);
	int [] theWindow = new int [1];
	if (OS.FindWindow (where, theWindow) != OS.inContent) return null;
	if (theWindow [0] == 0) return null;
	Rect rect = new Rect ();
	OS.GetWindowBounds (theWindow [0], (short) OS.kWindowContentRgn, rect);
	CGPoint inPoint = new CGPoint ();
	inPoint.x = where.h - rect.left;
	inPoint.y = where.v - rect.top;
	int [] theRoot = new int [1];
	OS.GetRootControl (theWindow [0], theRoot);
	int [] theControl = new int [1];
	OS.HIViewGetSubviewHit (theRoot [0], inPoint, true, theControl);
	while (theControl [0] != 0 && !OS.IsControlEnabled (theControl [0])) {				
		OS.GetSuperControl (theControl [0], theControl);
	}
	if (theControl [0] != 0) {
		do {
			Widget widget = getWidget (theControl [0]);
			if (widget != null) {
				if (widget instanceof Control) {
					Control control = (Control) widget;
					if (control.isEnabled ()) {
						return control.isEnabledModal () ? control : null;
					}
				}
			}
			OS.GetSuperControl (theControl [0], theControl);
		} while (theControl [0] != 0);
	}
	Widget widget = getWidget (theRoot [0]);
	if (widget != null && widget instanceof Control) return (Control) widget;
	return null;
}

/**
 * Returns the location of the on-screen pointer relative
 * to the top left corner of the screen.
 *
 * @return the cursor location
 *
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 */
public Point getCursorLocation () {
	checkDevice ();
	org.eclipse.swt.internal.carbon.Point pt = new org.eclipse.swt.internal.carbon.Point ();
	OS.GetGlobalMouse (pt);
	return new Point (pt.h, pt.v);
}

/**
 * Returns an array containing the recommended cursor sizes.
 *
 * @return the array of cursor sizes
 *
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 * 
 * @since 3.0
 */
public Point [] getCursorSizes () {
	checkDevice ();
	return new Point [] {new Point (16, 16)};
}

/**
 * Returns the default display. One is created (making the
 * thread that invokes this method its user-interface thread)
 * if it did not already exist.
 *
 * @return the default display
 */
public static synchronized Display getDefault () {
	if (Default == null) Default = new Display ();
	return Default;
}

/**
 * Returns the application defined property of the receiver
 * with the specified name, or null if it has not been set.
 * <p>
 * Applications may have associated arbitrary objects with the
 * receiver in this fashion. If the objects stored in the
 * properties need to be notified when the display is disposed
 * of, it is the application's responsibility to provide a
 * <code>disposeExec()</code> handler which does so.
 * </p>
 *
 * @param key the name of the property
 * @return the value of the property or null if it has not been set
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the key is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 *
 * @see #setData(String, Object)
 * @see #disposeExec(Runnable)
 */
public Object getData (String key) {
	checkDevice ();
	if (key == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (keys == null) return null;
	for (int i=0; i<keys.length; i++) {
		if (keys [i].equals (key)) return values [i];
	}
	return null;
}

/**
 * Returns the application defined, display specific data
 * associated with the receiver, or null if it has not been
 * set. The <em>display specific data</em> is a single,
 * unnamed field that is stored with every display. 
 * <p>
 * Applications may put arbitrary objects in this field. If
 * the object stored in the display specific data needs to
 * be notified when the display is disposed of, it is the
 * application's responsibility to provide a
 * <code>disposeExec()</code> handler which does so.
 * </p>
 *
 * @return the display specific data
 *
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 *
 * @see #setData(Object)
 * @see #disposeExec(Runnable)
 */
public Object getData () {
	checkDevice ();
	return data;
}

/**
 * Returns the button dismissal alignment, one of <code>LEFT</code> or <code>RIGHT</code>.
 * The button dismissal alignment is the ordering that should be used when positioning the
 * default dismissal button for a dialog.  For example, in a dialog that contains an OK and
 * CANCEL button, on platforms where the button dismissal alignment is <code>LEFT</code>, the
 * button ordering should be OK/CANCEL.  When button dismissal alignment is <code>RIGHT</code>,
 * the button ordering should be CANCEL/OK.
 *
 * @return the button dismissal order
 *
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 * 
 * @since 2.1
 */
public int getDismissalAlignment () {
	checkDevice ();
	return SWT.RIGHT;
}

/**
 * Returns the longest duration, in milliseconds, between
 * two mouse button clicks that will be considered a
 * <em>double click</em> by the underlying operating system.
 *
 * @return the double click time
 *
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 */
public int getDoubleClickTime () {
	checkDevice ();
	return OS.GetDblTime () * 1000 / 60; 
}

/**
 * Returns the control which currently has keyboard focus,
 * or null if keyboard events are not currently going to
 * any of the controls built by the currently running
 * application.
 *
 * @return the control under the cursor
 *
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 */
public Control getFocusControl () {
	checkDevice ();
	if (focusControl != null && !focusControl.isDisposed ()) {
		return focusControl;
	}
	int theWindow = OS.GetUserFocusWindow ();
	if (theWindow == 0) return null;
	return getFocusControl (theWindow);
}

Control getFocusControl (int window) {
	int [] theControl = new int [1];
	OS.GetKeyboardFocus (window, theControl);
	if (theControl [0] == 0) return null;
	do {
		Widget widget = getWidget (theControl [0]);
		if (widget != null && widget instanceof Control) {
			Control control = (Control) widget;
			return control.isEnabled () ? control : null;
		}
		OS.GetSuperControl (theControl [0], theControl);
	} while (theControl [0] != 0);
	return null;
}

/**
 * Returns true when the high contrast mode is enabled.
 * Otherwise, false is returned.
 * <p>
 * Note: This operation is a hint and is not supported on
 * platforms that do not have this concept.
 * </p>
 *
 * @return the high contrast mode
 *
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 * 
 * @since 3.0
 */
public boolean getHighContrast () {
	checkDevice ();
	return false;
}

/**
 * Returns the maximum allowed depth of icons on this display, in bits per pixel.
 * On some platforms, this may be different than the actual depth of the display.
 *
 * @return the maximum icon depth
 *
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 * 
 * @see Device#getDepth
 */
public int getIconDepth () {
	return getDepth ();
}

/**
 * Returns an array containing the recommended icon sizes.
 *
 * @return the array of icon sizes
 *
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 * 
 * @see Decorations#setImages(Image[])
 * 
 * @since 3.0
 */
public Point [] getIconSizes () {
	checkDevice ();
	return new Point [] { 
		new Point (16, 16), new Point (32, 32), 
		new Point (64, 64), new Point (128, 128)};	
}

int getLastEventTime () {
	/*
	* This code is intentionally commented.  Event time is
	* in seconds and we need an accurate time in milliseconds.
	*/
//	return (int) (OS.GetLastUserEventTime () * 1000.0);
	return (int) System.currentTimeMillis ();
}

Menu [] getMenus (Decorations shell) {
	if (menus == null) return new Menu [0];
	int count = 0;
	for (int i = 0; i < menus.length; i++) {
		Menu menu = menus[i];
		if (menu != null && menu.parent == shell) count++;
	}
	int index = 0;
	Menu[] result = new Menu[count];
	for (int i = 0; i < menus.length; i++) {
		Menu menu = menus[i];
		if (menu != null && menu.parent == shell) {
			result[index++] = menu;
		}
	}
	return result;
}

Menu getMenu (int id) {
	if (menus == null) return null;
	int index = id - ID_START;
	if (0 <= index && index < menus.length) return menus [index];
	return null;
}

Menu getMenuBar () {
	return menuBar;
}

int getMessageCount () {
	return synchronizer.getMessageCount ();
}

/**
 * Returns an array of monitors attached to the device.
 * 
 * @return the array of monitors
 * 
 * @since 3.0
 */
public Monitor [] getMonitors () {
	checkDevice ();
	int count = 0;
	Monitor [] monitors = new Monitor [1];
	Rect rect = new Rect ();
	GDevice device = new GDevice ();
	int gdevice = OS.GetDeviceList ();
	while (gdevice != 0) {
		if (count >= monitors.length) {
			Monitor [] newMonitors = new Monitor [monitors.length + 4];
			System.arraycopy (monitors, 0, newMonitors, 0, monitors.length);
			monitors = newMonitors;
		}
		Monitor monitor = new Monitor ();
		monitor.handle = gdevice;
		int [] ptr = new int [1];
		OS.memcpy (ptr, gdevice, 4);
		OS.memcpy (device, ptr [0], GDevice.sizeof);				
		monitor.x = device.left;
		monitor.y = device.top;
		monitor.width = device.right - device.left;
		monitor.height = device.bottom - device.top;
		OS.GetAvailableWindowPositioningBounds (gdevice, rect);
		monitor.clientX = rect.left;
		monitor.clientY = rect.top;
		monitor.clientWidth = rect.right - rect.left;
		monitor.clientHeight = rect.bottom - rect.top;
		monitors [count++] = monitor;
		gdevice = OS.GetNextDevice (gdevice);		
	}
	if (count < monitors.length) {
		Monitor [] newMonitors = new Monitor [count];
		System.arraycopy (monitors, 0, newMonitors, 0, count);
		monitors = newMonitors;
	}	
	return monitors;
}

/**
 * Returns the primary monitor for that device.
 * 
 * @return the primary monitor
 * 
 * @since 3.0
 */
public Monitor getPrimaryMonitor () {
	checkDevice ();
	int gdevice = OS.GetMainDevice ();
	Monitor monitor = new Monitor ();
	monitor.handle = gdevice;
	int [] ptr = new int [1];
	OS.memcpy (ptr, gdevice, 4);
	GDevice device = new GDevice ();
	OS.memcpy (device, ptr [0], GDevice.sizeof);		
	monitor.x = device.left;
	monitor.y = device.top;
	monitor.width = device.right - device.left;
	monitor.height = device.bottom - device.top;
	Rect rect = new Rect ();		
	OS.GetAvailableWindowPositioningBounds (gdevice, rect);
	monitor.clientX = rect.left;
	monitor.clientY = rect.top;
	monitor.clientWidth = rect.right - rect.left;
	monitor.clientHeight = rect.bottom - rect.top;
	return monitor;
}

/**
 * Returns a (possibly empty) array containing all shells which have
 * not been disposed and have the receiver as their display.
 *
 * @return the receiver's shells
 *
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 */
public Shell [] getShells () {
	checkDevice ();
	int index = 0;
	Shell [] result = new Shell [16];
	for (int i = 0; i < widgetTable.length; i++) {
		Widget widget = widgetTable [i];
		if (widget != null && widget instanceof Shell) {
			int j = 0;
			while (j < index) {
				if (result [j] == widget) break;
				j++;
			}
			if (j == index) {
				if (index == result.length) {
					Shell [] newResult = new Shell [index + 16];
					System.arraycopy (result, 0, newResult, 0, index);
					result = newResult;
				}
				result [index++] = (Shell) widget;	
			}
		}
	}
	if (index == result.length) return result;
	Shell [] newResult = new Shell [index];
	System.arraycopy (result, 0, newResult, 0, index);
	return newResult;
}

/**
 * Returns the thread that has invoked <code>syncExec</code>
 * or null if no such runnable is currently being invoked by
 * the user-interface thread.
 * <p>
 * Note: If a runnable invoked by asyncExec is currently
 * running, this method will return null.
 * </p>
 *
 * @return the receiver's sync-interface thread
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 */
public Thread getSyncThread () {
	if (isDisposed ()) error (SWT.ERROR_DEVICE_DISPOSED);
	return synchronizer.syncThread;
}

/**
 * Returns the matching standard color for the given
 * constant, which should be one of the color constants
 * specified in class <code>SWT</code>. Any value other
 * than one of the SWT color constants which is passed
 * in will result in the color black. This color should
 * not be free'd because it was allocated by the system,
 * not the application.
 *
 * @param id the color constant
 * @return the matching color
 *
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 *
 * @see SWT
 */
public Color getSystemColor (int id) {
	checkDevice ();
	RGBColor rgb = new RGBColor ();
	switch (id) {
		case SWT.COLOR_INFO_FOREGROUND: return super.getSystemColor (SWT.COLOR_BLACK);
		case SWT.COLOR_INFO_BACKGROUND: return Color.carbon_new (this, new float [] {0xFF / 255f, 0xFF / 255f, 0xE1 / 255f, 1});
		case SWT.COLOR_TITLE_FOREGROUND: OS.GetThemeTextColor((short)OS.kThemeTextColorDocumentWindowTitleActive, (short)getDepth(), true, rgb); break;
		case SWT.COLOR_TITLE_BACKGROUND: OS.GetThemeBrushAsColor((short)-5/*undocumented darker highlight color*/, (short)getDepth(), true, rgb); break;
		case SWT.COLOR_TITLE_BACKGROUND_GRADIENT: 	OS.GetThemeBrushAsColor((short)OS.kThemeBrushPrimaryHighlightColor, (short)getDepth(), true, rgb) ; break;
		case SWT.COLOR_TITLE_INACTIVE_FOREGROUND:	OS.GetThemeTextColor((short)OS.kThemeTextColorDocumentWindowTitleInactive, (short)getDepth(), true, rgb); break;
		case SWT.COLOR_TITLE_INACTIVE_BACKGROUND: 	OS.GetThemeBrushAsColor((short)OS.kThemeBrushSecondaryHighlightColor, (short)getDepth(), true, rgb); break;
		case SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT: OS.GetThemeBrushAsColor((short)OS.kThemeBrushSecondaryHighlightColor, (short)getDepth(), true, rgb); break;
		case SWT.COLOR_WIDGET_DARK_SHADOW: return Color.carbon_new (this, new float [] {0x33 / 255f, 0x33 / 255f, 0x33 / 255f, 1});
		case SWT.COLOR_WIDGET_NORMAL_SHADOW: return Color.carbon_new (this, new float [] {0x66 / 255f, 0x66 / 255f, 0x66 / 255f, 1});
		case SWT.COLOR_WIDGET_LIGHT_SHADOW: return Color.carbon_new (this, new float [] {0x99 / 255f, 0x99 / 255f, 0x99 / 255f, 1});
		case SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW: return Color.carbon_new (this, new float [] {0xCC / 255f, 0xCC / 255f, 0xCC / 255f, 1});
		case SWT.COLOR_WIDGET_BACKGROUND: OS.GetThemeBrushAsColor((short)OS.kThemeBrushButtonFaceActive, (short)getDepth(), true, rgb); break;
		case SWT.COLOR_WIDGET_FOREGROUND: OS.GetThemeTextColor((short)OS.kThemeTextColorPushButtonActive, (short)getDepth(), true, rgb); break;
		case SWT.COLOR_WIDGET_BORDER: return super.getSystemColor (SWT.COLOR_BLACK);
		case SWT.COLOR_LIST_FOREGROUND: OS.GetThemeTextColor((short)OS.kThemeTextColorListView, (short)getDepth(), true, rgb); break;
		case SWT.COLOR_LIST_BACKGROUND: OS.GetThemeBrushAsColor((short)OS.kThemeBrushListViewBackground, (short)getDepth(), true, rgb); break;
		case SWT.COLOR_LIST_SELECTION_TEXT: OS.GetThemeTextColor((short)OS.kThemeTextColorListView, (short)getDepth(), true, rgb); break;
		case SWT.COLOR_LIST_SELECTION: OS.GetThemeBrushAsColor((short)OS.kThemeBrushPrimaryHighlightColor, (short)getDepth(), true, rgb); break;
		default:
			return super.getSystemColor (id);	
	}
	float red = ((rgb.red >> 8) & 0xFF) / 255f;
	float green = ((rgb.green >> 8) & 0xFF) / 255f;
	float blue = ((rgb.blue >> 8) & 0xFF) / 255f;
	return Color.carbon_new (this, new float[]{red, green, blue, 1});
}

/**
 * Returns the matching standard platform cursor for the given
 * constant, which should be one of the cursor constants
 * specified in class <code>SWT</code>. This cursor should
 * not be free'd because it was allocated by the system,
 * not the application.  A value of <code>null</code> will
 * be returned if the supplied constant is not an SWT cursor
 * constant. 
 *
 * @param id the SWT cursor constant
 * @return the corresponding cursor or <code>null</code>
 *
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 *
 * @see SWT#CURSOR_ARROW
 * @see SWT#CURSOR_WAIT
 * @see SWT#CURSOR_CROSS
 * @see SWT#CURSOR_APPSTARTING
 * @see SWT#CURSOR_HELP
 * @see SWT#CURSOR_SIZEALL
 * @see SWT#CURSOR_SIZENESW
 * @see SWT#CURSOR_SIZENS
 * @see SWT#CURSOR_SIZENWSE
 * @see SWT#CURSOR_SIZEWE
 * @see SWT#CURSOR_SIZEN
 * @see SWT#CURSOR_SIZES
 * @see SWT#CURSOR_SIZEE
 * @see SWT#CURSOR_SIZEW
 * @see SWT#CURSOR_SIZENE
 * @see SWT#CURSOR_SIZESE
 * @see SWT#CURSOR_SIZESW
 * @see SWT#CURSOR_SIZENW
 * @see SWT#CURSOR_UPARROW
 * @see SWT#CURSOR_IBEAM
 * @see SWT#CURSOR_NO
 * @see SWT#CURSOR_HAND
 * 
 * @since 3.0
 */
public Cursor getSystemCursor (int id) {
	checkDevice ();
	if (!(0 <= id && id < cursors.length)) return null;
	if (cursors [id] == null) {
		cursors [id] = new Cursor (this, id);
	}
	return cursors [id];
}

/**
 * Returns the matching standard platform image for the given
 * constant, which should be one of the icon constants
 * specified in class <code>SWT</code>. This image should
 * not be free'd because it was allocated by the system,
 * not the application.  A value of <code>null</code> will
 * be returned either if the supplied constant is not an
 * SWT icon constant or if the platform does not define an
 * image that corresponds to the constant. 
 *
 * @param id the SWT icon constant
 * @return the corresponding image or <code>null</code>
 *
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 *
 * @see SWT#ICON_ERROR
 * @see SWT#ICON_INFORMATION
 * @see SWT#ICON_QUESTION
 * @see SWT#ICON_WARNING
 * @see SWT#ICON_WORKING
 * 
 * @since 3.0
 */
public Image getSystemImage (int id) {
	checkDevice ();
	switch (id) {
		case SWT.ICON_ERROR: {	
			if (errorImage != null) return errorImage;
			int [] image = createImage (OS.kAlertStopIcon);
			if (image == null) break;
			return errorImage = Image.carbon_new (this, SWT.ICON, image [0], image [1]);
		}
		case SWT.ICON_INFORMATION:
		case SWT.ICON_QUESTION:
		case SWT.ICON_WORKING: {
			if (infoImage != null) return infoImage;
			int [] image = createImage (OS.kAlertNoteIcon);
			if (image == null) break;
			return infoImage = Image.carbon_new (this, SWT.ICON, image [0], image [1]);
		}
		case SWT.ICON_WARNING: {
			if (warningImage != null) return warningImage;
			int [] image = createImage (OS.kAlertCautionIcon);
			if (image == null) break;
			return warningImage = Image.carbon_new (this, SWT.ICON, image [0], image [1]);
		}
	}
	return null;
}

/**
 * Returns the single instance of the system tray or null
 * when there is no system tray available for the platform.
 *
 * @return the system tray or <code>null</code>
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 *
 * @since 3.0
 */
public Tray getSystemTray () {
	checkDevice ();
	return null;
}

/**
 * Returns the user-interface thread for the receiver.
 *
 * @return the receiver's user-interface thread
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 */
public Thread getThread () {
	if (isDisposed ()) error (SWT.ERROR_DEVICE_DISPOSED);
	return thread;
}

Widget getWidget (int handle) {
	if (handle == 0) return null;
	property [0] = 0;
	OS.GetControlProperty (handle, SWT0, SWT0, 4, null, property);
	int index = property [0] - 1;
	if (0 <= index && index < widgetTable.length) return widgetTable [index];
	return null;
}

int helpProc (int inControl, int inGlobalMouse, int inRequest, int outContentProvided, int ioHelpContent) {
	Widget widget = getWidget (inControl);
	if (widget != null) return widget.helpProc (inControl, inGlobalMouse, inRequest, outContentProvided, ioHelpContent);
	return OS.eventNotHandledErr;
}

int hitTestProc (int browser, int item, int property, int theRect, int mouseRect) {
	Widget widget = getWidget (browser);
	if (widget != null) return widget.hitTestProc (browser, item, property, theRect, mouseRect);
	return OS.noErr;
}

/**
 * Initializes any internal resources needed by the
 * device.
 * <p>
 * This method is called after <code>create</code>.
 * </p>
 * 
 * @see #create
 */
protected void init () {
	super.init ();
	initializeCallbacks ();
	initializeInsets ();
	initializeWidgetTable ();
	initializeFonts ();
}
	
void initializeCallbacks () {
	/* Create Callbacks */
	actionCallback = new Callback (this, "actionProc", 2);
	actionProc = actionCallback.getAddress ();
	if (actionProc == 0) error (SWT.ERROR_NO_MORE_CALLBACKS);
	appleEventCallback = new Callback (this, "appleEventProc", 3);
	appleEventProc = appleEventCallback.getAddress ();
	if (appleEventProc == 0) error (SWT.ERROR_NO_MORE_CALLBACKS);
	caretCallback = new Callback(this, "caretProc", 2);
	caretProc = caretCallback.getAddress();
	if (caretProc == 0) error (SWT.ERROR_NO_MORE_CALLBACKS);
	commandCallback = new Callback (this, "commandProc", 3);
	commandProc = commandCallback.getAddress ();
	if (commandProc == 0) error (SWT.ERROR_NO_MORE_CALLBACKS);
	controlCallback = new Callback (this, "controlProc", 3);
	controlProc = controlCallback.getAddress ();
	if (controlProc == 0) error (SWT.ERROR_NO_MORE_CALLBACKS);
	accessibilityCallback = new Callback (this, "accessibilityProc", 3);
	accessibilityProc = accessibilityCallback.getAddress ();
	if (accessibilityProc == 0) error (SWT.ERROR_NO_MORE_CALLBACKS);
	drawItemCallback = new Callback (this, "drawItemProc", 7);
	drawItemProc = drawItemCallback.getAddress ();
	if (drawItemProc == 0) error (SWT.ERROR_NO_MORE_CALLBACKS);
	itemCompareCallback = new Callback (this, "itemCompareProc", 4);
	itemCompareProc = itemCompareCallback.getAddress ();
	if (itemCompareProc == 0) error (SWT.ERROR_NO_MORE_CALLBACKS);
	itemDataCallback = new Callback (this, "itemDataProc", 5);
	itemDataProc = itemDataCallback.getAddress ();
	if (itemDataProc == 0) error (SWT.ERROR_NO_MORE_CALLBACKS);
	itemNotificationCallback = new Callback (this, "itemNotificationProc", 3);
	itemNotificationProc = itemNotificationCallback.getAddress ();
	if (itemNotificationProc == 0) error (SWT.ERROR_NO_MORE_CALLBACKS);
	helpCallback = new Callback (this, "helpProc", 5);
	helpProc = helpCallback.getAddress ();
	if (helpProc == 0) error (SWT.ERROR_NO_MORE_CALLBACKS);
	hitTestCallback = new Callback (this, "hitTestProc", 5);
	hitTestProc = hitTestCallback.getAddress ();
	if (hitTestProc == 0) error (SWT.ERROR_NO_MORE_CALLBACKS);
	keyboardCallback = new Callback (this, "keyboardProc", 3);
	keyboardProc = keyboardCallback.getAddress ();
	if (keyboardProc == 0) error (SWT.ERROR_NO_MORE_CALLBACKS);
	menuCallback = new Callback (this, "menuProc", 3);
	menuProc = menuCallback.getAddress ();
	if (menuProc == 0) error (SWT.ERROR_NO_MORE_CALLBACKS);
	mouseHoverCallback = new Callback (this, "mouseHoverProc", 2);
	mouseHoverProc = mouseHoverCallback.getAddress ();
	if (mouseHoverProc == 0) error (SWT.ERROR_NO_MORE_CALLBACKS);
	mouseCallback = new Callback (this, "mouseProc", 3);
	mouseProc = mouseCallback.getAddress ();
	if (mouseProc == 0) error (SWT.ERROR_NO_MORE_CALLBACKS);
	timerCallback = new Callback (this, "timerProc", 2);
	timerProc = timerCallback.getAddress ();
	if (timerProc == 0) error (SWT.ERROR_NO_MORE_CALLBACKS);
	trackingCallback = new Callback (this, "trackingProc", 6);
	trackingProc = trackingCallback.getAddress ();
	if (trackingProc == 0) error (SWT.ERROR_NO_MORE_CALLBACKS);
	windowCallback = new Callback (this, "windowProc", 3);
	windowProc = windowCallback.getAddress ();
	if (windowProc == 0) error (SWT.ERROR_NO_MORE_CALLBACKS);
	colorCallback = new Callback (this, "colorProc", 4);
	colorProc = colorCallback.getAddress ();
	if (colorProc == 0) error (SWT.ERROR_NO_MORE_CALLBACKS);
	textInputCallback = new Callback (this, "textInputProc", 3);
	textInputProc = textInputCallback.getAddress ();
	if (textInputProc == 0) error (SWT.ERROR_NO_MORE_CALLBACKS);
	appearanceCallback = new Callback (this, "appearanceProc", 3);
	appearanceProc = appearanceCallback.getAddress ();
	if (appearanceProc == 0) error (SWT.ERROR_NO_MORE_CALLBACKS);

	/* Install Event Handlers */
	int[] mask1 = new int[] {
		OS.kEventClassCommand, OS.kEventProcessCommand,
	};
	int appTarget = OS.GetApplicationEventTarget ();
	OS.InstallEventHandler (appTarget, commandProc, mask1.length / 2, mask1, 0, null);
	int[] mask2 = new int[] {
		OS.kEventClassMouse, OS.kEventMouseDown,
		OS.kEventClassMouse, OS.kEventMouseDragged,
//		OS.kEventClassMouse, OS.kEventMouseEntered,
//		OS.kEventClassMouse, OS.kEventMouseExited,
		OS.kEventClassMouse, OS.kEventMouseMoved,
		OS.kEventClassMouse, OS.kEventMouseUp,
		OS.kEventClassMouse, OS.kEventMouseWheelMoved,
	};
	OS.InstallEventHandler (appTarget, mouseProc, mask2.length / 2, mask2, 0, null);
	int [] mask3 = new int[] {
		OS.kEventClassApplication, OS.kEventAppAvailableWindowBoundsChanged,
		OS.kEventClassAppleEvent, OS.kEventAppleEvent,
	};
	OS.InstallEventHandler (appTarget, appleEventProc, mask3.length / 2, mask3, 0, null);
	int [] mask4 = new int[] {
		OS.kEventClassKeyboard, OS.kEventRawKeyDown,
		OS.kEventClassKeyboard, OS.kEventRawKeyModifiersChanged,
		OS.kEventClassKeyboard, OS.kEventRawKeyRepeat,
		OS.kEventClassKeyboard, OS.kEventRawKeyUp,
	};
	int focusTarget = OS.GetUserFocusEventTarget ();
	OS.InstallEventHandler (focusTarget, keyboardProc, mask4.length / 2, mask4, 0, null);
	int [] mask5 = new int[] {
		OS.kEventClassTextInput, OS.kEventTextInputUnicodeForKeyEvent,
	};
	OS.InstallEventHandler (focusTarget, textInputProc, mask5.length / 2, mask5, 0, null);
	OS.AEInstallEventHandler (OS.kAppearanceEventClass, OS.kAEAppearanceChanged, appearanceProc, 0, false);
	OS.AEInstallEventHandler (OS.kAppearanceEventClass, OS.kAESmallSystemFontChanged, appearanceProc, 0, false);
	OS.AEInstallEventHandler (OS.kAppearanceEventClass, OS.kAESystemFontChanged, appearanceProc, 0, false);
	OS.AEInstallEventHandler (OS.kAppearanceEventClass, OS.kAEViewsFontChanged, appearanceProc, 0, false);
}

void initializeFonts () {
	//TEMPORARY CODE
	smallFonts = System.getProperty("org.eclipse.swt.internal.carbon.smallFonts") != null;
	noFocusRing = System.getProperty("org.eclipse.swt.internal.carbon.noFocusRing") != null;
}

void initializeInsets () {
	int [] outControl = new int [1];
	Rect rect = new Rect ();
	rect.right = rect.bottom = (short) 200;
	
	OS.CreatePushButtonControl (0, rect, 0, outControl);
	buttonInset = computeInset (outControl [0]);
	OS.DisposeControl (outControl [0]);
	
	OS.CreateTabsControl (0, rect, (short)OS.kControlTabSizeLarge, (short)OS.kControlTabDirectionNorth, (short) 0, 0, outControl);
	tabFolderNorthInset = computeInset (outControl [0]);
	OS.DisposeControl (outControl [0]);

	OS.CreateTabsControl (0, rect, (short)OS.kControlTabSizeLarge, (short)OS.kControlTabDirectionSouth, (short) 0, 0, outControl);
	tabFolderSouthInset = computeInset (outControl [0]);
	OS.DisposeControl (outControl [0]);

	/* For some reason, this code calculates insets too big. */
//	OS.CreateEditUnicodeTextControl (0, rect, 0, false, null, outControl);
//	editTextInset = computeInset (outControl [0]);
//	OS.DisposeControl (outControl [0]);	
	editTextInset = new Rect ();
	int [] outMetric = new int [1];
	OS.GetThemeMetric (OS.kThemeMetricFocusRectOutset, outMetric);
	int inset = outMetric [0]; 
	OS.GetThemeMetric (OS.kThemeMetricEditTextFrameOutset, outMetric);
	inset += outMetric [0];
	editTextInset.left = editTextInset.top = editTextInset.right = editTextInset.bottom = (short) inset;

	CGRect cgRect = new CGRect ();
	cgRect.width = cgRect.height = 200;
	int inAttributes = OS.kHIComboBoxAutoCompletionAttribute | OS.kHIComboBoxAutoSizeListAttribute;
	OS.HIComboBoxCreate (cgRect, 0, null, 0, inAttributes, outControl);
	comboInset = computeInset (outControl [0]);
	 //FIXME - 
	comboInset.bottom = comboInset.top;
	OS.DisposeControl (outControl [0]);
}

void initializeWidgetTable () {
	property = new int [1];
	indexTable = new int [GROW_SIZE];
	widgetTable = new Widget [GROW_SIZE];
	for (int i=0; i<GROW_SIZE-1; i++) indexTable [i] = i + 1;
	indexTable [GROW_SIZE - 1] = -1;
}

/**	 
 * Invokes platform specific functionality to allocate a new GC handle.
 * <p>
 * <b>IMPORTANT:</b> This method is <em>not</em> part of the public
 * API for <code>Display</code>. It is marked public only so that it
 * can be shared within the packages provided by SWT. It is not
 * available on all platforms, and should never be called from
 * application code.
 * </p>
 *
 * @param data the platform specific GC data 
 * @return the platform specific GC handle
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 * @exception SWTError <ul>
 *    <li>ERROR_NO_HANDLES if a handle could not be obtained for gc creation</li>
 * </ul>
 */
public int internal_new_GC (GCData data) {
	if (isDisposed()) SWT.error(SWT.ERROR_DEVICE_DISPOSED);
	//TODO - multiple monitors
	int window = gcWindow;
	if (window == 0) {
		window = gcWindow = createOverlayWindow ();
	} else {
		int gdevice = OS.GetMainDevice ();
		int [] ptr = new int [1];
		OS.memcpy (ptr, gdevice, 4);
		GDevice device = new GDevice ();
		OS.memcpy (device, ptr [0], GDevice.sizeof);
		Rect rect = new Rect ();	
		OS.SetRect (rect, device.left, device.top, device.right, device.bottom);
		OS.SetWindowBounds (window, (short) OS.kWindowStructureRgn, rect);
	}
	int port = OS.GetWindowPort (window);
	int [] buffer = new int [1];
	OS.CreateCGContextForPort (port, buffer);
	int context = buffer [0];
	if (context == 0) SWT.error (SWT.ERROR_NO_HANDLES);
	Rect portRect = new Rect ();
	OS.GetPortBounds (port, portRect);
	OS.CGContextScaleCTM (context, 1, -1);
	OS.CGContextTranslateCTM (context, 0, portRect.top - portRect.bottom);
	if (data != null) {
		int mask = SWT.LEFT_TO_RIGHT | SWT.RIGHT_TO_LEFT;
		if ((data.style & mask) == 0) {
			data.style |= SWT.LEFT_TO_RIGHT;
		}
		data.device = this;
		data.window = window;
		data.background = getSystemColor (SWT.COLOR_WHITE).handle;
		data.foreground = getSystemColor (SWT.COLOR_BLACK).handle;
		data.font = getSystemFont ();
		data.updateClip = true;
	} else {
		OS.ShowWindow (window);
	}
	return context;
}

/**	 
 * Invokes platform specific functionality to dispose a GC handle.
 * <p>
 * <b>IMPORTANT:</b> This method is <em>not</em> part of the public
 * API for <code>Display</code>. It is marked public only so that it
 * can be shared within the packages provided by SWT. It is not
 * available on all platforms, and should never be called from
 * application code.
 * </p>
 *
 * @param hDC the platform specific GC handle
 * @param data the platform specific GC data 
 */
public void internal_dispose_GC (int context, GCData data) {
	if (isDisposed()) SWT.error(SWT.ERROR_DEVICE_DISPOSED);
	if (data != null) {
		int window = data.window;
		if (gcWindow == window) {
			OS.HideWindow (window);
		} else {
			OS.DisposeWindow (window);
		}
		data.window = 0;
	}
	
	/*
	* This code is intentionaly commented. Use CGContextSynchronize
	* instead of CGContextFlush to improve performance.
	*/
//	OS.CGContextFlush (context);
	OS.CGContextSynchronize (context);
	OS.CGContextRelease (context);
}

static boolean isValidClass (Class clazz) {
	String name = clazz.getName ();
	int index = name.lastIndexOf ('.');
	return name.substring (0, index + 1).equals (PACKAGE_PREFIX);
}

boolean isValidThread () {
	return thread == Thread.currentThread ();
}

int itemCompareProc (int browser, int itemOne, int itemTwo, int sortProperty) {
	Widget widget = getWidget (browser);
	if (widget != null) return widget.itemCompareProc (browser, itemOne, itemTwo, sortProperty);
	return OS.noErr;
}

int itemDataProc (int browser, int item, int property, int itemData, int setValue) {
	Widget widget = getWidget (browser);
	if (widget != null) return widget.itemDataProc (browser, item, property, itemData, setValue);
	return OS.noErr;
}

int itemNotificationProc (int browser, int item, int message) {
	Widget widget = getWidget (browser);
	if (widget != null) return widget.itemNotificationProc (browser, item, message);
	return OS.noErr;
}

int keyboardProc (int nextHandler, int theEvent, int userData) {	
	int theWindow = OS.GetUserFocusWindow ();
	if (theWindow != 0) {
		int [] theControl = new int [1];
		OS.GetKeyboardFocus (theWindow, theControl);
		Widget widget = getWidget (theControl [0]);
		if (widget != null) return widget.keyboardProc (nextHandler, theEvent, userData);
	}
	return OS.eventNotHandledErr;
}

/**
 * Generate a low level system event.
 * 
 * <code>post</code> is used to generate low level keyboard
 * and mouse events. The intent is to enable automated UI
 * testing by simulating the input from the user.  Most
 * SWT applications should never need to call this method.
 * <p>
 * Note that this operation can fail when the operating system
 * fails to generate the event for any reason.  For example,
 * this can happen when there is no such key or mouse button
 * or when the system event queue is full.
 * </p>
 * <p>
 * <b>Event Types:</b>
 * <p>KeyDown, KeyUp
 * <p>The following fields in the <code>Event</code> apply:
 * <ul>
 * <li>(in) type KeyDown or KeyUp</li>
 * <p> Either one of:
 * <li>(in) character a character that corresponds to a keyboard key</li>
 * <li>(in) keyCode the key code of the key that was typed,
 *          as defined by the key code constants in class <code>SWT</code></li>
 * </ul>
 * <p>MouseDown, MouseUp</p>
 * <p>The following fields in the <code>Event</code> apply:
 * <ul>
 * <li>(in) type MouseDown or MouseUp
 * <li>(in) button the button that is pressed or released
 * </ul>
 * <p>MouseMove</p>
 * <p>The following fields in the <code>Event</code> apply:
 * <ul>
 * <li>(in) type MouseMove
 * <li>(in) x the x coordinate to move the mouse pointer to in screen coordinates
 * <li>(in) y the y coordinate to move the mouse pointer to in screen coordinates
 * </ul>
 * </dl>
 * 
 * @param event the event to be generated
 * 
 * @return true if the event was generated or false otherwise
 * 
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the event is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 *
 * @since 3.0
 * 
 */
public boolean post(Event event) {
	if (isDisposed ()) error (SWT.ERROR_DEVICE_DISPOSED);
	if (event == null) error (SWT.ERROR_NULL_ARGUMENT);
	int type = event.type;
	switch (type) {
		case SWT.KeyDown:
		case SWT.KeyUp: {
			int vKey = Display.untranslateKey (event.keyCode);
			if (vKey != 0) {
				return OS.CGPostKeyboardEvent (0, vKey, type == SWT.KeyDown) == 0;
			} else {
				vKey = -1;
				int kchrPtr = OS.GetScriptManagerVariable ((short) OS.smKCHRCache);
				int key = -1;
				int [] state = new int [1];
				int [] encoding = new int [1];
				short keyScript = (short) OS.GetScriptManagerVariable ((short) OS.smKeyScript);
				short regionCode = (short) OS.GetScriptManagerVariable ((short) OS.smRegionCode);
				if (OS.UpgradeScriptInfoToTextEncoding (keyScript, (short) OS.kTextLanguageDontCare, regionCode, null, encoding) == OS.paramErr) {
					if (OS.UpgradeScriptInfoToTextEncoding (keyScript, (short) OS.kTextLanguageDontCare, (short) OS.kTextRegionDontCare, null, encoding) == OS.paramErr) {
						encoding [0] = OS.kTextEncodingMacRoman;
					}
				}
				int [] encodingInfo = new int [1];
				OS.CreateUnicodeToTextInfoByEncoding (encoding [0], encodingInfo);
				if (encodingInfo [0] != 0) {
					char [] input = {event.character};
					byte [] buffer = new byte [2];
					OS.ConvertFromUnicodeToPString (encodingInfo [0], 2, input, buffer);
					OS.DisposeUnicodeToTextInfo (encodingInfo);
					key = buffer [1] & 0x7f;
				}
				if (key == -1) return false;				
				for (int i = 0 ; i <= 0x7F ; i++) {
					int result1 = OS.KeyTranslate (kchrPtr, (short) (i | 512), state);
					int result2 = OS.KeyTranslate (kchrPtr, (short) i, state);
					if ((result1 & 0x7f) == key || (result2 & 0x7f) == key) {
						vKey = i;
						break;
					}
				}
				if (vKey == -1) return false;
				return OS.CGPostKeyboardEvent (key, vKey, type == SWT.KeyDown) == 0;
			}
		}
		case SWT.MouseDown:
		case SWT.MouseMove: 
		case SWT.MouseUp: {
			CGPoint mouseCursorPosition = new CGPoint ();
			int chord = OS.GetCurrentEventButtonState ();
			if (type == SWT.MouseMove) {
				mouseCursorPosition.x = event.x;
				mouseCursorPosition.y = event.y;
				return OS.CGPostMouseEvent (mouseCursorPosition, true, 3, (chord & 0x1) != 0, (chord & 0x2) != 0, (chord & 0x4) != 0) == 0;
			} else {
				int button = event.button;
				if (button < 1 || button > 3) return false;
				boolean button1 = false, button2 = false, button3 = false;
				switch (button) {
					case 1: {
						button1 = type == SWT.MouseDown;
						button2 = (chord & 0x4) != 0;
						button3 = (chord & 0x2) != 0;
						break;
					}
					case 2: {
						button1 = (chord & 0x1) != 0;
						button2 = type == SWT.MouseDown;
						button3 = (chord & 0x2) != 0;
						break;
					}
					case 3: {
						button1 = (chord & 0x1) != 0;
						button2 = (chord & 0x4) != 0;
						button3 = type == SWT.MouseDown;
						break;
					}
				}
				org.eclipse.swt.internal.carbon.Point pt = new org.eclipse.swt.internal.carbon.Point ();
				OS.GetGlobalMouse (pt);
				mouseCursorPosition.x = pt.h;
				mouseCursorPosition.y = pt.v;
				return OS.CGPostMouseEvent (mouseCursorPosition, true, 3, button1, button3, button2) == 0;
			}
		}
	} 
	return false;
}

void postEvent (Event event) {
	/*
	* Place the event at the end of the event queue.
	* This code is always called in the Display's
	* thread so it must be re-enterant but does not
	* need to be synchronized.
	*/
	if (eventQueue == null) eventQueue = new Event [4];
	int index = 0;
	int length = eventQueue.length;
	while (index < length) {
		if (eventQueue [index] == null) break;
		index++;
	}
	if (index == length) {
		Event [] newQueue = new Event [length + 4];
		System.arraycopy (eventQueue, 0, newQueue, 0, length);
		eventQueue = newQueue;
	}
	eventQueue [index] = event;
}

/**
 * Maps a point from one coordinate system to another.
 * When the control is null, coordinates are mapped to
 * the display.
 * <p>
 * NOTE: On right-to-left platforms where the coordinate
 * systems are mirrored, special care needs to be taken
 * when mapping coordinates from one control to another
 * to ensure the result is correctly mirrored.
 * 
 * Mapping a point that is the origin of a rectangle and
 * then adding the width and height is not equivalent to
 * mapping the rectangle.  When one control is mirrored
 * and the other is not, adding the width and height to a
 * point that was mapped causes the rectangle to extend
 * in the wrong direction.  Mapping the entire rectangle
 * instead of just one point causes both the origin and
 * the corner of the rectangle to be mapped.
 * </p>
 * 
 * @param from the source <code>Control</code> or <code>null</code>
 * @param to the destination <code>Control</code> or <code>null</code>
 * @param point to be mapped 
 * @return point with mapped coordinates 
 * 
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the point is null</li>
 *    <li>ERROR_INVALID_ARGUMENT - if the Control from or the Control to have been disposed</li> 
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 * 
 * @since 2.1.2
 */
public Point map (Control from, Control to, Point point) {
	checkDevice ();
	if (point == null) error (SWT.ERROR_NULL_ARGUMENT);	
	return map (from, to, point.x, point.y);
}

/**
 * Maps a point from one coordinate system to another.
 * When the control is null, coordinates are mapped to
 * the display.
 * <p>
 * NOTE: On right-to-left platforms where the coordinate
 * systems are mirrored, special care needs to be taken
 * when mapping coordinates from one control to another
 * to ensure the result is correctly mirrored.
 * 
 * Mapping a point that is the origin of a rectangle and
 * then adding the width and height is not equivalent to
 * mapping the rectangle.  When one control is mirrored
 * and the other is not, adding the width and height to a
 * point that was mapped causes the rectangle to extend
 * in the wrong direction.  Mapping the entire rectangle
 * instead of just one point causes both the origin and
 * the corner of the rectangle to be mapped.
 * </p>
 * 
 * @param from the source <code>Control</code> or <code>null</code>
 * @param to the destination <code>Control</code> or <code>null</code>
 * @param x coordinates to be mapped
 * @param y coordinates to be mapped
 * @return point with mapped coordinates
 * 
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_ARGUMENT - if the Control from or the Control to have been disposed</li> 
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 * 
 * @since 2.1.2
 */
public Point map (Control from, Control to, int x, int y) {
	checkDevice ();
	if (from != null && from.isDisposed()) error (SWT.ERROR_INVALID_ARGUMENT);
	if (to != null && to.isDisposed()) error (SWT.ERROR_INVALID_ARGUMENT);
	Point point = new Point (x, y);
	Rect rect = new Rect ();
	if (from != null) {
		int window = OS.GetControlOwner (from.handle);
		if (OS.HIVIEW) {
			CGPoint pt = new CGPoint ();
			OS.HIViewConvertPoint (pt, from.handle, 0);
			point.x += (int) pt.x;
			point.y += (int) pt.y;
			OS.GetWindowBounds (window, (short) OS.kWindowStructureRgn, rect);
		} else {
			OS.GetControlBounds (from.handle, rect);
			point.x += rect.left;
			point.y += rect.top;
			OS.GetWindowBounds (window, (short) OS.kWindowContentRgn, rect);
		}
		point.x += rect.left;
		point.y += rect.top;
		Rect inset = from.getInset ();
		point.x -= inset.left; 
		point.y -= inset.top;
	}
	if (to != null) {
		int window = OS.GetControlOwner (to.handle);
		if (OS.HIVIEW) {
			CGPoint pt = new CGPoint ();
			OS.HIViewConvertPoint (pt, to.handle, 0);
			point.x -= (int) pt.x;
			point.y -= (int) pt.y;
			OS.GetWindowBounds (window, (short) OS.kWindowStructureRgn, rect);
		} else {
			OS.GetControlBounds (to.handle, rect);
			point.x -= rect.left;
			point.y -= rect.top;
			OS.GetWindowBounds (window, (short) OS.kWindowContentRgn, rect);
		}
		point.x -= rect.left;
		point.y -= rect.top;
		Rect inset = to.getInset ();
		point.x += inset.left; 
		point.y += inset.top;
	}
	return point;
}

/**
 * Maps a point from one coordinate system to another.
 * When the control is null, coordinates are mapped to
 * the display.
 * <p>
 * NOTE: On right-to-left platforms where the coordinate
 * systems are mirrored, special care needs to be taken
 * when mapping coordinates from one control to another
 * to ensure the result is correctly mirrored.
 * 
 * Mapping a point that is the origin of a rectangle and
 * then adding the width and height is not equivalent to
 * mapping the rectangle.  When one control is mirrored
 * and the other is not, adding the width and height to a
 * point that was mapped causes the rectangle to extend
 * in the wrong direction.  Mapping the entire rectangle
 * instead of just one point causes both the origin and
 * the corner of the rectangle to be mapped.
 * </p>
 * 
 * @param from the source <code>Control</code> or <code>null</code>
 * @param to the destination <code>Control</code> or <code>null</code>
 * @param rectangle to be mapped
 * @return rectangle with mapped coordinates
 * 
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the rectangle is null</li>
 *    <li>ERROR_INVALID_ARGUMENT - if the Control from or the Control to have been disposed</li> 
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 * 
 * @since 2.1.2
 */
public Rectangle map (Control from, Control to, Rectangle rectangle) {
	checkDevice ();
	if (rectangle == null) error (SWT.ERROR_NULL_ARGUMENT);	
	return map (from, to, rectangle.x, rectangle.y, rectangle.width, rectangle.height);
}

/**
 * Maps a point from one coordinate system to another.
 * When the control is null, coordinates are mapped to
 * the display.
 * <p>
 * NOTE: On right-to-left platforms where the coordinate
 * systems are mirrored, special care needs to be taken
 * when mapping coordinates from one control to another
 * to ensure the result is correctly mirrored.
 * 
 * Mapping a point that is the origin of a rectangle and
 * then adding the width and height is not equivalent to
 * mapping the rectangle.  When one control is mirrored
 * and the other is not, adding the width and height to a
 * point that was mapped causes the rectangle to extend
 * in the wrong direction.  Mapping the entire rectangle
 * instead of just one point causes both the origin and
 * the corner of the rectangle to be mapped.
 * </p>
 * 
 * @param from the source <code>Control</code> or <code>null</code>
 * @param to the destination <code>Control</code> or <code>null</code>
 * @param x coordinates to be mapped
 * @param y coordinates to be mapped
 * @param width coordinates to be mapped
 * @param height coordinates to be mapped
 * @return rectangle with mapped coordinates
 * 
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_INVALID_ARGUMENT - if the Control from or the Control to have been disposed</li> 
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 * 
 * @since 2.1.2
 */
public Rectangle map (Control from, Control to, int x, int y, int width, int height) {
	checkDevice ();
	if (from != null && from.isDisposed()) error (SWT.ERROR_INVALID_ARGUMENT);
	if (to != null && to.isDisposed()) error (SWT.ERROR_INVALID_ARGUMENT);
	Rectangle rectangle = new Rectangle (x, y, width, height);
	Rect rect = new Rect ();
	if (from != null) {
		int window = OS.GetControlOwner (from.handle);
		if (OS.HIVIEW) {
			CGPoint pt = new CGPoint ();
			OS.HIViewConvertPoint (pt, from.handle, 0);
			rectangle.x += (int) pt.x;
			rectangle.y += (int) pt.y;
			OS.GetWindowBounds (window, (short) OS.kWindowStructureRgn, rect);
		} else {
			OS.GetControlBounds (from.handle, rect);
			rectangle.x += rect.left;
			rectangle.y += rect.top;
			OS.GetWindowBounds (window, (short) OS.kWindowContentRgn, rect);
		}
		rectangle.x += rect.left;
		rectangle.y += rect.top;
		Rect inset = from.getInset ();
		rectangle.x -= inset.left; 
		rectangle.y -= inset.top;
	}
	if (to != null) {
		int window = OS.GetControlOwner (to.handle);
		if (OS.HIVIEW) {
			CGPoint pt = new CGPoint ();
			OS.HIViewConvertPoint (pt, to.handle, 0);
			rectangle.x -= (int) pt.x;
			rectangle.y -= (int) pt.y;
			OS.GetWindowBounds (window, (short) OS.kWindowStructureRgn, rect);
		} else {
			OS.GetControlBounds (to.handle, rect);
			rectangle.x -= rect.left;
			rectangle.y -= rect.top;
			OS.GetWindowBounds (window, (short) OS.kWindowContentRgn, rect);
		}
		rectangle.x -= rect.left;
		rectangle.y -= rect.top;
		Rect inset = to.getInset ();
		rectangle.x += inset.left; 
		rectangle.y += inset.top;
	}
	return rectangle;
}
	
int menuProc (int nextHandler, int theEvent, int userData) {
	short menuID = 0;
	if (userData != 0) {
		menuID = OS.GetMenuID (userData);
	} else {
		int [] theMenu = new int [1];
		OS.GetEventParameter (theEvent, OS.kEventParamDirectObject, OS.typeMenuRef, null, 4, null, theMenu);
		menuID = OS.GetMenuID (theMenu [0]);
	}
	Menu menu = getMenu (menuID);
	if (menu != null) return menu.menuProc (nextHandler, theEvent, userData);
	return OS.eventNotHandledErr;
}

int mouseProc (int nextHandler, int theEvent, int userData) {
	int eventKind = OS.GetEventKind (theEvent);
	switch (eventKind) {
		case OS.kEventMouseDown:
			short [] buttonData = new short [1];
			OS.GetEventParameter (theEvent, OS.kEventParamMouseButton, OS.typeMouseButton, null, 2, null, buttonData);
			int [] clickCountData = new int [1];
			OS.GetEventParameter (theEvent, OS.kEventParamClickCount, OS.typeUInt32, null, 4, null, clickCountData);
			clickCount = clickCountButton == buttonData [0] ? clickCountData [0] : 1;
			clickCountButton = buttonData [0];
			break;
		case OS.kEventMouseDragged:
		case OS.kEventMouseMoved:
			mouseMoved = true;
	}
	if (mouseUpControl != null && eventKind == OS.kEventMouseUp) {
		if (!mouseUpControl.isDisposed ()) {
			mouseUpControl.mouseProc (nextHandler, theEvent, userData);
			mouseUpControl = null;
			return OS.noErr;
		}
		mouseUpControl = null;
	}
	int sizeof = org.eclipse.swt.internal.carbon.Point.sizeof;
	org.eclipse.swt.internal.carbon.Point where = new org.eclipse.swt.internal.carbon.Point ();
	OS.GetEventParameter (theEvent, OS.kEventParamMouseLocation, OS.typeQDPoint, null, sizeof, null, where);
	int [] theWindow = new int [1];
	int part = OS.FindWindow (where, theWindow);
	switch (part) {
		case OS.inMenuBar: {
			if (eventKind == OS.kEventMouseDown) {
				clearMenuFlags ();
				if (menuBar == null || menuBar.isEnabled ()) {
					OS.MenuSelect (where);
				}					 
				clearMenuFlags ();
				return OS.noErr;
			}
			break;
		}
		case OS.inContent: {
			Rect windowRect = new Rect ();
			OS.GetWindowBounds (theWindow [0], (short) OS.kWindowContentRgn, windowRect);
			CGPoint inPoint = new CGPoint ();
			inPoint.x = where.h - windowRect.left;
			inPoint.y = where.v - windowRect.top;
			if (OS.HIVIEW) {
				int root = OS.HIViewGetRoot (theWindow [0]);
				int [] buffer = new int [1];
				OS.HIViewGetViewForMouseEvent (root, theEvent, buffer);
				int view = buffer [0];
				OS.HIViewFindByID (root, OS.kHIViewWindowContentID (), buffer);
				int contentView = buffer [0]; 
				while (view != 0 && view != contentView && !OS.IsControlEnabled (view)) {	
					view = OS.HIViewGetSuperview (view);
				}
				Widget widget = null;
				boolean consume = false;
				do {
					widget = getWidget (view);
					if (widget != null) {
						if (widget.isEnabled ()) break;
						consume = true;
					}
					view = OS.HIViewGetSuperview (view);
				} while (view != 0 && view != contentView);
				if (widget != null) {
					if (widget.contains ((int) inPoint.x, (int) inPoint.y)) {
						int result = userData != 0 ? widget.mouseProc (nextHandler, theEvent, userData) : OS.eventNotHandledErr;
						return consume ? OS.noErr : result;
					}
				}
			} else {
				int [] theRoot = new int [1];
				OS.GetRootControl (theWindow [0], theRoot);
				int [] theControl = new int [1];
				OS.HIViewGetSubviewHit (theRoot [0], inPoint, true, theControl);
				while (theControl [0] != 0 && !OS.IsControlEnabled (theControl [0])) {				
					OS.GetSuperControl (theControl [0], theControl);
				}
				Widget widget = null;
				boolean consume = false;
				if (theControl [0] == 0) theControl [0] = theRoot [0];
				do {
					widget = getWidget (theControl [0]);
					if (widget != null) {
						if (widget.isEnabled ()) break;
						consume = true;
					}
					OS.GetSuperControl (theControl [0], theControl);
				} while (theControl [0] != 0);
				if (theControl [0] == 0) widget = getWidget (theRoot [0]);
				if (widget != null) {
					if (widget.contains ((int) inPoint.x, (int) inPoint.y)) {
						int result = userData != 0 ? widget.mouseProc (nextHandler, theEvent, userData) : OS.eventNotHandledErr;
						return consume ? OS.noErr : result;
					}
				}
			}
			break;
		}
	}
	switch (eventKind) {
		case OS.kEventMouseDragged:
		case OS.kEventMouseMoved:  OS.InitCursor ();
	}
	return OS.eventNotHandledErr;
}

int mouseHoverProc (int id, int handle) {
	OS.RemoveEventLoopTimer (id);
	mouseHoverID = 0;
	mouseMoved = false;
	if (currentControl != null && !currentControl.isDisposed ()) {
		//OPTIMIZE - use OS calls
		int chord = OS.GetCurrentEventButtonState ();
		int modifiers = OS.GetCurrentEventKeyModifiers ();
		Point pt = currentControl.toControl (getCursorLocation ());
		currentControl.sendMouseEvent (SWT.MouseHover, (short)0, true, chord, (short)pt.x, (short)pt.y, modifiers);
	}
	return 0;
}

int[] readImageRef(int path) {
	int[] image = null;
	int url = OS.CFURLCreateFromFileSystemRepresentation(OS.kCFAllocatorDefault, path, OS.strlen(path), false);
	if (url != 0) {
		int extention = OS.CFURLCopyPathExtension(url);
		if (extention != 0) {
			int length = OS.CFStringGetLength(extention);
			char[] buffer = new char[length];
			CFRange range = new CFRange();
			range.length = length;
			OS.CFStringGetCharacters(extention, range, buffer);
			String ext = new String(buffer);
			if (ext.equalsIgnoreCase("png")) {
				int provider = OS.CGDataProviderCreateWithURL(url);
				if (provider != 0) {
					image = new int[]{OS.CGImageCreateWithPNGDataProvider(provider, null, true, OS.kCGRenderingIntentDefault), 0};
					OS.CGDataProviderRelease(provider);
				}
			} else if (ext.equalsIgnoreCase("jpeg") || ext.equals("jpg")) {
				int provider = OS.CGDataProviderCreateWithURL(url);
				if (provider != 0) {
					image = new int[]{OS.CGImageCreateWithJPEGDataProvider(provider, null, true, OS.kCGRenderingIntentDefault), 0};
					OS.CGDataProviderRelease(provider);
				}
			} else if (ext.equalsIgnoreCase("icns")) {
				byte[] fsRef = new byte[80];
				if (OS.CFURLGetFSRef(url, fsRef)) {
					byte[] fsSpec = new byte[70];
					if (OS.FSGetCatalogInfo(fsRef, 0, null, null, fsSpec, null) == OS.noErr) {
						int[] iconFamily = new int[1];
						OS.ReadIconFile(fsSpec, iconFamily);						
						if (iconFamily[0] != 0) {
							image = createImageFromFamily(iconFamily[0], OS.kThumbnail32BitData, OS.kThumbnail8BitMask, 128, 128);
							OS.DisposeHandle(iconFamily[0]);
						}
					}
				}
			}
			OS.CFRelease(extention);
		}
		OS.CFRelease(url);
	}
	return image;
}


/**
 * Reads an event from the operating system's event queue,
 * dispatches it appropriately, and returns <code>true</code>
 * if there is potentially more work to do, or <code>false</code>
 * if the caller can sleep until another event is placed on
 * the event queue.
 * <p>
 * In addition to checking the system event queue, this method also
 * checks if any inter-thread messages (created by <code>syncExec()</code>
 * or <code>asyncExec()</code>) are waiting to be processed, and if
 * so handles them before returning.
 * </p>
 *
 * @return <code>false</code> if the caller can sleep upon return from this method
 *
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_FAILED_EXEC - if an exception occurred while running an inter-thread message</li>
 * </ul>
 *
 * @see #sleep
 * @see #wake
 */
public boolean readAndDispatch () {
	checkDevice ();
	boolean events = false;
	events |= runSettings ();
	events |= runTimers ();
	events |= runEnterExit ();
	events |= runPopups ();
	events |= runGrabs ();
	int [] outEvent  = new int [1];
	int status = OS.ReceiveNextEvent (0, null, OS.kEventDurationNoWait, true, outEvent);
	if (status == OS.noErr) {
		events = true;
		int eventClass = OS.GetEventClass (outEvent [0]);
		int eventKind = OS.GetEventKind (outEvent [0]);
		OS.SendEventToEventTarget (outEvent [0], OS.GetEventDispatcherTarget ());
		OS.ReleaseEvent (outEvent [0]);

		/*
		* Feature in the Macintosh.  HIComboBox does not send any
		* notification when the selection changes.  The fix is to
		* detect if the combo text has changed after every event
		* has been dispatched.  This is only necessary when the
		* combo has focus. 
		*/
		if (focusCombo != null && !focusCombo.isDisposed ()) {
			focusCombo.checkSelection ();
		}

		/*
		* Feature in the Macintosh.  When an indeterminate progress
		* bar is running, it floods the event queue with messages in
		* order to show the animation.  This means that async messages
		* will never run because there are always messages from the
		* operating system.  The fix is to run async messages when ever
		* there is a wake message.
		*
		* NOTE:  This is not the correct behavior.  Operating system
		* messages are supposed to have priority over async messages.
		*/
		if (eventClass == WAKE_CLASS && eventKind == WAKE_KIND) {
			runAsyncMessages (false);
		}
	}
	events |= runPaint ();
	if (events) {
		runDeferredEvents ();
		return true;
	}
	return runAsyncMessages (false);
}

static synchronized void register (Display display) {
	for (int i=0; i<Displays.length; i++) {
		if (Displays [i] == null) {
			Displays [i] = display;
			return;
		}
	}
	Display [] newDisplays = new Display [Displays.length + 4];
	System.arraycopy (Displays, 0, newDisplays, 0, Displays.length);
	newDisplays [Displays.length] = display;
	Displays = newDisplays;
}

/**
 * Releases any internal resources back to the operating
 * system and clears all fields except the device handle.
 * <p>
 * Disposes all shells which are currently open on the display. 
 * After this method has been invoked, all related related shells
 * will answer <code>true</code> when sent the message
 * <code>isDisposed()</code>.
 * </p><p>
 * When a device is destroyed, resources that were acquired
 * on behalf of the programmer need to be returned to the
 * operating system.  For example, if the device allocated a
 * font to be used as the system font, this font would be
 * freed in <code>release</code>.  Also,to assist the garbage
 * collector and minimize the amount of memory that is not
 * reclaimed when the programmer keeps a reference to a
 * disposed device, all fields except the handle are zero'd.
 * The handle is needed by <code>destroy</code>.
 * </p>
 * This method is called before <code>destroy</code>.
 * 
 * @see Device#dispose
 * @see #destroy
 */
protected void release () {
	sendEvent (SWT.Dispose, new Event ());
	Shell [] shells = getShells ();
	for (int i=0; i<shells.length; i++) {
		Shell shell = shells [i];
		if (!shell.isDisposed ()) shell.dispose ();
	}
	if (tray != null) tray.dispose ();
	tray = null;
	while (readAndDispatch ()) {}
	if (disposeList != null) {
		for (int i=0; i<disposeList.length; i++) {
			if (disposeList [i] != null) disposeList [i].run ();
		}
	}
	disposeList = null;
	synchronizer.releaseSynchronizer ();
	synchronizer = null;
	releaseDisplay ();
	super.release ();
}

void releaseDisplay () {
	disposeWindows ();

	if (gcWindow != 0) OS.DisposeWindow (gcWindow);
	gcWindow = 0;

	/* Release Timers */
	if (caretID != 0) OS.RemoveEventLoopTimer (caretID);
	if (mouseHoverID != 0) OS.RemoveEventLoopTimer (mouseHoverID);
	caretID = mouseHoverID = 0;
	if (timerIds != null) {
		for (int i=0; i<timerIds.length; i++) {
			if (timerIds [i] != 0 && timerIds [i] != -1) {
				 OS.RemoveEventLoopTimer (timerIds [i]);
			}
		}
	}
	timerIds = null;
	
	actionCallback.dispose ();
	appleEventCallback.dispose ();
	caretCallback.dispose ();
	commandCallback.dispose ();
	controlCallback.dispose ();
	accessibilityCallback.dispose ();
	drawItemCallback.dispose ();
	itemCompareCallback.dispose ();
	itemDataCallback.dispose ();
	itemNotificationCallback.dispose ();
	helpCallback.dispose ();
	hitTestCallback.dispose ();
	keyboardCallback.dispose ();
	menuCallback.dispose ();
	mouseHoverCallback.dispose ();
	mouseCallback.dispose ();
	trackingCallback.dispose ();
	windowCallback.dispose ();
	colorCallback.dispose ();
	textInputCallback.dispose ();
	appearanceCallback.dispose ();
	actionCallback = appleEventCallback = caretCallback = commandCallback = appearanceCallback = null;
	accessibilityCallback = controlCallback = drawItemCallback = itemDataCallback = itemNotificationCallback = null;
	helpCallback = hitTestCallback = keyboardCallback = menuCallback = itemCompareCallback = null;
	mouseHoverCallback = mouseCallback = trackingCallback = windowCallback = colorCallback = null;
	textInputCallback = null;
	actionProc = appleEventProc = caretProc = commandProc = appearanceProc = 0;
	accessibilityProc = controlProc = drawItemProc = itemDataProc = itemNotificationProc = itemCompareProc = 0;
	helpProc = hitTestProc = keyboardProc = menuProc = 0;
	mouseHoverProc = mouseProc = trackingProc = windowProc = colorProc = 0;
	textInputProc = 0;
	timerCallback.dispose ();
	timerCallback = null;
	timerProc = 0;
	grabControl = currentControl = mouseUpControl = focusControl = focusCombo = null;
	helpWidget = null;
	if (helpString != 0) OS.CFRelease (helpString);
	helpString = 0;
	menus = popups = null;
	menuBar = null;

	/* Release the System Images */
	if (errorImage != null) errorImage.dispose ();
	if (infoImage != null) infoImage.dispose ();
	if (warningImage != null) warningImage.dispose ();
	errorImage = infoImage = warningImage = null;

	/* Release the System Cursors */
	for (int i = 0; i < cursors.length; i++) {
		if (cursors [i] != null) cursors [i].dispose ();
	}
	cursors = null;
	
	/* Release Dock image */
	if (dockImage != 0) OS.CGImageRelease (dockImage);
	if (dockImageData != 0) OS.DisposePtr (dockImageData);
	dockImage = dockImageData = 0;

	//NOT DONE - call terminate TXN if this is the last display 
	//NOTE: - display create and dispose needs to be synchronized on all platforms
//	 TXNTerminateTextension ();
}

/**
 * Removes the listener from the collection of listeners who will
 * be notified when an event of the given type occurs anywhere in
 * a widget. The event type is one of the event constants defined
 * in class <code>SWT</code>.
 *
 * @param eventType the type of event to listen for
 * @param listener the listener which should no longer be notified when the event occurs
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 * </ul>
 *
 * @see Listener
 * @see SWT
 * @see #addFilter
 * @see #addListener
 * 
 * @since 3.0
 */
public void removeFilter (int eventType, Listener listener) {
	checkDevice ();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (filterTable == null) return;
	filterTable.unhook (eventType, listener);
	if (filterTable.size () == 0) filterTable = null;
}

/**
 * Removes the listener from the collection of listeners who will
 * be notified when an event of the given type occurs. The event type
 * is one of the event constants defined in class <code>SWT</code>.
 *
 * @param eventType the type of event to listen for
 * @param listener the listener which should no longer be notified when the event occurs
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the listener is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 *
 * @see Listener
 * @see SWT
 * @see #addListener
 * 
 * @since 2.0 
 */
public void removeListener (int eventType, Listener listener) {
	checkDevice ();
	if (listener == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (eventTable == null) return;
	eventTable.unhook (eventType, listener);
}

void removeMenu (Menu menu) {
	if (menus == null) return;
	menus [menu.id - ID_START] = null;
}

void removePopup (Menu menu) {
	if (popups == null) return;
	for (int i=0; i<popups.length; i++) {
		if (popups [i] == menu) {
			popups [i] = null;
			return;
		}
	}
}

Widget removeWidget (int handle) {
	if (handle == 0) return null;
	Widget widget = null;
	property [0] = 0;
	OS.GetControlProperty (handle, SWT0, SWT0, 4, null, property);
	int index = property [0] - 1;
	if (0 <= index && index < widgetTable.length) {
		widget = widgetTable [index];
		widgetTable [index] = null;
		indexTable [index] = freeSlot;
		freeSlot = index;
		OS.RemoveControlProperty (handle, SWT0, SWT0);

	}
	return widget;
}

boolean runAsyncMessages (boolean all) {
	return synchronizer.runAsyncMessages (all);
}

boolean runEnterExit () {
	//OPTIMIZE - no garbage, widget hit tested again in mouse move
	boolean eventSent = false;
	Control control = null;
	int [] theControl = new int [1];
	org.eclipse.swt.internal.carbon.Point where = new org.eclipse.swt.internal.carbon.Point ();
	OS.GetGlobalMouse (where);
	int [] theWindow = new int [1];
	if (OS.FindWindow (where, theWindow) == OS.inContent) {
		if (theWindow [0] != 0) {
			Rect rect = new Rect ();
			OS.GetWindowBounds (theWindow [0], (short) OS.kWindowContentRgn, rect);
			CGPoint inPoint = new CGPoint ();
			inPoint.x = where.h - rect.left;
			inPoint.y = where.v - rect.top;
			int [] theRoot = new int [1];
			OS.GetRootControl (theWindow [0], theRoot);
			OS.HIViewGetSubviewHit (theRoot [0], inPoint, true, theControl);
			while (theControl [0] != 0 && !OS.IsControlEnabled (theControl [0])) {				
				OS.GetSuperControl (theControl [0], theControl);
			}
			boolean propagate = true;
			if (theControl [0] != 0) {
				do {
					Widget widget = getWidget (theControl [0]);
					if (widget != null) {
						if (widget instanceof Control) {
							Control cursorControl = (Control) widget;
							if (cursorControl.isEnabled ()) {
								if (cursorControl.isEnabledModal ()) {
									if (widget.isTrimHandle (theControl [0])) {
										propagate = false;
										break;
									}
									control = cursorControl;
								}
								break;
							}
						}
					}
					OS.GetSuperControl (theControl [0], theControl);
				} while (theControl [0] != 0);
			}
			if (control == null && propagate) {
				theControl [0] = theRoot [0];
				Widget widget = getWidget (theControl [0]);
				if (widget != null && widget instanceof Control) {
					Control cursorControl = (Control) widget;
					if (cursorControl.isEnabled ()) {
						if (cursorControl.isEnabledModal ()) {
							control = cursorControl;
							theControl[0] = control.handle;
						}
					}
				}
			}
			if (control != null && !control.contains ((int) inPoint.x, (int) inPoint.y)) {
				control = null;
			}
		}
	}
	if (control != currentControl) {
		if (currentControl != null && !currentControl.isDisposed ()) {
			eventSent = true;
			int chord = OS.GetCurrentEventButtonState ();
			int modifiers = OS.GetCurrentEventKeyModifiers ();
			Point pt = currentControl.toControl (where.h, where.v);
			currentControl.sendMouseEvent (SWT.MouseExit, (short)0, true, chord, (short)pt.x, (short)pt.y, modifiers);
			if (mouseHoverID != 0) OS.RemoveEventLoopTimer (mouseHoverID);
			mouseHoverID = 0;
			mouseMoved = false;
		}
		// widget could be disposed at this point
		if (control != null && control.isDisposed()) control = null;
		if ((currentControl = control) != null) {
			eventSent = true;
			int chord = OS.GetCurrentEventButtonState ();
			int modifiers = OS.GetCurrentEventKeyModifiers ();
			Point pt = currentControl.toControl (where.h, where.v);
			currentControl.sendMouseEvent (SWT.MouseEnter, (short)0, true, chord, (short)pt.x, (short)pt.y, modifiers);
		}
	}
	if (control != null && mouseMoved) {
		int [] outDelay = new int [1];
		OS.HMGetTagDelay (outDelay);
		if (mouseHoverID != 0) {
			OS.SetEventLoopTimerNextFireTime (mouseHoverID, outDelay [0] / 1000.0);
		} else {
			int eventLoop = OS.GetCurrentEventLoop ();
			int [] id = new int [1];
			OS.InstallEventLoopTimer (eventLoop, outDelay [0] / 1000.0, 0.0, mouseHoverProc, 0, id);
			mouseHoverID = id [0];
		}
		mouseMoved = false;
	}
	if (!OS.StillDown () && theWindow [0] != 0 && theControl [0] != 0) {
		Rect rect = new Rect ();
		OS.GetWindowBounds (theWindow [0], (short) OS.kWindowContentRgn, rect);
		where.h -= rect.left;
		where.v -= rect.top;
		int modifiers = OS.GetCurrentEventKeyModifiers ();
		boolean [] cursorWasSet = new boolean [1];
		OS.HandleControlSetCursor (theControl [0], where, (short) modifiers, cursorWasSet);
		//GOOGLE: Allow WebKit to maintain it's own cursor state.
		if (!cursorWasSet [0] && !(control instanceof Browser)) OS.SetThemeCursor (OS.kThemeArrowCursor);
	}
	return eventSent;
}

boolean runDeferredEvents () {
	/*
	* Run deferred events.  This code is always
	* called  in the Display's thread so it must
	* be re-enterant need not be synchronized.
	*/
	while (eventQueue != null) {
		
		/* Take an event off the queue */
		Event event = eventQueue [0];
		if (event == null) break;
		int length = eventQueue.length;
		System.arraycopy (eventQueue, 1, eventQueue, 0, --length);
		eventQueue [length] = null;

		/* Run the event */
		Widget widget = event.widget;
		if (widget != null && !widget.isDisposed ()) {
			Widget item = event.item;
			if (item == null || !item.isDisposed ()) {
				widget.notifyListeners (event.type, event);
			}
		}

		/*
		* At this point, the event queue could
		* be null due to a recursive invokation
		* when running the event.
		*/
	}

	/* Clear the queue */
	eventQueue = null;
	return true;
}

boolean runEventLoopTimers () {
	allowTimers = false;
	boolean result = OS.ReceiveNextEvent (0, null, OS.kEventDurationNoWait, false, null) == OS.noErr;
	allowTimers = true;
	return result;
}

boolean runGrabs () {
	if (grabControl == null || grabbing) return false;
	if (!OS.StillDown ()) {
		grabControl = null;
		return false;
	}
	Rect rect = new Rect ();
	int [] outModifiers = new int [1];
	short [] outResult = new short [1];
	CGPoint pt = new CGPoint ();
	org.eclipse.swt.internal.carbon.Point outPt = new org.eclipse.swt.internal.carbon.Point ();
	grabbing = true;
	mouseUpControl = null;
	try {
		while (grabControl != null && !grabControl.isDisposed () && outResult [0] != OS.kMouseTrackingMouseUp) {
			if (!OS.HIVIEW) grabControl.getShell().update (true);
			lastModifiers = OS.GetCurrentEventKeyModifiers ();
			int oldState = OS.GetCurrentEventButtonState ();
			int handle = grabControl.handle;
			int window = OS.GetControlOwner (handle);
			int port = OS.HIVIEW ? -1 : OS.GetWindowPort (window);
			OS.TrackMouseLocationWithOptions (port, OS.kTrackMouseLocationOptionDontConsumeMouseUp, 10 / 1000.0, outPt, outModifiers, outResult);
			int type = 0, button = 0;
			switch ((int) outResult [0]) {
				case OS.kMouseTrackingTimedOut: {
					runAsyncMessages (false);
					break;
				}
				case OS.kMouseTrackingMouseDown: {
					type = SWT.MouseDown;
					int newState = OS.GetCurrentEventButtonState ();
					if ((oldState & 0x1) == 0 && (newState & 0x1) != 0) button = 1;
					if ((oldState & 0x2) == 0 && (newState & 0x2) != 0) button = 2;
					if ((oldState & 0x4) == 0 && (newState & 0x4) != 0) button = 3;
					break;
				}
				case OS.kMouseTrackingMouseUp: {
					type = SWT.MouseUp;
					int newState = OS.GetCurrentEventButtonState ();
					if ((oldState & 0x1) != 0 && (newState & 0x1) == 0) button = 1;
					if ((oldState & 0x2) != 0 && (newState & 0x2) == 0) button = 2;
					if ((oldState & 0x4) != 0 && (newState & 0x4) == 0) button = 3;
					break;
				}
//				case OS.kMouseTrackingMouseExited: 				type = SWT.MouseExit; break;
//				case OS.kMouseTrackingMouseEntered: 			type = SWT.MouseEnter; break;
				case OS.kMouseTrackingMouseDragged: {
					mouseMoved = true;
					type = SWT.MouseMove;
					dragDetect (grabControl);
					break;
				}
				case OS.kMouseTrackingMouseKeyModifiersChanged:	break;
				case OS.kMouseTrackingUserCancelled:	 break;
				case OS.kMouseTrackingMouseMoved: {
					mouseMoved = true;
					type = SWT.MouseMove;
					break;
				}
			}
			boolean events = type != 0;
			if (type != 0) {
				int x = outPt.h;
				int y = outPt.v;
				if (OS.HIVIEW) {
					OS.GetWindowBounds (window, (short) OS.kWindowStructureRgn, rect);
					pt.x = x - rect.left;
					pt.y = y - rect.top;
					OS.HIViewConvertPoint (pt, 0, handle);
					x = (int) pt.x;
					y = (int) pt.y;
				} else {
					OS.GetControlBounds (handle, rect);
					x -= rect.left;
					y -= rect.top;
				}
				int chord = OS.GetCurrentEventButtonState ();
				if (grabControl != null && !grabControl.isDisposed ()) {
					if (type == SWT.MouseUp) {
						mouseUpControl = grabControl;
					} else {
						grabControl.sendMouseEvent (type, (short)button, true, chord, (short)x, (short)y, outModifiers [0]);
					}
				}
			}
			if (events) runDeferredEvents ();
		}
	} finally {
		grabbing = false;
		grabControl = null;
	}
	return true;
}

boolean runPaint () {
	if (!needsPaint) return false;
	needsPaint = false;
	for (int i = 0; i < widgetTable.length; i++) {
		Widget widget = widgetTable [i];
		if (widget != null && widget instanceof Shell) {
			Shell shell = (Shell) widget;
			if (shell.invalRgn != 0) {
				int invalRgn = shell.invalRgn;
				shell.invalRgn = 0;
				shell.redrawChildren (OS.HIViewGetRoot (shell.shellHandle), invalRgn);
				OS.DisposeRgn (invalRgn);
			}
		}
	}
	return true;
}

boolean runPopups () {
	if (popups == null) return false;
	grabControl = null;
	boolean result = false;
	while (popups != null) {
		Menu menu = popups [0];
		if (menu == null) break;
		int length = popups.length;
		System.arraycopy (popups, 1, popups, 0, --length);
		popups [length] = null;
		clearMenuFlags ();
		runDeferredEvents ();
		menu._setVisible (true);
		clearMenuFlags ();
		result = true;
	}
	popups = null;
	return result;
}

boolean runSettings () {
	if (!runSettings) return false;
	runSettings = false;
	initializeInsets ();
	sendEvent (SWT.Settings, null);
	Shell [] shells = getShells ();
	for (int i=0; i<shells.length; i++) {
		Shell shell = shells [i];
		if (!shell.isDisposed ()) {
			shell.redraw (true);
			shell.layout (true, true);
		}
	}
	return true;
}

boolean runTimers () {
	if (timerList == null) return false;
	boolean result = false;
	for (int i=0; i<timerList.length; i++) {
		if (timerIds [i] == -1) {
			Runnable runnable = timerList [i];
			timerList [i] = null;
			timerIds [i] = 0;
			if (runnable != null) {
				result = true;
				runnable.run ();
			}
		}
	}
	return result;
}

void sendEvent (int eventType, Event event) {
	if (eventTable == null && filterTable == null) {
		return;
	}
	if (event == null) event = new Event ();
	event.display = this;
	event.type = eventType;
	if (event.time == 0) event.time = getLastEventTime ();
	if (!filterEvent (event)) {
		if (eventTable != null) eventTable.sendEvent (event);
	}
}

/**
 * On platforms which support it, sets the application name
 * to be the argument. On Motif, for example, this can be used
 * to set the name used for resource lookup.  Specifying
 * <code>null</code> for the name clears it.
 *
 * @param name the new app name or <code>null</code>
 */
public static void setAppName (String name) {
	APP_NAME = name;
}

void setCurrentCaret (Caret caret) {
	if (caretID != 0) OS.RemoveEventLoopTimer (caretID);
	caretID = 0;
	currentCaret = caret;
	if (currentCaret != null) {
		int blinkRate = currentCaret.blinkRate;
		int [] timerId = new int [1];
		double time = blinkRate / 1000.0;
		int eventLoop = OS.GetCurrentEventLoop ();
		OS.InstallEventLoopTimer (eventLoop, time, time, caretProc, 0, timerId);
		caretID = timerId [0];
	}
}

void setCursor (int cursor) {
	switch (cursor) {
		case OS.kThemePointingHandCursor:
		case OS.kThemeArrowCursor:
		case OS.kThemeSpinningCursor:
		case OS.kThemeCrossCursor:
		case OS.kThemeWatchCursor:
		case OS.kThemeIBeamCursor:
		case OS.kThemeNotAllowedCursor:
		case OS.kThemeResizeLeftRightCursor:
		case OS.kThemeResizeLeftCursor:
		case OS.kThemeResizeRightCursor:
			OS.SetThemeCursor (cursor);
			break;
		default:
			OS.SetCursor (cursor);
	}
}

/**
 * Sets the location of the on-screen pointer relative to the top left corner
 * of the screen.  <b>Note: It is typically considered bad practice for a
 * program to move the on-screen pointer location.</b>
 *
 * @param x the new x coordinate for the cursor
 * @param y the new y coordinate for the cursor
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 * 
 * @since 2.1
 */
public void setCursorLocation (int x, int y) {
	checkDevice ();
	CGPoint pt = new CGPoint ();
	pt.x = x;  pt.y = y;
	OS.CGWarpMouseCursorPosition (pt);
}

/**
 * Sets the location of the on-screen pointer relative to the top left corner
 * of the screen.  <b>Note: It is typically considered bad practice for a
 * program to move the on-screen pointer location.</b>
 *
 * @param point new position
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_NULL_ARGUMENT - if the point is null
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 * 
 * @since 2.0
 */
public void setCursorLocation (Point point) {
	checkDevice ();
	if (point == null) error (SWT.ERROR_NULL_ARGUMENT);
	setCursorLocation (point.x, point.y);
}

/**
 * Sets the application defined property of the receiver
 * with the specified name to the given argument.
 * <p>
 * Applications may have associated arbitrary objects with the
 * receiver in this fashion. If the objects stored in the
 * properties need to be notified when the display is disposed
 * of, it is the application's responsibility provide a
 * <code>disposeExec()</code> handler which does so.
 * </p>
 *
 * @param key the name of the property
 * @param value the new value for the property
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the key is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 *
 * @see #getData(String)
 * @see #disposeExec(Runnable)
 */
public void setData (String key, Object value) {
	checkDevice ();
	if (key == null) error (SWT.ERROR_NULL_ARGUMENT);
	
	if (key.equals (ADD_WIDGET_KEY)) {
		Object [] data = (Object [])value;
		int handle = ((Integer)data [0]).intValue ();
		Widget widget = (Widget)data [1];
		if (widget == null) removeWidget (handle);
		else addWidget (handle, widget);
	}
	
	/* Remove the key/value pair */
	if (value == null) {
		if (keys == null) return;
		int index = 0;
		while (index < keys.length && !keys [index].equals (key)) index++;
		if (index == keys.length) return;
		if (keys.length == 1) {
			keys = null;
			values = null;
		} else {
			String [] newKeys = new String [keys.length - 1];
			Object [] newValues = new Object [values.length - 1];
			System.arraycopy (keys, 0, newKeys, 0, index);
			System.arraycopy (keys, index + 1, newKeys, index, newKeys.length - index);
			System.arraycopy (values, 0, newValues, 0, index);
			System.arraycopy (values, index + 1, newValues, index, newValues.length - index);
			keys = newKeys;
			values = newValues;
		}
		return;
	}
	
	/* Add the key/value pair */
	if (keys == null) {
		keys = new String [] {key};
		values = new Object [] {value};
		return;
	}
	for (int i=0; i<keys.length; i++) {
		if (keys [i].equals (key)) {
			values [i] = value;
			return;
		}
	}
	String [] newKeys = new String [keys.length + 1];
	Object [] newValues = new Object [values.length + 1];
	System.arraycopy (keys, 0, newKeys, 0, keys.length);
	System.arraycopy (values, 0, newValues, 0, values.length);
	newKeys [keys.length] = key;
	newValues [values.length] = value;
	keys = newKeys;
	values = newValues;
}

/**
 * Sets the application defined, display specific data
 * associated with the receiver, to the argument.
 * The <em>display specific data</em> is a single,
 * unnamed field that is stored with every display. 
 * <p>
 * Applications may put arbitrary objects in this field. If
 * the object stored in the display specific data needs to
 * be notified when the display is disposed of, it is the
 * application's responsibility provide a
 * <code>disposeExec()</code> handler which does so.
 * </p>
 *
 * @param data the new display specific data
 *
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 *
 * @see #getData()
 * @see #disposeExec(Runnable)
 */
public void setData (Object data) {
	checkDevice ();
	this.data = data;
}

/**
 * Sets the synchronizer used by the display to be
 * the argument, which can not be null.
 *
 * @param synchronizer the new synchronizer for the display (must not be null)
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the synchronizer is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 *    <li>ERROR_FAILED_EXEC - if an exception occurred while running an inter-thread message</li>
 * </ul>
 */
public void setSynchronizer (Synchronizer synchronizer) {
	checkDevice ();
	if (synchronizer == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (this.synchronizer != null) {
		this.synchronizer.runAsyncMessages(true);
	}
	this.synchronizer = synchronizer;
}

void setMenuBar (Menu menu) {
	/*
	* Feature in the Macintosh.  SetRootMenu() does not
	* accept NULL to indicate that their should be no
	* menu bar. The fix is to create a temporary empty
	* menu, set that to be the menu bar, clear the menu
	* bar and then delete the temporary menu.
	*/
	if (menu == menuBar) return;
	int theMenu = 0;
	if (menu == null) {
		int outMenuRef [] = new int [1];
		OS.CreateNewMenu ((short) ID_TEMPORARY, 0, outMenuRef);
		theMenu = outMenuRef [0];
	} else {
		theMenu = menu.handle;
	}
	OS.SetRootMenu (theMenu);
	if (menu == null) {
		OS.ClearMenuBar ();
		OS.DisposeMenu (theMenu);
	}
	menuBar = menu;
}

/**
 * Causes the user-interface thread to <em>sleep</em> (that is,
 * to be put in a state where it does not consume CPU cycles)
 * until an event is received or it is otherwise awakened.
 *
 * @return <code>true</code> if an event requiring dispatching was placed on the queue.
 *
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 *
 * @see #wake
 */
public boolean sleep () {
	checkDevice ();
	if (getMessageCount () != 0) return true;
	disposeWindows ();
	if (eventTable != null && eventTable.hooks (SWT.Settings)) {
		RGBColor color = new RGBColor ();
		int status = OS.noErr, depth = getDepth ();
		do {
			allowTimers = false;
			status = OS.ReceiveNextEvent (0, null, 0.5, false, null);
			allowTimers = true;
			if (status == OS.eventLoopTimedOutErr) {
				OS.GetThemeBrushAsColor ((short) OS.kThemeBrushPrimaryHighlightColor, (short) depth, true, color);
				if (highlightColor.red != color.red || highlightColor.green != color.green || highlightColor.blue != color.blue) {
					highlightColor = color;
					runSettings = true;
					return true;
				}
			}
		} while (status == OS.eventLoopTimedOutErr);
		return status == OS.noErr;
	}
	allowTimers = false;
	int status = OS.ReceiveNextEvent (0, null, OS.kEventDurationForever, false, null);
	allowTimers = true;
	return status == OS.noErr;
}

/**
 * Causes the <code>run()</code> method of the runnable to
 * be invoked by the user-interface thread at the next 
 * reasonable opportunity. The thread which calls this method
 * is suspended until the runnable completes.  Specifying <code>null</code>
 * as the runnable simply wakes the user-interface thread.
 * <p>
 * Note that at the time the runnable is invoked, widgets 
 * that have the receiver as their display may have been
 * disposed. Therefore, it is necessary to check for this
 * case inside the runnable before accessing the widget.
 * </p>
 * 
 * @param runnable code to run on the user-interface thread or <code>null</code>
 *
 * @exception SWTException <ul>
 *    <li>ERROR_FAILED_EXEC - if an exception occured when executing the runnable</li>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 *
 * @see #asyncExec
 */
public void syncExec (Runnable runnable) {
	if (isDisposed ()) error (SWT.ERROR_DEVICE_DISPOSED);
	synchronizer.syncExec (runnable);
}

int textInputProc (int nextHandler, int theEvent, int userData) {
	int theWindow = OS.GetUserFocusWindow ();
	if (theWindow != 0) {
		int [] theControl = new int [1];
		OS.GetKeyboardFocus (theWindow, theControl);
		Widget widget = getWidget (theControl [0]);
		if (widget != null) {
			/* Stop the default event handler from activating the default button */
			OS.GetWindowDefaultButton (theWindow, theControl);
			OS.SetWindowDefaultButton (theWindow, 0);
			int result = widget.textInputProc (nextHandler, theEvent, userData);
			if (result == OS.eventNotHandledErr) {
				result = OS.CallNextEventHandler (nextHandler, theEvent);
			}
			OS.SetWindowDefaultButton (theWindow, theControl [0]);
			return result;
		}
	}
	return OS.eventNotHandledErr;
}

/**
 * Causes the <code>run()</code> method of the runnable to
 * be invoked by the user-interface thread after the specified
 * number of milliseconds have elapsed. If milliseconds is less
 * than zero, the runnable is not executed.
 * <p>
 * Note that at the time the runnable is invoked, widgets 
 * that have the receiver as their display may have been
 * disposed. Therefore, it is necessary to check for this
 * case inside the runnable before accessing the widget.
 * </p>
 *
 * @param milliseconds the delay before running the runnable
 * @param runnable code to run on the user-interface thread
 *
 * @exception IllegalArgumentException <ul>
 *    <li>ERROR_NULL_ARGUMENT - if the runnable is null</li>
 * </ul>
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 *
 * @see #asyncExec
 */
public void timerExec (int milliseconds, Runnable runnable) {
	checkDevice ();
	if (runnable == null) error (SWT.ERROR_NULL_ARGUMENT);
	if (timerList == null) timerList = new Runnable [4];
	if (timerIds == null) timerIds = new int [4];
	int index = 0;
	while (index < timerList.length) {
		if (timerList [index] == runnable) break;
		index++;
	}
	if (index != timerList.length) {
		int timerId = timerIds [index];
		if (milliseconds < 0) {
			OS.RemoveEventLoopTimer (timerId);
			timerList [index] = null;
			timerIds [index] = 0;
		} else {
			OS.SetEventLoopTimerNextFireTime (timerId, milliseconds / 1000.0);
		}
		return;
	} 
	if (milliseconds < 0) return;
	index = 0;
	while (index < timerList.length) {
		if (timerList [index] == null) break;
		index++;
	}
	if (index == timerList.length) {
		Runnable [] newTimerList = new Runnable [timerList.length + 4];
		System.arraycopy (timerList, 0, newTimerList, 0, timerList.length);
		timerList = newTimerList;
		int [] newTimerIds = new int [timerIds.length + 4];
		System.arraycopy (timerIds, 0, newTimerIds, 0, timerIds.length);
		timerIds = newTimerIds;
	}
	int [] timerId = new int [1];
	int eventLoop = OS.GetCurrentEventLoop ();
	OS.InstallEventLoopTimer (eventLoop, milliseconds / 1000.0, 0.0, timerProc, index, timerId);
	if (timerId [0] != 0) {
		timerIds [index] = timerId [0];
		timerList [index] = runnable;
	}
}

int timerProc (int id, int index) {
	OS.RemoveEventLoopTimer (id);
	if (timerList == null) return 0;
	if (0 <= index && index < timerList.length) {
		if (allowTimers) {
			Runnable runnable = timerList [index];
			timerList [index] = null;
			timerIds [index] = 0;
			if (runnable != null) runnable.run ();
		} else {
			timerIds [index] = -1;
			wakeThread ();
		}
	}
	return 0;
}

int trackingProc (int browser, int itemID, int property, int theRect, int startPt, int modifiers) {
	Widget widget = getWidget (browser);
	if (widget != null) return widget.trackingProc (browser, itemID, property, theRect, startPt, modifiers);
	return OS.noErr;
}

/**
 * Forces all outstanding paint requests for the display
 * to be processed before this method returns.
 *
 * @exception SWTException <ul>
 *    <li>ERROR_THREAD_INVALID_ACCESS - if not called from the thread that created the receiver</li>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 * 
 * @see Control#update()
 */
public void update () {
	checkDevice ();	
	Shell [] shells = getShells ();
	for (int i=0; i<shells.length; i++) {
		Shell shell = shells [i];
		if (!shell.isDisposed ()) shell.update (true);
	}
	/*
	* This code is intentionally commented.
	*/
//	int [] outEvent = new int [1];
//	int [] mask = new int [] {OS.kEventClassWindow, OS.kEventWindowUpdate};
//	while (OS.ReceiveNextEvent (mask.length / 2, mask, OS.kEventDurationNoWait, true, outEvent) == OS.noErr) {
//		/*
//		* Bug in the Macintosh.  For some reason, when a hierarchy of
//		* windows is disposed from kEventWindowClose, despite the fact
//		* that DisposeWindow() has been called, the window is not
//		* disposed and there are outstandings kEventWindowUpdate events
//		* in the event queue.  Dispatching these events will cause a
//		* segment fault.  The fix is to dispatch events to visible
//		* windows only.
//		*/
//		int [] theWindow = new int [1];
//		OS.GetEventParameter (outEvent [0], OS.kEventParamDirectObject, OS.typeWindowRef, null, 4, null, theWindow);
//		if (OS.IsWindowVisible (theWindow [0])) OS.SendEventToEventTarget (outEvent [0], OS.GetEventDispatcherTarget ());
//		OS.ReleaseEvent (outEvent [0]);
//	}
}

void updateQuitMenu () {
	boolean enabled = true;
	Shell [] shells = getShells ();
	int mask = SWT.PRIMARY_MODAL | SWT.APPLICATION_MODAL | SWT.SYSTEM_MODAL;
	for (int i=0; i<shells.length; i++) {
		Shell shell = shells [i];
		if ((shell.style & mask) != 0 && shell.isVisible ()) {
			enabled = false;
			break;
		}
	}
	if (enabled) {
		OS.EnableMenuCommand (0, OS.kHICommandQuit);
	} else {
		OS.DisableMenuCommand (0, OS.kHICommandQuit);		
	}
}

/**
 * If the receiver's user-interface thread was <code>sleep</code>ing, 
 * causes it to be awakened and start running again. Note that this
 * method may be called from any thread.
 * 
 * @exception SWTException <ul>
 *    <li>ERROR_DEVICE_DISPOSED - if the receiver has been disposed</li>
 * </ul>
 * 
 * @see #sleep
 */
public void wake () {
	if (isDisposed ()) error (SWT.ERROR_DEVICE_DISPOSED);
	if (thread == Thread.currentThread ()) return;
	wakeThread ();
}

void wakeThread () {
	int [] wakeEvent = new int [1];
	OS.CreateEvent (0, WAKE_CLASS, WAKE_KIND, 0.0, OS.kEventAttributeUserEvent, wakeEvent);
	OS.PostEventToQueue (queue, wakeEvent [0], (short) OS.kEventPriorityStandard);
	if (wakeEvent [0] != 0) OS.ReleaseEvent (wakeEvent [0]);
}

int windowProc (int nextHandler, int theEvent, int userData) {
	Widget widget = getWidget (userData);
	if (widget == null) {
		int [] theWindow = new int [1];
		OS.GetEventParameter (theEvent, OS.kEventParamDirectObject, OS.typeWindowRef, null, 4, null, theWindow);
		int [] theRoot = new int [1];
		OS.GetRootControl (theWindow [0], theRoot);
		widget = getWidget (theRoot [0]);
	}
	if (widget != null) return widget.windowProc (nextHandler, theEvent, userData); 
	return OS.eventNotHandledErr;
}

}
