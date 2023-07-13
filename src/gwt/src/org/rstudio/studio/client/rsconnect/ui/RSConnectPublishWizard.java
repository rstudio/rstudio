/*
 * RSConnectPublishWizard.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */package org.rstudio.studio.client.rsconnect.ui;

import java.util.ArrayList;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.Wizard;
import org.rstudio.core.client.widget.WizardPage;
import org.rstudio.studio.client.rsconnect.RsconnectConstants;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishInput;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishResult;
import org.rstudio.studio.client.rsconnect.ui.RSConnectDeploy.ServerType;

public class RSConnectPublishWizard 
   extends Wizard<RSConnectPublishInput, RSConnectPublishResult>
{
   public RSConnectPublishWizard(RSConnectPublishInput input, 
         ProgressOperationWithInput<RSConnectPublishResult> operation)
   {
      super(constants_.publish(), constants_.publish(), Roles.getDialogRole(), input, createFirstPage(input), operation);
   }
   
   private static WizardPage<RSConnectPublishInput, RSConnectPublishResult>
      createFirstPage(RSConnectPublishInput input)
   {
      if (!input.hasDocOutput() && input.isMultiRmd() && !input.isWebsiteRmd())
      {
         // multiple docs -- ask user if we should send them all up
         // can be published to Connect or Posit Cloud if user has accounts configured
         return new PublishMultiplePage(constants_.publish(), constants_.publish(), null, input, null);
      }
      else if (!input.isCloudUIEnabled() &&
               (input.isWebsiteRmd() ||
               (!input.isMultiRmd() && !input.isExternalUIEnabled())))
      {
         // a single doc, but it can't go to RPubs because
         // the doc is a website or RPubs is disabled,
         // and it can't go to Cloud because Cloud is disabled
         // so it has to go to Connect -- don't prompt the user for a destination
         return new PublishReportSourcePage(constants_.publish(), constants_.publish(),
               constants_.publishToRstudioConnect(),null, input,
               false, true, ServerType.RSCONNECT);
      }
      else
      {
         // non-Shiny doc--see which service user wants to publish to
         return new PublishDocServicePage(constants_.publish(), constants_.publish(), null, input);
      }
      // note that single Shiny docs don't require a wizard (the user can choose
      // a destination directly in the dialog)
   }
   
   @Override
   protected ArrayList<String> getWizardBodyStyles()
   {
      ArrayList<String> styles = super.getWizardBodyStyles();
      styles.add(RSConnectDeploy.RESOURCES.style().wizardDeployPage());
      return styles;
   }
   private static final RsconnectConstants constants_ = GWT.create(RsconnectConstants.class);
}
