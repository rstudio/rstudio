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

public class ResizeTerminalEvent
{  
   public interface Handler
   {
      void onResizeTerminal(ResizeTerminalEvent event);
   }

   public ResizeTerminalEvent(int cols, int rows)
   {
      cols_ = cols;
      rows_ = rows;
   }
    
   protected void dispatch(Handler handler)
   {
      handler.onResizeTerminal(this);
   }
   
   public int getRows()
   {
      return rows_;
   }

   public int getCols()
   {
      return cols_;
   }
   
   private final int rows_;
   private final int cols_;
}