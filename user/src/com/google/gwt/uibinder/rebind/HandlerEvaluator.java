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
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.uibinder.rebind.model.OwnerClass;

/**
 * This class implements an easy way to bind widget event handlers to methods
 * annotated with {@link com.google.gwt.uibinder.client.UiHandler} so that the
 * user doesn't need to worry about writing code to implement these bindings.
 *
 * <p>
 * For instance, the class defined below:
 *
 * <pre>
 *   public class MyClass {
 *     @UiField Label label;
 *
 *     @UiBinder({"label", "link"})
 *     public void doClick(ClickEvent e) {
 *       // do something
 *     }
 *   }
 * </pre>
 *
 * will generate a piece of code like:
 *
 * <pre>
 *    ClickHandler handler0 = new ClickHandler() {
 *      @Override
 *      public void onClick(ClickEvent event) {
 *        owner.doClick(event);
 *      }
 *   });
 *   label.addClickHandler(handler0);
 *   link.addClickHandler(handler0);
 * </pre>
 *
 * Notice that the <b>link</b> object doesn't need to be annotated with
 * {@link com.google.gwt.uibinder.client.UiField} as long as it exists
 * (annotated with ui:field) in the template.
 */
class HandlerEvaluator {

  private static final String HANDLER_BASE_NAME =
      "handlerMethodWithNameVeryUnlikelyToCollideWithUserFieldNames";
  /*
   * TODO(rjrjr) The correct fix is to put the handlers in a locally defined
   * class, making the generated code look like this
   *
   * http://docs.google.com/Doc?docid=0AQfnKgX9tAdgZGZ2cTM5YjdfMmQ4OTk0eGhz&hl=en
   *
   * But that needs to wait for a refactor to get most of this stuff out of here
   * and into com.google.gwt.uibinder.rebind.model
   */
  private int varCounter = 0;

  private final MortalLogger logger;

  private final JClassType handlerRegistrationJClass;
  private final JClassType eventHandlerJClass;
  private final OwnerClass ownerClass;
  private final boolean useLazyWidgetBuilders;

  /**
   * The verbose testable constructor.
   *
   * @param ownerClass a descriptor of the UI owner class
   * @param logger the logger for warnings and errors
   * @param oracle the type oracle
   */
  HandlerEvaluator(OwnerClass ownerClass, MortalLogger logger,
      TypeOracle oracle, boolean useLazyWidgetBuilders) {
    this.ownerClass = ownerClass;
    this.logger = logger;
    this.useLazyWidgetBuilders = useLazyWidgetBuilders;

    handlerRegistrationJClass = oracle.findType(HandlerRegistration.class.getName());
    eventHandlerJClass = oracle.findType(EventHandler.class.getName());
  }

  /**
   * Runs the evaluator in the given class according to the valid fields
   * extracted from the template (via attribute ui:field).
   *
   * @param writer the writer used to output the results
   * @param fieldManager the field manager instance
   * @param uiOwner the name of the class evaluated here that owns the template
   */
  public void run(IndentedWriter writer, FieldManager fieldManager,
      String uiOwner) throws UnableToCompleteException {

    // Iterate through all methods defined in the class.
    for (JMethod method : ownerClass.getUiHandlers()) {
      // Evaluate the method.
      String boundMethod = method.getName();
      if (method.isPrivate()) {
        logger.die("Method '%s' cannot be private.", boundMethod);
      }

      // Retrieves both event and handler types.
      JParameter[] parameters = method.getParameters();
      if (parameters.length != 1) {
        logger.die("Method '%s' must have a single event parameter defined.",
            boundMethod);
      }
      JClassType eventType = parameters[0].getType().isClass();
      if (eventType == null) {
        logger.die("Parameter type is not a class.");
      }

      JClassType handlerType = getHandlerForEvent(eventType);
      if (handlerType == null) {
        logger.die("Parameter '%s' is not an event (subclass of GwtEvent).",
            eventType.getName());
      }

      // Cool to add the handler in the output.
      String handlerVarName = HANDLER_BASE_NAME + (++varCounter);
      writeHandler(writer, uiOwner, handlerVarName, handlerType, eventType,
          boundMethod);

      // Adds the handler created above.
      UiHandler annotation = method.getAnnotation(UiHandler.class);
      for (String objectName : annotation.value()) {
        // Is the field object valid?
        FieldWriter fieldWriter = fieldManager.lookup(objectName);
        if (fieldWriter == null) {
          logger.die(
              ("Method '%s' can not be bound. You probably missed ui:field='%s' "
                  + "in the template."), boundMethod, objectName);
        }

        // Retrieves the "add handler" method in the object.
        JMethod addHandlerMethodType = getAddHandlerMethodForObject(
            fieldWriter.getInstantiableType(), handlerType);
        if (addHandlerMethodType == null) {
          logger.die("Field '%s' does not have an 'add%s' method associated.",
              objectName, handlerType.getName());
        }

        // Cool to tie the handler into the object.
        writeAddHandler(writer, fieldManager, handlerVarName,
            addHandlerMethodType.getName(), objectName);
      }
    }
  }

