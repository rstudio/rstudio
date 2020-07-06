/*
 * XTermFitAddon.java
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

import jsinterop.annotations.JsType;
/**
 * An xterm.js addon that enables resizing the terminal to the dimensions of
 * its containing element.
 * https://github.com/xtermjs/xterm.js/blob/4.7.0/addons/xterm-addon-fit/typings/xterm-addon-fit.d.ts
 */
@JsType(isNative = true, namespace = "FitAddon", name = "FitAddon")
public class XTermFitAddon extends XTermAddon
{
   /**
    * Resizes the terminal to the dimensions of its containing element.
    */
   public native void fit();

   /**
    * Gets the proposed dimensions that will be used for a fit.
    */
   public native XTermDimensions proposeDimensions();
}

