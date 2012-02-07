/*
 * WritingPreferencesPane.java
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
package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;
import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.studio.client.common.latex.LatexProgramSelectWidget;
import org.rstudio.studio.client.common.rnw.RnwWeaveSelectWidget;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

public class WritingPreferencesPane extends PreferencesPane
{
   @Inject
   public WritingPreferencesPane(UIPrefs prefs,
                                 PreferencesDialogResources res)
   {
      prefs_ = prefs;
      res_ = res;
      PreferencesDialogBaseResources baseRes = PreferencesDialogBaseResources.INSTANCE;

      Label pdfCompilationLabel = new Label("Compile PDF");
      pdfCompilationLabel.addStyleName(baseRes.styles().headerLabel());
      nudgeRight(pdfCompilationLabel);
      add(pdfCompilationLabel);
            
      defaultSweaveEngine_ = new RnwWeaveSelectWidget();
      defaultSweaveEngine_.setValue(
                              prefs.defaultSweaveEngine().getGlobalValue());
      add(defaultSweaveEngine_);
      
      defaultLatexProgram_ = new LatexProgramSelectWidget();
      defaultLatexProgram_.setValue(
                              prefs.defaultLatexProgram().getGlobalValue());
      add(defaultLatexProgram_);
      
      Label perProjectLabel = new Label(
            "NOTE: The Rnw weave and LaTeX program options are also set on a " +
            "per-project (and optionally per-file) basis. Click the help " +
            "icons above for more details.");
           
      perProjectLabel.addStyleName(baseRes.styles().infoLabel());
      nudgeRight(perProjectLabel);
      add(perProjectLabel);
      
   }

  


   @Override
   public ImageResource getIcon()
   {
      return PreferencesDialogBaseResources.INSTANCE.iconWriting();
   }

   @Override
   public boolean validate()
   {
      return true;
   }

   @Override
   public String getName()
   {
      return "Authoring";
   }

   @Override
   protected void initialize(RPrefs prefs)
   {
   }
   
   @Override
   public boolean onApply(RPrefs rPrefs)
   {
      boolean requiresRestart = super.onApply(rPrefs);
      prefs_.defaultSweaveEngine().setGlobalValue(
                                    defaultSweaveEngine_.getValue());
      prefs_.defaultLatexProgram().setGlobalValue(
                                    defaultLatexProgram_.getValue());
      return requiresRestart;
   }

   private final UIPrefs prefs_;
   @SuppressWarnings("unused")
   private final PreferencesDialogResources res_;
   
   private RnwWeaveSelectWidget defaultSweaveEngine_;
   private LatexProgramSelectWidget defaultLatexProgram_;
   
}
