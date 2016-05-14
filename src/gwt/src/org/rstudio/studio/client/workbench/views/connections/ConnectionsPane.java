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

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.inject.Inject;

import org.rstudio.core.client.theme.RStudioDataGridResources;
import org.rstudio.core.client.theme.RStudioDataGridStyle;
import org.rstudio.core.client.widget.SearchWidget;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.connections.model.Connection;

public class ConnectionsPane extends WorkbenchPane implements ConnectionsPresenter.Display
{
   @Inject
   public ConnectionsPane(Commands commands)
   {
      super("Connections");
      
      commands_ = commands;
      
      // create data grid
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
      
      // add type column
      typeColumn_ = new TextColumn<Connection>() {
         @Override
         public String getValue(Connection connection)
         {
            return connection.getType();
         }
      };
      connectionsDataGrid_.addColumn(typeColumn_, new TextHeader("Type"));
      connectionsDataGrid_.setColumnWidth(typeColumn_, "30px");
            
      // add name column
      nameColumn_ = new TextColumn<Connection>() {
         @Override
         public String getValue(Connection connection)
         {
            return connection.getName();
         }
      };      
      connectionsDataGrid_.addColumn(nameColumn_, new TextHeader("Name"));
      connectionsDataGrid_.setColumnWidth(nameColumn_, "120px");
      
      // add status column
      statusColumn_ = new TextColumn<Connection>() {

         @Override
         public String getValue(Connection connection)
         {
            if (connection.isConnected())
               return "Connected";
            else
               return "Disconnected";
         }
      };
      connectionsDataGrid_.addColumn(statusColumn_, new TextHeader("Status"));
      connectionsDataGrid_.setColumnWidth(statusColumn_, "40px");
      
      // data provider
      dataProvider_ = new ListDataProvider<Connection>();
      dataProvider_.addDataDisplay(connectionsDataGrid_);
      
      // create widget
      ensureWidget();
   }
   
   @Override
   public void setConnections(List<Connection> connections)
   {
      dataProvider_.setList(connections);
   }
   
   @Override
   public Connection getSelectedConnection()
   {
      return selectionModel_.getSelectedObject();
   }
   
   @Override
   public HandlerRegistration addSelectedConnectionChangeHandler(
                                    SelectionChangeEvent.Handler handler)
   {
      return selectionModel_.addSelectionChangeHandler(handler);
   }
   
   @Override
   public HandlerRegistration addSearchFilterChangeHandler(
                                          ValueChangeHandler<String> handler)
   {
      return searchWidget_.addValueChangeHandler(handler);
   }
   
   
   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar();
      
      toolbar.addLeftWidget(commands_.newConnection().createToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands_.removeConnection().createToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands_.connectConnection().createToolbarButton());
      toolbar.addLeftWidget(commands_.disconnectConnection().createToolbarButton());
      
      searchWidget_ = new SearchWidget(new SuggestOracle() {
         @Override
         public void requestSuggestions(Request request, Callback callback)
         {
            // no suggestions
            callback.onSuggestionsReady(
                  request,
                  new Response(new ArrayList<Suggestion>()));
         }
      });
      toolbar.addRightWidget(searchWidget_);
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
   private final TextColumn<Connection> statusColumn_;
   
   private final ProvidesKey<Connection> keyProvider_;
   private final SingleSelectionModel<Connection> selectionModel_;
   private final ListDataProvider<Connection> dataProvider_;
   
   private SearchWidget searchWidget_;
   
   private final Commands commands_;
   
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