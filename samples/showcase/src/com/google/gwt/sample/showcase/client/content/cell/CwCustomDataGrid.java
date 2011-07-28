/*
 * Copyright 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.sample.showcase.client.content.cell;

import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.cell.client.EditTextCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.cell.client.SelectionCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.dom.builder.shared.DivBuilder;
import com.google.gwt.dom.builder.shared.TableCellBuilder;
import com.google.gwt.dom.builder.shared.TableRowBuilder;
import com.google.gwt.dom.client.Style.OutlineStyle;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.sample.showcase.client.ContentWidget;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseData;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseRaw;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseSource;
import com.google.gwt.sample.showcase.client.content.cell.ContactDatabase.Category;
import com.google.gwt.sample.showcase.client.content.cell.ContactDatabase.ContactInfo;
import com.google.gwt.text.shared.AbstractSafeHtmlRenderer;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.AbstractCellTable.Style;
import com.google.gwt.user.cellview.client.CellTableBuilder;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.cellview.client.SafeHtmlHeader;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.SimplePager.TextLocation;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.DefaultSelectionEventManager;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.SelectionModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Example file.
 */
@ShowcaseRaw({"ContactDatabase.java", "CwCustomDataGrid.ui.xml", "CwCustomDataGrid.css"})
public class CwCustomDataGrid extends ContentWidget {

  /**
   * The constants used in this Content Widget.
   */
  @ShowcaseSource
  public static interface CwConstants extends Constants {
    String cwCustomDataGridColumnAddress();

    String cwCustomDataGridColumnAge();

    String cwCustomDataGridColumnCategory();

    String cwCustomDataGridColumnFirstName();

    String cwCustomDataGridColumnLastName();

    String cwCustomDataGridDescription();

    String cwCustomDataGridEmpty();

    String cwCustomDataGridName();
  }

  /**
   * The UiBinder interface used by this example.
   */
  @ShowcaseSource
  interface Binder extends UiBinder<Widget, CwCustomDataGrid> {
  }

  /**
   * The resources used by this example.
   */
  @ShowcaseSource
  interface Resources extends ClientBundle {

    /**
     * Get the styles used but this example.
     */
    @Source("CwCustomDataGrid.css")
    Styles styles();
  }

  /**
   * The CSS Resources used by this example.
   */
  @ShowcaseSource
  interface Styles extends CssResource {
    /**
     * Indents cells in child rows.
     */
    String childCell();
  }

  /**
   * A custom version of {@link CellTableBuilder}.
   */
  @ShowcaseSource
  private class CustomTableBuilder implements CellTableBuilder<ContactInfo> {

    private final int todayMonth;
    private final Set<Integer> showingFriends = new HashSet<Integer>();

    private final String childCell = " " + resources.styles().childCell();
    private final String rowStyle;
    private final String selectedRowStyle;
    private final String cellStyle;
    private final String selectedCellStyle;

