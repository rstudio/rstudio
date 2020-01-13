package org.rstudio.studio.client.panmirror;

import jsinterop.annotations.JsType;

import com.google.gwt.core.client.JavaScriptObject;

import elemental2.promise.Promise;


@JsType(isNative = true, name="Editor", namespace = "Panmirror")
public class PanmirrorEditor
{  
   
   // TODO:
   // subscribe
   // getOutline
   
   public native static Promise<PanmirrorEditor> create(PanmirrorEditorConfig config);
   
   public native void destroy();
   
   public native void setTitle(String title);
   public native String getTitle();
   
   public native Promise<Boolean> setMarkdown(String markdown, boolean emitUpdate);
   public native Promise<String> getMarkdown();
   
   public native String getHTML();
   
   public native JavaScriptObject getSelection();
   
   public native void focus();
   public native void blur();
   
   public native void resize();
   
   public native void navigate(String id);
 
}
