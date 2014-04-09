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
package com.google.gwt.dev.cfg;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.Linker;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.LinkerOrder;
import com.google.gwt.core.ext.linker.PropertyProviderGenerator;
import com.google.gwt.dev.cfg.ModuleDef.ModuleType;
import com.google.gwt.dev.js.JsParser;
import com.google.gwt.dev.js.JsParserException;
import com.google.gwt.dev.js.ast.JsExprStmt;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.util.Empty;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dev.util.xml.AttributeConverter;
import com.google.gwt.dev.util.xml.Schema;

import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

// CHECKSTYLE_NAMING_OFF

/**
 * Configures a module definition object using XML.
 */
public class ModuleDefSchema extends Schema {

  private final class BodySchema extends Schema {

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __add_linker_1_name = null;

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __clear_configuration_property_1_name = null;

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __collapse_all_properties_1_value = "true";

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __collapse_property_1_name = null;

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __collapse_property_2_values = null;

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __define_configuration_property_1_name = null;

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __define_configuration_property_2_is_multi_valued = null;

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __define_linker_1_name = null;

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __define_linker_2_class = null;

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __define_property_1_name = null;

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __define_property_2_values = null;

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __entry_point_1_class = null;

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __extend_configuration_property_1_name = null;

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __extend_configuration_property_2_value = null;

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __extend_property_1_name = null;

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __extend_property_2_values = null;

    @SuppressWarnings("unused") // referenced reflectively ($ means optional)
    protected final String __extend_property_3_fallback_value$ = null;

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __generate_with_1_class = null;

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __inherits_1_name = null;

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __property_provider_1_name = null;

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __property_provider_2_generator = "";

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __public_1_path = null;

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __public_2_includes = "";

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __public_3_excludes = "";

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __public_4_skips = "";

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __public_5_defaultexcludes = "yes";

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __public_6_casesensitive = "true";

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __replace_with_1_class = null;

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __script_1_src = null;

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __servlet_1_path = null;

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __servlet_2_class = null;

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __set_configuration_property_1_name = null;

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __set_configuration_property_2_value = null;

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __set_property_1_name = null;

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __set_property_2_value = null;

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __set_property_fallback_1_name = null;

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __set_property_fallback_2_value = null;

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __resource_1_path = "";

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __source_1_path = "";

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __source_2_includes = "";

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __source_3_excludes = "";

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __source_4_skips = "";

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __source_5_defaultexcludes = "yes";

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __source_6_casesensitive = "true";

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __stylesheet_1_src = null;

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __super_source_1_path = "";

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __super_source_2_includes = "";

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __super_source_3_excludes = "";

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __super_source_4_skips = "";

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __super_source_5_defaultexcludes = "yes";

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __super_source_6_casesensitive = "true";

    /**
     * Used to accumulate binding property conditions before recording the
     * newly-allowed values.
     */
    private ConditionAll bindingPropertyCondition;

    private Schema fChild;

    @SuppressWarnings("unused") // called reflectively
    protected Schema __add_linker_begin(LinkerName name)
        throws UnableToCompleteException {
      if (moduleDef.getLinker(name.name) == null) {
        Messages.LINKER_NAME_INVALID.log(logger, name.name, null);
        throw new UnableToCompleteException();
      }
      moduleDef.addLinker(name.name);
      return null;
    }

    @SuppressWarnings("unused") // called reflectively
    protected Schema __clear_configuration_property_begin(PropertyName name)
        throws UnableToCompleteException {
      // Don't allow configuration properties with the same name as a
      // deferred-binding property.
      Property prop = moduleDef.getProperties().find(name.token);
      if (prop == null) {
        logger.log(TreeLogger.ERROR, "No property named " + name.token
            + " has been defined");
        throw new UnableToCompleteException();
      } else if (!(prop instanceof ConfigurationProperty)) {
        if (prop instanceof BindingProperty) {
          logger.log(TreeLogger.ERROR, "The property " + name.token
              + " is already defined as a deferred-binding property");
        } else {
          logger.log(TreeLogger.ERROR, "The property " + name.token
              + " is already defined as a property of unknown type");
        }
        throw new UnableToCompleteException();
      }

      ((ConfigurationProperty) prop).clear();

      // No children.
      return null;
    }

    @SuppressWarnings("unused") // called reflectively
    protected Schema __collapse_all_properties_begin(boolean collapse) {
      moduleDef.setCollapseAllProperties(collapse);
      return null;
    }

    @SuppressWarnings("unused") // called reflectively
    protected Schema __collapse_property_begin(PropertyName name,
        PropertyValueGlob[] values) throws UnableToCompleteException {
      Property prop = moduleDef.getProperties().find(name.token);
      if (prop == null) {
        logger.log(TreeLogger.ERROR, "No property named " + name.token
            + " has been defined");
        throw new UnableToCompleteException();
      } else if (!(prop instanceof BindingProperty)) {
        logger.log(TreeLogger.ERROR, "The property " + name.token
            + " is not a deferred-binding property");
        throw new UnableToCompleteException();
      }

      BindingProperty binding = (BindingProperty) prop;
      List<String> allowed = Arrays.asList(binding.getDefinedValues());

      String[] tokens = new String[values.length];
      boolean error = false;
      for (int i = 0, j = values.length; i < j; i++) {
        tokens[i] = values[i].token;
        if (values[i].isGlob()) {
          // Expanded later in BindingProperty
          continue;
        } else if (!allowed.contains(tokens[i])) {
          logger.log(TreeLogger.ERROR, "The value " + tokens[i]
              + " was not previously defined for the property "
              + binding.getName());
          error = true;
        }
      }

      if (error) {
        throw new UnableToCompleteException();
      }

      binding.addCollapsedValues(tokens);

      // No children
      return null;
    }

