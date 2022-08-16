/*
 * VisualModeLintSource.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
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
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.workbench.views.output.lint.model.LintItem;
import org.rstudio.studio.client.workbench.views.output.lint.model.LintSource;
import org.rstudio.studio.client.workbench.views.source.editors.text.CompletionContext;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetSpelling;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
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
      // Adjust offsets to account for chunk header row
      for (int i = 0; i < lint.length(); i++)
      {
         LintItem item = lint.get(i);
         item.setStartRow(item.getStartRow() + 1);
         item.setEndRow(item.getEndRow() + 1);
      }

      chunk_.showLint(lint, false);
   }

   @Override
   public String getCode()
   {
      // The code to be linted is the contents of the chunk after the first
      // line (which is, of course, the chunk header)
      return chunk_.getAceInstance().getCode(
         Position.create(1, 0),
         chunk_.getAceInstance().getDocumentEnd());
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
