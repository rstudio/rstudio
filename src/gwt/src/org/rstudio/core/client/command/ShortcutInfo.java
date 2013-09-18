package org.rstudio.core.client.command;

public class ShortcutInfo
{
   public ShortcutInfo (KeyboardShortcut shortcut, AppCommand command)
   {
      shortcut_ = shortcut.toString(true);
      description_ = command.getMenuLabel(false);
      groupName_ = shortcut.getGroupName();
   }

   public String getDescription()
   {
      return description_;
   }

   public String getShortcut()
   {
      return shortcut_;
   }
   
   public String getGroupName()
   {
      return groupName_;
   }
   
   private String shortcut_;
   private String description_;
   private String groupName_;
}