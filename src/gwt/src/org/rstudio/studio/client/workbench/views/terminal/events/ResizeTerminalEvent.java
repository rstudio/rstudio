/*
 * ResizeTerminalEvent.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import org.rstudio.studio.client.workbench.views.terminal.events.ResizeTerminalEvent.Handler;

/**
 * Event sent when a terminal is resized.
 */
public class ResizeTerminalEvent extends GwtEvent<Handler>
{  
   public interface Handler extends EventHandler
   {
      /**
       * Event sent when terminal has resized
       * @param event event containing new text dimensions
       */
      void onResizeTerminal(ResizeTerminalEvent event);
   }
   
   public interface HasHandlers extends com.google.gwt.event.shared.HasHandlers
   {
      HandlerRegistration addResizeTerminalHandler(Handler handler);
   }
   
   /**
    * Create a new terminal resize event 
    * @param cols new number of columns
    * @param rows new number of rows
    */
   public ResizeTerminalEvent(int cols, int rows)
   {
      cols_ = cols;
      rows_ = rows;
   }
   
   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onResizeTerminal(this);
   } 


   /**
    * @return number of rows in resized terminal
    */
   public int getRows()
   {
      return rows_;
   }

   /**
    * @return number of columns in resized terminal
    */
   public int getCols()
   {
      return cols_;
   }
   
   private final int rows_;
   private final int cols_;
   
   public static final Type<Handler> TYPE = new Type<Handler>();
}