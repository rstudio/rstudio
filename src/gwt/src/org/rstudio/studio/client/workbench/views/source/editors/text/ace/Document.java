/*
 * Document.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import com.google.gwt.core.client.JavaScriptObject;

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

   public native final int getLength() /*-{
      return this.getLength();
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
