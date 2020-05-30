/*
 * SourceLocation.java
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

package org.rstudio.studio.client.common.synctex.model;

import com.google.gwt.core.client.JavaScriptObject;

public class SourceLocation extends JavaScriptObject
{
   protected SourceLocation()
   {
   }
   

   public final static native SourceLocation create(String file,
                                                    int line,
                                                    int column,
                                                    boolean fromClick) /*-{
      var location = new Object();
      location.file = file;
      location.line = line;
      location.column = column;
      location.from_click = fromClick;
      return location;
   }-*/;
   
   public native final String getFile() /*-{
      return this.file;
   }-*/;
   
   public native final int getLine() /*-{
      return this.line;
   }-*/;

   public native final int getColumn() /*-{
      return this.column;
   }-*/;
   
   public native final boolean fromClick() /*-{
      return this.from_click;
   }-*/;

}
