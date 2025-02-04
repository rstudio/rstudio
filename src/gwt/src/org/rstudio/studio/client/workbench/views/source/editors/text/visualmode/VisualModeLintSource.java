/*
 * VisualModeLintSource.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text.visualmode;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.Command;

import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.workbench.views.output.lint.model.LintItem;
import org.rstudio.studio.client.workbench.views.output.lint.model.LintSource;
import org.rstudio.studio.client.workbench.views.source.editors.text.CompletionContext;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetSpelling;
import org.rstudio.studio.client.workbench.views.source.editors.text.cpp.CppCompletionContext;

public class VisualModeLintSource implements LintSource
{
   public VisualModeLintSource(VisualModeChunk chunk)
   {
      chunk_ = chunk;
      parent_ = chunk.getParentEditingTarget();
   }

   @Override
   public DocDisplay getDisplay()
   {
      return chunk_.getAceInstance();
   }

   @Override
   public TextFileType getTextFileType()
   {
      return chunk_.getAceInstance().getFileType();
   }

   @Override
   public void withSavedDocument(boolean fullSave, Command onComplete)
   {
      // The TextEditingTargetLintSource needs to save the document prior to linting because
      // the server works with the saved copy in the source database. But this lint source
      // works with unsaved (per-chunk) data, so we don't need to perform a save before
      // triggering the lint.
      onComplete.execute();
   }

   @Override
   public String getPath()
   {
      return parent_.getPath();
   }

   @Override
   public String getId()
   {
      return parent_.getId();
   }

   @Override
   public TextEditingTargetSpelling getSpellingTarget()
   {
      return parent_.getSpellingTarget();
   }

   @Override
   public CompletionContext getRCompletionContext()
   {
      return parent_.getRCompletionContext();
   }

   @Override
   public CppCompletionContext getCppCompletionContext()
   {
      return parent_.getCppCompletionContext();
   }

   @Override
   public void showLint(JsArray<LintItem> lint)
   {
      chunk_.showLint(lint, false);
   }

   @Override
   public String getCode()
   {
      // Get the code for this chunk.
      // Append a newline to make pattern matching easier below.
      String code = chunk_.getAceInstance().getCode() + "\n";

      // Remove a leading chunk header, if any. Note that we apply
      // code diagnostics to both evaluated an un-evaluated R code
      // chunks, and un-evaluated code chunks do not include a header.
      //
      // https://github.com/rstudio/rstudio/issues/15592
      Pattern pattern = Pattern.create("^{\\s*[rR]\\b[^\\n]*}\\n", "");
      Match match = pattern.match(code, 0);
      if (match != null)
      {
         // Remove the chunk header, but retain the newline so lint positions
         // can match correctly.
         int index = match.getGroup(0).length() - 1;
         code = code.substring(index);
      }

      return code;
   }

   @Override
   public boolean lintOnSave()
   {
      // Individual chunks of code aren't linted on save in visual mode;
      // lint on save happens in the outer editor and results are forwarded
      // into chunks.
      return false;
   }

   private final VisualModeChunk chunk_;
   private final TextEditingTarget parent_;
}