    @SuppressWarnings("deprecation")
    public CustomTableBuilder(ListHandler<ContactInfo> sortHandler) {
      // Cache styles for faster access.
      Style style = dataGrid.getResources().style();
      rowStyle = style.evenRow();
      selectedRowStyle = " " + style.selectedRow();
      cellStyle = style.cell() + " " + style.evenRowCell();
      selectedCellStyle = " " + style.selectedRowCell();

      // Record today's date.
      Date today = new Date();
      todayMonth = today.getMonth();

      /*
       * Checkbox column.
       * 
       * This table will uses a checkbox column for selection. Alternatively,
       * you can call dataGrid.setSelectionEnabled(true) to enable mouse
       * selection.
       */
      Column<ContactInfo, Boolean> checkboxColumn =
          new Column<ContactInfo, Boolean>(new CheckboxCell(true, false)) {
            @Override
            public Boolean getValue(ContactInfo object) {
              // Get the value from the selection model.
              return dataGrid.getSelectionModel().isSelected(object);
            }
          };
      dataGrid.addColumn(checkboxColumn, SafeHtmlUtils.fromSafeConstant("<br/>"));
      dataGrid.setColumnWidth(0, 40, Unit.PX);

      // View friends.
      SafeHtmlRenderer<String> anchorRenderer = new AbstractSafeHtmlRenderer<String>() {
        @Override
        public SafeHtml render(String object) {
          SafeHtmlBuilder sb = new SafeHtmlBuilder();
          sb.appendHtmlConstant("(<a href=\"javascript:;\">").appendEscaped(object)
              .appendHtmlConstant("</a>)");
          return sb.toSafeHtml();
        }
      };
      Column<ContactInfo, String> viewFriendsColumn =
          new Column<ContactInfo, String>(new ClickableTextCell(anchorRenderer)) {
            @Override
            public String getValue(ContactInfo object) {
              if (showingFriends.contains(object.getId())) {
                return "hide friends";
              } else {
                return "show friends";
              }
            }
          };
      dataGrid.addColumn(viewFriendsColumn, SafeHtmlUtils.fromSafeConstant("<br/>"));
      viewFriendsColumn.setFieldUpdater(new FieldUpdater<ContactInfo, String>() {
        @Override
        public void update(int index, ContactInfo object, String value) {
          if (showingFriends.contains(object.getId())) {
            showingFriends.remove(object.getId());
          } else {
            showingFriends.add(object.getId());
          }

          // Redraw the modified row.
          dataGrid.redrawRow(index);
        }
      });
      dataGrid.setColumnWidth(1, 10, Unit.EM);

      // First name.
      Column<ContactInfo, String> firstNameColumn =
          new Column<ContactInfo, String>(new EditTextCell()) {
            @Override
            public String getValue(ContactInfo object) {
              return object.getFirstName();
            }
          };
      firstNameColumn.setSortable(true);
      sortHandler.setComparator(firstNameColumn, new Comparator<ContactInfo>() {
        @Override
        public int compare(ContactInfo o1, ContactInfo o2) {
          return o1.getFirstName().compareTo(o2.getFirstName());
        }
      });
      dataGrid.addColumn(firstNameColumn, constants.cwCustomDataGridColumnFirstName());
      firstNameColumn.setFieldUpdater(new FieldUpdater<ContactInfo, String>() {
        @Override
        public void update(int index, ContactInfo object, String value) {
          // Called when the user changes the value.
          object.setFirstName(value);
          ContactDatabase.get().refreshDisplays();
        }
      });
      dataGrid.setColumnWidth(2, 20, Unit.PCT);

      // Last name.
      Column<ContactInfo, String> lastNameColumn =
          new Column<ContactInfo, String>(new EditTextCell()) {
            @Override
            public String getValue(ContactInfo object) {
              return object.getLastName();
            }
          };
      lastNameColumn.setSortable(true);
      sortHandler.setComparator(lastNameColumn, new Comparator<ContactInfo>() {
        @Override
        public int compare(ContactInfo o1, ContactInfo o2) {
          return o1.getLastName().compareTo(o2.getLastName());
        }
      });
      dataGrid.addColumn(lastNameColumn, constants.cwCustomDataGridColumnLastName());
      lastNameColumn.setFieldUpdater(new FieldUpdater<ContactInfo, String>() {
        @Override
        public void update(int index, ContactInfo object, String value) {
          // Called when the user changes the value.
          object.setLastName(value);
          ContactDatabase.get().refreshDisplays();
        }
      });
      dataGrid.setColumnWidth(3, 20, Unit.PCT);

      // Age.
      Column<ContactInfo, Number> ageColumn = new Column<ContactInfo, Number>(new NumberCell()) {
        @Override
        public Number getValue(ContactInfo object) {
          return object.getAge();
        }
      };
      ageColumn.setSortable(true);
      sortHandler.setComparator(ageColumn, new Comparator<ContactInfo>() {
        @Override
        public int compare(ContactInfo o1, ContactInfo o2) {
          return o1.getAge() - o2.getAge();
        }
      });
      Header<String> ageFooter = new Header<String>(new TextCell()) {
        @Override
        public String getValue() {
          List<ContactInfo> items = dataGrid.getVisibleItems();
          if (items.size() == 0) {
            return "";
          } else {
            int totalAge = 0;
            for (ContactInfo item : items) {
              totalAge += item.getAge();
            }
            return "Avg: " + totalAge / items.size();
          }
        }
      };
      dataGrid.addColumn(ageColumn, new SafeHtmlHeader(SafeHtmlUtils.fromSafeConstant(constants
          .cwCustomDataGridColumnAge())), ageFooter);
      dataGrid.setColumnWidth(4, 7, Unit.EM);

      // Category.
      final Category[] categories = ContactDatabase.get().queryCategories();
      List<String> categoryNames = new ArrayList<String>();
      for (Category category : categories) {
        categoryNames.add(category.getDisplayName());
      }
      SelectionCell categoryCell = new SelectionCell(categoryNames);
      Column<ContactInfo, String> categoryColumn = new Column<ContactInfo, String>(categoryCell) {
        @Override
        public String getValue(ContactInfo object) {
          return object.getCategory().getDisplayName();
        }
      };
      dataGrid.addColumn(categoryColumn, constants.cwCustomDataGridColumnCategory());
      categoryColumn.setFieldUpdater(new FieldUpdater<ContactInfo, String>() {
        @Override
        public void update(int index, ContactInfo object, String value) {
          for (Category category : categories) {
            if (category.getDisplayName().equals(value)) {
              object.setCategory(category);
            }
          }
          ContactDatabase.get().refreshDisplays();
        }
      });
      dataGrid.setColumnWidth(5, 130, Unit.PX);

      // Address.
      Column<ContactInfo, String> addressColumn = new Column<ContactInfo, String>(new TextCell()) {
        @Override
        public String getValue(ContactInfo object) {
          return object.getAddress();
        }
      };
      addressColumn.setSortable(true);
      sortHandler.setComparator(addressColumn, new Comparator<ContactInfo>() {
        @Override
        public int compare(ContactInfo o1, ContactInfo o2) {
          return o1.getAddress().compareTo(o2.getAddress());
        }
      });
      dataGrid.addColumn(addressColumn, constants.cwCustomDataGridColumnAddress());
      dataGrid.setColumnWidth(6, 60, Unit.PCT);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void buildRow(ContactInfo rowValue, int absRowIndex,
        CellTableBuilder.Utility<ContactInfo> utility) {
      buildContactRow(rowValue, absRowIndex, utility, false);

      // Display information about the user in another row that spans the entire
      // table.
      if (rowValue.getAge() > 65) {
        TableRowBuilder row = utility.startRow();
        TableCellBuilder td = row.startTD().colSpan(7).className(cellStyle);
        td.style().trustedBackgroundColor("#ffa").outlineStyle(OutlineStyle.NONE).endStyle();
        td.text(rowValue.getFirstName() + " is elegible for retirement benefits").endTD();
        row.endTR();
      }

      // Display information about the user in another row that spans the entire
      // table.
      Date dob = rowValue.getBirthday();
      if (dob.getMonth() == todayMonth) {
        TableRowBuilder row = utility.startRow();
        TableCellBuilder td = row.startTD().colSpan(7).className(cellStyle);
        td.style().trustedBackgroundColor("#ccf").endStyle();
        td.text(rowValue.getFirstName() + "'s birthday is this month!").endTD();
        row.endTR();
      }

      // Display list of friends.
      if (showingFriends.contains(rowValue.getId())) {
        Set<ContactInfo> friends = ContactDatabase.get().queryFriends(rowValue);
        for (ContactInfo friend : friends) {
          buildContactRow(friend, absRowIndex, utility, true);
        }
      }
    }

    /**
     * Build a row.
     * 
     * @param rowValue the contact info
     * @param absRowIndex the absolute row index
     * @param utility the utility used to add rows and Cells
     * @param isFriend true if this is a subrow, false if a top level row
     */
    @SuppressWarnings("deprecation")
    private void buildContactRow(ContactInfo rowValue, int absRowIndex,
        CellTableBuilder.Utility<ContactInfo> utility, boolean isFriend) {
      // Calculate the row styles.
      SelectionModel<? super ContactInfo> selectionModel = dataGrid.getSelectionModel();
      boolean isSelected =
          (selectionModel == null || rowValue == null) ? false : selectionModel
              .isSelected(rowValue);
      boolean isEven = absRowIndex % 2 == 0;
      StringBuilder trClasses = new StringBuilder(rowStyle);
      if (isSelected) {
        trClasses.append(selectedRowStyle);
      }

      // Calculate the cell styles.
      String cellStyles = cellStyle;
      if (isSelected) {
        cellStyles += selectedCellStyle;
      }
      if (isFriend) {
        cellStyles += childCell;
      }

      TableRowBuilder row = utility.startRow();
      row.className(trClasses.toString());

      /*
       * Checkbox column.
       * 
       * This table will uses a checkbox column for selection. Alternatively,
       * you can call dataGrid.setSelectionEnabled(true) to enable mouse
       * selection.
       */
      TableCellBuilder td = row.startTD();
      td.className(cellStyles);
      td.style().outlineStyle(OutlineStyle.NONE).endStyle();
      if (!isFriend) {
        utility.renderCell(td, utility.createContext(0), dataGrid.getColumn(0), rowValue);
      }
      td.endTD();

      /*
       * View friends column.
       * 
       * Displays a link to "show friends". When clicked, the list of friends is
       * displayed below the contact.
       */
      td = row.startTD();
      td.className(cellStyles);
      if (!isFriend) {
        td.style().outlineStyle(OutlineStyle.NONE).endStyle();
        utility.renderCell(td, utility.createContext(1), dataGrid.getColumn(1), rowValue);
      }
      td.endTD();

      // First name column.
      td = row.startTD();
      td.className(cellStyles);
      td.style().outlineStyle(OutlineStyle.NONE).endStyle();
      if (isFriend) {
        td.text(rowValue.getFirstName());
      } else {
        utility.renderCell(td, utility.createContext(2), dataGrid.getColumn(2), rowValue);
      }
      td.endTD();

      // Last name column.
      td = row.startTD();
      td.className(cellStyles);
      td.style().outlineStyle(OutlineStyle.NONE).endStyle();
      if (isFriend) {
        td.text(rowValue.getLastName());
      } else {
        utility.renderCell(td, utility.createContext(3), dataGrid.getColumn(3), rowValue);
      }
      td.endTD();

      // Age column.
      td = row.startTD();
      td.className(cellStyles);
      td.style().outlineStyle(OutlineStyle.NONE).endStyle();
      td.text(NumberFormat.getDecimalFormat().format(rowValue.getAge())).endTD();

      // Category column.
      td = row.startTD();
      td.className(cellStyles);
      td.style().outlineStyle(OutlineStyle.NONE).endStyle();
      if (isFriend) {
        td.text(rowValue.getCategory().getDisplayName());
      } else {
        utility.renderCell(td, utility.createContext(5), dataGrid.getColumn(5), rowValue);
      }
      td.endTD();

      // Address column.
      td = row.startTD();
      td.className(cellStyles);
      DivBuilder div = td.startDiv();
      div.style().outlineStyle(OutlineStyle.NONE).endStyle();
      div.text(rowValue.getAddress()).endDiv();
      td.endTD();

      row.endTR();
    }
  }

