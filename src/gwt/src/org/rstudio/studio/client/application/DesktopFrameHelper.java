/*
 * DesktopFrameHelper.java
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
package org.rstudio.studio.client.application;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.StringUtil;

import com.google.gwt.user.client.Command;

public class DesktopFrameHelper
{
   /**
    * Tell the desktop frame what the coming session exit is for. `command`
    * runs once the frame has recorded it, and not at all when it could not:
    * a session exit the frame does not know about is taken for a crash.
    */
   public static void setPendingQuit(int quitType, Command command)
   {
      setPendingQuit(quitType, null, (String error) ->
      {
         if (StringUtil.isNullOrEmpty(error) && command != null)
            command.execute();
      });
   }

   /**
    * As above, naming the R (prepared by the frame's setPendingRVersion)
    * the next session should run with, or null; `onDone` receives "" once
    * recorded or the frame's error when it was not.
    */
   public static void setPendingQuit(int quitType,
                                     String rExecutablePath,
                                     CommandWithArg<String> onDone)
   {
      if (Desktop.isDesktop() && Desktop.hasDesktopFrame())
         Desktop.getFrame().setPendingQuit(quitType, StringUtil.notNull(rExecutablePath), onDone);
      else
         onDone.execute("");
   }
}
