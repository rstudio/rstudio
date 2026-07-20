/*
 * HelpPopoutParams.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
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
package org.rstudio.studio.client.workbench.views.help;

import com.google.gwt.core.client.JavaScriptObject;

public class HelpPopoutParams extends JavaScriptObject
{
   protected HelpPopoutParams()
   {
   }

   public static final native HelpPopoutParams create(String url, String title)
   /*-{
      return {
         url: url,
         title: title
      };
   }-*/;

   public final native String getUrl()
   /*-{
      return this.url;
   }-*/;

   public final native String getTitle()
   /*-{
      return this.title;
   }-*/;
}
