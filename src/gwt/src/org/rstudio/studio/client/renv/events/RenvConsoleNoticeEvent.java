/*
 * RenvConsoleNoticeEvent.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
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
package org.rstudio.studio.client.renv.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.user.client.Command;

/** Shows a project setup notice in the Console, or hides it if message is null. */
public class RenvConsoleNoticeEvent extends GwtEvent<RenvConsoleNoticeEvent.Handler>
{
   public RenvConsoleNoticeEvent(boolean severe, String message,
                                 String actionLabel, Command action)
   {
      this(severe, message, actionLabel, action, null, null);
   }

   public RenvConsoleNoticeEvent(boolean severe, String message,
                                 String actionLabel, Command action,
                                 String secondaryActionLabel, Command secondaryAction)
   {
      severe_ = severe;
      message_ = message;
      actionLabel_ = actionLabel;
      action_ = action;
      secondaryActionLabel_ = secondaryActionLabel;
      secondaryAction_ = secondaryAction;
   }

   public interface Handler extends EventHandler
   {
      void onRenvConsoleNotice(RenvConsoleNoticeEvent event);
   }

   public boolean isSevere()
   {
      return severe_;
   }

   public String getMessage()
   {
      return message_;
   }

   public String getActionLabel()
   {
      return actionLabel_;
   }

   public Command getAction()
   {
      return action_;
   }

   public String getSecondaryActionLabel()
   {
      return secondaryActionLabel_;
   }

   public Command getSecondaryAction()
   {
      return secondaryAction_;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onRenvConsoleNotice(this);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   private final boolean severe_;
   private final String message_;
   private final String actionLabel_;
   private final Command action_;
   private final String secondaryActionLabel_;
   private final Command secondaryAction_;

   public static final Type<Handler> TYPE = new Type<>();
}
