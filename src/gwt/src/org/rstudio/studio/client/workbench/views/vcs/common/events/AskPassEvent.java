/*
 * AskPassEvent.java
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
package org.rstudio.studio.client.workbench.views.vcs.common.events;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class AskPassEvent extends GwtEvent<AskPassEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onAskPass(AskPassEvent e);
   }

   public static class Data extends JavaScriptObject
   {
      protected Data() {}

      public native final String getPrompt() /*-{
         return this.prompt;
      }-*/;

      public native final String getRememberPrompt() /*-{
         return this.remember_prompt;
      }-*/;

      public native final String getWindow() /*-{
         return this.window;
      }-*/;
   }

   public AskPassEvent(AskPassEvent.Data data)
   {
      prompt_ = data.getPrompt();
      rememberPrompt_ = data.getRememberPrompt();
      window_ = data.getWindow();
   }

   public String getPrompt()
   {
      return prompt_;
   }

   public String getRememberPasswordPrompt()
   {
      return rememberPrompt_;
   }

   public String getWindow()
   {
      return window_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onAskPass(this);
   }

   private final String prompt_;
   private final String rememberPrompt_;
   private final String window_;

   public static final Type<Handler> TYPE = new Type<>();
}
