/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.i18n.rebind;

import com.google.gwt.codegen.rebind.GwtCodeGenContext;
import com.google.gwt.codegen.server.CodeGenContext;
import com.google.gwt.codegen.server.CodeGenUtils;
import com.google.gwt.codegen.server.JavaSourceWriterBuilder;
import com.google.gwt.codegen.server.SourceWriter;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.i18n.client.Constants;
import com.google.gwt.i18n.client.ConstantsWithLookup;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.i18n.client.Messages;
import com.google.gwt.i18n.shared.GwtLocale;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Generator used to bind classes extending the <code>Localizable</code> and
 * <code>Constants</code> interfaces.
 */
public class LocalizableGenerator extends Generator {

  /**
   * Comparator for methods - sorts first by visibility, then name, then number
   * of arguments, then argument names.
   */
  public class JMethodComparator implements Comparator<JMethod> {

    public int compare(JMethod a, JMethod b) {
      if (a.isPublic() != b.isPublic()) {
        return a.isPublic() ? -1 : 1;
      }
      if (a.isDefaultAccess() != b.isDefaultAccess()) {
        return a.isDefaultAccess() ? -1 : 1;
      }
      if (a.isProtected() != b.isProtected()) {
        return a.isProtected() ? -1 : 1;
      }
      int c = a.getName().compareTo(b.getName());
      if (c != 0) {
        return c;
      }
      JParameter[] aParams = a.getParameters();
      JParameter[] bParams = b.getParameters();
      c = aParams.length - bParams.length;
      if (c != 0) {
        return c;
      }
      for (int i = 0; i < aParams.length; ++i) {
        c = aParams[i].getName().compareTo(bParams[i].getName());
        if (c != 0) {
          return c;
        }
      }
      return 0;
    }
  }

   /**
    * Obsolete comment for GWT metadata - needs to be removed once all
    * references have been removed.
    */
  public static final String GWT_KEY = "gwt.key";

  public static final String CONSTANTS_NAME = Constants.class.getName();

  public static final String CONSTANTS_WITH_LOOKUP_NAME = ConstantsWithLookup.class.getName();

  public static final String MESSAGES_NAME = Messages.class.getName();

  private LocalizableLinkageCreator linkageCreator = new LocalizableLinkageCreator();

  /**
   * Generate an implementation for the given type.
   * 
   * @param logger error logger
   * @param context generator context
   * @param typeName target type name
   * @return a fully-qualified classname of the generated implementation, or
   *     null to use the base class
   * @throws UnableToCompleteException
   */
  @Override
  public final String generate(TreeLogger logger, GeneratorContext context,
      String typeName) throws UnableToCompleteException {
    // Get the current locale
    PropertyOracle propertyOracle = context.getPropertyOracle();
    LocaleUtils localeUtils = LocaleUtils.getInstance(logger, propertyOracle,
        context);
    GwtLocale locale = localeUtils.getCompileLocale();
    return generate(logger, context, typeName, localeUtils, locale);
  }

  /**
   * Generate an implementation for a given type.
   * 
   * @param logger
   * @param context
   * @param typeName
   * @param localeUtils
   * @param locale
   * @return a fully-qualified classname of the generated implementation, or
   *     null to use the base class
   * @throws UnableToCompleteException
   */
  public final String generate(TreeLogger logger, GeneratorContext context,
      String typeName, LocaleUtils localeUtils, GwtLocale locale) throws UnableToCompleteException {
    TypeOracle typeOracle = context.getTypeOracle();
    JClassType targetClass;
    try {
      targetClass = typeOracle.getType(typeName);
    } catch (NotFoundException e) {
      logger.log(TreeLogger.ERROR, "No such type", e);
      throw new UnableToCompleteException();
    }

    // Link current locale and interface type to correct implementation class.
    String generatedClass = AbstractLocalizableImplCreator.generateConstantOrMessageClass(
        logger, context, locale, targetClass);
    if (generatedClass != null) {
      return generatedClass;
    }

    // Now that we know it is a regular Localizable class, handle runtime
    // locales
    Set<GwtLocale> runtimeLocales = localeUtils.getRuntimeLocales();
    String returnedClass = linkageCreator.linkWithImplClass(logger, targetClass, locale);
    if (!runtimeLocales.isEmpty()) {
      Map<String, Set<GwtLocale>> localeMap = new TreeMap<String, Set<GwtLocale>>();
      Set<GwtLocale> localeSet = new TreeSet<GwtLocale>();
      localeSet.add(locale);
      localeMap.put(returnedClass, localeSet);
      for (GwtLocale rtLocale : runtimeLocales) {
        String rtClass = linkageCreator.linkWithImplClass(logger, targetClass, rtLocale);
        localeSet = localeMap.get(rtClass);
        if (localeSet == null) {
          localeSet = new TreeSet<GwtLocale>();
          localeMap.put(rtClass, localeSet);
        }
        localeSet.add(rtLocale);
      }
      if (localeMap.size() > 1) {
        CodeGenContext genCtx = new GwtCodeGenContext(logger, context);
        returnedClass = generateRuntimeSelection(genCtx, targetClass, returnedClass, locale,
            localeMap);
      }
    }

    return returnedClass;
  }

