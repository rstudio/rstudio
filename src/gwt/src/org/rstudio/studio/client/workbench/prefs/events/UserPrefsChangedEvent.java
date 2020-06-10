/*
 * UserPrefsChangedEvent.java
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

@JavaScriptSerializable
public class UserPrefsChangedEvent extends CrossWindowEvent<UserPrefsChangedHandler>
{
   public static final Type<UserPrefsChangedHandler> TYPE = new Type<UserPrefsChangedHandler>();
   
   public UserPrefsChangedEvent()
   {
   }

   public UserPrefsChangedEvent(PrefLayer data)
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
   public Type<UserPrefsChangedHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(UserPrefsChangedHandler handler)
   {
      handler.onUserPrefsChanged(this);
   }

   private PrefLayer data_;
}
