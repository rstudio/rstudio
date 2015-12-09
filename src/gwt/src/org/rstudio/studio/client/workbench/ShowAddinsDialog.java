/*
 * ShowAddinsDialog.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.cellview.client.TextHeader;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.inject.Inject;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.theme.RStudioDataGridResources;
import org.rstudio.core.client.theme.RStudioDataGridStyle;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.addins.Addins.RAddin;
import org.rstudio.studio.client.workbench.addins.Addins.RAddins;
import org.rstudio.studio.client.workbench.addins.AddinsServerOperations;

import java.util.ArrayList;
import java.util.List;

public class ShowAddinsDialog extends ModalDialog<Command>
{
   public ShowAddinsDialog(OperationWithInput<Command> operation)
   {
      super("Addins", operation);
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      keyProvider_ = new ProvidesKey<RAddin>()
      {
         @Override
         public Object getKey(RAddin addin)
         {
            return addin.hashCode();
         }
      };
      
      table_ = new DataGrid<RAddin>(1000, RES, keyProvider_);
      table_.setWidth("700px");
      table_.setHeight("400px");
      
      selectionModel_ = new SingleSelectionModel<RAddin>();
      selectionModel_.addSelectionChangeHandler(new SelectionChangeEvent.Handler()
      {
         @Override
         public void onSelectionChange(SelectionChangeEvent event)
         {
            selection_ = selectionModel_.getSelectedObject();
         }
      });
      
      table_.setSelectionModel(selectionModel_);
      
      addColumns();
      
      dataProvider_ = new ListDataProvider<RAddin>();
      dataProvider_.addDataDisplay(table_);
      
      server_.getRAddins(new ServerRequestCallback<RAddins>()
      {
         @Override
         public void onResponseReceived(RAddins addins)
         {
            List<RAddin> data = new ArrayList<RAddin>();
            for (String key : JsUtil.asIterable(addins.keys()))
               data.add(addins.get(key));
            dataProvider_.setList(data);
         }
         
         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
         }
      });
   }
   
   @Inject
   private void initialize(AddinsServerOperations server)
   {
      server_ = server;
   }
   
   private void addColumns()
   {
      // Package ----
      TextColumn<RAddin> pkgColumn = new TextColumn<RAddin>()
      {
         @Override
         public String getValue(RAddin addin)
         {
            return addin.getPackage();
         }
      };
      table_.addColumn(pkgColumn, new TextHeader("Package"));
      table_.setColumnWidth(pkgColumn, "120px");
            
      // Name ----
      TextColumn<RAddin> nameColumn = new TextColumn<RAddin>()
      {
         @Override
         public String getValue(RAddin addin)
         {
            return addin.getName();
         }
      };
      table_.addColumn(nameColumn, new TextHeader("Name"));
      table_.setColumnWidth(nameColumn, "120px");
      
      // Description ----
      TextColumn<RAddin> descColumn = new TextColumn<RAddin>()
      {
         @Override
         public String getValue(RAddin addin)
         {
            return addin.getDescription();
         }
      };
      table_.addColumn(descColumn, new TextHeader("Description"));
     
   }
   
   @Override
   protected Command collectInput()
   {
      final String id = selection_.getPackage() + "::" + selection_.getBinding();
      return new Command()
      {
         @Override
         public void execute()
         {
            server_.executeRAddin(id, new VoidServerRequestCallback());
         }
      };
   }

   @Override
   protected Widget createMainWidget()
   {
      return table_;
   }
   
   // Private members ----
   private final DataGrid<RAddin> table_;
   private final ProvidesKey<RAddin> keyProvider_;
   private final ListDataProvider<RAddin> dataProvider_;
   private final SingleSelectionModel<RAddin> selectionModel_;
   
   private RAddin selection_;
   
   // Injected ----
   private AddinsServerOperations server_;
   
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
