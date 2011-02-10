/*
 * FontSizeManager.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.ui;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.CommandHandler;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.core.client.widget.FontSizer.Size;
import org.rstudio.studio.client.application.events.ChangeFontSizeEvent;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.helper.StringStateValue;

@Singleton
public class FontSizeManager
{
   @Inject
   public FontSizeManager(Session session,
                          EventBus events,
                          Commands commands)
   {
      session_ = session;
      events_ = events;
      commands_ = commands;
      commands.fontSize10().addHandler(createHandler(FontSizer.Size.Pt10));
      commands.fontSize12().addHandler(createHandler(FontSizer.Size.Pt12));
      commands.fontSize14().addHandler(createHandler(FontSizer.Size.Pt14));
      commands.fontSize16().addHandler(createHandler(FontSizer.Size.Pt16));
      commands.fontSize18().addHandler(createHandler(FontSizer.Size.Pt18));

      new StringStateValue("font",
                           "size",
                           true,
                           session.getSessionInfo().getClientState())
      {
         @Override
         protected void onInit(String value)
         {
            final Size DEFAULT_SIZE = Size.Pt12;
            try
            {
               if (value != null)
                  size_ = FontSizer.Size.valueOf(value);
               else
                  size_ = DEFAULT_SIZE;
            }
            catch (Exception e)
            {
               size_ = DEFAULT_SIZE;
            }
         }

         @Override
         protected String getValue()
         {
            return size_.toString();
         }
      };
   }

   private CommandHandler createHandler(final Size size)
   {
      return new CommandHandler()
      {
         public void onCommand(AppCommand command)
         {
            size_ = size;
            events_.fireEvent(new ChangeFontSizeEvent(size));
            session_.persistClientState();
         }
      };
   }

   public Size getSize()
   {
      return size_;
   }

   private final Session session_;
   private final EventBus events_;
   private final Commands commands_;
   private Size size_;
}
