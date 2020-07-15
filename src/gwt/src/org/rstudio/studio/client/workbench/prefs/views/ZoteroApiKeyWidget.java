/*
 * ZoteroApiKeyWidget.java
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
package org.rstudio.studio.client.workbench.prefs.views;

import org.rstudio.core.client.MessageDisplay;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.NullProgressIndicator;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.SmallButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.panmirror.server.PanmirrorZoteroServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextBox;

public class ZoteroApiKeyWidget extends Composite
{
   public ZoteroApiKeyWidget(PanmirrorZoteroServerOperations server, String textWidth)
   {
      server_ = server;
      progressIndicator_ = new NullProgressIndicator();

      FlowPanel panel = new FlowPanel();

      // text box
      txtApiKey_ = new TextBox();
      txtApiKey_.addStyleName(RES.styles().apiKey());
      txtApiKey_.setWidth(textWidth);

      // caption panel
      HorizontalPanel captionPanel = new HorizontalPanel();
      captionPanel.setWidth(textWidth);
      FormLabel apiKeyLabel = new FormLabel("Zotero API Key:", txtApiKey_);
      captionPanel.add(apiKeyLabel);
      captionPanel.setCellHorizontalAlignment(
            apiKeyLabel,
            HasHorizontalAlignment.ALIGN_LEFT);

      HorizontalPanel linkPanel = new HorizontalPanel();
      helpLink_ = new HelpLink("Using Zotero", 
                               "visual_markdown_editing-zotero",
                               false);
      helpLink_.addStyleName(RES.styles().helpLink());
   
      linkPanel.add(helpLink_);
      captionPanel.add(helpLink_);
      captionPanel.setCellHorizontalAlignment(
            helpLink_, 
            HasHorizontalAlignment.ALIGN_RIGHT);

      panel.add(captionPanel);
      panel.add(txtApiKey_);
      
      // verify key button
      HorizontalPanel verifyKeyPanel = new HorizontalPanel();
      verifyKeyPanel.addStyleName(RES.styles().verifyKeyPanel());
      SmallButton verifyKeyButton = new SmallButton();
      verifyKeyButton.setText("Verify Connection...");
      verifyKeyButton.addClickHandler(event -> verifyKey());
      verifyKeyPanel.add(verifyKeyButton);
      panel.add(verifyKeyPanel);

      initWidget(panel);
   }

   public void setProgressIndicator(ProgressIndicator progressIndicator)
   {
      progressIndicator_ = progressIndicator;
   }
   
   public void setKey(String key)
   {
      txtApiKey_.setText(key);
   }

   public String getKey()
   {
      return txtApiKey_.getText().trim();
   }
  
   private void verifyKey()
   {
      String key = getKey();
      if (key.length() > 0)
      {
         progressIndicator_.onProgress("Verifying Connection...");
         server_.zoteroValidateWebAPIKey(key, new ServerRequestCallback<Boolean>() {
            
            @Override
            public void onResponseReceived(Boolean valid)
            {
               progressIndicator_.onCompleted();
               GlobalDisplay display = RStudioGinjector.INSTANCE.getGlobalDisplay();
               if (valid)
               {
                  display.showMessage(MessageDisplay.MSG_INFO, "Zotero", 
                                      "Zotero API connection sucessfully verified.");
               }
               else
               {
                  display.showMessage(MessageDisplay.MSG_WARNING, "Zotero", 
                                      "Unable to verify Zotero API connection.\n\n" +
                                      "You should verify that your API key is still valid, " +
                                      "and if necessary create a new key.");
               }
            }
            
            @Override
            public void onError(ServerError error)
            {
               progressIndicator_.onError(error.getUserMessage());
            }
         });
      }
      
   }

   interface Styles extends CssResource
   {
      String apiKey();
      String helpLink();
      String verifyKeyPanel();
   }

   interface Resources extends ClientBundle
   {
      @Source("ZoteroApiKeyWidget.css")
      Styles styles();
   }

   static final Resources RES = GWT.create(Resources.class);
   public static void ensureStylesInjected()
   {
      RES.styles().ensureInjected();
   }

   private final HelpLink helpLink_;
   private final TextBox txtApiKey_;

   private PanmirrorZoteroServerOperations server_;
   private ProgressIndicator progressIndicator_;

}
