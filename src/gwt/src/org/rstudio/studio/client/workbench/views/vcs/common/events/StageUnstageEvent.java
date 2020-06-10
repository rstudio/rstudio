/*
 * StageUnstageEvent.java
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
import org.rstudio.studio.client.common.vcs.StatusAndPath;

import java.util.ArrayList;

public class StageUnstageEvent extends GwtEvent<StageUnstageHandler>
{
   public StageUnstageEvent(boolean unstage, ArrayList<StatusAndPath> paths)
   {
      unstage_ = unstage;
      this.paths = paths;
   }

   public boolean isUnstage()
   {
      return unstage_;
   }

   public ArrayList<StatusAndPath> getPaths()
   {
      return paths;
   }

   @Override
   public Type<StageUnstageHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(StageUnstageHandler handler)
   {
      handler.onStageUnstage(this);
   }

   private final boolean unstage_;
   private final ArrayList<StatusAndPath> paths;

   public static final Type<StageUnstageHandler> TYPE = new Type<StageUnstageHandler>();
}
