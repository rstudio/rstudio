/*
 * AceEditorCommandDispatcher.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorCommandEvent.CommandType;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorCommandEvent.ExecutionPolicy;

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
            CommandType.YANK_REGION,
            ExecutionPolicy.FOCUSED);
   }
   
   @Handler
   public void onYankBeforeCursor()
   {
      fireEvent(
            CommandType.YANK_BEFORE_CURSOR,
            ExecutionPolicy.FOCUSED);
   }
   
   @Handler
   public void onYankAfterCursor()
   {
      fireEvent(
            CommandType.YANK_AFTER_CURSOR,
            ExecutionPolicy.FOCUSED);
   }
   
   @Handler
   public void onPasteLastYank()
   {
      fireEvent(
            AceEditorCommandEvent.CommandType.PASTE_LAST_YANK,
            ExecutionPolicy.FOCUSED);
   }
   
   @Handler
   public void onInsertAssignmentOperator()
   {
      fireEvent(
            AceEditorCommandEvent.CommandType.INSERT_ASSIGNMENT_OPERATOR,
            ExecutionPolicy.FOCUSED);
   }
   
   @Handler
   public void onInsertPipeOperator()
   {
      fireEvent(
            AceEditorCommandEvent.CommandType.INSERT_PIPE_OPERATOR,
            ExecutionPolicy.FOCUSED);
   }
   
   @Handler
   public void onJumpToMatching()
   {
      fireEvent(
            AceEditorCommandEvent.CommandType.JUMP_TO_MATCHING,
            ExecutionPolicy.FOCUSED);
   }
   
   @Handler
   public void onSelectToMatching()
   {
      fireEvent(
            AceEditorCommandEvent.CommandType.SELECT_TO_MATCHING,
            ExecutionPolicy.FOCUSED);
   }
   
   @Handler
   public void onExpandToMatching()
   {
      fireEvent(
            AceEditorCommandEvent.CommandType.EXPAND_TO_MATCHING,
            ExecutionPolicy.FOCUSED);
   }
   
   @Handler
   public void onAddCursorAbove()
   {
      fireEvent(
            AceEditorCommandEvent.CommandType.ADD_CURSOR_ABOVE,
            ExecutionPolicy.FOCUSED);
   }
   
   @Handler
   public void onAddCursorBelow()
   {
      fireEvent(
            AceEditorCommandEvent.CommandType.ADD_CURSOR_BELOW,
            ExecutionPolicy.FOCUSED);
   }
   
   
   // Private methods ----
   
   private void fireEvent(CommandType type, ExecutionPolicy policy)
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
