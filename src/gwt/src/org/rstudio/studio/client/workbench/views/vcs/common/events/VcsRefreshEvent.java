/*
 * VcsRefreshEvent.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

public class VcsRefreshEvent extends GwtEvent<VcsRefreshEvent.Handler>
{
   public enum Reason { NA, FileChange, VcsOperation }

   private final Reason reason_;
   private final int delayMs_;

   public static final Type<Handler> TYPE = new Type<>();

   public interface Handler extends EventHandler
   {
      void onVcsRefresh(VcsRefreshEvent event);
   }

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
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onVcsRefresh(this);
   }
}
