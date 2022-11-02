/*
 * ZoteroApiKeyWidget.java
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
package org.rstudio.studio.client.workbench.prefs.views.zotero;

import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.MessageDisplay;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.NullProgressIndicator;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.SmallButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.panmirror.server.PanmirrorZoteroServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.prefs.PrefsConstants;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
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
      HorizontalPanel apiKeyPanel = new HorizontalPanel();
      apiKeyPanel.addStyleName(RES.styles().apiKeyPanel());
      txtApiKey_ = new TextBox();
      txtApiKey_.getElement().setAttribute("spellcheck", "false");
      txtApiKey_.addStyleName(RES.styles().apiKey());
      txtApiKey_.setWidth(textWidth);
      apiKeyPanel.add(txtApiKey_);
      
      // verify key button
      SmallButton verifyKeyButton = new SmallButton();

      verifyKeyButton.setText(constants_.verifyKey());
      verifyKeyButton.addClickHandler(event -> verifyKey());
      apiKeyPanel.add(verifyKeyButton);

      panel.add(new FormLabel(constants_.zoteroWebApiKey(), txtApiKey_));
      panel.add(apiKeyPanel);
      
      

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
   
   public void focus()
   {
      txtApiKey_.setFocus(true);
   }
  
   private void verifyKey()
   {
      String key = getKey();
      if (key.length() > 0)
      {
         progressIndicator_.onProgress(constants_.verifyingKey());
         server_.zoteroValidateWebAPIKey(key, new ServerRequestCallback<Boolean>() {
            
            @Override
            public void onResponseReceived(Boolean valid)
            {
               progressIndicator_.onCompleted();
               GlobalDisplay display = RStudioGinjector.INSTANCE.getGlobalDisplay();
               if (valid)
               {
                  display.showMessage(MessageDisplay.MSG_INFO, constants_.zotero(),
                                      constants_.zoteroKeyVerified());
               }
               else
               {
                  display.showMessage(MessageDisplay.MSG_WARNING, constants_.zotero(),
                                      constants_.zoteroVerifyKeyFailedMessage());
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

   static final ZoteroResources RES = ZoteroResources.INSTANCE;
  
   private final TextBox txtApiKey_;

   private PanmirrorZoteroServerOperations server_;
   private ProgressIndicator progressIndicator_;
  
   private static final PrefsConstants constants_ = GWT.create(PrefsConstants.class);
}
