/*
 * CodeMirrorConfig.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.codemirror.client;


import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import org.rstudio.codemirror.client.resources.CodeMirrorResources;

public class CodeMirrorConfig extends JavaScriptObject
{
   public final static CodeMirrorConfig create() 
   {
      JsArrayString files = (JsArrayString) JsArrayString.createArray();
      files.push(CodeMirrorResources.INSTANCE.codemirror_inner().getUrl());

      return create("", files);
   }
   
   private final static native CodeMirrorConfig create(
         String jsPath,
         JsArrayString baseFiles) /*-{

      return {
         path: jsPath,
         basefiles: baseFiles,
         width: "100%",
         height: "100%"
      };
   }-*/;
   
   protected CodeMirrorConfig() 
   {
   }
   
   // path (note this is set automatically by the create method)
   public final native String getPath() /*-{
      return this.path;
   }-*/;
   public final native void setPath(String path) /*-{
      this.path = path;
   }-*/;
   
   // low-level stylesheet path getter and setter. client may want to
   // use codeMirrorModuleBaseURL & codeMirrorPublicPath to assist with
   // forming correct paths
   public final native String getStylesheetPath() /*-{ 
      return this.stylesheet; 
   }-*/;
   public final native void setStylesheetPath(String stylesheet) /*-{
      this.stylesheet = stylesheet;
   }-*/;
   
   // parserfile
   public final native void setParserfile(String parserfile) /*-{
      this.parserfile = parserfile;
   }-*/;

   /** Tells CodeMirror to load multiple files as the parser. If multiple
    * parsers are passed in, the last one will be used by default.
    * setParserByName can be called on the CodeMirror object to switch
    * to one of the parsers.
    */
   public final native void setParserfiles(JsArrayString parserfiles) /*-{
      this.parserfile = parserfiles;
   }-*/;

   // height
   public final native String getHeight() /*-{ 
      return this.height; 
   }-*/;
   public final native void setHeight(String height) /*-{
      this.height = height;
   }-*/;
   
   // width
   public final native String getWidth() /*-{ 
      return this.width; 
   }-*/;
   public final native void setWidth(String width) /*-{
      this.width = width;
   }-*/;
   
   // textWrapping
   public final native boolean getTextWrapping() /*-{
      return this.textWrapping;
   }-*/;
   public final native void setTextWrapping(boolean textWrapping) /*-{
      this.textWrapping = textWrapping;
   }-*/;
   
   // lineNumbers
   public final native boolean getLineNumbers() /*-{
      return this.lineNumbers;
   }-*/;
   public final native void setLineNumbers(boolean lineNumbers) /*-{
      this.lineNumbers = lineNumbers;
   }-*/;

// Not safe to use--use CodeMirrorEditor#setCode instead, as it works around
// a Firefox bug
//   // content
//   public final native String getContent() /*-{
//      return this.content;
//   }-*/;
//   public final native void setContent(String content) /*-{
//      this.content = content;
//   }-*/;

   // iframeClass
   public final native void getIframeClass() /*-{
      return this.iframeClass;
   }-*/;
   public final native void setIframeClass(String iframeClass) /*-{
      this.iframeClass = iframeClass;
   }-*/;
   
   // passDelay
   public final native int getPassDelay() /*-{
      return this.passDelay || 200;
   }-*/;
   public final native void setPassDelay(int delay) /*-{
      this.passDelay = delay;
   }-*/;
   
   // passTime
   public final native int getPassTime() /*-{
      return this.passTime || 50;
   }-*/;
   public final native void setPassTime(int time) /*-{
      this.passTime = time;
   }-*/;
   
   // autoMatchParens
   public final native boolean getAutoMatchParens() /*-{
      return this.autoMatchParens;
   }-*/;
   public final native void setAutoMatchParens(boolean match) /*-{
      this.autoMatchParens = match;
   }-*/;

   public final native void setTabMode(String tabMode) /*-{
      this.tabMode = tabMode;
   }-*/;
}

