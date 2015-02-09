/*
 * RPubsUploadDialog.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

package org.rstudio.studio.client.common.rpubs.ui;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.resources.CoreResources;
import org.rstudio.core.client.widget.FixedTextArea;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressImage;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.rpubs.RPubsHtmlGenerator;
import org.rstudio.studio.client.common.rpubs.events.RPubsUploadStatusEvent;
import org.rstudio.studio.client.common.rpubs.model.RPubsServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.views.vcs.common.ConsoleProgressDialog;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class RPubsUploadDialog extends ModalDialogBase
{
   public RPubsUploadDialog(String contextId,
                            String title, 
                            String htmlFile, 
                            boolean isPublished)
   {
      this(contextId, title, htmlFile, null, isPublished);
   }
   
   public RPubsUploadDialog(String contextId,
                            String title, 
                            RPubsHtmlGenerator htmlGenerator, 
                            boolean isPublished)
   {
      this(contextId, title, null, htmlGenerator, isPublished);
   }
   
   private RPubsUploadDialog(String contextId,
                             String title, 
                             String htmlFile, 
                             RPubsHtmlGenerator htmlGenerator,
                             boolean isPublished)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      setText("Publish to RPubs");
      title_ = title;
      htmlFile_ = htmlFile;
      htmlGenerator_ = htmlGenerator;
      isPublished_ = isPublished;
      contextId_ = contextId;
   }

   
   
   @Inject
   void initialize(GlobalDisplay globalDisplay,
                   EventBus eventBus,
                   RPubsServerOperations server)
   {
      globalDisplay_ = globalDisplay;
      eventBus_ = eventBus;
      server_ = server;
   }
   
   @Override
   protected Widget createMainWidget()
   {
      Styles styles = RESOURCES.styles();
      
      SimplePanel mainPanel = new SimplePanel();
      mainPanel.addStyleName(styles.mainWidget());
      
      VerticalPanel verticalPanel = new VerticalPanel();
  
      HorizontalPanel headerPanel = new HorizontalPanel();
      headerPanel.addStyleName(styles.headerPanel());
      headerPanel.add(new Image(RESOURCES.publishLarge()));
      
      Label headerLabel = new Label("Publish to RPubs");
      headerLabel.addStyleName(styles.headerLabel());
      headerPanel.add(headerLabel);
      headerPanel.setCellVerticalAlignment(headerLabel,
                                           HasVerticalAlignment.ALIGN_MIDDLE);
      
      verticalPanel.add(headerPanel);

      String msg;
      if (!isPublished_)
      {
         msg = "RPubs is a free service from RStudio for sharing " +
                       "documents on the web. Click Publish to get " +
                       "started.";
      }
      else
      {
         msg = "This document has already been published on RPubs. You can " +
               "choose to either update the existing RPubs document, or " +
               "create a new one.";
      }
      Label descLabel = new Label(msg);
      descLabel.addStyleName(styles.descLabel());
      verticalPanel.add(descLabel);

      // if we have a generator then show title and comment UI
      if (htmlGenerator_ != null)
      {
         Label titleLabel = new Label("Title (optional):");
         titleLabel.addStyleName(styles.fieldLabel());
         verticalPanel.add(titleLabel);
         titleTextBox_ = new TextBox();
         titleTextBox_.addStyleName(styles.titleTextBox());
         titleTextBox_.getElement().setAttribute("spellcheck", "false");
         verticalPanel.add(titleTextBox_);
         
         Label commentLabel = new Label("Comment (optional):");
         commentLabel.addStyleName(styles.fieldLabel());
         verticalPanel.add(commentLabel);
         commentTextArea_ = new FixedTextArea(6);
         commentTextArea_.addStyleName(styles.commentTextArea());
         verticalPanel.add(commentTextArea_);
         
         // not using either for now
         titleLabel.setVisible(false);
         titleTextBox_.setVisible(false);
         commentLabel.setVisible(false);
         commentTextArea_.setVisible(false);
         
         previewButton_ = new ThemedButton("Preview");
         previewButton_.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event)
            { 
               htmlGenerator_.generateRPubsHtml(
                  titleTextBox_.getText().trim(), 
                  commentTextArea_.getText().trim(),
                  new CommandWithArg<String>() {
                     @Override
                     public void execute(String rpubsFile)
                     {
                        globalDisplay_.showHtmlFile(rpubsFile);
                     }
                  });       
            }
         });
         addLeftButton(previewButton_);
      }
      
      HTML warningLabel =  new HTML(
        "<strong>IMPORTANT: All documents published to RPubs are " +
        "publicly visible.</strong> You should " +
        "only publish documents that you wish to share publicly.");
      warningLabel.addStyleName(styles.warningLabel());
      verticalPanel.add(warningLabel);
        
      ThemedButton cancelButton = createCancelButton(new Operation() {
         @Override
         public void execute()
         {
            // if an upload is in progress then terminate it
            if (uploadInProgress_)
            {
               server_.rpubsTerminateUpload(contextId_,
                                            new VoidServerRequestCallback());
               
               if (uploadProgressWindow_ != null)
                  uploadProgressWindow_.close();
            }
         }
         
      });

      continueButton_ = new ThemedButton("Publish", new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {   
            performUpload(false);
         }
      });

      updateButton_ = new ThemedButton("Update Existing", new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            performUpload(true);
         }
      });

      createButton_ = new ThemedButton("Create New", new ClickHandler()
      {
         @Override
         public void onClick(ClickEvent event)
         {
            performUpload(false);
         }
      });

      if (!isPublished_)
      {
         addOkButton(continueButton_);
         addCancelButton(cancelButton);
      }
      else
      {
         addOkButton(updateButton_);
         addButton(createButton_);
         addCancelButton(cancelButton);
      }
     
      mainPanel.setWidget(verticalPanel);
      return mainPanel;
   }
   
   @Override
   protected void onDialogShown()
   {
      super.onDialogShown();
      
      if (hasTitle())
         titleTextBox_.setFocus(true);
   }
   
   protected void onUnload()
   {
      eventRegistrations_.removeHandler();
      super.onUnload();
   }
   
   private String getTitleText()
   {
      if (hasTitle())
         return titleTextBox_.getText().trim();
      else
         return title_;
   }
   
   private String getCommentText()
   {
      if (hasComment())
         return commentTextArea_.getText().trim();
      else
         return "";
   }
   
   private boolean hasTitle()
   {
      return (titleTextBox_ != null) && titleTextBox_.isVisible();
   }
   
   private boolean hasComment()
   {
      return (commentTextArea_ != null) && commentTextArea_.isVisible();
   }
  
   private void performUpload(final boolean modify)
   {
      // set state
      uploadInProgress_ = true;
    
      // do upload
      if (Desktop.isDesktop())
      {
         performUpload(null, modify);
      }
      else
      {
         // randomize the name so firefox doesn't prevent us from reactivating
         // the window programatically 
         globalDisplay_.openProgressWindow(
               "_rpubs_upload" + (int)(Math.random() * 10000), 
               PROGRESS_MESSAGE, 
               new OperationWithInput<WindowEx>() {

                  @Override
                  public void execute(WindowEx window)
                  {
                     performUpload(window, modify);
                  }
               });
      }
      
   }
   
   
   private void performUpload(final WindowEx progressWindow,
                              final boolean modify)
   {  
      // record progress window
      uploadProgressWindow_ = progressWindow;
      
      // show progress
      showProgressPanel();
      
      // subscribe to notification of upload completion
      eventRegistrations_.add(
                        eventBus_.addHandler(RPubsUploadStatusEvent.TYPE, 
                        new RPubsUploadStatusEvent.Handler()
      {
         @Override
         public void onRPubsPublishStatus(RPubsUploadStatusEvent event)
         {
            // make sure it applies to our context
            RPubsUploadStatusEvent.Status status = event.getStatus();
            if (!status.getContextId().equals(contextId_))
               return;
            
            uploadInProgress_ = false;
            
            closeDialog();

            if (!StringUtil.isNullOrEmpty(status.getError()))
            {
               if (progressWindow != null)
                  progressWindow.close();
               
               new ConsoleProgressDialog("Upload Error Occurred", 
                     status.getError(),
                     1).showModal();
            }
            else
            {
               if (progressWindow != null)
               {
                  progressWindow.replaceLocationHref(status.getContinueUrl());
               }
               else
               {
                  globalDisplay_.openWindow(status.getContinueUrl());
               }
            }

         }
      }));
      
      // synthesize html generator if necessary
      RPubsHtmlGenerator htmlGenerator = htmlGenerator_;
      if (htmlGenerator == null)
      {
         htmlGenerator = new RPubsHtmlGenerator() {

            @Override
            public void generateRPubsHtml(String title, 
                                          String comment,
                                          CommandWithArg<String> onCompleted)
            {
               onCompleted.execute(htmlFile_);
            }
         };
      }
      
      // generate html and initiate the upload
      final String title = getTitleText();
      htmlGenerator.generateRPubsHtml(
          title, getCommentText(), new CommandWithArg<String>() {

         @Override
         public void execute(String htmlFile)
         {
            // initiate the upload
            server_.rpubsUpload(
               contextId_,
               title, 
               htmlFile,
               modify,
               new ServerRequestCallback<Boolean>() {

                  @Override
                  public void onResponseReceived(Boolean response)
                  {
                     if (!response.booleanValue())
                     {
                        closeDialog();
                        globalDisplay_.showErrorMessage(
                               "Error",
                               "Unable to continue " +
                               "(another publish is currently running)");
                     }
                  }
                  
                  @Override
                  public void onError(ServerError error)
                  {
                     closeDialog();
                     globalDisplay_.showErrorMessage("Error",
                                                     error.getUserMessage());
                  }
              });
            
         }
           
        });
      
      
   }
   
   private void showProgressPanel()
   {
      // disable continue button
      continueButton_.setVisible(false);
      updateButton_.setVisible(false);
      createButton_.setVisible(false);
      if (previewButton_ != null)
         previewButton_.setVisible(false);
      enableOkButton(false);
      
      // add progress
      HorizontalPanel progressPanel = new HorizontalPanel();
      ProgressImage progressImage =  new ProgressImage(
                                       CoreResources.INSTANCE.progress_gray());
      progressImage.addStyleName(RESOURCES.styles().progressImage());
      progressImage.show(true);
      progressPanel.add(progressImage);
      progressPanel.add(new Label(PROGRESS_MESSAGE));
      addLeftWidget(progressPanel);
   }
   
   static interface Styles extends CssResource
   {
      String mainWidget();
      String headerPanel();
      String headerLabel();
      String descLabel();
      String progressImage();
      String warningLabel();
      String titleTextBox();
      String commentTextArea();
      String fieldLabel();
   }
  
   static interface Resources extends ClientBundle
   {
      @Source("RPubsUploadDialog.css")
      Styles styles();
      
      ImageResource publishLarge();
   }

   private final boolean isPublished_;

   static Resources RESOURCES = (Resources)GWT.create(Resources.class) ;
   public static void ensureStylesInjected()
   {
      RESOURCES.styles().ensureInjected();
   }
   
   private TextBox titleTextBox_;
   private TextArea commentTextArea_;
  
   private ThemedButton continueButton_;
   private ThemedButton updateButton_;
   private ThemedButton createButton_;
   private ThemedButton previewButton_;

   private final String title_;
   private final String htmlFile_;
   private final String contextId_;
   private final RPubsHtmlGenerator htmlGenerator_;
   
   private boolean uploadInProgress_ = false;
   private WindowEx uploadProgressWindow_ = null;
   
   private GlobalDisplay globalDisplay_;
   private EventBus eventBus_;
   private RPubsServerOperations server_;
   
   private HandlerRegistrations eventRegistrations_ = new HandlerRegistrations();
   
   private static final String PROGRESS_MESSAGE = "Uploading document to RPubs...";
}
