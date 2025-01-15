/*
 * ChangeFontSizeEvent.java
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
package org.rstudio.studio.client.application.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class ChangeFontSizeEvent extends GwtEvent<ChangeFontSizeEvent.Handler>
{
   public static final Type<Handler> TYPE = new Type<>();

   public ChangeFontSizeEvent(double fontSize,
                              double lineHeight)
   {
      fontSize_ = fontSize;
      lineHeight_ = lineHeight;
   }

   public double getFontSize()
   {
      return fontSize_;
   }
   
   public double getLineHeight()
   {
      return lineHeight_;
   }

   private final double fontSize_;
   private final double lineHeight_;

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onChangeFontSize(this);
   }

   public interface Handler extends EventHandler
   {
      void onChangeFontSize(ChangeFontSizeEvent event);
   }
}
