/*
 * SourceControlPreferencesPane.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.workbench.prefs.views;

// TODO: project specific key paths

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.SafeHtmlUtil;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.FileChooserTextBox;
import org.rstudio.core.client.widget.FocusHelper;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.core.client.widget.HyperlinkLabel;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.SmallButton;
import org.rstudio.core.client.widget.TextBoxWithButton;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.vcs.CreateKeyDialog;
import org.rstudio.studio.client.common.vcs.VCSServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;
import org.rstudio.studio.client.workbench.prefs.model.SourceControlPrefs;

public class SourceControlPreferencesPane extends PreferencesPane
{
   @Inject
   public SourceControlPreferencesPane(PreferencesDialogResources res,
                                       final GlobalDisplay globalDisplay,
                                       VCSServerOperations server,
                                       RemoteFileSystemContext fsContext,
                                       FileDialogs fileDialogs)
   {
      res_ = res;
      server_ = server;

      chkVcsEnabled_ = new CheckBox(
            "Enable version control interface for RStudio projects");
      add(chkVcsEnabled_);
      
      
      // git bin dir chooser  
      gitBinDirChooser_ = new DirectoryChooserTextBox(null,
                                                      "(Not Found)",
                                                      null,
                                                      fileDialogs, 
                                                      fsContext);  
      addTextBoxChooser(new Label("Git bin directory:"), 
                        null, 
                        null, 
                        gitBinDirChooser_);
        
      /*
      TextBoxWithButton svnChooser = new DirectoryChooserTextBox(
                                                               null,
                                                               "(Not Found)",
                                                               null,
                                                               fileDialogs, 
                                                               fsContext);
      svnChooser.setText("");
      addTextBoxChooser(new Label("Svn bin directory:"), null, null, svnChooser);
      */
      
      // ssh key path
      sshKeyPathChooser_ = new FileChooserTextBox(
           null,
           "(Not Found)",
           null,
           new Command() {

            @Override
            public void execute()
            {
               publicKeyLink_.setVisible(true); 
            }    
      });
      
      publicKeyLink_ = new HyperlinkLabel("View public key");
      publicKeyLink_.addStyleName(res_.styles().viewPublicKeyLink());
      publicKeyLink_.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            viewPublicKey();
         }    
      });    
      Label sshKeyPathLabel = new Label("SSH key path:");
      addTextBoxChooser(sshKeyPathLabel, 
                        publicKeyLink_, 
                        res_.styles().newSection(),
                        sshKeyPathChooser_);
      tight(sshKeyPathChooser_);
      
      // ssh key path action buttons
      HorizontalPanel sshButtonPanel = new HorizontalPanel();
      sshButtonPanel.addStyleName(res_.styles().sshButtonPanel());
      createKeyButton_ = new SmallButton();
      createKeyButton_.setText("Create New Key...");
      createKeyButton_.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            new CreateKeyDialog(server_,
                                new OperationWithInput<String>() {
               @Override
               public void execute(String keyPath)
               {
                  sshKeyPathChooser_.setText(keyPath);             
               }
            }) /*.showModal() */;    
         }
      });
      // only add the create-key button in server mode
      sshButtonPanel.add(createKeyButton_);
      add(sshButtonPanel);
      
      // manipualte visiblity of ssh ui depending on mode/platform
      if (Desktop.isDesktop()) 
      {
         // never allow key creation in desktop mode (because the user
         // has other ways to accomplish this that are better supported
         // and documented)
         sshButtonPanel.setVisible(false);
         
         // wipe out all references to ssh keys on mac & linux desktop
         // (because we defer entirely to the system in these configurations)
         if (BrowseCap.isMacintosh() || BrowseCap.isLinux())
         {
            sshKeyPathLabel.setVisible(false);
            publicKeyLink_.setVisible(false);
            sshKeyPathChooser_.setVisible(false);
         }
      }
      
      
      HorizontalPanel helpPanel = new HorizontalPanel();
      helpPanel.addStyleName(res_.styles().nudgeRight());
      helpPanel.addStyleName(res_.styles().usingVcsHelp());
      Image helpImage = new Image(ThemeResources.INSTANCE.help());
      helpImage.addStyleName(res_.styles().helpImage());
      helpPanel.add(helpImage);
      HyperlinkLabel helpLink = new HyperlinkLabel(
                                       "Using Version Control with RStudio");
      helpLink.addClickHandler(new ClickHandler() {
         public void onClick(ClickEvent event)
         {
            globalDisplay.openRStudioLink("using_version_control");
         }  
      });
      helpPanel.add(helpLink);
      add(helpPanel);
                                      
      chkVcsEnabled_.setEnabled(false);
      gitBinDirChooser_.setEnabled(false);
      sshKeyPathChooser_.setEnabled(false);
      createKeyButton_.setEnabled(false);
   }

   @Override
   protected void initializeRPrefs(RPrefs rPrefs)
   {
      // source control prefs
      SourceControlPrefs prefs = rPrefs.getSourceControlPrefs();
      
      chkVcsEnabled_.setEnabled(true);
      gitBinDirChooser_.setEnabled(true);
      sshKeyPathChooser_.setEnabled(true);
      createKeyButton_.setEnabled(true);
      
      chkVcsEnabled_.setValue(prefs.getVcsEnabled());
      gitBinDirChooser_.setText(prefs.getGitBinDir());
      sshKeyPathChooser_.setText(prefs.getSSHKeyPath());
      
      publicKeyLink_.setVisible(prefs.getSSHKeyPath().length() > 0);
      
   }

   @Override
   public ImageResource getIcon()
   {
      return res_.iconSourceControl();
   }

   @Override
   public boolean validate()
   {
      return true;
   }

   @Override
   public String getName()
   {
      return "Version Control";
   }

   @Override
   public void onApply(RPrefs rPrefs)
   {
      super.onApply(rPrefs);
      
      SourceControlPrefs prefs = SourceControlPrefs.create(
                                          chkVcsEnabled_.getValue(),
                                          gitBinDirChooser_.getText(),
                                          sshKeyPathChooser_.getText());
      
      rPrefs.setSourceControlPrefs(prefs);
   }
   
   private void viewPublicKey()
   {
      final ProgressIndicator indicator = getProgressIndicator();
      indicator.onProgress("Reading public key...");
      
      // compute path to public key
      FileSystemItem privKey = 
               FileSystemItem.createFile(sshKeyPathChooser_.getText());
      FileSystemItem keyDir = privKey.getParentPath();
      final String keyPath = keyDir.completePath(privKey.getStem() + ".pub");
      
      server_.vcsSshPublicKey(keyPath,
                              new ServerRequestCallback<String> () {
         
         @Override
         public void onResponseReceived(String publicKeyContents)
         {
            indicator.onCompleted();
            
            // transform contents into displayable form
            SafeHtmlBuilder htmlBuilder = new SafeHtmlBuilder();
            SafeHtmlUtil.appendDiv(htmlBuilder,
                                   res_.styles().viewPublicKeyContent(),
                                   publicKeyContents);
            
            new ShowPublicKeyDialog(publicKeyContents).showModal();
         }

         @Override
         public void onError(ServerError error)
         {
            String msg = "Error attempting to read key '" + keyPath + "' (" +
                         error.getUserMessage() + ")";
            indicator.onError(msg);
         } 
      }); 
   }
   
   private void addTextBoxChooser(Label captionLabel, 
                                  HyperlinkLabel link,
                                  String captionPanelStyle,
                                  TextBoxWithButton chooser)
   {
      String textWidth = "250px";
      
      HorizontalPanel captionPanel = new HorizontalPanel();
      captionPanel.setWidth(textWidth);
      captionPanel.addStyleName(res_.styles().nudgeRight());
      if (captionPanelStyle != null)
         captionPanel.addStyleName(captionPanelStyle);
      
      captionPanel.add(captionLabel);
      captionPanel.setCellHorizontalAlignment(
                                          captionLabel,
                                          HasHorizontalAlignment.ALIGN_LEFT);
      
      if (link != null)
      {
         HorizontalPanel linkPanel = new HorizontalPanel();
         linkPanel.add(link);
         captionPanel.add(linkPanel);
         captionPanel.setCellHorizontalAlignment(
                                           linkPanel, 
                                           HasHorizontalAlignment.ALIGN_RIGHT);
       
      }
      
      add(tight(captionPanel));
      
      chooser.setTextWidth(textWidth);
      chooser.addStyleName(res_.styles().nudgeRight());
      chooser.addStyleName(res_.styles().textBoxWithChooser());
      add(chooser);    
   }
   
   private class ShowPublicKeyDialog extends ModalDialogBase
   {
      public ShowPublicKeyDialog(String publicKey)
      {
         publicKey_ = publicKey;
         
         setText("Public Key");
         
         setButtonAlignment(HasHorizontalAlignment.ALIGN_CENTER);
         
         ThemedButton closeButton = new ThemedButton("Close",
                                                     new ClickHandler() {
            public void onClick(ClickEvent event) {
               closeDialog();
            }
         });
         addOkButton(closeButton); 
      }
      
      @Override
      protected Widget createMainWidget()
      {
         VerticalPanel panel = new VerticalPanel();
         
         int mod = BrowseCap.hasMetaKey() ? KeyboardShortcut.META : 
                                            KeyboardShortcut.CTRL;
         String cmdText = new KeyboardShortcut(mod, 'C').toString(true);
         HTML label = new HTML("Press " + cmdText + 
                               " to copy the key to the clipboard");
         label.addStyleName(res_.styles().viewPublicKeyLabel());
         panel.add(label);
         
         textArea_ = new TextArea();
         textArea_.setText(publicKey_);
         textArea_.addStyleName(res_.styles().viewPublicKeyContent());
         textArea_.setSize("400px", "250px");
         textArea_.getElement().setAttribute("spellcheck", "false");
         FontSizer.applyNormalFontSize(textArea_.getElement());
         
         panel.add(textArea_);
         
         return panel;
      }
      
      @Override
      protected void onLoad()
      {
         super.onLoad();
        
         textArea_.selectAll();
         FocusHelper.setFocusDeferred(textArea_);
        
         
      }
      
      
      private final String publicKey_;
      private TextArea textArea_;
   }

   private final PreferencesDialogResources res_;
   
   private final VCSServerOperations server_;
 
   
   private final CheckBox chkVcsEnabled_;
   
   private TextBoxWithButton gitBinDirChooser_;
   
   private HyperlinkLabel publicKeyLink_;
   private TextBoxWithButton sshKeyPathChooser_;
   
   private SmallButton createKeyButton_;
}
