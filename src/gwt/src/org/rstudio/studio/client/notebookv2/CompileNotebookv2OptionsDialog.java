/*
 * CompileNotebookv2OptionsDialog.java
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
package org.rstudio.studio.client.notebookv2;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;


public class CompileNotebookv2OptionsDialog extends ModalDialog<CompileNotebookv2Options>
{
   interface Binder extends UiBinder<Widget, CompileNotebookv2OptionsDialog>
   {}
   
   public interface CompileNotebookv2Style extends CssResource
   {
      String dialog();
      String format();
      String formatPanel();
   }
   
   public CompileNotebookv2OptionsDialog(
         String defaultFormat,
         final OperationWithInput<CompileNotebookv2Options> operation)
   {
      super("Compile Report from R Script", Roles.getDialogRole(), operation);
      widget_ = GWT.<Binder>create(Binder.class).createAndBindUi(this);
      style.ensureInjected();
      
      setFormat(defaultFormat);
      lblFormat_.setFor(listFormat_);
      setOkButtonCaption("Compile");
      
      // read the message when dialog is shown
      setARIADescribedBy(dialogLabel_);
   }

   @Override
   protected void focusInitialControl()
   {
      listFormat_.setFocus(true);
   }
   
   @Override
   protected CompileNotebookv2Options collectInput()
   {
      return CompileNotebookv2Options.create(getFormat());
   }


   @Override
   protected boolean validate(CompileNotebookv2Options input)
   {
      return true;
   }

   @Override
   protected Widget createMainWidget()
   {
      return widget_;
   }

   private String getFormat()
   {
      return listFormat_.getValue(listFormat_.getSelectedIndex());
   }
   
   private void setFormat(String format)
   {
      int formatIndex = 0;
      for (int i=0; i<listFormat_.getItemCount(); i++)
      {
         if (format == listFormat_.getValue(i))
         {
            formatIndex = i;
            break;
         }
      }
      listFormat_.setSelectedIndex(formatIndex);
   }
   
   @UiField
   Element dialogLabel_;
   @UiField 
   CompileNotebookv2Style style;
   @UiField
   DivElement divFormatSelector_;
   @UiField
   HorizontalPanel formatLabelPanel_;
   @UiField
   FormLabel lblFormat_;
   @UiField
   ListBox listFormat_;

   private Widget widget_;
   
   private CompileNotebookv2OptionsDialog()
   {
      super("Caption", Roles.getDialogRole(), new OperationWithInput<CompileNotebookv2Options>() {

         @Override
         public void execute(CompileNotebookv2Options input)
         {
         }  
      });
      
      GWT.<Binder>create(Binder.class).createAndBindUi(this);
      style.ensureInjected();
   }
   
   public static void ensureStylesInjected()
   {
      try
      {
         new CompileNotebookv2OptionsDialog();
      }
      catch(Exception e)
      { 
      }
   }

}
