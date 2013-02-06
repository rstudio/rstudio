/*
 * ProjectGeneralPreferencesPane.java
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
package org.rstudio.studio.client.projects.ui.prefs;

import org.rstudio.core.client.prefs.PreferencesDialogBaseResources;
import org.rstudio.studio.client.projects.model.RProjectConfig;
import org.rstudio.studio.client.projects.model.RProjectOptions;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.inject.Inject;

public class ProjectGeneralPreferencesPane extends ProjectPreferencesPane
{
   @Inject
   public ProjectGeneralPreferencesPane()
   {        
      Grid grid = new Grid(4, 2);
      grid.addStyleName(RESOURCES.styles().workspaceGrid());
      grid.setCellSpacing(8);
      
      Label infoLabel = new Label("Use (Default) to inherit the global default setting");
      infoLabel.addStyleName(PreferencesDialogBaseResources.INSTANCE.styles().infoLabel());
      grid.setWidget(0, 0, infoLabel);
      
      // restore workspace
      grid.setWidget(1, 0, new Label("Restore .RData into workspace at startup"));
      grid.setWidget(1, 1, restoreWorkspace_ = new YesNoAskDefault(false));
     
      // save workspace      
      grid.setWidget(2, 0, new Label("Save workspace to .RData on exit"));
      grid.setWidget(2, 1, saveWorkspace_ = new YesNoAskDefault(true));

      // always save history
      grid.setWidget(3, 0, new Label("Always save history (even if not saving .RData)"));
      grid.setWidget(3, 1, alwaysSaveHistory_ = new YesNoAskDefault(false));
      
      add(grid);
      
      
      // add presentation option
      chkShowPresentation_ = new CheckBox(
                           "Always show presentation when opening project");
      chkShowPresentation_.addStyleName(RESOURCES.styles().showPresentationCheckBox());
      chkShowPresentation_.setVisible(false);
      add(chkShowPresentation_);
      
      
      
   }

   @Override
   public ImageResource getIcon()
   {
      return PreferencesDialogBaseResources.INSTANCE.iconR();
   }

   @Override
   public String getName()
   {
      return "General";
   }

   @Override
   protected void initialize(RProjectOptions options)
   {
      RProjectConfig config = options.getConfig();
      restoreWorkspace_.setSelectedValue(config.getRestoreWorkspace());
      saveWorkspace_.setSelectedValue(config.getSaveWorkspace());
      alwaysSaveHistory_.setSelectedValue(config.getAlwaysSaveHistory());
      
      chkShowPresentation_.setValue(config.getShowPresentation());
      chkShowPresentation_.setVisible(options.getHasPresentation());
   }

   @Override
   public boolean onApply(RProjectOptions options)
   {
      RProjectConfig config = options.getConfig();
      config.setRestoreWorkspace(restoreWorkspace_.getSelectedValue());
      config.setSaveWorkspace(saveWorkspace_.getSelectedValue());
      config.setAlwaysSaveHistory(alwaysSaveHistory_.getSelectedValue());
      config.setShowPresentation(chkShowPresentation_.getValue());
      return false;
   }
   
   private class YesNoAskDefault extends ListBox
   {
      public YesNoAskDefault(boolean includeAsk)
      {
         super(false);
         
         String[] items = includeAsk ? new String[] {USE_DEFAULT, YES, NO, ASK}:
                                       new String[] {USE_DEFAULT, YES, NO};
         
         for (int i=0; i<items.length; i++)
            addItem(items[i]);
      }
      
      public void setSelectedValue(int value)
      {
         if (value < getItemCount())
            setSelectedIndex(value);
         else
            setSelectedIndex(0);
      }
      
      public int getSelectedValue()
      {
         return getSelectedIndex();
      }
   }
   
   private static final String USE_DEFAULT = "(Default)";
   private static final String YES = "Yes";
   private static final String NO = "No";
   private static final String ASK ="Ask";
   
   private YesNoAskDefault restoreWorkspace_;
   private YesNoAskDefault saveWorkspace_;
   private YesNoAskDefault alwaysSaveHistory_;
   
   private CheckBox chkShowPresentation_;
   
}
