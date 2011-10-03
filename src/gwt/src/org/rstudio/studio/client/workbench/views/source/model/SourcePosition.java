/*
 * SourcePosition.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.model;

import com.google.gwt.core.client.JavaScriptObject;

public class SourcePosition extends JavaScriptObject
{
   protected SourcePosition() {}

   public static native SourcePosition create(int row, 
                                              int column) /*-{
      return {context: null, row: row, column: column};
   }-*/;

   public static native SourcePosition create(String context, 
                                              int row, 
                                              int column) /*-{
      return {context: context, row: row, column: column};
   }-*/;
   
   /*
    * NOTE: optional context for editors that have multiple internal
    * contexts with independent rows & columns (e.g. code browser)
    * this will be null for some implmentations including TextEditingTarget
    */
   public native final String getContext() /*-{
      return this.context;
   }-*/;

   public native final int getRow() /*-{
      return this.row;
   }-*/;

   public native final int getColumn() /*-{
      return this.column;
   }-*/;
}
