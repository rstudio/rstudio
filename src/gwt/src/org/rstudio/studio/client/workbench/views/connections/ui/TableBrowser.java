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

import org.rstudio.core.client.widget.HorizontalCenterPanel;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.workbench.views.connections.model.Connection;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionsServerOperations;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Style.BorderStyle;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.inject.Inject;

public class TableBrowser extends Composite implements RequiresResize
{
   public TableBrowser()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      container_ = new SimplePanel();
      
      tables_ = new ListBox();
      tables_.getElement().getStyle().setBorderStyle(BorderStyle.NONE);
      tables_.setVisibleItemCount(2);
      tables_.setSize("100%", "100%");
      
      noTables_ = new NoTablesPanel();
      
      container_.setWidget(noTables_);
     
      initWidget(container_);
   }
   
   @Inject
   public void initialize(ConnectionsServerOperations server)
   {
      server_ = server;
   }
   
   public void update(Connection connection, String hint)
   {
      connection_ = connection;
      
      server_.connectionListTables(
        connection_, 
        new SimpleRequestCallback<JsArrayString>() {
           @Override
           public void onResponseReceived(JsArrayString tables)
           {
              tables_.clear();
              tables_.setVisibleItemCount(Math.max(2, tables.length()));
              for (int i = 0; i<tables.length(); i++)
                 tables_.addItem(tables.get(i));
              container_.setWidget(tables.length() > 0 ? tables_ : noTables_);
           }    
        });
   }
   
   public void clear()
   {
      tables_.clear();
      container_.setWidget(noTables_);
   }
   

   @Override
   public void onResize()
   {
   }
   


   private final SimplePanel container_;
   private final ListBox tables_;
   private final NoTablesPanel noTables_;
   private ConnectionsServerOperations server_;
   
   private Connection connection_;
   
   private static class NoTablesPanel extends HorizontalCenterPanel
   {
      public NoTablesPanel()
      {
         super(createStatusLabel(), 50);
         setSize("100%", "100%");
      }

      
      private static Label createStatusLabel()
      {
         Label label = new Label("(No tables)");
         label.getElement().getStyle().setColor("#888");
         return label;
      }
   }
}
