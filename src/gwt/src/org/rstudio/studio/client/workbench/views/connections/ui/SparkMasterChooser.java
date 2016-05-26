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
import org.rstudio.studio.client.common.GlobalDisplay;

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
   public SparkMasterChooser(List<String> remoteServers)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
     
      panel_ = new SimplePanel();
      
      listBox_ = new ListBox();
      listBox_.setWidth("100%");
      listBox_.addItem(LOCAL);
      for (int i = 0; i<remoteServers.size(); i++)
        listBox_.addItem(remoteServers.get(i));
      listBox_.addItem(REMOTE_SERVER);
      lastListBoxSelectedIndex_ = listBox_.getSelectedIndex();
      listBox_.addChangeHandler(new ChangeHandler() {

         @Override
         public void onChange(ChangeEvent event)
         {
            if (listBox_.getSelectedValue().equals(REMOTE_SERVER))
            {
               globalDisplay_.promptForTextWithOption(
                   "Connect to Remote Server", 
                   "Spark cluster master node:", 
                   "", 
                   false, 
                   null, 
                   false, 
                   new ProgressOperationWithInput<PromptWithOptionResult>() {

                     @Override
                     public void execute(PromptWithOptionResult input,
                                         ProgressIndicator indicator)
                     {
                        indicator.onCompleted();
                        
                        // add the item to the list if necessary
                        int targetItem = -1;
                        for (int i = 0; i<listBox_.getItemCount(); i++)
                        {
                           if (listBox_.getItemText(i).equals(input.input))
                           {
                              targetItem = i;
                              break;
                           }
                        }
                        if (targetItem == -1)
                        {
                           int lastIndex = listBox_.getItemCount() - 1;
                           listBox_.removeItem(lastIndex);
                           listBox_.addItem(input.input);
                           listBox_.addItem(REMOTE_SERVER);
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
   
   public boolean isLocalMaster(String master)
   {
      return master.equals(LOCAL);
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
   
   private final static String LOCAL = "Local";
   private final static String REMOTE_SERVER = "Remote Server...";
}
