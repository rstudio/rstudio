/*
 * Timers.java
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
package org.rstudio.studio.client.common;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;

public class Timers
{
   public static final void singleShot(int delayMs, final Command command)
   {
      new Timer()
      {
         @Override
         public void run()
         {
            command.execute();
         }
      }.schedule(delayMs);
   }
   
   public static final void singleShot(final Command command)
   {
      singleShot(0, command);
   }
}
