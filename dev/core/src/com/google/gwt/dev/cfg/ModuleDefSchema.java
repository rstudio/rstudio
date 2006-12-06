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
package com.google.gwt.dev.cfg;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.js.JsParser;
import com.google.gwt.dev.js.JsParserException;
import com.google.gwt.dev.js.JsParserException.SourceDetail;
import com.google.gwt.dev.js.ast.JsExprStmt;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsStatements;
import com.google.gwt.dev.util.Empty;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.xml.AttributeConverter;
import com.google.gwt.dev.util.xml.Schema;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Configures a module definition object using XML.
 */
public class ModuleDefSchema extends Schema {

  private final class BodySchema extends Schema {

    protected Schema __define_property_begin(PropertyName name,
        PropertyValue[] values) throws UnableToCompleteException {
      if (moduleDef.getProperties().find(name.token) != null) {
        // Disallow redefinition.
        String msg = "Attempt to redefine property '" + name.token + "'";
        logger.log(TreeLogger.ERROR, msg, null);
        throw new UnableToCompleteException();
      }

      Property prop = moduleDef.getProperties().create(name.token);
      for (int i = 0; i < values.length; i++) {
        prop.addKnownValue(values[i].token);
      }

      // No children.
      return null;
    }

    protected Schema __extend_property_begin(Property property,
        PropertyValue[] values) {
      for (int i = 0; i < values.length; i++) {
        property.addKnownValue(values[i].token);
      }

      // No children.
      return null;
    }

    protected Schema __entry_point_begin(String className) {
      moduleDef.addEntryPointTypeName(className);
      return null;
    }

    protected Schema __fail_begin() {
      RuleFail rule = new RuleFail();
      moduleDef.getRules().prepend(rule);
      return new ConditionSchema(rule.getRootCondition());
    }

    protected Schema __generate_with_begin(Generator gen) {
      RuleGenerateWith rule = new RuleGenerateWith(gen);
      moduleDef.getRules().prepend(rule);
      return new ConditionSchema(rule.getRootCondition());
    }

    protected Schema __inherits_begin(String name)
        throws UnableToCompleteException {
      TreeLogger branch = logger.branch(TreeLogger.TRACE,
        "Loading inherited module '" + name + "'", null);
      loader.nestedLoad(branch, name, moduleDef);
      return null;
    }

    protected Schema __property_provider_begin(Property property) {
      property.setProvider(new PropertyProvider(property));
      return fChild = new PropertyProviderBodySchema();
    }

    protected void __property_provider_end(Property property)
        throws UnableToCompleteException {
      PropertyProviderBodySchema childSchema = ((PropertyProviderBodySchema) fChild);
      String script = childSchema.getScript();
      if (script == null) {
        // This is a problem.
        //
        logger.log(TreeLogger.ERROR,
          "Property providers must specify a JavaScript body", null);
        throw new UnableToCompleteException();
      }

      int lineNumber = childSchema.getStartLineNumber();
      JsFunction fn = parseJsBlock(lineNumber, script);

      property.getProvider().setBody(fn.getBody());
    }

    protected Schema __public_begin(String path, String includes,
        String excludes, String defaultExcludes, String caseSensitive) {
      return fChild = new IncludeExcludeSchema();
    }

    protected void __public_end(String path, String includes, String excludes,
        String defaultExcludes, String caseSensitive) {
      IncludeExcludeSchema childSchema = ((IncludeExcludeSchema) fChild);
      foundAnyPublic = true;

      Set includeSet = childSchema.getIncludes();
      addDelimitedStringToSet(includes, "[ ,]", includeSet);
      String[] includeList = (String[]) includeSet.toArray(new String[includeSet.size()]);

      Set excludeSet = childSchema.getExcludes();
      addDelimitedStringToSet(excludes, "[ ,]", excludeSet);
      String[] excludeList = (String[]) excludeSet.toArray(new String[excludeSet.size()]);

      boolean doDefaultExcludes = "yes".equalsIgnoreCase(defaultExcludes)
        || "true".equalsIgnoreCase(defaultExcludes);
      boolean doCaseSensitive = "yes".equalsIgnoreCase(caseSensitive)
        || "true".equalsIgnoreCase(caseSensitive);

      addPublicPackage(modulePackageAsPath, path, includeList, excludeList,
        doDefaultExcludes, doCaseSensitive);
    }

