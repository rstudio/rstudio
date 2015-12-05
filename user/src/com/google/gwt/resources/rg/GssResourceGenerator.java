/*
 * Copyright 2014 Google Inc.
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
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.util.Util;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.resources.client.ClientBundle.Source;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.CssResource.ClassName;
import com.google.gwt.resources.client.CssResource.Import;
import com.google.gwt.resources.client.CssResource.ImportedWithPrefix;
import com.google.gwt.resources.client.CssResource.NotStrict;
import com.google.gwt.resources.client.CssResource.Shared;
import com.google.gwt.resources.client.ResourcePrototype;
import com.google.gwt.resources.converter.Css2Gss;
import com.google.gwt.resources.converter.Css2GssConversionException;
import com.google.gwt.resources.ext.ClientBundleRequirements;
import com.google.gwt.resources.ext.ResourceContext;
import com.google.gwt.resources.ext.ResourceGeneratorUtil;
import com.google.gwt.resources.ext.SupportsGeneratorResultCaching;
import com.google.gwt.resources.gss.BooleanConditionCollector;
import com.google.gwt.resources.gss.CollectAndRemoveConstantDefinitions;
import com.google.gwt.resources.gss.CreateRuntimeConditionalNodes;
import com.google.gwt.resources.gss.CssPrinter;
import com.google.gwt.resources.gss.ExtendedEliminateConditionalNodes;
import com.google.gwt.resources.gss.ExternalClassesCollector;
import com.google.gwt.resources.gss.GwtGssFunctionMapProvider;
import com.google.gwt.resources.gss.ImageSpriteCreator;
import com.google.gwt.resources.gss.PermutationsCollector;
import com.google.gwt.resources.gss.RecordingBidiFlipper;
import com.google.gwt.resources.gss.RenamingSubstitutionMap;
import com.google.gwt.resources.gss.RuntimeConditionalBlockCollector;
import com.google.gwt.resources.gss.ValidateRuntimeConditionalNode;
import com.google.gwt.resources.rg.CssResourceGenerator.JClassOrderComparator;
import com.google.gwt.thirdparty.common.css.MinimalSubstitutionMap;
import com.google.gwt.thirdparty.common.css.PrefixingSubstitutionMap;
import com.google.gwt.thirdparty.common.css.SourceCode;
import com.google.gwt.thirdparty.common.css.SourceCodeLocation;
import com.google.gwt.thirdparty.common.css.SubstitutionMap;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssCompositeValueNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssDefinitionNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssNumericNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssTree;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssValueNode;
import com.google.gwt.thirdparty.common.css.compiler.ast.ErrorManager;
import com.google.gwt.thirdparty.common.css.compiler.ast.GssError;
import com.google.gwt.thirdparty.common.css.compiler.ast.GssFunction;
import com.google.gwt.thirdparty.common.css.compiler.ast.GssParser;
import com.google.gwt.thirdparty.common.css.compiler.ast.GssParserException;
import com.google.gwt.thirdparty.common.css.compiler.passes.AbbreviatePositionalValues;
import com.google.gwt.thirdparty.common.css.compiler.passes.CheckDependencyNodes;
import com.google.gwt.thirdparty.common.css.compiler.passes.CollectConstantDefinitions;
import com.google.gwt.thirdparty.common.css.compiler.passes.CollectMixinDefinitions;
import com.google.gwt.thirdparty.common.css.compiler.passes.ColorValueOptimizer;
import com.google.gwt.thirdparty.common.css.compiler.passes.ConstantDefinitions;
import com.google.gwt.thirdparty.common.css.compiler.passes.CreateComponentNodes;
import com.google.gwt.thirdparty.common.css.compiler.passes.CreateConditionalNodes;
import com.google.gwt.thirdparty.common.css.compiler.passes.CreateConstantReferences;
import com.google.gwt.thirdparty.common.css.compiler.passes.CreateDefinitionNodes;
import com.google.gwt.thirdparty.common.css.compiler.passes.CreateForLoopNodes;
import com.google.gwt.thirdparty.common.css.compiler.passes.CreateMixins;
import com.google.gwt.thirdparty.common.css.compiler.passes.CreateStandardAtRuleNodes;
import com.google.gwt.thirdparty.common.css.compiler.passes.CreateVendorPrefixedKeyframes;
import com.google.gwt.thirdparty.common.css.compiler.passes.CssClassRenaming;
import com.google.gwt.thirdparty.common.css.compiler.passes.DisallowDuplicateDeclarations;
import com.google.gwt.thirdparty.common.css.compiler.passes.EliminateEmptyRulesetNodes;
import com.google.gwt.thirdparty.common.css.compiler.passes.EliminateUnitsFromZeroNumericValues;
import com.google.gwt.thirdparty.common.css.compiler.passes.EliminateUselessRulesetNodes;
import com.google.gwt.thirdparty.common.css.compiler.passes.HandleUnknownAtRuleNodes;
import com.google.gwt.thirdparty.common.css.compiler.passes.MarkNonFlippableNodes;
import com.google.gwt.thirdparty.common.css.compiler.passes.MarkRemovableRulesetNodes;
import com.google.gwt.thirdparty.common.css.compiler.passes.MergeAdjacentRulesetNodesWithSameDeclarations;
import com.google.gwt.thirdparty.common.css.compiler.passes.MergeAdjacentRulesetNodesWithSameSelector;
import com.google.gwt.thirdparty.common.css.compiler.passes.ProcessComponents;
import com.google.gwt.thirdparty.common.css.compiler.passes.ProcessKeyframes;
import com.google.gwt.thirdparty.common.css.compiler.passes.ProcessRefiners;
import com.google.gwt.thirdparty.common.css.compiler.passes.ReplaceConstantReferences;
import com.google.gwt.thirdparty.common.css.compiler.passes.ReplaceMixins;
import com.google.gwt.thirdparty.common.css.compiler.passes.ResolveCustomFunctionNodes;
import com.google.gwt.thirdparty.common.css.compiler.passes.SplitRulesetNodes;
import com.google.gwt.thirdparty.common.css.compiler.passes.UnrollLoops;
import com.google.gwt.thirdparty.common.css.compiler.passes.ValidatePropertyValues;
import com.google.gwt.thirdparty.guava.common.base.CaseFormat;
import com.google.gwt.thirdparty.guava.common.base.Charsets;
import com.google.gwt.thirdparty.guava.common.base.Joiner;
import com.google.gwt.thirdparty.guava.common.base.Predicate;
import com.google.gwt.thirdparty.guava.common.base.Predicates;
import com.google.gwt.thirdparty.guava.common.base.Strings;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableMap;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableSet;
import com.google.gwt.thirdparty.guava.common.collect.ImmutableSet.Builder;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Sets;
import com.google.gwt.thirdparty.guava.common.io.ByteSource;
import com.google.gwt.thirdparty.guava.common.io.Resources;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.gwt.user.rebind.StringSourceWriter;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Adler32;

/**
 * This generator parses and compiles a GSS file to a css string and generates the implementation
 * of the corresponding CssResource interface.
 */
