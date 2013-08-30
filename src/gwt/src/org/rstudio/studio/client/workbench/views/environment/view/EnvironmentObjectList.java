package org.rstudio.studio.client.workbench.views.environment.view;

import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.studio.client.workbench.views.environment.view.RObjectEntry.Categories;

import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.builder.shared.TableCellBuilder;
import com.google.gwt.dom.builder.shared.TableRowBuilder;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.text.shared.AbstractSafeHtmlRenderer;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.AbstractCellTableBuilder;
import com.google.gwt.user.cellview.client.Column;

public class EnvironmentObjectList extends EnvironmentObjectDisplay
{
   public interface Style extends CssResource
   {
      String categoryHeaderRow();
      String expandIcon();
      String unclickableIcon();
      String unevaluatedPromise();
      String widthSettingRow();
      String expandCol();
      String nameCol();
      String valueCol();
      String categoryHeaderText();
      String clickableCol();
      String dataFrameValueCol();
      String detailRow();
   }

   public interface Resources extends ClientBundle
   {
      @Source("EnvironmentObjectList.css")
      Style style();
   }

   public EnvironmentObjectList(EnvironmentObjectDisplay.Host host,
                                EnvironmentObjectsObserver observer)
   {
      super(host, observer);
      setTableBuilder(new EnvironmentObjectTableBuilder(this));
      createColumns();
      addColumn(objectExpandColumn_);
      addColumn(objectNameColumn_);
      addColumn(objectDescriptionColumn_);
      setSkipRowHoverCheck(true);
      style_ = ((Resources)GWT.create(Resources.class)).style();
      style_.ensureInjected();
   }

   private void createColumns()
   {
      createExpandColumn();
      createNameColumn(filterRenderer_);
      createDescriptionColumn(filterRenderer_);
   }

   // attaches a handler to a column that invokes the associated object
   private void attachClickToInvoke(Column<RObjectEntry, String> column)
   {
      column.setFieldUpdater(new FieldUpdater<RObjectEntry, String>()
      {
         @Override
         public void update(int index, RObjectEntry object, String value)
         {
            if (object.getCategory() == RObjectEntry.Categories.Data &&
                host_.enableClickableObjects())
            {
               observer_.viewObject(object.rObject.getName());
            }
         }
      });
   }

   private void createNameColumn(SafeHtmlRenderer<String> renderer)
   {
      // the name of the object (simple text column)
      objectNameColumn_ = new Column<RObjectEntry, String>(
              new ClickableTextCell(renderer))
              {
                  @Override
                  public String getValue(RObjectEntry object)
                  {
                     return object.rObject.getName();
                  }
              };
      attachClickToInvoke(objectNameColumn_);
   }

   private void createDescriptionColumn(SafeHtmlRenderer<String> renderer)
   {
      // the description *or* value of the object; when clicked, we'll view
      // or edit the data inside the object.
      objectDescriptionColumn_ = new Column<RObjectEntry, String>(
              new ClickableTextCell(renderer))
              {
                  @Override
                  public String getValue(RObjectEntry object)
                  {
                     String val = object.rObject.getValue();
                     return val == RObjectEntry.NO_VALUE ?
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
               sb.appendHtmlConstant(object);
               return sb.toSafeHtml();
            }
         };
      objectExpandColumn_ = new Column<RObjectEntry, String>(
         new ClickableTextCell(expanderRenderer))
         {
            @Override
            public String getValue(RObjectEntry object)
            {
               String imageUri = "";
               String imageStyle = style_.expandIcon();
               if (object.canExpand())
               {
                  ImageResource expandImage = object.expanded ?
                      EnvironmentResources.INSTANCE.collapseIcon() :
                      EnvironmentResources.INSTANCE.expandIcon();

                  imageUri = expandImage.getSafeUri().asString();
               }
               else if (object.hasTraceInfo())
               {
                  imageUri = EnvironmentResources.INSTANCE
                        .tracedFunction().getSafeUri().asString();
                  imageStyle += (" " + style_.unclickableIcon());
               }
               if (imageUri.length() > 0)
               {
                  return "<input type=\"image\" src=\"" + imageUri + "\" " +
                         "class=\"" + imageStyle + "\" />";                        
               }
               return "";
            }
         };
      objectExpandColumn_.setFieldUpdater(
              new FieldUpdater<RObjectEntry, String>()
              {
                 @Override
                 public void update(int index,
                                    RObjectEntry object,
                                    String value)
                 {
                    if (object.canExpand())
                    {
                       object.expanded = !object.expanded;
                       // tell the observer this happened, so it can persist
                       // the state
                       if (host_.useStatePersistence())
                       {
                          if (object.expanded)
                          {
                             observer_.setObjectExpanded(object.rObject
                                                                 .getName());
                          }
                          else
                          {
                             observer_.setObjectCollapsed(object.rObject
                                                                  .getName());
                          }
                       }
                       redrawRow(index);
                    }
                 }
              });
   }

