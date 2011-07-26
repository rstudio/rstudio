/*
 * DiffLineActionEvent.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.vcs.events;

import com.google.gwt.event.shared.GwtEvent;
import org.rstudio.studio.client.workbench.views.vcs.diff.Line;
import org.rstudio.studio.client.workbench.views.vcs.events.DiffChunkActionEvent.Action;

public class DiffLineActionEvent extends GwtEvent<DiffLineActionHandler>
{
   public DiffLineActionEvent(Action action, Line line)
   {
      action_ = action;
      line_ = line;
   }

   public Action getAction()
   {
      return action_;
   }

   public Line getLine()
   {
      return line_;
   }

   @Override
   public Type<DiffLineActionHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(DiffLineActionHandler handler)
   {
      handler.onDiffLineAction(this);
   }

   private final Action action_;
   private final Line line_;

   public static final Type<DiffLineActionHandler> TYPE = new Type<DiffLineActionHandler>();
}