    protected Schema __replace_with_begin(String className) {
      RuleReplaceWith rule = new RuleReplaceWith(className);
      moduleDef.getRules().prepend(rule);
      return new ConditionSchema(rule.getRootCondition());
    }

    /**
     * @param src a partial or full url to a script file to inject
     * @return <code>null</code> since there can be no children
     */
    protected Schema __script_begin(String src) {
      return fChild = new ScriptReadyBodySchema();
    }

    protected void __script_end(String src) throws UnableToCompleteException {
      ScriptReadyBodySchema childSchema = (ScriptReadyBodySchema) fChild;
      String js = childSchema.getScriptReadyBlock();
      if (js == null) {
        // This is a problem.
        //
        logger.log(
          TreeLogger.ERROR,
          "Injected scripts require an associated JavaScript block that indicates when the corresponding script is fully loaded and ready for use",
          null);
        throw new UnableToCompleteException();
      }

      // For consistency, we allow the ready functions to use $wnd even though
      // they'll be running in the context of the host html window anyway.
      // We make up the difference by injecting a local variable into the
      // function we wrap around their code.
      js = "var $wnd = window; " + js;

      int lineNumber = childSchema.getStartLineNumber();
      JsFunction fn = parseJsBlock(lineNumber, js);
      Script script = new Script(src, fn);
      moduleDef.getScripts().append(script);
    }

    protected Schema __servlet_begin(String path, String servletClass)
        throws UnableToCompleteException {

      // Only absolute paths, although it is okay to have multiple slashes.
      if (!path.startsWith("/")) {
        logger.log(TreeLogger.ERROR, "Servlet path '" + path
          + "' must begin with forward slash (e.g. '/foo')", null);
        throw new UnableToCompleteException();
      }

      // Map the path within this module.
      moduleDef.mapServlet(path, servletClass);

      return null;
    }

    protected Schema __set_property_begin(Property prop, PropertyValue value) {
      prop.setActiveValue(value.token);

      // No children.
      return null;
    }

    /**
     * Indicates which subdirectories contain translatable source without
     * necessarily adding a sourcepath entry.
     */
    protected Schema __source_begin(String path) {
      foundExplicitSourceOrSuperSource = true;

      // Build a new path entry rooted at the classpath base.
      //
      addSourcePackage(modulePackageAsPath, path, false);
      return null;
    }

    /**
     * @param src a partial or full url to a stylesheet file to inject
     * @return <code>null</code> since there can be no children
     */
    protected Schema __stylesheet_begin(String src) {
      moduleDef.getStyles().append(src);
      return null;
    }

    /**
     * Like adding a translatable source package, but such that it uses the
     * module's package itself as its sourcepath root entry.
     */
    protected Schema __super_source_begin(String path) {
      foundExplicitSourceOrSuperSource = true;

      // Build a new path entry rooted at this module's dir.
      //
      addSourcePackage(modulePackageAsPath, path, true);
      return null;
    }

    private void addDelimitedStringToSet(String delimited, String delimiter,
        Set toSet) {
      if (delimited.length() > 0) {
        String[] split = delimited.split(delimiter);
        for (int i = 0; i < split.length; ++i) {
          if (split[i].length() > 0) {
            toSet.add(split[i]);
          }
        }
      }
    }

    private void addPublicPackage(String parentDir, String relDir,
        String[] includeList, String[] excludeList, boolean defaultExcludes,
        boolean caseSensitive) {
      String normChildDir = normalizePathEntry(relDir);
      if (normChildDir.startsWith("/")) {
        logger.log(TreeLogger.WARN, "Non-relative public package: "
          + normChildDir, null);
        return;
      }
      if (normChildDir.startsWith("./") || normChildDir.indexOf("/./") >= 0) {
        logger.log(TreeLogger.WARN, "Non-canonical public package: "
          + normChildDir, null);
        return;
      }
      if (normChildDir.startsWith("../") || normChildDir.indexOf("/../") >= 0) {
        logger.log(TreeLogger.WARN, "Non-canonical public package: "
          + normChildDir, null);
        return;
      }
      String fullDir = parentDir + normChildDir;
      moduleDef.addPublicPackage(fullDir, includeList, excludeList,
        defaultExcludes, caseSensitive);
    }

