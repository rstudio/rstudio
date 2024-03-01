/*
 * Markdown.java
 *
 * Copyright (C) 2024 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client;

import org.rstudio.core.client.resources.StaticDataResource;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;

public class Markdown
{
   public static final void markdownToHtml(String markdown,
                                           CommandWithArg<String> callback)
   {
      LOADER.addCallback(new ExternalJavaScriptLoader.Callback()
      {
         @Override
         public void onLoaded()
         {
            String html = markdownToHtmlImpl(markdown);
            callback.execute(html);
         }
      });
   }
   
   private static final native String markdownToHtmlImpl(String markdown)
   /*-{
      var showdown = $wnd.showdown;
      var converter = new showdown.Converter({
         literalMidWordUnderscores: true
      });
      return converter.makeHtml(markdown);
   }-*/;
   
   // Boilerplate ----
   
   public interface Resources extends ClientBundle
   {
      @Source("resources/js/showdown.min.js")
      StaticDataResource showdownMinJs();
   }
   
   public static final Resources RES = GWT.create(Resources.class);
   
   private static final ExternalJavaScriptLoader LOADER =
         new ExternalJavaScriptLoader(RES.showdownMinJs().getSafeUri().asString());
}
