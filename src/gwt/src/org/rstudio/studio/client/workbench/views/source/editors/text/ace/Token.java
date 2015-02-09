/*
 * Token.java
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

public class Token extends JavaScriptObject
{
   protected Token() {}
   
   public static native final Token create() /*-{
      return {
         "value": "",
         "type": "",
         "column": 0
      };
   }-*/;
   
   public static native final Token create(String value,
                                           String type,
                                           int column) /*-{
      return {
         "value": value,
         "type": type,
         "column": column
      };
   }-*/;

   public native final String getValue() /*-{
      return this.value;
   }-*/;

   public native final String getType() /*-{
      return this.type;
   }-*/;
   
   public native final int getColumn() /*-{
      return this.column;
   }-*/;
   
   // NOTE: Tokens attached to a document should be considered immutable;
   // use setters only when applying to a tokenized line separate from an
   // active editor!
   public native final void setValue(String value) /*-{
      this.value = value;
   }-*/;
}
