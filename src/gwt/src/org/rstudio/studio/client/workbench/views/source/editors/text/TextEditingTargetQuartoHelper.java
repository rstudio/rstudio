/*
 * TextEditingTargetQuartoHelper.java
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


import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTarget;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;

import com.google.inject.Inject;

public class TextEditingTargetQuartoHelper
{
   public TextEditingTargetQuartoHelper(EditingTarget editingTarget, DocDisplay docDisplay)
   {
      docDisplay_ = docDisplay;
      editingTarget_ = editingTarget;
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   void initialize(GlobalDisplay display, Commands commands)
   {
      display_ = display;
      commands_ = commands;
   }
   
   
   public void manageCommands()
   {
      boolean isQuartoDoc = SourceDocument.XT_QUARTO_DOCUMENT
                                .equals(editingTarget_.getExtendedFileType());
      boolean hasQuartoExt = docDisplay_.getFileType().isQuartoMarkdown();
      
      // enable quarto render for quarto docs
      commands_.quartoRenderDocument().setVisible(isQuartoDoc);
      
      // disable traditional rmd knit commands for quarto docs
      commands_.previewHTML().setVisible(!isQuartoDoc && docDisplay_.getFileType().canPreviewHTML());
      commands_.knitDocument().setVisible(!isQuartoDoc && docDisplay_.getFileType().canKnitToHTML());
      commands_.knitWithParameters().setVisible(!isQuartoDoc && docDisplay_.getFileType().canKnitToHTML());
      commands_.clearKnitrCache().setVisible(!isQuartoDoc && docDisplay_.getFileType().canKnitToHTML());
      
      // disable some rmd file specific stuff for quarto ext
      commands_.executeSetupChunk().setVisible(!hasQuartoExt && docDisplay_.getFileType().canKnitToHTML());
      
   }
   
  
   
   private Commands commands_;
   private DocDisplay docDisplay_;
   private EditingTarget editingTarget_;
   
   private GlobalDisplay display_;
}
