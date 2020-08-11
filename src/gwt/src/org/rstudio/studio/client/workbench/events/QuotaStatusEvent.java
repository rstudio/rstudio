/*
 * QuotaStatusEvent.java
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
package org.rstudio.studio.client.workbench.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import org.rstudio.studio.client.workbench.model.QuotaStatus;

public class QuotaStatusEvent extends GwtEvent<QuotaStatusEvent.Handler>
{
   public static final GwtEvent.Type<Handler> TYPE = new GwtEvent.Type<>();

   public interface Handler extends EventHandler
   {
      void onQuotaStatus(QuotaStatusEvent event);
   }

   public QuotaStatusEvent(QuotaStatus quotaStatus)
   {
      quotaStatus_ = quotaStatus;
   }

   public QuotaStatus getQuotaStatus()
   {
      return quotaStatus_;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onQuotaStatus(this);
   }

   @Override
   public GwtEvent.Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   private final QuotaStatus quotaStatus_;
}
