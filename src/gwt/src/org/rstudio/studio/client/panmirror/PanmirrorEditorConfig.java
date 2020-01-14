/*
 * PanmirrorEditorConfig.java
 *
 * Copyright (C) 2009-20 by RStudio, Inc.
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
import org.rstudio.studio.client.panmirror.ui.PanmirrorEditorUI;


import com.google.gwt.dom.client.Element;

import elemental2.core.JsObject;
import jsinterop.annotations.JsType;

@JsType
public class PanmirrorEditorConfig
{  
   
   public PanmirrorEditorConfig(
        Element parent,
        String format,
        PanmirrorEditorOptions options) {
      
      this.parent = parent;
      this.pandoc = new PanmirrorPandocEngine();
      this.format = format;
      this.ui = new PanmirrorEditorUI();
      this.options = options;
   
   }
   
   public final Element parent;
   public final String format;
   public final PanmirrorEditorOptions options;
   public final PanmirrorPandocEngine pandoc;
   public final PanmirrorEditorUI ui;
   
   // TODO: separate parent element from config?
   // TODO: keybindings?: EditorKeybindings;
   
   public PanmirrorEditorHooks hooks;
   public JsObject[] extensions;
 
}
