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

import org.rstudio.studio.client.panmirror.model.PanmirrorPandocEngine;
import org.rstudio.studio.client.panmirror.ui.PanmirrorEditorUI;


import com.google.gwt.dom.client.Element;

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
   
   
   public Element parent;
   public PanmirrorPandocEngine pandoc;
   public String format;
   public PanmirrorEditorUI ui;
   public PanmirrorEditorOptions options;
   
   
   /*  
   readonly hooks?: EditorHooks;
   readonly keybindings?: EditorKeybindings;
   readonly extensions?: readonly Extension[];
   */
 
}
