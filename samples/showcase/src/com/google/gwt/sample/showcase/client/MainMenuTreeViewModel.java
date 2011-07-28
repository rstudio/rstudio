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
package com.google.gwt.sample.showcase.client;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.prefetch.RunAsyncCode;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.sample.showcase.client.content.cell.CwCellBrowser;
import com.google.gwt.sample.showcase.client.content.cell.CwCellList;
import com.google.gwt.sample.showcase.client.content.cell.CwCellSampler;
import com.google.gwt.sample.showcase.client.content.cell.CwCellTable;
import com.google.gwt.sample.showcase.client.content.cell.CwCellTree;
import com.google.gwt.sample.showcase.client.content.cell.CwCellValidation;
import com.google.gwt.sample.showcase.client.content.cell.CwCustomDataGrid;
import com.google.gwt.sample.showcase.client.content.cell.CwDataGrid;
import com.google.gwt.sample.showcase.client.content.i18n.CwBidiFormatting;
import com.google.gwt.sample.showcase.client.content.i18n.CwBidiInput;
import com.google.gwt.sample.showcase.client.content.i18n.CwConstantsExample;
import com.google.gwt.sample.showcase.client.content.i18n.CwConstantsWithLookupExample;
import com.google.gwt.sample.showcase.client.content.i18n.CwDateTimeFormat;
import com.google.gwt.sample.showcase.client.content.i18n.CwDictionaryExample;
import com.google.gwt.sample.showcase.client.content.i18n.CwMessagesExample;
import com.google.gwt.sample.showcase.client.content.i18n.CwNumberFormat;
import com.google.gwt.sample.showcase.client.content.i18n.CwPluralFormsExample;
import com.google.gwt.sample.showcase.client.content.lists.CwListBox;
import com.google.gwt.sample.showcase.client.content.lists.CwMenuBar;
import com.google.gwt.sample.showcase.client.content.lists.CwStackLayoutPanel;
import com.google.gwt.sample.showcase.client.content.lists.CwStackPanel;
import com.google.gwt.sample.showcase.client.content.lists.CwSuggestBox;
import com.google.gwt.sample.showcase.client.content.lists.CwTree;
import com.google.gwt.sample.showcase.client.content.other.CwAnimation;
import com.google.gwt.sample.showcase.client.content.other.CwCookies;
import com.google.gwt.sample.showcase.client.content.panels.CwAbsolutePanel;
import com.google.gwt.sample.showcase.client.content.panels.CwDecoratorPanel;
import com.google.gwt.sample.showcase.client.content.panels.CwDisclosurePanel;
import com.google.gwt.sample.showcase.client.content.panels.CwDockPanel;
import com.google.gwt.sample.showcase.client.content.panels.CwFlowPanel;
import com.google.gwt.sample.showcase.client.content.panels.CwHorizontalPanel;
import com.google.gwt.sample.showcase.client.content.panels.CwSplitLayoutPanel;
import com.google.gwt.sample.showcase.client.content.panels.CwTabLayoutPanel;
import com.google.gwt.sample.showcase.client.content.panels.CwVerticalPanel;
import com.google.gwt.sample.showcase.client.content.popups.CwBasicPopup;
import com.google.gwt.sample.showcase.client.content.popups.CwDialogBox;
import com.google.gwt.sample.showcase.client.content.tables.CwFlexTable;
import com.google.gwt.sample.showcase.client.content.tables.CwGrid;
import com.google.gwt.sample.showcase.client.content.text.CwBasicText;
import com.google.gwt.sample.showcase.client.content.text.CwRichText;
import com.google.gwt.sample.showcase.client.content.widgets.CwBasicButton;
import com.google.gwt.sample.showcase.client.content.widgets.CwCheckBox;
import com.google.gwt.sample.showcase.client.content.widgets.CwCustomButton;
import com.google.gwt.sample.showcase.client.content.widgets.CwDatePicker;
import com.google.gwt.sample.showcase.client.content.widgets.CwFileUpload;
import com.google.gwt.sample.showcase.client.content.widgets.CwHyperlink;
import com.google.gwt.sample.showcase.client.content.widgets.CwRadioButton;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.TreeViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The {@link TreeViewModel} used by the main menu.
 */
