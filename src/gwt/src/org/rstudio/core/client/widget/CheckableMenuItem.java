package org.rstudio.core.client.widget;

import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.ui.MenuItem;

// A menu item that can be checked or unchecked--appears similarly to a
// checkable AppCommand but isn't backed by an AppCommand.
public abstract class CheckableMenuItem extends MenuItem
{
   public CheckableMenuItem()
   {
      this("");
   }

   public CheckableMenuItem(String label)
   {
      super(label, false, (Scheduler.ScheduledCommand)null);
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
   
   private String getHTMLContent()
   {
      return AppCommand.formatMenuLabel(
            isChecked() ? 
                  new ImageResource2x(ThemeResources.INSTANCE.menuCheck2x()) :
                  null,
            getLabel(), "");
      
   }
}