   // builds individual rows of the object table
   private class EnvironmentObjectTableBuilder
           extends AbstractCellTableBuilder<RObjectEntry>
   {
      public EnvironmentObjectTableBuilder(
             AbstractCellTable<RObjectEntry> cellTable)
      {
         super(cellTable);
      }

      // (re)build the given row
      public void buildRowImpl(RObjectEntry rowValue, int absRowIndex)
      {
         // build nothing for invisible rows
         if (!rowValue.visible)
            return;
         
         // build the header for the row (if any)
         buildRowHeader(rowValue, absRowIndex);

         TableRowBuilder row = startRow();

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
         expandCol.className(style_.expandCol());
         renderCell(expandCol, createContext(0), objectExpandColumn_, rowValue);
         expandCol.endTD();
      }

      private void buildNameColumn(RObjectEntry rowValue, TableRowBuilder row)
      {
         TableCellBuilder nameCol = row.startTD();
         String styleName = style_.nameCol();
         if (rowValue.getCategory() == Categories.Data &&
             host_.enableClickableObjects())
         {
            styleName += (" " + style_.clickableCol());
         }
         String size = rowValue.rObject.getSize() > 0 ?
                              ", " + rowValue.rObject.getSize() + " bytes" :
                              "";
         nameCol.className(styleName);
         nameCol.title(
                 rowValue.rObject.getName() +
                 " (" + rowValue.rObject.getType() + size + ")");
         renderCell(nameCol, createContext(1), objectNameColumn_, rowValue);
         nameCol.endTD();
      }

      private void buildDescriptionColumn(RObjectEntry rowValue,
                                          TableRowBuilder row)
      {
         // build the column containing the description of the object
         TableCellBuilder descCol = row.startTD();
         String title = rowValue.rObject.getValue();
         if ((!title.equals(RObjectEntry.NO_VALUE)) &&
             title != null)
         {
            if (rowValue.isPromise())
            {
               title += " (unevaluated promise)";
            }
            descCol.title(title);
         }
         String descriptionStyle = style_.valueCol();
         if (rowValue.isPromise())
         {
            descriptionStyle += (" " + style_.unevaluatedPromise());
         }
         else if (rowValue.getCategory() == RObjectEntry.Categories.Data &&
             host_.enableClickableObjects())
         {
            descriptionStyle += (" " +
                                 style_.dataFrameValueCol() + " " +
                                 style_.clickableCol());
         }
         if (rowValue.getCategory() == RObjectEntry.Categories.Data)
         {
            descriptionStyle += (" " + 
                                ThemeStyles.INSTANCE.environmentDataFrameCol());
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
         if (rowValue.isFirstObject)
         {
            TableRowBuilder widthSettingRow = startRow().className(
                    style_.widthSettingRow());
            widthSettingRow.startTD().className(style_.expandCol()).endTD();
            widthSettingRow.startTD().className(style_.nameCol()).endTD();
            widthSettingRow.startTD().className(style_.valueCol()).endTD();
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
                    style_.categoryHeaderRow());
            TableCellBuilder objectHeader = leaderRow.startTD();
            objectHeader.colSpan(3)
                    .className(style_.categoryHeaderText())
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
            TableRowBuilder detail = startRow().className(style_.detailRow());
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
   
   private Style style_;

   private Column<RObjectEntry, String> objectExpandColumn_;
   private Column<RObjectEntry, String> objectNameColumn_;
   private Column<RObjectEntry, String> objectDescriptionColumn_;
}
