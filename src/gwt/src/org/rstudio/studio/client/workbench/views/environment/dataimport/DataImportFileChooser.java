/*
 * DataImportFileChooser.java
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

package org.rstudio.studio.client.workbench.views.environment.dataimport;

import com.google.gwt.aria.client.Roles;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.CanSetControlId;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.WorkbenchContext;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class DataImportFileChooser extends Composite
                                   implements CanSetControlId
{
   public DataImportFileChooser(Operation updateOperation, boolean growTextbox)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);

      initWidget(uiBinder.createAndBindUi(this));

      updateOperation_ = updateOperation;

      if (growTextbox)
      {
         locationTextBox_.getElement().getStyle().setHeight(22, Unit.PX);
         locationTextBox_.getElement().getStyle().setMarginTop(0, Unit.PX);
      }

      locationTextBox_.addValueChangeHandler(stringValueChangeEvent ->
      {
      });

      actionButton_.addClickHandler(event ->
      {
         if (updateMode_)
         {
            updateOperation_.execute();
         }
         else
         {
            FileSystemItem fileSystemItemPath = FileSystemItem.createFile(getText());
            if (getText() == "") {
               fileSystemItemPath = workbenchContext_.getDefaultFileDialogDir();
            }

            RStudioGinjector.INSTANCE.getFileDialogs().openFile(
                  "Choose File",
                  RStudioGinjector.INSTANCE.getRemoteFileSystemContext(),
                  fileSystemItemPath,
                  new ProgressOperationWithInput<FileSystemItem>()
                  {
                     public void execute(FileSystemItem input,
                                         ProgressIndicator indicator)
                     {
                        if (input == null)
                           return;

                        locationTextBox_.setText(input.getPath());
                        preventModeChange();

                        indicator.onCompleted();

                        updateOperation_.execute();
                     }
                  });
         }
      });

      checkForTextBoxChange();
   }

   @Inject
   private void initialize(WorkbenchContext workbenchContext)
   {
      workbenchContext_ = workbenchContext;
   }

   public void setEnabled(boolean enabled)
   {
      locationTextBox_.setEnabled(enabled);
      actionButton_.setEnabled(enabled);
   }

   public String getText()
   {
      return locationTextBox_.getText();
   }

   @Override
   public void onDetach()
   {
      checkTextBoxInterval_ = 0;
   }

   public void setFocus()
   {
      locationTextBox_.setFocus(true);
   }

   @UiField
   TextBox locationTextBox_;

   @UiField
   ThemedButton actionButton_;

   private void checkForTextBoxChange()
   {
      if (checkTextBoxInterval_ == 0)
         return;

      // Check continuously for changes in the textbox to reliably detect changes even when OS pastes text
      new Timer()
      {
         @Override
         public void run()
         {
            if (lastTextBoxValue_ != null && locationTextBox_.getText() != lastTextBoxValue_)
            {
               switchToUpdateMode(!locationTextBox_.getText().isEmpty());
            }

            lastTextBoxValue_ = locationTextBox_.getText();
            checkForTextBoxChange();
         }
      }.schedule(checkTextBoxInterval_);
   }

   private void preventModeChange()
   {
      lastTextBoxValue_ = locationTextBox_.getText();
   }

   public void switchToUpdateMode(Boolean updateMode)
   {
      if (updateMode_ != updateMode)
      {
         updateMode_ = updateMode;
         if (updateMode)
         {
            actionButton_.setText(updateModeCaption_);
         }
         else
         {
            actionButton_.setText(browseModeCaption_ + "...");
         }
         updateButtonAriaLabel();
      }
   }

   /**
    * @param suffix aria-label for the button to provide additional context to
    *               screen reader users; applied as a suffix to the visible
    *               button text, e.g. "Browse..." becomes "Browse for File/URL..."
    */
   public void setAriaLabelSuffix(String suffix)
   {
      ariaLabelSuffix_ = suffix;
      updateButtonAriaLabel();
   }

   public void updateButtonAriaLabel()
   {
      if (StringUtil.isNullOrEmpty(ariaLabelSuffix_))
      {
         Roles.getButtonRole().setAriaLabelProperty(actionButton_.getElement(), "");
         return;
      }

      final String prefix = updateMode_ ? updateModeCaption_ : browseModeCaption_ + " for";
      final String finalSuffix = updateMode_ ? "" : "...";
      Roles.getButtonRole().setAriaLabelProperty(actionButton_.getElement(),
         prefix + " " + ariaLabelSuffix_ + finalSuffix);
   }

   @Override
   public void setElementId(String id)
   {
      locationTextBox_.getElement().setId(id);
   }

   private static final String browseModeCaption_ = "Browse";
   private static final String updateModeCaption_ = "Update";
   private boolean updateMode_ = false;
   private String lastTextBoxValue_;
   private int checkTextBoxInterval_ = 250;
   private final Operation updateOperation_;
   private String ariaLabelSuffix_;

   private static DataImportFileChooserUiBinder uiBinder = GWT.create(DataImportFileChooserUiBinder.class);
   interface DataImportFileChooserUiBinder extends UiBinder<Widget, DataImportFileChooser> {}

   private WorkbenchContext workbenchContext_;
}
