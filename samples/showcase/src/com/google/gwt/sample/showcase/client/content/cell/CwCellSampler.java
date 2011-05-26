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
package com.google.gwt.sample.showcase.client.content.cell;

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
import com.google.gwt.cell.client.ImageCell;
import com.google.gwt.cell.client.NumberCell;
import com.google.gwt.cell.client.SelectionCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.cell.client.TextInputCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.sample.showcase.client.ContentWidget;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseData;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseRaw;
import com.google.gwt.sample.showcase.client.ShowcaseAnnotations.ShowcaseSource;
import com.google.gwt.sample.showcase.client.content.cell.ContactDatabase.Category;
import com.google.gwt.sample.showcase.client.content.cell.ContactDatabase.ContactInfo;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Example file.
 */
@ShowcaseRaw({"ContactDatabase.java", "CwCellSampler.ui.xml"})
public class CwCellSampler extends ContentWidget {

  /**
   * The UiBinder interface used by this example.
   */
  @ShowcaseSource
  interface Binder extends UiBinder<Widget, CwCellSampler> {
  }

  /**
   * The constants used in this Content Widget.
   */
  @ShowcaseSource
  public static interface CwConstants extends Constants {
    String cwCellSamplerDescription();

    String cwCellSamplerName();
  }

  /**
   * The images used for this example.
   */
  @ShowcaseSource
  static interface Images extends ClientBundle {
    ImageResource contactsGroup();
  }

  /**
   * Get a cell value from a record.
   * 
   * @param <C> the cell type
   */
  @ShowcaseSource
  private static interface GetValue<C> {
    C getValue(ContactInfo contact);
  }

  /**
   * A pending change to a {@link ContactInfo}. Changes aren't committed
   * immediately to illustrate that cells can remember their pending changes.
   * 
   * @param <T> the data type being changed
   */
  @ShowcaseSource
  private abstract static class PendingChange<T> {
    private final ContactInfo contact;
    private final T value;

    public PendingChange(ContactInfo contact, T value) {
      this.contact = contact;
      this.value = value;
    }

    /**
     * Commit the change to the contact.
     */
    public void commit() {
      doCommit(contact, value);
    }

    /**
     * Update the appropriate field in the {@link ContactInfo}.
     * 
     * @param contact the contact to update
     * @param value the new value
     */
    protected abstract void doCommit(ContactInfo contact, T value);
  }

  /**
   * Updates the birthday.
   */
  @ShowcaseSource
  private static class BirthdayChange extends PendingChange<Date> {

    public BirthdayChange(ContactInfo contact, Date value) {
      super(contact, value);
    }

    @Override
    protected void doCommit(ContactInfo contact, Date value) {
      contact.setBirthday(value);
    }
  }

  /**
   * Updates the category.
   */
  @ShowcaseSource
  private static class CategoryChange extends PendingChange<Category> {

    public CategoryChange(ContactInfo contact, Category value) {
      super(contact, value);
    }

    @Override
    protected void doCommit(ContactInfo contact, Category value) {
      contact.setCategory(value);
    }
  }

  /**
   * Updates the first name.
   */
  @ShowcaseSource
  private static class FirstNameChange extends PendingChange<String> {

    public FirstNameChange(ContactInfo contact, String value) {
      super(contact, value);
    }

    @Override
    protected void doCommit(ContactInfo contact, String value) {
      contact.setFirstName(value);
    }
  }

  /**
   * Updates the last name.
   */
  @ShowcaseSource
  private static class LastNameChange extends PendingChange<String> {

    public LastNameChange(ContactInfo contact, String value) {
      super(contact, value);
    }

    @Override
    protected void doCommit(ContactInfo contact, String value) {
      contact.setLastName(value);
    }
  }

  /**
   * The main CellTable.
   */
  @ShowcaseData
  @UiField(provided = true)
  DataGrid<ContactInfo> contactList;

  /**
   * The commit button.
   */
  @ShowcaseData
  @UiField
  Button commitButton;

  /**
   * The redraw button.
   */
  @ShowcaseData
  @UiField
  Button redrawButton;