    private void addSourcePackage(String parentDir, String relDir,
        boolean isSuperSource) {
      String normChildDir = normalizePathEntry(relDir);
      if (normChildDir.startsWith("/")) {
        logger.log(TreeLogger.WARN, "Non-relative source package: "
          + normChildDir, null);
        return;
      }
      if (normChildDir.startsWith("./") || normChildDir.indexOf("/./") >= 0) {
        logger.log(TreeLogger.WARN, "Non-canonical source package: "
          + normChildDir, null);
        return;
      }
      if (normChildDir.startsWith("../") || normChildDir.indexOf("/../") >= 0) {
        logger.log(TreeLogger.WARN, "Non-canonical source package: "
          + normChildDir, null);
        return;
      }

      String fullDir = parentDir + normChildDir;
      if (isSuperSource) {
        moduleDef.addSuperSourcePackage(fullDir);
      } else {
        moduleDef.addSourcePackage(fullDir);
      }
    }

    /**
     * Normalizes a path entry such that it does not start with but does end
     * with '/'.
     */
    private String normalizePathEntry(String path) {
      path = path.trim();

      if (path.length() == 0) {
        return "";
      }

      path = path.replace('\\', '/');

      if (!path.endsWith("/")) {
        path += "/";
      }

      return path;
    }

    protected final String __define_property_1_name = null;
    protected final String __define_property_2_values = null;
    protected final String __extend_property_1_name = null;
    protected final String __extend_property_2_values = null;
    protected final String __entry_point_1_class = null;
    protected final String __generate_with_1_class = null;
    protected final String __inherits_1_name = null;
    protected final String __property_provider_1_name = null;
    protected final String __public_1_path = null;
    protected final String __public_2_includes = "";
    protected final String __public_3_excludes = "";
    protected final String __public_4_defaultexcludes = "yes";
    protected final String __public_5_casesensitive = "true";
    protected final String __replace_with_1_class = null;
    protected final String __script_1_src = null;
    protected final String __servlet_1_path = null;
    protected final String __servlet_2_class = null;
    protected final String __set_property_1_name = null;
    protected final String __set_property_2_value = null;
    protected final String __source_1_path = "";
    protected final String __stylesheet_1_src = null;
    protected final String __super_source_1_path = "";
    private Schema fChild;
  }

  private final class ConditionSchema extends Schema {

    public ConditionSchema(CompoundCondition parentCondition) {
      this.parentCondition = parentCondition;
    }

    protected Schema __all_begin() {
      CompoundCondition cond = new ConditionAll();
      parentCondition.getConditions().add(cond);
      return new ConditionSchema(cond);
    }

    protected Schema __any_begin() {
      CompoundCondition cond = new ConditionAny();
      parentCondition.getConditions().add(cond);
      return new ConditionSchema(cond);
    }

    protected Schema __none_begin() {
      CompoundCondition cond = new ConditionNone();
      parentCondition.getConditions().add(cond);
      return new ConditionSchema(cond);
    }

    // We intentionally use the Property type here for tough-love on module
    // writers. It prevents them from trying to create property providers for
    // unknown properties.
    //
    protected Schema __when_property_is_begin(Property prop, PropertyValue value) {
      Condition cond = new ConditionWhenPropertyIs(prop.getName(), value.token);
      parentCondition.getConditions().add(cond);

      // No children allowed.
      return null;
    }

    protected Schema __when_type_assignable_begin(String className) {
      Condition cond = new ConditionWhenTypeAssignableTo(className);
      parentCondition.getConditions().add(cond);

      // No children allowed.
      return null;
    }

    protected Schema __when_type_is_begin(String className) {
      Condition cond = new ConditionWhenTypeIs(className);
      parentCondition.getConditions().add(cond);

      // No children allowed.
      return null;
    }

    protected final String __when_property_is_1_name = null;
    protected final String __when_property_is_2_value = null;
    protected final String __when_type_assignable_1_class = null;
    protected final String __when_type_is_1_class = null;
    private final CompoundCondition parentCondition;
  }

  private final class IncludeExcludeSchema extends Schema {

    public Set getExcludes() {
      return excludes;
    }

    public Set getIncludes() {
      return includes;
    }

    protected Schema __exclude_begin(String name) {
      excludes.add(name);
      return null;
    }

    protected Schema __include_begin(String name) {
      includes.add(name);
      return null;
    }

    protected final String __exclude_1_name = null;
    protected final String __include_1_name = null;
    private final Set excludes = new HashSet();
    private final Set includes = new HashSet();
  }

  /**
   * Creates singleton instances of objects based on an attribute containing a
   * class name.
   */
  private final class ObjAttrCvt extends AttributeConverter {

