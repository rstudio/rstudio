/*
 * TableBrowser.java
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

import java.util.HashSet;
import java.util.Set;

import org.rstudio.studio.client.workbench.views.connections.model.Connection;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.BorderStyle;
import com.google.gwt.i18n.client.LocalizableResource.DefaultLocale;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.user.cellview.client.CellTree;
import com.google.gwt.user.cellview.client.TreeNode;
import com.google.gwt.user.cellview.client.CellTree.CellTreeMessages;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class TableBrowser extends Composite implements RequiresResize
{
   public TableBrowser()
   {  
      // create tables model and widget
      tablesModel_ = new TableBrowserModel();
      
      tables_ = new CellTree(tablesModel_, null, RES, MESSAGES);
      tables_.setDefaultNodeSize(Integer.MAX_VALUE);
      tables_.getElement().getStyle().setBorderStyle(BorderStyle.NONE);
      tables_.setWidth("100%");
      
      // wrap in vertical panel to get correct scrollbar behavior
      VerticalPanel verticalWrapper = new VerticalPanel();
      verticalWrapper.setWidth("100%");
      verticalWrapper.add(tables_);
      
      // create scroll panel and set the vertical wrapper as it's widget
      scrollPanel_ = new ScrollPanel();
      scrollPanel_.setSize("100%", "100%");
      scrollPanel_.setWidget(verticalWrapper);
       
      // init widget
      initWidget(scrollPanel_);
   }
  
   public void clear()
   {
      tablesModel_.clear();
   }
   
   public void update(Connection connection, String hint)
   { 
      // capture scroll position
      final int scrollPosition = scrollPanel_.getVerticalScrollPosition();
      
      // capture expanded nodes
      final Set<String> expandedNodes = new HashSet<String>();
      TreeNode rootNode = tables_.getRootTreeNode();
      for (int i = 0; i < rootNode.getChildCount(); i++)
      {
         if (rootNode.isChildOpen(i))
         {
            String node = (String)rootNode.getChildValue(i);
            expandedNodes.add(node);
         }
      }
      
      // update the table then restore expanded nodes
      tablesModel_.update(
         connection,      // connection 
         expandedNodes,    // track nodes to expand
         new Command() {   // table update completed, expand nodes
            @Override
            public void execute()
            {
               TreeNode rootNode = tables_.getRootTreeNode();
               for (int i = 0; i < rootNode.getChildCount(); i++)
               {
                  final String nodeName = (String)(rootNode.getChildValue(i));
                  if (expandedNodes.contains(nodeName))
                     rootNode.setChildOpen(i, true, false);
               }
            }
         },                      
         new Command() {   // node expansion completed, restore scroll position
            @Override
            public void execute()
            {
               // delay 100ms to allow expand animation to complete
               new Timer() {

                  @Override
                  public void run()
                  {
                     scrollPanel_.setVerticalScrollPosition(scrollPosition); 
                  }
                  
               }.schedule(100);
              
            }
         });  
   }
   
 
   @Override
   public void onResize()
   {
   }
   
   public interface Resources extends CellTree.Resources {
      
      ImageResource zoomDataset();
      
      @ImageOptions(flipRtl = true)
      @Source("ExpandIcon.png")
      ImageResource cellTreeClosedItem();

      /**
       * An image indicating that a node is loading.
       */
      @ImageOptions(flipRtl = true)
      @Source("progress.gif")
      ImageResource cellTreeLoading();

    
      @ImageOptions(flipRtl = true)
      @Source("CollapseIcon.png")
      ImageResource cellTreeOpenItem();
      
      
      @Source({CellTree.Style.DEFAULT_CSS, "TableBrowser.css"})
      public Style cellTreeStyle();   
      
      public interface Style extends CellTree.Style
      {
         String fieldName();
         String fieldType();
         String tableViewDataset();
      }
   }
   
   static final Resources RES = GWT.create(Resources.class);
   
   static {
      RES.cellTreeStyle().ensureInjected();
   }
   
   @DefaultLocale("en_US")
   public interface TableBrowserMessages extends CellTreeMessages {
     @DefaultMessage("Show more")
     String showMore();
     @DefaultMessage("(No tables)")
     String emptyTree();
   }
   
   private static final TableBrowserMessages MESSAGES 
                              = GWT.create(TableBrowserMessages.class);
   
   private final ScrollPanel scrollPanel_;
   private final CellTree tables_;
   private final TableBrowserModel tablesModel_;
  
}
