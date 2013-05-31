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
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.builder.shared.TableCellBuilder;
import com.google.gwt.dom.builder.shared.TableRowBuilder;
import com.google.gwt.event.dom.client.ScrollEvent;
import com.google.gwt.event.dom.client.ScrollHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.text.shared.AbstractSafeHtmlRenderer;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.AbstractCellTableBuilder;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.NoSelectionModel;
import org.rstudio.core.client.cellview.ScrollingDataGrid;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.studio.client.workbench.views.environment.model.RObject;

import java.util.*;

public class EnvironmentObjects extends Composite
   implements RequiresResize
{
   // Public interfaces -------------------------------------------------------

   public interface Binder extends UiBinder<Widget, EnvironmentObjects>
   {
   }

   public interface Observer
   {
      void editObject(String objectName);
      void viewObject(String objectName);
      void forceEvalObject(String objectName);
      void setObjectExpanded(String objectName);
      void setObjectCollapsed(String objectName);
      void setPersistedScrollPosition(int scrollPosition);
   }

   // Constructor -------------------------------------------------------------

   public EnvironmentObjects()
   {
      // initialize the data grid and hook it up to the list of R objects in
      // the environment pane
      objectList_ = new ScrollingDataGrid<RObjectEntry>(
              1024,
              RObjectEntry.KEY_PROVIDER);
      objectDataProvider_ = new ListDataProvider<RObjectEntry>();
      objectDataProvider_.addDataDisplay(objectList_);
      createColumns();
      objectList_.addColumn(objectExpandColumn_);
      objectList_.addColumn(objectNameColumn_);
      objectList_.addColumn(objectDescriptionColumn_);
      objectList_.setTableBuilder(new EnvironmentObjectTableBuilder());
      objectList_.setSkipRowHoverCheck(true);

      // disable persistent and transient row selection (currently necessary
      // because we emit more than one row per object and the DataGrid selection
      // behaviors aren't designed to work that way)
      objectList_.setSelectionModel(new NoSelectionModel<RObjectEntry>(
              RObjectEntry.KEY_PROVIDER));
      objectList_.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
      objectList_.getScrollPanel().addScrollHandler(new ScrollHandler()
      {
         @Override
         public void onScroll(ScrollEvent event)
         {
            if (useStatePersistence())
            {
               deferredScrollPosition_ = getScrollPosition();
               observer_.setPersistedScrollPosition(deferredScrollPosition_);
            }
         }
      });

      initWidget(GWT.<Binder>create(Binder.class).createAndBindUi(this));

      // these need to be done post-initWidget since they reference objects
      // created by initWidget
      objectList_.setEmptyTableWidget(buildEmptyGridMessage());
      objectList_.setStyleName(style.objectGrid());
      environmentContents.add(objectList_);
   }

   // Public methods ----------------------------------------------------------

   public void setContextDepth(int contextDepth)
   {
      contextDepth_ = contextDepth;
   }

   public void addObject(RObject obj)
   {
      int idx = indexOfExistingObject(obj.getName());
      RObjectEntry newEntry = new RObjectEntry(obj);
      boolean added = false;

      // if the object is already in the environment, just update the value
      if (idx >= 0)
      {
         RObjectEntry oldEntry = objectDataProvider_.getList().get(idx);

         if (oldEntry.rObject.getType().equals(obj.getType()))
         {
            // type did not change; update in-place and preserve expansion flag
            newEntry.expanded = oldEntry.expanded;
            objectDataProvider_.getList().set(idx, newEntry);
            added = true;
         }
         else
         {
            // types did change, do a full add/remove
            objectDataProvider_.getList().remove(idx);
         }
      }
      if (!added)
      {
         RObjectEntry entry = new RObjectEntry(obj);
         idx = indexOfNewObject(entry);
         objectDataProvider_.getList().add(idx, entry);
      }
      updateCategoryLeaders(true);
      objectList_.getRowElement(idx).scrollIntoView();
   }

   public void removeObject(String objName)
   {
      int idx = indexOfExistingObject(objName);
      if (idx >= 0)
      {
         objectDataProvider_.getList().remove(idx);
      }

      updateCategoryLeaders(true);
   }
   
   public void clearObjects()
   {
      objectDataProvider_.getList().clear();
   }

   // bulk add for objects--used on init or environment switch
   public void addObjects(JsArray<RObject> objects)
   {
      // create an entry for each object and sort the array
      int numObjects = objects.length();
      ArrayList<RObjectEntry> objectEntryList = new ArrayList<RObjectEntry>();
      for (int i = 0; i < numObjects; i++)
      {
         objectEntryList.add(new RObjectEntry(objects.get(i)));
      }
      Collections.sort(objectEntryList, new RObjectEntrySort());

      // push the list into the UI and update category leaders
      objectDataProvider_.getList().addAll(objectEntryList);
      updateCategoryLeaders(false);

      if (useStatePersistence())
      {
         setDeferredState();
      }
   }

   public void setObserver(Observer observer)
   {
      observer_ = observer;
   }

   public void setEnvironmentName(String environmentName)
   {
      environmentName_.setText(contextDepth_ > 0 ? environmentName + "()" : "");
      environmentEmptyMessage_.setText(contextDepth_ > 0 ?
                                       EMPTY_FUNCTION_ENVIRONMENT_MESSAGE :
                                       EMPTY_GLOBAL_ENVIRONMENT_MESSAGE);
   }

   public int getScrollPosition()
   {
      return objectList_.getScrollPanel().getVerticalScrollPosition();
   }

   public void setScrollPosition(int scrollPosition)
   {
      deferredScrollPosition_ = scrollPosition;
   }

   public void setExpandedObjects(JsArrayString objects)
   {
      deferredExpandedObjects_ = objects;
   }

   // RequiresResize implementation -------------------------------------------

   public void onResize()
   {
      objectList_.onResize();
   }

   // Private methods: object management --------------------------------------

   private int indexOfExistingObject(String objectName)
   {
      List<RObjectEntry> objects = objectDataProvider_.getList();

      // find the position of the object in the list--we can't use binary
      // search here since we're matching on names and the list isn't sorted
      // by name (it's sorted by type, then name)
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

   // returns the position a new object entry should occupy in the table
   private int indexOfNewObject(RObjectEntry obj)
   {
      List<RObjectEntry> objects = objectDataProvider_.getList();
      RObjectEntrySort sort = new RObjectEntrySort();
      int numObjects = objects.size();
      int idx;
      // consider: can we use binary search here?
      for (idx = 0; idx < numObjects; idx++)
      {
         if (sort.compare(obj, objects.get(idx)) < 0)
         {
            break;
         }
      }
      return idx;
   }

   // after adds or removes, we need to tag the new category-leading objects
   private void updateCategoryLeaders(boolean redrawUpdatedRows)
   {
      List<RObjectEntry> objects = objectDataProvider_.getList();

      // whether or not we've found a leader for each category
      Boolean[] leaders = { false, false, false };

      for (int i = 0; i < objects.size(); i++)
      {
         RObjectEntry entry = objects.get(i);
         int category = entry.getCategory();
         Boolean leader = entry.isCategoryLeader;
         // if we haven't found a leader for this category yet, make this object
         // the leader if it isn't already
         if (!leaders[category])
         {
            leaders[category] = true;
            if (!leader)
            {
               entry.isCategoryLeader = true;
            }
         }
         // if this object is marked as the leader but we've already found a
         // leader, unmark it
         else if (leader)
         {
            entry.isCategoryLeader = false;
         }

         // if we changed the leader flag, redraw the row
         if (leader != entry.isCategoryLeader
             && redrawUpdatedRows)
         {
            objectList_.redrawRow(i);
         }
      }
   }

   // Private methods: DataGrid setup -----------------------------------------

   // create each column for the data grid
   private void createColumns()
   {
      createExpandColumn();
      createNameColumn();
      createDescriptionColumn();
   }

   // attaches a handler to a column that invokes the associated object
   private void attachClickToInvoke(Column<RObjectEntry, String> column)
   {
      column.setFieldUpdater(new FieldUpdater<RObjectEntry, String>()
      {
         @Override
         public void update(int index, RObjectEntry object, String value)
         {
            // initial click on a promise forces eval, at which point it gets a
            // value we can interact with
            if (object.isPromise())
            {
               observer_.forceEvalObject(object.rObject.getName());
            }
            else if (object.getCategory() == RObjectEntry.Categories.Data)
            {
               observer_.viewObject(object.rObject.getName());
            }
            else
            {
               observer_.editObject(object.rObject.getName());
            }
         }
      });
   }

   private void createNameColumn()
   {
      // the name of the object (simple text column)
      objectNameColumn_ = new Column<RObjectEntry, String>(
              new ClickableTextCell())
              {
                  @Override
                  public String getValue(RObjectEntry object)
                  {
                     return object.rObject.getName();
                  }
              };
      attachClickToInvoke(objectNameColumn_);
   }

   private void createDescriptionColumn()
   {
      // the description *or* value of the object; when clicked, we'll view
      // or edit the data inside the object.
      objectDescriptionColumn_ = new Column<RObjectEntry, String>(
              new ClickableTextCell())
              {
                  @Override
                  public String getValue(RObjectEntry object)
                  {
                     String val = object.rObject.getValue();
                     return val == "NO_VALUE" ?
                            object.rObject.getDescription() :
                            val;
                  }
              };
      attachClickToInvoke(objectDescriptionColumn_);
   }

   private void createExpandColumn()
   {
      // the column containing the expand command; available only on objects
      // with contents (such as lists and data frames).
      SafeHtmlRenderer<String> expanderRenderer =
         new AbstractSafeHtmlRenderer<String>()
         {
            @Override
            public SafeHtml render(String object)
            {
               SafeHtmlBuilder sb = new SafeHtmlBuilder();
               if (!object.equals(""))
               {
                  sb.appendHtmlConstant("<input type=\"image\" src=\"")
                          .appendEscaped(object)
                          .appendHtmlConstant("\" class=\"")
                          .appendEscaped(style.expandIcon())
                          .appendHtmlConstant("\" />");
               }
               else
               {
                  sb.appendHtmlConstant("&nbsp;");
               }
               return sb.toSafeHtml();
            }
         };
      objectExpandColumn_ = new Column<RObjectEntry, String>(
         new ClickableTextCell(expanderRenderer))
         {
            @Override
            public String getValue(RObjectEntry object)
            {
               if (object.canExpand())
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
      objectExpandColumn_.setFieldUpdater(
         new FieldUpdater<RObjectEntry, String>()
         {
            @Override
            public void update(int index, RObjectEntry object, String value)
            {
               if (object.canExpand())
               {
                  object.expanded = !object.expanded;
                  // tell the observer this happened, so it can persist
                  // the state
                  if (useStatePersistence())
                  {
                     if (object.expanded)
                     {
                        observer_.setObjectExpanded(object.rObject.getName());
                     }
                     else
                     {
                        observer_.setObjectCollapsed(object.rObject.getName());
                     }
                  }
                  objectList_.redrawRow(index);
               }
            }
         });
   }

   private Widget buildEmptyGridMessage()
   {
      HTMLPanel messagePanel = new HTMLPanel("");
      messagePanel.setStyleName(style.emptyEnvironmentPanel());
      environmentName_ = new Label("");
      environmentName_.setStyleName(style.emptyEnvironmentName());
      environmentEmptyMessage_ = new Label(EMPTY_GLOBAL_ENVIRONMENT_MESSAGE);
      environmentEmptyMessage_.setStyleName(style.emptyEnvironmentMessage());
      messagePanel.add(environmentName_);
      messagePanel.add(environmentEmptyMessage_);
      return messagePanel;
   }

   // Private methods: state persistence --------------------------------------

   private void setDeferredState()
   {
      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         @Override
         public void execute()
         {
            // loop through the objects in the list and check to see if each
            // is marked expanded in the persisted list of expanded objects
            List<RObjectEntry> objects = objectDataProvider_.getList();
            for (int idxObj = 0; idxObj < objects.size(); idxObj++)
            {
               for (int idxExpanded = 0;
                    idxExpanded < deferredExpandedObjects_.length();
                    idxExpanded++)
               {
                  if (objects.get(idxObj).rObject.getName() ==
                      deferredExpandedObjects_.get(idxExpanded))
                  {
                     objects.get(idxObj).expanded = true;
                     objectList_.redrawRow(idxObj);
                  }
               }
            }

            // set the cached scroll position
            objectList_.getScrollPanel().setVerticalScrollPosition(
                    deferredScrollPosition_);

         }
      });
   }

   // we currently only set and/or get persisted state at the root context
   // level.
   private boolean useStatePersistence()
   {
      return contextDepth_ == 0;
   }

   // Private nested classes --------------------------------------------------

   // builds individual rows of the object table
   private class EnvironmentObjectTableBuilder
           extends AbstractCellTableBuilder<RObjectEntry>
   {
      public EnvironmentObjectTableBuilder()
      {
         super(objectList_);
      }

      // (re)build the given row
      public void buildRowImpl(RObjectEntry rowValue, int absRowIndex)
      {

         // build the header for the row (if any)
         buildRowHeader(rowValue, absRowIndex);

         TableRowBuilder row = startRow();

         if (rowValue.getCategory() == RObjectEntry.Categories.Data)
         {
            row.className(ThemeStyles.INSTANCE.environmentDataFrameRow());
         }

         // build the columns
         buildExpandColumn(rowValue, row);
         buildNameColumn(rowValue, row);
         buildDescriptionColumn(rowValue, row);

         row.endTR();

         // if the row is expanded, draw its content
         if (rowValue.expanded)
         {
            buildExpandedContentRow(rowValue);
         }
      }

      private void buildExpandColumn(RObjectEntry rowValue, TableRowBuilder row)
      {
         TableCellBuilder expandCol = row.startTD();
         expandCol.className(style.expandCol());
         renderCell(expandCol, createContext(0), objectExpandColumn_, rowValue);
         expandCol.endTD();
      }

      private void buildNameColumn(RObjectEntry rowValue, TableRowBuilder row)
      {
         TableCellBuilder nameCol = row.startTD();
         nameCol.className(style.nameCol());
         nameCol.title(
                 rowValue.rObject.getName() +
                 " (" + rowValue.rObject.getType() + ")");
         renderCell(nameCol, createContext(1), objectNameColumn_, rowValue);
         nameCol.endTD();
      }

      private void buildDescriptionColumn(RObjectEntry rowValue,
                                          TableRowBuilder row)
      {
         // build the column containing the description of the object
         TableCellBuilder descCol = row.startTD();
         String title = rowValue.rObject.getValue();
         if (!title.equals("NO_VALUE"))
         {
            if (rowValue.isPromise())
            {
               title += " (unevaluated promise)";
            }
            descCol.title(title);
         }
         String descriptionStyle = style.valueCol();
         if (rowValue.isPromise())
         {
            descriptionStyle += (" " + style.unevaluatedPromise());
         }
         else if (rowValue.getCategory() == RObjectEntry.Categories.Data)
         {
            descriptionStyle += (" " + style.dataFrameValueCol());
         }
         descCol.className(descriptionStyle);
         renderCell(descCol, createContext(2), objectDescriptionColumn_, rowValue);
         descCol.endTD();
      }

      private void buildRowHeader(RObjectEntry rowValue, int absRowIndex)
      {
         // if building the first row, we need to add a dummy row to the top.
         // since the grid uses a fixed table layout, the first row sets the
         // column widths, so we can't let the first row be a spanning header.
         if (absRowIndex == 0)
         {
            TableRowBuilder widthSettingRow = startRow().className(
                    style.widthSettingRow());
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
            TableRowBuilder leaderRow = startRow().className(
                    style.categoryHeaderRow());
            TableCellBuilder objectHeader = leaderRow.startTD();
            objectHeader.colSpan(3)
                    .className(style.categoryHeaderText())
                    .text(categoryTitle)
                    .endTD();
            leaderRow.endTR();
         }
      }

      // draw additional rows when the row has been expanded
      private void buildExpandedContentRow(RObjectEntry rowValue)
      {
         JsArrayString contents = rowValue.rObject.getContents();

         for (int idx = 0; idx < contents.length(); idx++)
         {
            TableRowBuilder detail = startRow().className(style.detailRow());
            detail.startTD().endTD();
            TableCellBuilder objectDetail = detail.startTD();
            String content = contents.get(idx);
            // ignore the first two characters of output
            // ("$ value:" becomes "value:")
            content = content.substring(2, content.length()).trim();
            objectDetail.colSpan(2)
                    .title(content)
                    .text(content)
                    .endTD();
            detail.endTR();
         }
      }
   }

   private final static String EMPTY_GLOBAL_ENVIRONMENT_MESSAGE =
           "Global environment is empty";
   private final static String EMPTY_FUNCTION_ENVIRONMENT_MESSAGE =
           "Function environment is empty";

   @UiField HTMLPanel environmentContents;
   @UiField EnvironmentStyle style;

   ScrollingDataGrid<RObjectEntry> objectList_;
   Label environmentName_;
   Label environmentEmptyMessage_;

   private Column<RObjectEntry, String> objectExpandColumn_;
   private Column<RObjectEntry, String> objectNameColumn_;
   private Column<RObjectEntry, String> objectDescriptionColumn_;
   private ListDataProvider<RObjectEntry> objectDataProvider_;

   private Observer observer_;
   private int contextDepth_;

   // deferred settings--set on load but not applied until we have data.
   private int deferredScrollPosition_;
   private JsArrayString deferredExpandedObjects_;
}
