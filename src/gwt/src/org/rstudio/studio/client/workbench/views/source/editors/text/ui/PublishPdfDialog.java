/*
 * PublishPdfDialog.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.*;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.workbench.views.source.editors.text.PublishPdf;

import java.util.Date;

public class PublishPdfDialog extends ModalDialogBase
                              implements PublishPdf.Display
{
  
   public PublishPdfDialog()
   {
      setText("Publish to Google Docs");
      setButtonAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
      okButton_ = new ThemedButton("Publish");
      addOkButton(okButton_);
      addCancelButton();
   }
   
   public void showPublishUI(String defaultTitle)
   {
      showPublishUI(defaultTitle, null, new Date());
   }
    
   
   public void showPublishUI(String title, String id, Date lastUpdated)
   {
      // add ok button with appropriate title
      boolean updateExisting = id != null;
      if (updateExisting)
         showOkButton("Update");
      else
         showOkButton("Publish");
      
      // add title field and focus it
      VerticalPanel titlePanel = new VerticalPanel();
      titlePanel.setWidth("100%");
      titlePanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
      Label label = new Label("Document Title:");
      titlePanel.add(label);
      titleTextBox_ = new TextBox();
      titleTextBox_.setWidth("98%");
      titleTextBox_.setText(title);
      titlePanel.add(titleTextBox_);
      mainWidget_.add(titlePanel);
      titleTextBox_.setFocus(true);
      titleTextBox_.selectAll();
      
      // if this is an update then show the update controls
      if (updateExisting)
      { 
         // previously published
         Label publishedLabel = new Label("Last published: " +
                                          StringUtil.formatDate(lastUpdated));
         publishedLabel.setStylePrimaryName(RESOURCES.styles().publishedLabel());
         mainWidget_.add(publishedLabel);
         
         // radio buttons
         updateExistingRadioButton_ = new RadioButton(
                  "Update",
                  "Update the previously published document");
         mainWidget_.add(updateExistingRadioButton_);
         updateExistingRadioButton_.addClickHandler(publishTypeClickHandler_);
         updateExistingRadioButton_.setValue(true);
        
         publishNewRadioButton_ = new RadioButton(
                  "Update",
                  "Publish a new document");
         publishNewRadioButton_.addClickHandler(publishTypeClickHandler_);
         mainWidget_.add(publishNewRadioButton_);     
      }
   }
   
   public String getTitle()
   {
      return titleTextBox_.getText().trim();
   }
   
   public boolean getUpdateExisting()
   {
      return updateExistingRadioButton_ != null &&
             updateExistingRadioButton_.getValue();
   }
   
   public HasClickHandlers getOkButton()
   {
      return okButton_;
   }
  
   
   public void dismiss()
   {
      closeDialog();
   }
   
   
   @Override
   protected Widget createMainWidget()
   {
      mainWidget_ = new VerticalPanel();
      mainWidget_.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);
      mainWidget_.setStylePrimaryName(RESOURCES.styles().mainWidget()); 
      return mainWidget_;
   }
   
   private void showOkButton(String caption)
   {
      okButton_.setText(caption);
      okButton_.setVisible(true);
   }
   
   private VerticalPanel mainWidget_;
   private ThemedButton okButton_;
   private TextBox titleTextBox_;
   private RadioButton updateExistingRadioButton_;
   private RadioButton publishNewRadioButton_;
   private ClickHandler publishTypeClickHandler_ = new ClickHandler() {
      public void onClick(ClickEvent event)
      {
         if (getUpdateExisting())
            okButton_.setText("Update");
         else
            okButton_.setText("Publish");
;      }      
   };
   
   static interface Styles extends CssResource
   {
      String mainWidget();
      String publishedLabel();
   }
  
   static interface Resources extends ClientBundle
   {
      @Source("PublishPdfDialog.css")
      Styles styles();
   }
   
   static Resources RESOURCES = (Resources)GWT.create(Resources.class) ;
   public static void ensureStylesInjected()
   {
      RESOURCES.styles().ensureInjected();
   }
}
