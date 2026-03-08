/*
 * AboutDialog.java
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
 */

package org.rstudio.studio.client.application.ui;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.dom.Clipboard;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.StudioClientApplicationConstants;
import org.rstudio.studio.client.application.model.ProductEditionInfo;
import org.rstudio.studio.client.application.model.ProductInfo;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.chat.PaiUtil;
import org.rstudio.studio.client.workbench.views.chat.server.ChatServerOperations;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class AboutDialog extends ModalDialogBase
{
   public AboutDialog(ProductInfo info)
   {
      super(Roles.getDialogRole());
      RStudioGinjector.INSTANCE.injectMembers(this);

      setText(constants_.title(editionInfo_.editionName()));

      ThemedButton copyVersionButton = new ThemedButton(constants_.copyVersionButtonTitle(), (ClickEvent) ->
      {
         String versionText = "RStudio " + info.version + " " +
            "\"" + info.release_name + "\" " + info.build_type +
            " (" + info.commit + ", " + info.date + ") " +
            constants_.forText() + info.os + "\n" +
            Window.Navigator.getUserAgent() + quartoDetails();
         
         // Append Posit Assistant version if available
         if (!positAssistantVersion_.isEmpty())
         {
            versionText += ", Posit AI " + positAssistantVersion_;
         }
         
         Clipboard.setText(versionText);
         RStudioGinjector.INSTANCE.getGlobalDisplay().showMessage(GlobalDisplay.MSG_INFO, constants_.versionCopiedText(),
                 constants_.versionInformationCopiedText());
      });
      addButton(copyVersionButton, constants_.copyVersionButton());

      ThemedButton OKButton = new ThemedButton(constants_.okBtn(), (ClickEvent) -> closeDialog());
      addOkButton(OKButton);

      if (editionInfo_.proLicense() && Desktop.hasDesktopFrame())
      {
         ThemedButton licenseButton = new ThemedButton(constants_.manageLicenseBtn(), (ClickEvent) ->
         {
            closeDialog();
            editionInfo_.showLicense();
         });
         addLeftButton(licenseButton, ElementIds.ABOUT_MANAGE_LICENSE_BUTTON);
      }
      contents_ = new AboutDialogContents(info, editionInfo_, quartoDetails());
      fetchPositAssistantVersion();
      setARIADescribedBy(contents_.getDescriptionElement());
      setWidth("600px"); //$NON-NLS-1$
   }

   @Override
   protected Widget createMainWidget()
   {
      return contents_;
   }

   @Override
   protected void focusInitialControl()
   {
      focusOkButton();
   }

   @Inject
   private void initialize(ProductEditionInfo editionInfo,
                           Session session,
                           UserPrefs userPrefs,
                           PaiUtil paiUtil,
                           ChatServerOperations chatServer)
   {
      editionInfo_ = editionInfo;
      session_ = session;
      userPrefs_ = userPrefs;
      paiUtil_ = paiUtil;
      chatServer_ = chatServer;
   }

   private String quartoDetails()
   {
      String quartoDetails = "";
      SessionInfo sessionInfo = session_.getSessionInfo();
      if (sessionInfo.getQuartoConfig().enabled && sessionInfo.getQuartoConfig().version.length() > 0)
      {
         quartoDetails = ", Quarto " + sessionInfo.getQuartoConfig().version;
         if (sessionInfo.getQuartoConfig().user_installed.length() > 0)
         {
            quartoDetails += " (" + sessionInfo.getQuartoConfig().user_installed + ")";
         }
      }
      return quartoDetails;
   }

   private void fetchPositAssistantVersion()
   {
      // Only fetch if user has selected Posit AI as their assistant or chat provider
      if (!paiUtil_.isPaiSelected() && !paiUtil_.isChatProviderPosit())
         return;

      chatServer_.chatGetVersion(new ServerRequestCallback<String>()
      {
         @Override
         public void onResponseReceived(String version)
         {
            // Only display if we got a non-empty version
            if (version != null && !version.isEmpty())
            {
               positAssistantVersion_ = version;
               contents_.appendToUserAgent(", Posit AI " + version);
            }
         }

         @Override
         public void onError(ServerError error)
         {
            // Silently ignore errors - don't display version if unavailable
         }
      });
   }

   private AboutDialogContents contents_;
   private ProductEditionInfo editionInfo_;
   private Session session_;
   private UserPrefs userPrefs_;
   private PaiUtil paiUtil_;
   private ChatServerOperations chatServer_;
   private String positAssistantVersion_ = "";
   private static final StudioClientApplicationConstants constants_ = GWT.create(StudioClientApplicationConstants.class);
}
