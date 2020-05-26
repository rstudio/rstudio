/*
 * RmdTemplateOptionsDialog.java
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
package org.rstudio.studio.client.rmarkdown.ui;

import com.google.gwt.aria.client.Roles;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.application.ui.RStudioThemes;
import org.rstudio.studio.client.rmarkdown.model.RmdFrontMatter;
import org.rstudio.studio.client.rmarkdown.model.RmdFrontMatterOutputOptions;
import org.rstudio.studio.client.rmarkdown.model.RmdTemplate;

import com.google.gwt.user.client.ui.Widget;

public class RmdTemplateOptionsDialog 
   extends ModalDialog<RmdTemplateOptionsDialog.Result>
{
   public class Result
   {
      public Result(String format, RmdFrontMatterOutputOptions options)
      {
         this.outputOptions = options;
         this.format = format;
      }

      public String format;
      public RmdFrontMatterOutputOptions outputOptions;
   }

   public RmdTemplateOptionsDialog(RmdTemplate template,
         String initialFormat,
         RmdFrontMatter frontMatter, 
         FileSystemItem document,
         boolean isShiny,
         OperationWithInput<RmdTemplateOptionsDialog.Result> onSaved,
         Operation onCancelled)
   {
      super("Edit " + (isShiny ? "Shiny " : "R Markdown ") + 
            template.getName() + " Options", Roles.getDialogRole(), onSaved, onCancelled);
      setWidth("425px");
      setHeight(
         RStudioThemes.isFlat() ? "430px" : "450px"
      );
      templateOptions_ = new RmdTemplateOptionsWidget(!isShiny);
      templateOptions_.setDocument(document);
      templateOptions_.setTemplate(template, false, frontMatter);
      templateOptions_.setSelectedFormat(initialFormat);
      templateOptions_.setHeight("350px");
      templateOptions_.setWidth("450px");
   }

   @Override
   protected Widget createMainWidget()
   {
      return templateOptions_;
   }
   
   @Override
   protected RmdTemplateOptionsDialog.Result collectInput()
   {
      return new Result(templateOptions_.getSelectedFormat(),
                        templateOptions_.getOutputOptions());
   }

   @Override
   protected boolean validate(RmdTemplateOptionsDialog.Result input)
   {
      return true;
   }

   RmdTemplateOptionsWidget templateOptions_;
}
