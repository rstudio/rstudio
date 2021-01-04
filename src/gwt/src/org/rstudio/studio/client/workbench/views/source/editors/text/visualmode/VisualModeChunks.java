/*
 * VisualModeChunks.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.studio.client.panmirror.ui.PanmirrorUIChunks;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.Scope;
import org.rstudio.studio.client.workbench.views.source.editors.text.ScopeList;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetScopeHelper;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.ChunkDefinition;
import org.rstudio.studio.client.workbench.views.source.editors.text.rmd.TextEditingTargetNotebook;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.Command;

public class VisualModeChunks implements ChunkDefinition.Provider
{
   public VisualModeChunks(DocUpdateSentinel sentinel,
                           DocDisplay display,
                           TextEditingTarget target, 
                           VisualModeEditorSync sync)
   {
      target_ = target;
      sentinel_ = sentinel;
      parent_ = display;
      sync_ = sync;
      chunks_ = new ArrayList<VisualModeChunk>();
   }

   public PanmirrorUIChunks uiChunks()
   {
      PanmirrorUIChunks chunks = new PanmirrorUIChunks();
      chunks.createChunkEditor = (type, index, callbacks) ->
      {
         // only know how to create ace instances right now
         if (!type.equals("ace"))
         {
            Debug.logToConsole("Unknown chunk editor type: " + type);
            return null;
         }
         
         VisualModeChunk chunk = new VisualModeChunk(
               index, callbacks, sentinel_, target_, sync_);

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

   /**
    * Gets the visual mode chunk nearest to the given position.
    *
    * @param pos The position in visual mode (usually of the cursor)
    * @param dir The direction in which to look
    *
    * @return The nearest chunk, or null if no chunks are found.
    */
   public VisualModeChunk getNearestChunk(int pos, int dir)
   {
      // Candidate for nearest chunk
      VisualModeChunk nearest = null;

      // Distance of nearest chunk from the given position
      int best = 0;

      for (VisualModeChunk chunk: chunks_)
      {
         // Check distance from position
         int chunkPos = chunk.getVisualPosition();
         int offset = dir == TextEditingTargetScopeHelper.PREVIOUS_CHUNKS ?
            pos - chunkPos : chunkPos - pos;
         if (offset < 0)
         {
            // Skip if past target
            continue;
         }

         if (nearest == null || offset < best)
         {
            // Record this chunk if it's the nearest we've found so far
            nearest = chunk;
            best = offset;
         }
      }

      // Return nearest chunk; could be null if e.g., there are no chunks after the cursor
      // and FOLLOWING_CHUNKS was specified
      return nearest;
   }
   
   /**
    * Find the visual mode chunk editor corresponding to the given visual
    * position.
    * 
    * @param pos The visual position (as reported by prosemirror)
    * 
    * @return A visual mode chunk editor at the given position, or null if one
    *   was not found.
    */
   public VisualModeChunk getChunkAtVisualPosition(int pos)
   {
      for (VisualModeChunk chunk: chunks_)
      {
         if (chunk.getVisualPosition() == pos)
         {
            return chunk;
         }
      }
      return null;
   }
   
   /**
    * Performs an arbitrary command after synchronizing the selection state of
    * the child editor to the parent.
    * 
    * @param command The command to perform. The new position of the cursor in
    *    source mode is passed as an argument.
    */
   public void performWithSelection(CommandWithArg<Position> command)
   {
      withActiveChunk((chunk) ->
      {
         if (chunk == null)
         {
            command.execute(null);
         }
         else
         {
            chunk.performWithSelection(command);
         }
      });
   }
   
   /**
    * Make a list of the chunk definitions known in visual mode.
    */
   public JsArray<ChunkDefinition> getChunkDefs()
   {
      JsArray<ChunkDefinition> defs = JsArray.createArray().cast();
      ScopeList scopes = new ScopeList(parent_);
      for (VisualModeChunk chunk: chunks_)
      {
         ChunkDefinition def = chunk.getDefinition();
         Scope scope = chunk.getScope();
         if (def != null && scope != null)
         {
            int row = scope.getEnd().getRow();
            defs.push(def.with(row, TextEditingTargetNotebook.getKnitrChunkLabel(
                        row, parent_, scopes)));
         }
      }
      return defs;
   }
   
   /**
    * Sets the execution state of a range of lines in the visual editor.
    * 
    * @param start The first line
    * @param end The last line
    * @param state The execution state
    */
   public void setChunkLineExecState(int start, int end, int state)
   {
      for (VisualModeChunk chunk: chunks_)
      {
         Scope scope = chunk.getScope();
         if (scope == null)
         {
            // Expected if the position of this chunk in the code editor isn't
            // known
            continue;
         }
         
         if (start >= scope.getPreamble().getRow() &&
             end <= scope.getEnd().getRow())
         {
            int offset = scope.getPreamble().getRow();
            chunk.setLineExecState(start - offset, end - offset, state);
            break;
         }
      }
   }
   
   /**
    * Sets the execution state of a single chunk.
    * 
    * @param target The location of the chunk
    * @param state The chunk's new state
    */
   public void setChunkState(Scope target, int state)
   {
      for (VisualModeChunk chunk: chunks_)
      {
         Scope scope = chunk.getScope();
         if (scope != null && 
             scope.getPreamble().getRow() == target.getPreamble().getRow())
         {
            chunk.setState(state);
            break;
         }
      }
   }

   /**
    * Executes a command with the currently active visual chunk (or null if no chunk is active)
    *
    * @param command The command to execute.
    */
   private void withActiveChunk(CommandWithArg<VisualModeChunk> command)
   {
      for (VisualModeChunk chunk: chunks_)
      {
         if (chunk.isActive())
         {
            command.execute(chunk);
            return;
         }
      }

      // No chunk found; execute without one
      command.execute(null);
   }

   private final VisualModeEditorSync sync_;
   private final List<VisualModeChunk> chunks_;
   private final DocUpdateSentinel sentinel_;
   private final DocDisplay parent_;
   private final TextEditingTarget target_;
}
