/*
 * ProjectCompilePdfPreferencesPane.java
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
package org.rstudio.studio.client.projects.ui.prefs;

import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.HelpButton;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.TextBoxWithButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.latex.LatexProgramSelectWidget;
import org.rstudio.studio.client.common.rnw.RnwWeaveSelectWidget;
import org.rstudio.studio.client.projects.StudioClientProjectConstants;
import org.rstudio.studio.client.projects.model.RProjectConfig;
import org.rstudio.studio.client.projects.model.RProjectOptions;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.inject.Inject;

public class ProjectCompilePdfPreferencesPane extends ProjectPreferencesPane
{
   @Inject
   public ProjectCompilePdfPreferencesPane()
   {
      addHeader(constants_.pdfGenerationCaption());

      defaultSweaveEngine_ = new RnwWeaveSelectWidget();
      add(defaultSweaveEngine_);

      defaultLatexProgram_ = new LatexProgramSelectWidget();
      add(defaultLatexProgram_);

      addHeader(constants_.pdfPreviewCaption());

      rootDoc_ = new RootDocumentChooser();
      nudgeRight(rootDoc_);
      add(rootDoc_);
   }

   @Override
   public ImageResource getIcon()
   {
      return new ImageResource2x(PreferencesDialogBaseResources.INSTANCE.iconCompilePdf2x());
   }

   @Override
   public String getName()
   {
      return "Sweave";
   }

   @Override
   protected void initialize(RProjectOptions options)
   {
      RProjectConfig config = options.getConfig();
      defaultSweaveEngine_.setValue(config.getDefaultSweaveEngine());
      defaultLatexProgram_.setValue(config.getDefaultLatexProgram());
      rootDoc_.setText(config.getRootDocument());
   }

   @Override
   public boolean validate()
   {
      return true;
   }

   @Override
   public RestartRequirement onApply(RProjectOptions options)
   {
      RProjectConfig config = options.getConfig();
      config.setDefaultSweaveEngine(defaultSweaveEngine_.getValue());
      config.setDefaultLatexProgram(defaultLatexProgram_.getValue());
      config.setRootDocument(rootDoc_.getText().trim());
      return new RestartRequirement();
   }

   private class RootDocumentChooser extends TextBoxWithButton
   {
      public RootDocumentChooser()
      {
         super(constants_.compilePDFLabel(),
               constants_.compilePDFEmptyLabel(),
               constants_.browseActionLabel(),
               new HelpButton("pdf_root_document", constants_.rootDocumentChooserTitle()),
               ElementIds.TextBoxButtonId.PDF_ROOT,
               true,
               null);

         // allow user to set the value to empty string
         setReadOnly(false);

         addClickHandler(new ClickHandler()
         {
            public void onClick(ClickEvent event)
            {
               final FileSystemItem projDir = RStudioGinjector.INSTANCE.
                         getSession().getSessionInfo().getActiveProjectDir();

               RStudioGinjector.INSTANCE.getFileDialogs().openFile(
                     constants_.chooseFileCaption(),
                     RStudioGinjector.INSTANCE.getRemoteFileSystemContext(),
                     projDir,
                     new ProgressOperationWithInput<FileSystemItem>()
                     {
                        public void execute(FileSystemItem input,
                                            ProgressIndicator indicator)
                        {
                           if (input == null)
                              return;

                           indicator.onCompleted();

                           String proj = projDir.getPath();
                           if (input.getPath().startsWith(proj + "/"))
                           {
                              String projRelative =
                                input.getPath().substring(proj.length() + 1);
                              setText(projRelative);
                           }
                           else
                           {

                           }
                        }
                     });
            }
         });

      }

      // allow user to set the value to empty string
      @Override
      public String getText()
      {
         if (getTextBox().getText().trim().isEmpty())
            return "";
         else
            return super.getText();
      }
   }

   private RnwWeaveSelectWidget defaultSweaveEngine_;
   private LatexProgramSelectWidget defaultLatexProgram_;
   private TextBoxWithButton rootDoc_;
   private static final StudioClientProjectConstants constants_ = GWT.create(StudioClientProjectConstants.class);

}
