/*
 * Document.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

public class Document extends JavaScriptObject
{
   protected Document()
   {}

   public native final void setValue(String value) /*-{
      this.setValue(value);
   }-*/;

   public native final String getLine(int row) /*-{
      return this.getLine(row);
   }-*/;
   
   public native final JsArrayString getLines() /*-{
      return this.$lines;
   }-*/;

   public native final int getLength() /*-{
      return this.getLength();
   }-*/;
   
   public native final Position indexToPosition(int index, int startRow) /*-{
      return this.indexToPosition(index, startRow);
   }-*/;
   
   public native final int positionToIndex(Position pos, int startRow) /*-{
      return this.positionToIndex(pos, startRow);
   }-*/;

   public final String getDocumentDump()
   {
      StringBuilder output = new StringBuilder();
      for (int i = 0; i < getLength(); i++)
      {
         String line = getLine(i);
         for (int j = 0; j < line.length(); j++)
         {
            char c = line.charAt(j);
            output.append((int)c);
            output.append(' ');
         }

         output.append(". \n");
      }
      return output.toString();
   }
}
