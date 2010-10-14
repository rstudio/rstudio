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
package com.google.gwt.editor.client;

/**
 * Allows traversal of an AutoBean object graph.
 */
public class AutoBeanVisitor {
  /**
   * Reserved for future expansion to avoid API breaks.
   */
  public interface Context {
  }

  /**
   * Allows properties to be reset.
   */
  public interface PropertyContext {
    /**
     * Indicates if the {@link #set} method will succeed.
     *
     * @return {@code true} if the property can be set
     */
    boolean canSet();

    /**
     * If the reference property is a collection, returns the collection's
     * element type.
     * 
     * @return a Class object representing the element type or {@code null} if
     *         the property is not a collection type
     */
    Class<?> getElementType();

    /**
     * Returns the expected type of the property.
     *
     * @return a Class object representing the property type
     */
    Class<?> getType();

    /**
     * Sets a property value.
     *
     * @param value the new value
     */
    void set(Object value);
  }

  /**
   * Called after visiting an {@link AutoBean}.
   * 
   * @param bean an {@link AutoBean}
   * @param ctx a Context
   */
  public void endVisit(AutoBean<?> bean, Context ctx) {
  }

  /**
   * Called after visiting a reference property.
   * 
   * @param propertyName the property name, as a String
   * @param value the property value
   * @param ctx a PropertyContext
   */
  public void endVisitReferenceProperty(String propertyName, AutoBean<?> value,
      PropertyContext ctx) {
  }

  /**
   * Called after visiting a value property.
   * 
   * @param propertyName the property name, as a String
   * @param value the property value
   * @param ctx a PropertyContext
   */
  public void endVisitValueProperty(String propertyName, Object value,
      PropertyContext ctx) {
  }

  /**
   * Called when visiting an {@link AutoBean}.
   * 
   * @param bean an {@link AutoBean}
   * @param ctx a Context
   */
  public boolean visit(AutoBean<?> bean, Context ctx) {
    return true;
  }

  /**
   * Called every time, but {@link #visit(AutoBean, Context)} will be called for
   * the value only the first time it is encountered.
   * 
   * @param propertyName the property name, as a String
   * @param value the property value
   * @param ctx a PropertyContext
   */
  public boolean visitReferenceProperty(String propertyName, AutoBean<?> value,
      PropertyContext ctx) {
    return true;
  }

  /**
   * TODO: document.
   * 
   * @param propertyName the property name, as a String
   * @param value the property value
   * @param ctx a PropertyContext
   */
  public boolean visitValueProperty(String propertyName, Object value,
      PropertyContext ctx) {
    return true;
  }
}