    @SuppressWarnings("unused") // called reflectively
    protected Schema __define_configuration_property_begin(PropertyName name,
        String is_multi_valued) throws UnableToCompleteException {
      boolean isMultiValued = toPrimitiveBoolean(is_multi_valued);

      // Don't allow configuration properties with the same name as a
      // deferred-binding property.
      Property existingProperty = moduleDef.getProperties().find(name.token);
      if (existingProperty == null) {
        // Create the property
        moduleDef.getProperties().createConfiguration(name.token, isMultiValued);
        if (!propertyDefinitions.containsKey(name.token)) {
          propertyDefinitions.put(name.token, moduleName);
        }
      } else if (existingProperty instanceof ConfigurationProperty) {
        // Allow redefinition only if the 'is-multi-valued' setting is identical
        // The previous definition may have been explicit, via
        // <define-configuration-property>, or implicit, via
        // <set-configuration-property>. In the latter case, the
        // value of the 'is-multi-valued' attribute was taken as false.
        String originalDefinition = propertyDefinitions.get(name.token);
        if (((ConfigurationProperty) existingProperty).allowsMultipleValues() != isMultiValued) {
          if (originalDefinition != null) {
            logger.log(
                TreeLogger.ERROR,
                "The configuration property named "
                    + name.token
                    + " is already defined with a different 'is-multi-valued' setting");
          } else {
            logger.log(
                TreeLogger.ERROR,
                "The configuration property named "
                    + name.token
                    + " is already defined implicitly by 'set-configuration-property'"
                    + " in " + propertySettings.get(name.token)
                    + " with 'is-multi-valued' set to 'false'");
          }
          throw new UnableToCompleteException();
        } else {
          if (originalDefinition != null) {
            logger.log(TreeLogger.WARN,
                "Ignoring identical definition of the configuration property named "
                    + name.token + " (originally defined in "
                    + originalDefinition + ", redefined in " + moduleName + ")");
          } else {
            logger.log(TreeLogger.WARN,
                "Definition of already set configuration property named "
                    + name.token + " in " + moduleName + " (set in "
                    + propertySettings.get(name.token)
                    + ").  This may be disallowed in the future.");
          }
        }
      } else {
        if (existingProperty instanceof BindingProperty) {
          logger.log(TreeLogger.ERROR, "The property " + name.token
              + " is already defined as a deferred-binding property");
        } else {
          // Future proofing if other subclasses are added.
          logger.log(TreeLogger.ERROR, "May not replace property named "
              + name.token + " of unknown type "
              + existingProperty.getClass().getName());
        }
        throw new UnableToCompleteException();
      }

      // No children.
      return null;
    }

    @SuppressWarnings("unused") // called reflectively
    protected Schema __define_linker_begin(LinkerName name,
        Class<? extends Linker> linker) throws UnableToCompleteException {
      if (!Linker.class.isAssignableFrom(linker)) {
        logger.log(TreeLogger.ERROR, "A linker must extend "
            + Linker.class.getName(), null);
        throw new UnableToCompleteException();
      }
      if (linker.getAnnotation(LinkerOrder.class) == null) {
        logger.log(TreeLogger.ERROR, "Linkers must be annotated with the "
            + LinkerOrder.class.getName() + " annotation", null);
        throw new UnableToCompleteException();
      }
      moduleDef.defineLinker(logger, name.name, linker);
      return null;
    }

    @SuppressWarnings("unused") // called reflectively
    protected Schema __define_property_begin(PropertyName name,
        PropertyValue[] values) throws UnableToCompleteException {

      Property existingProperty = moduleDef.getProperties().find(name.token);
      if (existingProperty != null) {
        // Disallow redefinition of properties, but provide a type-sensitive
        // error message to aid in diagnosis.
        if (existingProperty instanceof BindingProperty) {
          logger.log(TreeLogger.ERROR, "The deferred-binding property named "
              + name.token + " may not be redefined.");
        } else if (existingProperty instanceof ConfigurationProperty) {
          logger.log(TreeLogger.ERROR, "The property " + name.token
              + " is already defined as a configuration property");
        } else {
          // Future proofing if other subclasses are added.
          logger.log(TreeLogger.ERROR, "May not replace property named "
              + name.token + " of unknown type "
              + existingProperty.getClass().getName());
        }
        throw new UnableToCompleteException();
      }

      BindingProperty prop = moduleDef.getProperties().createBinding(name.token);

      for (int i = 0; i < values.length; i++) {
        moduleDef.addBindingPropertyDefinedValue(prop, values[i].token);
      }

      // No children.
      return null;
    }

