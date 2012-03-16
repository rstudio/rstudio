/*
 * SpellChecker.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.common.spelling;

import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.spelling.model.SpellingServerOperations;
import org.rstudio.studio.client.common.spelling.view.SpellingSandboxDialog;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class SpellChecker
{
   @Inject
   public SpellChecker(GlobalDisplay globalDisplay, 
                       SpellingServerOperations server,
                       Provider<SpellingSandboxDialog> pSpellingDialog)
   {
      globalDisplay_ = globalDisplay;
      server_ = server;
      pSpellingDialog_ = pSpellingDialog;
   }
   
   
   public void checkSpelling(String path, DocDisplay docDisplay)
   {
      SpellingSandboxDialog dlg = pSpellingDialog_.get();
      
      dlg.showModal();
   }
   
   @SuppressWarnings("unused")
   private final GlobalDisplay globalDisplay_;
   @SuppressWarnings("unused")
   private final SpellingServerOperations server_;
   private final Provider<SpellingSandboxDialog> pSpellingDialog_;
}
