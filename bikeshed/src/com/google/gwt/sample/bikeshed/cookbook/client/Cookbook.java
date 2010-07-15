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

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTree;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListViewAdapter;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwt.view.client.TreeViewModel;
import com.google.gwt.view.client.SelectionModel.SelectionChangeEvent;

import java.util.List;

/**
 * Cookbook sample.
 */
public class Cookbook implements EntryPoint {

  interface Binder extends UiBinder<Widget, Cookbook> {
  }

  static class CategoryCell extends AbstractCell<Category> {
    @Override
    public void render(Category value, Object viewData, StringBuilder sb) {
      sb.append(value.getTitle());
    }
  }

  static class RecipeCell extends AbstractCell<Recipe> {
    @Override
    public void render(Recipe value, Object viewData, StringBuilder sb) {
      sb.append(value.getTitle());
    }
  }

  private static final class RecipeTreeModel implements TreeViewModel {
    private ListViewAdapter<Category> adapter = new ListViewAdapter<Category>();
    private SelectionModel<Object> selectionModel;

    public RecipeTreeModel(SelectionModel<Object> selectionModel) {
      this.selectionModel = selectionModel;
    }

    public <T> NodeInfo<?> getNodeInfo(T value) {
      if (value == null) {
        // Categories at the root.
        return new DefaultNodeInfo<Category>(adapter, new CategoryCell(),
            selectionModel, null);
      } else if (value instanceof Category) {
        // Demos for each category.
        Category category = (Category) value;
        return new DefaultNodeInfo<Recipe>(new ListViewAdapter<Recipe>(
            category.getRecipes()), new RecipeCell(), selectionModel, null);
      }
      return null;
    }

    public boolean isLeaf(Object value) {
      // The root and categories have children.
      if (value == null || value instanceof Category) {
        return false;
      }

      // Demos do not.
      return true;
    }
  }

  private static final Binder binder = GWT.create(Binder.class);

  @UiField DockLayoutPanel dock;
  @UiField CellTree recipeTree;
  @UiField LayoutPanel container;

  private RecipeTreeModel recipeTreeModel;
  private Recipe defaultRecipe;
  private Recipe curRecipe;

  public void onModuleLoad() {
    // Initialize the UI.
    final SingleSelectionModel<Object> treeSelection = new SingleSelectionModel<Object>();
    recipeTreeModel = new RecipeTreeModel(treeSelection);
    RootLayoutPanel.get().add(binder.createAndBindUi(this));
    createRecipes(recipeTreeModel.adapter.getList());

    // Select a recipe on selection.
    treeSelection.addSelectionChangeHandler(new SelectionModel.SelectionChangeHandler() {
      public void onSelectionChange(SelectionChangeEvent event) {
        Object o = treeSelection.getSelectedObject();
        if (o instanceof Recipe) {
          showRecipe((Recipe) o);
        }
      }
    });

    showRecipe(defaultRecipe);
  }

  @UiFactory
  CellTree createTreeView() {
    return new CellTree(recipeTreeModel, null);
  }

  private void createRecipes(List<Category> cats) {
    defaultRecipe = new CellListRecipe();

    cats.add(new Category("Lists", new Recipe[] {defaultRecipe}));
    cats.add(new Category("Tables", new Recipe[] {
        new BasicTableRecipe(), new EditableTableRecipe(),
        new CellSamplerRecipe()}));
    cats.add(new Category("Trees", new Recipe[] {
        new CellTreeRecipe(), new CellBrowserRecipe(),}));
    cats.add(new Category("Other", new Recipe[] {
        new ValidationRecipe(), new MailRecipe(),}));
  }

  private void showRecipe(Recipe recipe) {
    if (curRecipe != null) {
      container.remove(curRecipe.getWidget());
    }
    container.add(recipe.getWidget());
    curRecipe = recipe;
  }
}
