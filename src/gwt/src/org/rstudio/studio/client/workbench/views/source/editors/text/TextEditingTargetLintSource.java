/*
 * TextEditingTargetLintSource.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.Command;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.workbench.views.output.lint.model.LintItem;
import org.rstudio.studio.client.workbench.views.output.lint.model.LintSource;
import org.rstudio.studio.client.workbench.views.source.editors.text.cpp.CppCompletionContext;

public class TextEditingTargetLintSource implements LintSource
{
   public TextEditingTargetLintSource(TextEditingTarget target)
   {
      target_ = target;
   }

   @Override
   public DocDisplay getDisplay()
   {
      return target_.getDocDisplay();
   }

   @Override
   public TextFileType getTextFileType()
   {
      return target_.getTextFileType();
   }

   @Override
   public void withSavedDocument(boolean fullSave, Command onComplete)
   {
      if (fullSave)
      {
         target_.saveThenExecute(null, false, onComplete);
      }
      else
      {
         target_.withSavedDocNoRetry(onComplete);
      }
   }

   @Override
   public String getPath()
   {
      return target_.getPath();
   }

   @Override
   public String getId()
   {
      return target_.getId();
   }

   @Override
   public TextEditingTargetSpelling getSpellingTarget()
   {
      return target_.getSpellingTarget();
   }

   @Override
   public CompletionContext getRCompletionContext()
   {
      return target_.getRCompletionContext();
   }

   @Override
   public CppCompletionContext getCppCompletionContext()
   {
      return target_.getCppCompletionContext();
   }

   @Override
   public void showLint(JsArray<LintItem> lint)
   {
      if (target_.isVisualEditorActive())
      {
         target_.getVisualMode().showLint(lint);
      }
      else
      {
         target_.getDocDisplay().showLint(lint);
      }
   }

   @Override
   public String getCode()
   {
      // Returning nothing here causes the server to retrieve the code to
      // lint from the source database.
      return "";
   }

   private final TextEditingTarget target_;
}
