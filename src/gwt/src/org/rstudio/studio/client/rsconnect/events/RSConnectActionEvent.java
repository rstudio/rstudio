/*
 * RSConnectActionEvent.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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
package org.rstudio.studio.client.rsconnect.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class RSConnectActionEvent extends GwtEvent<RSConnectActionEvent.Handler>
{ 
   public interface Handler extends EventHandler
   {
      void onRSConnectAction(RSConnectActionEvent event);
   }

   public static final GwtEvent.Type<RSConnectActionEvent.Handler> TYPE =
      new GwtEvent.Type<RSConnectActionEvent.Handler>();
   
   public RSConnectActionEvent(int action, String path)
   {
      action_ = action;
      path_ = path;
   }
   
   public String getPath()
   {
      return path_;
   }
   
   public int getAction()
   {
      return action_;
   }
   
   @Override
   protected void dispatch(RSConnectActionEvent.Handler handler)
   {
      handler.onRSConnectAction(this);
   }

   @Override
   public GwtEvent.Type<RSConnectActionEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }
   
   public static int ACTION_TYPE_DEPLOY = 0;
   public static int ACTION_TYPE_CONFIGURE = 1;
   
   private String path_;
   private int action_;
}