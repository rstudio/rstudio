/*
 * ConsoleSuspendBlockedIcon.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.console;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.resources.ImageResource2x;
import com.google.gwt.resources.client.ImageResource;
import org.rstudio.core.client.theme.res.ThemeResources;

public class ConsoleSuspendBlockedIcon
   extends Composite

{
   public ConsoleSuspendBlockedIcon(boolean announce)
   {
      ImageResource sus = new ImageResource2x(ThemeResources.INSTANCE.suspended());
      ImageResource blocked = new ImageResource2x(ThemeResources.INSTANCE.suspendBlocked());

      suspended_ = new Image(sus);
      suspended_.getElement().getStyle().setWidth(15, Style.Unit.PX);
      suspended_.getElement().getStyle().setHeight(15, Style.Unit.PX);
      suspended_.getElement().setId(ElementIds.CONSOLE_SESSION_SUSPENDED);
      suspendBlocked_ = new Image(blocked);
      suspendBlocked_.getElement().setId(ElementIds.CONSOLE_SESSION_SUSPEND_BLOCKED);

      if (announce)
      {
         Roles.getAlertRole().set(suspended_.getElement());
         Roles.getAlertRole().set(suspendBlocked_.getElement());
      }
   }

   public Image getSuspendBlocked()
   {
      return suspendBlocked_;
   }

   public Image getSuspended()
   {
      return suspended_;
   }

   private final Image suspended_;
   private final Image suspendBlocked_;
}
