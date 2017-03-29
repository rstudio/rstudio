/*
 * ObjectBrowserModel.java
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
import java.util.HashMap;
import java.util.Set;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.JsArrayUtil;
import org.rstudio.core.client.SafeHtmlUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.workbench.views.connections.events.ViewConnectionDatasetEvent;
import org.rstudio.studio.client.workbench.views.connections.model.Connection;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionObjectSpecifier;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionObjectType;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionsServerOperations;
import org.rstudio.studio.client.workbench.views.connections.model.DatabaseObject;
import org.rstudio.studio.client.workbench.views.connections.model.Field;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.Command;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.gwt.view.client.TreeViewModel;
import com.google.inject.Inject;

public class ObjectBrowserModel implements TreeViewModel
{
   public ObjectBrowserModel()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   public void initialize(ConnectionsServerOperations server,
                          EventBus eventBus)
   {
      server_ = server;
      eventBus_ = eventBus;
   }
  
   
   public void update(Connection connection,
                      Set<DatabaseObject> expandedNodes,
                      Command onTableUpdateCompleted,
                      Command onNodeExpansionCompleted)
   {
      connection_ = connection;
      expandedNodeRefreshQueue_ = expandedNodes;
      onTableUpdateCompleted_ = onTableUpdateCompleted;
      onNodeExpansionCompleted_ = onNodeExpansionCompleted;
      objectProvider_ = new ObjectProvider();
      refresh();
   }
   
   public void clear()
   {
      connection_ = null;
      if (objectProvider_ != null)
         objectProvider_.clear();
   }
   
   @Override
   public <T> NodeInfo<?> getNodeInfo(T value)
   {
      // no connection info yet
      if (connection_ == null)
         return null;

      // return the list of objects for the root node
      ConnectionObjectType rootType = null;
      boolean rootData = true;
      if (connection_.getObjectTypes() != null)
         rootType = connection_.getObjectTypes().get(0);
      if (rootType != null)
         rootData = rootType.getContains() == "data";

      if (value == null || rootData)
      {
         return new DefaultNodeInfo<DatabaseObject>(
                  objectProvider_,
                   new ContainerCell(),
                  noObjectSelectionModel_,
                  null);
      }
      else if (value instanceof DatabaseObject)
      {
         DatabaseObject val = (DatabaseObject)value;

         // ascertain whether this object contains data 
         if (connection_.isDataType(val.getType()))
         {
            FieldProvider fieldProvider = new FieldProvider((DatabaseObject)value);
            fieldProviders_.put(val, fieldProvider);
            return new DefaultNodeInfo<Field>(fieldProvider, 
                                              new FieldCell(),
                                              noFieldSelectionModel_,
                                              null);
         }
         
         ObjectProvider objectProvider = new ObjectProvider(val);
         objectProviders_.put(val, objectProvider);
         
         // does not contain data, so draw as a container cell
         return new DefaultNodeInfo<DatabaseObject>(
               objectProvider,
               new ContainerCell(),
               noObjectSelectionModel_, null);
      }
      
      // Unhandled type.
      String type = value.getClass().getName();
      throw new IllegalArgumentException("Unsupported object type: " + type);
   }

   @Override
   public boolean isLeaf(Object value)
   {
      if (value == null)
         return false;
      
      // extract the type of the object; if it's a known type, it's not a leaf
      JsObject jso = JsObject.fromJavaScriptObject((JavaScriptObject)value);
      String type = jso.getString("type").toLowerCase();
      JsArray<ConnectionObjectType> types = connection_.getObjectTypes();
      for (int i = 0; i < types.length(); i++)
      {
         if (types.get(i).getName() == type)
            return false;
      }
      return true;
   }
   
   public void refresh()
   {
      if (objectProvider_ != null)
         objectProvider_.refresh();
   }
   
   public void refreshObject(DatabaseObject object)
   {
      if (fieldProviders_.containsKey(object))
         fieldProviders_.get(object).refresh();
      else if (objectProviders_.containsKey(object))
         objectProviders_.get(object).refresh();
   }

   private void dequeNodeExpansion(DatabaseObject object)
   {
      if (expandedNodeRefreshQueue_ != null)
      {
         expandedNodeRefreshQueue_.remove(object);
         if (expandedNodeRefreshQueue_.size() == 0)
         {
            expandedNodeRefreshQueue_ = null;
            if (onNodeExpansionCompleted_ != null)
            {
               onNodeExpansionCompleted_.execute();
               onNodeExpansionCompleted_ = null;
            }
         }
      }
   }
   
   private class ObjectProvider extends AsyncDataProvider<DatabaseObject>
   {
      public ObjectProvider()
      {
         this(null);
      }

      public ObjectProvider(DatabaseObject parent)
      {
         parent_ = parent;
      }

      public void clear()
      {
         prefetchedObjectList_ = null;
         fieldProviders_.clear();
         clearData();
      }
      
      public void refresh()
      {  
         if (connection_ == null)
            return;
        
         // prefetch the objects so there is no gap between clearing the table
         // and redrawing the nodes
         listObjects(new CommandWithArg<JsArray<DatabaseObject>>() {
            @Override
            public void execute(JsArray<DatabaseObject> objects)
            {
               prefetchedObjectList_ = objects;
               for (HasData<DatabaseObject> display : getDataDisplays())
               {
                 display.setVisibleRangeAndClearData(display.getVisibleRange(), 
                                                     true);
               }
            }
         });
      }
      
      @Override
      protected void onRangeChanged(final HasData<DatabaseObject> display)
      {
        if (connection_ == null)
        {
           clearData();
        }
        else if (prefetchedObjectList_ != null)
        {
           JsArray<DatabaseObject> objects = prefetchedObjectList_;
           prefetchedObjectList_ = null;
           updateData(objects);
        }
        else
        {
           listObjects(new CommandWithArg<JsArray<DatabaseObject>>() {
               @Override
               public void execute(JsArray<DatabaseObject> objects)
               {
                  updateData(objects);
               }
           });
        }
      } 
      
      private void clearData()
      {
         updateRowCount(0, true);
         updateRowData(0, new ArrayList<DatabaseObject>());
         fireUpdateCompleted();
      }
      
      private void updateData(JsArray<DatabaseObject> objects)
      {
         updateRowCount(objects.length(), true);
         ArrayList<DatabaseObject> data = JsArrayUtil.toArrayList(objects);
         updateRowData(0, data);
         fireUpdateCompleted();
      }
      
      private void listObjects(final CommandWithArg<JsArray<DatabaseObject>> onCompleted)
      {
         ConnectionObjectSpecifier specifier = null;
         if (parent_ == null)
            specifier = new ConnectionObjectSpecifier();
         else
            specifier = parent_.createSpecifier();
         
         server_.connectionListObjects(
            connection_.getId(), 
            specifier,
            new SimpleRequestCallback<JsArray<DatabaseObject>>() {
               @Override
               public void onResponseReceived(JsArray<DatabaseObject> objects)
               {
                  for (int i = 0; i < objects.length(); i++)
                     objects.get(i).setParent(parent_);
                  onCompleted.execute(objects);
                  dequeNodeExpansion(parent_);
               }
               
               @Override
               public void onError(ServerError error)
               {
                  super.onError(error);
                  clearData();
               }
            });
      }
      
      private void fireUpdateCompleted()
      {
         if (onTableUpdateCompleted_ != null)
         {
            // execute update completed
            onTableUpdateCompleted_.execute();
            onTableUpdateCompleted_ = null;
            
            // if there is no node refresh queue execute node expansion complete
            if (expandedNodeRefreshQueue_ == null || 
                expandedNodeRefreshQueue_.size() == 0)
            {
               expandedNodeRefreshQueue_ = null;
               if (onNodeExpansionCompleted_ != null)
               {
                  onNodeExpansionCompleted_.execute();
                  onNodeExpansionCompleted_ = null;
               }
            }
         }
      }
      
      private final DatabaseObject parent_;
      private JsArray<DatabaseObject> prefetchedObjectList_ = null;
   }
   
   private class FieldProvider extends AsyncDataProvider<Field>
   {
      public FieldProvider(DatabaseObject table)
      {
         table_ = table;
      }
      
      public void refresh()
      {
         for (HasData<Field> display : getDataDisplays())  
           display.setVisibleRangeAndClearData(display.getVisibleRange(), true);
      }
      
      @Override
      protected void onRangeChanged(final HasData<Field> display)
      {  
         if (connection_ == null)
         {
            clearData();
            return;
         }

         server_.connectionListFields(
               connection_.getId(),
               table_.createSpecifier(),
               new SimpleRequestCallback<JsArray<Field>>() {
                  @Override
                  public void onResponseReceived(JsArray<Field> fields)
                  {
                     ArrayList<Field> data = new ArrayList<Field>();
                     if (fields != null)
                     {
                        for (int i=0; i<fields.length(); i++)
                           data.add(fields.get(i));
                     }
                     updateRowCount(data.size(), true);
                     updateRowData(0, data);
                     dequeNodeExpansion(table_);
                  }
                  
                  @Override
                  public void onError(ServerError error)
                  {
                     super.onError(error);
                     clearData();
                  } 
               });
      }
      
      private void clearData()
      {
         updateRowCount(0, true);
         updateRowData(0, new ArrayList<Field>());
         dequeNodeExpansion(table_);
      }
      
      private DatabaseObject table_;
   }
   
   private class ContainerCell extends AbstractCell<DatabaseObject>
   {
      public ContainerCell()
      {
         super("click");
      }
      
      @Override
      public void render(Cell.Context context, DatabaseObject container, 
            SafeHtmlBuilder sb)
      {
         SafeHtmlUtil.appendSpan(sb, "", container.getName());
         
         ConnectionObjectType type = connection_.getObjectType(
               container.getType());
         if (type != null && !StringUtil.isNullOrEmpty(type.getIconData()))
         {
            sb.appendHtmlConstant("<img src=\"" + type.getIconData() + "\" " + 
                 (type.isDataType() ? 
                     "class=\"" + RES.cellTreeStyle().tableViewDataset() + "\" " +
                     "title=\"View table (up to 1,000 records)\"" 
                     : 
                     "class=\"" + RES.cellTreeStyle().containerIcon() + "\" ") +
                 "/ >");
         }
      }
      
      @Override
      public void onBrowserEvent(Cell.Context context,
              Element parent, DatabaseObject value, NativeEvent event,
              ValueUpdater<DatabaseObject> valueUpdater) {
          if ("click".equals(event.getType()))
          {
             Element eventTarget = event.getEventTarget().cast();
             if (eventTarget.getAttribute("class").equals(
                               RES.cellTreeStyle().tableViewDataset()))
             {
                eventBus_.fireEvent(new ViewConnectionDatasetEvent(value));
             }
          }
      }
   }
   
   private static class FieldCell extends AbstractCell<Field> 
   {
      @Override
      public void render(Cell.Context context, Field value, SafeHtmlBuilder sb)
      {
         SafeHtmlUtil.appendSpan(sb, 
                                 RES.cellTreeStyle().fieldName(), 
                                 value.getName() + " : ");
         SafeHtmlUtil.appendSpan(sb, 
                                 RES.cellTreeStyle().fieldType(), 
                                 value.getType());
      }
   }
   
   private ObjectProvider objectProvider_;
   private HashMap<DatabaseObject,FieldProvider> fieldProviders_ 
                              = new HashMap<DatabaseObject,FieldProvider>();
   private HashMap<DatabaseObject,ObjectProvider> objectProviders_ 
                              = new HashMap<DatabaseObject,ObjectProvider>();
   
   private Connection connection_;
   
   
   private Set<DatabaseObject> expandedNodeRefreshQueue_ = null;
   private Command onTableUpdateCompleted_ = null;
   private Command onNodeExpansionCompleted_ = null;
   
   private ConnectionsServerOperations server_;
   private EventBus eventBus_;
   
   private static NoSelectionModel<DatabaseObject> noObjectSelectionModel_ = 
         new NoSelectionModel<DatabaseObject>();
   private static NoSelectionModel<Field> noFieldSelectionModel_ =
         new NoSelectionModel<Field>();
   
   static final ObjectBrowser.Resources RES = ObjectBrowser.RES;
  
}
