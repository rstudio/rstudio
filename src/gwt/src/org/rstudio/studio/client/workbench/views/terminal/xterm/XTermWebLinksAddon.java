/*
 * XTermWebLinksAddon.java
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
 * An xterm.js addon that enables web links.
 * https://github.com/xtermjs/xterm.js/blob/4.7.0/addons/xterm-addon-web-links/typings/xterm-addon-web-links.d.ts
 */
@JsType(isNative = true, namespace = "WebLinksAddon", name = "WebLinksAddon")
public class XTermWebLinksAddon extends XTermAddon
{
   /*
    * NOTE: only supporting the no-argument default constructor which makes URLs into clickable
    * links that launch external browser. Additional features are available in the add-on for
    * registering custom providers and callback.
    */
}
