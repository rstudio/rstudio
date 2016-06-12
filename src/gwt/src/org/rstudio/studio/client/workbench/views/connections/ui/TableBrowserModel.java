/*
 * TableBrowserModel.java
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

import java.util.ArrayList;
import java.util.HashMap;

import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.workbench.views.connections.model.Connection;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionsServerOperations;
import org.rstudio.studio.client.workbench.views.connections.model.Field;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.gwt.view.client.TreeViewModel;
import com.google.inject.Inject;

public class TableBrowserModel implements TreeViewModel
{
   public TableBrowserModel()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
    
   }
   
   @Inject
   public void initialize(ConnectionsServerOperations server)
   {
      server_ = server;
   }
  
   
   public void setConnection(Connection connection)
   {
      connection_ = connection;
      refresh();
   }
   
   @Override
   public <T> NodeInfo<?> getNodeInfo(T value)
   {
      // return the list of tables for the root node
      if (value == null)
      {
         tableProvider_ = new TableProvider();
         fieldProviders_.clear();
         return new DefaultNodeInfo<String>(tableProvider_, 
                                    new TextCell(),
                                    noTableSelectionModel_,
                                    null);
      }
      else if (value instanceof String)
      {
         FieldProvider fieldProvider = new FieldProvider((String)value);
         fieldProviders_.put((String)value, fieldProvider);
         return new DefaultNodeInfo<Field>(fieldProvider, 
                                           new FieldCell(),
                                           noFieldSelectionModel_,
                                           null);
      }
      
      // Unhandled type.
      String type = value.getClass().getName();
      throw new IllegalArgumentException("Unsupported object type: " + type);
   }

   @Override
   public boolean isLeaf(Object value)
   {
      return value instanceof Field;
   }
   
   public void refresh()
   {
      tableProvider_.refresh();
   }
   
   public void refreshTable(String table)
   {
      if (fieldProviders_.containsKey(table))
         fieldProviders_.get(table).refresh();
   }

   private class TableProvider extends AsyncDataProvider<String>
   {
      public void refresh()
      {
         fieldProviders_.clear();
         for (HasData<String> display : getDataDisplays())  
           display.setVisibleRangeAndClearData(display.getVisibleRange(), true);
      }
      
      @Override
      protected void onRangeChanged(final HasData<String> display)
      {
        if (connection_ == null)
        {
           clearData();
        }
        else
        {
           server_.connectionListTables(
              connection_, 
              new SimpleRequestCallback<JsArrayString>() {
                 @Override
                 public void onResponseReceived(JsArrayString tables)
                 {
                    updateRowCount(tables.length(), true);
                    ArrayList<String> data = new ArrayList<String>();
                    for (int i=0; i<tables.length(); i++)
                       data.add(tables.get(i));
                    updateRowData(0, data);
                 }    
                 
                 @Override
                 public void onError(ServerError error)
                 {
                    super.onError(error);
                    clearData();
                 }
              });
        }
      } 
      
      private void clearData()
      {
         updateRowCount(0, true);
         updateRowData(0, new ArrayList<String>());
      }
   }
   
   private class FieldProvider extends AsyncDataProvider<Field>
   {
      public FieldProvider(String table)
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
         ArrayList<Field> fields = new ArrayList<Field>();
         
         if (connection_ == null)
         {
            updateRowCount(0, true);
            updateRowData(0, fields);
            return;
         }

         server_.connectionListFields(
               connection_,
               table_,
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
                  }    
               });
      }
      
      
      private String table_;
   }
   
   private static class FieldCell extends AbstractCell<Field> {

      @Override
      public void render(Cell.Context context, Field value, SafeHtmlBuilder sb)
      {
         sb.appendEscaped(value.getName() + ": " + value.getType());
      }
    }
   
   private TableProvider tableProvider_;
   private HashMap<String,FieldProvider> fieldProviders_ 
                              = new HashMap<String,FieldProvider>();
   
   private Connection connection_;
   
   private ConnectionsServerOperations server_;
   
   private static NoSelectionModel<String> noTableSelectionModel_ = 
         new NoSelectionModel<String>();
   private static NoSelectionModel<Field> noFieldSelectionModel_ =
         new NoSelectionModel<Field>();
   
  
}
