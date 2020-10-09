/*
 * PanmirrorConfig.java
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

package org.rstudio.studio.client.panmirror;

import org.rstudio.studio.client.panmirror.server.PanmirrorServer;
import org.rstudio.studio.client.panmirror.ui.PanmirrorUI;
import org.rstudio.studio.client.panmirror.ui.PanmirrorUIChunks;
import org.rstudio.studio.client.panmirror.ui.PanmirrorUIContext;
import org.rstudio.studio.client.panmirror.ui.PanmirrorUIDisplay;
import org.rstudio.studio.client.panmirror.ui.PanmirrorUISpelling;

import elemental2.core.JsObject;
import jsinterop.annotations.JsType;

@JsType
public class PanmirrorContext
{  
   public PanmirrorContext(PanmirrorUIContext uiContext, 
                           PanmirrorUIDisplay uiDisplay,
                           PanmirrorUIChunks uiChunks,
                           PanmirrorUISpelling uiSpelling)
   {
      this.ui = new PanmirrorUI(uiContext, uiDisplay, uiChunks, uiSpelling); 
   }
   
   public PanmirrorUI ui;
   public PanmirrorServer server = new PanmirrorServer();
   public PanmirrorHooks hooks = new PanmirrorHooks();
   public JsObject[] extensions = null;
}