  /**
   * Writes a handler entry using the given writer.
   *
   * @param writer the writer used to output the results
   * @param uiOwner the name of the class evaluated here that owns the template
   * @param handlerVarName the name of the handler variable
   * @param handlerType the handler we want to create
   * @param eventType the event associated with the handler
   * @param boundMethod the method bound in the handler
   */
  protected void writeHandler(IndentedWriter writer, String uiOwner,
      String handlerVarName, JClassType handlerType, JClassType eventType,
      String boundMethod) throws UnableToCompleteException {

    // Retrieves the single method (usually 'onSomething') related to all
    // handlers. Ex: onClick in ClickHandler, onBlur in BlurHandler ...
    JMethod[] methods = handlerType.getMethods();
    if (methods.length != 1) {
      logger.die("'%s' has more than one method defined.",
          handlerType.getName());
    }

    // Checks if the method has an Event as parameter. Ex: ClickEvent in
    // onClick, BlurEvent in onBlur ...
    JParameter[] parameters = methods[0].getParameters();
    if (parameters.length != 1 || parameters[0].getType() != eventType) {
      logger.die("Method '%s' needs '%s' as parameter", methods[0].getName(),
          eventType.getName());
    }

    writer.newline();
    writer.write("final %1$s %2$s = new %1$s() {",
        handlerType.getParameterizedQualifiedSourceName(), handlerVarName);
    writer.indent();
    writer.write("public void %1$s(%2$s event) {", methods[0].getName(),
        eventType.getParameterizedQualifiedSourceName());
    writer.indent();
    writer.write("%1$s.%2$s(event);", uiOwner, boundMethod);
    writer.outdent();
    writer.write("}");
    writer.outdent();
    writer.write("};");
  }

  /**
   * Adds the created handler to the given object (field).
   *
   * @param writer the writer used to output the results
   * @param handlerVarName the name of the handler variable
   * @param addHandlerMethodName the "add handler" method name associated with
   *          the object
   * @param objectName the name of the object we want to tie the handler
   */
  void writeAddHandler(IndentedWriter writer, FieldManager fieldManager,
      String handlerVarName, String addHandlerMethodName, String objectName) {
    if (useLazyWidgetBuilders) {
      fieldManager.require(objectName).addStatement("%1$s.%2$s(%3$s);",
          objectName, addHandlerMethodName, handlerVarName);
    } else {
      writer.write("%1$s.%2$s(%3$s);", objectName, addHandlerMethodName,
          handlerVarName);
    }
  }

