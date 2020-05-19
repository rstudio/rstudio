/*
 * UserPrefDefinition.java
 *
 * Copyright (C) 2009-20 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.prefs.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

public class UserPrefDefinition extends JavaScriptObject
{
   protected UserPrefDefinition()
   {
   }

   public final native String getType() /*-{
      return this.type;
   }-*/;

   public final native String getTitle() /*-{
      return this.title;
   }-*/;

   public final native String getDescription() /*-{
      return this.description;
   }-*/;

   public final native JsArrayString getEnumValues() /*-{
      return this["enum"];
   }-*/;
}