  /**
   * The main DataGrid.
   */
  @ShowcaseData
  @UiField(provided = true)
  DataGrid<ContactInfo> dataGrid;

  /**
   * The pager used to change the range of data.
   */
  @ShowcaseData
  @UiField(provided = true)
  SimplePager pager;

  /**
   * An instance of the constants.
   */
  @ShowcaseData
  private final CwConstants constants;

  /**
   * The resources used by this example.
   */
  @ShowcaseData
  private Resources resources;

  /**
   * Constructor.
   * 
   * @param constants the constants
   */
  public CwCustomDataGrid(CwConstants constants) {
    super(constants.cwCustomDataGridName(), constants.cwCustomDataGridDescription(), false,
        "ContactDatabase.java", "CwCustomDataGrid.ui.xml", "CwCustomDataGrid.css");
    this.constants = constants;
  }

  @Override
  public boolean hasMargins() {
    return false;
  }

  @Override
  public boolean hasScrollableContent() {
    return false;
  }

  /**
   * Initialize this example.
   */
  @ShowcaseSource
  @Override
  public Widget onInitialize() {
    resources = GWT.create(Resources.class);
    resources.styles().ensureInjected();

    // Create a DataGrid.

    // Set a key provider that provides a unique key for each contact. If key is
    // used to identify contacts when fields (such as the name and address)
    // change.
    dataGrid = new DataGrid<ContactInfo>(ContactDatabase.ContactInfo.KEY_PROVIDER);
    dataGrid.setWidth("100%");

    // Set the message to display when the table is empty.
    dataGrid.setEmptyTableWidget(new Label(constants.cwCustomDataGridEmpty()));

    // Attach a column sort handler to the ListDataProvider to sort the list.
    ListHandler<ContactInfo> sortHandler =
        new ListHandler<ContactInfo>(ContactDatabase.get().getDataProvider().getList());
    dataGrid.addColumnSortHandler(sortHandler);

    // Create a Pager to control the table.
    SimplePager.Resources pagerResources = GWT.create(SimplePager.Resources.class);
    pager = new SimplePager(TextLocation.CENTER, pagerResources, false, 0, true);
    pager.setDisplay(dataGrid);

    // Add a selection model so we can select cells.
    final SelectionModel<ContactInfo> selectionModel =
        new MultiSelectionModel<ContactInfo>(ContactDatabase.ContactInfo.KEY_PROVIDER);
    dataGrid.setSelectionModel(selectionModel, DefaultSelectionEventManager
        .<ContactInfo> createCheckboxManager());

    // Specify a custom table.
    dataGrid.setTableBuilder(new CustomTableBuilder(sortHandler));

    // Add the CellList to the adapter in the database.
    ContactDatabase.get().addDataDisplay(dataGrid);

    // Create the UiBinder.
    Binder uiBinder = GWT.create(Binder.class);
    return uiBinder.createAndBindUi(this);
  }

  @Override
  protected void asyncOnInitialize(final AsyncCallback<Widget> callback) {
    GWT.runAsync(CwCustomDataGrid.class, new RunAsyncCallback() {

      @Override
      public void onFailure(Throwable caught) {
        callback.onFailure(caught);
      }

      @Override
      public void onSuccess() {
        callback.onSuccess(onInitialize());
      }
    });
  }
}
