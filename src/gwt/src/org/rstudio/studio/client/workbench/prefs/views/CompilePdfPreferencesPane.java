/*
 * CompilePdfPreferencesPane.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.HelpButton;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.common.latex.LatexProgramSelectWidget;
import org.rstudio.studio.client.common.rnw.RnwWeaveSelectWidget;
import org.rstudio.studio.client.common.synctex.SynctexUtils;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.prefs.model.CompilePdfPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefsAccessor;

public class CompilePdfPreferencesPane extends PreferencesPane
{
   @Inject
   public CompilePdfPreferencesPane(UIPrefs prefs,
                                    PreferencesDialogResources res)
   {
      prefs_ = prefs;
      res_ = res;
      PreferencesDialogBaseResources baseRes = PreferencesDialogBaseResources.INSTANCE;
   
      add(headerLabel("Program defaults (when not in a project)"));
     
      defaultSweaveEngine_ = new RnwWeaveSelectWidget();
      defaultSweaveEngine_.setValue(
                              prefs.defaultSweaveEngine().getGlobalValue());
      add(defaultSweaveEngine_);
      
      defaultLatexProgram_ = new LatexProgramSelectWidget();
      defaultLatexProgram_.setValue(
                              prefs.defaultLatexProgram().getGlobalValue());
      add(defaultLatexProgram_);
      
      Label perProjectLabel = new Label(
            "NOTE: The Rnw weave and LaTeX compilation options are also set on a " +
            "per-project (and optionally per-file) basis. Click the help " +
            "icons above for more details.");
           
      perProjectLabel.addStyleName(baseRes.styles().infoLabel());
      nudgeRight(perProjectLabel);
      spaced(perProjectLabel);
      add(perProjectLabel);
       
      add(headerLabel("LaTeX editing and compilation"));
      chkCleanTexi2DviOutput_ = new CheckBox(
                                     "Clean auxiliary output after compile");
      spaced(chkCleanTexi2DviOutput_);
      add(chkCleanTexi2DviOutput_);
      
      chkEnableShellEscape_ = new CheckBox("Enable shell escape commands");
      spaced(chkEnableShellEscape_);
      add(chkEnableShellEscape_);
      
      add(spaced(checkboxPref(
            "Insert numbered sections and subsections",
            prefs_.insertNumberedLatexSections(), false /*defaultSpace*/)));
            
      Label previewingOptionsLabel = headerLabel("PDF preview");
      previewingOptionsLabel.getElement().getStyle().setMarginTop(8, Unit.PX);
      add(previewingOptionsLabel);
     
      pdfPreview_ = new PdfPreviewSelectWidget();
      add(pdfPreview_);
      
      add(spaced(checkboxPref(
            "Always enable Rnw concordance (required for synctex)",
            prefs_.alwaysEnableRnwConcordance(),
            false /*defaultSpaces*/)));
   }

   private class PdfPreviewSelectWidget extends SelectWidget
   {
      public PdfPreviewSelectWidget()
      {
         super(
            "Preview PDF after compile using:", 
            new String[]{}, 
            new String[]{},
            false, 
            true, 
            false);   
         
         HelpButton.addHelpButton(this, "pdf_preview");
      }
   }
  


   @Override
   public ImageResource getIcon()
   {
      return new ImageResource2x(PreferencesDialogBaseResources.INSTANCE.iconCompilePdf2x());
   }

   @Override
   public boolean validate()
   {
      return true;
   }

   @Override
   public String getName()
   {
      return "Sweave";
   }

   @Override
   protected void initialize(RPrefs prefs)
   {
      CompilePdfPrefs compilePdfPrefs = prefs.getCompilePdfPrefs();
      chkCleanTexi2DviOutput_.setValue(compilePdfPrefs.getCleanOutput());
      chkEnableShellEscape_.setValue(compilePdfPrefs.getEnableShellEscape());
      
      pdfPreview_.addChoice("(No Preview)", UIPrefsAccessor.PDF_PREVIEW_NONE);
      
      String desktopSynctexViewer = SynctexUtils.getDesktopSynctexViewer();
      if (desktopSynctexViewer.length() > 0)
      {
         pdfPreview_.addChoice(desktopSynctexViewer  + " (Recommended)", 
                               UIPrefsAccessor.PDF_PREVIEW_DESKTOP_SYNCTEX);
      }
      
      pdfPreview_.addChoice("RStudio Viewer", 
                            UIPrefsAccessor.PDF_PREVIEW_RSTUDIO);
      
      pdfPreview_.addChoice("System Viewer",
                            UIPrefsAccessor.PDF_PREVIEW_SYSTEM);
      
      pdfPreview_.setValue(prefs_.pdfPreview().getValue());
   }
   
   @Override
   public boolean onApply(RPrefs rPrefs)
   {
      boolean requiresRestart = super.onApply(rPrefs);
      
      prefs_.defaultSweaveEngine().setGlobalValue(
                                    defaultSweaveEngine_.getValue());
      prefs_.defaultLatexProgram().setGlobalValue(
                                    defaultLatexProgram_.getValue());
      
      prefs_.pdfPreview().setGlobalValue(pdfPreview_.getValue());
         
      CompilePdfPrefs prefs = CompilePdfPrefs.create(
                                       chkCleanTexi2DviOutput_.getValue(),
                                       chkEnableShellEscape_.getValue());
      rPrefs.setCompilePdfPrefs(prefs);
      
      return requiresRestart;
   }

   private final UIPrefs prefs_;
   
   @SuppressWarnings("unused")
   private final PreferencesDialogResources res_;
   
   private RnwWeaveSelectWidget defaultSweaveEngine_;
   private LatexProgramSelectWidget defaultLatexProgram_;
   private CheckBox chkCleanTexi2DviOutput_;
   private CheckBox chkEnableShellEscape_;
   private PdfPreviewSelectWidget pdfPreview_;
   
}
