/*
 * VisualModeChunks.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.panmirror.ui.PanmirrorUIChunks;
import org.rstudio.studio.client.workbench.views.output.lint.model.LintItem;
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

public class VisualModeChunks implements ChunkDefinition.Provider
{
   public VisualModeChunks(DocUpdateSentinel sentinel,
                           DocDisplay display,
                           TextEditingTarget target,
                           final ArrayList<HandlerRegistration> releaseOnDismiss,
                           VisualModeEditorSync sync)
   {
      target_ = target;
      sentinel_ = sentinel;
      parent_ = display;
      sync_ = sync;
      chunks_ = new ArrayList<>();

      // Timer to auto-save the collapsed state of visual mode chunks
      saveCollapseTimer_ = new Timer()
      {
         @Override
         public void run()
         {
            saveCollapseState();
         }
      };

      // Load initial collapsed chunk state from doc property bag
      loadCollapsedState(
         sentinel_.getProperty(TextEditingTarget.RMD_VISUAL_MODE_COLLAPSED_CHUNKS));
   }

   public PanmirrorUIChunks uiChunks()
   {
      PanmirrorUIChunks chunks = new PanmirrorUIChunks();
      chunks.createChunkEditor = (type, ele, index, classes, callbacks) ->
      {
         // only know how to create ace instances right now
         if (!type.equals("ace"))
         {
            Debug.logToConsole("Unknown chunk editor type: " + type);
            return null;
         }

         // Read expansion state from document property
         boolean expanded = true;
         int pos = callbacks.getPos.getVisualPosition();
         if (collapsedChunkPos_.contains(pos))
         {
            expanded = false;
         }

         VisualModeChunk chunk = new VisualModeChunk(
               ele, index, expanded, classes, callbacks, sentinel_, target_, sync_);

         // Add the chunk to our index, and remove it when the underlying chunk
         // is removed in Prosemirror
         chunks_.add(chunk);
         chunk.addDestroyHandler(() ->
         {
            chunks_.remove(chunk);
         });

         return chunk.getEditor();
         
      };
      chunks.setChunksExpanded = (expanded ->
      {
         // Go through each chunk and set its expansion state to match the one provided
         for (VisualModeChunk chunk: chunks_)
         {
            chunk.setExpanded(expanded);
         }
      });

      // Save the collapse state of the visual mode chunks.
      target_.getDocDisplay().addValueChangeHandler(evt ->
      {
         nudgeSaveCollapseState();
      });

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
            chunk.setRowState(start - offset, end - offset, state, null);
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

   /**
    * Nudges the timer that saves the expansion state of each code chunk.
    */
   public void nudgeSaveCollapseState()
   {
      if (saveCollapseTimer_.isRunning())
      {
         saveCollapseTimer_.cancel();
      }
      saveCollapseTimer_.schedule(1000);
   }

   /**
    * Shows lint items in the visual editor.
    *
    * @param lint An array of lint items.
    */
   public void showLint(JsArray<LintItem> lint)
   {
      // Accumulator for each chunk's lint
      Map<VisualModeChunk, JsArray<LintItem>> chunkLint = new HashMap<>();

      for (int i = 0; i < lint.length(); i++)
      {
         LintItem item = lint.get(i);
         for (VisualModeChunk chunk: chunks_)
         {
            // Get the scope (editor position) associated with the chunk
            Scope scope = chunk.getScope();
            if (scope == null)
               continue;

            // Does this lint item begin within the chunk?
            if (item.getStartRow() >= scope.getBodyStart().getRow() &&
                item.getStartRow() <= scope.getEnd().getRow())
            {
               // Adjust the offsets of the lint item to correlate with the chunk editor
               int offset = scope.getPreamble().getRow();
               item.setStartRow(item.getStartRow() - offset);
               item.setEndRow(item.getEndRow() - offset);

               // Create a key for this chunk if needed, then push the lint into the items for
               // this chunk. We accumulate this instead of setting it right away since each
               // time we call the chunk's setLint method it removes any previously set
               // lint markers.
               if (!chunkLint.containsKey(chunk))
               {
                  chunkLint.put(chunk, JsArray.createArray().cast());
               }
               chunkLint.get(chunk).push(item);

               // Found the chunk, no need keep looking
               break;
            }
         }
      }

      // Push the accumulated lint into each chunk
      for (VisualModeChunk chunk: chunkLint.keySet())
      {
         JsArray<LintItem> items = chunkLint.get(chunk);
         if (items != null && items.length() > 0)
         {
            chunk.showLint(items, true);
         }
      }
   }

   /**
    * Saves the expansion state of all chunks as a document property.
    */
   private void saveCollapseState()
   {
      ArrayList<Integer> positions = new ArrayList<Integer>();

      // Loop over each chunk and make a list of those that are collapsed.
      for (VisualModeChunk chunk : chunks_)
      {
         if (!chunk.getExpanded())
         {
            positions.add(chunk.getVisualPosition());
         }
      }

      // Convert to a string and bail out if nothing has changed.
      String val = StringUtil.join(positions, ",");
      if (val == collapseState_)
      {
         return;
      }

      // Write the new state to a document property.
      collapseState_ = val;
      sentinel_.setProperty(TextEditingTarget.RMD_VISUAL_MODE_COLLAPSED_CHUNKS, val);
   }

   /**
    * Load the collapse state of all chunks from a document property value.
    *
    * @param state The expansion state, a comma-delimited string of positions.
    */
   private void loadCollapsedState(String state)
   {
      collapsedChunkPos_ = new ArrayList<>();
      if (StringUtil.isNullOrEmpty(state))
      {
         // No chunks collapsed
         return;
      }

      // Split value to form array of collapsed chunks
      String[] positions = state.split(",");
      for (String pos: positions)
      {
         int position = StringUtil.parseInt(pos, -1);
         if (position >= 0)
         {
            collapsedChunkPos_.add(position);
         }
      }

      // Save current collapsed state to avoid unnecessary RPCs
      collapseState_ = state;
   }


   private final VisualModeEditorSync sync_;
   private final List<VisualModeChunk> chunks_;
   private final DocUpdateSentinel sentinel_;
   private final DocDisplay parent_;
   private final TextEditingTarget target_;
   private final Timer saveCollapseTimer_;
   private ArrayList<Integer> collapsedChunkPos_;
   private String collapseState_;
}
