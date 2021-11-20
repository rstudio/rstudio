/*
 * VisualModeLintSource.java
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
      return parent_.getTextFileType();
   }

   @Override
   public void withSavedDocument(boolean fullSave, Command onComplete)
   {
      // TODO: Do we need to save? Maybe not since we're providing code?
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
      chunk_.showLint(lint);
   }

   @Override
   public String getCode()
   {
      return chunk_.getAceInstance().getCode();
   }

   private final VisualModeChunk chunk_;
   private final TextEditingTarget parent_;
}
