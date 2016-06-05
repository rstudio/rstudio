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
package org.rstudio.studio.client.workbench.views.connections.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.gwt.cell.client.ImageResourceCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.layout.client.Layout.AnimationCallback;
import com.google.gwt.layout.client.Layout.Layer;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.inject.Inject;

import org.rstudio.core.client.cellview.ImageButtonColumn;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.VisibleChangedHandler;
import org.rstudio.core.client.theme.RStudioDataGridResources;
import org.rstudio.core.client.theme.RStudioDataGridStyle;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.SearchWidget;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarLabel;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.connections.ConnectionsPresenter;
import org.rstudio.studio.client.workbench.views.connections.events.ExploreConnectionEvent;
import org.rstudio.studio.client.workbench.views.connections.model.Connection;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionId;

public class ConnectionsPane extends WorkbenchPane implements ConnectionsPresenter.Display
{
   @Inject
   public ConnectionsPane(Commands commands)
   {
      // initialize
      super("Connections");
      commands_ = commands;
      
      // create main panel
      mainPanel_ = new LayoutPanel();
      
      // create data grid
      keyProvider_ = new ProvidesKey<Connection>() {
         @Override
         public Object getKey(Connection connection)
         {
            return connection.hashCode();
         }
      };
      connectionsDataGrid_ = new DataGrid<Connection>(1000, RES, keyProvider_);
      
      // add type column
      typeColumn_ = new Column<Connection, ImageResource>(new ImageResourceCell()) {
         @Override
         public ImageResource getValue(Connection object)
         {
            return RES.spark();
         }
      };
         
      connectionsDataGrid_.addColumn(typeColumn_, new TextHeader(""));
      connectionsDataGrid_.setColumnWidth(typeColumn_, 20, Unit.PX);
            
      // add host column
      hostColumn_ = new TextColumn<Connection>() {
         @Override
         public String getValue(Connection connection)
         {
            return connection.getHost();
         }
      };      
      connectionsDataGrid_.addColumn(hostColumn_, new TextHeader("Server"));
      connectionsDataGrid_.setColumnWidth(hostColumn_, 50, Unit.PCT);
      
      // add status column
      statusColumn_ = new TextColumn<Connection>() {

         @Override
         public String getValue(Connection connection)
         {
            if (isConnected(connection.getId()))
               return "Connected";
            else
               return "";
         }
      };
      connectionsDataGrid_.addColumn(statusColumn_, new TextHeader("Status"));
      connectionsDataGrid_.setColumnWidth(statusColumn_, 75, Unit.PX);
      
      
      // add explore column
      ImageButtonColumn<Connection> exploreColumn = 
            new ImageButtonColumn<Connection>(
              AbstractImagePrototype.create(RES.connectionExploreButton()),
              new OperationWithInput<Connection>() {
                @Override
                public void execute(Connection connection)
                {
                   fireEvent(new ExploreConnectionEvent(connection));
                }  
              },
              "Explore connection") {
         
         @Override
         protected boolean showButton(Connection connection)
         {
            return connection.getId().getType().equals("Spark");
         }
      };
      connectionsDataGrid_.addColumn(exploreColumn, new TextHeader(""));
      connectionsDataGrid_.setColumnWidth(exploreColumn, 30, Unit.PX);
      
      // data provider
      dataProvider_ = new ListDataProvider<Connection>();
      dataProvider_.addDataDisplay(connectionsDataGrid_);
      
      // add data grid to main panel
      mainPanel_.add(connectionsDataGrid_);
      mainPanel_.setWidgetTopBottom(connectionsDataGrid_, 0, Unit.PX, 0, Unit.PX);
      mainPanel_.setWidgetLeftRight(connectionsDataGrid_, 0, Unit.PX, 0, Unit.PX);

      
      // create connection explorer, add it, and hide it
      connectionExplorer_ = new ConnectionExplorer();
      connectionExplorer_.setSize("100%", "100%");
      connectionExplorer_.setConnected(commands_.disconnectConnection().isVisible());
      commands_.disconnectConnection().addVisibleChangedHandler(
            new VisibleChangedHandler() {

         @Override
         public void onVisibleChanged(AppCommand command)
         {
            connectionExplorer_.setConnected(command.isVisible());
         }   
      });
      
      mainPanel_.add(connectionExplorer_);
      mainPanel_.setWidgetTopBottom(connectionExplorer_, 0, Unit.PX, 0, Unit.PX);
      mainPanel_.setWidgetLeftRight(connectionExplorer_, -5000, Unit.PX, 5000, Unit.PX);
  
      // create widget
      ensureWidget();
   }
   
