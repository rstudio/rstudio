/*
 * AppMenuItem.java
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
package org.rstudio.core.client.command;

import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.ui.MenuItem;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.core.client.regex.Pattern.ReplaceOperation;

public class AppMenuItem extends MenuItem
{
   public AppMenuItem(AppCommand cmd, boolean mainMenu)
   {
      super(cmd.getMenuHTML(mainMenu), true, cmd.getMenuRole(), cmd.isChecked(), cmd);
      cmd_ = cmd;
      mainMenu_ = mainMenu;
      setTitle(cmd_.getDesc());
   }

   public AppMenuItem(AppCommand cmd, boolean mainMenu, ScheduledCommand cmdWrapper)
   {
      super(cmd.getMenuHTML(mainMenu), true, cmd.getMenuRole(), cmd.isChecked(), cmdWrapper);
      cmd_ = cmd;
      mainMenu_ = mainMenu;
      setTitle(cmd.getDesc());
   }

   @Override
   public ScheduledCommand getScheduledCommand()
   {
      return getScheduledCommand(false);
   }

   public ScheduledCommand getScheduledCommand(boolean evenIfDisabled)
   {
      return evenIfDisabled || cmd_.isEnabled() ? super.getScheduledCommand() : null;
   }

   public void onShow()
   {
      if (cmd_.isEnabled())
         getElement().removeClassName("disabled");
      else
         getElement().addClassName("disabled");

      setVisible(cmd_.isVisible());
      setEnabled(cmd_.isEnabled());

      setHTML(cmd_.getMenuHTML(mainMenu_));
      setTitle(cmd_.getDesc());
      setChecked(cmd_.isChecked());
   }

   public boolean cmdVisible()
   {
      return cmd_.isVisible();
   }

   public static String escapeMnemonics(String label)
   {
      return label.replace("_", "__");
   }

   public static String replaceMnemonics(String label, final String replacement)
   {
      return Pattern.create("_(_?)").replaceAll(label, new ReplaceOperation()
      {
         public String replace(Match m)
         {
            if (m.getGroup(1).length() > 0)
               return "_";
            else
               return replacement;
         }
      });
   }

   public String getId()
   {
      return cmd_.getId();
   }

   private final AppCommand cmd_;
   private final boolean mainMenu_;
}
