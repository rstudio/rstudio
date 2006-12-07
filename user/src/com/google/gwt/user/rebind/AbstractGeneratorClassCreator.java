/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.user.rebind;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Abstract functionality needed to create classes needed to supply generators.
 */
public abstract class AbstractGeneratorClassCreator extends
    AbstractSourceCreator {

  /**
   * Returns all interface methods associated with the given type.
   * 
   * @param type associated type
   * @return interface methods.
   */
  public static JMethod[] getAllInterfaceMethods(JClassType type) {
    Map methods = new HashMap();
    getAllInterfaceMethodsAux(type, methods);
    return (JMethod[]) methods.values().toArray(new JMethod[methods.size()]);
  }

  private static void getAllInterfaceMethodsAux(JClassType type, Map m) {
    if (type.isInterface() != null) {
      JMethod[] methods = type.getMethods();
      for (int i = 0; i < methods.length; i++) {
        String s = uniqueMethodKey(methods[i]);
        if (m.get(s) == null) {
          m.put(s, methods[i]);
        }
      }
      JClassType[] supers = type.getImplementedInterfaces();
      for (int i = 0; i < supers.length; i++) {
        getAllInterfaceMethodsAux(supers[i], m);
      }
    }
  }

  private static String uniqueMethodKey(JMethod method) {
    String name = method.getName();
    name += "(";
    JParameter[] m = method.getParameters();
    for (int i = 0; i < m.length; i++) {
      name += m[i].getType() + " ";
    }
    name += ")";
    return name;
  }

  /**
   * List of registered method factories associated with <code>Constant</code>
   * method implementations.
   */
  protected Map methodFactories = new HashMap();

  /**
   * The interface the generator is conforming to.
   */
  JClassType targetClass;

  private final SourceWriter writer;

  /**
   * Creates a new class creator, supplies a place to write the class, the
   * interface to conform to, and the new name.
   * 
   * @param writer writer
   * @param targetClass class name
   */
  public AbstractGeneratorClassCreator(SourceWriter writer,
      JClassType targetClass) {
    this.targetClass = targetClass;
    this.writer = writer;
  }

  /**
   * Emits the new class.
   * 
   * @param logger
   * @throws UnableToCompleteException
   */
  public void emitClass(TreeLogger logger) throws UnableToCompleteException {
    logger = branch(logger, branchMessage());
    classPrologue();
    emitMethods(logger, targetClass);
    classEpilog();
    getWriter().println("}");
  }

  public JClassType getTarget() {
    return targetClass;
  }

  /**
   * Registers a method creator.
   * 
   * @param returnType return type that this creator handles.
   * @param creator creator to register
   */
  public void register(JType returnType, AbstractMethodCreator creator) {
    methodFactories.put(returnType, creator);
  }

  /**
   * Returns the standard message when constructing a branch.
   * 
   * @return branch message
   */
  protected String branchMessage() {
    return "Constructing " + targetClass;
  }

  /**
   * Entry point for subclass cleanup code.
   */
  protected void classEpilog() {
  }

  /**
   * Entry point for subclass setup code.
   */
  protected void classPrologue() {
  }

  /**
   * Emit method body, arguments are arg1...argN.
   * 
   * @param logger TODO
   * @param method method to generate
   * @throws UnableToCompleteException
   */
  protected abstract void emitMethodBody(TreeLogger logger, JMethod method)
      throws UnableToCompleteException;

  /**
   * Gets the method creator associated with the return type of the method.
   * 
   * @param logger
   * @param method method to create
   * @return the method creator
   * @throws UnableToCompleteException
   */
  protected AbstractMethodCreator getMethodCreator(TreeLogger logger,
      JMethod method) throws UnableToCompleteException {
    JType type = method.getReturnType();
    AbstractMethodCreator methodCreator = (AbstractMethodCreator) methodFactories.get(type);
    if (methodCreator == null) {
      String msg = "No current method creator exists for " + method
          + " only methods with return types of ";
      Iterator iter = this.methodFactories.keySet().iterator();
      while (iter.hasNext()) {
        msg += ((JType) iter.next()).getSimpleSourceName();
        if (iter.hasNext()) {
          msg += ", ";
        }
      }
      msg += " can be created";
      throw error(logger, msg);
    }
    return methodCreator;
  }

  /**
   * Gets the associated writer.
   * 
   * @return writer
   */
  protected SourceWriter getWriter() {
    return writer;
  }

  private void emitMethods(TreeLogger logger, JClassType cur)
      throws UnableToCompleteException {
    JMethod[] x = getAllInterfaceMethods(cur);
    for (int i = 0; i < x.length; i++) {
      genMethod(logger, x[i]);
      getWriter().println();
    }
  }

  /**
   * Generates a method declaration for the given method.
   * 
   * @param method method to generate
   * @throws UnableToCompleteException
   */
  private void genMethod(TreeLogger logger, JMethod method)
      throws UnableToCompleteException {
    String name = method.getName();
    String returnType = method.getReturnType().getQualifiedSourceName();
    getWriter().print("public " + returnType + " " + name + "(");
    JParameter[] params = method.getParameters();
    for (int i = 0; i < params.length; i++) {
      if (i != 0) {
        getWriter().print(",");
      }
      getWriter().print(
          params[i].getType().getParameterizedQualifiedSourceName() + " arg"
              + (i));
    }
    getWriter().println(") {");
    getWriter().indent();
    String methodName = method.getName();
    TreeLogger branch = branch(logger, "Generating method body for "
        + methodName + "()");
    emitMethodBody(branch, method);
    getWriter().outdent();
    getWriter().println("}");
  }
}
