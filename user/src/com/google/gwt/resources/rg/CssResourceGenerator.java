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
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.CssResource.ClassName;
import com.google.gwt.resources.client.CssResource.Import;
import com.google.gwt.resources.client.CssResource.ImportedWithPrefix;
import com.google.gwt.resources.client.CssResource.NotStrict;
import com.google.gwt.resources.client.CssResource.Shared;
import com.google.gwt.resources.client.CssResource.Strict;
import com.google.gwt.resources.css.ClassRenamer;
import com.google.gwt.resources.css.CssGenerationVisitor;
import com.google.gwt.resources.css.DefsCollector;
import com.google.gwt.resources.css.ExternalClassesCollector;
import com.google.gwt.resources.css.GenerateCssAst;
import com.google.gwt.resources.css.IfEvaluator;
import com.google.gwt.resources.css.MergeIdenticalSelectorsVisitor;
import com.google.gwt.resources.css.MergeRulesByContentVisitor;
import com.google.gwt.resources.css.RequirementsCollector;
import com.google.gwt.resources.css.RtlVisitor;
import com.google.gwt.resources.css.SplitRulesVisitor;
import com.google.gwt.resources.css.Spriter;
import com.google.gwt.resources.css.SubstitutionCollector;
import com.google.gwt.resources.css.SubstitutionReplacer;
import com.google.gwt.resources.css.ast.CollapsedNode;
import com.google.gwt.resources.css.ast.CssCompilerException;
import com.google.gwt.resources.css.ast.CssDef;
import com.google.gwt.resources.css.ast.CssIf;
import com.google.gwt.resources.css.ast.CssNode;
import com.google.gwt.resources.css.ast.CssProperty;
import com.google.gwt.resources.css.ast.CssRule;
import com.google.gwt.resources.css.ast.CssStylesheet;
import com.google.gwt.resources.css.ast.HasNodes;
import com.google.gwt.resources.css.ast.CssProperty.DotPathValue;
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
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.Adler32;

/**
 * Provides implementations of CSSResources.
 */
public final class CssResourceGenerator extends AbstractResourceGenerator {

  @SuppressWarnings("serial")
  static class JClassOrderComparator implements Comparator<JClassType>,
      Serializable {
    public int compare(JClassType o1, JClassType o2) {
      return o1.getQualifiedSourceName().compareTo(o2.getQualifiedSourceName());
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

  public static boolean haveCommonProperties(CssRule a, CssRule b) {
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

  public static void main(String[] args) {
    for (int i = 0; i < 1000; i++) {
      System.out.println(makeIdent(i));
    }
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

    // Methods defined by CssResource interface
    writeEnsureInjected(sw);
    writeGetName(method, sw);

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

        String obfuscatedClassName = classPrefix
            + makeIdent(classCounter.next());
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

      (new SubstitutionReplacer(logger, context, collector.getSubstitutions())).accept(sheet);

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

    CssDef def = collector.getSubstitutions().get(name);
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

  private void writeEnsureInjected(SourceWriter sw) {
    sw.println("private boolean injected;");
    sw.println("public boolean ensureInjected() {");
    sw.indent();
    sw.println("if (!injected) {");
    sw.indentln("injected = true;");
    sw.indentln(StyleInjector.class.getName() + ".injectStylesheet(getText());");
    sw.indentln("return true;");
    sw.println("}");
    sw.println("return false;");
    sw.outdent();
    sw.println("}");
  }

  private void writeGetName(JMethod method, SourceWriter sw) {
    sw.println("public String getName() {");
    sw.indent();
    sw.println("return \"" + method.getName() + "\";");
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
    Set<String> defs = collector.getDefs();

    for (JMethod toImplement : methods) {
      String name = toImplement.getName();
      if ("getName".equals(name) || "getText".equals(name)
          || "ensureInjected".equals(name)) {
        continue;
      }

      // Bomb out if there is a collision between @def and a style name
      if (defs.contains(name) && obfuscatedClassNames.containsKey(toImplement)) {
        logger.log(TreeLogger.ERROR, "@def shadows CSS class name: " + name
            + ". Fix by renaming the @def name or the CSS class name.");
        throw new UnableToCompleteException();
      }

      if (defs.contains(toImplement.getName())
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
