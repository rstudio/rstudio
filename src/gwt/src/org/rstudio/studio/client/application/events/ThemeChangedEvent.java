/*
 * ThemeChangedEvent.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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
package org.rstudio.studio.client.application.events;

import org.rstudio.core.client.js.JavaScriptSerializable;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

@JavaScriptSerializable
public class ThemeChangedEvent extends CrossWindowEvent<ThemeChangedEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onThemeChanged(ThemeChangedEvent event);
   }

   public ThemeChangedEvent()
   {
   }
   
   public ThemeChangedEvent(String themeName)
   {
      themeName_ = themeName;
   }

   public String getName()
   {
      return themeName_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onThemeChanged(this);
   }

   private String themeName_;
   public static final Type<Handler> TYPE = new Type<Handler>();
}
