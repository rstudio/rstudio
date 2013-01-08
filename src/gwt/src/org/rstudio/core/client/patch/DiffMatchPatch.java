/*
 * DiffMatchPatch.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.core.client.patch;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.HeadElement;
import com.google.gwt.dom.client.ScriptElement;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;

public class DiffMatchPatch
{
   interface Resources extends ClientBundle
   {
      @Source("diff_match_patch.js")
      TextResource diff_match_patch();
   }

   static
   {
      injectJavascript(
            ((Resources) GWT.create(Resources.class)).diff_match_patch().getText());
   }
   
   private static void injectJavascript(String source)
   {
      Document doc = Document.get();
      HeadElement head = (HeadElement) doc.getElementsByTagName("head").getItem(0);
      if (head == null)
      {
         head = doc.createHeadElement();
         doc.insertBefore(head, doc.getBody());
      }
      ScriptElement script = doc.createScriptElement(
            source);
      script.setType("text/javascript");
      head.appendChild(script);
   }

   public static native String diff(String s1, String s2) /*-{
      var dmp = new $wnd.diff_match_patch();
      var patches = dmp.patch_make(s1, s2);
      return dmp.patch_toText(patches);
   }-*/;
}
