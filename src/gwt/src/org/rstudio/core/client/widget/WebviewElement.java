/*
 * WebviewElement.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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
package org.rstudio.core.client.widget;

import org.rstudio.core.client.dom.IFrameElementEx;
import org.rstudio.core.client.dom.WindowEx;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.TagName;

@TagName(WebviewElement.TAG)
public class WebviewElement extends Element
{
   protected WebviewElement()
   {
   }
   
   public static WebviewElement as(Element elem)
   {
      assert is(elem);
      return (WebviewElement) elem;
   }

   public static boolean is(JavaScriptObject o)
   {
      if (Element.is(o)) {
         return is((Element) o);
      }
      return false;
   }

   public static boolean is(Node node)
   {
      if (Element.is(node)) {
         return is((Element) node);
      }
      return false;
   }

   public static boolean is(Element elem)
   {
      return elem != null && elem.hasTagName(TAG);
   }
   
   public final WindowEx getContentWindow()
   {
      return getIFrameElement().getContentWindow();
   }
   
   public final Document getContentDocument()
   {
      return getIFrameElement().getContentDocument();
   }
   
   public final native IFrameElementEx getIFrameElement()
   /*-{
      var children = this.shadowRoot.childNodes;
      for (var i = 0; i < children.length; i++) {
         if (children[i].tagName === "IFRAME") {
            return chilren[i];
         }
      }
   }-*/;
   
   public static final String TAG = "webview";
   
}
