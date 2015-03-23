/*
 * PublishStatic.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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

import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.rsconnect.model.RSConnectDeploymentRecord;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerOperations;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class PublishStatic extends Composite
{

   private static PublishStaticUiBinder uiBinder = GWT
         .create(PublishStaticUiBinder.class);

   interface PublishStaticUiBinder extends UiBinder<Widget, PublishStatic>
   {
   }

   public PublishStatic(RSConnectDeploymentRecord fromPrevious)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      initWidget(uiBinder.createAndBindUi(this));
      
      // set panel visibility appropriate to whether this is a re-deploy or new
      // content
      if (fromPrevious == null) 
      {
         existingContentPanel_.setVisible(false);
      }
      else
      {
         newContentPanel_.setVisible(true);
         contentNameLabel_.setText(fromPrevious.getName());
         
         // TODO: preselect account correctly too
      }
   }
   
   @Inject
   public void initialize(RSConnectServerOperations server, 
         GlobalDisplay display)
   {
      accountList_ = new RSConnectAccountList(server, display, false);
   }
   
   public void onActivate()
   {
      accountList_.refreshAccountList();
   }
   
   @UiField TextBox contentNameTextbox_;
   @UiField Label contentNameLabel_;
   @UiField(provided=true) RSConnectAccountList accountList_;
   @UiField VerticalPanel newContentPanel_;
   @UiField VerticalPanel existingContentPanel_;
}
