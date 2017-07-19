package org.rstudio.core.client.widget;

import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.theme.res.ThemeStyles;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.ui.MenuItem;

// A menu item that can be checked or unchecked--appears similarly to a
// checkable AppCommand but isn't backed by an AppCommand.
public abstract class CheckableMenuItem extends MenuItem
{
   public CheckableMenuItem()
   {
      this("", false);
   }

   public CheckableMenuItem(String label)
   {
      this (label, false);
   }

   public CheckableMenuItem(String label, boolean html)
   {
      super(label, html, (Scheduler.ScheduledCommand)null);
      if (!html)
         setHTML(getHTMLContent());
      setScheduledCommand(new ScheduledCommand()
      {
         @Override
         public void execute()
         {
            onInvoked();
         }
      });
   }

   public void onStateChanged()
   {
      setHTML(getHTMLContent());
   }

   public abstract String getLabel();
   public abstract boolean isChecked();
   public abstract void onInvoked();
   
   public String getHTMLContent()
   {
      return AppCommand.formatMenuLabelWithStyle(
            isChecked() ? 
                  new ImageResource2x(ThemeResources.INSTANCE.menuCheck2x()) :
                  null,
            getLabel(), "", ThemeStyles.INSTANCE.menuCheckable());
      
   }
}
