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
