/*
 * RMarkdownPreferencesPane.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.DialogTabLayoutPanel;
import org.rstudio.core.client.theme.VerticalTabPanel;
import org.rstudio.core.client.widget.NumericValueWidget;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsAccessor;

public class RMarkdownPreferencesPane extends PreferencesPane
{
   @Inject
   public RMarkdownPreferencesPane(UserPrefs prefs,
                                   PreferencesDialogResources res,
                                   Session session)
   {
      prefs_ = prefs;
      res_ = res;
      PreferencesDialogBaseResources baseRes = PreferencesDialogBaseResources.INSTANCE;
      
      VerticalTabPanel basic = new VerticalTabPanel(ElementIds.RMARKDOWN_BASIC_PREFS);
      
      basic.add(headerLabel("R Markdown"));
      
      basic.add(checkboxPref("Show document outline by default", prefs_.showDocOutlineRmd()));
      basic.add(checkboxPref("Soft-wrap R Markdown files", prefs_.softWrapRmdFiles()));
      
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
      basic.add(docOutlineDisplay_);
      
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
      basic.add(rmdViewerMode_);

       
      // show output inline for all Rmds
      final CheckBox rmdInlineOutput = checkboxPref(
            "Show output inline for all R Markdown documents",
            prefs_.rmdChunkOutputInline());
      basic.add(rmdInlineOutput);
      
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
      basic.add(latexPreviewWidget_);
      
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
         basic.add(knitWorkingDir_);
      }
      else
      {
         knitWorkingDir_ = null;
      }
      
      basic.add(spacedBefore(headerLabel("R Notebooks")));

      // auto-execute the setup chunk
      final CheckBox autoExecuteSetupChunk = checkboxPref(
            "Execute setup chunk automatically in notebooks", 
            prefs_.autoRunSetupChunk());
      basic.add(autoExecuteSetupChunk);
      
      // hide console when executing notebook chunks
      final CheckBox notebookHideConsole = checkboxPref(
            "Hide console automatically when executing " +
            "notebook chunks",
            prefs_.hideConsoleOnChunkExecute());
      basic.add(notebookHideConsole);
      
      basic.add(spacedBefore(new HelpLink("Using R Notebooks", "using_notebooks")));
      
      VerticalTabPanel advanced = new VerticalTabPanel(ElementIds.RMARKDOWN_ADVANCED_PREFS);
      advanced.add(headerLabel("Display"));
      advanced.add(checkboxPref("Enable chunk background highlight", prefs_.highlightCodeChunks()));
      advanced.add(checkboxPref("Show inline toolbar for R code chunks", prefs_.showInlineToolbarForRCodeChunks()));
      final CheckBox showRmdRenderCommand = checkboxPref( "Display render command in R Markdown tab",
            prefs_.showRmdRenderCommand());
      advanced.add(showRmdRenderCommand);
      
      
      VerticalTabPanel visualMode = new VerticalTabPanel(ElementIds.RMARKDOWN_ADVANCED_PREFS);   
      CheckBox enableVisualMarkdownEditor = checkboxPref(
            "Enable visual markdown editing",
            prefs_.enableVisualMarkdownEditingMode());
      spaced(enableVisualMarkdownEditor);
      visualMode.add(enableVisualMarkdownEditor);
      
      Label visualMarkdownLabel = new Label(
            "Visual markdown editing is an experimental feature of " +
            "RStudio v1.4. Please click the link below for more details " +
            "before enabling this feature.");
           
      visualMarkdownLabel.addStyleName(baseRes.styles().infoLabel());
      nudgeRight(visualMarkdownLabel);
      spaced(visualMarkdownLabel);
      visualMode.add(visualMarkdownLabel);
      
      HelpLink visualModeHelpLink = new HelpLink(
            "Learn more about visual markdown editing",
            "visual_markdown_editing",
            false // no version info
      );
      nudgeRight(visualModeHelpLink); 
      mediumSpaced(visualModeHelpLink);
      visualMode.add(visualModeHelpLink);
      
      VerticalPanel visualModeOptions = new VerticalPanel();
      mediumSpaced(visualModeOptions);
      
      visualModeOptions.add(headerLabel("Display"));
      
      // show outline
      CheckBox visualEditorShowOutline = checkboxPref(
            "Show document outline by default",
            prefs_.visualMarkdownEditingShowDocOutline(),
            false);
      spaced(visualEditorShowOutline);
      visualModeOptions.add(visualEditorShowOutline);
      
      // content width
      visualModeContentWidth_ = numericPref(
            "Editor content width (pixels):", 
            100,
            NumericValueWidget.NoMaximum,
            prefs_.visualMarkdownEditingMaxContentWidth(),
            false
         );
      visualModeContentWidth_.setWidth("42px");
      visualModeContentWidth_.setLimits(100, NumericValueWidget.NoMaximum);
      spaced(visualModeContentWidth_);
      visualModeOptions.add(nudgeRightPlus(visualModeContentWidth_));
      
      // font size
      final String kDefault = "(Default)";
      String[] labels = {kDefault, "8", "9", "10", "11", "12",};
      String[] values = new String[labels.length];
      for (int i = 0; i < labels.length; i++) 
      {
         if (labels[i].equals(kDefault))
            values[i] = "0";
         else
            values[i] = Double.parseDouble(labels[i]) + "";
      }
      visualModeFontSize_ = new SelectWidget("Editor font size:", labels, values, false, true, false);
      if (!visualModeFontSize_.setValue(prefs_.visualMarkdownEditingFontSizePoints().getGlobalValue() + ""))
         visualModeFontSize_.getListBox().setSelectedIndex(0);
      visualModeOptions.add(visualModeFontSize_);
      
      
      visualModeOptions.add(headerLabel("Markdown"));
      
      // auto wrap
      CheckBox checkBoxAutoWrap = checkboxPref(
         "Auto-wrap text (break lines at specified column)", 
         prefs.visualMarkdownEditingWrapAuto(),
         false
      );
      visualModeOptions.add(checkBoxAutoWrap);
      visualModeOptions.add(indent(visualModeWrapColumn_ = numericPref(
          "Wrap column:", 1, UserPrefs.MAX_WRAP_COLUMN,
          prefs.visualMarkdownEditingWrapColumn()
      )));
      visualModeWrapColumn_.setWidth("36px");
      visualModeWrapColumn_.setEnabled(checkBoxAutoWrap.getValue());
      checkBoxAutoWrap.addValueChangeHandler((value) -> {
         visualModeWrapColumn_.setEnabled(checkBoxAutoWrap.getValue());
      });
      mediumSpaced(visualModeWrapColumn_);
      
      // references
      String[] referencesValues = {
         UserPrefsAccessor.VISUAL_MARKDOWN_EDITING_REFERENCES_LOCATION_BLOCK,
         UserPrefsAccessor.VISUAL_MARKDOWN_EDITING_REFERENCES_LOCATION_SECTION,
         UserPrefsAccessor.VISUAL_MARKDOWN_EDITING_REFERENCES_LOCATION_DOCUMENT
      };
      visualModeReferences_ = new SelectWidget("Write references at end of: ", referencesValues, referencesValues, false, true, false);
      if (!visualModeReferences_.setValue(prefs_.visualMarkdownEditingReferencesLocation().getGlobalValue()))
         visualModeReferences_.getListBox().setSelectedIndex(0);
      mediumSpaced(visualModeReferences_);
      visualModeOptions.add(visualModeReferences_);
      
      // help on per-file markdown options
      HelpLink markdownPerFileOptions = new HelpLink(
            "Setting markdown options on a per-file basis",
            "visual_markdown_editing-file-options",
            false // no version info
      );
      nudgeRight(markdownPerFileOptions); 
      mediumSpaced(markdownPerFileOptions);
      visualModeOptions.add(markdownPerFileOptions);
      
      
      visualMode.add(visualModeOptions);
      Runnable manageVisualModeUI = () -> {
         visualModeOptions.setVisible(enableVisualMarkdownEditor.getValue());
      };
      manageVisualModeUI.run();
      enableVisualMarkdownEditor.addValueChangeHandler((value) -> {
         manageVisualModeUI.run();
      });
       
      
      DialogTabLayoutPanel tabPanel = new DialogTabLayoutPanel("R Markdown");
      tabPanel.setSize("435px", "498px");
      tabPanel.add(basic, "Basic", basic.getBasePanelId());
      tabPanel.add(advanced, "Advanced", advanced.getBasePanelId());
      tabPanel.add(visualMode, "Visual", visualMode.getBasePanelId());
      tabPanel.selectTab(0);
      add(tabPanel);
   }

   @Override
   public ImageResource getIcon()
   {
      return new ImageResource2x(res_.iconRMarkdown2x());
   }

   @Override
   public boolean validate()
   {
      return visualModeWrapColumn_.validate() &&
             visualModeContentWidth_.validate();
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
   public RestartRequirement onApply(UserPrefs rPrefs)
   {
      RestartRequirement restartRequirement = super.onApply(rPrefs);
      
      prefs_.docOutlineShow().setGlobalValue(
            docOutlineDisplay_.getValue());
      
      prefs_.rmdViewerType().setGlobalValue(
            rmdViewerMode_.getValue());
      
      prefs_.latexPreviewOnCursorIdle().setGlobalValue(
            latexPreviewWidget_.getValue());
      
      prefs_.visualMarkdownEditingFontSizePoints().setGlobalValue(
            Integer.parseInt(visualModeFontSize_.getValue())); 
      
      prefs_.visualMarkdownEditingReferencesLocation().setGlobalValue(
            visualModeReferences_.getValue());
      
      if (knitWorkingDir_ != null)
      {
         prefs_.knitWorkingDir().setGlobalValue(
               knitWorkingDir_.getValue());
      }
      
      return restartRequirement;
   }

   private final UserPrefs prefs_;
   
   private final PreferencesDialogResources res_;
   
   private final SelectWidget rmdViewerMode_;
   private final SelectWidget docOutlineDisplay_;
   private final SelectWidget latexPreviewWidget_;
   private final SelectWidget knitWorkingDir_;
   
   private final SelectWidget visualModeFontSize_;
   private final NumericValueWidget visualModeContentWidth_;
   private final NumericValueWidget visualModeWrapColumn_;
   private final SelectWidget visualModeReferences_;   
}
