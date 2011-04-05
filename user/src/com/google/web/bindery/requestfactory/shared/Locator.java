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
package com.google.web.bindery.requestfactory.shared;

/**
 * A Locator allows entity types that do not conform to the RequestFactory
 * entity protocol to be used. Instead of attempting to use a {@code findFoo()},
 * {@code getId()}, and {@code getVersion()} declared in the domain entity type,
 * an instance of a Locator will be created to provide implementations of these
 * methods.
 * <p>
 * Locator subtypes must be default instantiable (i.e. public static types with
 * a no-arg constructor). Instances of Locators may be retained and reused by
 * the RequestFactory service layer.
 * 
 * @param <T> the type of domain object the Locator will operate on
 * @param <I> the type of object the Locator expects to use as an id for the
 *          domain object
 * @see ProxyFor#locator()
 */
public abstract class Locator<T, I> {
  /**
   * Create a new instance of the requested type.
   * 
   * @param clazz the type of object to create
   * @return the new instance of the domain type
   */
  public abstract T create(Class<? extends T> clazz);

  /**
   * Retrieve an object. May return {@code null} to indicate that the requested
   * object could not be found.
   * 
   * @param clazz the type of object to retrieve
   * @param id an id previously returned from {@link #getId(Object)}
   * @return the requested object or {@code null} if it could not be found
   */
  public abstract T find(Class<? extends T> clazz, I id);

  /**
   * Returns the {@code T} type.
   */
  public abstract Class<T> getDomainType();

  /**
   * Returns a domain object to be used as the id for the given object. This
   * method may return {@code null} if the object has not been persisted or
   * should be treated as irretrievable.
   * 
   * @param domainObject the object to obtain an id for
   * @return the object's id or {@code null}
   */
  public abstract I getId(T domainObject);

  /**
   * Returns the {@code I} type.
   */
  public abstract Class<I> getIdType();

  /**
   * Returns a domain object to be used as the version for the given object.
   * This method may return {@code null} if the object has not been persisted or
   * should be treated as irretrievable.
   * 
   * @param domainObject the object to obtain an id for
   * @return the object's version or {@code null}
   */
  public abstract Object getVersion(T domainObject);

  /**
   * Returns a value indicating if the domain object should no longer be
   * considered accessible. This method might return false if the record
   * underlying the domain object had been deleted as a side-effect of
   * processing a request.
   * <p>
   * The default implementation of this method uses {@link #getId(Object)} and
   * {@link #find(Class, Object)} to determine if an object can be retrieved.
   */
  public boolean isLive(T domainObject) {
    // Can't us Class.asSubclass() in web-mode code
    @SuppressWarnings("unchecked")
    Class<T> clazz = (Class<T>) domainObject.getClass();
    return find(clazz, getId(domainObject)) != null;
  }
}
