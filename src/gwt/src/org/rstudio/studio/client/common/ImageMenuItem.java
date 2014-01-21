package org.rstudio.studio.client.common;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.MenuItem;

public class ImageMenuItem 
{
   public static MenuItem create(ImageResource res, 
                                 String text, 
                                 ScheduledCommand command)
   {
      SafeHtmlBuilder shb = new SafeHtmlBuilder();
      shb.appendHtmlConstant("<img src=\"" +
                             res.getSafeUri().asString() +
                             "\" style=\"vertical-align: middle; " + 
                             "margin-right: 4px;\" />");
      shb.appendEscaped(text);
         
      return new MenuItem(shb.toSafeHtml(), command);
   }
}
