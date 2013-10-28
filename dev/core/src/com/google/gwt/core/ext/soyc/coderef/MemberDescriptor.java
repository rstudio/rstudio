/*
 * Copyright 2013 Google Inc.
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
package com.google.gwt.core.ext.soyc.coderef;

/**
 * Represents an abstract member, such as a field or a method. It is keep simple and will be
 * serialized to json.
 *
 */
public abstract class MemberDescriptor extends EntityDescriptor {

  /**
   * The member's type represents the type of the member value when it is accessed (fields)
   * or when it is called (methods). It is in jsni format.
   */
  protected String type;
  protected final ClassDescriptor enclosingClassDescriptor;

  protected MemberDescriptor(ClassDescriptor owner, String name) {
    super(name);
    enclosingClassDescriptor = owner;
  }

  public ClassDescriptor getEnclosingClassDescriptor() {
    return enclosingClassDescriptor;
  }

  /**
   * The signature of the member.
   *
   * @return The member name plus its signature
   */
  public abstract String getJsniSignature();

  /**
   * Returns the members value type, ie. a field will return its type and a method will return its
   * return type, in jsni format.
   */
  public String getType() {
    return type;
  }

  @Override
  public String getFullName() {
    return enclosingClassDescriptor.getFullName() + "::" + this.getJsniSignature();
  }
}
