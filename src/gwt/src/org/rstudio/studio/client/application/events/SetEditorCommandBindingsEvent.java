/*
 * SetEditorCommandBindingsEvent.java
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
package org.rstudio.studio.client.application.events;

import java.util.List;

import org.rstudio.core.client.command.KeySequence;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class SetEditorCommandBindingsEvent extends GwtEvent<SetEditorCommandBindingsEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onSetEditorCommandBindings(SetEditorCommandBindingsEvent event);
   }

   public SetEditorCommandBindingsEvent(String id, List<KeySequence> keys)
   {
      id_ = id;
      keys_ = keys;
   }

   public String getId() { return id_; }
   public List<KeySequence> getKeySequences() { return keys_; }

   private final String id_;
   private final List<KeySequence> keys_;

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onSetEditorCommandBindings(this);
   }

   public static final Type<Handler> TYPE = new Type<>();
}
