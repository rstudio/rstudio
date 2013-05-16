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

import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.builder.shared.TableCellBuilder;
import com.google.gwt.dom.builder.shared.TableRowBuilder;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.text.shared.AbstractSafeHtmlRenderer;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.AbstractCellTableBuilder;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.NoSelectionModel;
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
      String detailRow();
      String categoryHeaderRow();
   }

   // builds individual rows of the object table
   private class ObjectTableBuilder extends AbstractCellTableBuilder<RObjectEntry>
   {
      public ObjectTableBuilder()
      {
         super(objectList);
      }

      // (re)build the given row
      public void buildRowImpl(RObjectEntry rowValue, int absRowIndex)
      {
         // if building the first row, we need to add a dummy row to the top.
         // since the grid uses a fixed table layout, the first row sets the
         // column widths, so we can't let the first row be a spanning header.
         if (absRowIndex == 0)
         {
            TableRowBuilder widthSettingRow = startRow();
            widthSettingRow.startTD().className(style.expandCol()).endTD();
            widthSettingRow.startTD().className(style.nameCol()).endTD();
            widthSettingRow.startTD().className(style.valueCol()).endTD();
            widthSettingRow.endTR();
         }

         // if this row is the first of its category, draw the category header
         if (rowValue.isCategoryLeader)
         {
            String categoryTitle;
            switch (rowValue.getCategory())
            {
               case RObjectEntry.Categories.Data:
                  categoryTitle = "Data";
                  break;
               case RObjectEntry.Categories.Function:
                  categoryTitle = "Functions";
                  break;
               default:
                  categoryTitle = "Values";
                  break;
            }
            TableRowBuilder leaderRow = startRow().className(style.categoryHeaderRow());
            TableCellBuilder objectHeader = leaderRow.startTD();
            objectHeader.colSpan(3)
                    .text(categoryTitle)
                    .endTD();
            leaderRow.endTR();
         }

         TableRowBuilder row = startRow();

         // build the column containing the expand/collapse command
         TableCellBuilder expandCol = row.startTD();
         expandCol.className(style.expandCol());
         renderCell(expandCol, createContext(0), objectExpandColumn_, rowValue);
         expandCol.endTD();

         // build the column containing the name of the object
         TableCellBuilder nameCol = row.startTD();
         nameCol.className(style.nameCol());
         nameCol.title(
                 rowValue.rObject.getName() +
                 " (" + rowValue.rObject.getType() + ")");
         renderCell(nameCol, createContext(0), objectNameColumn_, rowValue);
         nameCol.endTD();

         // build the column containing the description of the object
         TableCellBuilder descCol = row.startTD();
         descCol.title(rowValue.rObject.getValue());
         descCol.className(style.valueCol());
         if (!rowValue.expanded)
         {
            renderCell(descCol,
                       createContext(1),
                       objectDescriptionColumn_,
                       rowValue);
         }
         descCol.endTD();
         row.endTR();

         // if the row is expanded, draw its content
         if (rowValue.expanded)
         {
            buildExpandedContentRow(rowValue);
         }
      }

      // draw additional rows when the row has been expanded
      private void buildExpandedContentRow(RObjectEntry rowValue)
      {
         JsArrayString contents = rowValue.rObject.getContents();
         for (int i = 0; i < contents.length(); i++)
         {
            TableRowBuilder detail = startRow().className(style.detailRow());
            detail.startTD().endTD();
            TableCellBuilder objectDetail = detail.startTD();
            objectDetail.colSpan(2)
                    .text(contents.get(i))
                    .endTD();
            detail.endTR();
         }
      }
   }

   interface Binder extends UiBinder<Widget, EnvironmentObjects>
   {
   }

   public void setContextDepth(int contextDepth)
   {
      // this is where we'll set UI to debug mode it contextDepth is > 0
   }

   public void addObject(RObject obj)
   {
      int idx = indexOfExistingObject(obj.getName());

      // if the object is already in the environment, just update the value
      if (idx >= 0)
      {
         objectDataProvider_.getList().set(idx, new RObjectEntry(obj));
      }
      else
      {
         RObjectEntry entry = new RObjectEntry(obj);
         objectDataProvider_.getList().add(indexOfNewObject(entry), entry);
      }

      updateCategoryLeaders();
   }

   public void removeObject(String objName)
   {
      int idx = indexOfExistingObject(objName);
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
      // initialize the data grid and hook it up to the list of R objects in
      // the environment pane
      objectList = new DataGrid<RObjectEntry>(RObjectEntry.KEY_PROVIDER);
      objectDataProvider_ = new ListDataProvider<RObjectEntry>();
      objectDataProvider_.addDataDisplay(objectList);
      createColumns();
      objectList.addColumn(objectExpandColumn_);
      objectList.addColumn(objectNameColumn_);
      objectList.addColumn(objectDescriptionColumn_);
      objectList.setTableBuilder(new ObjectTableBuilder());
      objectList.setSkipRowHoverCheck(true);

      // make the grid fill the pane
      objectList.setWidth("100%");
      objectList.setHeight("100%");
      objectList.setSelectionModel(new NoSelectionModel<RObjectEntry>(RObjectEntry.KEY_PROVIDER));
      initWidget(GWT.<Binder>create(Binder.class).createAndBindUi(this));

      // add the grid to the pane--needs to be done after initWidget since the
      // pane doesn't exist until UIBinder has done its thing
      environmentContents.add(objectList);
   }

   private int indexOfExistingObject(String objectName)
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

   private native int localeCompare(String first, String second) /*-{
       return first.localeCompare(second);
   }-*/;

   private int compareRObjectEntriesForSort(RObjectEntry first,
                                            RObjectEntry second)
   {
      int result = first.getCategory() - second.getCategory();
      if (result == 0)
      {
         result = localeCompare(first.rObject.getName(), second.rObject.getName());
      }
      return result;
   }

   private int indexOfNewObject(RObjectEntry obj)
   {
      List<RObjectEntry> objects = objectDataProvider_.getList();
      int numObjects = objects.size();
      int idx;
      for (idx = 0; idx < numObjects; idx++)
      {
         if (compareRObjectEntriesForSort(obj, objects.get(idx)) < 0)
         {
            break;
         }
      }
      return idx;
   }

   private void createColumns()
   {
      objectNameColumn_ = new Column<RObjectEntry, String>(new TextCell()) {
         @Override
         public String getValue(RObjectEntry object) {
            return object.rObject.getName();
         }
      };

      objectDescriptionColumn_ = new Column<RObjectEntry, String>(new TextCell()) {
         @Override
         public String getValue(RObjectEntry object) {
            String val = object.rObject.getValue();
            return val == "NO_VALUE" ? object.rObject.getDescription() : val;
         }
      };

      SafeHtmlRenderer<String> expanderRenderer = new AbstractSafeHtmlRenderer<String>()
      {
         @Override
         public SafeHtml render(String object)
         {
            SafeHtmlBuilder sb = new SafeHtmlBuilder();
            if (object != "")
            {
               sb.appendHtmlConstant("<input type=\"image\" src=\"")
                       .appendEscaped(object)
                       .appendHtmlConstant("\" />");
            }
            else
            {
               sb.appendHtmlConstant("&nbsp;");
            }
            return sb.toSafeHtml();
         }
      };

      objectExpandColumn_ = new Column<RObjectEntry, String>(new ClickableTextCell(expanderRenderer))
      {
         @Override
         public String getValue(RObjectEntry object)
         {
            if (object.canExpand)
            {
               ImageResource expandImage = object.expanded ?
                                           EnvironmentResources.INSTANCE.collapseIcon() :
                                           EnvironmentResources.INSTANCE.expandIcon();

               return expandImage.getSafeUri().asString();
            }
            else
            {
               return "";
            }
         }
      };
      objectExpandColumn_.setFieldUpdater(new FieldUpdater<RObjectEntry, String>()
      {
         @Override
         public void update(int index, RObjectEntry object, String value)
         {
            if (object.canExpand)
            {
               object.expanded = !object.expanded;
               objectList.redrawRow(index);
            }
         }
      });
   }

   private void updateCategoryLeaders()
   {
      List<RObjectEntry> objects = objectDataProvider_.getList();
      Boolean[] leaders = { false, false, false };

      for (int i = 0; i < objects.size(); i++)
      {
         RObjectEntry entry = objects.get(i);
         int category = entry.getCategory();
         Boolean leader = entry.isCategoryLeader;
         if (!leaders[category])
         {
            leaders[category] = true;
            if (!leader)
            {
               entry.isCategoryLeader = true;
            }
         }
         else if (leader)
         {
            entry.isCategoryLeader = false;
         }
      }
   }
   

   @UiField HTMLPanel environmentContents;
   @UiField Style style;

   DataGrid<RObjectEntry> objectList;

   private Column<RObjectEntry, String> objectExpandColumn_;
   private Column<RObjectEntry, String> objectNameColumn_;
   private Column<RObjectEntry, String> objectDescriptionColumn_;
   private ListDataProvider<RObjectEntry> objectDataProvider_;
}
