/*
 * UserStateChangedEvent.java
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
package org.rstudio.studio.client.workbench.prefs.events;

import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.application.events.CrossWindowEvent;
import org.rstudio.studio.client.workbench.prefs.model.PrefLayer;

import com.google.gwt.event.shared.EventHandler;

@JavaScriptSerializable
public class UserStateChangedEvent extends CrossWindowEvent<UserStateChangedEvent.Handler>
{
   public static final Type<UserStateChangedEvent.Handler> TYPE = new Type<UserStateChangedEvent.Handler>();
   
   public UserStateChangedEvent()
   {
   }

   public interface Handler extends EventHandler
   {
      void onUserStateChanged(UserStateChangedEvent e);
   }

   public UserStateChangedEvent(PrefLayer data)
   {
      data_ = data;
   }

   public String getName()
   {
      return data_.getName();
   }

   public JsObject getValues()
   {
      return data_.getValues();
   }

   @Override
   public Type<UserStateChangedEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onUserStateChanged(this);
   }

   private PrefLayer data_;
}
