/*
 * Copyright 2008 Google Inc.
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

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.prefetch.Prefetcher;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.HeadElement;
import com.google.gwt.dom.client.LinkElement;
import com.google.gwt.event.logical.shared.OpenEvent;
import com.google.gwt.event.logical.shared.OpenHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.sample.showcase.client.MainMenuTreeViewModel.Category;
import com.google.gwt.user.cellview.client.CellTree;
import com.google.gwt.user.cellview.client.TreeNode;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Showcase implements EntryPoint {
  /**
   * The type passed into the
   * {@link com.google.gwt.sample.showcase.generator.ShowcaseGenerator}.
   */
  private static final class GeneratorInfo {
  }

  /**
   * The static images used throughout the Showcase.
   */
  public static final ShowcaseResources images = GWT.create(
      ShowcaseResources.class);

  /**
   * The name of the style theme used in showcase.
   */
  public static final String THEME = "clean";

  /**
   * Get the token for a given content widget.
   *
   * @return the content widget token.
   */
  public static String getContentWidgetToken(ContentWidget content) {
    return getContentWidgetToken(content.getClass());
  }

  /**
   * Get the token for a given content widget.
   *
   * @return the content widget token.
   */
  public static <C extends ContentWidget> String getContentWidgetToken(
      Class<C> cwClass) {
    String className = cwClass.getName();
    className = className.substring(className.lastIndexOf('.') + 1);
    return "!" + className;
  }

  /**
   * The main application shell.
   */
  private ShowcaseShell shell;

  /**
   * This is the entry point method.
   */
  public void onModuleLoad() {
    // Generate the source code and css for the examples
    GWT.create(GeneratorInfo.class);

    // Inject global styles.
    injectThemeStyleSheet();
    images.css().ensureInjected();

    // Initialize the constants.
    ShowcaseConstants constants = GWT.create(ShowcaseConstants.class);

    // Create the application shell.
    final SingleSelectionModel<ContentWidget> selectionModel = new SingleSelectionModel<ContentWidget>();
    final MainMenuTreeViewModel treeModel = new MainMenuTreeViewModel(
        constants, selectionModel);
    Set<ContentWidget> contentWidgets = treeModel.getAllContentWidgets();
    shell = new ShowcaseShell(treeModel);
    RootLayoutPanel.get().add(shell);

    // Prefetch examples when opening the Category tree nodes.
    final List<Category> prefetched = new ArrayList<Category>();
    final CellTree mainMenu = shell.getMainMenu();
    mainMenu.addOpenHandler(new OpenHandler<TreeNode>() {
      public void onOpen(OpenEvent<TreeNode> event) {
        Object value = event.getTarget().getValue();
        if (!(value instanceof Category)) {
          return;
        }

        Category category = (Category) value;
        if (!prefetched.contains(category)) {
          prefetched.add(category);
          Prefetcher.prefetch(category.getSplitPoints());
        }
      }
    });

    // Always prefetch.
    Prefetcher.start();

    // Change the history token when a main menu item is selected.
    selectionModel.addSelectionChangeHandler(
        new SelectionChangeEvent.Handler() {
          public void onSelectionChange(SelectionChangeEvent event) {
            ContentWidget selected = selectionModel.getSelectedObject();
            if (selected != null) {
              History.newItem(getContentWidgetToken(selected), true);
            }
          }
        });

    // Setup a history handler to reselect the associate menu item.
    final ValueChangeHandler<String> historyHandler = new ValueChangeHandler<
        String>() {
      public void onValueChange(ValueChangeEvent<String> event) {
        // Get the content widget associated with the history token.
        ContentWidget contentWidget = treeModel.getContentWidgetForToken(
            event.getValue());
        if (contentWidget == null) {
          return;
        }

        // Expand the tree node associated with the content.
        Category category = treeModel.getCategoryForContentWidget(
            contentWidget);
        TreeNode node = mainMenu.getRootTreeNode();
        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
          if (node.getChildValue(i) == category) {
            node.setChildOpen(i, true, true);
            break;
          }
        }

        // Select the node in the tree.
        selectionModel.setSelected(contentWidget, true);

        // Display the content widget.
        displayContentWidget(contentWidget);
      }
    };
    History.addValueChangeHandler(historyHandler);

    // Show the initial example.
    if (History.getToken().length() > 0) {
      History.fireCurrentHistoryState();
    } else {
      // Use the first token available.
      TreeNode root = mainMenu.getRootTreeNode();
      TreeNode category = root.setChildOpen(0, true);
      ContentWidget content = (ContentWidget) category.getChildValue(0);
      selectionModel.setSelected(content, true);
    }

    // Generate a site map.
    createSiteMap(contentWidgets);
  }

  /**
   * Create a hidden site map for crawlability.
   * 
   * @param contentWidgets the {@link ContentWidget}s used in Showcase
   */
  private void createSiteMap(Set<ContentWidget> contentWidgets) {
    SafeHtmlBuilder sb = new SafeHtmlBuilder();
    for (ContentWidget cw : contentWidgets) {
      String token = getContentWidgetToken(cw);
      sb.append(SafeHtmlUtils.fromTrustedString("<a href=\"#" + token + "\">"
          + token + "</a>"));
    }

    // Add the site map to the page.
    HTML siteMap = new HTML(sb.toSafeHtml());
    siteMap.setVisible(false);
    RootPanel.get().add(siteMap, 0, 0);
  }
  
  /**
   * Set the content to the {@link ContentWidget}.
   *
   * @param content the {@link ContentWidget} to display
   */
  private void displayContentWidget(ContentWidget content) {
    if (content == null) {
      return;
    }

    shell.setContent(content);
    Window.setTitle("Showcase of Features: " + content.getName());
  }

  /**
   * Convenience method for getting the document's head element.
   *
   * @return the document's head element
   */
  private native HeadElement getHeadElement() /*-{
    return $doc.getElementsByTagName("head")[0];
  }-*/;

  /**
   * Inject the GWT theme style sheet based on the RTL direction of the current
   * locale.
   */
  private void injectThemeStyleSheet() {
    // Choose the name style sheet based on the locale.
    String styleSheet = "gwt/" + THEME + "/" + THEME;
    styleSheet += LocaleInfo.getCurrentLocale().isRTL() ? "_rtl.css" : ".css";

    // Load the GWT theme style sheet
    String modulePath = GWT.getModuleBaseURL();
    LinkElement linkElem = Document.get().createLinkElement();
    linkElem.setRel("stylesheet");
    linkElem.setType("text/css");
    linkElem.setHref(modulePath + styleSheet);
    getHeadElement().appendChild(linkElem);
  }
}
