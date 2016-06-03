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

import com.google.gwt.cell.client.ImageResourceCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;

public class ConnectionExplorer extends Composite
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
      
      
 
      
      initWidget(dataGrid_);
   }
   
   
   public void addItem(String item)
   {
      dataProvider_.getList().add(item);
   }
   
   public void clearItems()
   {
      dataProvider_.getList().clear();
   }
  
   private final DataGrid<String> dataGrid_; 
 
   private final Column<String, ImageResource> typeColumn_;
   private final TextColumn<String> nameColumn_;
  
   private final ProvidesKey<String> keyProvider_;
   private final ListDataProvider<String> dataProvider_;
   
   
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
