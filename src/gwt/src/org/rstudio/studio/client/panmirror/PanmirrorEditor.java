/*
 * PanmirrorEditor.java
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

import jsinterop.annotations.JsType;

import org.rstudio.core.client.jsinterop.JsVoidFunction;
import org.rstudio.studio.client.panmirror.command.PanmirrorCommand;

import elemental2.core.JsObject;
import elemental2.promise.Promise;


@JsType(isNative = true, name="Editor", namespace = "Panmirror")
public class PanmirrorEditor
{
   public native static Promise<PanmirrorEditor> create(PanmirrorEditorConfig config);
   
   public native void destroy();
   
   public native void setTitle(String title);
   public native String getTitle();
   
   public native Promise<Boolean> setMarkdown(String markdown, boolean emitUpdate);
   public native Promise<String> getMarkdown();
   
   public native JsVoidFunction subscribe(String event, JsVoidFunction handler);
   
   public native PanmirrorCommand[] commands();
   
   public native String getHTML();
   
   public native JsObject getSelection();
   
   public native void focus();
   public native void blur();
   
   public native void resize();
   
   public native void navigate(String id);
 
}