public class GssResourceGenerator extends AbstractCssResourceGenerator implements
    SupportsGeneratorResultCaching {

  /**
   * GssOptions contains the values of all configuration properties that can be used with
   * GssResource.
   */
  public static class GssOptions {
    private final boolean enabled;
    private final AutoConversionMode autoConversionMode;
    private final boolean gssDefaultInUiBinder;

    public GssOptions(boolean enabled, AutoConversionMode autoConversionMode, boolean gssDefaultInUiBinder) {
      this.enabled = enabled;
      this.autoConversionMode = autoConversionMode;
      this.gssDefaultInUiBinder = gssDefaultInUiBinder;
    }

    public boolean isEnabled() {
      return enabled;
    }

    public boolean isGssDefaultInUiBinder() {
      return gssDefaultInUiBinder;
    }

    public boolean isAutoConversionOff() {
      return autoConversionMode == AutoConversionMode.OFF;
    }

    public boolean isLenientConversion() {
      return autoConversionMode == AutoConversionMode.LENIENT;
    }
  }

  /**
   * Different conversion modes from css to gss.
   */
  public enum AutoConversionMode { STRICT, LENIENT, OFF }

  /*
   * TODO(dankurka): This is a nasty hack to get the compiler to output all @def's
   * it has seen in a compile. Once GSS migration is done this needs to be removed.
   */
  private static boolean shouldEmitVariables;
  private static PrintWriter printWriter;
  private static Set<String> writtenAtDefs = new HashSet<>();

  private static final String KEY_ENABLE_GSS = "CssResource.enableGss";
  private static final String KEY_GSS_DEFAULT_IN_UIBINDER = "CssResource.gssDefaultInUiBinder";

  static {
    String varFileName = System.getProperty("emitGssVarNameFile");
    shouldEmitVariables = varFileName != null;
    if (shouldEmitVariables) {
      try {
        File file = new File(varFileName);
        file.createNewFile();
        printWriter = new PrintWriter(new FileOutputStream(file));
      } catch (Exception e) {
        System.err.println("Error while opening file");
        e.printStackTrace();
        System.exit(-1);
      }
    }
  }

  public static SourceCode readUrlContent(URL fileUrl, TreeLogger logger) throws UnableToCompleteException {
    TreeLogger branchLogger = logger.branch(TreeLogger.DEBUG,
            "Reading GSS stylesheet " + fileUrl.toExternalForm());
    try {
      ByteSource byteSource = Resources.asByteSource(fileUrl);
      // default charset
      Charset charset = Charsets.UTF_8;

      // check if the stylesheet doesn't include a @charset at-rule
      String styleSheetCharset = extractCharset(byteSource);
      if (styleSheetCharset != null) {
        try {
          charset = Charset.forName(styleSheetCharset);
        } catch (UnsupportedCharsetException e) {
          logger.log(Type.ERROR, "Unsupported charset found: " + styleSheetCharset);
          throw new UnableToCompleteException();
        }
      }

      String fileContent = byteSource.asCharSource(charset).read();
      // If the stylesheet specified a charset, we have to remove the at-rule otherwise the GSS
      // compiler will fail.
      if (styleSheetCharset != null) {
        int charsetAtRuleLength = CHARSET_MIN_LENGTH + styleSheetCharset.length();
        // replace charset at-rule by blanks to keep correct source location of the rest of
        // the stylesheet.
        fileContent = Strings.repeat(" ", charsetAtRuleLength) +
                fileContent.substring(charsetAtRuleLength);
      }
      return new SourceCode(fileUrl.getFile(), fileContent);

    } catch (IOException e) {
      branchLogger.log(TreeLogger.ERROR, "Unable to parse CSS", e);
    }
    throw new UnableToCompleteException();
  }

  public static GssOptions getGssOptions(PropertyOracle propertyOracle, TreeLogger logger) throws UnableToCompleteException {
    boolean gssEnabled;
    boolean gssDefaultInUiBinder;
    AutoConversionMode conversionMode;

    try {
      ConfigurationProperty enableGssProp =
          propertyOracle.getConfigurationProperty(KEY_ENABLE_GSS);
      String enableGss = enableGssProp.getValues().get(0);
      gssEnabled = Boolean.parseBoolean(enableGss);
    } catch (BadPropertyValueException ex) {
      logger.log(Type.ERROR, "Unable to determine if GSS need to be used");
      throw new UnableToCompleteException();
    }
    try {
      conversionMode = Enum.valueOf(AutoConversionMode.class, propertyOracle
          .getConfigurationProperty(KEY_CONVERSION_MODE).getValues().get(0)
          .toUpperCase(Locale.ROOT));
    } catch (BadPropertyValueException ex) {
      logger.log(Type.ERROR, "Unable to conversion mode for GSS");
      throw new UnableToCompleteException();
    }
    try {
      ConfigurationProperty uiBinderGssDefaultProp =
          propertyOracle.getConfigurationProperty(KEY_GSS_DEFAULT_IN_UIBINDER);
      String uiBinderGssDefaultValue = uiBinderGssDefaultProp.getValues().get(0);
      gssDefaultInUiBinder =  Boolean.parseBoolean(uiBinderGssDefaultValue);
    } catch (BadPropertyValueException ex) {
      logger.log(Type.ERROR, "Unable to determine default for GSS in UiBinder");
      throw new UnableToCompleteException();
    }
    return new GssOptions(gssEnabled, conversionMode, gssDefaultInUiBinder);
  }

  private static synchronized void write(Set<String> variables) {
    for (String atDef : variables) {
      if (writtenAtDefs.add(atDef)) {
        printWriter.println("@def " + atDef + " 1px;");
      }
    }
    printWriter.flush();
  }

  /**
   * {@link ErrorManager} used to log the errors and warning messages produced by the different
   * {@link com.google.gwt.thirdparty.common.css.compiler.ast.CssCompilerPass}.
   */
  public static class LoggerErrorManager implements ErrorManager {
    private final TreeLogger logger;
    private boolean hasErrors;

    public LoggerErrorManager(TreeLogger logger) {
      this.logger = logger;
    }

    @Override
    public void generateReport() {
      // do nothing
    }

    @Override
    public boolean hasErrors() {
      return hasErrors;
    }

    @Override
    public void report(GssError error) {
      String fileName = "";
      String location = "";
      SourceCodeLocation codeLocation = error.getLocation();

      if (codeLocation != null) {
        fileName = codeLocation.getSourceCode().getFileName();
        location = "[line: " + codeLocation.getBeginLineNumber() + " column: " + codeLocation
            .getBeginIndexInLine() + "]";
      }

      logger.log(Type.ERROR, "Error in " + fileName + location + ": " + error.getMessage());
      hasErrors = true;
    }

    @Override
    public void reportWarning(GssError warning) {
      logger.log(Type.WARN, warning.getMessage());
    }
  }

  private static class ConversionResult {
    final String gss;
    final Map<String, String> defNameMapping;

    private ConversionResult(String gss, Map<String, String> defNameMapping) {
      this.gss = gss;
      this.defNameMapping = defNameMapping;
    }
  }

  private static class RenamingResult {
    final Map<String, String> mapping;
    final Set<String> externalClassCandidate;

    private RenamingResult(Map<String, String> mapping, Set<String> externalClassCandidate) {
      this.mapping = mapping;
      this.externalClassCandidate = externalClassCandidate;
    }
  }

  private static class CssParsingResult {
    final CssTree tree;
    final List<String> permutationAxes;
    final Map<String, String> originalConstantNameMapping;
    final Set<String> trueConditions;

    private CssParsingResult(CssTree tree, List<String> permutationAxis, Set<String> trueConditions,
        Map<String, String> originalConstantNameMapping) {
      this.tree = tree;
      this.permutationAxes = permutationAxis;
      this.originalConstantNameMapping = originalConstantNameMapping;
      this.trueConditions = trueConditions;
    }
  }

  /**
   * Predicate implementation used during the conversion to GSS.
   */
  private static class ConfigurationPropertyMatcher implements Predicate<String> {
    private final PropertyOracle propertyOracle;
    private final TreeLogger logger;

    private boolean error;

    ConfigurationPropertyMatcher(ResourceContext context, TreeLogger logger) {
      this.logger = logger;
      propertyOracle = context.getGeneratorContext().getPropertyOracle();
    }

    @Override
    public boolean apply(String booleanCondition) {
      // if the condition is negated, the string parameter contains the ! operator if this method
      // is called during the conversion to GSS
      if (booleanCondition.startsWith("!")) {
        booleanCondition = booleanCondition.substring(1);
      }

      try {
        ConfigurationProperty property = propertyOracle.getConfigurationProperty(booleanCondition);
        boolean valid = checkPropertyIsSingleValueAndBoolean(property, logger);

        error |= !valid;

        return valid;
      } catch (BadPropertyValueException e) {
        return false;
      }
    }
  }

  // To be sure to avoid conflict during the style classes renaming between different GssResources,
  // we will create a different prefix for each GssResource. We use a MinimalSubstitutionMap
  // that will create a String with 1-6 characters in length but keeping the length of the prefix
  // as short as possible. For instance if we have two GssResources to compile, the  prefix
  // for the first resource will be 'a' and the prefix for the second resource will be 'b' and so on
  private static final SubstitutionMap resourcePrefixBuilder = new MinimalSubstitutionMap();
  private static final String KEY_CONVERSION_MODE = "CssResource.conversionMode";
  private static final String KEY_STYLE = "CssResource.style";
  private static final String ALLOWED_AT_RULE = "CssResource.allowedAtRules";
  private static final String ALLOWED_FUNCTIONS = "CssResource.allowedFunctions";
  private static final String KEY_OBFUSCATION_PREFIX = "CssResource.obfuscationPrefix";
  private static final String KEY_CLASS_PREFIX = "cssResourcePrefix";
  private static final String KEY_BY_CLASS_AND_METHOD = "cssResourceClassAndMethod";
  private static final String KEY_HAS_CACHED_DATA = "hasCachedData";
  private static final String KEY_SHARED_METHODS = "sharedMethods";
  private static final char[] BASE32_CHARS = new char[]{
      'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
      'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', '0', '1',
      '2', '3', '4', '5', '6'};
  // We follow CSS specification to detect the charset:
  // - Authors using an @charset rule must place the rule at the very beginning of the style sheet,
  // preceded by no characters.
  // - @charset must be written literally, i.e., the 10 characters '@charset "' (lowercase, no
  // backslash escapes), followed by the encoding name, followed by '";'.
  // see: http://www.w3.org/TR/CSS2/syndata.html#charset
  private static final Pattern CHARSET = Pattern.compile("^@charset \"([^\"]*)\";");
  private static final int CHARSET_MIN_LENGTH = "@charset \"\";".length();

  /**
   * Returns the import prefix for a type, including the trailing hyphen.
   */
  public static String getImportPrefix(JClassType importType) {
    String prefix = importType.getSimpleSourceName();
    ImportedWithPrefix exp = importType.getAnnotation(ImportedWithPrefix.class);
    if (exp != null) {
      prefix = exp.value();
    }

    return prefix + "-";
  }

  private static String encode(long id) {
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

  private static boolean checkPropertyIsSingleValueAndBoolean(ConfigurationProperty property,
      TreeLogger logger) {
    List<String> values = property.getValues();

    if (values.size() > 1) {
      logger.log(Type.ERROR, "The configuration property " + property.getName() + " is used in " +
          "a conditional css and cannot be a multi-valued property");
      return false;
    }

    String value = values.get(0);

    if (!"true".equals(value) && !"false".equals(value)) {
      logger.log(Type.ERROR, "The configuration property " + property.getName() + " is used in " +
          "a conditional css. Its value must be either \"true\" or \"false\"");
      return false;
    }

    return true;
  }

  /**
   * Temporary method needed when GSS and the old CSS syntax are both supported by the sdk.
   * It aims to choose the right resource file according to whether gss is enabled or not. If gss is
   * enabled, it will try to find the resource file ending by .gss first. If GSS is disabled it will
   * try to find the .css file. This logic is applied even if a
   * {@link com.google.gwt.resources.client.ClientBundle.Source} annotation is used to define
   * the resource file.
   * <p>
   * This method can be deleted once the support for the old CssResource is removed and use directly
   * ResourceGeneratorUtil.findResources().
   */
  static URL[] findResources(TreeLogger logger, ResourceContext context, JMethod method,
      boolean gssEnabled) throws UnableToCompleteException {

    boolean isSourceAnnotationUsed = method.getAnnotation(Source.class) != null;

    if (!isSourceAnnotationUsed) {
      // ResourceGeneratorUtil will try to find automatically the resource file. Give him the right
      // extension to use first
      String[] extensions = gssEnabled ?
          new String[]{".gss", ".css"} : new String[]{".css", ".gss"};
      return  ResourceGeneratorUtil.findResources(logger, context, method, extensions);
    }

    // find the original resource files specified by the @Source annotation
    URL[] originalResources = ResourceGeneratorUtil.findResources(logger, context, method);
    URL[] resourcesToUse = new URL[originalResources.length];

    String preferredExtension = gssEnabled ? ".gss" : ".css";

    // Try to find all the resources by using the preferred extension according to whether gss is
    // enabled or not. If one file with the preferred extension is missing, return the original
    // resource files otherwise return the preferred files.
    String[] sourceFiles = method.getAnnotation(Source.class).value();
    for (int i = 0; i < sourceFiles.length; i++) {
      String original = sourceFiles[i];

      if (!original.endsWith(preferredExtension) && original.length() > 4) {
        String preferredFile = original.substring(0, original.length() - 4) + preferredExtension;

        // try to find the resource relative to the package
        String path = method.getEnclosingType().getPackage().getName().replace('.', '/') + '/';
        URL preferredUrl = ResourceGeneratorUtil
            .tryFindResource(logger, context.getGeneratorContext(), context, path + preferredFile);

        if (preferredUrl == null) {
          // if it doesn't exist, assume it is absolute
          preferredUrl = ResourceGeneratorUtil
              .tryFindResource(logger, context.getGeneratorContext(), context, preferredFile);
        }

        if (preferredUrl == null) {
          // avoid to mix gss and css, if one file with the preferred extension is missing
          return originalResources;
        }

        logger.log(Type.DEBUG, "Preferred resource file found: " + preferredFile + ". This file " +
            "will be used in replacement of " + original);

        resourcesToUse[i] = preferredUrl;
      } else {
        // gss and css files shouldn't be used together for a same resource. So if one of the file
        // is using the the preferred extension, return the original resources. If the dev has mixed
        // gss and ccs files, that will fail later.
        return originalResources;
      }
    }

    return resourcesToUse;
  }

  private Map<JMethod, CssParsingResult> cssParsingResultMap;
  private Set<String> allowedNonStandardFunctions;
  private LoggerErrorManager errorManager;
  private JMethod getTextMethod;
  private JMethod ensuredInjectedMethod;
  private JMethod getNameMethod;
  private String obfuscationPrefix;
  private CssObfuscationStyle obfuscationStyle;
  private Set<String> allowedAtRules;
  private Map<JClassType, Map<String, String>> replacementsByClassAndMethod;
  private Map<JMethod, String> replacementsForSharedMethods;
  private final GssOptions gssOptions;

  public GssResourceGenerator(GssOptions gssOptions) {
    this.gssOptions = gssOptions;
  }

  @Override
  public String createAssignment(TreeLogger logger, ResourceContext context, JMethod method)
      throws UnableToCompleteException {
    CssParsingResult cssParsingResult = cssParsingResultMap.get(method);
    CssTree cssTree = cssParsingResult.tree;

    RenamingResult renamingResult = doClassRenaming(cssTree, method, logger, context);

    // TODO : Should we foresee configuration properties for simplifyCss and eliminateDeadCode
    // booleans ?
    ConstantDefinitions constantDefinitions = optimizeTree(cssParsingResult, context, true, true,
        logger);

    checkErrors();

    Set<String> externalClasses = revertRenamingOfExternalClasses(cssTree, renamingResult);

    checkErrors();

    // Validate that classes not assigned to one of the interface methods are external
    validateExternalClasses(externalClasses, renamingResult.externalClassCandidate, method, logger);

    SourceWriter sw = new StringSourceWriter();
    sw.println("new " + method.getReturnType().getQualifiedSourceName() + "() {");
    sw.indent();

    Map<JMethod, String> actualReplacements = writeMethods(logger, context, method, sw, constantDefinitions,
        cssParsingResult.originalConstantNameMapping, renamingResult.mapping);

    sw.outdent();
    sw.println("}");

    CssResourceGenerator.outputCssMapArtifact(logger, context, method, actualReplacements);

    return sw.toString();
  }

  private void validateExternalClasses(Set<String> externalClasses,
      Set<String> externalClassCandidates, JMethod method,
      TreeLogger logger) throws UnableToCompleteException {
    if (!isStrictResource(method)) {
      return;
    }

    boolean hasError = false;

    for (String candidate : externalClassCandidates) {
      if (!externalClasses.contains(candidate)) {
        logger.log(Type.ERROR, "The following non-obfuscated class is present in a strict "
            + "CssResource: " + candidate + ". Fix by adding String accessor "
            + "method(s) to the CssResource interface for obfuscated classes, "
            + "or use an @external declaration for unobfuscated classes.");
        hasError = true;
      }
    }

    if (hasError) {
      throw new UnableToCompleteException();
    }
  }

  @Override
  public void init(TreeLogger logger, ResourceContext context) throws UnableToCompleteException {
    cssParsingResultMap = new IdentityHashMap<>();
    errorManager = new LoggerErrorManager(logger);

    allowedNonStandardFunctions = new HashSet<>();
    allowedAtRules = Sets.newHashSet(ExternalClassesCollector.EXTERNAL_AT_RULE);

    try {
      PropertyOracle propertyOracle = context.getGeneratorContext().getPropertyOracle();

      ConfigurationProperty styleProp = propertyOracle.getConfigurationProperty(KEY_STYLE);
      obfuscationStyle = CssObfuscationStyle.getObfuscationStyle(styleProp.getValues().get(0));
      obfuscationPrefix = getObfuscationPrefix(propertyOracle, context);

      ConfigurationProperty allowedAtRuleProperty = propertyOracle
          .getConfigurationProperty(ALLOWED_AT_RULE);
      allowedAtRules.addAll(allowedAtRuleProperty.getValues());

      ConfigurationProperty allowedFunctionsProperty = propertyOracle
          .getConfigurationProperty(ALLOWED_FUNCTIONS);
      allowedNonStandardFunctions.addAll(allowedFunctionsProperty.getValues());

      ClientBundleRequirements requirements = context.getRequirements();
      requirements.addConfigurationProperty(KEY_STYLE);
      requirements.addConfigurationProperty(KEY_OBFUSCATION_PREFIX);
      requirements.addConfigurationProperty(ALLOWED_AT_RULE);
      requirements.addConfigurationProperty(ALLOWED_FUNCTIONS);
      requirements.addConfigurationProperty(KEY_CONVERSION_MODE);
    } catch (BadPropertyValueException e) {
      logger.log(TreeLogger.ERROR, "Unable to query module property", e);
      throw new UnableToCompleteException();
    }

    TypeOracle typeOracle = context.getGeneratorContext().getTypeOracle();
    JClassType cssResourceInterface = typeOracle.findType(CssResource.class.getCanonicalName());
    JClassType resourcePrototypeInterface = typeOracle.findType(ResourcePrototype.class
        .getCanonicalName());

    try {
      getTextMethod = cssResourceInterface.getMethod("getText", new JType[0]);
      ensuredInjectedMethod = cssResourceInterface.getMethod("ensureInjected", new JType[0]);
      getNameMethod = resourcePrototypeInterface.getMethod("getName", new JType[0]);
    } catch (NotFoundException e) {
      logger.log(TreeLogger.ERROR, "Unable to lookup methods from CssResource and " +
          "ResourcePrototype interface", e);
      throw new UnableToCompleteException();
    }

    initReplacement(context);
  }

  @SuppressWarnings("unchecked")
  private void initReplacement(ResourceContext context) {
    if (context.getCachedData(KEY_HAS_CACHED_DATA, Boolean.class) != Boolean.TRUE) {

      context.putCachedData(KEY_SHARED_METHODS, new IdentityHashMap<JMethod, String>());
      context.putCachedData(KEY_BY_CLASS_AND_METHOD, new IdentityHashMap<JClassType, Map<String,
          String>>());
      context.putCachedData(KEY_HAS_CACHED_DATA, Boolean.TRUE);
    }

    replacementsByClassAndMethod = context.getCachedData(KEY_BY_CLASS_AND_METHOD, Map.class);
    replacementsForSharedMethods = context.getCachedData(KEY_SHARED_METHODS, Map.class);
  }

  private String getObfuscationPrefix(PropertyOracle propertyOracle, ResourceContext context)
      throws BadPropertyValueException {
    String prefix = propertyOracle.getConfigurationProperty(KEY_OBFUSCATION_PREFIX)
        .getValues().get(0);
    if ("empty".equalsIgnoreCase(prefix)) {
      return "";
    } else if ("default".equalsIgnoreCase(prefix)) {
      return getDefaultObfuscationPrefix(context);
    }

    return prefix;
  }

  private String getDefaultObfuscationPrefix(ResourceContext context) {
    String prefix = context.getCachedData(KEY_CLASS_PREFIX, String.class);
    if (prefix == null) {
      prefix = computeDefaultPrefix(context);
      context.putCachedData(KEY_CLASS_PREFIX, prefix);
    }

    return prefix;
  }

  private String computeDefaultPrefix(ResourceContext context) {
    SortedSet<JClassType> gssResources = computeOperableTypes(context);

    Adler32 checksum = new Adler32();

    for (JClassType type : gssResources) {
      checksum.update(Util.getBytes(type.getQualifiedSourceName()));
    }

    int seed = Math.abs((int) checksum.getValue());

    return encode(seed) + "-";
  }

  private SortedSet<JClassType> computeOperableTypes(ResourceContext context) {
    TypeOracle typeOracle = context.getGeneratorContext().getTypeOracle();
    JClassType baseInterface = typeOracle.findType(CssResource.class.getCanonicalName());

    SortedSet<JClassType> toReturn = new TreeSet<>(new JClassOrderComparator());

    JClassType[] cssResourceSubtypes = baseInterface.getSubtypes();
    for (JClassType type : cssResourceSubtypes) {
      if (type.isInterface() != null) {
        toReturn.add(type);
      }
    }

    return toReturn;
  }

  @Override
  public void prepare(TreeLogger logger, ResourceContext context,
      ClientBundleRequirements requirements, JMethod method) throws UnableToCompleteException {

    if (method.getReturnType().isInterface() == null) {
      logger.log(TreeLogger.ERROR, "Return type must be an interface");
      throw new UnableToCompleteException();
    }

    URL[] resourceUrls = findResources(logger, context, method, gssOptions.isEnabled());
    if (resourceUrls.length == 0) {
      logger.log(TreeLogger.ERROR, "At least one source must be specified");
      throw new UnableToCompleteException();
    }

    CssParsingResult cssParsingResult = parseResources(Lists.newArrayList(resourceUrls), context,
        logger);

    cssParsingResultMap.put(method, cssParsingResult);

    for (String permutationAxis : cssParsingResult.permutationAxes) {
      try {
        context.getRequirements().addPermutationAxis(permutationAxis);
      } catch (BadPropertyValueException e) {
        logger.log(TreeLogger.ERROR, "Unknown deferred-binding property " + permutationAxis, e);
        throw new UnableToCompleteException();
      }
    }
  }

  @Override
  protected String getCssExpression(TreeLogger logger, ResourceContext context,
      JMethod method) throws UnableToCompleteException {
    CssTree cssTree = cssParsingResultMap.get(method).tree;

    String standard = printCssTree(cssTree);

    // TODO add configuration properties for swapLtrRtlInUrl, swapLeftRightInUrl and
    // shouldFlipConstantReferences booleans
    RecordingBidiFlipper recordingBidiFlipper =
        new RecordingBidiFlipper(cssTree.getMutatingVisitController(), false, false, true);
    recordingBidiFlipper.runPass();

    if (recordingBidiFlipper.nodeFlipped()) {
      String reversed = printCssTree(cssTree);
      return LocaleInfo.class.getName() + ".getCurrentLocale().isRTL() ? "
          + reversed + " : " + standard;
    } else {
      return standard;
    }
  }

  private void checkErrors() throws UnableToCompleteException {
    if (errorManager.hasErrors()) {
      throw new UnableToCompleteException();
    }
  }

  private RenamingResult doClassRenaming(CssTree cssTree, JMethod method, TreeLogger logger,
      ResourceContext context) throws UnableToCompleteException {
    Map<String, Map<String, String>> replacementsWithPrefix = computeReplacements(method, logger,
        context);

    RenamingSubstitutionMap substitutionMap = new RenamingSubstitutionMap(replacementsWithPrefix);

    new CssClassRenaming(cssTree.getMutatingVisitController(), substitutionMap, null).runPass();

    Map<String, String> mapping = replacementsWithPrefix.get("");

    mapping = Maps.newHashMap(Maps.filterKeys(mapping, Predicates.in(substitutionMap
        .getStyleClasses())));

    return new RenamingResult(mapping, substitutionMap.getExternalClassCandidates());
  }

  /**
   * When the tree is fully processed, we can now collect the external classes and revert the
   * renaming for these classes. We cannot collect the external classes during the original renaming
   * because some external at-rule could be located inside a conditional block and could be
   * removed when these blocks are evaluated.
   */
  private Set<String> revertRenamingOfExternalClasses(CssTree cssTree, RenamingResult renamingResult) {
    ExternalClassesCollector externalClassesCollector = new ExternalClassesCollector(cssTree
        .getMutatingVisitController(), errorManager);

    externalClassesCollector.runPass();

    Map<String, String> styleClassesMapping = renamingResult.mapping;

    // set containing all the style classes before the renaming.
    Set<String> allStyleClassSet = Sets.newHashSet(styleClassesMapping.keySet());
    // add the style classes that aren't associated to a method
    allStyleClassSet.addAll(renamingResult.externalClassCandidate);

    Set<String> externalClasses = externalClassesCollector.getExternalClassNames(allStyleClassSet,
        renamingResult.externalClassCandidate);

    final Map<String, String> revertMap = new HashMap<>(externalClasses.size());

    for (String external : externalClasses) {
      revertMap.put(styleClassesMapping.get(external), external);
      // override the mapping
      styleClassesMapping.put(external, external);
    }

    SubstitutionMap revertExternalClasses = new SubstitutionMap() {
      @Override
      public String get(String key) {
        return revertMap.get(key);
      }
    };

    new CssClassRenaming(cssTree.getMutatingVisitController(), revertExternalClasses, null)
        .runPass();

    return externalClasses;
  }

  private boolean isStrictResource(JMethod method) {
    NotStrict notStrict = method.getAnnotation(NotStrict.class);
    return notStrict == null;
  }

  private void finalizeTree(CssTree cssTree) throws UnableToCompleteException {
    new CheckDependencyNodes(cssTree.getMutatingVisitController(), errorManager, false).runPass();

    // Don't continue if errors exist
    checkErrors();

    new CreateStandardAtRuleNodes(cssTree.getMutatingVisitController(), errorManager).runPass();
    new CreateMixins(cssTree.getMutatingVisitController(), errorManager).runPass();
    new CreateDefinitionNodes(cssTree.getMutatingVisitController(), errorManager).runPass();
    new CreateConstantReferences(cssTree.getMutatingVisitController()).runPass();
    new CreateConditionalNodes(cssTree.getMutatingVisitController(), errorManager).runPass();
    new CreateRuntimeConditionalNodes(cssTree.getMutatingVisitController()).runPass();
    new CreateForLoopNodes(cssTree.getMutatingVisitController(), errorManager).runPass();
    new CreateComponentNodes(cssTree.getMutatingVisitController(), errorManager).runPass();
    new ValidatePropertyValues(cssTree.getVisitController(), errorManager).runPass();

    new HandleUnknownAtRuleNodes(cssTree.getMutatingVisitController(), errorManager,
        allowedAtRules, true, false).runPass();
    new ProcessKeyframes(cssTree.getMutatingVisitController(), errorManager, true, true).runPass();
    new CreateVendorPrefixedKeyframes(cssTree.getMutatingVisitController(),
            errorManager).runPass();
    new UnrollLoops(cssTree.getMutatingVisitController(), errorManager).runPass();
    new ProcessRefiners(cssTree.getMutatingVisitController(), errorManager, true).runPass();
    new MarkNonFlippableNodes(cssTree.getMutatingVisitController(), errorManager).runPass();
  }

  private ConstantDefinitions optimizeTree(CssParsingResult cssParsingResult, ResourceContext context,
      boolean simplifyCss, boolean eliminateDeadStyles, TreeLogger logger)
      throws UnableToCompleteException {
    CssTree cssTree = cssParsingResult.tree;

    // Collect mixin definitions and replace mixins
    CollectMixinDefinitions collectMixinDefinitions = new CollectMixinDefinitions(
        cssTree.getMutatingVisitController(), errorManager);
    collectMixinDefinitions.runPass();
    new ReplaceMixins(cssTree.getMutatingVisitController(), errorManager,
        collectMixinDefinitions.getDefinitions()).runPass();

    new ProcessComponents<>(cssTree.getMutatingVisitController(), errorManager).runPass();

    RuntimeConditionalBlockCollector runtimeConditionalBlockCollector = new
        RuntimeConditionalBlockCollector(cssTree.getVisitController());
    runtimeConditionalBlockCollector.runPass();

    Set<String> trueCompileTimeConditions = ImmutableSet.<String>builder()
        .addAll(getCurrentDeferredBindingProperties(context, cssParsingResult.permutationAxes,
            logger))
        .addAll(getTrueConfigurationProperties(context, cssParsingResult.trueConditions, logger))
        .build();

    new ExtendedEliminateConditionalNodes(cssTree.getMutatingVisitController(),
        trueCompileTimeConditions, runtimeConditionalBlockCollector.getRuntimeConditionalBlock())
        .runPass();

    new ValidateRuntimeConditionalNode(cssTree.getVisitController(), errorManager,
        gssOptions.isLenientConversion()).runPass();

    // Don't continue if errors exist
    checkErrors();

    CollectConstantDefinitions collectConstantDefinitionsPass = new CollectConstantDefinitions(
        cssTree);
    collectConstantDefinitionsPass.runPass();

    ReplaceConstantReferences replaceConstantReferences = new ReplaceConstantReferences(cssTree,
        collectConstantDefinitionsPass.getConstantDefinitions(), false, errorManager, false);
    replaceConstantReferences.runPass();

    new ImageSpriteCreator(cssTree.getMutatingVisitController(), context, errorManager).runPass();

    Map<String, GssFunction> gssFunctionMap = new GwtGssFunctionMapProvider(context).get();
    new ResolveCustomFunctionNodes(cssTree.getMutatingVisitController(), errorManager,
        gssFunctionMap, true, allowedNonStandardFunctions).runPass();

    // collect the final value of the constants and remove them.
    collectConstantDefinitionsPass = new CollectAndRemoveConstantDefinitions(cssTree);
    collectConstantDefinitionsPass.runPass();

    if (simplifyCss) {
      // Eliminate empty rules.
      new EliminateEmptyRulesetNodes(cssTree.getMutatingVisitController()).runPass();
      // Eliminating units for zero values.
      new EliminateUnitsFromZeroNumericValues(cssTree.getMutatingVisitController()).runPass();
      // Optimize color values.
      new ColorValueOptimizer(cssTree.getMutatingVisitController()).runPass();
      // Compress redundant top-right-bottom-left value lists.
      new AbbreviatePositionalValues(cssTree.getMutatingVisitController()).runPass();
    }

    if (eliminateDeadStyles) {
      // Report errors for duplicate declarations
      new DisallowDuplicateDeclarations(cssTree.getVisitController(), errorManager).runPass();
      // Split rules by selector and declaration.
      new SplitRulesetNodes(cssTree.getMutatingVisitController()).runPass();
      // Dead code elimination.
      new MarkRemovableRulesetNodes(cssTree).runPass();
      new EliminateUselessRulesetNodes(cssTree).runPass();
      // Merge of rules with same selector.
      new MergeAdjacentRulesetNodesWithSameSelector(cssTree).runPass();
      new EliminateUselessRulesetNodes(cssTree).runPass();
      // Merge of rules with same styles.
      new MergeAdjacentRulesetNodesWithSameDeclarations(cssTree).runPass();
      new EliminateUselessRulesetNodes(cssTree).runPass();
      new MarkNonFlippableNodes(cssTree.getMutatingVisitController(), errorManager).runPass();
    }

    return collectConstantDefinitionsPass.getConstantDefinitions();
  }

  private Set<String> getTrueConfigurationProperties(ResourceContext context,
      Set<String> configurationProperties, TreeLogger logger) throws UnableToCompleteException {
    Builder<String> setBuilder = ImmutableSet.builder();
    PropertyOracle oracle = context.getGeneratorContext().getPropertyOracle();

    for (String property : configurationProperties) {
      try {
        // TODO : only check configuration properties ?
        ConfigurationProperty confProp = oracle.getConfigurationProperty(property);

        if (!checkPropertyIsSingleValueAndBoolean(confProp, logger)) {
          throw new UnableToCompleteException();
        }

        if ("true".equals(confProp.getValues().get(0))) {
          setBuilder.add(property);
        }
      } catch (BadPropertyValueException e1) {
        logger.log(Type.ERROR, "Unknown configuration property [" + property + "]");
        throw new UnableToCompleteException();
      }
    }

    return setBuilder.build();
  }

  private Set<String> getCurrentDeferredBindingProperties(ResourceContext context,
      List<String> permutationAxes, TreeLogger logger) throws UnableToCompleteException {
    Builder<String> setBuilder = ImmutableSet.builder();
    PropertyOracle oracle = context.getGeneratorContext().getPropertyOracle();

    for (String permutationAxis : permutationAxes) {
      String propValue;
      try {
        SelectionProperty selProp = oracle.getSelectionProperty(null,
            permutationAxis);
        propValue = selProp.getCurrentValue();
      } catch (BadPropertyValueException e) {
        try {
          ConfigurationProperty confProp = oracle.getConfigurationProperty(permutationAxis);
          propValue = confProp.getValues().get(0);
        } catch (BadPropertyValueException e1) {
          logger.log(Type.ERROR, "Unknown configuration property [" + permutationAxis + "]");
          throw new UnableToCompleteException();
        }
      }

      if (propValue != null) {
        setBuilder.add(permutationAxis + ":" + propValue);
      }
    }
    return setBuilder.build();
  }

  private CssParsingResult parseResources(List<URL> resources, ResourceContext context,
      TreeLogger logger) throws UnableToCompleteException {
    List<SourceCode> sourceCodes = new ArrayList<>(resources.size());
    ImmutableMap.Builder<String, String> constantNameMappingBuilder = ImmutableMap.builder();

    // assert that we only support either gss or css on one resource.
    boolean css = ensureEitherCssOrGss(resources, logger);

    if (css && gssOptions.isAutoConversionOff()) {
      logger.log(Type.ERROR,
          "Your ClientBundle is referencing css files instead of gss. "
              + "You will need to either convert these files to gss using the "
              + "converter tool or turn on auto convertion in your gwt.xml file. "
              + "Note: Autoconversion will be removed in the next version of GWT, "
              + "you will need to move to gss."
              + "Add this line to your gwt.xml file to temporary avoid this:"
              + "<set-configuration-property name=\"CssResource.conversionMode\""
              + "    value=\"strict\" /> "
              + "Details on how to migrate to GSS can be found at: http://goo.gl/tEQnmJ");
      throw new UnableToCompleteException();
    }

    if (css) {
      String concatenatedCss = concatCssFiles(resources, logger);

      ConversionResult result = convertToGss(concatenatedCss, context, logger);

      if (shouldEmitVariables) {
        write(result.defNameMapping.keySet());
      }

      String gss = result.gss;
      String name = "[auto-converted gss files from : " + resources + "]";
      sourceCodes.add(new SourceCode(name, gss));

      constantNameMappingBuilder.putAll(result.defNameMapping);
    } else {
      for (URL stylesheet : resources) {
          sourceCodes.add(readUrlContent(stylesheet, logger));
      }
    }

    CssTree tree;

    try {
      tree = new GssParser(sourceCodes).parse();
    } catch (GssParserException e) {
      logger.log(TreeLogger.ERROR, "Unable to parse CSS", e);
      throw new UnableToCompleteException();
    }

    // create more explicit nodes
    finalizeTree(tree);

    checkErrors();

    // collect boolean conditions that have to be mapped to configuration properties
    BooleanConditionCollector booleanConditionCollector = new BooleanConditionCollector(tree
        .getMutatingVisitController());
    booleanConditionCollector.runPass();

    // collect permutations axis used in conditionals.
    PermutationsCollector permutationsCollector = new PermutationsCollector(tree
        .getMutatingVisitController());
    permutationsCollector.runPass();

    return new CssParsingResult(tree, permutationsCollector.getPermutationAxes(),
        booleanConditionCollector.getBooleanConditions(), constantNameMappingBuilder.build());
  }

  private static String extractCharset(ByteSource byteSource) throws IOException {
    String firstLine = byteSource.asCharSource(Charsets.UTF_8).readFirstLine();

    if (firstLine != null) {
      Matcher matcher = CHARSET.matcher(firstLine);

      if (matcher.matches()) {
        return matcher.group(1);
      }
    }

    return null;
  }

  private ConversionResult convertToGss(String concatenatedCss, ResourceContext context,
      TreeLogger logger) throws UnableToCompleteException {
    File tempFile = null;
    FileOutputStream fos = null;
    try {
      // We actually need a URL for the old CssResource to work. So create a temp file.
      tempFile = File.createTempFile(UUID.randomUUID() + "css_converter", "css.tmp");

      fos = new FileOutputStream(tempFile);
      IOUtils.write(concatenatedCss, fos);
      fos.close();

      ConfigurationPropertyMatcher configurationPropertyMatcher =
          new ConfigurationPropertyMatcher(context, logger);

      Css2Gss converter = new Css2Gss(tempFile.toURI().toURL(), logger,
          gssOptions.isLenientConversion(), configurationPropertyMatcher);

      String gss = converter.toGss();

      if (configurationPropertyMatcher.error) {
        throw new UnableToCompleteException();
      }

      return new ConversionResult(gss, converter.getDefNameMapping());

    } catch (Css2GssConversionException e) {
      String message = "An error occurs during the automatic conversion: " + e.getMessage();
      if (!gssOptions.isLenientConversion()) {
        message += "\n You should try to change the faulty css to fix this error. If you are " +
            "unable to change the css, you can setup the automatic conversion to be lenient. Add " +
            "the following line to your gwt.xml file: " +
            "<set-configuration-property name=\"CssResource.conversionMode\" value=\"lenient\" />";
      }
      logger.log(Type.ERROR, message, e);
      throw new UnableToCompleteException();
    } catch (IOException e) {
      logger.log(Type.ERROR, "Error while writing temporary css file", e);
      throw new UnableToCompleteException();
    } finally {
      if (tempFile != null) {
        tempFile.delete();
      }
      if (fos != null) {
        IOUtils.closeQuietly(fos);
      }
    }
  }

  public static String concatCssFiles(List<URL> resources, TreeLogger logger)
      throws UnableToCompleteException {
    StringBuffer buffer = new StringBuffer();
    for (URL stylesheet : resources) {
      try {
        String fileContent = Resources.asByteSource(stylesheet).asCharSource(Charsets.UTF_8)
            .read();
        buffer.append(fileContent);
        buffer.append("\n");

      } catch (IOException e) {
        logger.log(TreeLogger.ERROR, "Unable to parse CSS", e);
        throw new UnableToCompleteException();
      }
    }
    return buffer.toString();
  }

  private boolean ensureEitherCssOrGss(List<URL> resources, TreeLogger logger)
      throws UnableToCompleteException {
    boolean css = resources.get(0).toString().endsWith(".css");
    for (URL stylesheet : resources) {
      if (css && !stylesheet.toString().endsWith(".css")) {
        logger.log(Type.ERROR,
            "Only either css files or gss files are supported on one interface");
        throw new UnableToCompleteException();
      } else if (!css && !stylesheet.toString().endsWith(".gss")) {
        logger.log(Type.ERROR,
            "Only either css files or gss files are supported on one interface");
        throw new UnableToCompleteException();
      }
    }
    return css;
  }

  private String printCssTree(CssTree tree) {
    CssPrinter cssPrinterPass = new CssPrinter(tree);
    cssPrinterPass.runPass();

    return cssPrinterPass.getCompactPrintedString();
  }

  private boolean writeClassMethod(TreeLogger logger, JMethod userMethod,
      Map<String, String> substitutionMap, SourceWriter sw) throws
      UnableToCompleteException {

    if (userMethod.getParameters().length > 0) {
      logger.log(Type.ERROR, "The method [" + userMethod.getName() + "] shouldn't contain any " +
          "parameters");
      throw new UnableToCompleteException();
    }

    String name = getClassName(userMethod);

    String value = substitutionMap.get(name);

    if (value == null) {
      logger.log(Type.ERROR, "The following style class [" + name + "] is missing from the source" +
          " CSS file");
      return false;
    } else {
      writeSimpleGetter(userMethod, "\"" + value + "\"", sw);
    }

    return true;
  }

  private String getClassName(JMethod method) {
    String name = method.getName();

    ClassName classNameOverride = method.getAnnotation(ClassName.class);
    if (classNameOverride != null) {
      name = classNameOverride.value();
    }
    return name;
  }

  private boolean writeDefMethod(CssDefinitionNode definitionNode, TreeLogger logger,
      JMethod userMethod, SourceWriter sw) throws UnableToCompleteException {

    String name = userMethod.getName();

    JClassType classReturnType = userMethod.getReturnType().isClass();
    List<CssValueNode> params = definitionNode.getParameters();

    if (params.size() != 1 && !isReturnTypeString(classReturnType)) {
      logger.log(TreeLogger.ERROR, "@def rule " + name
          + " must define exactly one value or return type must be String");
      return false;
    }

    String returnExpr;
    if (isReturnTypeString(classReturnType)) {
      List<String> returnValues = new ArrayList<String>();
      for (CssValueNode valueNode : params) {
        returnValues.add(Generator.escape(valueNode.toString()));
      }
      returnExpr = "\"" + Joiner.on(" ").join(returnValues) + "\"";
    } else {
      JPrimitiveType returnType = userMethod.getReturnType().isPrimitive();
      if (returnType == null) {
        logger.log(TreeLogger.ERROR, name + ": Return type must be primitive type " +
            "or String for @def accessors");
        return false;
      }
      CssValueNode valueNode = params.get(0);

      // when a constant refers to another constant, closure-stylesheet wrap the CssNumericNode in
      // a CssCompositeValueNode. Unwrap it.
      if (valueNode instanceof CssCompositeValueNode) {
        CssCompositeValueNode toUnwrap = (CssCompositeValueNode) valueNode;
        if (toUnwrap.getValues().size() == 1) {
          valueNode = toUnwrap.getValues().get(0);
        }
      }

      if (!(valueNode instanceof CssNumericNode)) {
        logger.log(TreeLogger.ERROR, "The value of the constant defined by @" + name + " is not a" +
            " numeric");
        return false;
      }
      String numericValue = ((CssNumericNode) valueNode).getNumericPart();

      if (returnType == JPrimitiveType.INT || returnType == JPrimitiveType.LONG) {
        returnExpr = "" + Long.parseLong(numericValue);
      } else if (returnType == JPrimitiveType.FLOAT) {
        returnExpr = numericValue + "F";
      } else if (returnType == JPrimitiveType.DOUBLE) {
        returnExpr = "" + numericValue;
      } else {
        logger.log(TreeLogger.ERROR, returnType.getQualifiedSourceName()
            + " is not a valid primitive return type for @def accessors");
        return false;
      }
    }

    writeSimpleGetter(userMethod, returnExpr, sw);

    return true;
  }

  private Map<JMethod, String> writeMethods(TreeLogger logger, ResourceContext context, JMethod method,
      SourceWriter sw, ConstantDefinitions constantDefinitions,
      Map<String, String> originalConstantNameMapping, Map<String, String> substitutionMap)
      throws UnableToCompleteException {
    JClassType gssResource = method.getReturnType().isInterface();

    boolean success = true;

    Map<JMethod, String> methodToClassName = new LinkedHashMap<>();

    for (JMethod toImplement : gssResource.getOverridableMethods()) {
      if (toImplement == getTextMethod) {
        writeGetText(logger, context, method, sw);
      } else if (toImplement == ensuredInjectedMethod) {
        writeEnsureInjected(sw);
      } else if (toImplement == getNameMethod) {
        writeGetName(method, sw);
      } else {
        success &= writeUserMethod(logger, toImplement, sw, constantDefinitions,
            originalConstantNameMapping, substitutionMap, methodToClassName);
      }
    }

    if (!success) {
      throw new UnableToCompleteException();
    }

    return methodToClassName;
  }

  private boolean writeUserMethod(TreeLogger logger, JMethod userMethod, SourceWriter sw,
      ConstantDefinitions constantDefinitions, Map<String, String> originalConstantNameMapping,
      Map<String, String> substitutionMap, Map<JMethod, String> methodToClassName)
      throws UnableToCompleteException {

    String className = getClassName(userMethod);

    // method to access style class ?
    if (substitutionMap.containsKey(className) &&
        isReturnTypeString(userMethod.getReturnType().isClass())) {
      methodToClassName.put(userMethod, substitutionMap.get(className));
      return writeClassMethod(logger, userMethod, substitutionMap, sw);
    }

    // method to access constant value ?
    CssDefinitionNode definitionNode;
    String methodName = userMethod.getName();

    if (originalConstantNameMapping.containsKey(methodName)) {
      // method name maps a constant that has been renamed during the auto conversion
      String constantName = originalConstantNameMapping.get(methodName);
      definitionNode = constantDefinitions.getConstantDefinition(constantName);
    } else {
      definitionNode = constantDefinitions.getConstantDefinition(methodName);

      if (definitionNode == null) {
        // try with upper case
        definitionNode = constantDefinitions.getConstantDefinition(toUpperCase(methodName));
      }
    }

    if (definitionNode != null) {
      return writeDefMethod(definitionNode, logger, userMethod, sw);
    }

    if (substitutionMap.containsKey(className)) {
      // method matched a class name but not a constant and the return type is not a string
      logger.log(Type.ERROR, "The return type of the method [" + userMethod.getName() + "] must " +
          "be java.lang.String.");
      throw new UnableToCompleteException();
    }

    // the method doesn't match a style class nor a constant
    logger.log(Type.ERROR,
        "The following method [" + userMethod.getName() + "()] doesn't match a constant" +
            " nor a style class. You could fix that by adding ." + className + " {}"
    );

    return false;
  }

  /**
   * Transform a camel case string to upper case. Each word is separated by a '_'
   *
   * @param camelCase
   */
  private String toUpperCase(String camelCase) {
    return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, camelCase);
  }

  private Map<String, Map<String, String>> computeReplacements(JMethod method, TreeLogger logger,
      ResourceContext context) throws UnableToCompleteException {
    Map<String, Map<String, String>> replacementsWithPrefix = new HashMap<String, Map<String,
        String>>();

    replacementsWithPrefix
        .put("", computeReplacementsForType(method.getReturnType().isInterface()));

    // Process the Import annotation if any
    Import imp = method.getAnnotation(Import.class);

    if (imp != null) {
      boolean fail = false;
      TypeOracle typeOracle = context.getGeneratorContext().getTypeOracle();

      for (Class<? extends CssResource> clazz : imp.value()) {
        JClassType importType = typeOracle.findType(clazz.getName().replace('$', '.'));
        assert importType != null : "TypeOracle does not have type " + clazz.getName();

        // add this import type as a requirement for this generator
        context.getRequirements().addTypeHierarchy(importType);

        String prefix = getImportPrefix(importType);

        if (replacementsWithPrefix.put(prefix, computeReplacementsForType(importType)) != null) {
          logger.log(TreeLogger.ERROR, "Multiple imports that would use the prefix " + prefix);
          fail = true;
        }
      }

      if (fail) {
        throw new UnableToCompleteException();
      }
    }

    return replacementsWithPrefix;
  }

  private Map<String, String> computeReplacementsForType(JClassType cssResource) {
    Map<String, String> replacements = replacementsByClassAndMethod.get(cssResource);

    if (replacements == null) {
      replacements = new HashMap<String, String>();
      replacementsByClassAndMethod.put(cssResource, replacements);

      String resourcePrefix = resourcePrefixBuilder.get(cssResource.getQualifiedSourceName());

      // This substitution map will prefix each renamed class with the resource prefix and use a
      // MinimalSubstitutionMap for computing the obfuscated name.
      SubstitutionMap prefixingSubstitutionMap = new PrefixingSubstitutionMap(
          new MinimalSubstitutionMap(), obfuscationPrefix + resourcePrefix + "-");

      for (JMethod method : cssResource.getOverridableMethods()) {
        if (method == getNameMethod || method == getTextMethod || method == ensuredInjectedMethod) {
          continue;
        }

        String styleClass = getClassName(method);

        if (replacementsForSharedMethods.containsKey(method)) {
          replacements.put(styleClass, replacementsForSharedMethods.get(method));
        } else {
          String obfuscatedClassName = prefixingSubstitutionMap.get(styleClass);
          String replacement = obfuscationStyle.getPrettyName(styleClass, cssResource,
              obfuscatedClassName);

          if (hasSharedAnnotation(method)) {
            // We always use the base type for obfuscation if this is a shared method
            replacement = obfuscationStyle.getPrettyName(styleClass, method.getEnclosingType(),
                obfuscatedClassName);
            replacementsForSharedMethods.put(method, replacement);
          }
          replacements.put(styleClass, replacement);
        }
      }
    }

    return replacements;
  }

  private boolean hasSharedAnnotation(JMethod method) {
    JClassType enclosingType = method.getEnclosingType();
    Shared shared = enclosingType.getAnnotation(Shared.class);
    return shared != null;
  }
}
