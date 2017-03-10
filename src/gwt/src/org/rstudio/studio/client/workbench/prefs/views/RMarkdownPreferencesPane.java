/*
 * RMarkdownPreferencesPane.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.inject.Inject;

import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.rmarkdown.RmdOutput;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefsAccessor;

public class RMarkdownPreferencesPane extends PreferencesPane
{
   @Inject
   public RMarkdownPreferencesPane(UIPrefs prefs,
                                   PreferencesDialogResources res,
                                   Session session)
   {
      prefs_ = prefs;
      res_ = res;
    
      add(headerLabel("R Markdown"));
      
      add(checkboxPref("Show inline toolbar for R code chunks", prefs_.showInlineToolbarForRCodeChunks()));
      add(checkboxPref("Show document outline by default", prefs_.showDocumentOutlineRmd()));
      
      docOutlineDisplay_ = new SelectWidget(
            "Show in Document Outline: ",
            new String[] {
                  "Sections Only",
                  "Sections and Named Chunks",
                  "Sections and All Chunks"
            },
            new String[] {
                  UIPrefsAccessor.DOC_OUTLINE_SHOW_SECTIONS_ONLY,
                  UIPrefsAccessor.DOC_OUTLINE_SHOW_SECTIONS_AND_NAMED_CHUNKS,
                  UIPrefsAccessor.DOC_OUTLINE_SHOW_ALL
            },
            false,
            true,
            false);
      add(docOutlineDisplay_);
      
      rmdViewerMode_ = new SelectWidget(
            "Show output preview in: ",
            new String[] {
                  "Window",
                  "Viewer Pane",
                  "(None)"
            },
            new String[] {
                  new Integer(RmdOutput.RMD_VIEWER_TYPE_WINDOW).toString(),
                  new Integer(RmdOutput.RMD_VIEWER_TYPE_PANE).toString(),
                  new Integer(RmdOutput.RMD_VIEWER_TYPE_NONE).toString()
            },
            false,
            true,
            false);
      add(rmdViewerMode_);

       
      // show output inline for all Rmds
      final CheckBox rmdInlineOutput = checkboxPref(
            "Show output inline for all R Markdown documents",
            prefs_.showRmdChunkOutputInline());
      add(rmdInlineOutput);
      
      // behavior for latex and image preview popups
      latexPreviewWidget_ = new SelectWidget(
            "Show equation and image previews: ",
            new String[] {
                  "Never",
                  "In a popup",
                  "Inline"
            },
            new String[] {
                  UIPrefsAccessor.LATEX_PREVIEW_SHOW_NEVER,
                  UIPrefsAccessor.LATEX_PREVIEW_SHOW_INLINE_ONLY,
                  UIPrefsAccessor.LATEX_PREVIEW_SHOW_ALWAYS
            },
            false,
            true,
            false);
      add(latexPreviewWidget_);
      
      if (session.getSessionInfo().getKnitWorkingDirAvailable())
      {
         knitWorkingDir_ = new SelectWidget(
               "Evaluate chunks in directory: ",
               new String[] {
                     "Document",
                     "Current",
                     "Project"
               },
               new String[] {
                     UIPrefsAccessor.KNIT_DIR_DEFAULT,
                     UIPrefsAccessor.KNIT_DIR_CURRENT,
                     UIPrefsAccessor.KNIT_DIR_PROJECT
               },
               false,
               true,
               false);
         add(knitWorkingDir_);
      }
      else
      {
         knitWorkingDir_ = null;
      }
      
      add(spacedBefore(headerLabel("R Notebooks")));

      // auto-execute the setup chunk
      final CheckBox autoExecuteSetupChunk = checkboxPref(
            "Execute setup chunk automatically in notebooks", 
            prefs_.autoRunSetupChunk());
      add(autoExecuteSetupChunk);
      
      // hide console when executing notebook chunks
      final CheckBox notebookHideConsole = checkboxPref(
            "Hide console automatically when executing " +
            "notebook chunks",
            prefs_.hideConsoleOnChunkExecute());
      add(notebookHideConsole);
      
      add(spacedBefore(new HelpLink("Using R Notebooks", "using_notebooks")));
   }

   @Override
   public ImageResource getIcon()
   {
      return new ImageResource2x(res_.iconRMarkdown2x());
   }

   @Override
   public boolean validate()
   {
      return true;
   }

   @Override
   public String getName()
   {
      return "R Markdown";
   }

   @Override
   protected void initialize(RPrefs prefs)
   {
      docOutlineDisplay_.setValue(prefs_.shownSectionsInDocumentOutline().getValue().toString());
      rmdViewerMode_.setValue(prefs_.rmdViewerType().getValue().toString());
      latexPreviewWidget_.setValue(prefs_.showLatexPreviewOnCursorIdle().getValue().toString());
   }
   
   @Override
   public boolean onApply(RPrefs rPrefs)
   {
      boolean requiresRestart = super.onApply(rPrefs);
      
      prefs_.shownSectionsInDocumentOutline().setGlobalValue(
            docOutlineDisplay_.getValue());
      
      prefs_.rmdViewerType().setGlobalValue(Integer.decode(
            rmdViewerMode_.getValue()));
      
      prefs_.showLatexPreviewOnCursorIdle().setGlobalValue(
            latexPreviewWidget_.getValue());
      
      if (knitWorkingDir_ != null)
      {
         prefs_.knitWorkingDir().setGlobalValue(
               knitWorkingDir_.getValue());
      }
      
      return requiresRestart;
   }

   private final UIPrefs prefs_;
   
   private final PreferencesDialogResources res_;
   
   private final SelectWidget rmdViewerMode_;
   private final SelectWidget docOutlineDisplay_;
   private final SelectWidget latexPreviewWidget_;
   private final SelectWidget knitWorkingDir_;
}
