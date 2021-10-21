/*
 * ConsoleInterpreterVersion.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.views.console;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import org.rstudio.core.client.resources.ImageResource2x;
import com.google.gwt.resources.client.ImageResource;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.SessionSuspendBlockedEvent;
import org.rstudio.studio.client.common.filetypes.FileIconResources;

public class ConsoleSuspendBlockedIcon
   extends Composite

{
   public ConsoleSuspendBlockedIcon()
   {
      ImageResource sus = new ImageResource2x(FileIconResources.INSTANCE.iconPng2x());
      ImageResource blocked = new ImageResource2x(FileIconResources.INSTANCE.iconCoffee2x());

      suspended_ = new Image(sus);
      suspendBlocked_ = new Image(blocked);

//      events.addHandler(SessionSuspendBlockedEvent.TYPE, event ->
//      {
//         if (event.getBlocking().size() > 5)
//         {
//
//         }
//      });
   }

   public Image getSuspendBlocked()
   {
      return suspendBlocked_;
//      if (data.size() > 5)
//      {
//         suspendBlocked_.setVisible(true);
//
//         return suspendBlocked_;
//      }
//      else
//      {
//         suspendBlocked_.setVisible(false);
//         return suspendBlocked_;
//      }
   }

   public Image getSuspended()
   {
//      suspended_.setVisible(true);
      return suspended_;
   }

   private final Image suspended_;
   private final Image suspendBlocked_;
}
