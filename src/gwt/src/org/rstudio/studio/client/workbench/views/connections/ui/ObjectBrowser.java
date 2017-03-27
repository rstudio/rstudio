/*
 * ObjectBrowser.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

import java.util.ArrayList;
import java.util.HashSet;

import org.rstudio.studio.client.workbench.views.connections.model.Connection;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.BorderStyle;
import com.google.gwt.i18n.client.LocalizableResource.DefaultLocale;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.user.cellview.client.CellTree;
import com.google.gwt.user.cellview.client.CellTree.CellTreeMessages;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ObjectBrowser extends Composite implements RequiresResize
{
   public ObjectBrowser()
   {
      // create scroll panel and set the vertical wrapper as it's widget
      scrollPanel_ = new ScrollPanel();
      scrollPanel_.setSize("100%", "100%");
       
      // init widget
      initWidget(scrollPanel_);
   }
  
   public void clear()
   {
      if (objectsModel_ != null)
         objectsModel_.clear();
   }
   
   public void update(Connection connection, String hint)
   { 
      // create tables model and widget
      objectsModel_ = new ObjectBrowserModel();
      
      objects_ = new ArrayList<CellTree>();
      
      
      // TODO: capture expanded nodes
      // final Set<String> expandedNodes = new HashSet<String>();
      // TreeNode rootNode = objects_.get(0).getRootTreeNode();
      // for (int i = 0; i < rootNode.getChildCount(); i++)
      // {
      //    if (rootNode.isChildOpen(i))
      //    {
      //       String node = (String)rootNode.getChildValue(i);
      //       expandedNodes.add(node);
      //    }
      // }

      
      // capture scroll position
      // final int scrollPosition = scrollPanel_.getVerticalScrollPosition();
      // update the table then restore expanded nodes
      objectsModel_.update(
         connection,      // connection 
         new HashSet<String>(),    // TODO: track nodes to expand
         new Command() {   // table update completed, expand nodes
            @Override
            public void execute()
            {
               // TODO: restore expanded notes
               // TreeNode rootNode = objects_.get(0).getRootTreeNode();
               // for (int i = 0; i < rootNode.getChildCount(); i++)
               // {
               //    final String nodeName = (String)(rootNode.getChildValue(i));
               //    if (expandedNodes.contains(nodeName))
               //       rootNode.setChildOpen(i, true, false);
               // }
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
                     // TODO: update scroll pos
                     // scrollPanel_.setVerticalScrollPosition(scrollPosition); 
                  }
                  
               }.schedule(100);
              
            }
         });

      // create the top level list of objects
      CellTree top = new CellTree(objectsModel_, null, RES, MESSAGES);
      top.setDefaultNodeSize(Integer.MAX_VALUE);
      top.getElement().getStyle().setBorderStyle(BorderStyle.NONE);
      top.setWidth("100%");
      
      // wrap in vertical panel to get correct scrollbar behavior
      VerticalPanel verticalWrapper = new VerticalPanel();
      verticalWrapper.setWidth("100%");
      verticalWrapper.add(top);
      
      objects_.add(top);
      scrollPanel_.setWidget(verticalWrapper);
   }
   
   @Override
   public void onResize()
   {
   }
   
   public interface Resources extends CellTree.Resources {
      
      @Source("zoomDataset_2x.png")
      ImageResource zoomDataset2x();
      
      @ImageOptions(flipRtl = true)
      @Source("ExpandIcon_2x.png")
      ImageResource cellTreeClosedItem();

      /**
       * An image indicating that a node is loading.
       */
      @ImageOptions(flipRtl = true)
      @Source("progress.gif")
      ImageResource cellTreeLoading();
    
      @ImageOptions(flipRtl = true)
      @Source("CollapseIcon_2x.png")
      ImageResource cellTreeOpenItem();
      
      @Source({CellTree.Style.DEFAULT_CSS, "TableBrowser.css"})
      public Style cellTreeStyle();   
      
      public interface Style extends CellTree.Style
      {
         String fieldName();
         String fieldType();
         String tableViewDataset();
         String containerIcon();
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
   private ArrayList<CellTree> objects_;
   private ObjectBrowserModel objectsModel_;
  
}
