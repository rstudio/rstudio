/*
 * JSON2.java
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
package org.rstudio.core.client.jsonrpc;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ScriptElement;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;

public class JSON2
{
   interface Resources extends ClientBundle
   {
      @Source("json2.min.js")
      TextResource json2_js();
   }

   public static void ensureInjected()
   {
      if (injected_)
         return;
      injected_ = true;

      Resources res = GWT.create(Resources.class);

      ScriptElement script = Document.get().createScriptElement();
      script.setType("text/javascript");
      script.setInnerText(res.json2_js().getText());
      Element head = Document.get().getElementsByTagName("head").getItem(0);
      head.appendChild(script);
   }

   private static boolean injected_ = false;
}
