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

import java.util.Arrays;
import java.util.List;

/**
 * A category of recipes.
 */
public class Category {
  private final String title;
  private final Recipe[] recipes;

  public Category(String title, Recipe[] recipes) {
    this.title = title;
    this.recipes = recipes;
  }

  /**
   * Gets the recipes in this category.
   *
   * @return this category's recipes
   */
  public List<Recipe> getRecipes() {
    return Arrays.asList(recipes);
  }

  /**
   * Gets this category's title.
   *
   * @return the category title
   */
  public String getTitle() {
    return title;
  }
}
