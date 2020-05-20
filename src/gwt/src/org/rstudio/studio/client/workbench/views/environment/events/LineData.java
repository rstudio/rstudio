/*
 * LineData.java
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
package org.rstudio.studio.client.workbench.views.environment.events;

import com.google.gwt.core.client.JavaScriptObject;

public class LineData extends JavaScriptObject
{
   protected LineData()
   {
   }

   public final native int getLineNumber() /*-{
       return this.line_number;
   }-*/;

   public final native int getEndLineNumber() /*-{
       return this.end_line_number;
   }-*/;
     
   public final native int getCharacterNumber() /*-{
       return this.character_number;
   }-*/;
     
   public final native int getEndCharacterNumber() /*-{
       return this.end_character_number;
   }-*/;
}
