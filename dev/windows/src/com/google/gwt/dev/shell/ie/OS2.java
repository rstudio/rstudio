// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.shell.ie;

/**
 * Windows constants not defined in {@link org.eclipse.swt.internal.win32.OS}.
 */
public class OS2 {

  public static final int WM_NCPAINT = 0x0085;
  public static final int WM_NCLBUTTONUP = 0x00A2;

  public static final int SWP_FRAMECHANGED = 0x0020;

  public static final int LR_DEFAULTCOLOR = 0x0000;
  public static final int LR_MONOCHROME = 0x0001;
  public static final int LR_COLOR = 0x0002;
  public static final int LR_COPYRETURNORG = 0x0004;
  public static final int LR_COPYDELETEORG = 0x0008;
  public static final int LR_LOADFROMFILE = 0x0010;
  public static final int LR_LOADTRANSPARENT = 0x0020;
  public static final int LR_DEFAULTSIZE = 0x0040;
  public static final int LR_VGACOLOR = 0x0080;
  public static final int LR_LOADMAP3DCOLORS = 0x1000;
  public static final int LR_CREATEDIBSECTION = 0x2000;
  public static final int LR_COPYFROMRESOURCE = 0x4000;
  public static final int LR_SHARED = 0x8000;

  public static final int DCX_WINDOW = 0x0001;
  public static final int DCX_CACHE = 0x0002;
  public static final int DCX_NORESETATTRS = 0x0004;
  public static final int DCX_CLIPCHILDREN = 0x0008;
  public static final int DCX_CLIPSIBLINGS = 0x0010;
  public static final int DCX_PARENTCLIP = 0x0020;
  public static final int DCX_EXCLUDERGN = 0x0040;
  public static final int DCX_INTERSECTRGN = 0x0080;
  public static final int DCX_EXCLUDEUPDATE = 0x0100;
  public static final int DCX_INTERSECTUPDATE = 0x0200;
  public static final int DCX_LOCKWINDOWUPDATE = 0x0400;
  public static final int DCX_USESTYLE = 0x10000;
  public static final int DCX_KEEPCLIPRGN = 0x40000;

  public static final int HTERROR = -2;
  public static final int HTTRANSPARENT = -1;
  public static final int HTNOWHERE = 0;
  public static final int HTCLIENT = 1;
  public static final int HTCAPTION = 2;
  public static final int HTSYSMENU = 3;
  public static final int HTGROWBOX = 4;
  public static final int HTSIZE = 4;
  public static final int HTMENU = 5;
  public static final int HTHSCROLL = 6;
  public static final int HTVSCROLL = 7;
  public static final int HTMINBUTTON = 8;
  public static final int HTMAXBUTTON = 9;
  public static final int HTREDUCE = 8;
  public static final int HTZOOM = 9;
  public static final int HTLEFT = 10;
  public static final int HTSIZEFIRST = 10;
  public static final int HTRIGHT = 11;
  public static final int HTTOP = 12;
  public static final int HTTOPLEFT = 13;
  public static final int HTTOPRIGHT = 14;
  public static final int HTBOTTOM = 15;
  public static final int HTBOTTOMLEFT = 16;
  public static final int HTBOTTOMRIGHT = 17;
  public static final int HTSIZELAST = 17;
  public static final int HTBORDER = 18;
  public static final int HTOBJECT = 19;
  public static final int HTCLOSE = 20;
  public static final int HTHELP = 21;
}