    @SuppressWarnings("unused") // called reflectively
    protected Schema __entry_point_begin(String className) {
      moduleDef.addEntryPointTypeName(className);
      return null;
    }

    @SuppressWarnings("unused") // called reflectively
    protected Schema __extend_configuration_property_begin(PropertyName name, String value)
        throws UnableToCompleteException {

      // Property must already exist as a configuration property
      Property configurationProperty = moduleDef.getProperties().find(name.token);
      if ((configurationProperty == null)
          || !(configurationProperty instanceof ConfigurationProperty)) {
        logger.log(TreeLogger.ERROR,
            "The property " + name.token + " must already exist as a configuration property");
        throw new UnableToCompleteException();
      }

      ConfigurationProperty configProp = (ConfigurationProperty) configurationProperty;
      if (!configProp.allowsMultipleValues()) {
        logger.log(
            TreeLogger.ERROR, "The property " + name.token + " does not support multiple values");
        throw new UnableToCompleteException();
      }
      moduleDef.addConfigurationPropertyValue(configProp, value);

      return null;
    }

    @SuppressWarnings("unused") // called reflectively
    protected Schema __extend_property_begin(BindingProperty bindingProperty,
        PropertyValue[] values, PropertyFallbackValue fallbackValue)
        throws UnableToCompleteException {
      for (int i = 0; i < values.length; i++) {
        moduleDef.addBindingPropertyDefinedValue(bindingProperty, values[i].token);
      }

      // validate fallback-property value if present
      if (fallbackValue != null) {
        if (!bindingProperty.isDefinedValue(fallbackValue.token)) {
          logger.log(TreeLogger.ERROR,
              "The property " + bindingProperty.name + " fallback-value was not defined");
          throw new UnableToCompleteException();
        }
        // add fallback map to the property
        for (int i = 0; i < values.length; i++) {
          bindingProperty.addFallbackValue(values[i].token, fallbackValue.token);
        }
      }

      // No children.
      return null;
    }

    @SuppressWarnings("unused") // called reflectively
    protected Schema __fail_begin() {
      RuleFail rule = new RuleFail();
      moduleDef.addRule(rule);
      return new FullConditionSchema(rule.getRootCondition());
    }

    @SuppressWarnings("unused") // called reflectively
    protected Schema __generate_with_begin(Class<? extends Generator> generator)
        throws UnableToCompleteException {
      if (!Generator.class.isAssignableFrom(generator)) {
        logger.log(TreeLogger.ERROR, "A generator must extend "
            + Generator.class.getName(), null);
        throw new UnableToCompleteException();
      }
      RuleGenerateWith rule = new RuleGenerateWith(generator);
      // Generator rebind rules for even distant dependencies are always considered primary since
      // knowing whether they should be run is more complicated and is expected to be analyzed later
      // during compilation.
      moduleDef.getRules().prepend(rule);
      return new FullConditionSchema(rule.getRootCondition());
    }

    @SuppressWarnings("unused") // called reflectively
    protected Schema __inherits_begin(String name)
        throws UnableToCompleteException {
      moduleDef.addDirectDependent(moduleName, name);
      loader.nestedLoad(logger, name, moduleDef);
      return null;
    }

    @SuppressWarnings("unused") // called reflectively
    protected Schema __property_provider_begin(BindingProperty property,
        Class<? extends PropertyProviderGenerator> generator) {
      fChild = new ScriptSchema();
      return fChild;
    }

    @SuppressWarnings("unused") // called reflectively
    protected void __property_provider_end(BindingProperty property,
        Class<? extends PropertyProviderGenerator> generator)
        throws UnableToCompleteException {
      if (generator != null
          && !PropertyProviderGenerator.class.isAssignableFrom(generator)) {
        logger.log(TreeLogger.ERROR, "A property provider generator must "
            + "extend " + PropertyProviderGenerator.class.getName(), null);
        throw new UnableToCompleteException();
      }
      ScriptSchema childSchema = ((ScriptSchema) fChild);
      String script = childSchema.getScript();
      property.setProviderGenerator(generator);
      if (script == null) {
        if (generator == null) {
          // This is a problem.
          //
          logger.log(TreeLogger.ERROR, "Property providers must specify a "
              + "JavaScript body or a provider generator", null);
          throw new UnableToCompleteException();
        }
      } else {
        int lineNumber = childSchema.getStartLineNumber();
        JsFunction fn = parseJsBlock(lineNumber, script);
        property.setProvider(new PropertyProvider(fn.getBody().toSource()));
      }
    }

    @SuppressWarnings("unused") // called reflectively
    protected Schema __public_begin(String path, String includes,
        String excludes, String skips, String defaultExcludes,
        String caseSensitive) {
      return fChild = new IncludeExcludeSchema();
    }

