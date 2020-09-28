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

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.inject.Inject;

import elemental2.core.JsObject;
import jsinterop.base.Js;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.prefs.RestartRequirement;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.DialogTabLayoutPanel;
import org.rstudio.core.client.theme.VerticalTabPanel;
import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.HelpButton;
import org.rstudio.core.client.widget.NumericValueWidget;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.common.FileDialogs;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.panmirror.server.PanmirrorZoteroLocalConfig;
import org.rstudio.studio.client.panmirror.server.PanmirrorZoteroServerOperations;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.model.RemoteFileSystemContext;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsAccessor;
import org.rstudio.studio.client.workbench.prefs.model.UserState;
import org.rstudio.studio.client.workbench.prefs.model.UserStateAccessor;
import org.rstudio.studio.client.workbench.prefs.views.zotero.ZoteroApiKeyWidget;
import org.rstudio.studio.client.workbench.prefs.views.zotero.ZoteroConnectionWidget;
import org.rstudio.studio.client.workbench.prefs.views.zotero.ZoteroLibrariesWidget;

public class RMarkdownPreferencesPane extends PreferencesPane
{
   @Inject
   public RMarkdownPreferencesPane(UserPrefs prefs,
                                   UserState state,
                                   PreferencesDialogResources res,
                                   Session session,
                                   RemoteFileSystemContext fsContext,
                                   FileDialogs fileDialogs,
                                   PanmirrorZoteroServerOperations zoteroServer)
   {
      prefs_ = prefs;
      state_ = state;
      res_ = res;
      
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

      VerticalTabPanel visualMode = new VerticalTabPanel(ElementIds.RMARKDOWN_VISUAL_MODE_PREFS);

      visualMode.add(headerLabel("General"));
        
      CheckBox visualMarkdownIsDefault = checkboxPref(
            "Use visual editor by default for new documents",
            prefs_.visualMarkdownEditingIsDefault());
      visualMarkdownIsDefault.getElement().getStyle().setMarginBottom(10, Unit.PX);
      visualMode.add(visualMarkdownIsDefault);
      
      HelpLink visualModeHelpLink = new HelpLink(
         "Learn more about visual editing mode",
         "visual_markdown_editing",
         false // no version info
      );
      nudgeRight(visualModeHelpLink);
      spaced(visualModeHelpLink);
      visualMode.add(visualModeHelpLink);
      

      visualMode.add(headerLabel("Display"));
      
      
      VerticalPanel visualModeOptions = new VerticalPanel();
      
      
      
      // show outline
      CheckBox visualEditorShowOutline = checkboxPref(
            "Show document outline by default",
            prefs_.visualMarkdownEditingShowDocOutline(),
            false);
      lessSpaced(visualEditorShowOutline);
      visualModeOptions.add(visualEditorShowOutline);
      
      // show margin
      CheckBox visualEditorShowMargin = checkboxPref(
            "Show margin column indicator in code blocks",
            prefs_.visualMarkdownEditingShowMargin(),
            false);
      lessSpaced(visualEditorShowMargin);
      visualModeOptions.add(visualEditorShowMargin);
      

      // content width
      visualModeContentWidth_ = numericPref(
            "Editor content width (px):",
            100,
            NumericValueWidget.NoMaximum,
            prefs_.visualMarkdownEditingMaxContentWidth(),
            false
         );
      visualModeContentWidth_.setWidth("42px");
      visualModeContentWidth_.setLimits(100, NumericValueWidget.NoMaximum);
      lessSpaced(visualModeContentWidth_);
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
      visualModeFontSize_.getElement().getStyle().setMarginTop(3, Unit.PX);
      visualModeFontSize_.getElement().getStyle().setMarginBottom(15, Unit.PX);
      visualModeOptions.add(visualModeFontSize_);

     
      visualModeOptions.add(headerLabel("Markdown"));

      
      // list spacing
      String[] listSpacingValues = {
         UserPrefsAccessor.VISUAL_MARKDOWN_EDITING_LIST_SPACING_TIGHT,
         UserPrefsAccessor.VISUAL_MARKDOWN_EDITING_LIST_SPACING_SPACED
      };
      visualModeListSpacing_ = new SelectWidget("Default spacing between list items: ", 
            listSpacingValues, listSpacingValues, 
            false, true, false);
      visualModeListSpacing_.getElement().getStyle().setMarginBottom(10, Unit.PX);
      if (!visualModeListSpacing_.setValue(prefs_.visualMarkdownEditingListSpacing().getGlobalValue()))
         visualModeListSpacing_.getListBox().setSelectedIndex(0);
      visualModeOptions.add(visualModeListSpacing_);
      
      // auto wrap
      String[] wrapValues = {
         UserPrefsAccessor.VISUAL_MARKDOWN_EDITING_WRAP_NONE,
         UserPrefsAccessor.VISUAL_MARKDOWN_EDITING_WRAP_COLUMN,
         UserPrefsAccessor.VISUAL_MARKDOWN_EDITING_WRAP_SENTENCE
      };
      visualModeWrap_ = new SelectWidget("Automatic text wrapping (line breaks): ", wrapValues, wrapValues, false, true, false);
      if (!visualModeWrap_.setValue(prefs_.visualMarkdownEditingWrap().getGlobalValue()))
         visualModeWrap_.getListBox().setSelectedIndex(0);
      HelpButton.addHelpButton(visualModeWrap_, "visual_markdown_editing-line-wrapping", "Learn more about automatic line wrapping", 0);
      visualModeWrap_.addStyleName(res.styles().visualModeWrapSelectWidget());
      visualModeOptions.add(visualModeWrap_);
      
      visualModeOptions.add(indent(visualModeWrapColumn_ = numericPref(
          "Wrap at column:", 1, UserPrefs.MAX_WRAP_COLUMN,
          prefs.visualMarkdownEditingWrapAtColumn()
      )));
      visualModeWrapColumn_.getElement().getStyle().setMarginBottom(8, Unit.PX);
      visualModeWrapColumn_.setWidth("36px");
      Command manageWrapColumn = () -> {
         boolean wrapAtColumn = visualModeWrap_.getValue().equals(UserPrefsAccessor.VISUAL_MARKDOWN_EDITING_WRAP_COLUMN);
         visualModeWrapColumn_.setVisible(wrapAtColumn);
         visualModeWrap_.getElement().getStyle().setMarginBottom(wrapAtColumn ? 2 : 8, Unit.PX);
      };
      manageWrapColumn.execute();
      visualModeWrap_.addChangeHandler((arg) -> {
         manageWrapColumn.execute();
      });
      lessSpaced(visualModeWrapColumn_);

      // references
      String[] referencesValues = {
         UserPrefsAccessor.VISUAL_MARKDOWN_EDITING_REFERENCES_LOCATION_BLOCK,
         UserPrefsAccessor.VISUAL_MARKDOWN_EDITING_REFERENCES_LOCATION_SECTION,
         UserPrefsAccessor.VISUAL_MARKDOWN_EDITING_REFERENCES_LOCATION_DOCUMENT
      };
      visualModeReferences_ = new SelectWidget("Write references at end of current: ", referencesValues, referencesValues, false, true, false);
      if (!visualModeReferences_.setValue(prefs_.visualMarkdownEditingReferencesLocation().getGlobalValue()))
         visualModeReferences_.getListBox().setSelectedIndex(0);
      spaced(visualModeReferences_);
      visualModeOptions.add(visualModeReferences_);

      // canonical mode
      CheckBox visualModeCanonical = checkboxPref(
            "Write canonical visual mode markdown in source mode",
            prefs_.visualMarkdownEditingCanonical(),
            false);
      spaced(visualModeCanonical);
      visualModeOptions.add(visualModeCanonical);
      
      // help on per-file markdown options
      HelpLink markdownPerFileOptions = new HelpLink(
         "Learn more about markdown writer options",
         "visual_markdown_editing-writer-options",
         false // no version info
      );
      nudgeRight(markdownPerFileOptions);
      spaced(markdownPerFileOptions);
      visualModeOptions.add(markdownPerFileOptions);
      
      visualMode.add(visualModeOptions);
      
      VerticalTabPanel citations = new VerticalTabPanel(ElementIds.RMARKDOWN_CITATIONS_PREFS);
 
      Label citationsLabel = new Label("Citation features are available within R Markdown visual mode.");
      spaced(citationsLabel);
      citations.add(citationsLabel);
      
      // help on per-file markdown options
      HelpLink citationsHelpLink = new HelpLink(
         "Learn more about using citations with visual editing mode",
         "visual_markdown_editing-citations",
         false // no version info
      );
      spaced(citationsHelpLink);
      citations.add(citationsHelpLink);
      
      citations.add(headerLabel("Zotero"));
      
      zoteroConnection_ = new ZoteroConnectionWidget(res, false);
      spaced(zoteroConnection_);
      citations.add(zoteroConnection_);
     
      zoteroApiKey_ = new ZoteroApiKeyWidget(zoteroServer, "240px");
      zoteroApiKey_.getElement().getStyle().setMarginLeft(4, Unit.PX);
      spaced(zoteroApiKey_);
      zoteroApiKey_.setKey(state_.zoteroApiKey().getValue());
      citations.add(zoteroApiKey_);
      
      zoteroDataDir_ = new DirectoryChooserTextBox(
         "Zotero Data Directory:",
         "(None Detected)",
         ElementIds.TextBoxButtonId.ZOTERO_DATA_DIRECTORY,
         null,
         fileDialogs,
         fsContext
      );
      spaced(zoteroDataDir_);
      nudgeRight(zoteroDataDir_);
      textBoxWithChooser(zoteroDataDir_);
      zoteroDataDir_.getTextBox().addStyleName(res.styles().smallerText());
      String dataDir = state_.zoteroDataDir().getValue();
      if (!dataDir.isEmpty())
         zoteroDataDir_.setText(dataDir);
      citations.add(zoteroDataDir_);
      
      
      zoteroLibs_ = new ZoteroLibrariesWidget(zoteroServer);
      lessSpaced(zoteroLibs_);
      citations.add(zoteroLibs_);
      
      zoteroUseBetterBibtex_ = checkboxPref(
         "Use Better BibTeX for citation keys and BibTeX export",
         state_.zoteroUseBetterBibtex(),
         false);
      spaced(zoteroUseBetterBibtex_);
      citations.add(zoteroUseBetterBibtex_);
       
      // kickoff query for detected zotero data directory
      zoteroServer.zoteroDetectLocalConfig(new ServerRequestCallback<JsObject>() {    
         
         @Override
         public void onResponseReceived(JsObject response)
         {
            zoteroLocalConfig_ = Js.uncheckedCast(response);
            
            if (zoteroDataDir_.getText().isEmpty())
               zoteroDataDir_.setText(zoteroLocalConfig_.dataDirectory);
            
            // resolve 'auto'
            String connectionType = state_.zoteroConnectionType().getValue();
            zoteroIsAuto_ = connectionType.equals(UserStateAccessor.ZOTERO_CONNECTION_TYPE_AUTO);
            if (zoteroIsAuto_)
            {
               if (!zoteroDataDir_.getText().isEmpty())
                  connectionType = UserStateAccessor.ZOTERO_CONNECTION_TYPE_LOCAL;
               else
                  connectionType = UserStateAccessor.ZOTERO_CONNECTION_TYPE_NONE;
            }
            zoteroConnection_.setType(connectionType);
            
            // manage ui. if the user ever interact with the control then 'auto' is gone
            manageZoteroUI(true);
            zoteroConnection_.addChangeHandler((event) -> { 
               zoteroIsAuto_ = false;
               // connection type change invalidates libraries (as they were
               // retrived from the previous connection). default back to
               // 'My Library'
               zoteroLibs_.setMyLibrary();
               manageZoteroUI(false); 
            });
         }
         
         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
         }
         
      });
      
   
      DialogTabLayoutPanel tabPanel = new DialogTabLayoutPanel("R Markdown");
      tabPanel.setSize("435px", "533px");
      tabPanel.add(basic, "Basic", basic.getBasePanelId());
      tabPanel.add(advanced, "Advanced", advanced.getBasePanelId());
      tabPanel.add(visualMode, "Visual", visualMode.getBasePanelId());
      tabPanel.add(citations, "Citations", citations.getBasePanelId());
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
             visualModeContentWidth_.validate() &&
             zoteroLibs_.validate();
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
      
