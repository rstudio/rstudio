/*
 * VcsRefreshEvent.java
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

public class VcsRefreshEvent extends GwtEvent<VcsRefreshHandler>
{
   public enum Reason { NA, FileChange, VcsOperation }

   private final Reason reason_;
   private final int delayMs_;

   public static final Type<VcsRefreshHandler> TYPE = new Type<VcsRefreshHandler>();

   public VcsRefreshEvent(Reason reason)
   {
      this(reason, 0);
   }

   public VcsRefreshEvent(Reason reason, int delayMs)
   {
      reason_ = reason;
      delayMs_ = delayMs;
   }

   public Reason getReason()
   {
      return reason_;
   }

   public int getDelayMs()
   {
      return delayMs_;
   }

   @Override
   public Type<VcsRefreshHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(VcsRefreshHandler handler)
   {
      handler.onVcsRefresh(this);
   }
}
