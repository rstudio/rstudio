/*
 * VisualModeChunks.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.visualmode;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.Debug;
import org.rstudio.studio.client.panmirror.ui.PanmirrorUIChunks;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;

public class VisualModeChunks
{
   public VisualModeChunks(DocUpdateSentinel sentinel,
                           DocDisplay display,
                           TextEditingTarget target)
   {
      target_ = target;
      sentinel_ = sentinel;
      parent_ = display;
      chunks_ = new ArrayList<VisualModeChunk>();
   }

   public PanmirrorUIChunks uiChunks()
   {
      PanmirrorUIChunks chunks = new PanmirrorUIChunks();
      chunks.createChunkEditor = (type, index) ->
      {

         // only know how to create ace instances right now
         if (!type.equals("ace"))
         {
            Debug.logToConsole("Unknown chunk editor type: " + type);
            return null;
         }
         
         VisualModeChunk chunk = new VisualModeChunk(
               index, sentinel_, parent_, 
               target_.getNotebook(), target_.getRCompletionContext());

         // Add the chunk to our index, and remove it when the underlying chunk
         // is removed in Prosemirror
         chunks_.add(chunk);
         chunk.addDestroyHandler(() ->
         {
            chunks_.remove(chunk);
         });

         return chunk.getEditor();
         
      };
      return chunks;
   }
   
   /**
    * Finds the visual mode chunk editor corresponding to a given document row.
    * 
    * @param row The document row.
    * @return A visual mode chunk editor at the given row, or null if one was
    *   not found.
    */
   public VisualModeChunk getChunkAtRow(int row)
   {
      for (VisualModeChunk chunk: chunks_)
      {
         Scope scope = chunk.getScope();
         if (scope == null)
            continue;
         if (row >= scope.getPreamble().getRow() &&
             row <= scope.getEnd().getRow())
         {
            return chunk;
         }
      }
      return null;
   }
   
   private final List<VisualModeChunk> chunks_;
   private final DocUpdateSentinel sentinel_;
   private final DocDisplay parent_;
   private final TextEditingTarget target_;
}
