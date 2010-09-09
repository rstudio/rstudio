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
package com.google.gwt.uibinder.elementparsers;

import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JAbstractMethod;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.dev.util.Pair;
import com.google.gwt.uibinder.rebind.UiBinderContext;
import com.google.gwt.uibinder.rebind.UiBinderWriter;
import com.google.gwt.uibinder.rebind.XMLElement;
import com.google.gwt.uibinder.rebind.XMLElement.Interpreter;
import com.google.gwt.uibinder.rebind.model.OwnerFieldClass;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Parses any children of widgets that use the {@link UiChild} annotation.
 */
public class UiChildParser implements ElementParser {

  private String fieldName;

  /**
   * Mapping of child tag to the number of times it has been called
   */
  private Map<String, Integer> numCallsToChildMethod = new HashMap<String, Integer>();
  private Map<String, Pair<JMethod, Integer>> uiChildMethods;
  private UiBinderWriter writer;
  private final UiBinderContext uiBinderCtx;

  /**
   * @param uiBinderCtx
   */
  public UiChildParser(UiBinderContext uiBinderCtx) {
    this.uiBinderCtx = uiBinderCtx;
  }

  public void parse(final XMLElement elem, String fieldName, JClassType type,
      UiBinderWriter writer) throws UnableToCompleteException {
    this.fieldName = fieldName;
    this.writer = writer;

    OwnerFieldClass ownerFieldClass = OwnerFieldClass.getFieldClass(type,
        writer.getLogger(), uiBinderCtx);

    uiChildMethods = ownerFieldClass.getUiChildMethods();

    // Parse children.
    elem.consumeChildElements(new Interpreter<Boolean>() {
      public Boolean interpretElement(XMLElement child)
          throws UnableToCompleteException {
        if (isValidChildElement(elem, child)) {
          handleChild(child);
          return true;
        }
        return false;
      }
    });
  }

  /**
   * Checks if this call will go over the limit for the number of valid calls.
   * If it won't, it will increment the number of calls made.
   * 
   * @throws UnableToCompleteException
   */
  private void checkLimit(int limit, String tag, XMLElement toAdd)
      throws UnableToCompleteException {
    Integer priorCalls = numCallsToChildMethod.get(tag);
    if (priorCalls == null) {
      priorCalls = 0;
    }
    if (limit > 0 && priorCalls > 0 && priorCalls + 1 > limit) {
      writer.die(toAdd, "Can only use the @UiChild tag " + tag + " " + limit
          + " time(s).");
    }
    numCallsToChildMethod.put(tag, priorCalls + 1);
  }

  /**
   * Process a child element that should be added using a {@link UiChild} method
   */
  private void handleChild(XMLElement child) throws UnableToCompleteException {
    String tag = child.getLocalName();
    Pair<JMethod, Integer> methodPair = uiChildMethods.get(tag);
    JMethod method = methodPair.left;
    int limit = methodPair.right;
    Iterator<XMLElement> children = child.consumeChildElements().iterator();

    // If the UiChild tag has no children just return.
    if (!children.hasNext()) {
      return;
    }
    XMLElement toAdd = children.next();

    if (!writer.isWidgetElement(toAdd)) {
      writer.die(child, "Expecting only widgets in %s", child);
    }

    // Make sure that there is only one element per tag.
    if (children.hasNext()) {
      writer.die(toAdd, "Can only have one element per @UiChild parser tag.");
    }

    // Check that this element won't put us over the limit.
    checkLimit(limit, tag, toAdd);

    // Add the child using the @UiChild function
    String[] parameters = makeArgsList(child, method, toAdd);

    writer.addStatement("%1$s.%2$s(%3$s);", fieldName, method.getName(),
        UiBinderWriter.asCommaSeparatedList(parameters));
  }

  private boolean isValidChildElement(XMLElement parent, XMLElement child) {
    if (child != null && child.getNamespaceUri() != null
        && child.getNamespaceUri().equals(parent.getNamespaceUri())
        && uiChildMethods.containsKey(child.getLocalName())) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Go through all of the given method's required attributes and consume them
   * from the given element. Any attributes not required by the method are left
   * untouched. If a parameter is not present in the element, it will be passed
   * null.
   * 
   * @param element The element to find the necessary attributes for the
   *          parameters to the method.
   * @param method The method to gather parameters for.
   * @return The list of parameters to send to the function.
   * @throws UnableToCompleteException
   */
  private String[] makeArgsList(XMLElement element, JAbstractMethod method,
      XMLElement toAdd) throws UnableToCompleteException {
    JParameter[] params = method.getParameters();
    String[] args = new String[params.length];
    args[0] = writer.parseElementToField(toAdd);

    // First parameter is the child widget
    for (int index = 1; index < params.length; index++) {
      JParameter param = params[index];
      String defaultValue = null;

      if (param.getType().isPrimitive() != null) {
        defaultValue = param.getType().isPrimitive().getUninitializedFieldExpression();
      }
      String value = element.consumeAttributeWithDefault(param.getName(),
          defaultValue, param.getType());
      args[index] = value;
    }
    return args;
  }
}