    public ObjAttrCvt(Class reqdSuperclass) {
      fReqdSuperclass = reqdSuperclass;
    }

    public Object convertToArg(Schema schema, int lineNumber, String elemName,
        String attrName, String attrValue) throws UnableToCompleteException {

      Object found = singletonsByName.get(attrValue);
      if (found != null) {
        // Found in the cache.
        //
        return found;
      }

      try {
        // Load the class.
        //
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Class clazz = cl.loadClass(attrValue);

        // Make sure it's compatible.
        //
        if (!fReqdSuperclass.isAssignableFrom(clazz)) {
          Messages.INVALID_CLASS_DERIVATION.log(logger, clazz, fReqdSuperclass,
            null);
          throw new UnableToCompleteException();
        }

        Object object = clazz.newInstance();
        singletonsByName.put(attrValue, object);
        return object;
      } catch (ClassNotFoundException e) {
        Messages.UNABLE_TO_LOAD_CLASS.log(logger, attrValue, e);
        throw new UnableToCompleteException();
      } catch (InstantiationException e) {
        Messages.UNABLE_TO_CREATE_OBJECT.log(logger, attrValue, e);
        throw new UnableToCompleteException();
      } catch (IllegalAccessException e) {
        Messages.UNABLE_TO_CREATE_OBJECT.log(logger, attrValue, e);
        throw new UnableToCompleteException();
      }
    }

    private final Class fReqdSuperclass;
  }

  /**
   * Converts property names into their corresponding property objects.
   */
  private final class PropertyAttrCvt extends AttributeConverter {
    public Object convertToArg(Schema schema, int line, String elem,
        String attr, String value) throws UnableToCompleteException {
      // Find the named property.
      //
      Property prop = moduleDef.getProperties().find(value);

      if (prop != null) {
        // Found it.
        //
        return prop;
      } else {
        // Property not defined. This is a problem.
        //
        Messages.PROPERTY_NOT_FOUND.log(logger, value, null);
        throw new UnableToCompleteException();
      }
    }
  }

  private static class PropertyName {
    public PropertyName(String token) {
      this.token = token;
    }

    public final String token;
  }

  /**
   * Converts a string into a property name, validating it in the process.
   */
  private final class PropertyNameAttrCvt extends AttributeConverter {

    public Object convertToArg(Schema schema, int line, String elem,
        String attr, String value) throws UnableToCompleteException {
      // Ensure each part of the name is valid.
      //
      String[] tokens = (value + ". ").split("\\.");
      for (int i = 0; i < tokens.length - 1; i++) {
        String token = tokens[i];
        if (!Util.isValidJavaIdent(token)) {
          Messages.PROPERTY_NAME_INVALID.log(logger, value, null);
          throw new UnableToCompleteException();
        }
      }

      // It is a valid name.
      //
      return new PropertyName(value);
    }
  }

  private class PropertyProviderBodySchema extends Schema {

    public PropertyProviderBodySchema() {
    }

    public void __text(String text) {
      if (script == null) {
        script = new StringBuffer();
        startLineNumber = getLineNumber();
      }
      script.append(text);
    }

    public String getScript() {
      return script != null ? script.toString() : null;
    }

    public int getStartLineNumber() {
      return startLineNumber;
    }

    private StringBuffer script;
    private int startLineNumber = -1;
  }

  private static class PropertyValue {
    public PropertyValue(String token) {
      this.token = token;
    }

    public final String token;
  }

  /**
   * Converts a comma-separated string into an array of property value tokens.
   */
  private final class PropertyValueArrayAttrCvt extends AttributeConverter {
    public Object convertToArg(Schema schema, int line, String elem,
        String attr, String value) throws UnableToCompleteException {
      String[] tokens = value.split(",");
      PropertyValue[] values = new PropertyValue[tokens.length];

      // Validate each token as we copy it over.
      //
      for (int i = 0; i < tokens.length; i++) {
        values[i] = (PropertyValue) propValueAttrCvt.convertToArg(schema, line,
          elem, attr, tokens[i]);
      }

      return values;
    }
  }

  /**
   * Converts a string into a property value, validating it in the process.
   */
  private final class PropertyValueAttrCvt extends AttributeConverter {
    public Object convertToArg(Schema schema, int line, String elem,
        String attr, String value) throws UnableToCompleteException {

      String token = value.trim();
      if (Util.isValidJavaIdent(token)) {
        return new PropertyValue(token);
      } else {
        Messages.PROPERTY_VALUE_INVALID.log(logger, token, null);
        throw new UnableToCompleteException();
      }
    }
  }

