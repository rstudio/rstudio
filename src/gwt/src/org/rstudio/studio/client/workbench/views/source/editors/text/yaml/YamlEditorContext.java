/*
 * YamlCompletionParams.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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

package org.rstudio.studio.client.workbench.views.source.editors.text.yaml;

import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;

import com.google.gwt.core.client.JavaScriptObject;

public class YamlCompletionParams extends JavaScriptObject
{
   protected YamlCompletionParams()
   {
   }
   
   public static native YamlCompletionParams create(
      String location, String line, String code, Position position) /*-{
      return { 
         location: location,
         line: line, 
         code: code,
         position: position
      };
   }-*/;
   
   public native final String getLocation() /*-{
      return this.location;
   }-*/;
   
   public native final String getLine() /*-{
      return this.line;
   }-*/;
   
   public native final String getCode() /*-{
      return this.code;
   }-*/;
   
   public native final Position getPosition() /*-{
      return this.position;
   }-*/;
   
}