  /**
   * The list of cells that are editable.
   */
  @ShowcaseData
  private List<AbstractEditableCell<?, ?>> editableCells;

  /**
   * The list of pending changes.
   */
  @ShowcaseData
  private List<PendingChange<?>> pendingChanges = new ArrayList<PendingChange<?>>();

  /**
   * Constructor.
   * 
   * @param constants the constants
   */
  public CwCellSampler(CwConstants constants) {
    super(constants.cwCellSamplerName(), constants.cwCellSamplerDescription(), false,
        "ContactDatabase.java", "CwCellSampler.ui.xml");
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
    Images images = GWT.create(Images.class);

    // Create the table.
    editableCells = new ArrayList<AbstractEditableCell<?, ?>>();
    contactList = new DataGrid<ContactInfo>(25, ContactInfo.KEY_PROVIDER);
    contactList.setMinimumTableWidth(140, Unit.EM);
    ContactDatabase.get().addDataDisplay(contactList);

    // CheckboxCell.
    final Category[] categories = ContactDatabase.get().queryCategories();
    addColumn(new CheckboxCell(), "Checkbox", new GetValue<Boolean>() {
      @Override
      public Boolean getValue(ContactInfo contact) {
        // Checkbox indicates that the contact is a relative.
        // Index 0 = Family.
        return contact.getCategory() == categories[0];
      }
    }, new FieldUpdater<ContactInfo, Boolean>() {
      @Override
      public void update(int index, ContactInfo object, Boolean value) {
        if (value) {
          // If a relative, use the Family Category.
          pendingChanges.add(new CategoryChange(object, categories[0]));
        } else {
          // If not a relative, use the Contacts Category.
          pendingChanges.add(new CategoryChange(object, categories[categories.length - 1]));
        }
      }
    });

    // TextCell.
    addColumn(new TextCell(), "Text", new GetValue<String>() {
      @Override
      public String getValue(ContactInfo contact) {
        return contact.getFullName();
      }
    }, null);

    // EditTextCell.
    Column<ContactInfo, String> editTextColumn =
        addColumn(new EditTextCell(), "EditText", new GetValue<String>() {
          @Override
          public String getValue(ContactInfo contact) {
            return contact.getFirstName();
          }
        }, new FieldUpdater<ContactInfo, String>() {
          @Override
          public void update(int index, ContactInfo object, String value) {
            pendingChanges.add(new FirstNameChange(object, value));
          }
        });
    contactList.setColumnWidth(editTextColumn, 16.0, Unit.EM);

    // TextInputCell.
    Column<ContactInfo, String> textInputColumn =
        addColumn(new TextInputCell(), "TextInput", new GetValue<String>() {
          @Override
          public String getValue(ContactInfo contact) {
            return contact.getLastName();
          }
        }, new FieldUpdater<ContactInfo, String>() {
          @Override
          public void update(int index, ContactInfo object, String value) {
            pendingChanges.add(new LastNameChange(object, value));
          }
        });
    contactList.setColumnWidth(textInputColumn, 16.0, Unit.EM);

    // ClickableTextCell.
    addColumn(new ClickableTextCell(), "ClickableText", new GetValue<String>() {
      @Override
      public String getValue(ContactInfo contact) {
        return "Click " + contact.getFirstName();
      }
    }, new FieldUpdater<ContactInfo, String>() {
      @Override
      public void update(int index, ContactInfo object, String value) {
        Window.alert("You clicked " + object.getFullName());
      }
    });

    // ActionCell.
    addColumn(new ActionCell<ContactInfo>("Click Me", new ActionCell.Delegate<ContactInfo>() {
      @Override
      public void execute(ContactInfo contact) {
        Window.alert("You clicked " + contact.getFullName());
      }
    }), "Action", new GetValue<ContactInfo>() {
      @Override
      public ContactInfo getValue(ContactInfo contact) {
        return contact;
      }
    }, null);

    // ButtonCell.
    addColumn(new ButtonCell(), "Button", new GetValue<String>() {
      @Override
      public String getValue(ContactInfo contact) {
        return "Click " + contact.getFirstName();
      }
    }, new FieldUpdater<ContactInfo, String>() {
      @Override
      public void update(int index, ContactInfo object, String value) {
        Window.alert("You clicked " + object.getFullName());
      }
    });

    // DateCell.
    DateTimeFormat dateFormat = DateTimeFormat.getFormat(PredefinedFormat.DATE_MEDIUM);
    addColumn(new DateCell(dateFormat), "Date", new GetValue<Date>() {
      @Override
      public Date getValue(ContactInfo contact) {
        return contact.getBirthday();
      }
    }, null);

    // DatePickerCell.
    addColumn(new DatePickerCell(dateFormat), "DatePicker", new GetValue<Date>() {
      @Override
      public Date getValue(ContactInfo contact) {
        return contact.getBirthday();
      }
    }, new FieldUpdater<ContactInfo, Date>() {
      @Override
      public void update(int index, ContactInfo object, Date value) {
        pendingChanges.add(new BirthdayChange(object, value));
      }
    });

    // NumberCell.
    Column<ContactInfo, Number> numberColumn =
        addColumn(new NumberCell(), "Number", new GetValue<Number>() {
          @Override
          public Number getValue(ContactInfo contact) {
            return contact.getAge();
          }
        }, null);
    numberColumn.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LOCALE_END);

