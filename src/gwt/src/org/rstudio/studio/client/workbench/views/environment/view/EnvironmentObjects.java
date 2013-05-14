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
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
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
   // provide access to styles defined in associated CSS file
   interface Style extends CssResource
   {
      String expandCol();
      String nameCol();
      String valueCol();
   }

   private class ObjectTableBuilder extends AbstractCellTableBuilder<RObjectEntry>
   {
      public ObjectTableBuilder()
      {
         super(objectList);
      }

      public void buildRowImpl(RObjectEntry rowValue, int absRowIndex)
      {
         TableRowBuilder row = startRow();

         // build the column containing the expand/collapse command
         TableCellBuilder expandCol = row.startTD();
         expandCol.className(style.expandCol());
         if (rowValue.canExpand)
         {
            ImageResource expandImage = rowValue.expanded ?
               EnvironmentResources.INSTANCE.collapseIcon() :
               EnvironmentResources.INSTANCE.expandIcon();

            expandCol.startImage().src(expandImage.getSafeUri().asString());
            expandCol.endImage();
         }
         expandCol.endTD();

         // build the column containing the name of the object
         TableCellBuilder nameCol = row.startTD();
         nameCol.className(style.nameCol());
         nameCol.text(rowValue.rObject.getName());
         nameCol.endTD();

         // build the column containing the description of the object
         TableCellBuilder descCol = row.startTD();
         nameCol.className(style.valueCol());
         descCol.text(rowValue.rObject.getValue());

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
      int idx = indexOfObject(obj.getName());

      // if the object is already in the environment, just update the value
      if (idx >= 0)
      {
         objectDataProvider_.getList().set(idx, new RObjectEntry(obj));
      }
      else
      {
         objectDataProvider_.getList().add(new RObjectEntry(obj));
      }
   }

   public void removeObject(String objName)
   {
      int idx = indexOfObject(objName);
      if (idx >= 0)
      {
         objectDataProvider_.getList().remove(idx);
      }
   }
   
   public void clearObjects()
   {
      objectDataProvider_.getList().clear();
   }

   public EnvironmentObjects()
   {
      objectList = new DataGrid<RObjectEntry>(RObjectEntry.KEY_PROVIDER);
      objectDataProvider_ = new ListDataProvider<RObjectEntry>();
      objectDataProvider_.addDataDisplay(objectList);
      objectNameColumn_ = new Column<RObjectEntry, String>(new TextCell()) {
         @Override
         public String getValue(RObjectEntry object) {
            return object.rObject.getName();
         }
      };

      objectDescriptionColumn_ = new Column<RObjectEntry, String>(new TextCell()) {
         @Override
         public String getValue(RObjectEntry object) {
            return object.rObject.getValue();
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

   private int indexOfObject(String objectName)
   {
      List<RObjectEntry> objects = objectDataProvider_.getList();

      // find the position of the object in the list
      int index;
      boolean foundObject = false;
      for (index = 0; index < objects.size(); index++)
      {
         if (objects.get(index).rObject.getName() == objectName)
         {
            foundObject = true;
            break;
         }
      }

      return foundObject ? index : -1;
   }
   

   @UiField HTMLPanel environmentContents;
   @UiField Style style;

   DataGrid<RObjectEntry> objectList;

   private Column<RObjectEntry, String> objectNameColumn_;
   private Column<RObjectEntry, String> objectDescriptionColumn_;
   private ListDataProvider<RObjectEntry> objectDataProvider_;
}
