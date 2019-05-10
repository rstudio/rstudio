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
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefUtils;

public class RMarkdownPreferencesPane extends PreferencesPane
{
   @Inject
   public RMarkdownPreferencesPane(UserPrefs prefs,
                                   PreferencesDialogResources res,
                                   Session session)
   {
      prefs_ = prefs;
      res_ = res;
    
      add(headerLabel("R Markdown"));
      
      add(checkboxPref("Show inline toolbar for R code chunks", prefs_.showInlineToolbarForRCodeChunks()));
      add(checkboxPref("Show document outline by default", prefs_.showDocOutlineRmd()));
      add(checkboxPref("Enable chunk background highlight", prefs_.highlightCodeChunks()));
      
      docOutlineDisplay_ = new SelectWidget(
            "Show in document outline: ",
            new String[] {
                  "Sections Only",
                  "Sections and Named Chunks",
                  "Sections and All Chunks"
            },
            new String[] {
                 UserPrefs.DOC_OUTLINE_SHOW_SECTIONS_ONLY,
                 UserPrefs.DOC_OUTLINE_SHOW_SECTIONS_AND_CHUNKS,
                 UserPrefs.DOC_OUTLINE_SHOW_ALL
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
                  UserPrefs.RMD_VIEWER_TYPE_WINDOW,
                  UserPrefs.RMD_VIEWER_TYPE_PANE,
                  UserPrefs.RMD_VIEWER_TYPE_NONE
            },
            false,
            true,
            false);
      add(rmdViewerMode_);

       
      // show output inline for all Rmds
      final CheckBox rmdInlineOutput = checkboxPref(
            "Show output inline for all R Markdown documents",
            prefs_.rmdChunkOutputInline());
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
                  UserPrefs.LATEX_PREVIEW_ON_CURSOR_IDLE_NEVER,
                  UserPrefs.LATEX_PREVIEW_ON_CURSOR_IDLE_INLINE_ONLY,
                  UserPrefs.LATEX_PREVIEW_ON_CURSOR_IDLE_ALWAYS
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
                     UserPrefs.KNIT_WORKING_DIR_DEFAULT,
                     UserPrefs.KNIT_WORKING_DIR_CURRENT,
                     UserPrefs.KNIT_WORKING_DIR_PROJECT
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
      
      final CheckBox showRmdRenderCommand = checkboxPref(
            "Display render command in R Markdown tab",
            prefs_.showRmdRenderCommand());
      add(showRmdRenderCommand);
      
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
   protected void initialize(UserPrefs prefs)
   {
      docOutlineDisplay_.setValue(prefs_.docOutlineShow().getValue());
      rmdViewerMode_.setValue(prefs_.rmdViewerType().getValue().toString());
      latexPreviewWidget_.setValue(prefs_.latexPreviewOnCursorIdle().getValue());
      if (knitWorkingDir_ != null)
         knitWorkingDir_.setValue(prefs_.knitWorkingDir().getValue());
   }
   
   @Override
   public boolean onApply(UserPrefs rPrefs)
   {
      boolean requiresRestart = super.onApply(rPrefs);
      
      prefs_.docOutlineShow().setGlobalValue(
            docOutlineDisplay_.getValue());
      
      prefs_.rmdViewerType().setGlobalValue(
            rmdViewerMode_.getValue());
      
      prefs_.latexPreviewOnCursorIdle().setGlobalValue(
            latexPreviewWidget_.getValue());
      
      if (knitWorkingDir_ != null)
      {
         prefs_.knitWorkingDir().setGlobalValue(
               knitWorkingDir_.getValue());
      }
      
      return requiresRestart;
   }

   private final UserPrefs prefs_;
   
   private final PreferencesDialogResources res_;
   
   private final SelectWidget rmdViewerMode_;
   private final SelectWidget docOutlineDisplay_;
   private final SelectWidget latexPreviewWidget_;
   private final SelectWidget knitWorkingDir_;
}
