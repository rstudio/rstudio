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
import org.rstudio.studio.client.rsconnect.RSConnect;
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
      // Select the first page of the wizard based on the kind of content we're
      // dealing with (all other content type situations don't require a 
      // wizard to resolve)
      if (input.getContentType() == RSConnect.CONTENT_TYPE_PLOT ||
          input.getContentType() == RSConnect.CONTENT_TYPE_HTML ||
          input.getContentType() == RSConnect.CONTENT_TYPE_PRES)
      {
         // self-contained static content
         return new PublishStaticDestPage("Publish", "Publish", null, input, 
               false);
      }
      else if (input.getContentType() == RSConnect.CONTENT_TYPE_DOCUMENT &&
               input.getSourceRmd().getExtension().toLowerCase().equals(".md"))
      {
         // pure Markdown -- always publish as static
         return new PublishStaticDestPage("Publish", "Publish", null, input, 
               false);
      }
      else if (input.getContentType() == RSConnect.CONTENT_TYPE_DOCUMENT &&
               input.isMultiRmd())
      {
         return new PublishMultiplePage(input);
      }
      else if (input.getContentType() == RSConnect.CONTENT_TYPE_DOCUMENT &&
               !input.isMultiRmd() &&
               input.isConnectUIEnabled())
      {
         return new PublishReportSourcePage("Publish", "Publish", input, false);
      }
      else
      {
         // shouldn't happen but this is a safe default
         return new PublishFilesPage("Publish", "Publish", null, input, false,
               false);
      }
   }
}
