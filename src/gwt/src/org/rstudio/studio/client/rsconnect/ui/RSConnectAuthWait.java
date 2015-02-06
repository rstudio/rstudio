/*
 * RSConnectAuthWait.java
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

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class RSConnectAuthWait extends Composite
{
   private static RSConnectAuthWaitUiBinder uiBinder = GWT
         .create(RSConnectAuthWaitUiBinder.class);

   interface RSConnectAuthWaitUiBinder extends
         UiBinder<Widget, RSConnectAuthWait>
   {
   }

   public RSConnectAuthWait()
   {
      initWidget(uiBinder.createAndBindUi(this));
      setWidth("300px");
   }
   
   public void setClaimLink(String serverName, String url)
   {
      claimLink_.setText("Confirm account on " + serverName);
      claimLink_.setHref(url);
   }
   
   public void showError(String header, String message)
   {
      errorHeader_.setText(header);
      errorMessage_.setText(message);

      // toggle panel visibility
      successPanel_.setVisible(false);
      waitingPanel_.setVisible(false);
      errorPanel_.setVisible(true);
   }
   
   public void showSuccess(String serverName, String accountName)
   {
    
      // toggle panel visibility
      successPanel_.setVisible(true);
      waitingPanel_.setVisible(false);
      errorPanel_.setVisible(false);
   }
   
   @UiField Anchor claimLink_;
   @UiField HTMLPanel waitingPanel_;
   @UiField HTMLPanel successPanel_;
   @UiField HTMLPanel errorPanel_;
   @UiField Label errorHeader_;
   @UiField Label errorMessage_;
}
