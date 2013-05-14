/*
 * EnvironmentObjects.java
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

package org.rstudio.studio.client.workbench.views.environment.view;

import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.builder.shared.TableCellBuilder;
import com.google.gwt.dom.builder.shared.TableRowBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.AbstractCellTableBuilder;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import org.rstudio.studio.client.workbench.views.environment.model.RObject;

import java.util.List;

public class EnvironmentObjects extends Composite
{
   private class ObjectTableBuilder extends AbstractCellTableBuilder<RObject>
   {
      public ObjectTableBuilder()
      {
         super(objectList);
      }

      public void buildRowImpl(RObject rowValue, int absRowIndex)
      {
         TableRowBuilder row = startRow();

         // build the column containing the name of the object
         TableCellBuilder nameCol = row.startTD();
         nameCol.text(rowValue.getName());
         nameCol.endTD();

         // build the column containing the description of the object
         TableCellBuilder descCol = row.startTD();
         descCol.text(rowValue.getValue());
         descCol.endTD();

         row.endTR();
      }
   }

   interface Binder extends UiBinder<Widget, EnvironmentObjects>
   {

   }

   public void setContextDepth(int contextDepth)
   {
   }

   public void addObject(RObject obj)
   {
      objectDataProvider_.getList().add(obj);
   }
   
   public void clearObjects()
   {
      objectDataProvider_.getList().clear();
   }

   public EnvironmentObjects()
   {
      objectList = new DataGrid<RObject>(RObject.KEY_PROVIDER);
      objectDataProvider_ = new ListDataProvider<RObject>();
      objectDataProvider_.addDataDisplay(objectList);
      objectNameColumn_ = new Column<RObject, String>(new TextCell()) {
         @Override
         public String getValue(RObject object) {
            return object.getName();
         }
      };

      objectDescriptionColumn_ = new Column<RObject, String>(new TextCell()) {
         @Override
         public String getValue(RObject object) {
            return object.getValue();
         }
      };
      objectList.addColumn(objectNameColumn_);
      objectList.addColumn(objectDescriptionColumn_);
      objectList.setWidth("100%");
      objectList.setHeight("100%");
      objectList.setTableBuilder(new ObjectTableBuilder());
      objectList.setEmptyTableWidget(new Label("No data."));
      initWidget(GWT.<Binder>create(Binder.class).createAndBindUi(this));

      environmentContents.add(objectList);
   }
   

   @UiField
   HTMLPanel environmentContents;
   DataGrid<RObject> objectList;

   private Column<RObject, String> objectNameColumn_;
   private Column<RObject, String> objectDescriptionColumn_;
   private ListDataProvider<RObject> objectDataProvider_;
}
