/*
 * DiffChunkActionEvent.java
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
package org.rstudio.studio.client.workbench.views.vcs.common.events;

import com.google.gwt.event.shared.GwtEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.diff.DiffChunk;

public class DiffChunkActionEvent extends GwtEvent<DiffChunkActionHandler>
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
   public Type<DiffChunkActionHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(DiffChunkActionHandler handler)
   {
      handler.onDiffChunkAction(this);
   }

   private Action action_;
   private DiffChunk diffChunk_;

   public static final Type<DiffChunkActionHandler> TYPE = new Type<DiffChunkActionHandler>();
}
