/*
 * PanmirrorConfig.java
 *
 * Copyright (C) 2009-20 by RStudio, PBC
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

package org.rstudio.studio.client.panmirror;

import org.rstudio.studio.client.panmirror.pandoc.PanmirrorPandocEngine;

import elemental2.core.JsObject;
import jsinterop.annotations.JsType;

@JsType
public class PanmirrorContext
{  
   public PanmirrorContext(String format, PanmirrorUIContext uiContext)
   {
      this.format = format;
      ui = new PanmirrorUI(uiContext);
   }
   
   public String format;
   public PanmirrorUI ui;
   public PanmirrorPandocEngine pandoc = new PanmirrorPandocEngine();
   public PanmirrorHooks hooks = new PanmirrorHooks();
   public JsObject[] extensions = null;
}
