/*
 * AskSecretEvent.java
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
package org.rstudio.studio.client.common.rstudioapi.events;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class AskSecretEvent extends GwtEvent<AskSecretEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onAskSecret(AskSecretEvent e);
   }

   public static class Data extends JavaScriptObject
   {
      protected Data() {}

      public native final String getTitle() /*-{
         return this.title;
      }-*/;

      public native final String getPrompt() /*-{
         return this.prompt;
      }-*/;

      public native final String getWindow() /*-{
         return this.window;
      }-*/;

      public native final boolean getCanRemember() /*-{
         return this.can_remember;
      }-*/;

      public native final boolean getHasSecret() /*-{
         return this.has_secret;
      }-*/;
   }

   public AskSecretEvent(AskSecretEvent.Data data)
   {
      title_ = data.getTitle();
      prompt_ = data.getPrompt();
      window_ = data.getWindow();
      canRemember_ = data.getCanRemember();
      hasSecret_ = data.getHasSecret();
   }

   public String getTitle()
   {
      return title_;
   }

   public String getPrompt()
   {
      return prompt_;
   }

   public String getWindow()
   {
      return window_;
   }

   public boolean getCanRemember()
   {
      return canRemember_;
   }

   public Boolean getHasSecret()
   {
      return hasSecret_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onAskSecret(this);
   }

   private final String title_;
   private final String prompt_;
   private final String window_;
   private final boolean canRemember_;
   private final boolean hasSecret_;

   public static final Type<Handler> TYPE = new Type<>();
}
