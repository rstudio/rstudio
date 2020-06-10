/*
 * AceEditorCommandDispatcher.java
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

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.workbench.commands.Commands;

@Singleton
public class AceEditorCommandDispatcher
{
   public interface Binder extends CommandBinder<Commands, AceEditorCommandDispatcher>
   {
   }
   
   @Inject
   public AceEditorCommandDispatcher(Commands commands,
                                     Binder binder,
                                     EventBus events)
   {
      binder.bind(commands, this);
      events_ = events;
   }
   
   @Handler
   public void onYankRegion()
   {
      fireEvent(
            AceEditorCommandEvent.YANK_REGION,
            AceEditorCommandEvent.EXECUTION_POLICY_FOCUSED);
   }
   
   @Handler
   public void onYankBeforeCursor()
   {
      fireEvent(
            AceEditorCommandEvent.YANK_BEFORE_CURSOR,
            AceEditorCommandEvent.EXECUTION_POLICY_FOCUSED);
   }
   
   @Handler
   public void onYankAfterCursor()
   {
      fireEvent(
            AceEditorCommandEvent.YANK_AFTER_CURSOR,
            AceEditorCommandEvent.EXECUTION_POLICY_FOCUSED);
   }
   
   @Handler
   public void onPasteLastYank()
   {
      fireEvent(
            AceEditorCommandEvent.PASTE_LAST_YANK,
            AceEditorCommandEvent.EXECUTION_POLICY_FOCUSED);
   }
   
   @Handler
   public void onInsertAssignmentOperator()
   {
      fireEvent(
            AceEditorCommandEvent.INSERT_ASSIGNMENT_OPERATOR,
            AceEditorCommandEvent.EXECUTION_POLICY_FOCUSED);
   }
   
   @Handler
   public void onInsertPipeOperator()
   {
      fireEvent(
            AceEditorCommandEvent.INSERT_PIPE_OPERATOR,
            AceEditorCommandEvent.EXECUTION_POLICY_FOCUSED);
   }
   
   @Handler
   public void onJumpToMatching()
   {
      fireEvent(
            AceEditorCommandEvent.JUMP_TO_MATCHING,
            AceEditorCommandEvent.EXECUTION_POLICY_FOCUSED);
   }
   
   @Handler
   public void onSelectToMatching()
   {
      fireEvent(
            AceEditorCommandEvent.SELECT_TO_MATCHING,
            AceEditorCommandEvent.EXECUTION_POLICY_FOCUSED);
   }
   
   @Handler
   public void onExpandToMatching()
   {
      fireEvent(
            AceEditorCommandEvent.EXPAND_TO_MATCHING,
            AceEditorCommandEvent.EXECUTION_POLICY_FOCUSED);
   }
   
   @Handler
   public void onAddCursorAbove()
   {
      fireEvent(
            AceEditorCommandEvent.ADD_CURSOR_ABOVE,
            AceEditorCommandEvent.EXECUTION_POLICY_FOCUSED);
   }
   
   @Handler
   public void onAddCursorBelow()
   {
      fireEvent(
            AceEditorCommandEvent.ADD_CURSOR_BELOW,
            AceEditorCommandEvent.EXECUTION_POLICY_FOCUSED);
   }
   
   @Handler
   public void onEditLinesFromStart()
   {
      fireEvent(
            AceEditorCommandEvent.EDIT_LINES_FROM_START,
            AceEditorCommandEvent.EXECUTION_POLICY_FOCUSED);
   }
   
   @Handler
   public void onInsertSnippet()
   {
      fireEvent(
            AceEditorCommandEvent.INSERT_SNIPPET,
            AceEditorCommandEvent.EXECUTION_POLICY_FOCUSED);
   }
   
   @Handler
   public void onMoveLinesUp()
   {
      fireEvent(
            AceEditorCommandEvent.MOVE_LINES_UP,
            AceEditorCommandEvent.EXECUTION_POLICY_FOCUSED);
   }

   @Handler
   public void onMoveLinesDown()
   {
      fireEvent(
            AceEditorCommandEvent.MOVE_LINES_DOWN,
            AceEditorCommandEvent.EXECUTION_POLICY_FOCUSED);
   }

   @Handler
   public void onExpandToLine()
   {
      fireEvent(
            AceEditorCommandEvent.EXPAND_TO_LINE,
            AceEditorCommandEvent.EXECUTION_POLICY_FOCUSED);
   }

   @Handler
   public void onCopyLinesDown()
   {
      fireEvent(
            AceEditorCommandEvent.COPY_LINES_DOWN,
            AceEditorCommandEvent.EXECUTION_POLICY_FOCUSED);
   }

   @Handler
   public void onJoinLines()
   {
      fireEvent(
            AceEditorCommandEvent.JOIN_LINES,
            AceEditorCommandEvent.EXECUTION_POLICY_FOCUSED);
   }

   @Handler
   public void onRemoveLine()
   {
      fireEvent(
            AceEditorCommandEvent.REMOVE_LINE,
            AceEditorCommandEvent.EXECUTION_POLICY_FOCUSED);
   }

   @Handler
   public void onSplitIntoLines()
   {
      fireEvent(
            AceEditorCommandEvent.SPLIT_INTO_LINES,
            AceEditorCommandEvent.EXECUTION_POLICY_FOCUSED);
   }


   @Handler
   public void onBlockIndent()
   {
      fireEvent(
            AceEditorCommandEvent.BLOCK_INDENT,
            AceEditorCommandEvent.EXECUTION_POLICY_FOCUSED);
   }

   @Handler
   public void onBlockOutdent()
   {
      fireEvent(
            AceEditorCommandEvent.BLOCK_OUTDENT,
            AceEditorCommandEvent.EXECUTION_POLICY_FOCUSED);
   }

   @Handler
   public void onReindent()
   {
      fireEvent(
            AceEditorCommandEvent.REINDENT,
            AceEditorCommandEvent.EXECUTION_POLICY_FOCUSED);
   }

   // Private methods ----
   
   private void fireEvent(int type, int policy)
   {
      AceEditorCommandEvent event = new AceEditorCommandEvent(type, policy);
      if (Satellite.isCurrentWindowSatellite())
         events_.fireEventToSatellite(event, WindowEx.get());
      else
         events_.fireEvent(event);
   }
   
   // Private fields ----
   
   private final EventBus events_;
}
