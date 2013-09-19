package org.rstudio.core.client.command;

import java.util.ArrayList;
import java.util.List;

public class ShortcutInfo
{
   public ShortcutInfo (KeyboardShortcut shortcut, AppCommand command)
   {
      shortcuts_ = new ArrayList<String>(); 
      description_ = shortcut.getTitle().length() > 0 ?
                        shortcut.getTitle() :
                        command.getMenuLabel(false);
      groupName_ = shortcut.getGroupName();
      isActive_ = command.isEnabled() && command.isVisible();
      order_ = shortcut.getOrder();
      addShortcut(shortcut);
   }

   public String getDescription()
   {
      return description_;
   }

   public List<String> getShortcuts()
   {
      return shortcuts_;
   }
   
   public void addShortcut(KeyboardShortcut shortcut)
   {
      shortcuts_.clear();
      shortcuts_.add(shortcut.toString(true));
   }
   
   public String getGroupName()
   {
      return groupName_;
   }
   
   public boolean isActive()
   {
      return isActive_;
   }
   
   public int getOrder()
   {
      return order_;
   }
   
   private List<String> shortcuts_;
   private String description_;
   private String groupName_;
   private boolean isActive_;
   private int order_;
}