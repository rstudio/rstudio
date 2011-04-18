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
package com.google.web.bindery.autobean.shared;

import java.util.Collection;
import java.util.Map;

/**
 * Allows traversal of an AutoBean object graph.
 */
public class AutoBeanVisitor {
  /**
   * A PropertyContext that describes the parameterization of the Collection
   * being visited.
   */
  public interface CollectionPropertyContext extends PropertyContext {
    /**
     * Returns the collection's element type.
     * 
     * @return a Class object representing the element type
     */
    Class<?> getElementType();
  }

  /**
   * Reserved for future expansion to avoid API breaks.
   */
  public interface Context {
  }

  /**
   * A PropertyContext that describes the parameterization of the Map being
   * visited.
   */
  public interface MapPropertyContext extends PropertyContext {
    /**
     * Returns the map's key type.
     * 
     * @return a Class object representing the key type
     */
    Class<?> getKeyType();

    /**
     * Returns the map's value type.
     * 
     * @return a Class object representing the value type
     */
    Class<?> getValueType();
  }

  /**
   * The ParameterizationVisitor provides access to more complete type
   * information than a simple class literal can provide.
   * <p>
   * The order of traversal reflects the declared parameterization of the
   * property. For example, a {@code Map<String, List<Foo>>} would be traversed
   * via the following sequence:
   * 
   * <pre>
   * visitType(Map.class);
   *   visitParameter();
   *     visitType(String.class);
   *     endVisitType(String.class);
   *   endVisitParameter();
   *   visitParameter();
   *     visitType(List.class);
   *       visitParameter();
   *         visitType(Foo.class);
   *         endVisitType(Foo.class);
   *       endParameter();
   *     endVisitType(List.class);
   *   endVisitParameter();
   * endVisitType(Map.class);
   * </pre>
   */
  public static class ParameterizationVisitor {
    /**
     * Called when finished with a type parameter.
     */
    public void endVisitParameter() {
    }

    /**
     * Called when finished with a type.
     * 
     * @param type a Class object
     */
    public void endVisitType(Class<?> type) {
    }

    /**
     * Called when visiting a type parameter.
     * 
     * @return {@code true} if the type parameter should be visited
     */
    public boolean visitParameter() {
      return true;
    }

    /**
     * Called when visiting a possibly parameterized type.
     * 
     * @param type a Class object
     * @return {@code true} if the type should be visited
     */
    public boolean visitType(Class<?> type) {
      return true;
    }
  }

  /**
   * Allows properties to be reset.
   */
  public interface PropertyContext {
    /**
     * Allows deeper inspection of the declared parameterization of the
     * property.
     */
    void accept(ParameterizationVisitor visitor);

    /**
     * Indicates if the {@link #set} method will succeed.
     * 
     * @return {@code true} if the property can be set
     */
    boolean canSet();

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
  public void endVisitCollectionProperty(String propertyName, AutoBean<Collection<?>> value,
      CollectionPropertyContext ctx) {
    endVisitReferenceProperty(propertyName, value, ctx);
  }

  /**
   * Called after visiting a reference property.
   * 
   * @param propertyName the property name, as a String
   * @param value the property value
   * @param ctx a PropertyContext
   */
  public void endVisitMapProperty(String propertyName, AutoBean<Map<?, ?>> value,
      MapPropertyContext ctx) {
    endVisitReferenceProperty(propertyName, value, ctx);
  }

  /**
   * Called after visiting a reference property.
   * 
   * @param propertyName the property name, as a String
   * @param value the property value
   * @param ctx a PropertyContext
   */
  public void endVisitReferenceProperty(String propertyName, AutoBean<?> value, PropertyContext ctx) {
  }

  /**
   * Called after visiting a value property.
   * 
   * @param propertyName the property name, as a String
   * @param value the property value
   * @param ctx a PropertyContext
   */
  public void endVisitValueProperty(String propertyName, Object value, PropertyContext ctx) {
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
  public boolean visitCollectionProperty(String propertyName, AutoBean<Collection<?>> value,
      CollectionPropertyContext ctx) {
    return visitReferenceProperty(propertyName, value, ctx);
  }

  /**
   * Called every time, but {@link #visit(AutoBean, Context)} will be called for
   * the value only the first time it is encountered.
   * 
   * @param propertyName the property name, as a String
   * @param value the property value
   * @param ctx a PropertyContext
   */
  public boolean visitMapProperty(String propertyName, AutoBean<Map<?, ?>> value,
      MapPropertyContext ctx) {
    return visitReferenceProperty(propertyName, value, ctx);
  }

  /**
   * Called every time, but {@link #visit(AutoBean, Context)} will be called for
   * the value only the first time it is encountered.
   * 
   * @param propertyName the property name, as a String
   * @param value the property value
   * @param ctx a PropertyContext
   */
  public boolean visitReferenceProperty(String propertyName, AutoBean<?> value, PropertyContext ctx) {
    return true;
  }

  /**
   * TODO: document.
   * 
   * @param propertyName the property name, as a String
   * @param value the property value
   * @param ctx a PropertyContext
   */
  public boolean visitValueProperty(String propertyName, Object value, PropertyContext ctx) {
    return true;
  }
}
