/*
 * PublishStaticDialog.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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
package org.rstudio.studio.client.rsconnect.ui;

import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.rsconnect.model.RSConnectDeploymentRecord;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerOperations;

public class PublishStaticDialog
             extends RSConnectDialog<PublishStatic>
{

   public PublishStaticDialog(RSConnectServerOperations server,
         GlobalDisplay display, RSConnectDeploymentRecord fromPrevious)
   {
      super(server, display, new PublishStatic(fromPrevious));

      setText("Publish to RStudio Connect");

      // add publish/cancel buttons
      publishButton_ = new ThemedButton("Publish");
      addCancelButton();
      addOkButton(publishButton_);

      // activate immediately (the PublishStatic widget may also be hosted in a
      // wizard page, when it doesn't become active until the wizard page is
      // reached)
      contents_.onActivate();
   }
   
   private final ThemedButton publishButton_;
}
