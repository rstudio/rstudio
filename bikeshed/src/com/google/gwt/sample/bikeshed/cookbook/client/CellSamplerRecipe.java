/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.sample.bikeshed.cookbook.client;

import com.google.gwt.cell.client.AbstractEditableCell;
import com.google.gwt.cell.client.ActionCell;
import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.cell.client.DateCell;
import com.google.gwt.cell.client.DatePickerCell;
import com.google.gwt.cell.client.EditTextCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.IconCellDecorator;
import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.cell.client.SelectionCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.cell.client.TextInputCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListViewAdapter;
import com.google.gwt.view.client.ProvidesKey;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A table containing most cell types.
 *
 * TODO (jlabanca): Refactor this sample to use more realistic data.
 */
public class CellSamplerRecipe extends Recipe {

  /**
   * A generic status.
   */
  private static enum Status {
    ACTIVE("Active"), INACTIVE("Inactive"), ARCHIVED("Archived");

    private final String displayName;

    private Status(String displayName) {
      this.displayName = displayName;
    }

    public String getDisplayName() {
      return displayName;
    }
  }

  /**
   * The images used for this recipe.
   */
  static interface Images extends ClientBundle {
    ImageResource gwtLogo();
  }

  /**
   * Get a cell value from a record.
   *
   * @param <C> the cell type
   */
  private static interface GetValue<C> {
    C getValue(Record record);
  }

  /**
   * The record object used for each row.
   */
  @SuppressWarnings("deprecation")
  private static class Record implements Comparable<Record> {

    private final int id;
    private Date date;
    private Status status;
    private String text;

    /**
     * The pending values are set by field updaters until they are committed.
     */
    private Status pendingStatus;
    private Date pendingDate;
    private String pendingText;

    public Record(int id) {
      this.id = id;
      this.date = new Date(2010 - 1900, 1, id);
      this.text = "Item " + id;
      this.status = Status.ACTIVE;
    }

    public void commit() {
      if (pendingDate != null) {
        date = pendingDate;
        pendingDate = null;
      }
      if (pendingText != null) {
        text = pendingText;
        pendingText = null;
      }
      if (pendingStatus != null) {
        status = pendingStatus;
        pendingStatus = null;
      }
    }

    public int compareTo(Record o) {
      return o == null ? 1 : id - o.id;
    }

    public void setDate(Date date) {
      this.pendingDate = date;
    }

    public void setStatus(Status status) {
      this.pendingStatus = status;
    }

    public void setText(String text) {
      this.pendingText = text;
    }
  }

  private List<AbstractEditableCell<?, ?>> editableCells;
  private CellTable<Record> table;

  public CellSamplerRecipe() {
    super("Cell Sampler");
  }

