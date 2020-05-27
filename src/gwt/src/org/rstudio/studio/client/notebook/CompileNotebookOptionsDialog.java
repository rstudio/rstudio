/*
 * CompileNotebookOptionsDialog.java
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
package org.rstudio.studio.client.notebook;

import java.util.Date;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.a11y.A11y;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.HelpButton;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.filetypes.FileTypeCommands;

public class CompileNotebookOptionsDialog extends ModalDialog<CompileNotebookOptions>
{
   interface Binder extends UiBinder<Widget, CompileNotebookOptionsDialog>
   {}

   public CompileNotebookOptionsDialog(
         String docId,
         String defaultTitle,
         String defaultAuthor,
         String defaultType,
         final OperationWithInput<CompileNotebookOptions> operation)
   {
      super("Compile Report from R Script", Roles.getDialogRole(), operation);
      docId_ = docId;
      RStudioGinjector.INSTANCE.injectMembers(this);

      widget_ = GWT.<Binder>create(Binder.class).createAndBindUi(this);
      txtTitle_.setText(defaultTitle);
      A11y.associateLabelWithField(titleLabel_, txtTitle_.getElement());
      txtAuthor_.setText(defaultAuthor);
      A11y.associateLabelWithField(authorLabel_, txtAuthor_.getElement());
      
      if (showTypes_)
      {
         setType(defaultType);
        
         typeLabelPanel_.setCellVerticalAlignment(
                                       lblType_, 
                                       HasVerticalAlignment.ALIGN_MIDDLE);
         
         HelpButton helpButton = HelpButton.createHelpButton("notebook_types", "Help on report types");
         typeLabelPanel_.add(helpButton);
         typeLabelPanel_.setCellVerticalAlignment(
                                       helpButton, 
                                       HasVerticalAlignment.ALIGN_MIDDLE);

         
         divTypeSelector_.getStyle().setPaddingBottom(10, Unit.PX);
         lblType_.setFor(listType_); 
      }
      else
      {
         setType(CompileNotebookOptions.TYPE_DEFAULT);
         divTypeSelector_.getStyle().setDisplay(Style.Display.NONE);
      }
      
      setOkButtonCaption("Compile");

      // read the message when dialog is shown
      setARIADescribedBy(dialogInfo_);
   }
   
   @Inject
   void initialize(FileTypeCommands fileTypeCommands)
   {
      showTypes_ = fileTypeCommands.getHTMLCapabiliites().isStitchSupported();
   }

   @Override
   protected void focusInitialControl()
   {
      txtTitle_.setFocus(true);
      txtTitle_.selectAll();
   }
   
   @Override
   protected CompileNotebookOptions collectInput()
   {
      return CompileNotebookOptions.create(docId_,
                                           createPrefix(),
                                           createSuffix(),
                                           true,
                                           txtTitle_.getValue().trim(),
                                           txtAuthor_.getValue().trim(),
                                           getType());
   }

   private String createPrefix()
   {
      StringBuilder builder = new StringBuilder();
      String title = txtTitle_.getValue().trim();
      if (title.length() > 0)
      {
         builder.append("### ")
                .append(SafeHtmlUtils.htmlEscape(title))
                .append("\n");
      }

      String author = txtAuthor_.getValue().trim();
      if (author.length() > 0)
      {
         builder.append(SafeHtmlUtils.htmlEscape(author))
                .append(" --- ");
      }
      builder.append("*");
      builder.append(StringUtil.formatDate(new Date()));
      builder.append("*");
      
      return builder.toString();
   }

   private String createSuffix()
   {
      return "";
   }

   @Override
   protected boolean validate(CompileNotebookOptions input)
   {
      return true;
   }

   @Override
   protected Widget createMainWidget()
   {
      return widget_;
   }

   private String getType()
   {
      return listType_.getValue(listType_.getSelectedIndex());
   }
   
   private void setType(String type)
   {
      int typeIndex = 0;
      for (int i=0; i<listType_.getItemCount(); i++)
      {
         if (type == listType_.getValue(i))
         {
            typeIndex = i;
            break;
         }
      }
      listType_.setSelectedIndex(typeIndex);
   }
   
   private final String docId_;

   @UiField
   Element dialogInfo_;
   @UiField
   Element titleLabel_;
   @UiField
   TextBox txtTitle_;
   @UiField
   Element authorLabel_;
   @UiField
   TextBox txtAuthor_;
   @UiField
   DivElement divTypeSelector_;
   @UiField
   HorizontalPanel typeLabelPanel_;
   @UiField
   FormLabel lblType_;
   @UiField
   ListBox listType_;
   
   private boolean showTypes_;

   private Widget widget_;
}
