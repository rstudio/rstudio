/*
 * ConnectionsPane.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.connections;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.inject.Inject;

import org.rstudio.core.client.theme.RStudioDataGridResources;
import org.rstudio.core.client.theme.RStudioDataGridStyle;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.connections.model.Connection;

public class ConnectionsPane extends WorkbenchPane implements ConnectionsPresenter.Display
{
   @Inject
   public ConnectionsPane()
   {
      super("Connections");
      
      keyProvider_ = new ProvidesKey<Connection>() {
         @Override
         public Object getKey(Connection connection)
         {
            return connection.hashCode();
         }
      };
      
      selectionModel_ = new SingleSelectionModel<Connection>();
      
      connectionsDataGrid_ = new DataGrid<Connection>(1000, RES, keyProvider_);
      connectionsDataGrid_.setSelectionModel(selectionModel_);
     
      typeColumn_ = new TextColumn<Connection>() {
         @Override
         public String getValue(Connection connection)
         {
            return connection.getType();
         }
      };
      
      connectionsDataGrid_.addColumn(typeColumn_, new TextHeader("Type"));
      connectionsDataGrid_.setColumnWidth(typeColumn_, "20px");
            
      // Name ----
      nameColumn_ = new TextColumn<Connection>() {
         @Override
         public String getValue(Connection connection)
         {
            return connection.getName();
         }
      };
      
      connectionsDataGrid_.addColumn(nameColumn_, new TextHeader("Name"));
      connectionsDataGrid_.setColumnWidth(nameColumn_, "120px");
      
      
      dataProvider_ = new ListDataProvider<Connection>();
      dataProvider_.addDataDisplay(connectionsDataGrid_);
      
      ensureWidget();
      
   }
   
   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar();
      return toolbar;
   }
   
   @Override 
   protected Widget createMainWidget()
   {
      return connectionsDataGrid_;
   }
   
   
   private final DataGrid<Connection> connectionsDataGrid_; 
   
   private final TextColumn<Connection> typeColumn_;
   private final TextColumn<Connection> nameColumn_;
   
   private final ProvidesKey<Connection> keyProvider_;
   private final SingleSelectionModel<Connection> selectionModel_;
   private final ListDataProvider<Connection> dataProvider_;
   
   // Resources, etc ----
   public interface Resources extends RStudioDataGridResources
   {
      @Source({RStudioDataGridStyle.RSTUDIO_DEFAULT_CSS})
      Styles dataGridStyle();
   }
   
   public interface Styles extends RStudioDataGridStyle
   {
   }
   
   private static final Resources RES = GWT.create(Resources.class);
   
   static {
      RES.dataGridStyle().ensureInjected();
   }
}