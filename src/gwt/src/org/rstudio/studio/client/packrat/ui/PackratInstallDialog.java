/*
 * PackratInstallDialog.java
 *
 * Copyright (C) 2014 by RStudio, Inc.
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
package org.rstudio.studio.client.packrat.ui;

import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.ThemedButton;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Widget;

public class PackratInstallDialog extends ModalDialogBase
{
   public PackratInstallDialog(String userAction, final Command onInstall)
   {
      install_ = new PackratInstall();

      setWidth("400px");
      setText("Packrat: " + userAction);
      
      ThemedButton installButton = new ThemedButton("Install Packrat",
            new ClickHandler()
            {
               @Override
               public void onClick(ClickEvent arg0)
               {
                  hide();
                  onInstall.execute();
               }
            });
      addOkButton(installButton);
      addCancelButton();
   }

   @Override
   protected Widget createMainWidget()
   {
      return install_;
   }

   private PackratInstall install_;
}
