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
package com.google.gwt.resources.rg;

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.ConfigurationProperty;
import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.SelectionProperty;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.Util;
import com.google.gwt.dom.client.Element;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.DataResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.CssResource.ClassName;
import com.google.gwt.resources.client.CssResource.Import;
import com.google.gwt.resources.client.CssResource.ImportedWithPrefix;
import com.google.gwt.resources.client.CssResource.NotStrict;
import com.google.gwt.resources.client.CssResource.Shared;
import com.google.gwt.resources.client.CssResource.Strict;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.resources.css.CssGenerationVisitor;
import com.google.gwt.resources.css.GenerateCssAst;
import com.google.gwt.resources.css.ast.CollapsedNode;
import com.google.gwt.resources.css.ast.Context;
import com.google.gwt.resources.css.ast.CssCompilerException;
import com.google.gwt.resources.css.ast.CssDef;
import com.google.gwt.resources.css.ast.CssEval;
import com.google.gwt.resources.css.ast.CssExternalSelectors;
import com.google.gwt.resources.css.ast.CssIf;
import com.google.gwt.resources.css.ast.CssMediaRule;
import com.google.gwt.resources.css.ast.CssModVisitor;
import com.google.gwt.resources.css.ast.CssNoFlip;
import com.google.gwt.resources.css.ast.CssNode;
import com.google.gwt.resources.css.ast.CssNodeCloner;
import com.google.gwt.resources.css.ast.CssProperty;
import com.google.gwt.resources.css.ast.CssRule;
import com.google.gwt.resources.css.ast.CssSelector;
import com.google.gwt.resources.css.ast.CssSprite;
import com.google.gwt.resources.css.ast.CssStylesheet;
import com.google.gwt.resources.css.ast.CssUrl;
import com.google.gwt.resources.css.ast.CssVisitor;
import com.google.gwt.resources.css.ast.HasNodes;
import com.google.gwt.resources.css.ast.CssProperty.DotPathValue;
import com.google.gwt.resources.css.ast.CssProperty.ExpressionValue;
import com.google.gwt.resources.css.ast.CssProperty.IdentValue;
import com.google.gwt.resources.css.ast.CssProperty.ListValue;
import com.google.gwt.resources.css.ast.CssProperty.NumberValue;
import com.google.gwt.resources.css.ast.CssProperty.Value;
import com.google.gwt.resources.ext.AbstractResourceGenerator;
import com.google.gwt.resources.ext.ClientBundleRequirements;
import com.google.gwt.resources.ext.ResourceContext;
import com.google.gwt.resources.ext.ResourceGeneratorUtil;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.gwt.user.rebind.StringSourceWriter;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Adler32;

/**
 * Provides implementations of CSSResources.
 */
public final class CssResourceGenerator extends AbstractResourceGenerator {
  static class ClassRenamer extends CssVisitor {
    private final Map<JMethod, String> actualReplacements = new IdentityHashMap<JMethod, String>();

    /**
     * This is a map of local prefixes to the obfuscated names of imported
     * methods. If a CssResource makes use of the {@link Import} annotation, the
     * keys of this map will correspond to the {@link ImportedWithPrefix} value
     * defined on the imported CssResource. The zero-length string key holds the
     * obfuscated names for the CssResource that is being generated.
     */
    private final Map<String, Map<JMethod, String>> classReplacementsWithPrefix;
    private final Set<String> cssDefs = new HashSet<String>();
    private final Set<String> externalClasses;
    private final TreeLogger logger;
    private final Set<JMethod> missingClasses;
    private final Set<String> replacedClasses = new HashSet<String>();
    private final boolean strict;
    private final Set<String> unknownClasses = new HashSet<String>();

    public ClassRenamer(TreeLogger logger,
        Map<String, Map<JMethod, String>> classReplacementsWithPrefix,
        boolean strict, Set<String> externalClasses) {
      this.logger = logger.branch(TreeLogger.DEBUG, "Replacing CSS class names");
      this.classReplacementsWithPrefix = classReplacementsWithPrefix;
      this.strict = strict;
      this.externalClasses = externalClasses;

      // Require a definition for all classes in the default namespace
      assert classReplacementsWithPrefix.containsKey("");
      missingClasses = new HashSet<JMethod>(
          classReplacementsWithPrefix.get("").keySet());
    }

    @Override
    public void endVisit(CssDef x, Context ctx) {
      cssDefs.add(x.getKey());
    }

    @Override
    public void endVisit(CssSelector x, Context ctx) {
      String sel = x.getSelector();

      // TODO This would be simplified by having a class hierarchy for selectors
      for (Map.Entry<String, Map<JMethod, String>> outerEntry : classReplacementsWithPrefix.entrySet()) {
        String prefix = outerEntry.getKey();
        for (Map.Entry<JMethod, String> entry : outerEntry.getValue().entrySet()) {
          JMethod method = entry.getKey();
          String sourceClassName = method.getName();
          String obfuscatedClassName = entry.getValue();

          ClassName className = method.getAnnotation(ClassName.class);
          if (className != null) {
            sourceClassName = className.value();
          }

          sourceClassName = prefix + sourceClassName;

          Pattern p = Pattern.compile("(.*)\\.("
              + Pattern.quote(sourceClassName) + ")([ :>+#.].*|$)");
          Matcher m = p.matcher(sel);
          if (m.find()) {
            if (externalClasses.contains(sourceClassName)) {
              actualReplacements.put(method, sourceClassName);
            } else {
              sel = m.group(1) + "." + obfuscatedClassName + m.group(3);
              actualReplacements.put(method, obfuscatedClassName);
            }

            missingClasses.remove(method);
            if (strict) {
              replacedClasses.add(obfuscatedClassName);
            }
          }
        }
      }

      sel = sel.trim();

      if (strict) {
        Matcher m = CssSelector.CLASS_SELECTOR_PATTERN.matcher(sel);
        while (m.find()) {
          String classSelector = m.group(1);
          if (!replacedClasses.contains(classSelector)
              && !externalClasses.contains(classSelector)) {
            unknownClasses.add(classSelector);
          }
        }
      }

      x.setSelector(sel);
    }

    @Override
    public void endVisit(CssStylesheet x, Context ctx) {
      boolean stop = false;

      // Skip names corresponding to @def entries. They too can be declared as
      // String accessors.
      List<JMethod> toRemove = new ArrayList<JMethod>();
      for (JMethod method : missingClasses) {
        if (cssDefs.contains(method.getName())) {
          toRemove.add(method);
        }
      }
      for (JMethod method : toRemove) {
        missingClasses.remove(method);
      }

      if (!missingClasses.isEmpty()) {
        stop = true;
        TreeLogger errorLogger = logger.branch(TreeLogger.INFO,
            "The following obfuscated style classes were missing from "
                + "the source CSS file:");
        for (JMethod m : missingClasses) {
          String name = m.getName();
          ClassName className = m.getAnnotation(ClassName.class);
          if (className != null) {
            name = className.value();
          }
          errorLogger.log(TreeLogger.ERROR, name + ": Fix by adding ." + name
              + "{}");
        }
      }

      if (strict && !unknownClasses.isEmpty()) {
        stop = true;
        TreeLogger errorLogger = logger.branch(TreeLogger.ERROR,
            "The following unobfuscated classes were present in a strict CssResource:");
        for (String s : unknownClasses) {
          errorLogger.log(TreeLogger.ERROR, s);
        }
        errorLogger.log(TreeLogger.INFO, "Fix by adding String accessor "
            + "method(s) to the CssResource interface for obfuscated classes, "
            + "or using an @external declaration for unobfuscated classes.");
      }

      if (stop) {
        throw new CssCompilerException("Missing a CSS replacement");
      }
    }

