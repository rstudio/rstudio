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
import org.rstudio.studio.client.application.events.EventBus;
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
   
   // Private methods ----
   
   private void fireEvent(CommandType type, ExecutionPolicy policy)
   {
      events_.fireEvent(new AceEditorCommandEvent(type, policy));
   }
   
   // Private fields ----
   
   private final EventBus events_;
}
