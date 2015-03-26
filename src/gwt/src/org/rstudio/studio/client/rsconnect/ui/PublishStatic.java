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

import java.util.ArrayList;

import org.rstudio.core.client.JsArrayUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.rsconnect.model.RSConnectDeploymentFiles;
import org.rstudio.studio.client.rsconnect.model.RSConnectDeploymentRecord;
import org.rstudio.studio.client.rsconnect.model.RSConnectPublishResult;
import org.rstudio.studio.client.rsconnect.model.RSConnectServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
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

   public PublishStatic(final RSConnectDeploymentRecord fromPrevious)
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
         newContentPanel_.setVisible(false);
         contentNameLabel_.setText(fromPrevious.getName());
         accountList_.setOnRefreshCompleted(new Operation()
         {
            @Override
            public void execute()
            {
               accountList_.selectAccount(fromPrevious.getAccount());
            }
         });
      }
   }
   
   @Inject
   public void initialize(RSConnectServerOperations server, 
         GlobalDisplay display)
   {
      server_ = server;
      accountList_ = new RSConnectAccountList(server, display, false);
   }
   
   public void onActivate()
   {
      if (!populated_) 
      {
         accountList_.refreshAccountList();
         server_.getDeploymentFiles(contentPath_, asMultiple_, 
               new ServerRequestCallback<RSConnectDeploymentFiles>()
               {
                  @Override
                  public void onResponseReceived(RSConnectDeploymentFiles files)
                  {
                     setDeploymentFiles(files);
                  }
                  
                  @Override
                  public void onError(ServerError error)
                  {
                     // TODO: if this fails we can't really publish the content,
                     // so we need some way to take down the wizard
                  }
               });
         populated_ = true;
      }
   }
   
   public void focus()
   {
      contentNameTextbox_.setFocus(true);
   }
   
   public void setContentPath(String contentPath, boolean asMultiple)
   {
      contentPath_ = contentPath;
      asMultiple_ = asMultiple;
   }
   
   public RSConnectPublishResult getResult() 
   {
      return new RSConnectPublishResult(
            contentNameTextbox_.getText(), 
            accountList_.getSelectedAccount(),
            FileSystemItem.createFile(contentPath_).getParentPathString(), 
            contentPath_,
            deployFiles_, 
            null, null);
   }
   
   public boolean isResultValid()
   {
      return populated_ && contentNameTextbox_.validateAppName();
   }

   // Private methods ---------------------------------------------------------

   private void setDeploymentFiles(RSConnectDeploymentFiles files)
   {
      deployFiles_ = JsArrayUtil.fromJsArrayString(files.getDirList());
   }
   
   @UiField AppNameTextbox contentNameTextbox_;
   @UiField Label contentNameLabel_;
   @UiField(provided=true) RSConnectAccountList accountList_;
   @UiField VerticalPanel newContentPanel_;
   @UiField VerticalPanel existingContentPanel_;
   
   private String contentPath_;
   private boolean asMultiple_;

   private ArrayList<String> deployFiles_ = new ArrayList<String>();
   private boolean populated_;
   private RSConnectServerOperations server_;
}