    @SuppressWarnings("unused") // called reflectively
    protected void __public_end(String path, String includes, String excludes,
        String skips, String defaultExcludes, String caseSensitive) {
      IncludeExcludeSchema childSchema = ((IncludeExcludeSchema) fChild);
      foundAnyPublic = true;

      Set<String> includeSet = childSchema.getIncludes();
      addDelimitedStringToSet(includes, "[ ,]", includeSet);
      String[] includeList = includeSet.toArray(new String[includeSet.size()]);

      Set<String> excludeSet = childSchema.getExcludes();
      addDelimitedStringToSet(excludes, "[ ,]", excludeSet);
      String[] excludeList = excludeSet.toArray(new String[excludeSet.size()]);

      Set<String> skipSet = childSchema.getSkips();
      addDelimitedStringToSet(skips, "[ ,]", skipSet);
      String[] skipList = skipSet.toArray(new String[skipSet.size()]);

      boolean doDefaultExcludes = toPrimitiveBoolean(defaultExcludes);
      boolean doCaseSensitive = toPrimitiveBoolean(caseSensitive);

      addPublicPackage(modulePackageAsPath, path, includeList, excludeList,
          skipList, doDefaultExcludes, doCaseSensitive);
    }

    @SuppressWarnings("unused") // called reflectively
    protected Schema __replace_with_begin(String className) {
      RuleReplaceWith rule = new RuleReplaceWith(className);
      moduleDef.addRule(rule);
      return new FullConditionSchema(rule.getRootCondition());
    }

    /**
     * @param src a partial or full url to a script file to inject
     * @return <code>null</code> since there can be no children
     */
    @SuppressWarnings("unused") // called reflectively
    protected Schema __script_begin(String src) {
      return fChild = new ScriptSchema();
    }

    @SuppressWarnings("unused") // called reflectively
    protected void __script_end(String src) {
      ScriptSchema childSchema = (ScriptSchema) fChild;
      String js = childSchema.getScript();
      if (js != null) {
        logger.log(
            TreeLogger.WARN,
            "Injected scripts no longer use an associated JavaScript block; ignoring.",
            null);
      }
      moduleDef.getScripts().append(new Script(src));
    }

    @SuppressWarnings("unused") // called reflectively
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

    @SuppressWarnings("unused") // called reflectively
    protected Schema __set_configuration_property_begin(PropertyName name,
        String value) throws UnableToCompleteException {

      Property existingProperty = moduleDef.getProperties().find(name.token);
      if (existingProperty == null) {
        // If a property is created by "set-configuration-property" without
        // a previous "define-configuration-property", allow it for backwards
        // compatibility but don't allow multiple values.
        existingProperty = moduleDef.getProperties().createConfiguration(
            name.token, false);
        if (!propertySettings.containsKey(name.token)) {
          propertySettings.put(name.token, moduleName);
        }

        logger.log(TreeLogger.WARN, "Setting configuration property named "
            + name.token + " in " + moduleName
            + " that has not been previously defined."
            + "  This may be disallowed in the future.");
      } else if (!(existingProperty instanceof ConfigurationProperty)) {
        if (existingProperty instanceof BindingProperty) {
          logger.log(TreeLogger.ERROR, "The property " + name.token
              + " is already defined as a deferred-binding property");
        } else {
          // Future proofing if other subclasses are added.
          logger.log(TreeLogger.ERROR, "May not replace property named "
              + name.token + " of unknown type "
              + existingProperty.getClass().getName());
        }
        throw new UnableToCompleteException();
      }
      moduleDef.setConfigurationPropertyValue((ConfigurationProperty) existingProperty, value);

      // No children.
      return null;
    }

    @SuppressWarnings("unused") // called reflectively
    protected Schema __set_property_begin(BindingProperty prop,
        PropertyValue[] value) throws UnableToCompleteException {
      bindingPropertyCondition = new ConditionAll();
      return new PropertyConditionSchema(bindingPropertyCondition);
    }

    @SuppressWarnings("unused") // called reflectively
    protected void __set_property_end(BindingProperty prop,
        PropertyValue[] value) throws UnableToCompleteException {
      boolean error = false;
      String[] stringValues = new String[value.length];
      for (int i = 0, len = stringValues.length; i < len; i++) {
        if (!prop.isDefinedValue(stringValues[i] = value[i].token)) {
          logger.log(TreeLogger.ERROR, "The value " + stringValues[i]
              + " was not previously defined.");
          error = true;
        }
      }
      if (error) {
        throw new UnableToCompleteException();
      }

      // No conditions were specified, so use the property's root condition
      if (!bindingPropertyCondition.getConditions().iterator().hasNext()) {
        bindingPropertyCondition = prop.getRootCondition();
      }

      prop.setAllowedValues(bindingPropertyCondition, stringValues);
    }

    @SuppressWarnings("unused") // called reflectively
    protected Schema __set_property_fallback_begin(BindingProperty prop,
        PropertyValue value) throws UnableToCompleteException {
      boolean error = true;
      for (String possibleValue : prop.getAllowedValues(prop.getRootCondition())) {
        if (possibleValue.equals(value.token)) {
          error = false;
          break;
        }
      }
      if (error) {
        logger.log(TreeLogger.ERROR, "The fallback value '" + value.token
            + "' was not previously defined for property '" + prop.getName()
            + "'");
        throw new UnableToCompleteException();
      }
      prop.setFallback(value.token);
      return null;
    }

    /**
     * Indicates which subdirectories contain translatable source without
     * necessarily adding a sourcepath entry.
     */
    @SuppressWarnings("unused") // called reflectively
    protected Schema __source_begin(String path, String includes,
        String excludes, String skips, String defaultExcludes,
        String caseSensitive) {
      return fChild = new IncludeExcludeSchema();
    }

