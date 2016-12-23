/*
 * TerminalCanonicalEvent.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.terminal.events;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Sent if terminal mode changes (canonical or non-canonical).
 * Canonical is line-by-line mode and this is used to indicate that a non-
 * interactive program is running, such as a batch job.
 */
public class TerminalCanonicalEvent extends GwtEvent<TerminalCanonicalEvent.Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }

      /**
       * @return Terminal handle
       */
      public final native String getHandle() /*-{
            return this.handle;
      }-*/;

      /**
       * @return true if terminal is in canonical (line-by-line) mode.
       */
      public final native boolean getCanonical() /*-{
            return this.canonical;
      }-*/;
   }

   public interface Handler extends EventHandler
   {
      void onTerminalCanonical(TerminalCanonicalEvent event);
   }

   public TerminalCanonicalEvent(Data data)
   {
      data_ = data;
   }

   public String getHandle()
   {
      return data_.getHandle();
   }

   public boolean isCanonical()
   {
      return data_.getCanonical();
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onTerminalCanonical(this);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   private final Data data_;

   public static final Type<Handler> TYPE = new Type<Handler>();
}
