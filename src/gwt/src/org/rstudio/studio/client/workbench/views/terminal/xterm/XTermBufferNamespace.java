/*
 * XTermBufferNamespace.java
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
import org.rstudio.core.client.jsinterop.JsConsumerWithArg;

/**
 * Represents the terminal's set of buffers (IBufferNamespace).
 * https://github.com/xtermjs/xterm.js/blob/4.7.0/typings/xterm.d.ts
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
public class XTermBufferNamespace
{
   /**
    * The active buffer, this will either be the normal or alternate buffers.
    */
   @JsProperty public native XTermBuffer getActive();

   /**
    * The normal buffer.
    */
   @JsProperty public native XTermBuffer getNormal();

   /**
    * The alternate buffer, this becomes the active buffer when an application
    * enters this mode via DECSET (`CSI ? 4 7 h`)
    */
   @JsProperty public native XTermBuffer getAlternate();

   /**
    * Adds an event listener for when the active buffer changes.
    * @returns an `IDisposable` to stop listening.
    */
   public native XTermDisposable onBufferChange(JsConsumerWithArg<XTermBuffer> callback);
}

