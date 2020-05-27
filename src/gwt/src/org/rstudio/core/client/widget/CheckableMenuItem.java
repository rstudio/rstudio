/*
 * CheckableMenuItem.java
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
package org.rstudio.core.client.widget;

import com.google.gwt.aria.client.Roles;
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
      this("", false, false);
   }

   public CheckableMenuItem(String label)
   {
      this (label, false, false);
   }

   public CheckableMenuItem(String label, boolean html)
   {
      this(label, html, false);
   }

   public CheckableMenuItem(String label, boolean html, boolean checked)
   {
      super(label, 
            html,
            Roles.getMenuitemcheckboxRole(),
            checked, 
            (Scheduler.ScheduledCommand)null);
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
      setChecked(checked);
   }

   public void onStateChanged()
   {
      setHTML(getHTMLContent());
      setChecked(isChecked());
   }

   public abstract String getLabel();
   public String getShortcut() { return ""; }
   public abstract boolean isChecked();
   public abstract void onInvoked();
   
   public String getHTMLContent()
   {
      return AppCommand.formatMenuLabelWithStyle(
            isChecked() ? 
                  new ImageResource2x(ThemeResources.INSTANCE.menuCheck2x()) :
                  null,
            getLabel(), getShortcut(), ThemeStyles.INSTANCE.menuCheckable());
      
   }
}
