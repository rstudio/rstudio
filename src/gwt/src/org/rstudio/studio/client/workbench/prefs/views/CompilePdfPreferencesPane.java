/*
 * CompilePdfPreferencesPane.java
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
package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Label;
import com.google.inject.Inject;

import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.HelpButton;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.common.latex.LatexProgramSelectWidget;
import org.rstudio.studio.client.common.rnw.RnwWeaveSelectWidget;
import org.rstudio.studio.client.common.synctex.SynctexUtils;
import org.rstudio.studio.client.workbench.prefs.PrefsConstants;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;

public class CompilePdfPreferencesPane extends PreferencesPane
{
   @Inject
   public CompilePdfPreferencesPane(UserPrefs prefs,
                                    PreferencesDialogResources res)
   {
      prefs_ = prefs;
      res_ = res;
      PreferencesDialogBaseResources baseRes = PreferencesDialogBaseResources.INSTANCE;

      add(headerLabel(constants_.headerPDFGenerationLabel()));

      defaultSweaveEngine_ = new RnwWeaveSelectWidget();
      defaultSweaveEngine_.setValue(
                              prefs.defaultSweaveEngine().getGlobalValue());
      add(defaultSweaveEngine_);

      defaultLatexProgram_ = new LatexProgramSelectWidget();
      defaultLatexProgram_.setValue(
                              prefs.defaultLatexProgram().getGlobalValue());
      add(defaultLatexProgram_);

      Label perProjectLabel = new Label(
            constants_.perProjectNoteLabel());

      perProjectLabel.addStyleName(baseRes.styles().infoLabel());
      nudgeRight(perProjectLabel);
      spaced(perProjectLabel);
      add(perProjectLabel);

      add(headerLabel(constants_.perProjectHeaderLabel()));

      chkUseTinytex_ = new CheckBox(constants_.chkUseTinytexLabel());
      spaced(chkUseTinytex_);
      add(chkUseTinytex_);

      chkCleanTexi2DviOutput_ = new CheckBox(constants_.chkCleanTexi2DviOutputLabel());
      spaced(chkCleanTexi2DviOutput_);
      add(chkCleanTexi2DviOutput_);

      chkEnableShellEscape_ = new CheckBox(constants_.chkEnableShellEscapeLabel());
      spaced(chkEnableShellEscape_);
      add(chkEnableShellEscape_);

      add(spaced(checkboxPref(
            constants_.insertNumberedLatexSectionsLabel(),
            prefs_.insertNumberedLatexSections(), false /*defaultSpace*/)));

      Label previewingOptionsLabel = headerLabel(constants_.previewingOptionsHeaderLabel());
      previewingOptionsLabel.getElement().getStyle().setMarginTop(8, Unit.PX);
      add(previewingOptionsLabel);

      pdfPreview_ = new PdfPreviewSelectWidget();
      add(pdfPreview_);

      add(spaced(checkboxPref(
            constants_.alwaysEnableRnwConcordanceLabel(),
            prefs_.alwaysEnableRnwConcordance(),
            false /*defaultSpaces*/)));
   }

   private class PdfPreviewSelectWidget extends SelectWidget
   {
      public PdfPreviewSelectWidget()
      {
         super(
            constants_.pdfPreviewSelectWidgetLabel(),
            new String[]{},
            new String[]{},
            false,
            true,
            false);

         HelpButton.addHelpButton(this, "pdf_preview", constants_.pdfPreviewHelpButtonTitle());
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
      return constants_.preferencesPaneTitle();
   }

   @Override
   protected void initialize(UserPrefs prefs)
   {
      chkUseTinytex_.setValue(prefs.useTinytex().getValue());
      chkCleanTexi2DviOutput_.setValue(prefs.cleanTexi2dviOutput().getValue());
      chkEnableShellEscape_.setValue(prefs.latexShellEscape().getValue());

      pdfPreview_.addChoice(constants_.pdfNoPreviewOption(), UserPrefs.PDF_PREVIEWER_NONE);

      String desktopSynctexViewer = SynctexUtils.getDesktopSynctexViewer();
      if (desktopSynctexViewer.length() > 0)
      {
         pdfPreview_.addChoice(desktopSynctexViewer  + " " + constants_.pdfPreviewSumatraOption(),
                               UserPrefs.PDF_PREVIEWER_DESKTOP_SYNCTEX);
      }

      pdfPreview_.addChoice(constants_.pdfPreviewRStudioViewerOption(),
                            UserPrefs.PDF_PREVIEWER_RSTUDIO);

      pdfPreview_.addChoice(constants_.pdfPreviewSystemViewerOption(),
                            UserPrefs.PDF_PREVIEWER_SYSTEM);

      pdfPreview_.setValue(prefs_.pdfPreviewer().getValue());
   }

   @Override
   public RestartRequirement onApply(UserPrefs rPrefs)
   {
      RestartRequirement restartRequirement = super.onApply(rPrefs);

      prefs_.defaultSweaveEngine().setGlobalValue(
                                    defaultSweaveEngine_.getValue());
      prefs_.defaultLatexProgram().setGlobalValue(
                                    defaultLatexProgram_.getValue());

      prefs_.pdfPreviewer().setGlobalValue(pdfPreview_.getValue());

      prefs_.useTinytex().setGlobalValue(chkUseTinytex_.getValue());

      prefs_.cleanTexi2dviOutput().setGlobalValue(
            chkCleanTexi2DviOutput_.getValue());

      prefs_.latexShellEscape().setGlobalValue(
            chkEnableShellEscape_.getValue());

      return restartRequirement;
   }

   private final UserPrefs prefs_;

   @SuppressWarnings("unused")
   private final PreferencesDialogResources res_;

   private RnwWeaveSelectWidget defaultSweaveEngine_;
   private LatexProgramSelectWidget defaultLatexProgram_;
   private CheckBox chkUseTinytex_;
   private CheckBox chkCleanTexi2DviOutput_;
   private CheckBox chkEnableShellEscape_;
   private PdfPreviewSelectWidget pdfPreview_;
   private static final PrefsConstants constants_ = GWT.create(PrefsConstants.class);

}