    public Map<JMethod, String> getReplacements() {
      return actualReplacements;
    }
  }

  static class DefsCollector extends CssVisitor {
    private final Set<String> defs = new HashSet<String>();

    @Override
    public void endVisit(CssDef x, Context ctx) {
      defs.add(x.getKey());
    }
  }

  /**
   * Collects all {@code @external} declarations in the stylesheet.
   */
  static class ExternalClassesCollector extends CssVisitor {
    private final Set<String> classes = new HashSet<String>();

    @Override
    public void endVisit(CssExternalSelectors x, Context ctx) {
      classes.addAll(x.getClasses());
    }

    public Set<String> getClasses() {
      return classes;
    }
  }

  /**
   * Statically evaluates {@literal @if} rules.
   */
  static class IfEvaluator extends CssModVisitor {
    private final TreeLogger logger;
    private final PropertyOracle oracle;

    public IfEvaluator(TreeLogger logger, PropertyOracle oracle) {
      this.logger = logger.branch(TreeLogger.DEBUG,
          "Replacing property-based @if blocks");
      this.oracle = oracle;
    }

    @Override
    public void endVisit(CssIf x, Context ctx) {
      if (x.getExpression() != null) {
        // This gets taken care of by the runtime substitution visitor
      } else {
        try {
          String propertyName = x.getPropertyName();
          String propValue = null;
          try {
            SelectionProperty selProp = oracle.getSelectionProperty(logger,
                propertyName);
            propValue = selProp.getCurrentValue();
          } catch (BadPropertyValueException e) {
            ConfigurationProperty confProp = oracle.getConfigurationProperty(propertyName);
            propValue = confProp.getValues().get(0);
          }

          /*
           * If the deferred binding property's value is in the list of values
           * in the @if rule, move the rules into the @if's context.
           */
          if (Arrays.asList(x.getPropertyValues()).contains(propValue)
              ^ x.isNegated()) {
            for (CssNode n : x.getNodes()) {
              ctx.insertBefore(n);
            }
          } else {
            // Otherwise, move the else block into the if statement's position
            for (CssNode n : x.getElseNodes()) {
              ctx.insertBefore(n);
            }
          }

          // Always delete @if rules that we can statically evaluate
          ctx.removeMe();
        } catch (BadPropertyValueException e) {
          logger.log(TreeLogger.ERROR, "Unable to evaluate @if block", e);
          throw new CssCompilerException("Unable to parse CSS", e);
        }
      }
    }
  }

  @SuppressWarnings("serial")
  static class JClassOrderComparator implements Comparator<JClassType>,
      Serializable {
    public int compare(JClassType o1, JClassType o2) {
      return o1.getQualifiedSourceName().compareTo(o2.getQualifiedSourceName());
    }
  }

  /**
   * Merges rules that have matching selectors.
   */
  static class MergeIdenticalSelectorsVisitor extends CssModVisitor {
    private final Map<String, CssRule> canonicalRules = new HashMap<String, CssRule>();
    private final List<CssRule> rulesInOrder = new ArrayList<CssRule>();

    @Override
    public boolean visit(CssIf x, Context ctx) {
      visitInNewContext(x.getNodes());
      visitInNewContext(x.getElseNodes());
      return false;
    }

    @Override
    public boolean visit(CssMediaRule x, Context ctx) {
      visitInNewContext(x.getNodes());
      return false;
    }

    @Override
    public boolean visit(CssRule x, Context ctx) {
      // Assumed to run immediately after SplitRulesVisitor
      assert x.getSelectors().size() == 1;
      CssSelector sel = x.getSelectors().get(0);

      if (canonicalRules.containsKey(sel.getSelector())) {
        CssRule canonical = canonicalRules.get(sel.getSelector());

        // Check everything between the canonical rule and this rule for common
        // properties. If there are common properties, it would be unsafe to
        // promote the rule.
        boolean hasCommon = false;
        int index = rulesInOrder.indexOf(canonical) + 1;
        assert index != 0;

        for (Iterator<CssRule> i = rulesInOrder.listIterator(index); i.hasNext()
            && !hasCommon;) {
          hasCommon = haveCommonProperties(i.next(), x);
        }

        if (!hasCommon) {
          // It's safe to promote the rule
          canonical.getProperties().addAll(x.getProperties());
          ctx.removeMe();
          return false;
        }
      }

      canonicalRules.put(sel.getSelector(), x);
      rulesInOrder.add(x);
      return false;
    }

    private void visitInNewContext(List<CssNode> nodes) {
      MergeIdenticalSelectorsVisitor v = new MergeIdenticalSelectorsVisitor();
      v.acceptWithInsertRemove(nodes);
      rulesInOrder.addAll(v.rulesInOrder);
    }
  }

  /**
   * Merges rules that have identical content.
   */
  static class MergeRulesByContentVisitor extends CssModVisitor {
    private Map<String, CssRule> rulesByContents = new HashMap<String, CssRule>();
    private final List<CssRule> rulesInOrder = new ArrayList<CssRule>();

    @Override
    public boolean visit(CssIf x, Context ctx) {
      visitInNewContext(x.getNodes());
      visitInNewContext(x.getElseNodes());
      return false;
    }

    @Override
    public boolean visit(CssMediaRule x, Context ctx) {
      visitInNewContext(x.getNodes());
      return false;
    }

    @Override
    public boolean visit(CssRule x, Context ctx) {
      StringBuilder b = new StringBuilder();
      for (CssProperty p : x.getProperties()) {
        b.append(p.getName()).append(":").append(p.getValues().getExpression());
      }

      String content = b.toString();
      CssRule canonical = rulesByContents.get(content);

      // Check everything between the canonical rule and this rule for common
      // properties. If there are common properties, it would be unsafe to
      // promote the rule.
      if (canonical != null) {
        boolean hasCommon = false;
        int index = rulesInOrder.indexOf(canonical) + 1;
        assert index != 0;

        for (Iterator<CssRule> i = rulesInOrder.listIterator(index); i.hasNext()
            && !hasCommon;) {
          hasCommon = haveCommonProperties(i.next(), x);
        }

        if (!hasCommon) {
          canonical.getSelectors().addAll(x.getSelectors());
          ctx.removeMe();
          return false;
        }
      }

      rulesByContents.put(content, x);
      rulesInOrder.add(x);
      return false;
    }

    private void visitInNewContext(List<CssNode> nodes) {
      MergeRulesByContentVisitor v = new MergeRulesByContentVisitor();
      v.acceptWithInsertRemove(nodes);
      rulesInOrder.addAll(v.rulesInOrder);
    }
  }

  static class RequirementsCollector extends CssVisitor {
    private final TreeLogger logger;
    private final ClientBundleRequirements requirements;

    public RequirementsCollector(TreeLogger logger,
        ClientBundleRequirements requirements) {
      this.logger = logger.branch(TreeLogger.DEBUG,
          "Scanning CSS for requirements");
      this.requirements = requirements;
    }

