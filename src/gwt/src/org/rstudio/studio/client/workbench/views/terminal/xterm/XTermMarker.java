/*
 * XTermMarker.java
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.terminal.xterm;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * Represents a specific line in the terminal that is tracked when scrollback
 * is trimmed and lines are added or removed. This is a single line that may
 * be part of a larger wrapped line.
 * https://github.com/xtermjs/xterm.js/blob/4.7.0/typings/xterm.d.ts
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
public class XTermMarker extends XTermDisposable
{
   /**
    * A unique identifier for this marker.
    */
   @JsProperty public native double getId();

   /**
    * Whether this marker is disposed.
    */
   @JsProperty public native boolean getIsDisposed();

   /**
    * The actual line index in the buffer at this point in time. This is set to
    * -1 if the marker has been disposed.
    */
   @JsProperty public native int getLine();
}

