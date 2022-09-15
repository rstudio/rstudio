/*
 * RmdOutputFormatChangedEvent.java
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

package org.rstudio.studio.client.rmarkdown.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class RmdOutputFormatChangedEvent extends GwtEvent<RmdOutputFormatChangedEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onRmdOutputFormatChanged(RmdOutputFormatChangedEvent event);
   }
   
   public RmdOutputFormatChangedEvent(String format)
   {
      this(format, false, false);
   }

   public RmdOutputFormatChangedEvent(String format, boolean isQuarto, boolean isQuartoBook)
   {
      format_ = format;
      isQuarto_ = isQuarto;
      isQuartoBook_ = isQuartoBook;
   }

   public String getFormat()
   {
      return format_;
   }
   
   public boolean isQuarto()
   {
      return isQuarto_;
   }
   
   public boolean isQuartoBook()
   {
      return isQuartoBook_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onRmdOutputFormatChanged(this);
   }

   private final String format_;
   private final boolean isQuarto_;
   private final boolean isQuartoBook_;

   public static final Type<Handler> TYPE = new Type<>();
}
