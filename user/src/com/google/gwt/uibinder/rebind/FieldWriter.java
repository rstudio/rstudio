/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.uibinder.rebind;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;

/**
 * Models a field to be written in the generated binder code. Note that this is
 * not necessarily a field that the user has declared. It's basically any
 * variable the generated UiBinder#createAndBindUi implementation will need.
 * <p>
 * A field can have a custom initialization statement, set via
 * {@link #setInitializer}. Without one it will be initialized via a
 * {@link com.google.gwt.core.client.GWT#create} call. (In the rare case that
 * you need a field not to be initialized, initialize it to "null".)
 * <p>
 * Dependencies can be declared between fields via {@link #needs}, to ensure
 * that one can be initialized via reference to another. Circular references are
 * not supported, nor detected.
 */
public interface FieldWriter {

  String getQualifiedSourceName();

  String getInitializer();
  
  /**
   * @return the type of this field, or null if this field is of a type that has
   *         not yet been generated
   */
  JClassType getType();

  /**
   * Declares that the receiver depends upon the given field.
   */
  void needs(FieldWriter f);

  /**
   * Used to provide an initializer string to use instead of a
   * {@link com.google.gwt.core.client.GWT#create()} call. Note that this is an
   * RHS expression. Don't include the leading '=', and don't end it with ';'.
   * <p>
   * TODO(rjrjr) Should be able to make this a constructor argument when
   * BundleAttributeParser dies.
   *
   * @throws UnableToCompleteException
   * @throws IllegalStateException on second attempt to set the initializer
   */
  void setInitializer(String initializer);

  /**
   * @deprecated needed only by
   *             {@link com.google.gwt.uibinder.attributeparsers.BundleAttributeParser},
   *             which will die soon
   * @throws IllegalStateException if initializer in a later call doesn't match
   *           earlier call
   */
  @Deprecated
  void setInitializerMaybe(String initializer);

  /**
   * Write the field delcaration.
   */
  void write(IndentedWriter w)
      throws UnableToCompleteException;
}