   @Override
   public void setConnections(List<Connection> connections)
   {
      dataProvider_.setList(connections);
      sortConnections();
   }
   
   @Override
   public void setActiveConnections(List<ConnectionId> connections)
   {
      activeConnections_ = connections;
      sortConnections();
      connectionsDataGrid_.redraw();
   }
  
   @Override
   public boolean isConnected(ConnectionId id)
   {
      for (int i=0; i<activeConnections_.size(); i++)
         if (activeConnections_.get(i).equalTo(id))
            return true;
      return false;
   }
   
   @Override
   public String getSearchFilter()
   {
      return searchWidget_.getValue();
   }
   
   @Override
   public HandlerRegistration addSearchFilterChangeHandler(
                                          ValueChangeHandler<String> handler)
   {
      return searchWidget_.addValueChangeHandler(handler);
   }
   
   @Override
   public HandlerRegistration addExploreConnectionHandler(
                              ExploreConnectionEvent.Handler handler)
   {
      return addHandler(handler, ExploreConnectionEvent.TYPE);
   }
   
   @Override
   public void showConnectionExplorer(final Connection connection, 
                                      String connectVia)
   {
      connectionExplorer_.setConnection(connection, connectVia);
      
      animate(connectionsDataGrid_,
              connectionExplorer_,
              true,
              new Command() {
               @Override
               public void execute()
               {
                  installConnectionExplorerToolbar(connection);
               }
      });
   }
   
   @Override
   public void showConnectionsList()
   {
      animate(connectionExplorer_,
              connectionsDataGrid_,
              false,
              new Command() {
                @Override
                public void execute()
                {
                   installConnectionsToolbar();
                }
             });
   }
   
   @Override
   public HasClickHandlers backToConnectionsButton()
   {
      return backToConnectionsButton_;
   }
  
   @Override
   public String getExplorerConnectVia()
   {
      return connectionExplorer_.getConnectVia();
   }
   
   
   @Override
   public void addToConnectionExplorer(String item)
   {
      connectionExplorer_.addItem(item);
   }
   
   
   @Override
   protected Toolbar createMainToolbar()
   {
      toolbar_ = new Toolbar();
      
      ToolbarPopupMenu actionsMenu = new ToolbarPopupMenu();
      actionsMenu.addItem(commands_.sparkLog().createMenuItem(false));
      actionsMenu.addItem(commands_.sparkUI().createMenuItem(false));
      actionsMenu.addSeparator();
      actionsMenu.addItem(commands_.removeConnection().createMenuItem(false));
      
      actionsMenuButton_ = new ToolbarButton(
            "",
            StandardIcons.INSTANCE.more_actions(),
            actionsMenu);

   
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
      
      backToConnectionsButton_ = new ToolbarButton(
            commands_.helpBack().getImageResource(), (ClickHandler)null);
      connectionName_ = new ToolbarLabel();
      connectionName_.getElement().getStyle().setMarginRight(8, Unit.PX);
          
      installConnectionsToolbar();
      
      return toolbar_;
   }
   
   @Override 
   protected Widget createMainWidget()
   {
      return mainPanel_;
   }
   
   @Override
   public void onBeforeSelected()
   {
      super.onBeforeSelected();
      connectionsDataGrid_.redraw();
   }
   