    // IconCellDecorator.
    addColumn(new IconCellDecorator<String>(images.contactsGroup(), new TextCell()), "Icon",
        new GetValue<String>() {
          @Override
          public String getValue(ContactInfo contact) {
            return contact.getCategory().getDisplayName();
          }
        }, null);

    // ImageCell.
    addColumn(new ImageCell(), "Image", new GetValue<String>() {
      @Override
      public String getValue(ContactInfo contact) {
        return "contact.jpg";
      }
    }, null);

    // SelectionCell.
    List<String> options = new ArrayList<String>();
    for (Category category : categories) {
      options.add(category.getDisplayName());
    }
    addColumn(new SelectionCell(options), "Selection", new GetValue<String>() {
      @Override
      public String getValue(ContactInfo contact) {
        return contact.getCategory().getDisplayName();
      }
    }, new FieldUpdater<ContactInfo, String>() {
      @Override
      public void update(int index, ContactInfo object, String value) {
        for (Category category : categories) {
          if (category.getDisplayName().equals(value)) {
            pendingChanges.add(new CategoryChange(object, category));
            break;
          }
        }
      }
    });

    // Create the UiBinder.
    Binder uiBinder = GWT.create(Binder.class);
    Widget widget = uiBinder.createAndBindUi(this);

    // Add handlers to redraw or refresh the table.
    redrawButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        contactList.redraw();
      }
    });
    commitButton.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        // Commit the changes.
        for (PendingChange<?> pendingChange : pendingChanges) {
          pendingChange.commit();
        }
        pendingChanges.clear();

        // Push the changes to the views.
        ContactDatabase.get().refreshDisplays();
      }
    });

    return widget;
  }

  @Override
  protected void asyncOnInitialize(final AsyncCallback<Widget> callback) {
    GWT.runAsync(CwCellSampler.class, new RunAsyncCallback() {

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

  /**
   * Add a column with a header.
   * 
   * @param <C> the cell type
   * @param cell the cell used to render the column
   * @param headerText the header string
   * @param getter the value getter for the cell
   */
  @ShowcaseSource
  private <C> Column<ContactInfo, C> addColumn(Cell<C> cell, String headerText,
      final GetValue<C> getter, FieldUpdater<ContactInfo, C> fieldUpdater) {
    Column<ContactInfo, C> column = new Column<ContactInfo, C>(cell) {
      @Override
      public C getValue(ContactInfo object) {
        return getter.getValue(object);
      }
    };
    column.setFieldUpdater(fieldUpdater);
    if (cell instanceof AbstractEditableCell<?, ?>) {
      editableCells.add((AbstractEditableCell<?, ?>) cell);
    }
    contactList.addColumn(column, headerText);
    return column;
  }
}
