/*
 * SparkMasterChooser.java
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

package org.rstudio.studio.client.workbench.views.connections.ui;

import java.util.List;

import org.rstudio.core.client.MessageDisplay.PromptWithOptionResult;
import org.rstudio.core.client.widget.CanFocus;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.views.connections.model.NewSparkConnectionContext;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.inject.Inject;

public class SparkMasterChooser extends Composite 
                                implements CanFocus,
                                SelectionChangeEvent.HasSelectionChangedHandlers
{
   public SparkMasterChooser(final NewSparkConnectionContext context)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
     
      panel_ = new SimplePanel();
      
      listBox_ = new ListBox();
      listBox_.setWidth("100%");
      
      // support local connections if possible
      if (context.getLocalConnectionsSupported())
         listBox_.addItem(LOCAL, LOCAL_VALUE);
      
      // support cluster connections on RStudio Server
      if (!Desktop.isDesktop())
      {
         // add remote servers if cluster connections are supported
         if (context.getClusterConnectionsSupported())
         {
            List<String> clusterServers = context.getClusterServers();
            for (int i = 0; i<clusterServers.size(); i++)
               listBox_.addItem(clusterServers.get(i));
            
            if (context.getClusterConnectionsEnabled())
            {
               // if list box is empty then add the default cluster URL
               if (listBox_.getItemCount() == 0)
                  listBox_.addItem(context.getDefaultClusterUrl());
            }
         }
      }   
      
      // add cluster options if cluster connections are enabled
      // (will message for states where we can't connect, e.g. 
      // on the desktop or when there is no SPARK_HOME
      if (context.getClusterConnectionsEnabled())
         listBox_.addItem(CLUSTER);
         
      // track last selected
      lastListBoxSelectedIndex_ = listBox_.getSelectedIndex();
      
      // prompt for cluster
      listBox_.addChangeHandler(new ChangeHandler() {

         @Override
         public void onChange(ChangeEvent event)
         {
            if (listBox_.getSelectedValue().equals(CLUSTER))
            {
               if (Desktop.isDesktop())
               {
                  ComponentsNotInstalledDialogs.showServerRequiredForCluster();
                  listBox_.setSelectedIndex(lastListBoxSelectedIndex_);
               }
               else if (context.getClusterConnectionsSupported())
               {
                  globalDisplay_.promptForTextWithOption(
                   "Connect to Cluster", 
                   "Spark master:", 
                   context.getDefaultClusterUrl(), 
                   false, 
                   null, 
                   false, 
                   new ProgressOperationWithInput<PromptWithOptionResult>() {

                     @Override
                     public void execute(PromptWithOptionResult input,
                                         ProgressIndicator indicator)
                     {
                        indicator.onCompleted();
                        
                        // get master
                        String master = input.input;
                        
                        // add the item to the list if necessary
                        int targetItem = -1;
                        for (int i = 0; i<listBox_.getItemCount(); i++)
                        {
                           if (listBox_.getItemText(i).equals(master))
                           {
                              targetItem = i;
                              break;
                           }
                        }
                        if (targetItem == -1)
                        {
                           int lastIndex = listBox_.getItemCount() - 1;
                           listBox_.removeItem(lastIndex);
                           listBox_.addItem(master);
                           listBox_.addItem(CLUSTER);
                           targetItem = lastIndex;
                        }
                        listBox_.setSelectedIndex(targetItem);
                        SelectionChangeEvent.fire(SparkMasterChooser.this);
                     }        
                   }, 
                   new Operation() {
                     @Override
                     public void execute()
                     {
                        listBox_.setSelectedIndex(lastListBoxSelectedIndex_);
                     }
                   });
               }
               else
               {
                  ComponentsNotInstalledDialogs.showSparkHomeNotDefined();
                  listBox_.setSelectedIndex(lastListBoxSelectedIndex_);
               }
            }
            else
            {
               SelectionChangeEvent.fire(SparkMasterChooser.this);
            }
         }
         
      });
      
      textBox_ = new TextBox();
      textBox_.setWidth("100%");
      
      panel_.setWidget(listBox_);
      
      initWidget(panel_);
   }
   
   @Inject
   void initialize(GlobalDisplay globalDisplay)
   {
      globalDisplay_ = globalDisplay;
   }
   
   @Override
   public HandlerRegistration addSelectionChangeHandler(
                                    SelectionChangeEvent.Handler handler)
   {
      return addHandler(handler, SelectionChangeEvent.getType());
   }
   
   public String getSelection()
   {
      return listBox_.getSelectedValue();
   }
   
   public void setSelection(String master)
   {
      for (int i = 0; i<listBox_.getItemCount(); i++)
      {
         if (listBox_.getItemText(i).equals(master))
            listBox_.setSelectedIndex(i);
      }
   }
   
   public boolean isLocalMasterSelected()
   {
      return isLocalMaster(getSelection());
   }
   
   public boolean isLocalMaster(String master)
   {
      return master.equals(LOCAL_VALUE);
   }

   @Override
   public void focus()
   {
      listBox_.setFocus(true);
   }
   
   private GlobalDisplay globalDisplay_;
   
   private ListBox listBox_;
   private int lastListBoxSelectedIndex_;
   private TextBox textBox_;
   private SimplePanel panel_;
   
   private final static String LOCAL = "local";
   private final static String LOCAL_VALUE = "local";
   private final static String CLUSTER = "Cluster...";
}
