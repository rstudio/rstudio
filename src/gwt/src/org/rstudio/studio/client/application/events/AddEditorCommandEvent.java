/*
 * AddEditorCommandEvent.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.application.events;

import org.rstudio.core.client.command.KeyboardShortcut.KeySequence;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class AddEditorCommandEvent extends GwtEvent<AddEditorCommandEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onAddEditorCommand(AddEditorCommandEvent event);
   }

   public AddEditorCommandEvent(String id, KeySequence keys, boolean replace)
   {
      id_ = id;
      keys_ = keys;
      replace_ = replace;
   }
   
   public String getId() { return id_; }
   public KeySequence getKeySequence() { return keys_; }
   public boolean replaceOldBindings() { return replace_; }
   
   private final String id_;
   private final KeySequence keys_;
   private final boolean replace_;
   
   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onAddEditorCommand(this);
   }

   public static final Type<Handler> TYPE = new Type<Handler>();
}
