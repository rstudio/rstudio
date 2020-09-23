/*
 * DocTabDragParams.java
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

import com.google.gwt.core.client.JavaScriptObject;

public class DocTabDragParams extends JavaScriptObject
{
   protected DocTabDragParams()
   {
   }
   
   public final native static DocTabDragParams create(String docId, 
         int tabWidth, int cursorOffset) /*-{
     return {
        doc_id         : docId,
        tab_width      : tabWidth,
        cursor_offset  : cursorOffset,
        source_position: null,
        display_name: null
     };
   }-*/;

   public final native static DocTabDragParams create(String docId,
                                                      SourcePosition position) /*-{
       return {
           doc_id         : docId,
           tab_width      : 0,
           cursor_offset  : 0,
           source_position: position,
           display_name   : ""
       }
   }-*/;

   public final native static DocTabDragParams create(String docId,
         SourcePosition position, String displayName) /*-{
     return {
        doc_id         : docId,
        tab_width      : 0,
        cursor_offset  : 0,
        source_position: position,
        display_name   : displayName
     }
   }-*/;
   
   public final native String getDocId() /*-{
      return this.doc_id;
   }-*/;
   
   public final native int getTabWidth() /*-{
      return this.tab_width;
   }-*/;
   
   public final native int getCursorOffset() /*-{
      return this.cursor_offset;
   }-*/;

   public final native SourcePosition getSourcePosition() /*-{
      return this.source_position;
   }-*/;
   
   public final native void setSourcePosition(SourcePosition position) /*-{
      this.source_position = position;
   }-*/;

   public final native String getDisplayName() /*-{
      return this.display_name;
   }-*/;

   public final native void setDisplayName(String displayName) /*-{
      this.display_name = displayName;
   }-*/;

}
