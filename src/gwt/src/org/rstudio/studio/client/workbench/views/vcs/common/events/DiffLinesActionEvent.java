/*
 * DiffLinesActionEvent.java
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
import org.rstudio.studio.client.workbench.views.vcs.common.events.DiffChunkActionEvent.Action;

public class DiffLinesActionEvent extends GwtEvent<DiffLinesActionHandler>
{
   public DiffLinesActionEvent(Action action)
   {
      action_ = action;
   }

   public Action getAction()
   {
      return action_;
   }

   @Override
   public Type<DiffLinesActionHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(DiffLinesActionHandler handler)
   {
      handler.onDiffLinesAction(this);
   }

   private final Action action_;

   public static final Type<DiffLinesActionHandler> TYPE = new Type<DiffLinesActionHandler>();
}
