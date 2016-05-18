/*
 * RMarkdownPreferencesPane.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.inject.Inject;

import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.studio.client.rmarkdown.RmdOutput;
import org.rstudio.studio.client.workbench.prefs.model.RPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefsAccessor;

public class RMarkdownPreferencesPane extends PreferencesPane
{
   @Inject
   public RMarkdownPreferencesPane(UIPrefs prefs,
                                   PreferencesDialogResources res)
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

  
      // Short term option to enable notebooks (this will eventually
      // go away). This option has only three functions right now:
      //
      //  1) It controls whether the "New -> R Notebook" command is visible
      //  2) It controls whether the other notebook options are visible
      //     within the preferences pane.
      //  3) The act of checking it automatically checks the the 
      //     showRmdChunkOutputInline option within the UI.
      //
      enableNotebooks_ = checkboxPref(
            "Enable R notebook", prefs_.enableRNotebooks());
      add(enableNotebooks_);
      
      // show output inline for all Rmds
      final CheckBox rmdInlineOutput = checkboxPref(
            "Show output inline for all R Markdown documents",
            prefs_.showRmdChunkOutputInline());
      add(rmdInlineOutput);
      
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
      
      // manage visibility of notebook options
      final Command updateNotebookOptionVisibility = new Command() {
         @Override
         public void execute()
         {
            rmdInlineOutput.setVisible(enableNotebooks_.getValue());
            autoExecuteSetupChunk.setVisible(enableNotebooks_.getValue());
            notebookHideConsole.setVisible(enableNotebooks_.getValue());
         } 
      };
      updateNotebookOptionVisibility.execute();
      
      // manage visibility and default rmdInlineoutput when 
      // enable notebooks is checked
      enableNotebooks_.addValueChangeHandler(new ValueChangeHandler<Boolean>() {

         @Override
         public void onValueChange(ValueChangeEvent<Boolean> event)
         {
            // manage visibility of other options
            updateNotebookOptionVisibility.execute();
            
            // default to inline output when activating notebook mode
            if (event.getValue())
               rmdInlineOutput.setValue(true);
         } 
      });
   }

   @Override
   public ImageResource getIcon()
   {
      return res_.iconRMarkdown();
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
      initialEnableNotebooksPref_ = prefs_.enableRNotebooks().getValue();
   }
   
   @Override
   public boolean onApply(RPrefs rPrefs)
   {
      boolean requiresRestart = super.onApply(rPrefs);
      
      prefs_.shownSectionsInDocumentOutline().setGlobalValue(
            docOutlineDisplay_.getValue());
      
      prefs_.rmdViewerType().setGlobalValue(Integer.decode(
            rmdViewerMode_.getValue()));
      
      boolean enableNotebooksChanged = initialEnableNotebooksPref_ !=
                                       enableNotebooks_.getValue();
            
      return requiresRestart || enableNotebooksChanged;
   }

   private final UIPrefs prefs_;
   
   private final PreferencesDialogResources res_;
   
   private final SelectWidget rmdViewerMode_;
   private final SelectWidget docOutlineDisplay_;
   
   private final CheckBox enableNotebooks_;
   private boolean initialEnableNotebooksPref_;
}
