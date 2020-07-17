/*
 * XTermLocalizableStrings.java
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
import jsinterop.annotations.JsType;

/**
 * The set of localizable strings. (ILocalizableStrings)
 * https://github.com/xtermjs/xterm.js/blob/4.7.0/typings/xterm.d.ts
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
public class XTermLocalizableStrings
{
   /**
    * The aria label for the underlying input textarea for the terminal.
    */
   public String promptLabel;

   /**
    * Announcement for when line reading is suppressed due to too many lines
    * being printed to the terminal when `screenReaderMode` is enabled.
    */
   public String tooMuchOutput;
}

