/*
 * DiffChunkActionEvent.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.vcs.common.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.diff.DiffChunk;

public class DiffChunkActionEvent extends GwtEvent<DiffChunkActionEvent.Handler>
{
   public enum Action
   {
      Stage,
      Unstage,
      Discard
   }

   public DiffChunkActionEvent(Action action, DiffChunk diffChunk)
   {
      action_ = action;
      diffChunk_ = diffChunk;
   }

   public Action getAction()
   {
      return action_;
   }

   public DiffChunk getDiffChunk()
   {
      return diffChunk_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onDiffChunkAction(this);
   }

   private final Action action_;
   private final DiffChunk diffChunk_;

   public static final Type<Handler> TYPE = new Type<>();

   public interface Handler extends EventHandler
   {
      void onDiffChunkAction(DiffChunkActionEvent event);
   }

}
