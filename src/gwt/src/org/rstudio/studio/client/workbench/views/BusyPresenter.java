/*
 * BusyPresenter.java
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
package org.rstudio.studio.client.workbench.views;

import org.rstudio.core.client.widget.model.ProvidesBusy;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.events.BusyEvent;

import com.google.gwt.event.shared.HandlerManager;

public abstract class BusyPresenter extends BasePresenter
   implements ProvidesBusy
{
   protected BusyPresenter(WorkbenchView view)
   {
      super(view);
   }

   public boolean isBusy()
   {
      return isBusy_;
   }

   public void setIsBusy(boolean isBusy)
   {
      if (isBusy_ != isBusy)
      {
         handlerManager_.fireEvent(new BusyEvent(isBusy));
         isBusy_ = isBusy;
      }
   }

   @Override
   public void addBusyHandler(BusyEvent.Handler handler)
   {
      // if a handler is added when we're already busy, invoke the
      // handler immediately
      if (isBusy())
         handler.onBusy(new BusyEvent(true));
      handlerManager_.addHandler(BusyEvent.TYPE, handler);
   }

   private boolean isBusy_ = false;
   private final HandlerManager handlerManager_ = new HandlerManager(this);
}