      visualModeWrap_.setValue(prefs.visualMarkdownEditingWrap().getGlobalValue());
      visualModeReferences_.setValue(prefs.visualMarkdownEditingReferencesLocation().getGlobalValue());
        
      zoteroApiKey_.setProgressIndicator(getProgressIndicator());
      
      zoteroLibs_.setLibraries(prefs_.zoteroLibraries().getValue());
      zoteroLibs_.addAvailableLibraries();
 
   }
   
   private void manageZoteroUI(boolean showLibs)
   {
      zoteroApiKey_.setVisible(zoteroConnection_.getType().equals(UserStateAccessor.ZOTERO_CONNECTION_TYPE_WEB));
      zoteroDataDir_.setVisible(zoteroConnection_.getType().equals(UserStateAccessor.ZOTERO_CONNECTION_TYPE_LOCAL));
      zoteroLibs_.setVisible(showLibs && !zoteroConnection_.getType().equals(UserStateAccessor.ZOTERO_CONNECTION_TYPE_NONE));
      zoteroUseBetterBibtex_.setVisible(zoteroDataDir_.isVisible() && zoteroLocalConfig_.betterBibtex);
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
      
      prefs_.visualMarkdownEditingListSpacing().setGlobalValue(
            visualModeListSpacing_.getValue());

      prefs_.visualMarkdownEditingWrap().setGlobalValue(
            visualModeWrap_.getValue());
      
      prefs_.visualMarkdownEditingReferencesLocation().setGlobalValue(
            visualModeReferences_.getValue());

      if (knitWorkingDir_ != null)
      {
         prefs_.knitWorkingDir().setGlobalValue(
               knitWorkingDir_.getValue());
      }
      
      if (zoteroIsAuto_)
      {
         state_.zoteroConnectionType().setGlobalValue(UserStateAccessor.ZOTERO_CONNECTION_TYPE_AUTO);
      }
      else if (zoteroConnection_.getType().equals(UserStateAccessor.ZOTERO_CONNECTION_TYPE_LOCAL) &&
               zoteroDataDir_.getText().isEmpty())
      {
         // not a valid set
      }
      else if (zoteroConnection_.getType().equals(UserStateAccessor.ZOTERO_CONNECTION_TYPE_WEB) &&
               zoteroApiKey_.getKey().isEmpty())
      {
         // not a valid set
      }
      else
      {
         state_.zoteroConnectionType().setGlobalValue(zoteroConnection_.getType());
      }
      
      prefs_.zoteroLibraries().setGlobalValue(zoteroLibs_.getLibraries());
      
      // if the zotero data dir is same as the detected data dir then 
      // set it to empty (allowing the server to always get the right default)
      if (zoteroDataDir_.getText().equals(zoteroLocalConfig_.dataDirectory))
         state_.zoteroDataDir().setGlobalValue("");
      else
         state_.zoteroDataDir().setGlobalValue(zoteroDataDir_.getText());
      
      state_.zoteroApiKey().setGlobalValue(zoteroApiKey_.getKey());
      
      return restartRequirement;
   }

   private final UserPrefs prefs_;
   private final UserState state_;
   
   private final PreferencesDialogResources res_;

   private final SelectWidget rmdViewerMode_;
   private final SelectWidget docOutlineDisplay_;
   private final SelectWidget latexPreviewWidget_;
   private final SelectWidget knitWorkingDir_;

   private final SelectWidget visualModeFontSize_;
   private final NumericValueWidget visualModeContentWidth_;
   private final NumericValueWidget visualModeWrapColumn_;
   private final SelectWidget visualModeListSpacing_;
   private final SelectWidget visualModeWrap_;
   private final SelectWidget visualModeReferences_;   
   
   private final ZoteroConnectionWidget zoteroConnection_;
   private final DirectoryChooserTextBox zoteroDataDir_;
   private final ZoteroApiKeyWidget zoteroApiKey_;
   private final ZoteroLibrariesWidget zoteroLibs_;
   private final CheckBox zoteroUseBetterBibtex_;
   private PanmirrorZoteroLocalConfig zoteroLocalConfig_ = new PanmirrorZoteroLocalConfig();
   private boolean zoteroIsAuto_ = false;
}
