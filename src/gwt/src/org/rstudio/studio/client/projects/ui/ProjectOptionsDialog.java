/*
 * ProjectOptionsDialog.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.projects.ui;

import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.projects.model.RProjectConfig;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class ProjectOptionsDialog extends ModalDialog<RProjectConfig>
{
   public ProjectOptionsDialog(
         RProjectConfig initialSettings,
         ProgressOperationWithInput<RProjectConfig> operation)
   {
      super("Project Options", operation);
      initialSettings_ = initialSettings;
   }
   

   @Override
   protected RProjectConfig collectInput()
   {
      return RProjectConfig.create(restoreWorkspace_.getSelectedValue(), 
                                   saveWorkspace_.getSelectedValue(), 
                                   alwaysSaveHistory_.getSelectedValue());
   }

   @Override
   protected boolean validate(RProjectConfig input)
   {
      return true;
   }

   @Override
   protected Widget createMainWidget()
   {
      VerticalPanel mainPanel = new VerticalPanel();
      
      Label generalLabel = new Label("Workspace and History");
      generalLabel.addStyleName(RESOURCES.styles().headerLabel());
      mainPanel.add(generalLabel);
        
      Grid grid = new Grid(4, 2);
      grid.addStyleName(RESOURCES.styles().workspaceGrid());
      grid.setCellSpacing(8);
      
      Label infoLabel = new Label("Use (Default) to inherit the global default setting");
      infoLabel.addStyleName(RESOURCES.styles().infoLabel());
      grid.setWidget(0, 0, infoLabel);
      
      // restore workspace
      grid.setWidget(1, 0, new Label("Restore .RData into workspace at startup"));
      grid.setWidget(1, 1, restoreWorkspace_ = new YesNoAskDefault(false));
      restoreWorkspace_.setSelectedValue(initialSettings_.getRestoreWorkspace());
      
      // save workspace      
      grid.setWidget(2, 0, new Label("Save workspace to .RData on exit"));
      grid.setWidget(2, 1, saveWorkspace_ = new YesNoAskDefault(true));
      saveWorkspace_.setSelectedValue(initialSettings_.getSaveWorkspace());

      // always save history
      grid.setWidget(3, 0, new Label("Always save history (even when not saving .RData)"));
      grid.setWidget(3, 1, alwaysSaveHistory_ = new YesNoAskDefault(false));
      alwaysSaveHistory_.setSelectedValue(initialSettings_.getAlwaysSaveHistory());
      
      mainPanel.add(grid);
      
      return mainPanel;
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
   
   static interface Styles extends CssResource
   {
      String headerLabel();
      String infoLabel();
      String workspaceGrid();
   }
  
   static interface Resources extends ClientBundle
   {
      @Source("ProjectOptionsDialog.css")
      Styles styles();
   }
   
   static Resources RESOURCES = (Resources)GWT.create(Resources.class) ;
   public static void ensureStylesInjected()
   {
      RESOURCES.styles().ensureInjected();
   }
   
   private static final String USE_DEFAULT = "(Default)";
   private static final String YES = "Yes";
   private static final String NO = "No";
   private static final String ASK ="Ask";
   
   
   private YesNoAskDefault restoreWorkspace_;
   private YesNoAskDefault saveWorkspace_;
   private YesNoAskDefault alwaysSaveHistory_;
   private final RProjectConfig initialSettings_;
}
