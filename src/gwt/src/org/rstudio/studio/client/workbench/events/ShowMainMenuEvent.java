/*
 * ShowMainMenuEvent.java
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
import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;

/**
 * Activate main menu and give it keyboard focus.
 */
@JavaScriptSerializable
public class ShowMainMenuEvent extends CrossWindowEvent<ShowMainMenuEvent.Handler>
{
   public enum Menu
   {
      // keep in sync with mainMenu declared in Commands.cmd.xml
      File, Edit, Code, View, Plots, Session, Build, Debug, Profile, Tools, Help
   }

   public interface Handler extends EventHandler
   {
      void onShowMainMenu(ShowMainMenuEvent event);
   }

   public ShowMainMenuEvent()
   {
      this(Menu.File);
   }

   public ShowMainMenuEvent(Menu menu)
   {
      menu_ = menu;
   }

   public Menu getMenu()
   {
      return menu_;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onShowMainMenu(this);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   private final Menu menu_;

   public static final Type<Handler> TYPE = new Type<>();
}
