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

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.panmirror.PanmirrorSetMarkdownResult;
import org.rstudio.studio.client.rmarkdown.model.RmdEditorOptions;
import org.rstudio.studio.client.rmarkdown.model.YamlFrontMatter;
import org.rstudio.studio.client.workbench.WorkbenchContext;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsAccessor;
import org.rstudio.studio.client.workbench.prefs.model.UserState;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.editors.text.visualmode.dialogs.VisualModeConfirmDialog;
import org.rstudio.studio.client.workbench.views.source.editors.text.visualmode.dialogs.VisualModeConfirmLineWrappingDialog;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;

import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

// TODO: provide way to set the column

public class VisualModeConfirm
{ 
   public enum LineWrappingAction
   {
      SetFileLineWrapping,
      SetProjectLineWrapping,
      SetNothing
   }
   
   
   public VisualModeConfirm(DocUpdateSentinel docUpdateSentinel, DocDisplay docDisplay)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      docUpdateSentinel_ = docUpdateSentinel;
      docDisplay_ = docDisplay; 
   }
   
   @Inject
   void initialize(WorkbenchContext workbenchContext, UserPrefs prefs, UserState state)
   {
      workbenchContext_ = workbenchContext;
      userPrefs_ = prefs;
      userState_ = state;
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
               
               VisualModeConfirmLineWrappingDialog dialog = new VisualModeConfirmLineWrappingDialog(
                  result.line_wrapping,
                  userPrefs_.visualMarkdownEditingWrap().getValue(),
                  userPrefs_.visualMarkdownEditingWrap().hasProjectValue(),
                  VisualModeUtil.isDocInProject(workbenchContext_, docUpdateSentinel_),
                  (action) -> { 
                     doConfirm.execute(); 
                     performLineWrappingAction(action); 
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
   
   private void performLineWrappingAction(LineWrappingAction action)
   {
      Debug.logToRConsole(action.toString());
   }
   
   private final DocUpdateSentinel docUpdateSentinel_;
   private final DocDisplay docDisplay_;
   
   private WorkbenchContext workbenchContext_;
   private UserPrefs userPrefs_;
   private UserState userState_;
   
   private boolean userSwitchToVisualModePending_ = false;
  
}