    @Override
    public void endVisit(CssIf x, Context ctx) {
      String propertyName = x.getPropertyName();
      if (propertyName != null) {
        try {
          requirements.addPermutationAxis(propertyName);
        } catch (BadPropertyValueException e) {
          logger.log(TreeLogger.ERROR, "Unknown deferred-binding property "
              + propertyName, e);
          throw new CssCompilerException("Unknown deferred-binding property", e);
        }
      }
    }
  }

  static class RtlVisitor extends CssModVisitor {
    /**
     * Records if we're currently visiting a CssRule whose only selector is
     * "body".
     */
    private boolean inBodyRule;

    @Override
    public void endVisit(CssProperty x, Context ctx) {
      String name = x.getName();

      if (name.equalsIgnoreCase("left")) {
        x.setName("right");
      } else if (name.equalsIgnoreCase("right")) {
        x.setName("left");
      } else if (name.endsWith("-left")) {
        int len = name.length();
        x.setName(name.substring(0, len - 4) + "right");
      } else if (name.endsWith("-right")) {
        int len = name.length();
        x.setName(name.substring(0, len - 5) + "left");
      } else if (name.contains("-right-")) {
        x.setName(name.replace("-right-", "-left-"));
      } else if (name.contains("-left-")) {
        x.setName(name.replace("-left-", "-right-"));
      } else {
        List<Value> values = new ArrayList<Value>(x.getValues().getValues());
        invokePropertyHandler(x.getName(), values);
        x.setValue(new CssProperty.ListValue(values));
      }
    }

    @Override
    public boolean visit(CssNoFlip x, Context ctx) {
      return false;
    }

    @Override
    public boolean visit(CssRule x, Context ctx) {
      inBodyRule = x.getSelectors().size() == 1
          && x.getSelectors().get(0).getSelector().equals("body");
      return true;
    }

    void propertyHandlerBackground(List<Value> values) {
      /*
       * The first numeric value will be treated as the left position only if we
       * havn't seen any value that could potentially be the left value.
       */
      boolean seenLeft = false;

      for (ListIterator<Value> it = values.listIterator(); it.hasNext();) {
        Value v = it.next();
        Value maybeFlipped = flipLeftRightIdentValue(v);
        NumberValue nv = v.isNumberValue();
        if (v != maybeFlipped) {
          it.set(maybeFlipped);
          seenLeft = true;

        } else if (isIdent(v, "center")) {
          seenLeft = true;

        } else if (!seenLeft && (nv != null)) {
          seenLeft = true;
          if ("%".equals(nv.getUnits())) {
            float position = 100f - nv.getValue();
            it.set(new NumberValue(position, "%"));
            break;
          }
        }
      }
    }

    void propertyHandlerBackgroundPosition(List<Value> values) {
      propertyHandlerBackground(values);
    }

    Value propertyHandlerBackgroundPositionX(Value v) {
      ArrayList<Value> list = new ArrayList<Value>(1);
      list.add(v);
      propertyHandlerBackground(list);
      return list.get(0);
    }

    /**
     * Note there should be no propertyHandlerBorder(). The CSS spec states that
     * the border property must set all values at once.
     */
    void propertyHandlerBorderColor(List<Value> values) {
      swapFour(values);
    }

    void propertyHandlerBorderStyle(List<Value> values) {
      swapFour(values);
    }

    void propertyHandlerBorderWidth(List<Value> values) {
      swapFour(values);
    }

    Value propertyHandlerClear(Value v) {
      return propertyHandlerFloat(v);
    }

    Value propertyHandlerCursor(Value v) {
      IdentValue identValue = v.isIdentValue();
      if (identValue == null) {
        return v;
      }

      String ident = identValue.getIdent().toLowerCase();
      if (!ident.endsWith("-resize")) {
        return v;
      }

      StringBuffer newIdent = new StringBuffer();

      if (ident.length() == 9) {
        if (ident.charAt(0) == 'n') {
          newIdent.append('n');
          ident = ident.substring(1);
        } else if (ident.charAt(0) == 's') {
          newIdent.append('s');
          ident = ident.substring(1);
        } else {
          return v;
        }
      }

      if (ident.length() == 8) {
        if (ident.charAt(0) == 'e') {
          newIdent.append("w-resize");
        } else if (ident.charAt(0) == 'w') {
          newIdent.append("e-resize");
        } else {
          return v;
        }
        return new IdentValue(newIdent.toString());
      } else {
        return v;
      }
    }

    Value propertyHandlerDirection(Value v) {
      if (inBodyRule) {
        if (isIdent(v, "ltr")) {
          return new IdentValue("rtl");
        } else if (isIdent(v, "rtl")) {
          return new IdentValue("ltr");
        }
      }
      return v;
    }

    Value propertyHandlerFloat(Value v) {
      return flipLeftRightIdentValue(v);
    }

    void propertyHandlerMargin(List<Value> values) {
      swapFour(values);
    }

    void propertyHandlerPadding(List<Value> values) {
      swapFour(values);
    }

    Value propertyHandlerPageBreakAfter(Value v) {
      return flipLeftRightIdentValue(v);
    }

    Value propertyHandlerPageBreakBefore(Value v) {
      return flipLeftRightIdentValue(v);
    }

    Value propertyHandlerTextAlign(Value v) {
      return flipLeftRightIdentValue(v);
    }

    private Value flipLeftRightIdentValue(Value v) {
      if (isIdent(v, "right")) {
        return new IdentValue("left");

      } else if (isIdent(v, "left")) {
        return new IdentValue("right");
      }
      return v;
    }

    /**
     * Reflectively invokes a propertyHandler method for the named property.
     * Dashed names are transformed into camel-case names; only letters
     * following a dash will be capitalized when looking for a method to prevent
     * <code>fooBar<code> and <code>foo-bar</code> from colliding.
     */
    private void invokePropertyHandler(String name, List<Value> values) {
      // See if we have a property-handler function
      try {
        String[] parts = name.toLowerCase().split("-");
        StringBuffer methodName = new StringBuffer("propertyHandler");
        for (String part : parts) {
          methodName.append(Character.toUpperCase(part.charAt(0)));
          methodName.append(part, 1, part.length());
        }

        try {
          // Single-arg for simplicity
          Method m = getClass().getDeclaredMethod(methodName.toString(),
              Value.class);
          assert Value.class.isAssignableFrom(m.getReturnType());
          Value newValue = (Value) m.invoke(this, values.get(0));
          values.set(0, newValue);
        } catch (NoSuchMethodException e) {
          // OK
        }

        try {
          // Or the whole List for completeness
          Method m = getClass().getDeclaredMethod(methodName.toString(),
              List.class);
          m.invoke(this, values);
        } catch (NoSuchMethodException e) {
          // OK
        }

      } catch (SecurityException e) {
        throw new CssCompilerException(
            "Unable to invoke property handler function for " + name, e);
      } catch (IllegalArgumentException e) {
        throw new CssCompilerException(
            "Unable to invoke property handler function for " + name, e);
      } catch (IllegalAccessException e) {
        throw new CssCompilerException(
            "Unable to invoke property handler function for " + name, e);
      } catch (InvocationTargetException e) {
        throw new CssCompilerException(
            "Unable to invoke property handler function for " + name, e);
      }
    }

    private boolean isIdent(Value value, String query) {
      IdentValue v = value.isIdentValue();
      return v != null && v.getIdent().equalsIgnoreCase(query);
    }

