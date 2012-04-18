/*
 * SshKeyWidget.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.vcs;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.HyperlinkLabel;
import org.rstudio.core.client.widget.NullProgressIndicator;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.SmallButton;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;

public class SshKeyWidget extends Composite
{   
   public SshKeyWidget(GitServerOperations server, String textWidth)
                       
   {
      server_ = server;
      progressIndicator_ = new NullProgressIndicator();
      
      FlowPanel panel = new FlowPanel();
           
      // caption panel
      HorizontalPanel captionPanel = new HorizontalPanel();
      captionPanel.addStyleName(RES.styles().captionPanel());
      captionPanel.setWidth(textWidth);
      Label sshKeyPathLabel = new Label("SSH RSA Key:");
      captionPanel.add(sshKeyPathLabel);
      captionPanel.setCellHorizontalAlignment(
                                          sshKeyPathLabel,
                                          HasHorizontalAlignment.ALIGN_LEFT);
   
      HorizontalPanel linkPanel = new HorizontalPanel();
      publicKeyLink_ = new HyperlinkLabel("View public key");
      publicKeyLink_.addStyleName(RES.styles().viewPublicKeyLink());
      publicKeyLink_.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            viewPublicKey();
         }    
      });    
      linkPanel.add(publicKeyLink_);
      captionPanel.add(publicKeyLink_);
      captionPanel.setCellHorizontalAlignment(
                                        publicKeyLink_, 
                                        HasHorizontalAlignment.ALIGN_RIGHT);
      panel.add(captionPanel);
      
      
      // chooser
      txtSshKeyPath_ = new TextBox();
      txtSshKeyPath_.addStyleName(RES.styles().keyPath());
      txtSshKeyPath_.setReadOnly(true);
      txtSshKeyPath_.setWidth(textWidth);
      panel.add(txtSshKeyPath_);  
      
    
      // ssh key path action buttons
      HorizontalPanel sshButtonPanel = new HorizontalPanel();
      sshButtonPanel.addStyleName(RES.styles().sshButtonPanel());
      createKeyButton_ = new SmallButton();
      createKeyButton_.setText("Create RSA Key...");
      createKeyButton_.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            showCreateKeyDialog(); 
         }
      });
      sshButtonPanel.add(createKeyButton_);
      panel.add(sshButtonPanel);
           
      initWidget(panel);
   }
   
   public void setProgressIndicator(ProgressIndicator progressIndicator)
   {
      progressIndicator_ = progressIndicator;
   }
      
   public void setRsaSshKeyPath(String rsaSshKeyPath,
                                boolean haveRsaSshKey)
   {
      rsaSshKeyPath_ = rsaSshKeyPath;
      if (haveRsaSshKey)
         setSshKey(rsaSshKeyPath_);
      else
         setSshKey(NONE);
   }
   
   private void setSshKey(String keyPath)
   {
      txtSshKeyPath_.setText(keyPath);
      if (keyPath.equals(NONE))
      {
         publicKeyLink_.setVisible(false);
         txtSshKeyPath_.addStyleName(RES.styles().keyPathNone());
      }
      else
      {
         publicKeyLink_.setVisible(true);
         txtSshKeyPath_.removeStyleName(RES.styles().keyPathNone());
      }
   }
   
   private void showCreateKeyDialog()
   {
      new CreateKeyDialog(
         rsaSshKeyPath_,
         server_,
         new OperationWithInput<String>() 
         {
            @Override
            public void execute(String keyPath)
            {
               if (keyPath != null)
                  setSshKey(keyPath);
               else
                  setSshKey(NONE);
            }
         }).showModal();     
   }

   
   private void viewPublicKey()
   {
      progressIndicator_.onProgress("Reading public key...");
      
      // compute path to public key
      FileSystemItem privKey = 
               FileSystemItem.createFile(txtSshKeyPath_.getText());
      FileSystemItem keyDir = privKey.getParentPath();
      final String keyPath = keyDir.completePath(privKey.getStem() + ".pub");
      
      server_.gitSshPublicKey(keyPath,
                              new ServerRequestCallback<String> () {
         
         @Override
         public void onResponseReceived(String publicKeyContents)
         {
            progressIndicator_.onCompleted();
            
            new ShowPublicKeyDialog("Public Key", 
                                    publicKeyContents).showModal();
         }

         @Override
         public void onError(ServerError error)
         {
            String msg = "Error attempting to read key '" + keyPath + "' (" +
                         error.getUserMessage() + ")";
            progressIndicator_.onError(msg);
         } 
      }); 
   }
   
   
   static interface Styles extends CssResource
   {
      String viewPublicKeyLink();
      String captionPanel();
      String sshButtonPanel();
      String keyPath();
      String keyPathNone();
   }
  
   static interface Resources extends ClientBundle
   {
      @Source("SshKeyWidget.css")
      Styles styles();
   }
   
   static Resources RES = (Resources)GWT.create(Resources.class) ;
   public static void ensureStylesInjected()
   {
      RES.styles().ensureInjected();
   }
   
   private HyperlinkLabel publicKeyLink_;
   private TextBox txtSshKeyPath_;
   private SmallButton createKeyButton_;
   
   private final GitServerOperations server_;
   private ProgressIndicator progressIndicator_;
   private String rsaSshKeyPath_;
   
   private static final String NONE = "(None)";
}
