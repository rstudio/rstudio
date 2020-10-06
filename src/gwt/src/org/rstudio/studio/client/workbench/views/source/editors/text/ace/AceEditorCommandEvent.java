/*
 * AceEditorCommandEvent.java
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

import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;

import com.google.gwt.event.shared.EventHandler;

@JavaScriptSerializable
public class AceEditorCommandEvent extends CrossWindowEvent<AceEditorCommandEvent.Handler>
{
   public static final int YANK_REGION                =  1;
   public static final int YANK_BEFORE_CURSOR         =  2;
   public static final int YANK_AFTER_CURSOR          =  3;
   public static final int PASTE_LAST_YANK            =  4;
   public static final int INSERT_ASSIGNMENT_OPERATOR =  5;
   public static final int INSERT_PIPE_OPERATOR       =  6;
   public static final int JUMP_TO_MATCHING           =  7;
   public static final int SELECT_TO_MATCHING         =  8;
   public static final int EXPAND_TO_MATCHING         =  9;
   public static final int ADD_CURSOR_ABOVE           = 10;
   public static final int ADD_CURSOR_BELOW           = 11;
   public static final int EDIT_LINES_FROM_START      = 12;
   public static final int INSERT_SNIPPET             = 13;
   public static final int MOVE_LINES_UP              = 14;
   public static final int MOVE_LINES_DOWN            = 15;
   public static final int EXPAND_TO_LINE             = 16;
   public static final int COPY_LINES_DOWN            = 17;
   public static final int JOIN_LINES                 = 18;
   public static final int REMOVE_LINE                = 19;
   public static final int SPLIT_INTO_LINES           = 20;
   public static final int BLOCK_INDENT               = 21;
   public static final int BLOCK_OUTDENT              = 22;
   public static final int REINDENT                   = 23;

   public static final int EXECUTION_POLICY_FOCUSED = 1;
   public static final int EXECUTION_POLICY_ALWAYS  = 2;

   // Required for '@JavaScriptSerializable' but is otherwise unused
   public AceEditorCommandEvent()
   {
      this(-1, -1);
   }

   public AceEditorCommandEvent(int command, int policy)
   {
      command_ = command;
      policy_ = policy;
   }

   public final int getCommand()
   {
      return command_;
   }

   public final int getExecutionPolicy()
   {
      return policy_;
   }

   private final int command_;
   private final int policy_;

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

   public static final Type<Handler> TYPE = new Type<>();

}
