/*
 * RSConnectPublishWizard.java
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
 */package org.rstudio.studio.client.rsconnect.ui;

import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.Wizard;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishInput;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishResult;

public class RSConnectPublishWizard 
   extends Wizard<RSConnectPublishInput, RSConnectPublishResult>
{
   public RSConnectPublishWizard(RSConnectPublishInput input, 
         ProgressOperationWithInput<RSConnectPublishResult> operation)
   {
      super("Publish", "Publish", input, createFirstPage(input), operation);
   }
   
   private static WizardPage<RSConnectPublishInput, RSConnectPublishResult>
      createFirstPage(RSConnectPublishInput input)
   {
      if (input.isShiny() && input.isMultiRmd())
      {
         // multiple Shiny docs -- see if we should send them all up
         return new PublishMultiplePage("Publish", "Publish", null, input);
      }
      else 
      {
         // non-Shiny doc--see which service user wants to publish to
         return new PublishDocServicePage("Publish", "Publish", null, input);
      }
      // note that single Shiny docs don't require a wizard (the user can choose
      // a destination directly in the dialog)
   }
}
