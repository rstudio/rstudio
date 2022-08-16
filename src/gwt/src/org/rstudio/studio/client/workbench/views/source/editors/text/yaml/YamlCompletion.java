/*
 * YamlCompletion.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.workbench.views.source.editors.text.yaml;

import com.google.gwt.core.client.JavaScriptObject;

public class YamlCompletion extends JavaScriptObject
{
   protected YamlCompletion()
   {
   }
   
   public native final String getType() /*-{
      return this.type;
   }-*/;
   
   public native final String getValue() /*-{
      return this.value;
   }-*/;
   
   public native final String getDisplay() /*-{
      return this.display || this.value;
   }-*/;
   
   public native final String getDescription() /*-{
      return this.description;
   }-*/;
   
   public native final boolean getSuggestOnAccept() /*-{
      return !!this.suggest_on_accept;
   }-*/;
   
   public native final boolean getReplaceToEnd() /*-{
      return !!this.replace_to_end;
   }-*/;
   
}
