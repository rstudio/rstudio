/*
 * HelpNavigateEvent.java
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
package org.rstudio.studio.client.workbench.views.help.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class HelpNavigateEvent extends GwtEvent<HelpNavigateEvent.Handler>
{
   public static final GwtEvent.Type<Handler> TYPE = new GwtEvent.Type<>();

   public HelpNavigateEvent(String url, String title)
   {
      url_ = url;
      title_ = title;
   }

   public String getUrl()
   {
      return url_;
   }

   public String getTitle()
   {
      return title_;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onNavigate(this);
   }

   @Override
   public GwtEvent.Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   public interface Handler extends EventHandler
   {
      void onNavigate(HelpNavigateEvent event);
   }

   private final String url_;
   private final String title_;
}
