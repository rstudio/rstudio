/*
 * NewRdDialog.java
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


package org.rstudio.studio.client.workbench.views.source.editors.text.ui;


import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class NewRdDialog extends ModalDialog<NewRdDialog.Result>
{
   public static class Result
   {  
      public static final String TYPE_NONE = "none";
      
      public Result(String name, String type)
      {
         this.name = name;
         this.type = type;
      }
      public final String name;
      public final String type;
   }
   
   public interface Binder extends UiBinder<Widget, NewRdDialog>
   {
   }
   
   public NewRdDialog(OperationWithInput<Result> operation)
   {
      super("New R Documentation File", operation);
      mainWidget_ = GWT.<Binder>create(Binder.class).createAndBindUi(this);
      
   }
   
   @Override
   protected Widget createMainWidget()
   {
      return mainWidget_;
   }
   
   @Override
   protected void onDialogShown()
   {
      txtName_.setFocus(true);
   }
   
   
   @Override
   protected Result collectInput()
   {
      return new Result(txtName_.getText().trim(),
                        listDocType_.getValue(listDocType_.getSelectedIndex()));
   }


   @Override
   protected boolean validate(Result input)
   {
      if (input.name.length() == 0)
      {
         RStudioGinjector.INSTANCE.getGlobalDisplay().showErrorMessage(
               "Name Not Specified",
               "You must specify a topic name for the new Rd file.",
               txtName_);
         
         return false;
      }
      else
      {
         return true;
      }
   }

   
   @UiField
   TextBox txtName_;
   @UiField
   ListBox listDocType_;
   
   private Widget mainWidget_; 
}
