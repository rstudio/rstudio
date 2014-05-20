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

import com.google.gwt.user.client.ui.Widget;

public class PackratInstallDialog extends ModalDialogBase
{
   public PackratInstallDialog()
   {
      // This widget provides the main body of the dialog.
      install_ = new PackratInstall();

      // Use addOkButton here to create a button with an action that installs
      // Packrat 

      addCancelButton();
   }

   @Override
   protected Widget createMainWidget()
   {
      return install_;
   }

   private PackratInstall install_;
}