    @SuppressWarnings("unused") // called reflectively
    protected void __source_end(String path, String includes, String excludes,
        String skips, String defaultExcludes, String caseSensitive) {
      addSourcePackage(path, includes, excludes, skips, defaultExcludes,
          caseSensitive, false);
    }

    @SuppressWarnings("unused") // called reflectively
    protected Schema __resource_begin(String path) {
      addResourcePackage(path);
      return null;
    }

    /**
     * @param src a partial or full url to a stylesheet file to inject
     * @return <code>null</code> since there can be no children
     */
    @SuppressWarnings("unused") // called reflectively
    protected Schema __stylesheet_begin(String src) {
      moduleDef.getStyles().append(src);
      return null;
    }

    /**
     * Like adding a translatable source package, but such that it uses the
     * module's package itself as its sourcepath root entry.
     */
    @SuppressWarnings("unused") // called reflectively
    protected Schema __super_source_begin(String path, String includes,
        String excludes, String skips, String defaultExcludes,
        String caseSensitive) {
      return fChild = new IncludeExcludeSchema();
    }

    @SuppressWarnings("unused") // called reflectively
    protected void __super_source_end(String path, String includes,
        String excludes, String skips, String defaultExcludes,
        String caseSensitive) {
      addSourcePackage(path, includes, excludes, skips, defaultExcludes,
          caseSensitive, true);
    }

    private void addDelimitedStringToSet(String delimited, String delimiter,
        Set<String> toSet) {
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
        String[] includeList, String[] excludeList, String[] skipList,
        boolean defaultExcludes, boolean caseSensitive) {
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
      moduleDef.addPublicPackage(fullDir, includeList, excludeList, skipList,
          defaultExcludes, caseSensitive);
    }

    private void addResourcePackage(String relDir) {
      moduleDef.addResourcePath(modulePackageAsPath + relDir + "/");
    }

    private void addSourcePackage(String relDir, String includes,
        String excludes, String skips, String defaultExcludes,
        String caseSensitive, boolean isSuperSource) {
      IncludeExcludeSchema childSchema = ((IncludeExcludeSchema) fChild);
      foundExplicitSourceOrSuperSource = true;

      Set<String> includeSet = childSchema.getIncludes();
      addDelimitedStringToSet(includes, "[ ,]", includeSet);
      String[] includeList = includeSet.toArray(new String[includeSet.size()]);

      Set<String> excludeSet = childSchema.getExcludes();
      addDelimitedStringToSet(excludes, "[ ,]", excludeSet);
      String[] excludeList = excludeSet.toArray(new String[excludeSet.size()]);

      Set<String> skipSet = childSchema.getSkips();
      addDelimitedStringToSet(skips, "[ ,]", skipSet);
      String[] skipList = skipSet.toArray(new String[skipSet.size()]);

      boolean doDefaultExcludes = toPrimitiveBoolean(defaultExcludes);
      boolean doCaseSensitive = toPrimitiveBoolean(caseSensitive);

      addSourcePackage(modulePackageAsPath, relDir, includeList, excludeList,
          skipList, doDefaultExcludes, doCaseSensitive, isSuperSource);
    }