  /**
   * Generate a runtime-selection implementation of the target class if needed,
   * delegating all overridable methods to an instance chosen at runtime based
   * on the map of locales to implementing classes.
   * 
   * @param ctx code generator context
   * @param targetClass class being GWT.create'd
   * @param defaultClass the default implementation to use
   * @param locale compile-time locale for this runtime selection
   * @param localeMap map of target class names to the set of locales that are
   *     mapped to that implementation (for deterministic code generation, both
   *     the map and set should be ordered)
   * @return fully qualified classname of the runtime selection implementation
   */
  // @VisibleForTesting
  String generateRuntimeSelection(CodeGenContext ctx, JClassType targetClass, String defaultClass,
      GwtLocale locale, Map<String, Set<GwtLocale>> localeMap) {
    String className = targetClass.getName().replace('.', '_') + '_' + locale.getAsString()
        + "_runtimeSelection";
    String pkgName = targetClass.getPackage().getName();
    JavaSourceWriterBuilder builder = ctx.addClass(pkgName, className);
    if (builder != null) {
      writeRuntimeSelection(builder, targetClass, defaultClass, locale, localeMap);
    }
    return pkgName + '.' + className;
  }

  /**
   * Generate a runtime-selection implementation of the target class, delegating
   * all overridable methods to an instance chosen at runtime based on the map
   * of locales to implementing classes.
   * 
   * @param builder source writer builder
   * @param targetClass class being GWT.create'd
   * @param defaultClass the default implementation to use
   * @param locale compile-time locale for this runtime selection
   * @param localeMap map of target class names to the set of locales that are
   *     mapped to that implementation (for deterministic code generation, both
   *     the map and set should be ordered)
   */
  // @VisibleForTesting
  void writeRuntimeSelection(JavaSourceWriterBuilder builder, JClassType targetClass,
        String defaultClass, GwtLocale locale, Map<String, Set<GwtLocale>> localeMap) {
    boolean isInterface = targetClass.isInterface() != null;
    if (isInterface) {
      builder.addImplementedInterface(targetClass.getQualifiedSourceName());
    } else {
      builder.setSuperclass(targetClass.getQualifiedSourceName());
    }
    SourceWriter writer = builder.createSourceWriter();
    writer.println();
    writer.println("private " + targetClass.getQualifiedSourceName() + " instance;");
    for (JMethod method : collectOverridableMethods(targetClass)) {
      writer.println();
      if (!isInterface) {
        writer.println("@Override");
      }
      writer.println(method.getReadableDeclaration(false, true, true, true, true) + " {");
      writer.indent();
      writer.println("ensureInstance();");
      if (method.getReturnType() != JPrimitiveType.VOID) {
        writer.print("return ");
      }
      writer.print("instance." + method.getName() + '(');
      boolean first = true;
      for (JParameter param : method.getParameters()) {
        if (first) {
          first = false;
        } else {
          writer.print(", ");
        }
        writer.print(param.getName());
      }
      writer.println(");");
      writer.outdent();
      writer.println("}");
    }
    writer.println();
    writer.println("private void ensureInstance() {");
    writer.indent();
    writer.println("if (instance != null) {");
    writer.indentln("return;");
    writer.println("}");
    writer.println("String locale = " + LocaleInfo.class.getCanonicalName()
        + ".getCurrentLocale().getLocaleName();");
    String targetClassName = targetClass.getQualifiedSourceName() + '_' + locale.getAsString();
    for (Map.Entry<String, Set<GwtLocale>> entry : localeMap.entrySet()) {
      String implClassName = entry.getKey();
      if (defaultClass.equals(implClassName) || targetClassName.equals(implClassName)) {
        continue;
      }
      String prefix = "if (";
      for (GwtLocale match : entry.getValue()) {
        writer.print(prefix + CodeGenUtils.asStringLiteral(match.toString()) + ".equals(locale)");
        prefix = "\n    || ";
      }
      writer.println(") {");
      writer.indent();
      writer.println("instance = new " + implClassName + "();");
      writer.println("return;");
      writer.outdent();
      writer.println("}");
    }
    writer.println("instance = new " + defaultClass + "();");
    writer.outdent();
    writer.println("}");
    writer.close();
  }

  /**
   * @param targetClass
   * @return a set of overrideable methods, in the order they should appear in
   *     generated source 
   */
  private TreeSet<JMethod> collectOverridableMethods(JClassType targetClass) {
    TreeSet<JMethod> overrides = new TreeSet<JMethod>(new JMethodComparator());
    Set<String> seenSignatures = new HashSet<String>();
    // collect methods from superclass until we get to object
    for (JClassType clazz = targetClass; clazz != null; clazz = clazz.getSuperclass()) {
      if ("java.lang.Object".equals(clazz.getQualifiedSourceName())) {
        break;
      }
      for (JMethod method : clazz.getMethods()) {
        String signature = getSignature(method);
        if (!seenSignatures.contains(signature)) {
          seenSignatures.add(signature);
          if (!method.isPrivate() && !method.isFinal() && !method.isStatic()) {
            overrides.add(method);
          }
        }
      }
    }
    // collect methods from superinterfaces until hitting Localizable
    ArrayList<JClassType> todo = new ArrayList<JClassType>();
    todo.addAll(Arrays.asList(targetClass.getImplementedInterfaces()));
    while (!todo.isEmpty()) {
      JClassType clazz = todo.remove(0);
      for (JMethod method : clazz.getMethods()) {
        String signature = getSignature(method);
        if (!seenSignatures.contains(signature)) {
          seenSignatures.add(signature);
          if (!method.isPrivate() && !method.isFinal() && !method.isStatic()) {
            overrides.add(method);
          }
        }
      }
      if (!"Localizable".equals(clazz.getSimpleSourceName())) {
        todo.addAll(Arrays.asList(clazz.getImplementedInterfaces()));
      }
    }
    return overrides;
  }

  /**
   * @param method
   * @return JNI signature of the method
   */
  private String getSignature(JMethod method) {
    StringBuilder buf = new StringBuilder();
    buf.append(method.getName()).append('(');
    for (JParameter param : method.getParameters()) {
      JType type = param.getType();
      buf.append(type.getJNISignature());
    }
    return buf.append(')').toString();
  }
}
