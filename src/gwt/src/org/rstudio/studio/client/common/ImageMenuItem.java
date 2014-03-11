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
