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

import org.rstudio.core.client.MessageDisplay;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.panmirror.PanmirrorSetMarkdownResult;
import org.rstudio.studio.client.rmarkdown.model.RmdEditorOptions;
import org.rstudio.studio.client.rmarkdown.model.YamlFrontMatter;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsAccessor;
import org.rstudio.studio.client.workbench.prefs.model.UserState;
import org.rstudio.studio.client.workbench.views.source.editors.text.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;

import com.google.gwt.user.client.Command;
import com.google.inject.Inject;


// TOOD: test the various state permutations

public class VisualModeConfirm
{
   public VisualModeConfirm(DocUpdateSentinel docUpdateSentinel, DocDisplay docDisplay)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      docUpdateSentinel_ = docUpdateSentinel;
      docDisplay_ = docDisplay; 
   }
   
   @Inject
   void initialize(GlobalDisplay globalDisplay, UserPrefs prefs, UserState state)
   {
      globalDisplay_ = globalDisplay;
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
         userState_.visualModeConfirmed().setGlobalValue(true);
         docUpdateSentinel_.setBoolProperty(TextEditingTarget.RMD_VISUAL_MODE_WRAP_CONFIGURED, true);
         onConfirmed.execute();
      };
      
   
      if (userSwitchToVisualModePending_)
      {
         userSwitchToVisualModePending_ = false;
                  
         if (requiresLineWrappingPrompt(result.line_wrapping))
         {
            globalDisplay_.showYesNoMessage(
               MessageDisplay.MSG_QUESTION,
               "Line Wrapping", 
               "Whoa their pardner, fixup that line wrapping?", 
               false, 
               doConfirm, 
               () -> { 
                  onCancelled.execute(); 
               }, 
               null,
               "Fix", "Don't Fix", true
               );
         }
         else if (!userState_.visualModeConfirmed().getValue() &&
                  !hasBeenEditedInVisualMode())
         {
            globalDisplay_.showYesNoMessage(
               MessageDisplay.MSG_QUESTION,
               "Visual Mode", 
               "Are you sure you want to switch to Visual Mode?", 
               false, 
               doConfirm, 
               () -> { onCancelled.execute(); }, 
               null,
               "Switch", "Don't Switch", true
            );
         }
         else
         {
            doConfirm.execute();
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
         
         // if this file has soft breaks and they differ from the current configured preference
         return !lineWrapping.equals(UserPrefsAccessor.VISUAL_MARKDOWN_EDITING_WRAP_NONE) &&
                !lineWrapping.equals(userPrefs_.visualMarkdownEditingWrap().getValue());
      }
      else
      {
         return false;
      }
   }
   
   private boolean hasBeenEditedInVisualMode()
   {
      return docUpdateSentinel_.hasProperty(TextEditingTarget.RMD_VISUAL_MODE_WRAP_CONFIGURED);
   }
   
   private final DocUpdateSentinel docUpdateSentinel_;
   private final DocDisplay docDisplay_;
   
   private GlobalDisplay globalDisplay_;
   private UserPrefs userPrefs_;
   private UserState userState_;
   
   private boolean userSwitchToVisualModePending_ = false;
  
}


