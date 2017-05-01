/*
 * ConnectionsPane.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
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
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.inject.Inject;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.cellview.ImageButtonColumn;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.VisibleChangedHandler;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.RStudioDataGridResources;
import org.rstudio.core.client.theme.RStudioDataGridStyle;
import org.rstudio.core.client.widget.Base64ImageCell;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.SearchWidget;
import org.rstudio.core.client.widget.SecondaryToolbar;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarLabel;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.connections.ConnectionsPresenter;
import org.rstudio.studio.client.workbench.views.connections.events.ActiveConnectionsChangedEvent;
import org.rstudio.studio.client.workbench.views.connections.events.ExecuteConnectionActionEvent;
import org.rstudio.studio.client.workbench.views.connections.events.ExecuteConnectionActionEvent.Handler;
import org.rstudio.studio.client.workbench.views.connections.events.ExploreConnectionEvent;
import org.rstudio.studio.client.workbench.views.connections.events.PerformConnectionEvent;
import org.rstudio.studio.client.workbench.views.connections.model.Connection;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionAction;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionId;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionOptions;

public class ConnectionsPane extends WorkbenchPane 
                             implements ConnectionsPresenter.Display,
                                        ActiveConnectionsChangedEvent.Handler
{
   @Inject
   public ConnectionsPane(Commands commands, EventBus eventBus)
   {
      // initialize
      super("Connections");
      commands_ = commands;
      eventBus_ = eventBus;

      // track activation events to update the toolbar
      eventBus_.addHandler(ActiveConnectionsChangedEvent.TYPE, this);
      
      // create main panel
      mainPanel_ = new LayoutPanel();
      mainPanel_.addStyleName("ace_editor_theme");
      
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
      selectionModel_.addSelectionChangeHandler(new SelectionChangeEvent.Handler()
      {
         @Override
         public void onSelectionChange(SelectionChangeEvent event)
         {
            Connection selectedConnection = selectionModel_.getSelectedObject();
            if (selectedConnection != null)
               fireEvent(new ExploreConnectionEvent(selectedConnection));  
         }
      });
       
      // add type column; this is a package-provided image we scale to 16x16 
      typeColumn_ = new Column<Connection, String>(new Base64ImageCell(16, 16))
      {
         @Override
         public String getValue(Connection connection)
         {
            if (StringUtil.isNullOrEmpty(connection.getIconData()))
               return null;
            return connection.getIconData();
         }
      };
         
      connectionsDataGrid_.addColumn(typeColumn_, new TextHeader(""));
      connectionsDataGrid_.setColumnWidth(typeColumn_, 20, Unit.PX);
            
      // add host column
      hostColumn_ = new TextColumn<Connection>() {
         @Override
         public String getValue(Connection connection)
         {
            return connection.getDisplayName();
         }
      };      
      connectionsDataGrid_.addColumn(hostColumn_, new TextHeader("Connection"));
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
      statusColumn_.setCellStyleNames(RES.dataGridStyle().statusColumn());
      connectionsDataGrid_.addColumn(statusColumn_, new TextHeader("Status"));
      connectionsDataGrid_.setColumnWidth(statusColumn_, 75, Unit.PX);
      
      // add explore column
      ImageButtonColumn<Connection> exploreColumn = 
            new ImageButtonColumn<Connection>(
              new ImageResource2x(RES.connectionExploreButton2x()),
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
      
      mainPanel_.add(connectionExplorer_);
      mainPanel_.setWidgetTopBottom(connectionExplorer_, 0, Unit.PX, 0, Unit.PX);
      mainPanel_.setWidgetLeftRight(connectionExplorer_, -5000, Unit.PX, 5000, Unit.PX);
  
      // create widget
      ensureWidget();
      
      setSecondaryToolbarVisible(false);
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
      // update active connection
      activeConnections_ = connections;
      sortConnections();
      
      // redraw the data grid
      connectionsDataGrid_.redraw();  
      
      // update explored connection
      connectionExplorer_.setConnected(exploredConnection_ != null &&
                                       isConnected(exploredConnection_.getId()));
   }
  
   private boolean isConnected(ConnectionId id)
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
   public HandlerRegistration addExecuteConnectionActionHandler(Handler handler)
   {
      return addHandler(handler, ExecuteConnectionActionEvent.TYPE);
   }
   
   @Override
   public void showConnectionExplorer(final Connection connection, 
                                      String connectVia)
   {
      selectionModel_.clear();
      
      setConnection(connection, connectVia);
      
      installConnectionExplorerToolbar(connection);
      
      transitionMainPanel(
              connectionsDataGrid_,
              connectionExplorer_,
              true,
              true,
              new Command() {
               @Override
               public void execute()
               {
                  connectionExplorer_.onResize();
               }
      });
   }
   
   @Override
   public void setExploredConnection(Connection connection)
   {
      setConnection(connection, connectionExplorer_.getConnectVia());
   }
   
   private void setConnection(Connection connection, String connectVia)
   {
      exploredConnection_ = connection;
      
      if (exploredConnection_ != null) 
      {
         connectionExplorer_.setConnection(connection, connectVia);
         connectionExplorer_.setConnected(isConnected(connection.getId()));
      }
   }
   
   @Override
   public void updateExploredConnection(String hint)
   {
      connectionExplorer_.updateObjectBrowser(hint);
   }
   
   @Override
   public void showConnectionsList(boolean animate)
   {
      exploredConnection_ = null;
 
      installConnectionsToolbar();
      
      transitionMainPanel(
              connectionExplorer_,
              connectionsDataGrid_,
              false,
              animate,
              new Command() {
                @Override
                public void execute()
                {
                   
                  
                }
             });
   }
   
   
   @Override
   public HasClickHandlers backToConnectionsButton()
   {
      return backToConnectionsButton_;
   }
  
   @Override
   public String getConnectVia()
   {
      return connectionExplorer_.getConnectVia();
   }
   
   @Override
   public String getConnectCode()
   {
      return connectionExplorer_.getConnectCode();
   }
   
   @Override
   public void showConnectionProgress()
   {
      connectionExplorer_.showConnectionProgress();
   }
   
   @Override
   public void onResize()
   {
      connectionExplorer_.onResize();
   }
   
   @Override
   protected Toolbar createMainToolbar()
   {
      toolbar_ = new Toolbar();
   
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
      backToConnectionsButton_.setTitle("View all connections");
       
      // connect meuu
      ToolbarPopupMenu connectMenu = new ToolbarPopupMenu();
      connectMenu.addItem(connectMenuItem(
            commands_.historySendToConsole().getImageResource(),
            "R Console",
            ConnectionOptions.CONNECT_R_CONSOLE));
      connectMenu.addSeparator();
      connectMenu.addItem(connectMenuItem(
            commands_.newSourceDoc().getImageResource(),
            "New R Script",
            ConnectionOptions.CONNECT_NEW_R_SCRIPT));
      connectMenu.addItem(connectMenuItem(
         commands_.newRNotebook().getImageResource(),
         "New R Notebook",
            ConnectionOptions.CONNECT_NEW_R_NOTEBOOK));
      if (BrowseCap.INSTANCE.canCopyToClipboard())
      {
         connectMenu.addSeparator();
         connectMenu.addItem(connectMenuItem(
               commands_.copyPlotToClipboard().getImageResource(),
               "Copy to Clipboard",
               ConnectionOptions.CONNECT_COPY_TO_CLIPBOARD));
      }
      connectMenuButton_ = new ToolbarButton(
            "Connect", 
            commands_.newConnection().getImageResource(), 
            connectMenu);
      
      // manage connect menu visibility
      connectMenuButton_.setVisible(!commands_.disconnectConnection().isVisible());
      commands_.disconnectConnection().addVisibleChangedHandler(
                                       new VisibleChangedHandler() {
         @Override
         public void onVisibleChanged(AppCommand command)
         {
            connectMenuButton_.setVisible(
                  !commands_.disconnectConnection().isVisible());
         }  
      });
          
      installConnectionsToolbar();
      
      return toolbar_;
   }
   
   @Override
   protected SecondaryToolbar createSecondaryToolbar()
   {
      secondaryToolbar_ = new SecondaryToolbar();
      secondaryToolbar_.addLeftWidget(connectionName_ = new ToolbarLabel());
      connectionIcon_ = new Image();
      connectionIcon_.setWidth("16px");
      connectionIcon_.setHeight("16px");
      connectionType_ = new ToolbarLabel();
      connectionType_.getElement().getStyle().setMarginLeft(5, Unit.PX);
      connectionType_.getElement().getStyle().setMarginRight(10, Unit.PX);
      secondaryToolbar_.addRightWidget(connectionIcon_);
      secondaryToolbar_.addRightWidget(connectionType_);
      
      return secondaryToolbar_;
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
   
   private MenuItem connectMenuItem(ImageResource icon, 
         String text, 
         final String connectVia)
   {
      return new MenuItem(
            AppCommand.formatMenuLabel(icon, text, null),
            true,
            new Scheduler.ScheduledCommand() {

               @Override
               public void execute()
               {
                  eventBus_.fireEvent(
                        new PerformConnectionEvent(
                              connectVia, 
                              connectionExplorer_.getConnectCode()));    
               }
            });
   }

   
   private void transitionMainPanel(
                        final Widget from,
                        final Widget to,
                        boolean rightToLeft,
                        boolean animate,
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

      to.setVisible(true);
      from.setVisible(true);
      
      final Command completeLayout = new Command() {

         @Override
         public void execute()
         {
            mainPanel_.setWidgetLeftRight(to, 0, Unit.PX, 0, Unit.PX);
            from.setVisible(false);
            mainPanel_.forceLayout();
            onComplete.execute();
         }
         
      };
      
      if (animate)
      {
         mainPanel_.animate(300, new AnimationCallback()
         {
            public void onAnimationComplete()
            {
               completeLayout.execute();
            }
   
            public void onLayout(Layer layer, double progress)
            {
            }
         });
      }
      else
      {
         completeLayout.execute();
      }
   }
   
   private void installConnectionsToolbar()
   {
      toolbar_.removeAllWidgets();
      
      toolbar_.addLeftWidget(commands_.newConnection().createToolbarButton());
      
      toolbar_.addLeftSeparator();
        
      toolbar_.addRightWidget(searchWidget_);
      
      setSecondaryToolbarVisible(false);
   }
   
   private void installConnectionExplorerToolbar(final Connection connection)
   {
      toolbar_.removeAllWidgets();
     
      toolbar_.addLeftWidget(backToConnectionsButton_);
      toolbar_.addLeftSeparator();

      toolbar_.addLeftWidget(connectMenuButton_);
      
      toolbar_.addLeftSeparator();
      
      if (isConnected(connection.getId()) && connection.getActions() != null)
      {
         // if we have any actions, create a toolbar button for each one
         for (int i = 0; i < connection.getActions().length(); i++)
         {
            final ConnectionAction action = connection.getActions().get(i);

            // use the supplied base64 icon data if it was provided
            Image icon = StringUtil.isNullOrEmpty(action.getIconData()) ?
                  null :
                  new Image(action.getIconData());
            
            // force to 20x18
            if (icon != null)
            {
               icon.setWidth("20px");
               icon.setHeight("18px");
            }
             
            ToolbarButton button = new ToolbarButton(action.getName(), 
                  icon, // left image
                  null, // right image
                  // invoke the action when the button is clicked
                  new ClickHandler()
                  {
                     @Override
                     public void onClick(ClickEvent arg0)
                     {
                        fireEvent(new ExecuteConnectionActionEvent(
                              connection.getId(), action.getName()));
                     }
                  });
            
            // move the toolbar button up 5px to account for missing icon if
            // none was supplied
            if (StringUtil.isNullOrEmpty(action.getIconData()))
               button.getElement().getStyle().setMarginTop(-5, Unit.PX);
            toolbar_.addLeftWidget(button);
            toolbar_.addLeftSeparator();
         }
      }

      toolbar_.addLeftWidget(commands_.disconnectConnection().createToolbarButton());
      
      toolbar_.addRightWidget(commands_.removeConnection().createToolbarButton());
      toolbar_.addRightWidget(commands_.refreshConnection().createToolbarButton());
      
      connectionName_.setText(connection.getDisplayName());
      connectionIcon_.setUrl(connection.getIconData());
      connectionType_.setText(connection.getId().getType());
      setSecondaryToolbarVisible(true);
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
             return -1 * Double.compare(conn1.getLastUsed(), conn2.getLastUsed());
       }       
    });
   }
   
   
   private Toolbar toolbar_;
   private final LayoutPanel mainPanel_;
   private final DataGrid<Connection> connectionsDataGrid_; 
   private final SingleSelectionModel<Connection> selectionModel_;
   private final ConnectionExplorer connectionExplorer_;
   
   private Connection exploredConnection_ = null;
   
   private final Column<Connection, String> typeColumn_;
   private final TextColumn<Connection> hostColumn_;
   private final TextColumn<Connection> statusColumn_;
  
   private final ProvidesKey<Connection> keyProvider_;
   private final ListDataProvider<Connection> dataProvider_;
   private List<ConnectionId> activeConnections_ = new ArrayList<ConnectionId>();
   
   private SearchWidget searchWidget_;
   private ToolbarButton backToConnectionsButton_;
   private ToolbarButton connectMenuButton_;
   
   private SecondaryToolbar secondaryToolbar_;
   private ToolbarLabel connectionName_;
   private Image connectionIcon_;
   private ToolbarLabel connectionType_;
   
   private final Commands commands_;
   private final EventBus eventBus_;
   
   // Resources, etc ----
   public interface Resources extends RStudioDataGridResources
   {
      @Source({RStudioDataGridStyle.RSTUDIO_DEFAULT_CSS, "ConnectionsListDataGridStyle.css"})
      Styles dataGridStyle();
        
      @Source("connectionExploreButton_2x.png")
      ImageResource connectionExploreButton2x();
   }
   
   public interface Styles extends RStudioDataGridStyle
   {
      String statusColumn();
   }
   
   private static final Resources RES = GWT.create(Resources.class);
   
   static {
      RES.dataGridStyle().ensureInjected();
   }

   @Override
   public void onActiveConnectionsChanged(ActiveConnectionsChangedEvent event)
   {
      activeConnections_.clear();
      
      JsArray<ConnectionId> connections = event.getActiveConnections();
      for (int idxConn = 0; idxConn < connections.length(); idxConn++) {
         activeConnections_.add(connections.get(idxConn));
      }
      
      sortConnections();
      
      installConnectionExplorerToolbar(exploredConnection_);
   }
}