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

import org.rstudio.studio.client.workbench.views.connections.model.Connection;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.BorderStyle;
import com.google.gwt.i18n.client.LocalizableResource.DefaultLocale;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.user.cellview.client.CellTree;
import com.google.gwt.user.cellview.client.CellTree.CellTreeMessages;
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
      
      tables_.getElement().getStyle().setBorderStyle(BorderStyle.NONE);
      tables_.setWidth("100%");
      
      // wrap in vertical panel to get correct scrollbar behavior
      VerticalPanel verticalWrapper = new VerticalPanel();
      verticalWrapper.setWidth("100%");
      verticalWrapper.add(tables_);
      
      // create scroll panel and set the vertical wrapper as it's widget
      ScrollPanel scrollPanel = new ScrollPanel();
      scrollPanel.setSize("100%", "100%");
      scrollPanel.setWidget(verticalWrapper);
       
      // init widget
      initWidget(scrollPanel);
   }
  
   public void update(Connection connection, String hint)
   {
      connection_ = connection;
      tablesModel_.setConnection(connection_);  
   }
   
 
   @Override
   public void onResize()
   {
   }
   
   public interface Resources extends CellTree.Resources {
      
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
      
      
      @Source("TableBrowser.css")
      public CellTree.Style cellTreeStyle();   
   }
   
   private static final Resources RES = GWT.create(Resources.class);
   
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
   
   private final CellTree tables_;
   private final TableBrowserModel tablesModel_;
   
   private Connection connection_;
}