  @Override
  protected Widget createWidget() {
    Images images = GWT.create(Images.class);

    // Create the adapter.
    final ProvidesKey<Record> keyProvider = new ProvidesKey<Record>() {
      public Object getKey(Record item) {
        return item.id;
      }
    };
    final ListViewAdapter<Record> adapter = new ListViewAdapter<Record>();
    adapter.setKeyProvider(keyProvider);
    for (int i = 0; i < 10; ++i) {
      adapter.getList().add(new Record(i));
    }

    // Create the table.
    editableCells = new ArrayList<AbstractEditableCell<?, ?>>();
    table = new CellTable<Record>(10);
    table.setKeyProvider(keyProvider);
    adapter.addView(table);

    // CheckboxCell.
    addColumn(new CheckboxCell(), "CheckboxCell", new GetValue<Boolean>() {
      public Boolean getValue(Record record) {
        return Status.ACTIVE.equals(record.status);
      }
    }, new FieldUpdater<Record, Boolean>() {
      public void update(int index, Record object, Boolean value) {
        if (value) {
          object.setStatus(Status.ACTIVE);
        } else {
          object.setStatus(Status.INACTIVE);
        }
      }
    });

    // TextCell.
    addColumn(new TextCell(), "TextCell", new GetValue<String>() {
      public String getValue(Record record) {
        return record.text;
      }
    }, null);

    // ActionCell.
    addColumn(
        new ActionCell<Record>("Click Me", new ActionCell.Delegate<Record>() {
          public void execute(Record object) {
            Window.alert("You clicked #" + object.id);
          }
        }), "ActionCell", new GetValue<Record>() {
          public Record getValue(Record record) {
            return record;
          }
        }, null);

    // ButtonCell.
    addColumn(new ButtonCell(), "ButtonCell", new GetValue<String>() {
      public String getValue(Record record) {
        return "Click " + record.id;
      }
    }, new FieldUpdater<Record, String>() {
      public void update(int index, Record object, String value) {
        Window.alert("You clicked " + object.id);
      }
    });

    // ClickableTextCell.
    addColumn(
        new ClickableTextCell(), "ClickableTextCell", new GetValue<String>() {
          public String getValue(Record record) {
            return "Click " + record.id;
          }
        }, new FieldUpdater<Record, String>() {
          public void update(int index, Record object, String value) {
            Window.alert("You clicked " + object.id);
          }
        });

    // DateCell.
    DateTimeFormat dateFormat = DateTimeFormat.getFormat(
        PredefinedFormat.DATE_MEDIUM);
    addColumn(new DateCell(dateFormat), "DateCell", new GetValue<Date>() {
      public Date getValue(Record record) {
        return record.date;
      }
    }, null);

    // DatePickerCell.
    addColumn(
        new DatePickerCell(dateFormat), "DatePickerCell", new GetValue<Date>() {
          public Date getValue(Record record) {
            return record.date;
          }
        }, new FieldUpdater<Record, Date>() {
          public void update(int index, Record object, Date value) {
            object.setDate(value);
          }
        });

    // EditTextCell.
    addColumn(new EditTextCell(), "EditTextCell", new GetValue<String>() {
      public String getValue(Record record) {
        return record.text;
      }
    }, new FieldUpdater<Record, String>() {
      public void update(int index, Record object, String value) {
        object.setText(value);
      }
    });

    // IconCellDecorator.
    addColumn(new IconCellDecorator<String>(images.gwtLogo(), new TextCell()),
        "IconCellDecorator", new GetValue<String>() {
          public String getValue(Record record) {
            return record.text;
          }
        }, null);

    // NumberCell.
    addColumn(new NumberCell(), "NumberCell", new GetValue<Number>() {
      public Number getValue(Record record) {
        return record.id;
      }
    }, null);

    // SelectionCell.
    List<String> options = new ArrayList<String>();
    options.add(Status.ACTIVE.getDisplayName());
    options.add(Status.INACTIVE.getDisplayName());
    options.add(Status.ARCHIVED.getDisplayName());
    addColumn(
        new SelectionCell(options), "SelectionCell", new GetValue<String>() {
          public String getValue(Record record) {
            return record.status.getDisplayName();
          }
        }, new FieldUpdater<Record, String>() {
          public void update(int index, Record object, String value) {
            if (Status.ACTIVE.getDisplayName().equals(value)) {
              object.setStatus(Status.ACTIVE);
            } else if (Status.INACTIVE.getDisplayName().equals(value)) {
              object.setStatus(Status.INACTIVE);
            } else {
              object.setStatus(Status.ARCHIVED);
            }
          }
        });

    // TextInputCell.
    addColumn(new TextInputCell(), "TextInputCellCell", new GetValue<String>() {
      public String getValue(Record record) {
        return record.text;
      }
    }, new FieldUpdater<Record, String>() {
      public void update(int index, Record object, String value) {
        object.setText(value);
      }
    });

    // Add buttons to redraw or refresh.
    Button redrawButton = new Button("Redraw Table", new ClickHandler() {
      public void onClick(ClickEvent event) {
        table.redraw();
      }
    });
    Button commitButton = new Button("Commit Data", new ClickHandler() {
      public void onClick(ClickEvent event) {
        // Commit the changes.
        for (Record record : adapter.getList()) {
          record.commit();

          // Clear all view data.
          Object key = keyProvider.getKey(record);
          for (AbstractEditableCell<?, ?> cell : editableCells) {
            cell.setViewData(key, null);
          }
        }

        // Update the table.
        adapter.refresh();
      }
    });

    // Add the table to a flowPanel so it doesn't fill the height.
    FlowPanel fp = new FlowPanel();
    fp.add(table);
    fp.add(redrawButton);
    fp.add(commitButton);
    return fp;
  }

  /**
   * Add a column with a header.
   *
   * @param <C> the cell type
   * @param cell the cell used to render the column
   * @param headerText the header string
   * @param getter the value getter for the cell
   */
  private <C> void addColumn(Cell<C> cell, String headerText,
      final GetValue<C> getter, FieldUpdater<Record, C> fieldUpdater) {
    Column<Record, C> column = new Column<Record, C>(cell) {
      @Override
      public C getValue(Record object) {
        return getter.getValue(object);
      }
    };
    column.setFieldUpdater(fieldUpdater);
    if (cell instanceof AbstractEditableCell<?, ?>) {
      editableCells.add((AbstractEditableCell<?, ?>) cell);
    }
    table.addColumn(column, headerText);
  }
}