   private void animate(final Widget from,
                        final Widget to,
                        boolean rightToLeft,
                        final Command onComplete)
   {
      assert from != to;

      int width = getOffsetWidth();

      mainPanel_.setWidgetLeftWidth(from,
            0, Unit.PX,
            width, Unit.PX);
      mainPanel_.setWidgetLeftWidth(to,
            rightToLeft ? width : -width, Unit.PX,
                  width, Unit.PX);
      mainPanel_.forceLayout();

      mainPanel_.setWidgetLeftWidth(from,
            rightToLeft ? -width : width, Unit.PX,
                  width, Unit.PX);
      mainPanel_.setWidgetLeftWidth(to,
            0, Unit.PX,
            width, Unit.PX);

      mainPanel_.animate(300, new AnimationCallback()
      {
         public void onAnimationComplete()
         {
            mainPanel_.setWidgetLeftRight(to, 0, Unit.PX, 0, Unit.PX);
            mainPanel_.forceLayout();
            onComplete.execute();
         }

         public void onLayout(Layer layer, double progress)
         {
         }
      });
   }
   
   private void installConnectionsToolbar()
   {
      toolbar_.removeAllWidgets();
      
      toolbar_.addLeftWidget(commands_.newConnection().createToolbarButton());
        
      toolbar_.addRightWidget(searchWidget_);
   }
   
   private void installConnectionExplorerToolbar(Connection connection)
   {
      toolbar_.removeAllWidgets();
      
     
      toolbar_.addLeftWidget(backToConnectionsButton_);
      toolbar_.addLeftSeparator();
      toolbar_.addLeftWidget(connectionName_);
      toolbar_.addLeftSeparator();
      toolbar_.addLeftWidget(commands_.connectConnection().createToolbarButton());
      toolbar_.addLeftWidget(commands_.disconnectConnection().createToolbarButton());
      toolbar_.addLeftSeparator();
      toolbar_.addLeftWidget(actionsMenuButton_);
     
      
      connectionName_.setText(connection.getHost());
      
   }
   
   private void sortConnections()
   {
      // order the list
      List<Connection> connections = dataProvider_.getList();
      Collections.sort(connections, new Comparator<Connection>() {     
       @Override
       public int compare(Connection conn1, Connection conn2)
       {
          // values to use in comparison
          boolean conn1Connected = isConnected(conn1.getId());
          boolean conn2Connected = isConnected(conn2.getId());
          
          if (conn1Connected && !conn2Connected)
             return -1;
          else if (conn2Connected && !conn1Connected)
             return 1;
          else
             return Double.compare(conn1.getLastUsed(), conn2.getLastUsed());
       }       
    });
   }
   
   
   private Toolbar toolbar_;
   private final LayoutPanel mainPanel_;
   private final DataGrid<Connection> connectionsDataGrid_; 
   private final ConnectionExplorer connectionExplorer_;
   
   private final Column<Connection, ImageResource> typeColumn_;
   private final TextColumn<Connection> hostColumn_;
   private final TextColumn<Connection> statusColumn_;
  
   private final ProvidesKey<Connection> keyProvider_;
   private final ListDataProvider<Connection> dataProvider_;
   private List<ConnectionId> activeConnections_ = new ArrayList<ConnectionId>();
   
   private SearchWidget searchWidget_;
   private ToolbarButton backToConnectionsButton_;
   private ToolbarLabel connectionName_;
   private ToolbarButton actionsMenuButton_;
   
   private final Commands commands_;
   
   // Resources, etc ----
   public interface Resources extends RStudioDataGridResources
   {
      @Source({RStudioDataGridStyle.RSTUDIO_DEFAULT_CSS, "ConnectionsListDataGridStyle.css"})
      Styles dataGridStyle();
        
      ImageResource connectionExploreButton();
      
      ImageResource spark();
   }
   
   public interface Styles extends RStudioDataGridStyle
   {
   }
   
   private static final Resources RES = GWT.create(Resources.class);
   
   static {
      RES.dataGridStyle().ensureInjected();
   }
}