    /**
     * Swaps the second and fourth values in a list of four values.
     */
    private void swapFour(List<Value> values) {
      if (values.size() == 4) {
        Collections.swap(values, 1, 3);
      }
    }
  }

  /**
   * Splits rules with compound selectors into multiple rules.
   */
  static class SplitRulesVisitor extends CssModVisitor {
    @Override
    public void endVisit(CssRule x, Context ctx) {
      if (x.getSelectors().size() == 1) {
        return;
      }

      for (CssSelector sel : x.getSelectors()) {
        CssRule newRule = new CssRule();
        newRule.getSelectors().add(sel);
        newRule.getProperties().addAll(
            CssNodeCloner.clone(CssProperty.class, x.getProperties()));
        ctx.insertBefore(newRule);
      }
      ctx.removeMe();
      return;
    }
  }

  /**
   * Replaces CssSprite nodes with CssRule nodes that will display the sprited
   * image. The real trick with spriting the images is to reuse the
   * ImageResource processing framework by requiring the sprite to be defined in
   * terms of an ImageResource.
   */
  static class Spriter extends CssModVisitor {
    private final ResourceContext context;
    private final TreeLogger logger;

    public Spriter(TreeLogger logger, ResourceContext context) {
      this.logger = logger.branch(TreeLogger.DEBUG,
          "Creating image sprite classes");
      this.context = context;
    }

    @Override
    public void endVisit(CssSprite x, Context ctx) {
      JClassType bundleType = context.getClientBundleType();
      String functionName = x.getResourceFunction();

      if (functionName == null) {
        logger.log(TreeLogger.ERROR, "The @sprite rule " + x.getSelectors()
            + " must specify the " + CssSprite.IMAGE_PROPERTY_NAME
            + " property");
        throw new CssCompilerException("No image property specified");
      }

      // Find the image accessor method
      JMethod imageMethod = null;
      JMethod[] allMethods = bundleType.getOverridableMethods();
      for (int i = 0; imageMethod == null && i < allMethods.length; i++) {
        JMethod candidate = allMethods[i];
        // If the function name matches and takes no parameters
        if (candidate.getName().equals(functionName)
            && candidate.getParameters().length == 0) {
          // We have a match
          imageMethod = candidate;
        }
      }

      // Method unable to be located
      if (imageMethod == null) {
        logger.log(TreeLogger.ERROR, "Unable to find ImageResource method "
            + functionName + " in " + bundleType.getQualifiedSourceName());
        throw new CssCompilerException("Cannot find image function");
      }

      JClassType imageResourceType = context.getGeneratorContext().getTypeOracle().findType(
          ImageResource.class.getName());
      assert imageResourceType != null;

      if (!imageResourceType.isAssignableFrom(imageMethod.getReturnType().isClassOrInterface())) {
        logger.log(TreeLogger.ERROR, "The return type of " + functionName
            + " is not assignable to "
            + imageResourceType.getSimpleSourceName());
        throw new CssCompilerException("Incorrect return type for "
            + CssSprite.IMAGE_PROPERTY_NAME + " method");
      }

      ImageOptions options = imageMethod.getAnnotation(ImageOptions.class);
      RepeatStyle repeatStyle;
      if (options != null) {
        repeatStyle = options.repeatStyle();
      } else {
        repeatStyle = RepeatStyle.None;
      }

      String instance = "(" + context.getImplementationSimpleSourceName()
          + ".this." + functionName + "())";

      CssRule replacement = new CssRule();
      replacement.getSelectors().addAll(x.getSelectors());
      List<CssProperty> properties = replacement.getProperties();

      if (repeatStyle == RepeatStyle.None
          || repeatStyle == RepeatStyle.Horizontal) {
        properties.add(new CssProperty("height", new ExpressionValue(instance
            + ".getHeight() + \"px\""), false));
      }

      if (repeatStyle == RepeatStyle.None
          || repeatStyle == RepeatStyle.Vertical) {
        properties.add(new CssProperty("width", new ExpressionValue(instance
            + ".getWidth() + \"px\""), false));
      }
      properties.add(new CssProperty("overflow", new IdentValue("hidden"),
          false));

      String repeatText;
      switch (repeatStyle) {
        case None:
          repeatText = " no-repeat";
          break;
        case Horizontal:
          repeatText = " repeat-x";
          break;
        case Vertical:
          repeatText = " repeat-y";
          break;
        case Both:
          repeatText = " repeat";
          break;
        default:
          throw new RuntimeException("Unknown repeatStyle " + repeatStyle);
      }

      String backgroundExpression = "\"url(\\\"\" + " + instance
          + ".getURL() + \"\\\") -\" + " + instance
          + ".getLeft() + \"px -\" + " + instance + ".getTop() + \"px "
          + repeatText + "\"";
      properties.add(new CssProperty("background", new ExpressionValue(
          backgroundExpression), false));

      // Retain any user-specified properties
      properties.addAll(x.getProperties());

      ctx.replaceMe(replacement);
    }
  }

  static class SubstitutionCollector extends CssVisitor {
    private final Map<String, CssDef> substitutions = new HashMap<String, CssDef>();

    @Override
    public void endVisit(CssDef x, Context ctx) {
      substitutions.put(x.getKey(), x);
    }

    @Override
    public void endVisit(CssEval x, Context ctx) {
      substitutions.put(x.getKey(), x);
    }

    @Override
    public void endVisit(CssUrl x, Context ctx) {
      substitutions.put(x.getKey(), x);
    }
  }

  /**
   * Substitute symbolic replacements into string values.
   */
  static class SubstitutionReplacer extends CssVisitor {
    private final ResourceContext context;
    private final TreeLogger logger;
    private final Map<String, CssDef> substitutions;

    public SubstitutionReplacer(TreeLogger logger, ResourceContext context,
        Map<String, CssDef> substitutions) {
      this.context = context;
      this.logger = logger;
      this.substitutions = substitutions;
    }

    @Override
    public void endVisit(CssProperty x, Context ctx) {
      if (x.getValues() == null) {
        // Nothing to do
        return;
      }

      List<Value> values = new ArrayList<Value>(x.getValues().getValues());

      for (ListIterator<Value> i = values.listIterator(); i.hasNext();) {
        IdentValue v = i.next().isIdentValue();

        if (v == null) {
          // Don't try to substitute into anything other than idents
          continue;
        }

        String value = v.getIdent();
        CssDef def = substitutions.get(value);

        if (def == null) {
          continue;
        } else if (def instanceof CssUrl) {
          assert def.getValues().size() == 1;
          assert def.getValues().get(0).isIdentValue() != null;
          String functionName = def.getValues().get(0).isIdentValue().getIdent();

          // Find the method
          JMethod methods[] = context.getClientBundleType().getOverridableMethods();
          boolean foundMethod = false;
          if (methods != null) {
            for (JMethod method : methods) {
              if (method.getName().equals(functionName)) {
                foundMethod = true;
                break;
              }
            }
          }

          if (!foundMethod) {
            logger.log(TreeLogger.ERROR, "Unable to find DataResource method "
                + functionName + " in "
                + context.getClientBundleType().getQualifiedSourceName());
            throw new CssCompilerException("Cannot find data function");
          }

          String instance = "((" + DataResource.class.getName() + ")("
              + context.getImplementationSimpleSourceName() + ".this."
              + functionName + "()))";

          StringBuilder expression = new StringBuilder();
          expression.append("\"url('\" + ");
          expression.append(instance).append(".getUrl()");
          expression.append(" + \"')\"");
          i.set(new ExpressionValue(expression.toString()));

        } else {
          i.remove();
          for (Value defValue : def.getValues()) {
            i.add(defValue);
          }
        }
      }

      x.setValue(new ListValue(values));
    }
  }

