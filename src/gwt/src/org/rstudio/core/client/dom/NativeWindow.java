/*
 * NativeWindow.java
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
package org.rstudio.core.client.dom;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;

public class NativeWindow extends JavaScriptObject
{
   protected NativeWindow() {}

   public static final native NativeWindow get() /*-{
      return $wnd;
   }-*/;

   public static final native NativeWindow get(Document doc) /*-{
      return doc.defaultView || doc.parentWindow;
   }-*/;

   public final native Document getDocument() /*-{
      return this.document;
   }-*/;

   public final native int getPageXOffset() /*-{
      if (this.pageXOffset)
         return this.pageXOffset;
      if (this.scrollX)
         return this.scrollX;
      if (this.document.body && this.document.body.scrollLeft)
         return this.document.body.scrollLeft;
      if (this.document.documentElement && this.document.documentElement.scrollLeft)
         return this.document.documentElement.scrollLeft;
      return 0;
   }-*/;

   public final native int getPageYOffset() /*-{
      if (this.pageYOffset)
         return this.pageYOffset;
      if (this.scrollY)
         return this.scrollY;
      if (this.document.body && this.document.body.scrollTop)
         return this.document.body.scrollTop;
      if (this.document.documentElement && this.document.documentElement.scrollTop)
         return this.document.documentElement.scrollTop;
      return 0;
   }-*/;

   public final native void focus() /*-{
      this.focus();
   }-*/;

   public final native void print() /*-{
      this.print();
   }-*/;
}
