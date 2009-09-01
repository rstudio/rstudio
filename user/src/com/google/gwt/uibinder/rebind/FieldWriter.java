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

import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JType;

import java.util.HashSet;
import java.util.Set;

/**
 * Models a field to be written in the generated binder code. Note that this is
 * not necessarily a field that the user has declared. It's basically any
 * instance variable the generator will need.
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
public class FieldWriter {
  private final JClassType type;
  private final String name;
  private final Set<FieldWriter> needs = new HashSet<FieldWriter>();

  private String initializer;
  private boolean written;

  /**
   * Package protected, only TemplateWriter is allowed to instantiate
   * FieldWriters.
   * <p>
   * Public for testing only
   */
  FieldWriter(JClassType type, String name) {
    this.type = type;
    this.name = name;
  }

  /**
   * @return the fully qualified name of this field's type
   */
  public String getFullTypeName() {
    return type.getPackage().getName() + "." + type.getName();
  }

  /**
   * @return the type of this field
   */
  public JClassType getType() {
    return type;
  }

  /**
   * Declares that the receiver depends upon the give field.
   */
  public void needs(FieldWriter f) {
    needs.add(f);
  }

  /**
   * Used to provide an initializer string to use instead of a
   * {@link com.google.gwt.core.client.GWT#create()} call. Note that this is an
   * RHS expression. Don't include the leading '=', and don't end it with ';'.
   * 
   * @throws IllegalStateException on second attempt to set the initializer
   */
  public void setInitializer(String initializer) {
    // TODO(rjrjr) Should be able to make this a constructor argument
    // when BundleAttributeParser dies
    if (this.initializer != null) {
      throw new IllegalStateException(String.format(
          "Second attempt to set initializer for field \"%s\", "
              + "from \"%s\" to \"%s\"", name, this.initializer, initializer));
    }
    this.initializer = initializer;
  }

  /**
   * @deprecated needed only by
   *             {@link com.google.gwt.uibinder.parsers.BundleAttributeParser},
   *             which will die soon
   * @throws IllegalStateException if initializer in a later call doesn't match
   *           earlier call
   */
  @Deprecated
  public void setInitializerMaybe(String initializer) {
    if (this.initializer != null && !this.initializer.equals(initializer)) {
      throw new IllegalStateException(String.format(
          "Attempt to change initializer for field \"%s\", "
              + "from \"%s\" to \"%s\"", name, this.initializer, initializer));
    }
    this.initializer = initializer;
  }

  /**
   * Write the field delcaration.
   * 
   * @return false if unable to write for lack of a default constructor
   */
  // TODO(rjrjr) This return code thing is silly. We should
  // just be calling {@link TemplateWriter#die}, but that's made complicated
  // by an unfortunate override in UiBinderWriter. Fix this when that
  // subclassing goes away.
  public boolean write(IndentedWriter w) {
    if (written) {
      return true;
    }

    for (FieldWriter f : needs) {
      // TODO(rdamazio, rjrjr) This is simplistic, and will fail when
      // we support more interesting contexts (e.g. the same need being used
      // inside two different
      // LazyPanels)
      f.write(w);
    }

    if (initializer == null) {
      if ((type.isInterface() == null)
          && (type.findConstructor(new JType[0]) == null)) {
        return false;
      }
      initializer = String.format("(%1$s) GWT.create(%1$s.class)",
          getFullTypeName());
    }

    w.write("%s %s = %s;", getFullTypeName(), name, initializer);

    this.written = true;
    return true;
  }
}