  /**
   * A lookup table of base-32 chars we use to encode CSS idents. Because CSS
   * class selectors may be case-insensitive, we don't have enough characters to
   * use a base-64 encoding.
   */
  private static final char[] BASE32_CHARS = new char[] {
      'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
      'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '-', '0',
      '1', '2', '3', '4'};

  /**
   * This value is used by {@link #concatOp} to help create a more balanced AST
   * tree by producing parenthetical expressions.
   */
  private static final int CONCAT_EXPRESSION_LIMIT = 20;

  /**
   * These constants are used to cache obfuscated class names.
   */
  private static final String KEY_BY_CLASS_AND_METHOD = "classAndMethod";
  private static final String KEY_HAS_CACHED_DATA = "hasCachedData";
  private static final String KEY_SHARED_METHODS = "sharedMethods";
  private static final String KEY_CLASS_PREFIX = "prefix";
  private static final String KEY_CLASS_COUNTER = "counter";

  public static void main(String[] args) {
    for (int i = 0; i < 1000; i++) {
      System.out.println(makeIdent(i));
    }
  }

  static boolean haveCommonProperties(CssRule a, CssRule b) {
    if (a.getProperties().size() == 0 || b.getProperties().size() == 0) {
      return false;
    }

    SortedSet<String> aProperties = new TreeSet<String>();
    SortedSet<String> bProperties = new TreeSet<String>();

    for (CssProperty p : a.getProperties()) {
      aProperties.add(p.getName());
    }
    for (CssProperty p : b.getProperties()) {
      bProperties.add(p.getName());
    }

    Iterator<String> ai = aProperties.iterator();
    Iterator<String> bi = bProperties.iterator();

    String aName = ai.next();
    String bName = bi.next();
    for (;;) {
      int comp = aName.compareToIgnoreCase(bName);
      if (comp == 0) {
        return true;
      } else if (comp > 0) {
        if (aName.startsWith(bName + "-")) {
          return true;
        }

        if (!bi.hasNext()) {
          break;
        }
        bName = bi.next();
      } else {
        if (bName.startsWith(aName + "-")) {
          return true;
        }
        if (!ai.hasNext()) {
          break;
        }
        aName = ai.next();
      }
    }

    return false;
  }

  /**
   * Create a Java expression that evaluates to a string representation of the
   * given node. Visible only for testing.
   */
  static <T extends CssNode & HasNodes> String makeExpression(
      TreeLogger logger, ResourceContext context, JClassType cssResourceType,
      T node, boolean prettyOutput) throws UnableToCompleteException {
    // Generate the CSS template
    DefaultTextOutput out = new DefaultTextOutput(!prettyOutput);
    CssGenerationVisitor v = new CssGenerationVisitor(out);
    v.accept(node);

    // Generate the final Java expression
    String template = out.toString();
    StringBuilder b = new StringBuilder();
    int start = 0;

    /*
     * Very large concatenation expressions using '+' cause the GWT compiler to
     * overflow the stack due to deep AST nesting. The workaround for now is to
     * force it to be more balanced using intermediate concatenation groupings.
     *
     * This variable is used to track the number of subexpressions within the
     * current parenthetical expression.
     */
    int numExpressions = 0;

    b.append('(');
    for (Map.Entry<Integer, List<CssNode>> entry : v.getSubstitutionPositions().entrySet()) {
      // Add the static section between start and the substitution point
      b.append('"');
      b.append(Generator.escape(template.substring(start, entry.getKey())));
      b.append('\"');
      numExpressions = concatOp(numExpressions, b);

      // Add the nodes at the substitution point
      for (CssNode x : entry.getValue()) {
        TreeLogger loopLogger = logger.branch(TreeLogger.DEBUG,
            "Performing substitution in node " + x.toString());

        if (x instanceof CssIf) {
          CssIf asIf = (CssIf) x;

          // Generate the sub-expressions
          String expression = makeExpression(loopLogger, context,
              cssResourceType, new CollapsedNode(asIf), prettyOutput);

          String elseExpression;
          if (asIf.getElseNodes().isEmpty()) {
            // We'll treat an empty else block as an empty string
            elseExpression = "\"\"";
          } else {
            elseExpression = makeExpression(loopLogger, context,
                cssResourceType, new CollapsedNode(asIf.getElseNodes()),
                prettyOutput);
          }

          // ((expr) ? "CSS" : "elseCSS") +
          b.append("((" + asIf.getExpression() + ") ? " + expression + " : "
              + elseExpression + ") ");
          numExpressions = concatOp(numExpressions, b);

        } else if (x instanceof CssProperty) {
          CssProperty property = (CssProperty) x;

          validateValue(loopLogger, context.getClientBundleType(),
              property.getValues());

          // (expr) +
          b.append("(" + property.getValues().getExpression() + ") ");
          numExpressions = concatOp(numExpressions, b);

        } else {
          // This indicates that some magic node is slipping by our visitors
          loopLogger.log(TreeLogger.ERROR, "Unhandled substitution "
              + x.getClass());
          throw new UnableToCompleteException();
        }
      }
      start = entry.getKey();
    }

    // Add the remaining parts of the template
    b.append('"');
    b.append(Generator.escape(template.substring(start)));
    b.append('"');
    b.append(')');

    return b.toString();
  }

  /**
   * Check if number of concat expressions currently exceeds limit and either
   * append '+' if the limit isn't reached or ') + (' if it is.
   *
   * @return numExpressions + 1 or 0 if limit was exceeded.
   */
  private static int concatOp(int numExpressions, StringBuilder b) {
    /*
     * TODO: Fix the compiler to better handle arbitrarily long concatenation
     * expressions.
     */
    if (numExpressions >= CONCAT_EXPRESSION_LIMIT) {
      b.append(") + (");
      return 0;
    }

    b.append(" + ");
    return numExpressions + 1;
  }

  private static String makeIdent(long id) {
    assert id >= 0;

    StringBuilder b = new StringBuilder();

    // Use only guaranteed-alpha characters for the first character
    b.append(BASE32_CHARS[(int) (id & 0xf)]);
    id >>= 4;

    while (id != 0) {
      b.append(BASE32_CHARS[(int) (id & 0x1f)]);
      id >>= 5;
    }

    return b.toString();
  }

  /**
   * This function validates any context-sensitive Values.
   */
  private static void validateValue(TreeLogger logger,
      JClassType resourceBundleType, Value value)
      throws UnableToCompleteException {

    ListValue list = value.isListValue();
    if (list != null) {
      for (Value v : list.getValues()) {
        validateValue(logger, resourceBundleType, v);
      }
      return;
    }

    DotPathValue dot = value.isDotPathValue();
    if (dot != null) {
      String[] elements = dot.getPath().split("\\.");
      if (elements.length == 0) {
        logger.log(TreeLogger.ERROR, "value() functions must specify a path");
        throw new UnableToCompleteException();
      }

      JType currentType = resourceBundleType;
      for (Iterator<String> i = Arrays.asList(elements).iterator(); i.hasNext();) {
        String pathElement = i.next();

        JClassType referenceType = currentType.isClassOrInterface();
        if (referenceType == null) {
          logger.log(TreeLogger.ERROR, "Cannot resolve member " + pathElement
              + " on non-reference type "
              + currentType.getQualifiedSourceName());
          throw new UnableToCompleteException();
        }

        try {
          JMethod m = referenceType.getMethod(pathElement, new JType[0]);
          currentType = m.getReturnType();
        } catch (NotFoundException e) {
          logger.log(TreeLogger.ERROR, "Could not find no-arg method named "
              + pathElement + " in type "
              + currentType.getQualifiedSourceName());
          throw new UnableToCompleteException();
        }
      }
      return;
    }
  }