public class MainMenuTreeViewModel implements TreeViewModel {

  /**
   * The constants used in the menu.
   */
  public static interface MenuConstants extends Constants {

    String categoryCells();

    String categoryI18N();

    String categoryLists();

    String categoryOther();

    String categoryPanels();

    String categoryPopups();

    String categoryTables();

    String categoryTextInput();

    String categoryWidgets();
  }

  /**
   * The cell used to render categories.
   */
  private static class CategoryCell extends AbstractCell<Category> {
    @Override
    public void render(Context context, Category value, SafeHtmlBuilder sb) {
      if (value != null) {
        sb.appendEscaped(value.getName());
      }
    }
  }

  /**
   * The cell used to render examples.
   */
  private static class ContentWidgetCell extends AbstractCell<ContentWidget> {
    @Override
    public void render(Context context, ContentWidget value, SafeHtmlBuilder sb) {
      if (value != null) {
        sb.appendEscaped(value.getName());
      }
    }
  }

  /**
   * A top level category in the tree.
   */
  public class Category {

    private final ListDataProvider<ContentWidget> examples =
        new ListDataProvider<ContentWidget>();
    private final String name;
    private NodeInfo<ContentWidget> nodeInfo;
    private final List<RunAsyncCode> splitPoints =
        new ArrayList<RunAsyncCode>();

    public Category(String name) {
      this.name = name;
    }

    public void addExample(ContentWidget example, RunAsyncCode splitPoint) {
      examples.getList().add(example);
      if (splitPoint != null) {
        splitPoints.add(splitPoint);
      }
      contentCategory.put(example, this);
      contentToken.put(Showcase.getContentWidgetToken(example), example);
    }

    public String getName() {
      return name;
    }

    /**
     * Get the node info for the examples under this category.
     * 
     * @return the node info
     */
    public NodeInfo<ContentWidget> getNodeInfo() {
      if (nodeInfo == null) {
        nodeInfo = new DefaultNodeInfo<ContentWidget>(examples,
            contentWidgetCell, selectionModel, null);
      }
      return nodeInfo;
    }

    /**
     * Get the list of split points to prefetch for this category.
     * 
     * @return the list of classes in this category
     */
    public Iterable<RunAsyncCode> getSplitPoints() {
      return splitPoints;
    }
  }

  /**
   * The top level categories.
   */
  private final ListDataProvider<Category> categories = new ListDataProvider<Category>();

  /**
   * A mapping of {@link ContentWidget}s to their associated categories.
   */
  private final Map<ContentWidget, Category> contentCategory = new HashMap<ContentWidget, Category>();

  /**
   * The cell used to render examples.
   */
  private final ContentWidgetCell contentWidgetCell = new ContentWidgetCell();

  /**
   * A mapping of history tokens to their associated {@link ContentWidget}.
   */
  private final Map<String, ContentWidget> contentToken = new HashMap<String, ContentWidget>();

  /**
   * The selection model used to select examples.
   */
  private final SelectionModel<ContentWidget> selectionModel;

  public MainMenuTreeViewModel(ShowcaseConstants constants,
      SelectionModel<ContentWidget> selectionModel) {
    this.selectionModel = selectionModel;
    initializeTree(constants);
  }

  /**
   * Get the {@link Category} associated with a widget.
   * 
   * @param widget the {@link ContentWidget}
   * @return the associated {@link Category}
   */
  public Category getCategoryForContentWidget(ContentWidget widget) {
    return contentCategory.get(widget);
  }

  /**
   * Get the content widget associated with the specified history token.
   * 
   * @param token the history token
   * @return the associated {@link ContentWidget}
   */
  public ContentWidget getContentWidgetForToken(String token) {
    return contentToken.get(token);
  }

