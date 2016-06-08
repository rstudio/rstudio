/*
 * ConnectionExplorer.java
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

import org.rstudio.core.client.theme.RStudioDataGridResources;
import org.rstudio.core.client.theme.RStudioDataGridStyle;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.studio.client.workbench.views.connections.model.Connection;

import com.google.gwt.cell.client.ImageResourceCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;

public class ConnectionExplorer extends Composite implements RequiresResize
{
   public ConnectionExplorer()
   {
      // create data grid
      keyProvider_ = new ProvidesKey<String>() {
         @Override
         public Object getKey(String object)
         {
            return object.hashCode();
         }
      };
      dataGrid_ = new DataGrid<String>(1000, RES, keyProvider_);
      dataGrid_.setSize("100%", "100%");
      
      // add type column
      typeColumn_ = new Column<String, ImageResource>(new ImageResourceCell()) {
         @Override
         public ImageResource getValue(String object)
         {
            return RES.table();
         }
      };
         
      dataGrid_.addColumn(typeColumn_, new TextHeader(""));
      dataGrid_.setColumnWidth(typeColumn_, 20, Unit.PX);
            
      // add name column
      nameColumn_ = new TextColumn<String>() {
         @Override
         public String getValue(String object)
         {
            return object;
         }
      };      
      dataGrid_.addColumn(nameColumn_, new TextHeader("Name"));
      dataGrid_.setColumnWidth(nameColumn_, 80, Unit.PCT);
    
      // data provider
      dataProvider_ = new ListDataProvider<String>();
      dataProvider_.addDataDisplay(dataGrid_);
      
      int codePanelHeight = 125;
      codePanel_ = new ConnectionCodePanel();
      codePanel_.addStyleName(ThemeStyles.INSTANCE.secondaryToolbarPanel());
      codePanel_.getElement().getStyle().setPadding(8, Unit.PX);
      codePanel_.setHeight(codePanelHeight + "px");
      codePanel_.setWidth("100%");
      
      dockPanel_ = new DockLayoutPanel(Unit.PX);
      dockPanel_.addNorth(codePanel_, codePanelHeight);
      dockPanel_.add(dataGrid_);
     
      setConnected(false);
      
      initWidget(dockPanel_);
   }
   
   
   public void addItem(String item)
   {
      dataProvider_.getList().add(item);
   }
   
   public void setConnection(Connection connection, String connectVia)
   {
      codePanel_.setCode(connection.getConnectCode(), connectVia);
   }
   
   public void setConnected(boolean connected)
   {
      // always clear the list
      dataProvider_.getList().clear();
      
     
      dockPanel_.setWidgetHidden(codePanel_, connected);
   }
   
   public String getConnectCode()
   {
      return codePanel_.getCode();
   }
   
   public String getConnectVia()
   {
      return codePanel_.getConnectVia();
   }
  
   @Override
   public void onResize()
   {
      codePanel_.onResize();
      
   }
   
   private final DataGrid<String> dataGrid_; 
 
   private final Column<String, ImageResource> typeColumn_;
   private final TextColumn<String> nameColumn_;
  
   private final ProvidesKey<String> keyProvider_;
   private final ListDataProvider<String> dataProvider_;
   
   private final ConnectionCodePanel codePanel_;
      
   private final DockLayoutPanel dockPanel_;
   
   // Resources, etc ----
   public interface Resources extends RStudioDataGridResources
   {
      @Source({RStudioDataGridStyle.RSTUDIO_DEFAULT_CSS, "ConnectionExplorerDataGridStyle.css"})
      Styles dataGridStyle();
        
      ImageResource table();
   }
   
   public interface Styles extends RStudioDataGridStyle
   {
   }
   
   private static final Resources RES = GWT.create(Resources.class);
   
   static {
      RES.dataGridStyle().ensureInjected();
   }
}