  private Counter classCounter;
  private JClassType cssResourceType;
  private JClassType elementType;
  private boolean enableMerge;
  private boolean prettyOutput;
  private Map<JClassType, Map<JMethod, String>> replacementsByClassAndMethod;
  private Map<JMethod, String> replacementsForSharedMethods;
  private JClassType stringType;
  private Map<JMethod, CssStylesheet> stylesheetMap;

  @Override
  public String createAssignment(TreeLogger logger, ResourceContext context,
      JMethod method) throws UnableToCompleteException {

    SourceWriter sw = new StringSourceWriter();
    // Write the expression to create the subtype.
    sw.println("new " + method.getReturnType().getQualifiedSourceName()
        + "() {");
    sw.indent();

    JClassType cssResourceSubtype = method.getReturnType().isInterface();
    assert cssResourceSubtype != null;
    Map<String, Map<JMethod, String>> replacementsWithPrefix = new HashMap<String, Map<JMethod, String>>();

    replacementsWithPrefix.put("",
        computeReplacementsForType(cssResourceSubtype));
    Import imp = method.getAnnotation(Import.class);
    if (imp != null) {
      boolean fail = false;
      for (Class<? extends CssResource> clazz : imp.value()) {
        JClassType importType = context.getGeneratorContext().getTypeOracle().findType(
            clazz.getName().replace('$', '.'));
        assert importType != null;
        String prefix = importType.getSimpleSourceName();
        ImportedWithPrefix exp = importType.getAnnotation(ImportedWithPrefix.class);
        if (exp != null) {
          prefix = exp.value();
        }

        if (replacementsWithPrefix.put(prefix + "-",
            computeReplacementsForType(importType)) != null) {
          logger.log(TreeLogger.ERROR,
              "Multiple imports that would use the prefix " + prefix);
          fail = true;
        }
      }
      if (fail) {
        throw new UnableToCompleteException();
      }
    }

    sw.println("public String getText() {");
    sw.indent();
    boolean strict = isStrict(logger, context, method);
    Map<JMethod, String> actualReplacements = new IdentityHashMap<JMethod, String>();
    String cssExpression = makeExpression(logger, context, cssResourceSubtype,
        stylesheetMap.get(method), replacementsWithPrefix, strict,
        actualReplacements);
    sw.println("return " + cssExpression + ";");
    sw.outdent();
    sw.println("}");

    sw.println("public String getName() {");
    sw.indent();
    sw.println("return \"" + method.getName() + "\";");
    sw.outdent();
    sw.println("}");

    /*
     * getOverridableMethods is used to handle CssResources extending
     * non-CssResource types. See the discussion in computeReplacementsForType.
     */
    writeUserMethods(logger, sw, stylesheetMap.get(method),
        cssResourceSubtype.getOverridableMethods(), actualReplacements);

    sw.outdent();
    sw.println("}");

    return sw.toString();
  }

  @Override
  public void init(TreeLogger logger, ResourceContext context)
      throws UnableToCompleteException {
    String classPrefix;
    try {
      PropertyOracle propertyOracle = context.getGeneratorContext().getPropertyOracle();
      ConfigurationProperty styleProp = propertyOracle.getConfigurationProperty("CssResource.style");
      String style = styleProp.getValues().get(0);
      prettyOutput = style.equals("pretty");

      ConfigurationProperty mergeProp = propertyOracle.getConfigurationProperty("CssResource.mergeEnabled");
      String merge = mergeProp.getValues().get(0);
      enableMerge = merge.equals("true");

      ConfigurationProperty classPrefixProp = propertyOracle.getConfigurationProperty("CssResource.obfuscationPrefix");
      classPrefix = classPrefixProp.getValues().get(0);
    } catch (BadPropertyValueException e) {
      logger.log(TreeLogger.ERROR, "Unable to query module property", e);
      throw new UnableToCompleteException();
    }

    // Find all of the types that we care about in the type system
    TypeOracle typeOracle = context.getGeneratorContext().getTypeOracle();

    cssResourceType = typeOracle.findType(CssResource.class.getName());
    assert cssResourceType != null;

    elementType = typeOracle.findType(Element.class.getName());
    assert elementType != null;

    stringType = typeOracle.findType(String.class.getName());
    assert stringType != null;

    stylesheetMap = new IdentityHashMap<JMethod, CssStylesheet>();

    initReplacements(logger, context, classPrefix);
  }

  @Override
  public void prepare(TreeLogger logger, ResourceContext context,
      ClientBundleRequirements requirements, JMethod method)
      throws UnableToCompleteException {

    if (method.getReturnType().isInterface() == null) {
      logger.log(TreeLogger.ERROR, "Return type must be an interface");
      throw new UnableToCompleteException();
    }

    URL[] resources = ResourceGeneratorUtil.findResources(logger, context,
        method);

    if (resources.length == 0) {
      logger.log(TreeLogger.ERROR, "At least one source must be specified");
      throw new UnableToCompleteException();
    }

    // Create the AST and do a quick scan for requirements
    CssStylesheet sheet = GenerateCssAst.exec(logger, resources);
    stylesheetMap.put(method, sheet);
    (new RequirementsCollector(logger, requirements)).accept(sheet);
  }

  private String computeClassPrefix(String classPrefix,
      SortedSet<JClassType> cssResourceSubtypes) {
    if ("default".equals(classPrefix)) {
      classPrefix = null;
    } else if ("empty".equals(classPrefix)) {
      classPrefix = "";
    }

    if (classPrefix == null) {
      /*
       * Note that the checksum will miss some or all of the subtypes generated
       * by other generators.
       */
      Adler32 checksum = new Adler32();
      for (JClassType type : cssResourceSubtypes) {
        checksum.update(Util.getBytes(type.getQualifiedSourceName()));
      }
      classPrefix = "G"
          + Long.toString(checksum.getValue(), Character.MAX_RADIX);
    }

    return classPrefix;
  }

