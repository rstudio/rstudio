package org.rstudio.studio.client.panmirror;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;

import jsinterop.annotations.JsType;

@JsType
public class PanmirrorEditorConfig
{  
   
   public PanmirrorEditorConfig(
        Element parent,
        PanmirrorPandocEngine pandoc,
        String format,
        JavaScriptObject ui,
        PanmirrorEditorOptions options) {
      
      this.parent = parent;
      this.pandoc = pandoc;
      this.format = format;
      this.ui = ui;
      this.options = options;
   
   }
   
   
   public Element parent;
   public PanmirrorPandocEngine pandoc;
   public String format;
   public JavaScriptObject ui;
   public PanmirrorEditorOptions options;
   
   
   /*  
   readonly hooks?: EditorHooks;
   readonly keybindings?: EditorKeybindings;
   readonly extensions?: readonly Extension[];
   */
 
}
