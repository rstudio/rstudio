/*
 * Timers.java
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
package org.rstudio.studio.client.common;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;

public class Timers
{
   public static final Timer singleShot(int delayMs, final Command command)
   {
      Timer timer = new Timer()
      {
         @Override
         public void run()
         {
            command.execute();
         }
      };
      
      timer.schedule(delayMs);
      return timer;
   }
   
   public static final Timer singleShot(final Command command)
   {
      return singleShot(0, command);
   }
}
