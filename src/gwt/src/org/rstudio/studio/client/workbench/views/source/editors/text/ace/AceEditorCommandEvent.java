/*
 * AceEditorCommandEvent.java
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

import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;

import com.google.gwt.event.shared.EventHandler;

@JavaScriptSerializable
public class AceEditorCommandEvent extends CrossWindowEvent<AceEditorCommandEvent.Handler>
{
   public static enum CommandType
   {
      YANK_REGION,
      YANK_BEFORE_CURSOR,
      YANK_AFTER_CURSOR,
      PASTE_LAST_YANK,
      INSERT_ASSIGNMENT_OPERATOR,
      INSERT_PIPE_OPERATOR,
      JUMP_TO_MATCHING,
      SELECT_TO_MATCHING,
      EXPAND_TO_MATCHING,
      ADD_CURSOR_ABOVE,
      ADD_CURSOR_BELOW
   }
   
   public static enum ExecutionPolicy
   {
      FOCUSED,
      ALWAYS;
   }
   
   // Required for '@JavaScriptSerializable' but is otherwise unused
   public AceEditorCommandEvent()
   {
      this(null, null);
   }
   
   public AceEditorCommandEvent(CommandType command, ExecutionPolicy policy)
   {
      command_ = command;
      policy_ = policy;
   }
   
   public final CommandType getCommand()
   {
      return command_;
   }
   
   public final ExecutionPolicy getExecutionPolicy()
   {
      return policy_;
   }
   
   private final CommandType command_;
   private final ExecutionPolicy policy_;
   
   // Boilerplate ----
   
   public interface Handler extends EventHandler
   {
      void onEditorCommand(AceEditorCommandEvent event);
   }
   
   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onEditorCommand(this);
   }
   
   public static final Type<Handler> TYPE = new Type<Handler>();

}
