/*
 * VisualModeConfirm.java
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


package org.rstudio.studio.client.workbench.views.source.editors.text.visualmode;

import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.panmirror.PanmirrorSetMarkdownResult;
import org.rstudio.studio.client.projects.model.ProjectsServerOperations;
import org.rstudio.studio.client.projects.model.RProjectConfig;
import org.rstudio.studio.client.projects.model.RProjectOptions;
import org.rstudio.studio.client.rmarkdown.model.RmdEditorOptions;
import org.rstudio.studio.client.rmarkdown.model.YamlFrontMatter;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsAccessor;
import org.rstudio.studio.client.workbench.prefs.model.UserState;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.visualmode.dialogs.VisualModeConfirmDialog;
import org.rstudio.studio.client.workbench.views.source.editors.text.visualmode.dialogs.VisualModeLineWrappingDialog;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

public class VisualModeConfirm
{ 
   public interface Context
   {
      String getYamlFrontMatter();
      boolean applyYamlFrontMatter(String yaml);
   }
   
   public VisualModeConfirm(DocUpdateSentinel docUpdateSentinel, DocDisplay docDisplay, Context context)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      docUpdateSentinel_ = docUpdateSentinel;
      docDisplay_ = docDisplay; 
      context_ = context;
   }
   
   @Inject
   void initialize(WorkbenchContext workbenchContext, UserPrefs prefs, UserState state, ProjectsServerOperations server)
   {
      workbenchContext_ = workbenchContext;
      userPrefs_ = prefs;
      userState_ = state;
      server_= server;
   }
   
   public void onUserSwitchToVisualModePending()
   {
      userSwitchToVisualModePending_ = true;
   }
   
   public void withSwitchConfirmation(PanmirrorSetMarkdownResult result, 
                                       Command onConfirmed,
                                       Command onCancelled) 
   {
      
      // confirmation also updates state
      Operation doConfirm = () -> {    
         docUpdateSentinel_.setBoolProperty(TextEditingTarget.RMD_VISUAL_MODE_WRAP_CONFIGURED, true);
         onConfirmed.execute();
      };
      
      if (userSwitchToVisualModePending_)
      {
         userSwitchToVisualModePending_ = false;
                  
         // check if we require a line wrapping prompt and if setup an operation to do so
         Operation lineWrapCheck = requiresLineWrappingPrompt(result.line_wrapping) 
            ? ()-> {
               
               boolean isProjectDoc = VisualModeUtil.isDocInProject(workbenchContext_, docUpdateSentinel_);
               
               VisualModeLineWrappingDialog dialog = new VisualModeLineWrappingDialog(
                  result.line_wrapping,
                  userPrefs_.visualMarkdownEditingWrap().getValue(),
                  isProjectDoc && userPrefs_.visualMarkdownEditingWrap().hasProjectValue(),
                  isProjectDoc,
                  userPrefs_.visualMarkdownEditingWrapAtColumn().getGlobalValue(),
                  (lineWrappingResult) -> { 
                     
                     doConfirm.execute(); 
                     
                     // do this deferred so that the editor is fully hooked up 
                     Scheduler.get().scheduleDeferred(() -> {
                        configureLineWrapping(lineWrappingResult, result.line_wrapping); 
                     });
                 
                  }, 
                  () -> { 
                     onCancelled.execute(); 
                  }   
               );
               dialog.showModal();
            }
            : doConfirm
         ;
         
         // confirm visual mode
         if (!userPrefs_.visualMarkdownEditingIsDefault().getValue() &&
             !userState_.visualModeConfirmed().getValue())
         {
            VisualModeConfirmDialog dialog = new VisualModeConfirmDialog(
               (value) -> {
                  if (value)
                  {
                     userState_.visualModeConfirmed().setGlobalValue(true);
                     userState_.writeState(); 
                  }
                  lineWrapCheck.execute();
               }, 
               () -> { onCancelled.execute(); }
            );
            dialog.showModal();
         }
         else
         {
            lineWrapCheck.execute();
         }
      }
      else
      {
         // any non-interactive load of a doc updates state (migration)
         doConfirm.execute();
      }
   }
   
   private boolean requiresLineWrappingPrompt(String lineWrapping)
   { 
      boolean wrapConfigured = docUpdateSentinel_.getBoolProperty(
         TextEditingTarget.RMD_VISUAL_MODE_WRAP_CONFIGURED, false
      );
      if (!wrapConfigured)
      {    
         // no prompt if they already have a config set in the file
         String yaml = YamlFrontMatter.getFrontMatter(docDisplay_);
         String yamlWrap = RmdEditorOptions.getMarkdownOption(yaml,  "wrap");
         if (!yamlWrap.isEmpty())
            return false;
         
         
         // determine applicable preference (project if this file is in a project)
         String pref = VisualModeUtil.isDocInProject(workbenchContext_, docUpdateSentinel_)
             ? userPrefs_.visualMarkdownEditingWrap().getValue()
             : userPrefs_.visualMarkdownEditingWrap().getGlobalValue();
               
         // if this file has soft breaks and they differ from the current configured preference
         return !lineWrapping.equals(UserPrefsAccessor.VISUAL_MARKDOWN_EDITING_WRAP_NONE) &&
                !lineWrapping.equals(pref);
      }
      else
      {
         return false;
      }
   }
   
   private void configureLineWrapping(VisualModeLineWrappingDialog.Result result, String lineWrapping)
   {
      if (result.action == VisualModeLineWrappingDialog.Action.SetFileLineWrapping)
      {
         String value = lineWrapping == UserPrefsAccessor.VISUAL_MARKDOWN_EDITING_WRAP_COLUMN 
           ? result.column.toString()
           : UserPrefsAccessor.VISUAL_MARKDOWN_EDITING_WRAP_SENTENCE;
           
         String yaml = context_.getYamlFrontMatter();
         yaml = RmdEditorOptions.setMarkdownOption(yaml, RmdEditorOptions.MARKDOWN_WRAP_OPTION, value);
         context_.applyYamlFrontMatter(yaml);
      }
      else if (result.action == VisualModeLineWrappingDialog.Action.SetProjectLineWrapping)
      {
         server_.readProjectOptions(new SimpleRequestCallback<RProjectOptions>() {
            @Override
            public void onResponseReceived(RProjectOptions options)
            {
               RProjectConfig config = options.getConfig();
            
               if (lineWrapping == UserPrefsAccessor.VISUAL_MARKDOWN_EDITING_WRAP_COLUMN)
               {
                  config.setMarkdownWrap(RProjectConfig.MARKDOWN_WRAP_COLUMN);
                  config.setMarkdownWrapAtColumn(result.column);
                  userPrefs_.visualMarkdownEditingWrap().setProjectValue(lineWrapping);
                  userPrefs_.visualMarkdownEditingWrapAtColumn().setProjectValue(result.column);
               }
               else if (lineWrapping == UserPrefsAccessor.VISUAL_MARKDOWN_EDITING_WRAP_SENTENCE)
               {
                  config.setMarkdownWrap(RProjectConfig.MARKDOWN_WRAP_SENTENCE);
                  userPrefs_.visualMarkdownEditingWrap().setProjectValue(lineWrapping);
               }
               server_.writeProjectConfig(config, new VoidServerRequestCallback());
            }
         });
      }
   }
   
   private final VisualModeConfirm.Context context_;
   private final DocUpdateSentinel docUpdateSentinel_;
   private final DocDisplay docDisplay_;
   
   private WorkbenchContext workbenchContext_;
   private UserPrefs userPrefs_;
   private UserState userState_;
   private ProjectsServerOperations server_;
   
   private boolean userSwitchToVisualModePending_ = false;
  
}


