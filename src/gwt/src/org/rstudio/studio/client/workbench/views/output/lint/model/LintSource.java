/*
 * LintSource.java
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
package org.rstudio.studio.client.workbench.views.output.lint.model;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.Command;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.workbench.views.source.editors.text.CompletionContext;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTargetSpelling;
import org.rstudio.studio.client.workbench.views.source.editors.text.cpp.CppCompletionContext;

public interface LintSource
{
   /**
    * Returns the editor in which to display the lint.
    *
    * @return The document editor.
    */
   public DocDisplay getDisplay();

   /**
    * Returns the type of the file loaded into the editor.
    *
    * @return The file type.
    */
   public TextFileType getTextFileType();

   /**
    * Executes a command after saving the document.
    *
    * @param fullSave Whether to perform a full save.
    * @param onComplete The command to execute after saving.
    */
   public void withSavedDocument(boolean fullSave, Command onComplete);

   /**
    * Return the document's path.
    *
    * @return The path at which the document is saved, or null if unsaved
    */
   public String getPath();

   /**
    * Return the document's ID.
    *
    * @return A unique ID representing the document
    */
   public String getId();

   /**
    * Return the associated spelling target.
    *
    * @return The spelling target.
    */
   public TextEditingTargetSpelling getSpellingTarget();

   /**
    * Return the R code completion context.
    *
    * @return The R code completion context.
    */
   public CompletionContext getRCompletionContext();

   /**
    * Return the C++ code completion context
    *
    * @return the C++ code completion context.
    */
   public CppCompletionContext getCppCompletionContext();

   /**
    * Show an array of lint items.
    *
    * @param lint The lint to display.
    */
   public void showLint(JsArray<LintItem> lint);

   /**
    * Gets the code to lint.
    *
    * @return Code to lint, or empty string for all code.
    */
   public String getCode();

   /**
    * Indicates whether the document should be linted after a save operation.
    *
    * @return Whether to lint the document on save.
    */
   public boolean lintOnSave();
}