  /**
   * Each distinct type of CssResource has a unique collection of values that it
   * will return, excepting for those methods that are defined within an
   * interface that is tagged with {@code @Shared}.
   */
  private void computeObfuscatedNames(TreeLogger logger, String classPrefix,
      Set<JClassType> cssResourceSubtypes) {
    logger = logger.branch(TreeLogger.DEBUG, "Computing CSS class replacements");

    for (JClassType type : cssResourceSubtypes) {
      if (replacementsByClassAndMethod.containsKey(type)) {
        continue;
      }

      Map<JMethod, String> replacements = new IdentityHashMap<JMethod, String>();
      replacementsByClassAndMethod.put(type, replacements);

      for (JMethod method : type.getOverridableMethods()) {
        String name = method.getName();
        if ("getName".equals(name) || "getText".equals(name)
            || !stringType.equals(method.getReturnType())) {
          continue;
        }

        // The user provided the class name to use
        ClassName classNameOverride = method.getAnnotation(ClassName.class);
        if (classNameOverride != null) {
          name = classNameOverride.value();
        }

        String obfuscatedClassName = classPrefix + makeIdent(classCounter.next());
        if (prettyOutput) {
          obfuscatedClassName += "-"
              + type.getQualifiedSourceName().replaceAll("[.$]", "-") + "-"
              + name;
        }

        replacements.put(method, obfuscatedClassName);

        if (method.getEnclosingType() == type) {
          Shared shared = type.getAnnotation(Shared.class);
          if (shared != null) {
            replacementsForSharedMethods.put(method, obfuscatedClassName);
          }
        }

        logger.log(TreeLogger.SPAM, "Mapped " + type.getQualifiedSourceName()
            + "." + name + " to " + obfuscatedClassName);
      }
    }
  }

  /**
   * Returns all interfaces derived from CssResource, sorted by qualified name.
   * <p>
   * We'll ignore concrete implementations of CssResource, which include types
   * previously-generated by CssResourceGenerator and user-provided
   * implementations of CssResource, which aren't valid for use with
   * CssResourceGenerator anyway. By ignoring newly-generated CssResource types,
   * we'll ensure a stable ordering, regardless of the actual execution order
   * used by the Generator framework.
   * <p>
   * It is still possible that additional pure-interfaces could be introduced by
   * other generators, which would change the result of this computation, but
   * there is presently no way to determine when, or by what means, a type was
   * added to the TypeOracle.
   */
  private SortedSet<JClassType> computeOperableTypes(TreeLogger logger) {
    logger = logger.branch(TreeLogger.DEBUG,
        "Finding operable CssResource subtypes");

    SortedSet<JClassType> toReturn = new TreeSet<JClassType>(
        new JClassOrderComparator());

    JClassType[] cssResourceSubtypes = cssResourceType.getSubtypes();
    for (JClassType type : cssResourceSubtypes) {
      if (type.isInterface() != null) {
        logger.log(TreeLogger.SPAM, "Added " + type.getQualifiedSourceName());
        toReturn.add(type);

      } else {
        logger.log(TreeLogger.SPAM, "Ignored " + type.getQualifiedSourceName());
      }
    }

    return toReturn;
  }

  /**
   * Compute the mapping of original class names to obfuscated type names for a
   * given subtype of CssResource. Mappings are inherited from the type's
   * supertypes.
   */
  private Map<JMethod, String> computeReplacementsForType(JClassType type) {
    Map<JMethod, String> toReturn = new IdentityHashMap<JMethod, String>();

    /*
     * We check to see if the type is derived from CssResource so that we can
     * handle the case of a CssResource type being derived from a
     * non-CssResource base type. This basically collapses the non-CssResource
     * base types into their least-derived CssResource subtypes.
     */
    if (type == null || !derivedFromCssResource(type)) {
      return toReturn;
    }

    if (replacementsByClassAndMethod.containsKey(type)) {
      toReturn.putAll(replacementsByClassAndMethod.get(type));
    }

    /*
     * Replacements for methods defined in shared types will override any
     * locally-computed values.
     */
    for (JMethod method : type.getOverridableMethods()) {
      if (replacementsForSharedMethods.containsKey(method)) {
        assert toReturn.containsKey(method);
        toReturn.put(method, replacementsForSharedMethods.get(method));
      }
    }

    return toReturn;
  }

  /**
   * Determine if a type is derived from CssResource.
   */
  private boolean derivedFromCssResource(JClassType type) {
    List<JClassType> superInterfaces = Arrays.asList(type.getImplementedInterfaces());
    if (superInterfaces.contains(cssResourceType)) {
      return true;
    }

    JClassType superClass = type.getSuperclass();
    if (superClass != null) {
      if (derivedFromCssResource(superClass)) {
        return true;
      }
    }

    for (JClassType superInterface : superInterfaces) {
      if (derivedFromCssResource(superInterface)) {
        return true;
      }
    }
    return false;
  }

  /**
   * This method will initialize the maps that contain the obfuscated class
   * names.
   */
  @SuppressWarnings("unchecked")
  private void initReplacements(TreeLogger logger, ResourceContext context,
      String classPrefix) {
    /*
     * This code was originally written to take a snapshot of all the
     * CssResource descendants in the TypeOracle on its first run and calculate
     * the obfuscated names in one go, to ensure that the same obfuscation would
     * result regardless of the order in which the generators fired. (It no
     * longer behaves that way, as that scheme prevented the generation of new
     * CssResource interfaces, but the complexity lives on.)
     *
     * TODO(rjrjr,bobv) These days scottb tells us we're guaranteed that the
     * recompiling the same code will fire the generators in a consistent order,
     * so the old gymnastics aren't really justified anyway. It would probably
     * be be worth the effort to simplify this.
     */

    SortedSet<JClassType> cssResourceSubtypes = computeOperableTypes(logger);

    if (context.getCachedData(KEY_HAS_CACHED_DATA, Boolean.class) != Boolean.TRUE) {
      context.putCachedData(KEY_CLASS_COUNTER, new Counter());
      context.putCachedData(KEY_BY_CLASS_AND_METHOD,
          new IdentityHashMap<JClassType, Map<JMethod, String>>());
      context.putCachedData(KEY_SHARED_METHODS,
          new IdentityHashMap<JMethod, String>());
      context.putCachedData(KEY_CLASS_PREFIX, computeClassPrefix(classPrefix,
          cssResourceSubtypes));
      context.putCachedData(KEY_HAS_CACHED_DATA, Boolean.TRUE);
    }

    classCounter = context.getCachedData(KEY_CLASS_COUNTER, Counter.class);
    replacementsByClassAndMethod = context.getCachedData(
        KEY_BY_CLASS_AND_METHOD, Map.class);
    replacementsForSharedMethods = context.getCachedData(KEY_SHARED_METHODS,
        Map.class);
    classPrefix = context.getCachedData(KEY_CLASS_PREFIX, String.class);

    computeObfuscatedNames(logger, classPrefix, cssResourceSubtypes);
  }

  private boolean isStrict(TreeLogger logger, ResourceContext context,
      JMethod method) {
    Strict strictAnnotation = method.getAnnotation(Strict.class);
    NotStrict nonStrictAnnotation = method.getAnnotation(NotStrict.class);
    boolean strict = false;

    if (strictAnnotation != null && nonStrictAnnotation != null) {
      // Both annotations
      logger.log(TreeLogger.WARN, "Contradictory annotations "
          + Strict.class.getName() + " and " + NotStrict.class.getName()
          + " applied to the CssResource accessor method; assuming strict");
      strict = true;

    } else if (strictAnnotation == null && nonStrictAnnotation == null) {
      // Neither annotation

      /*
       * Fall back to using the to-be-deprecated strictAccessor property.
       */
      try {
        PropertyOracle propertyOracle = context.getGeneratorContext().getPropertyOracle();
        ConfigurationProperty prop = propertyOracle.getConfigurationProperty("CssResource.strictAccessors");
        String propertyValue = prop.getValues().get(0);
        if (Boolean.valueOf(propertyValue)) {
          logger.log(TreeLogger.WARN,
              "CssResource.strictAccessors is true, but " + method.getName()
                  + "() is missing the @Strict annotation.");
          strict = true;
        }
      } catch (BadPropertyValueException e) {
        // Ignore
      }

      if (!strict) {
        // This is a temporary warning during the transitional phase
        logger.log(TreeLogger.WARN, "Accessor does not specify "
            + Strict.class.getName() + " or " + NotStrict.class.getName()
            + ". The default behavior will change from non-strict "
            + "to strict in a future revision.");
      }

    } else if (nonStrictAnnotation != null) {
      // Only the non-strict annotation
      strict = false;

    } else if (strictAnnotation != null) {
      // Only the strict annotation
      strict = true;
    }

    return strict;
  }

