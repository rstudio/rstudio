package org.rstudio.studio.client.workbench;

import com.google.inject.Inject;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.CommandHandler;
import org.rstudio.core.client.command.MenuCallback;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.core.client.widget.FontSizer.Size;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.ChangeFontSizeEvent;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.commands.Commands;

public class FontSizeMenu
{
   public FontSizeMenu()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
   }

   @Inject
   void initialize(Commands commands, EventBus events)
   {
      commands_ = commands;
      events_ = events;
   }

   public void execute(MenuCallback callback)
   {
      register(callback, 10, FontSizer.Size.Pt10);
      register(callback, 12, FontSizer.Size.Pt12);
      register(callback, 14, FontSizer.Size.Pt14);
      register(callback, 16, FontSizer.Size.Pt16);
      register(callback, 18, FontSizer.Size.Pt18);
   }

   private void register(MenuCallback callback, int size, final Size realSize)
   {
      AppCommand command = new AppCommand();
      command.setMenuLabel(size + "pt");
      command.addHandler(new CommandHandler()
      {
         public void onCommand(AppCommand command)
         {
            events_.fireEvent(new ChangeFontSizeEvent(realSize));
         }
      });

      String id = "changeFontSize" + size;
      commands_.addCommand(id, command);
      callback.addCommand(id, command);
   }

   private Commands commands_;
   private EventBus events_;
}