    private void addSourcePackage(String modulePackagePath, String relDir,
        String[] includeList, String[] excludeList, String[] skipList,
        boolean defaultExcludes, boolean caseSensitive, boolean isSuperSource) {
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

      String fullPackagePath = modulePackagePath + normChildDir;

      if (isSuperSource) {
        /*
         * Super source does not include the module package path as part of the
         * logical class names.
         */
        moduleDef.addSuperSourcePackage(fullPackagePath, includeList,
            excludeList, skipList, defaultExcludes, caseSensitive);
      } else {
        /*
         * Add the full package path to the include/exclude/skip lists since the
         * logical name of classes on the source path includes the package path
         * but the include, exclude, and skip lists do not.
         */
        addPrefix(includeList, fullPackagePath);
        addPrefix(excludeList, fullPackagePath);
        addPrefix(skipList, fullPackagePath);

        moduleDef.addSourcePackage(fullPackagePath, includeList, excludeList,
            skipList, defaultExcludes, caseSensitive);
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
  }

  /**
   * Processes attributes of java.lang.Class type.
   */
  private final class ClassAttrCvt extends AttributeConverter {
    @Override
    public Object convertToArg(Schema schema, int line, String elem,
        String attr, String value) throws UnableToCompleteException {
      if (value.length() == 0) {
        // handle optional class names
        return null;
      }
      try {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return cl.loadClass(value);
      } catch (ClassNotFoundException e) {
        Messages.UNABLE_TO_LOAD_CLASS.log(logger, value, e);
        throw new UnableToCompleteException();
      }
    }
  }

  /**
   * All conditional expressions, including those based on types.
   */
  private final class FullConditionSchema extends PropertyConditionSchema {

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __when_type_assignable_1_class = null;

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __when_type_is_1_class = null;

    public FullConditionSchema(CompoundCondition parentCondition) {
      super(parentCondition);
    }

    @SuppressWarnings("unused") // called reflectively
    protected Schema __when_type_assignable_begin(String className) {
      Condition cond = new ConditionWhenTypeAssignableTo(className);
      parentCondition.getConditions().add(cond);

      // No children allowed.
      return null;
    }

    @SuppressWarnings("unused") // called reflectively
    protected Schema __when_type_is_begin(String className) {
      Condition cond = new ConditionWhenTypeIs(className);
      parentCondition.getConditions().add(cond);

      // No children allowed.
      return null;
    }

    @Override
    protected Schema subSchema(CompoundCondition cond) {
      return new FullConditionSchema(cond);
    }
  }

  private static final class IncludeExcludeSchema extends Schema {

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __exclude_1_name = null;

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __include_1_name = null;

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __skip_1_name = null;

    /**
     * Files neither in this module, nor available from other modules.
     */
    private final Set<String> excludes = new HashSet<String>();

    /**
     * Files in this module.
     */
    private final Set<String> includes = new HashSet<String>();

    /**
     * Files not in this module, but available from other modules.
     */
    private final Set<String> skips = new HashSet<String>();

    public Set<String> getExcludes() {
      return excludes;
    }

    public Set<String> getIncludes() {
      return includes;
    }

    public Set<String> getSkips() {
      return skips;
    }

    @SuppressWarnings("unused") // called reflectively
    protected Schema __exclude_begin(String name) {
      excludes.add(name);
      return null;
    }

    @SuppressWarnings("unused") // called reflectively
    protected Schema __include_begin(String name) {
      includes.add(name);
      return null;
    }

    @SuppressWarnings("unused") // called reflectively
    protected Schema __skip_begin(String name) {
      skips.add(name);
      return null;
    }
  }

  private static class LinkerName {
    public final String name;

    public LinkerName(String name) {
      this.name = name;
    }
  }

  /**
   * Converts a string into a linker name, validating it in the process.
   */
  private final class LinkerNameAttrCvt extends AttributeConverter {

    @Override
    public Object convertToArg(Schema schema, int line, String elem,
        String attr, String value) throws UnableToCompleteException {
      // Ensure the value is a valid Java identifier
      if (!Util.isValidJavaIdent(value)) {
        Messages.LINKER_NAME_INVALID.log(logger, value, null);
        throw new UnableToCompleteException();
      }

      // It is a valid name.
      return new LinkerName(value);
    }
  }

  /**
   * A dotted Java identifier or null. Zero-length names are represented as
   * null.
   */
  private static class NullableName {
    public final String token;

    public NullableName(String token) {
      this.token = token;
    }
  }

  /**
   * Converts a string into a nullable name, validating it in the process.
   */
  private final class NullableNameAttrCvt extends AttributeConverter {

    @Override
    public Object convertToArg(Schema schema, int line, String elem,
        String attr, String value) throws UnableToCompleteException {
      if (value == null || value.length() == 0) {
        return new NullableName(null);
      }

      // Ensure each part of the name is valid.
      //
      String[] tokens = (value + ". ").split("\\.");
      for (int i = 0; i < tokens.length - 1; i++) {
        String token = tokens[i];
        if (!Util.isValidJavaIdent(token)) {
          Messages.NAME_INVALID.log(logger, value, null);
          throw new UnableToCompleteException();
        }
      }

      // It is a valid name.
      //
      return new NullableName(value);
    }
  }

  /**
   * Converts property names into their corresponding property objects.
   */
  private final class PropertyAttrCvt extends AttributeConverter {
    private Class<? extends Property> concreteType;

    public PropertyAttrCvt(Class<? extends Property> concreteType) {
      this.concreteType = concreteType;
    }

    @Override
    public Object convertToArg(Schema schema, int line, String elem,
        String attr, String value) throws UnableToCompleteException {
      // Find the named property.
      //
      Property prop = moduleDef.getProperties().find(value);

      if (prop != null) {
        // Found it.
        //
        if (concreteType.isInstance(prop)) {
          return prop;
        }
        logger.log(TreeLogger.ERROR, "The specified property '"
            + prop.getName() + "' is not of the correct type; found '"
            + prop.getClass().getSimpleName() + "' expecting '"
            + concreteType.getSimpleName() + "'");
      } else {
        // Property not defined. This is a problem.
        //
        Messages.PROPERTY_NOT_FOUND.log(logger, value, null);
      }
      throw new UnableToCompleteException();
    }
  }

  /**
   * A limited number of conditional predicates based only on properties.
   */
  private class PropertyConditionSchema extends Schema {
    @SuppressWarnings("unused") // referenced reflectively
    protected final String __when_linker_added_1_name = null;

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __when_property_is_1_name = null;

    @SuppressWarnings("unused") // referenced reflectively
    protected final String __when_property_is_2_value = null;

    protected final CompoundCondition parentCondition;

    public PropertyConditionSchema(CompoundCondition parentCondition) {
      this.parentCondition = parentCondition;
    }

    @SuppressWarnings("unused") // called reflectively
    protected Schema __all_begin() {
      CompoundCondition cond = new ConditionAll();
      parentCondition.getConditions().add(cond);
      return subSchema(cond);
    }

    @SuppressWarnings("unused") // called reflectively
    protected Schema __any_begin() {
      CompoundCondition cond = new ConditionAny();
      parentCondition.getConditions().add(cond);
      return subSchema(cond);
    }

    @SuppressWarnings("unused") // called reflectively
    protected Schema __none_begin() {
      CompoundCondition cond = new ConditionNone();
      parentCondition.getConditions().add(cond);
      return subSchema(cond);
    }

    @SuppressWarnings("unused") // called reflectively
    protected Schema __when_linker_added_begin(LinkerName linkerName) {
      Condition cond = new ConditionWhenLinkerAdded(linkerName.name);
      parentCondition.getConditions().add(cond);

      // No children allowed.
      return null;
    }

    /*
     * We intentionally use the BindingProperty type here for tough-love on
     * module writers. It prevents them from trying to create property providers
     * for unknown properties.
     */
    @SuppressWarnings("unused") // called reflectively
    protected Schema __when_property_is_begin(BindingProperty prop,
        PropertyValue value) {
      Condition cond = new ConditionWhenPropertyIs(prop.getName(), value.token);
      parentCondition.getConditions().add(cond);

      // No children allowed.
      return null;
    }

    protected Schema subSchema(CompoundCondition cond) {
      return new PropertyConditionSchema(cond);
    }
  }

  private static class PropertyName {
    public final String token;

    public PropertyName(String token) {
      this.token = token;
    }
  }

  /**
   * Converts a string into a property name, validating it in the process.
   */
  private final class PropertyNameAttrCvt extends AttributeConverter {

    @Override
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

  private static class PropertyValue {
    public final String token;

    public PropertyValue(String token) {
      this.token = token;
    }
  }

  private static class PropertyFallbackValue {
    public final String token;

    public PropertyFallbackValue(String token) {
      this.token = token;
    }
  }

  /**
   * Converts a comma-separated string into an array of property value tokens.
   */
  private final class PropertyValueArrayAttrCvt extends AttributeConverter {
    @Override
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
    @Override
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

  private static class PropertyValueGlob {
    public final String token;

    public PropertyValueGlob(String token) {
      this.token = token;
    }

    public boolean isGlob() {
      return token.contains(BindingProperty.GLOB_STAR);
    }
  }

  /**
   * Converts a string into a property fallback value, validating it in the process.
   */
  private final class PropertyFallbackValueAttrCvt extends AttributeConverter {
    @Override
    public Object convertToArg(Schema schema, int line, String elem,
        String attr, String value) throws UnableToCompleteException {
      String token = value.trim();
      if (Util.isValidJavaIdent(token)) {
        return new PropertyFallbackValue(token);
      } else {
        Messages.PROPERTY_VALUE_INVALID.log(logger, token, null);
        throw new UnableToCompleteException();
      }
    }
  }

  /**
   * Converts a comma-separated string into an array of property value tokens.
   */
  private final class PropertyValueGlobArrayAttrCvt extends AttributeConverter {
    @Override
    public Object convertToArg(Schema schema, int line, String elem,
        String attr, String value) throws UnableToCompleteException {
      String[] tokens = value.split(",");
      PropertyValueGlob[] values = new PropertyValueGlob[tokens.length];

      // Validate each token as we copy it over.
      //
      for (int i = 0; i < tokens.length; i++) {
        values[i] = (PropertyValueGlob) propValueGlobAttrCvt.convertToArg(
            schema, line, elem, attr, tokens[i]);
      }

      return values;
    }
  }

  /**
   * Converts a string into a property value glob, validating it in the process.
   */
  private final class PropertyValueGlobAttrCvt extends AttributeConverter {
    @Override
    public Object convertToArg(Schema schema, int line, String elem,
        String attr, String value) throws UnableToCompleteException {

      String token = value.trim();
      String tokenNoStar = token.replaceAll(
          Pattern.quote(BindingProperty.GLOB_STAR), "");
      if (BindingProperty.GLOB_STAR.equals(token)
          || Util.isValidJavaIdent(tokenNoStar)) {
        return new PropertyValueGlob(token);
      } else {
        Messages.PROPERTY_VALUE_INVALID.log(logger, token, null);
        throw new UnableToCompleteException();
      }
    }
  }

  private static class ScriptSchema extends Schema {

    private StringBuffer script;

    private int startLineNumber = -1;

    public ScriptSchema() {
    }

    @SuppressWarnings("unused") // called reflectively
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
  }

  /**
   * Map of property names to the modules that defined them explicitly using
   * <define-configuration-property>, used to generate warning messages.
   */
  private static final HashMap<String, String> propertyDefinitions = new HashMap<String, String>();

  /**
   * Map of property names to the modules that defined them implicitly using
   * <set-configuration-property>, used to generate warning messages.
   */
  private static final HashMap<String, String> propertySettings = new HashMap<String, String>();

  private static void addPrefix(String[] strings, String prefix) {
    for (int i = 0; i < strings.length; ++i) {
      strings[i] = prefix + strings[i];
    }
  }

  /**
   * Returns <code>true</code> if the string equals "true" or "yes" using a case
   * insensitive comparison.
   */
  private static boolean toPrimitiveBoolean(String s) {
    return "yes".equalsIgnoreCase(s) || "true".equalsIgnoreCase(s);
  }

  protected final String __module_1_rename_to = "";
  protected final String __module_2_type = "library";
  private final PropertyAttrCvt bindingPropAttrCvt = new PropertyAttrCvt(
      BindingProperty.class);
  private final BodySchema bodySchema;
  private final ClassAttrCvt classAttrCvt = new ClassAttrCvt();
  private final PropertyAttrCvt configurationPropAttrCvt = new PropertyAttrCvt(
      ConfigurationProperty.class);
  private boolean foundAnyPublic;
  private boolean foundExplicitSourceOrSuperSource;
  private final JsProgram jsPgm = new JsProgram();
  private final LinkerNameAttrCvt linkerNameAttrCvt = new LinkerNameAttrCvt();
  private final ModuleDefLoader loader;
  private final TreeLogger logger;
  private final ModuleDef moduleDef;
  private final String moduleName;
  private final String modulePackageAsPath;
  private final URL moduleURL;
  private final NullableNameAttrCvt nullableNameAttrCvt = new NullableNameAttrCvt();
  private final PropertyNameAttrCvt propNameAttrCvt = new PropertyNameAttrCvt();
  private final PropertyValueArrayAttrCvt propValueArrayAttrCvt = new PropertyValueArrayAttrCvt();
  private final PropertyValueAttrCvt propValueAttrCvt = new PropertyValueAttrCvt();
  private final PropertyFallbackValueAttrCvt propFallbackValueAttrCvt = new PropertyFallbackValueAttrCvt();
  private final PropertyValueGlobArrayAttrCvt propValueGlobArrayAttrCvt = new PropertyValueGlobArrayAttrCvt();
  private final PropertyValueGlobAttrCvt propValueGlobAttrCvt = new PropertyValueGlobAttrCvt();

  public ModuleDefSchema(TreeLogger logger, ModuleDefLoader loader,
      String moduleName, URL moduleURL, String modulePackageAsPath,
      ModuleDef toConfigure) {
    this.logger = logger;
    this.loader = loader;
    this.moduleName = moduleName;
    this.moduleURL = moduleURL;
    this.modulePackageAsPath = modulePackageAsPath;
    assert (modulePackageAsPath.endsWith("/") || modulePackageAsPath.equals(""));
    this.moduleDef = toConfigure;
    this.bodySchema = new BodySchema();

    registerAttributeConverter(PropertyName.class, propNameAttrCvt);
    registerAttributeConverter(BindingProperty.class, bindingPropAttrCvt);
    registerAttributeConverter(ConfigurationProperty.class,
        configurationPropAttrCvt);
    registerAttributeConverter(PropertyValue.class, propValueAttrCvt);
    registerAttributeConverter(PropertyFallbackValue.class, propFallbackValueAttrCvt);
    registerAttributeConverter(PropertyValue[].class, propValueArrayAttrCvt);
    registerAttributeConverter(PropertyValueGlob.class, propValueGlobAttrCvt);
    registerAttributeConverter(PropertyValueGlob[].class,
        propValueGlobArrayAttrCvt);
    registerAttributeConverter(LinkerName.class, linkerNameAttrCvt);
    registerAttributeConverter(NullableName.class, nullableNameAttrCvt);
    registerAttributeConverter(Class.class, classAttrCvt);
  }

  @SuppressWarnings("unused")
  protected Schema __module_begin(NullableName renameTo, String type) {
    ModuleType moduleType = ModuleType.valueOf(type.toUpperCase(Locale.ENGLISH));
    moduleDef.enterModule(moduleType, moduleName);
    return bodySchema;
  }

  protected void __module_end(NullableName renameTo, String type) {
    if (!loader.enforceStrictResources()) {
      // If we're not being strict about resources and no dependencies have been added, go ahead and
      // implicitly add "client" and "public" resource dependencies.
      if (!foundExplicitSourceOrSuperSource) {
        bodySchema.addSourcePackage(modulePackageAsPath, "client", Empty.STRINGS,
            Empty.STRINGS, Empty.STRINGS, true, true, false);
      }

      if (!foundAnyPublic) {
        bodySchema.addPublicPackage(modulePackageAsPath, "public", Empty.STRINGS,
            Empty.STRINGS, Empty.STRINGS, true, true);
      }
    }

    // We do this in __module_end so this value is never inherited
    moduleDef.setNameOverride(renameTo.token);
    moduleDef.exitModule();
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
    List<JsStatement> stmts;
    try {
      // TODO Provide more context here
      stmts = JsParser.parse(jsPgm.createSourceInfo(startLineNumber,
          moduleURL.toExternalForm()), jsPgm.getScope(), r);
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Error reading script source", e);
      throw new UnableToCompleteException();
    } catch (JsParserException e) {
      logger.log(TreeLogger.ERROR, "Error parsing JavaScript source", e);
      throw new UnableToCompleteException();
    }

    // Rip the body out of the parsed function and attach the JavaScript
    // AST to the method.
    //
    JsFunction fn = (JsFunction) ((JsExprStmt) stmts.get(0)).getExpression();
    return fn;
  }
}
// CHECKSTYLE_NAMING_ON
