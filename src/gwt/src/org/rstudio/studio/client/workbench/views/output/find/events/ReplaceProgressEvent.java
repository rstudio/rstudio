/*
 * ReplaceProgressEvent.java
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.output.find.events;

import org.rstudio.studio.client.workbench.views.output.find.model.LocalReplaceProgress;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class ReplaceProgressEvent extends GwtEvent<ReplaceProgressEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onReplaceProgress(ReplaceProgressEvent event);
   }
   
   // constructor used when there's no progress
   public ReplaceProgressEvent()
   {
      progress_ = null;
   }
   
   public ReplaceProgressEvent(LocalReplaceProgress progress)
   {
      progress_ = progress;
   }
   
   public boolean hasProgress()
   {
      return progress_ != null;
   }
   
   public LocalReplaceProgress progress()
   {
      return progress_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onReplaceProgress(this);
   }
   
   private final LocalReplaceProgress progress_;

   public static final Type<Handler> TYPE = new Type<Handler>();
}