  /**
   * Create a Java expression that evaluates to the string representation of the
   * stylesheet resource.
   *
   * @param actualReplacements An out parameter that will be populated by the
   *          obfuscated class names that should be used for the particular
   *          instance of the CssResource, based on any substitution
   *          modifications encoded in the source CSS file
   */
  private String makeExpression(TreeLogger logger, ResourceContext context,
      JClassType cssResourceType, CssStylesheet sheet,
      Map<String, Map<JMethod, String>> classReplacementsWithPrefix,
      boolean strict, Map<JMethod, String> actualReplacements)
      throws UnableToCompleteException {

    try {

      // Create CSS sprites
      (new Spriter(logger, context)).accept(sheet);

      // Perform @def and @eval substitutions
      SubstitutionCollector collector = new SubstitutionCollector();
      collector.accept(sheet);

      (new SubstitutionReplacer(logger, context, collector.substitutions)).accept(sheet);

      // Evaluate @if statements based on deferred binding properties
      (new IfEvaluator(logger,
          context.getGeneratorContext().getPropertyOracle())).accept(sheet);

      /*
       * Rename css .class selectors. We look for all @external declarations in
       * the stylesheet and then compute the per-instance replacements.
       */
      ExternalClassesCollector externalClasses = new ExternalClassesCollector();
      externalClasses.accept(sheet);
      ClassRenamer renamer = new ClassRenamer(logger,
          classReplacementsWithPrefix, strict, externalClasses.getClasses());
      renamer.accept(sheet);
      actualReplacements.putAll(renamer.getReplacements());

      // Combine rules with identical selectors
      if (enableMerge) {
        (new SplitRulesVisitor()).accept(sheet);
        (new MergeIdenticalSelectorsVisitor()).accept(sheet);
        (new MergeRulesByContentVisitor()).accept(sheet);
      }

      String standard = makeExpression(logger, context, cssResourceType, sheet,
          prettyOutput);

      (new RtlVisitor()).accept(sheet);

      String reversed = makeExpression(logger, context, cssResourceType, sheet,
          prettyOutput);

      return LocaleInfo.class.getName() + ".getCurrentLocale().isRTL() ? ("
          + reversed + ") : (" + standard + ")";

    } catch (CssCompilerException e) {
      // Take this as a sign that one of the visitors was unhappy, but only
      // log the stack trace if there's a causal (i.e. unknown) exception.
      logger.log(TreeLogger.ERROR, "Unable to process CSS",
          e.getCause() == null ? null : e);
      throw new UnableToCompleteException();
    }
  }

  /**
   * Write the CssResource accessor method for simple String return values.
   */
  private void writeClassAssignment(SourceWriter sw, JMethod toImplement,
      Map<JMethod, String> classReplacements) {

    String replacement = classReplacements.get(toImplement);
    assert replacement != null : "Missing replacement for "
        + toImplement.getName();

    sw.println(toImplement.getReadableDeclaration(false, true, true, true, true)
        + "{");
    sw.indent();
    sw.println("return \"" + replacement + "\";");
    sw.outdent();
    sw.println("}");
  }

  private void writeDefAssignment(TreeLogger logger, SourceWriter sw,
      JMethod toImplement, CssStylesheet cssStylesheet)
      throws UnableToCompleteException {
    SubstitutionCollector collector = new SubstitutionCollector();
    collector.accept(cssStylesheet);

    String name = toImplement.getName();
    // TODO: Annotation for override

    CssDef def = collector.substitutions.get(name);
    if (def == null) {
      logger.log(TreeLogger.ERROR, "No @def rule for name " + name);
      throw new UnableToCompleteException();
    }

    // TODO: Allow returning an array of values
    if (def.getValues().size() != 1) {
      logger.log(TreeLogger.ERROR, "@def rule " + name
          + " must define exactly one value");
      throw new UnableToCompleteException();
    }

    NumberValue numberValue = def.getValues().get(0).isNumberValue();

    String returnExpr = "";
    JClassType classReturnType = toImplement.getReturnType().isClass();
    if (classReturnType != null
        && "java.lang.String".equals(classReturnType.getQualifiedSourceName())) {
      returnExpr = "\"" + Generator.escape(def.getValues().get(0).toString())
          + "\"";
    } else {
      JPrimitiveType returnType = toImplement.getReturnType().isPrimitive();
      if (returnType == null) {
        logger.log(TreeLogger.ERROR, toImplement.getName()
            + ": Return type must be primitive type or String for "
            + "@def accessors");
        throw new UnableToCompleteException();
      }
      if (returnType == JPrimitiveType.INT || returnType == JPrimitiveType.LONG) {
        returnExpr = "" + Math.round(numberValue.getValue());
      } else if (returnType == JPrimitiveType.FLOAT) {
        returnExpr = numberValue.getValue() + "F";
      } else if (returnType == JPrimitiveType.DOUBLE) {
        returnExpr = "" + numberValue.getValue();
      } else {
        logger.log(TreeLogger.ERROR, returnType.getQualifiedSourceName()
            + " is not a valid primitive return type for @def accessors");
        throw new UnableToCompleteException();
      }
    }
    sw.print(toImplement.getReadableDeclaration(false, false, false, false,
        true));
    sw.println(" {");
    sw.indent();
    sw.println("return " + returnExpr + ";");
    sw.outdent();
    sw.println("}");
  }

  /**
   * Write all of the user-defined methods in the CssResource subtype.
   */
  private void writeUserMethods(TreeLogger logger, SourceWriter sw,
      CssStylesheet sheet, JMethod[] methods,
      Map<JMethod, String> obfuscatedClassNames)
      throws UnableToCompleteException {

    // Get list of @defs
    DefsCollector collector = new DefsCollector();
    collector.accept(sheet);

    for (JMethod toImplement : methods) {
      String name = toImplement.getName();
      if ("getName".equals(name) || "getText".equals(name)) {
        continue;
      }

      // Bomb out if there is a collision between @def and a style name
      if (collector.defs.contains(name)
          && obfuscatedClassNames.containsKey(toImplement)) {
        logger.log(TreeLogger.ERROR, "@def shadows CSS class name: " + name
            + ". Fix by renaming the @def name or the CSS class name.");
        throw new UnableToCompleteException();
      }

      if (collector.defs.contains(toImplement.getName())
          && toImplement.getParameters().length == 0) {
        writeDefAssignment(logger, sw, toImplement, sheet);
      } else if (toImplement.getReturnType().equals(stringType)
          && toImplement.getParameters().length == 0) {
        writeClassAssignment(sw, toImplement, obfuscatedClassNames);
      } else {
        logger.log(TreeLogger.ERROR, "Don't know how to implement method "
            + toImplement.getName());
        throw new UnableToCompleteException();
      }
    }
  }
}
