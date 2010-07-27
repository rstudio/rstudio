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

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.CompositeCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.cell.client.HasCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.sample.showcase.client.content.cell.ContactDatabase.Category;
import com.google.gwt.sample.showcase.client.content.cell.ContactDatabase.ContactInfo;
import com.google.gwt.sample.showcase.client.content.cell.CwCellList.ContactCell;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.view.client.ListViewAdapter;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.TreeViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The {@link TreeViewModel} used to organize contacts into a hierarchy.
 */
public class ContactTreeViewModel implements TreeViewModel {

  /**
   * The images used for this example.
   */
  static interface Images extends ClientBundle {
    ImageResource contact();

    ImageResource contactsGroup();
  }

  /**
   * The cell used to render categories.
   */
  private static class CategoryCell extends AbstractCell<Category> {

    /**
     * The html of the image used for contacts.
     */
    private final String imageHtml;

    public CategoryCell(ImageResource image) {
      this.imageHtml = AbstractImagePrototype.create(image).getHTML();
    }

    @Override
    public void render(Category value, Object key, StringBuilder sb) {
      if (value != null) {
        sb.append(imageHtml).append(" ");
        sb.append(value.getDisplayName());
      }
    }
  }

  /**
   * Tracks the number of contacts in a category that begin with the same
   * letter.
   */
  private static class LetterCount implements Comparable<LetterCount> {
    private final Category category;
    private final char firstLetter;
    private int count;

    /**
     * Construct a new {@link LetterCount} for one contact.
     *
     * @param category the category
     * @param firstLetter the first letter of the contacts name
     */
    public LetterCount(Category category, char firstLetter) {
      this.category = category;
      this.firstLetter = firstLetter;
      this.count = 1;
    }

    public int compareTo(LetterCount o) {
      return (o == null) ? -1 : (firstLetter - o.firstLetter);
    }

    /**
     * Increment the count.
     */
    public void increment() {
      count++;
    }
  }

  /**
   * A Cell used to render the LetterCount.
   */
  private static class LetterCountCell extends AbstractCell<LetterCount> {

    @Override
    public void render(LetterCount value, Object key, StringBuilder sb) {
      if (value != null) {
        sb.append(value.firstLetter);
        sb.append(" (").append(value.count).append(")");
      }
    }
  }

  /**
   * The static images used in this model.
   */
  private static Images images;

  private final ListViewAdapter<Category> categoryAdapter;
  private final Cell<ContactInfo> contactCell;
  private final SelectionModel<ContactInfo> selectionModel;

  public ContactTreeViewModel(
      final SelectionModel<ContactInfo> selectionModel) {
    this.selectionModel = selectionModel;
    if (images == null) {
      images = GWT.create(Images.class);
    }

    // Create an adapter that provides categories.
    categoryAdapter = new ListViewAdapter<Category>();
    List<Category> categoryList = categoryAdapter.getList();
    for (Category category : ContactDatabase.get().queryCategories()) {
      categoryList.add(category);
    }

    // Construct a composite cell for contacts that includes a checkbox.
    List<HasCell<ContactInfo, ?>> hasCells = new ArrayList<
        HasCell<ContactInfo, ?>>();
    hasCells.add(new HasCell<ContactInfo, Boolean>() {
      public Cell<Boolean> getCell() {
        return new CheckboxCell(true);
      }

      public FieldUpdater<ContactInfo, Boolean> getFieldUpdater() {
        return new FieldUpdater<ContactInfo, Boolean>() {
          public void update(int index, ContactInfo object, Boolean value) {
            selectionModel.setSelected(object, value);
          }
        };
      }

      public Boolean getValue(ContactInfo object) {
        return selectionModel.isSelected(object);
      }
    });
    hasCells.add(new HasCell<ContactInfo, ContactInfo>() {
      public Cell<ContactInfo> getCell() {
        return new ContactCell(images.contact());
      }

      public FieldUpdater<ContactInfo, ContactInfo> getFieldUpdater() {
        return null;
      }

      public ContactInfo getValue(ContactInfo object) {
        return object;
      }
    });
    contactCell = new CompositeCell<ContactInfo>(hasCells) {
      @Override
      public void render(ContactInfo value, Object key, StringBuilder sb) {
        sb.append("<table><tbody><tr>");
        super.render(value, key, sb);
        sb.append("</tr></tbody></table>");
      }

      @Override
      protected Element getContainerElement(Element parent) {
        // Return the first TR element in the table.
        return parent.getFirstChildElement().getFirstChildElement().getFirstChildElement();
      }

      @Override
      protected <X> void render(ContactInfo value, Object key, StringBuilder sb,
          HasCell<ContactInfo, X> hasCell) {
        Cell<X> cell = hasCell.getCell();
        sb.append("<td>");
        cell.render(hasCell.getValue(value), key, sb);
        sb.append("</td>");
      }
    };
  }

  public <T> NodeInfo<?> getNodeInfo(T value) {
    if (value == null) {
      // Return top level categories.
      return new DefaultNodeInfo<Category>(
          categoryAdapter, new CategoryCell(images.contactsGroup()));
    } else if (value instanceof Category) {
      // Return the first letters of each first name.
      Category category = (Category) value;
          List<ContactInfo> contacts = ContactDatabase.get().queryContactsByCategory(category);
      Map<Character, LetterCount> counts = new TreeMap<
          Character, LetterCount>();
      for (ContactInfo contact : contacts) {
        Character first = contact.getFirstName().charAt(0);
        LetterCount count = counts.get(first);
        if (count == null) {
          count = new LetterCount(category, first);
          counts.put(first, count);
        } else {
          count.increment();
        }
      }
      List<LetterCount> orderedCounts = new ArrayList<LetterCount>(
          counts.values());
      return new DefaultNodeInfo<LetterCount>(
          new ListViewAdapter<LetterCount>(orderedCounts),
          new LetterCountCell());
    } else if (value instanceof LetterCount) {
      // Return the contacts with the specified character and first name.
      LetterCount count = (LetterCount) value;
          List<ContactInfo> contacts = ContactDatabase.get().queryContactsByCategoryAndFirstName(count.category, count.firstLetter + "");
      ListViewAdapter<ContactInfo> adapter = new ListViewAdapter<ContactInfo>(
          contacts);
      adapter.setKeyProvider(ContactInfo.KEY_PROVIDER);
      return new DefaultNodeInfo<ContactInfo>(
          adapter, contactCell, selectionModel, null);
    }

    // Unhandled type.
    String type = value.getClass().getName();
    throw new IllegalArgumentException("Unsupported object type: " + type);
  }

  public boolean isLeaf(Object value) {
    return value instanceof ContactInfo;
  }
}
