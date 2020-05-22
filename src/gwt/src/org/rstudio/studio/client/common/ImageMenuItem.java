/*
 * ImageMenuItem.java
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

import org.rstudio.core.client.command.AppCommand;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.MenuItem;

public class ImageMenuItem 
{
   public static MenuItem create(ImageResource res, 
                                 String text, 
                                 ScheduledCommand command, 
                                 Integer iconOffsetY)
   {
      
      return new MenuItem(AppCommand.formatMenuLabel(res, text, null, 
                                                     iconOffsetY), 
                          true, 
                          command);
   }
}