  private class ScriptReadyBodySchema extends Schema {

    public ScriptReadyBodySchema() {
    }

    public void __text(String text) {
      if (script == null) {
        script = new StringBuffer();
        startLineNumber = getLineNumber();
      }
      script.append(text);
    }

    public String getScriptReadyBlock() {
      return script != null ? script.toString() : null;
    }

    public int getStartLineNumber() {
      return startLineNumber;
    }

    private StringBuffer script;
    private int startLineNumber = -1;
  }

  private static final Map singletonsByName = new HashMap();

  public ModuleDefSchema(TreeLogger logger, ModuleDefLoader loader,
      URL moduleURL, String modulePackageAsPath, ModuleDef toConfigure) {
    this.logger = logger;
    this.loader = loader;
    this.moduleURL = moduleURL;
    this.modulePackageAsPath = modulePackageAsPath;
    assert (modulePackageAsPath.endsWith("/") || modulePackageAsPath.equals(""));
    this.moduleDef = toConfigure;
    this.bodySchema = new BodySchema();

    registerAttributeConverter(PropertyName.class, propNameAttrCvt);
    registerAttributeConverter(Property.class, propAttrCvt);
    registerAttributeConverter(PropertyValue.class, propValueAttrCvt);
    registerAttributeConverter(PropertyValue[].class, propValueArrayAttrCvt);
    registerAttributeConverter(Generator.class, genAttrCvt);
  }

  protected Schema __module_begin() {
    return bodySchema;
  }

  protected void __module_end() {
    // Maybe infer source and public.
    //
    if (!foundExplicitSourceOrSuperSource) {
      bodySchema.addSourcePackage(modulePackageAsPath, "client", false);
    }

    if (!foundAnyPublic) {
      bodySchema.addPublicPackage(modulePackageAsPath, "public", Empty.STRINGS,
        Empty.STRINGS, true, true);
    }
  }

  /**
   * Parses handwritten JavaScript found in the module xml, logging an error
   * message and throwing an exception if there's a problem.
   * 
   * @param startLineNumber the start line number where the script was found;
   *          used to report errors
   * @param script the JavaScript to wrap in "function() { script }" to parse
   * @return the parsed function
   * @throws UnableToCompleteException
   */
  private JsFunction parseJsBlock(int startLineNumber, String script)
      throws UnableToCompleteException {
    script = "function() { " + script + "}";
    StringReader r = new StringReader(script);
    JsStatements stmts;
    try {
      stmts = jsParser.parse(jsPgm.getScope(), r, startLineNumber);
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Error reading script source", e);
      throw new UnableToCompleteException();
    } catch (JsParserException e) {
      SourceDetail dtl = e.getSourceDetail();
      if (dtl != null) {
        StringBuffer sb = new StringBuffer();
        sb.append(moduleURL.toExternalForm());
        sb.append("(");
        sb.append(dtl.getLine());
        sb.append(", ");
        sb.append(dtl.getLineOffset());
        sb.append("): ");
        sb.append(e.getMessage());
        logger.log(TreeLogger.ERROR, sb.toString(), e);
      } else {
        logger.log(TreeLogger.ERROR, "Error parsing JavaScript source", e);
      }
      throw new UnableToCompleteException();
    }

    // Rip the body out of the parsed function and attach the JavaScript
    // AST to the method.
    //
    JsFunction fn = (JsFunction) ((JsExprStmt) stmts.get(0)).getExpression();
    return fn;
  }

  private final BodySchema bodySchema;
  private boolean foundAnyPublic;
  private boolean foundExplicitSourceOrSuperSource;
  private final ObjAttrCvt genAttrCvt = new ObjAttrCvt(Generator.class);
  private final JsParser jsParser = new JsParser();
  private final JsProgram jsPgm = new JsProgram();
  private final ModuleDefLoader loader;
  private final TreeLogger logger;
  private final ModuleDef moduleDef;
  private final String modulePackageAsPath;
  private final URL moduleURL;
  private final PropertyAttrCvt propAttrCvt = new PropertyAttrCvt();
  private final PropertyNameAttrCvt propNameAttrCvt = new PropertyNameAttrCvt();
  private final PropertyValueArrayAttrCvt propValueArrayAttrCvt = new PropertyValueArrayAttrCvt();
  private final PropertyValueAttrCvt propValueAttrCvt = new PropertyValueAttrCvt();

}