  /**
   * Checks if a specific handler is valid for a given object and return the
   * method that ties them. The object must override a method that returns
   * {@link com.google.gwt.event.shared.HandlerRegistration} and receives a
   * single input parameter of the same type of handlerType.
   *
   * <p>
   * Output an error in case more than one method match the conditions described
   * above.
   * </p>
   *
   * <pre>
   *   <b>Examples:</b>
   *    - HandlerRegistration addClickHandler(ClickHandler handler)
   *    - HandlerRegistration addMouseOverHandler(MouseOverHandler handler)
   *    - HandlerRegistration addSubmitCompleteHandler(
   *          FormPanel.SubmitCompleteHandler handler)
   * </pre>
   *
   * @param objectType the object type we want to check
   * @param handlerType the handler type we want to check in the object
   *
   * @return the method that adds handlerType into objectType, or <b>null</b> if
   *         no method was found
   */
  private JMethod getAddHandlerMethodForObject(JClassType objectType,
      JClassType handlerType) throws UnableToCompleteException {
    JMethod handlerMethod = null;
    JMethod alternativeHandlerMethod = null;
    for (JMethod method : objectType.getInheritableMethods()) {

      // Condition 1: returns HandlerRegistration?
      if (method.getReturnType() == handlerRegistrationJClass) {

        // Condition 2: single parameter of the same type of handlerType?
        JParameter[] parameters = method.getParameters();
        if (parameters.length != 1) {
          continue;
        }

        JType subjectHandler = parameters[0].getType();

        if (handlerType.equals(subjectHandler)) {

          // Condition 3: does more than one method match the condition?
          if (handlerMethod != null) {
            logger.die(
                ("This handler cannot be generated. Methods '%s' and '%s' are "
                    + "ambiguous. Which one to pick?"), method, handlerMethod);
          }

          handlerMethod = method;
        }

        /**
         * Normalize the parameter and check for an alternative handler method.
         * Might be the case where the given objectType is generic. In this
         * situation we need to normalize the method parameter to test for
         * equality. For instance:
         *
         *   handlerType => TableHandler<String>
         *   subjectHandler => TableHandler
         *
         * This is done as an alternative handler method to preserve the
         * original logic.
         */
        JParameterizedType ptype = handlerType.isParameterized();
        if (ptype != null) {
          if (subjectHandler.equals(ptype.getRawType())) {
            alternativeHandlerMethod = method;
          }
        }
      }
    }

    return (handlerMethod != null) ? handlerMethod : alternativeHandlerMethod;
  }

  /**
   * Retrieves the handler associated with the event.
   *
   * @param eventType the given event
   * @return the associated handler, <code>null</code> if not found
   */
  private JClassType getHandlerForEvent(JClassType eventType) {

    // All handlers event must have an overrided method getAssociatedType().
    // We take advantage of this information to get the associated handler.
    // Ex:
    // com.google.gwt.event.dom.client.ClickEvent
    // ---> com.google.gwt.event.dom.client.ClickHandler
    //
    // com.google.gwt.event.dom.client.BlurEvent
    // ---> com.google.gwt.event.dom.client.BlurHandler

    if (eventType == null) {
      return null;
    }

    JMethod method = eventType.findMethod("getAssociatedType", new JType[0]);
    if (method == null) {
      logger.warn(
          "Method 'getAssociatedType()' could not be found in the event '%s'.",
          eventType.getName());
      return null;
    }

    JType returnType = method.getReturnType();
    if (returnType == null) {
      logger.warn(
          "The method 'getAssociatedType()' in the event '%s' returns void.",
          eventType.getName());
      return null;
    }

    JParameterizedType isParameterized = returnType.isParameterized();
    if (isParameterized == null) {
      logger.warn(
          "The method 'getAssociatedType()' in '%s' does not return Type<? extends EventHandler>.",
          eventType.getName());
      return null;
    }

    JClassType[] argTypes = isParameterized.getTypeArgs();
    if ((argTypes.length != 1)
        && !argTypes[0].isAssignableTo(eventHandlerJClass)) {
      logger.warn(
          "The method 'getAssociatedType()' in '%s' does not return Type<? extends EventHandler>.",
          eventType.getName());
      return null;
    }

    return argTypes[0];
  }
}
