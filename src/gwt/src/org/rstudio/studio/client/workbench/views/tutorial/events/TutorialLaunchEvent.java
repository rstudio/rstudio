/*
 * TutorialLaunchEvent.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.tutorial.events;

import org.rstudio.studio.client.workbench.views.tutorial.TutorialPresenter.Tutorial;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class TutorialLaunchEvent extends GwtEvent<TutorialLaunchEvent.Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }

      public final native String getPackageName()  /*-{ return this["package"]; }-*/;
      public final native String getTutorialName() /*-{ return this["name"];    }-*/;
   }

   public TutorialLaunchEvent(Data data)
   {
      tutorial_ = new Tutorial(
            data.getTutorialName(),
            data.getPackageName());
   }

   public Tutorial getTutorial()
   {
      return tutorial_;
   }

   private final Tutorial tutorial_;

   // Boilerplate ----

   public interface Handler extends EventHandler
   {
      void onTutorialLaunch(TutorialLaunchEvent event);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onTutorialLaunch(this);
   }

   public static final Type<Handler> TYPE = new Type<>();
}
