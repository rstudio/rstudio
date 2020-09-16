/*
 * ChunkDefinition.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.rmd;

import org.rstudio.studio.client.rmarkdown.model.RmdChunkOptions;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class ChunkDefinition extends JavaScriptObject
{
   public static final String LINE_WIDGET_TYPE = "ChunkOutput";
   
   protected ChunkDefinition()
   {
   }
   
   public interface Provider
   {
      public JsArray<ChunkDefinition> getChunkDefs();
   }
   
   public static native final ChunkDefinition create(int row,
                                                 int rowCount,
                                                 boolean visible,
                                                 int expansionState,
                                                 RmdChunkOptions options,
                                                 String documentId,
                                                 String chunkId,
                                                 String chunkLabel) /*-{
      return {
        row: row,
        row_count: rowCount,
        visible: visible,
        expansion_state: expansionState,
        options: options,
        document_id: documentId,
        chunk_id: chunkId,
        chunk_label: chunkLabel
      };
   }-*/;
   
   public final ChunkDefinition with(int row, String chunkLabel)
   {
      return ChunkDefinition.create(row, getRowCount(), getVisible(), 
            getExpansionState(), getOptions(), getDocumentId(),
            getChunkId(), chunkLabel);
   }
   
   public native final int getRow()  /*-{
      return this.row;
   }-*/;
   
   public native final int getRowCount()  /*-{
      return this.row_count;
   }-*/;
   
   public native final boolean getVisible() /*-{
      return this.visible;
   }-*/;   
   
   public native final int getExpansionState() /*-{
      return this.expansion_state || 0;
   }-*/;
   
   public native final String getDocumentId() /*-{
      return this.document_id;
   }-*/;
   
   public native final String getChunkId() /*-{
      return this.chunk_id;
   }-*/;
   
   public native final String getChunkLabel() /*-{
      return this.chunk_label;
   }-*/;
   
   public native final void setChunkLabel(String label) /*-{
      this.chunk_label = label;
   }-*/;
   
   public native final void setExpansionState(int state) /*-{
      this.expansion_state = state;
   }-*/;

   public native final RmdChunkOptions getOptions() /*-{
      return this.options || {};
   }-*/;
   
   public native final void setOptions(RmdChunkOptions options) /*-{
      this.options = options;
   }-*/;
   
   public native final void setRow(int row) /*-{
      this.row = row;
   }-*/;
   
   public final boolean equalTo(ChunkDefinition other)
   {
      return getRow() == other.getRow() &&
             getRowCount() == other.getRowCount() &&
             getVisible() == other.getVisible() &&
             getChunkId() == other.getChunkId() &&
             getExpansionState() == other.getExpansionState() &&
             getOptions().equalTo(other.getOptions());
   }
   
   public final static boolean equalTo(JsArray<ChunkDefinition> a, 
                                       JsArray<ChunkDefinition> b)
   {
      if (a.length() != b.length())
         return false;
      
      for (int i = 0; i<a.length(); i++)
         if (!a.get(i).equalTo(b.get(i)))
            return false;
      
      return true;
   }
}
