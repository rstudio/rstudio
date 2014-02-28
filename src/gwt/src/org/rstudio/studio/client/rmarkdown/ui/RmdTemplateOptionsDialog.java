/*
 * RmdTemplateOptionsDialog.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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

import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.rmarkdown.model.RmdFrontMatter;
import org.rstudio.studio.client.rmarkdown.model.RmdTemplate;

import com.google.gwt.user.client.ui.Widget;

public class RmdTemplateOptionsDialog 
   extends ModalDialog<RmdTemplateOptionsDialog.Result>
{
   public class Result
   {
      public Result(RmdFrontMatter frontMatter, String format)
      {
         this.frontMatter = frontMatter;
         this.format = format;
      }

      public RmdFrontMatter frontMatter;
      public String format;
   }

   public RmdTemplateOptionsDialog(RmdTemplate template,
         String initialFormat,
         RmdFrontMatter frontMatter, 
         OperationWithInput<RmdTemplateOptionsDialog.Result> onSaved)
   {
      super("Edit R Markdown Format Options", onSaved);
      setWidth("350px");
      setHeight("400px");
      templateOptions_ = new RmdTemplateOptionsWidget();
      templateOptions_.setTemplate(template, false, frontMatter);
      templateOptions_.setSelectedFormat(initialFormat);
      templateOptions_.setHeight("300px");
      templateOptions_.setWidth("375px");
   }

   @Override
   protected Widget createMainWidget()
   {
      return templateOptions_;
   }
   
   @Override
   protected RmdTemplateOptionsDialog.Result collectInput()
   {
      return new Result(templateOptions_.getFrontMatter(), 
                        templateOptions_.getSelectedFormat());
   }

   @Override
   protected boolean validate(RmdTemplateOptionsDialog.Result input)
   {
      return true;
   }

   RmdTemplateOptionsWidget templateOptions_;
}
