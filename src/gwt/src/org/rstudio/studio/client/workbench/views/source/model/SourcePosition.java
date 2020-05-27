/*
 * SourcePosition.java
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
package org.rstudio.studio.client.workbench.views.source.model;

import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;

import com.google.gwt.core.client.JavaScriptObject;

public class SourcePosition extends JavaScriptObject
{
   protected SourcePosition() {}

   public static native SourcePosition create(int row, 
                                              int column) /*-{
      return {context: null, row: row, column: column, scroll_position: -1};
   }-*/;

   public static native SourcePosition create(String context, 
                                              int row, 
                                              int column,
                                              int scrollPosition) /*-{
      return {context: context, row: row, column: column, scroll_position: scrollPosition};
   }-*/;
   
   /*
    * NOTE: optional context for editors that have multiple internal
    * contexts with independent rows & columns (e.g. code browser)
    * this will be null for some implementations including TextEditingTarget
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
   
   /*
    * NOTE: optional scroll position -- can be -1 to indicate no 
    * scroll position recorded
    */
   public native final int getScrollPosition() /*-{
      return this.scroll_position;
   }-*/;
   
   public final boolean isSameRowAs(SourcePosition other) 
   {
      if (getContext() == null && other.getContext() == null)
         return other.getRow() == getRow();
      else if (getContext() == null && other.getContext() != null)
         return false;
      else if (other.getContext() == null && getContext() != null)
         return false;
      else
         return other.getContext() == getContext() &&
                (other.getRow() == getRow());
   }
   
   public final Position asPosition()
   {
      return Position.create(getRow(), getColumn());
   }
}