  public <T> NodeInfo<?> getNodeInfo(T value) {
    if (value == null) {
      // Return the top level categories.
      return new DefaultNodeInfo<Category>(categories, new CategoryCell());
    } else if (value instanceof Category) {
      // Return the examples within the category.
      Category category = (Category) value;
      return category.getNodeInfo();
    }
    return null;
  }

  public boolean isLeaf(Object value) {
    return value != null && !(value instanceof Category);
  }

  /**
   * Get the set of all {@link ContentWidget}s used in the model.
   * 
   * @return the {@link ContentWidget}s
   */
  Set<ContentWidget> getAllContentWidgets() {
    Set<ContentWidget> widgets = new HashSet<ContentWidget>();
    for (Category category : categories.getList()) {
      for (ContentWidget example : category.examples.getList()) {
        widgets.add(example);
      }
    }
    return widgets;
  }

  /**
   * Initialize the top level categories in the tree.
   */
  private void initializeTree(ShowcaseConstants constants) {
    List<Category> catList = categories.getList();

    // Widgets.
    {
      Category category = new Category(constants.categoryWidgets());
      catList.add(category);
      // CwCheckBox is the default example, so don't prefetch it.
      category.addExample(new CwCheckBox(constants), null);
      category.addExample(new CwRadioButton(constants),
          RunAsyncCode.runAsyncCode(CwRadioButton.class));
      category.addExample(new CwBasicButton(constants),
          RunAsyncCode.runAsyncCode(CwBasicButton.class));
      category.addExample(new CwCustomButton(constants),
          RunAsyncCode.runAsyncCode(CwCustomButton.class));
      category.addExample(new CwFileUpload(constants),
          RunAsyncCode.runAsyncCode(CwFileUpload.class));
      category.addExample(new CwDatePicker(constants),
          RunAsyncCode.runAsyncCode(CwDatePicker.class));
      category.addExample(new CwHyperlink(constants),
          RunAsyncCode.runAsyncCode(CwHyperlink.class));
    }

    // Lists and Menus.
    {
      Category category = new Category(constants.categoryLists());
      catList.add(category);
      category.addExample(new CwListBox(constants),
          RunAsyncCode.runAsyncCode(CwListBox.class));
      category.addExample(new CwSuggestBox(constants),
          RunAsyncCode.runAsyncCode(CwSuggestBox.class));
      category.addExample(new CwTree(constants),
          RunAsyncCode.runAsyncCode(CwTree.class));
      category.addExample(new CwMenuBar(constants),
          RunAsyncCode.runAsyncCode(CwMenuBar.class));
      category.addExample(new CwStackPanel(constants),
          RunAsyncCode.runAsyncCode(CwStackPanel.class));
      category.addExample(new CwStackLayoutPanel(constants),
          RunAsyncCode.runAsyncCode(CwStackLayoutPanel.class));
    }

    // Text Input.
    {
      Category category = new Category(constants.categoryTextInput());
      catList.add(category);
      category.addExample(new CwBasicText(constants),
          RunAsyncCode.runAsyncCode(CwBasicText.class));
      category.addExample(new CwRichText(constants),
          RunAsyncCode.runAsyncCode(CwRichText.class));
    }

    // Popups.
    {
      Category category = new Category(constants.categoryPopups());
      catList.add(category);
      category.addExample(new CwBasicPopup(constants),
          RunAsyncCode.runAsyncCode(CwBasicPopup.class));
      category.addExample(new CwDialogBox(constants),
          RunAsyncCode.runAsyncCode(CwDialogBox.class));
    }

    // Panels.
    {
      Category category = new Category(constants.categoryPanels());
      catList.add(category);
      category.addExample(new CwDecoratorPanel(constants),
          RunAsyncCode.runAsyncCode(CwDecoratorPanel.class));
      category.addExample(new CwFlowPanel(constants),
          RunAsyncCode.runAsyncCode(CwFlowPanel.class));
      category.addExample(new CwHorizontalPanel(constants),
          RunAsyncCode.runAsyncCode(CwHorizontalPanel.class));
      category.addExample(new CwVerticalPanel(constants),
          RunAsyncCode.runAsyncCode(CwVerticalPanel.class));
      category.addExample(new CwAbsolutePanel(constants),
          RunAsyncCode.runAsyncCode(CwAbsolutePanel.class));
      category.addExample(new CwDockPanel(constants),
          RunAsyncCode.runAsyncCode(CwDockPanel.class));
      category.addExample(new CwDisclosurePanel(constants),
          RunAsyncCode.runAsyncCode(CwDisclosurePanel.class));
      category.addExample(new CwTabLayoutPanel(constants),
          RunAsyncCode.runAsyncCode(CwTabLayoutPanel.class));
      category.addExample(new CwSplitLayoutPanel(constants),
          RunAsyncCode.runAsyncCode(CwSplitLayoutPanel.class));
    }

    // Tables.
    {
      Category category = new Category(constants.categoryTables());
      catList.add(category);
      category.addExample(new CwGrid(constants),
          RunAsyncCode.runAsyncCode(CwGrid.class));
      category.addExample(new CwFlexTable(constants),
          RunAsyncCode.runAsyncCode(CwFlexTable.class));
    }

    // Cells.
    {
      Category category = new Category(constants.categoryCells());
      catList.add(category);
      category.addExample(new CwCellList(constants),
          RunAsyncCode.runAsyncCode(CwCellList.class));
      category.addExample(new CwCellTable(constants),
          RunAsyncCode.runAsyncCode(CwCellTable.class));
      category.addExample(new CwDataGrid(constants),
          RunAsyncCode.runAsyncCode(CwDataGrid.class));
      category.addExample(new CwCustomDataGrid(constants),
          RunAsyncCode.runAsyncCode(CwCustomDataGrid.class));
      category.addExample(new CwCellTree(constants),
          RunAsyncCode.runAsyncCode(CwCellTree.class));
      category.addExample(new CwCellBrowser(constants),
          RunAsyncCode.runAsyncCode(CwCellBrowser.class));
      category.addExample(new CwCellSampler(constants),
          RunAsyncCode.runAsyncCode(CwCellSampler.class));
      category.addExample(new CwCellValidation(constants),
          RunAsyncCode.runAsyncCode(CwCellValidation.class));
    }

    // I18N.
    {
      Category category = new Category(constants.categoryI18N());
      catList.add(category);
      category.addExample(new CwNumberFormat(constants),
          RunAsyncCode.runAsyncCode(CwNumberFormat.class));
      category.addExample(new CwDateTimeFormat(constants),
          RunAsyncCode.runAsyncCode(CwDateTimeFormat.class));
      category.addExample(new CwMessagesExample(constants),
          RunAsyncCode.runAsyncCode(CwMessagesExample.class));
      category.addExample(new CwBidiInput(constants),
          RunAsyncCode.runAsyncCode(CwBidiInput.class));
      category.addExample(new CwBidiFormatting(constants),
          RunAsyncCode.runAsyncCode(CwBidiFormatting.class));
      category.addExample(new CwPluralFormsExample(constants),
          RunAsyncCode.runAsyncCode(CwPluralFormsExample.class));
      category.addExample(new CwConstantsExample(constants),
          RunAsyncCode.runAsyncCode(CwConstantsExample.class));
      category.addExample(new CwConstantsWithLookupExample(constants),
          RunAsyncCode.runAsyncCode(CwConstantsWithLookupExample.class));
      category.addExample(new CwDictionaryExample(constants),
          RunAsyncCode.runAsyncCode(CwDictionaryExample.class));
    }

    // Other.
    {
      Category category = new Category(constants.categoryOther());
      catList.add(category);
      category.addExample(new CwAnimation(constants),
          RunAsyncCode.runAsyncCode(CwAnimation.class));
      category.addExample(new CwCookies(constants),
          RunAsyncCode.runAsyncCode(CwCookies.class));
    }
  }
}
