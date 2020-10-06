/*
 * PanimrrorEditRawDialog.java
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


package org.rstudio.studio.client.panmirror.dialogs;


import com.google.gwt.aria.client.Roles;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorRawFormatProps;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorRawFormatResult;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;


public class PanmirrorEditRawDialog extends ModalDialog<PanmirrorRawFormatResult>
{
   public PanmirrorEditRawDialog(
               PanmirrorRawFormatProps raw,
               String[] outputFormats,
               boolean inline,
               OperationWithInput<PanmirrorRawFormatResult> operation)
   {
      super("Raw " + (inline ? "Inline" : "Block"), Roles.getDialogRole(), operation, () -> {
         // cancel returns null
         operation.execute(null);
      });
      
      RStudioGinjector.INSTANCE.injectMembers(this);
      mainWidget_ = GWT.<Binder>create(Binder.class).createAndBindUi(this);
      
      inline_ = inline;
   
      rawFormatSelect_.setFormats(outputFormats, raw.format);
      rawFormatSelect_.setValue(StringUtil.notNull(raw.format));
      
      rawContent_.setValue(raw.content);
      PanmirrorDialogsUtil.setFullWidthStyles(rawContent_);
      
      if (!inline_)
      {
         rawFormatSelect_.addStyleName(RES.styles().spaced());
         rawContentLabel_.setVisible(false);
         rawContent_.setVisible(false);
      }
      
      // make remove button available if we are editing an existing format
      if (!rawFormatSelect_.getValue().equals("")) 
      {
         ThemedButton removeFormatButton = new ThemedButton("Remove Format");
         addLeftButton(removeFormatButton, ElementIds.VISUAL_MD_RAW_FORMAT_REMOVE_BUTTON); 
         removeFormatButton.addClickHandler((event) -> {
            PanmirrorRawFormatResult input = collectInput();
            input.action = "remove";
            validateAndGo(input, new Command()
            {
               @Override
               public void execute()
               {
                  closeDialog();
                  if (operation != null)
                     operation.execute(input);
                  onSuccess();
               }
            });
         });
      }      
   }
   
   @Inject
   void initialize(GlobalDisplay globalDisplay)
   {
      globalDisplay_ = globalDisplay;
   }
   
   @Override
   protected Widget createMainWidget()
   {
      return mainWidget_;
   }
   
   @Override
   protected void focusInitialControl()
   {
      if (rawFormatSelect_.getValue().length() > 0)
      {
         rawContent_.setFocus(true);
      }
      else
      {
         rawFormatSelect_.getListBox().setFocus(true);
      }
   }
   
   @Override
   protected PanmirrorRawFormatResult collectInput()
   {
      PanmirrorRawFormatResult result = new PanmirrorRawFormatResult();
      result.raw = new PanmirrorRawFormatProps();
      result.raw.format = rawFormatSelect_.getValue();
      result.raw.content = rawContent();
      result.action = "edit";
      return result;
   }


   @Override
   protected boolean validate(PanmirrorRawFormatResult result)
   {
      if (inline_ && rawContent().length() == 0)
      {
         globalDisplay_.showErrorMessage(
               "No Content Specified", 
               "You must provide content to apply the raw format to.",
               rawContent_);
         
         return false;
      }
      else
      {
         return true;
      }
   }
   
   private String rawContent()
   {
      return rawContent_.getValue().trim();
   }
   
   private final boolean inline_;

   private GlobalDisplay globalDisplay_;
   
   private static PanmirrorDialogsResources RES = PanmirrorDialogsResources.INSTANCE;
   
   interface Binder extends UiBinder<Widget, PanmirrorEditRawDialog> {}
   
   private Widget mainWidget_; 
   @UiField PanmirrorRawFormatSelect rawFormatSelect_;
   @UiField TextBox rawContent_;
   @UiField Label rawContentLabel_;
   
   
}
