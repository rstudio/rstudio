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

import com.google.gwt.bikeshed.cells.client.Cell;
import com.google.gwt.bikeshed.list.shared.ListListModel;
import com.google.gwt.bikeshed.list.shared.SelectionModel;
import com.google.gwt.bikeshed.list.shared.SelectionModel.SelectionChangeEvent;
import com.google.gwt.bikeshed.tree.client.StandardTreeView;
import com.google.gwt.bikeshed.tree.client.TreeNode;
import com.google.gwt.bikeshed.tree.client.TreeViewModel;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.List;

/**
 * Cookbook sample.
 */
public class Cookbook implements EntryPoint {

  interface Binder extends UiBinder<Widget, Cookbook> {}

  static class CategoryCell extends Cell<Category, Void> {
    @Override
    public void render(Category value, Void viewData, StringBuilder sb) {
      sb.append(value.getTitle());
    }
  }

  static class RecipeCell extends Cell<Recipe, Void> {
    @Override
    public void render(Recipe value, Void viewData, StringBuilder sb) {
      sb.append(value.getTitle());
    }
  }

  private final class RecipeTreeModel implements TreeViewModel {
    private ListListModel<Category> catModel = new ListListModel<Category>();

    @Override
    public <T> NodeInfo<?> getNodeInfo(T value, TreeNode<T> treeNode) {
      if (value == null) {
        // Categories at the root.
        return new DefaultNodeInfo<Category>(catModel, new CategoryCell());
      } else if (value instanceof Category) {
        // Demos for each category.
        Category category = (Category) value;
        return new DefaultNodeInfo<Recipe>(new ListListModel<Recipe>(
            category.getRecipes()), new RecipeCell());
      }
      return null;
    }

    @Override
    public boolean isLeaf(Object value, TreeNode<?> treeNode) {
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
  @UiField StandardTreeView recipeTree;

  private RecipeTreeModel recipeTreeModel = new RecipeTreeModel();
  private SingleSelectionModel<Object> treeSelection;
  private SimpleCellListRecipe defaultRecipe;
  private Recipe curRecipe;

  @Override
  public void onModuleLoad() {
    RootLayoutPanel.get().add(binder.createAndBindUi(this));
    createRecipes(recipeTreeModel.catModel.getList());

    treeSelection = new SingleSelectionModel<Object>();
    recipeTree.setSelectionModel(treeSelection);
    treeSelection.addSelectionChangeHandler(new SelectionModel.SelectionChangeHandler() {
      @Override
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
  StandardTreeView createTreeView() {
    return new StandardTreeView(recipeTreeModel, null);
  }

  private void createRecipes(List<Category> cats) {
    defaultRecipe = new SimpleCellListRecipe();

    cats.add(new Category("Lists", new Recipe[] {
        defaultRecipe
    }));
    cats.add(new Category("Tables", new Recipe[] {
        new BasicTableRecipe(),
        new EditableTableRecipe(),
    }));
    cats.add(new Category("Trees", new Recipe[] {
        new BasicTreeRecipe(),
        new SideBySideTreeRecipe(),
    }));
    cats.add(new Category("Other", new Recipe[] {
        new ValidationRecipe(),
        new MailRecipe(),
    }));
  }

  private void showRecipe(Recipe recipe) {
    if (curRecipe != null) {
      dock.remove(curRecipe.getWidget());
    }
    dock.add(recipe.getWidget());
    curRecipe = recipe;
  }
}
