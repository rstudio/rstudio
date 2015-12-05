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
package com.google.gwt.resources.css;

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.CssResource.ClassName;
import com.google.gwt.resources.gss.ClassNamesCollector;
import com.google.gwt.resources.rg.GssResourceGenerator;
import com.google.gwt.resources.rg.GssResourceGenerator.LoggerErrorManager;
import com.google.gwt.thirdparty.common.css.compiler.ast.CssTree;
import com.google.gwt.thirdparty.common.css.compiler.ast.GssParser;
import com.google.gwt.thirdparty.common.css.compiler.ast.GssParserException;
import com.google.gwt.thirdparty.common.css.compiler.passes.CollectConstantDefinitions;
import com.google.gwt.thirdparty.common.css.compiler.passes.CreateDefinitionNodes;
import com.google.gwt.thirdparty.common.css.compiler.passes.CreateForLoopNodes;
import com.google.gwt.thirdparty.common.css.compiler.passes.UnrollLoops;
import com.google.gwt.thirdparty.guava.common.base.CaseFormat;
import com.google.gwt.thirdparty.guava.common.base.Function;
import com.google.gwt.thirdparty.guava.common.collect.Iterables;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.user.rebind.SourceWriter;
import com.google.gwt.user.rebind.StringSourceWriter;
import com.google.gwt.util.tools.ArgHandlerFile;
import com.google.gwt.util.tools.ArgHandlerFlag;
import com.google.gwt.util.tools.ArgHandlerString;
import com.google.gwt.util.tools.ToolBase;

import java.io.File;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * A utility class for creating a Java interface declaration for a given CSS
 * file.
 */
public class InterfaceGenerator extends ToolBase {

  private static final Comparator<String> NAME_COMPARATOR = new Comparator<String>() {
    public int compare(String o1, String o2) {
      return o1.compareToIgnoreCase(o2);
    }
  };

  private static final TreeLogger.Type LOG_LEVEL = TreeLogger.WARN;

  public static void main(String[] args) {
    (new InterfaceGenerator()).execImpl(args);
  }

  private String interfaceName;
  private File inputFile;
  private TreeLogger logger;
  private boolean standaloneFile;

  private class ArgHandlerAddPackageHeader extends ArgHandlerFlag {

    public ArgHandlerAddPackageHeader() {
      addTagValue("-standalone", true);
    }

    @Override
    public String getPurposeSnippet() {
      return "Add package and import statements to generated interface so that "
          + "they are still functional when they stand alone.";
    }

    @Override
    public String getLabel() {
      return "addPackageHeader";
    }

    @Override
    public boolean setFlag(boolean value) {
      standaloneFile = value;
      logger.log(TreeLogger.DEBUG, value ? "Not creating" : "Creating" + " a standalone file");
      return true;
    }

    @Override
    public boolean getDefaultValue() {
      return standaloneFile;
    }
  }

  private InterfaceGenerator() {
    registerHandler(new ArgHandlerAddPackageHeader());
    registerHandler(new ArgHandlerString() {

      @Override
      public String getPurpose() {
        return "The name of the generated CssResource subtype";
      }

      @Override
      public String getTag() {
        return "-typeName";
      }

      @Override
      public String[] getTagArgs() {
        return new String[] {"some.package.MyCssResource"};
      }

      @Override
      public boolean isRequired() {
        return true;
      }

      @Override
      public boolean setString(String str) {
        if (str.length() == 0) {
          return false;
        }
        if (!Character.isJavaIdentifierStart(str.charAt(0))) {
          return false;
        }
        for (int i = 1, j = str.length(); i < j; i++) {
          char c = str.charAt(i);
          if (!(Character.isJavaIdentifierPart(c) || c == '.')) {
            return false;
          }
        }
        interfaceName = str;
        if (logger.isLoggable(TreeLogger.DEBUG)) {
          logger.log(TreeLogger.DEBUG, "interfaceName = " + interfaceName);
        }
        return true;
      }
    });

    // -css in.css
    registerHandler(new ArgHandlerFile() {

      @Override
      public String getPurpose() {
        return "The input CSS file to process";
      }

      @Override
      public String getTag() {
        return "-css";
      }

      @Override
      public boolean isRequired() {
        return true;
      }

      @Override
      public void setFile(File file) {
        inputFile = file;
        if (logger.isLoggable(TreeLogger.DEBUG)) {
          logger.log(TreeLogger.DEBUG, "inputFile = " + file.getAbsolutePath());
        }
      }
    });
  }

  @Override
  protected String getDescription() {
    return "Create a CssResource interface based on a CSS file";
  }

  private void execImpl(String[] args) {
    // Set up logging
    PrintWriter logWriter = new PrintWriter(System.err);
    logger = new PrintWriterTreeLogger(logWriter);
    ((PrintWriterTreeLogger) logger).setMaxDetail(LOG_LEVEL);

    // Process args or die
    if (!processArgs(args)) {
      System.exit(-1);
    }

    boolean error = false;
    try {
      System.out.println(process());
    } catch (MalformedURLException e) {
      logger.log(TreeLogger.ERROR, "Unable to load CSS", e);
      error = true;
    } catch (UnableToCompleteException e) {
      logger.log(TreeLogger.ERROR, "Unable to process CSS", e);
      error = true;
    } finally {
      // Make sure the logs are emitted
      logWriter.flush();
    }

    System.exit(error ? -1 : 0);
  }

  /**
   * Munge a CSS class name into a Java identifier.
   */
  private String methodName(String className) {
    StringBuilder sb = new StringBuilder();
    char c = className.charAt(0);
    boolean nextUpCase = false;

    if (Character.isJavaIdentifierStart(c)) {
      sb.append(Character.toLowerCase(c));
    }

    for (int i = 1, j = className.length(); i < j; i++) {
      c = className.charAt(i);
      if (!Character.isJavaIdentifierPart(c)) {
        nextUpCase = true;
        continue;
      }

      if (nextUpCase) {
        nextUpCase = false;
        c = Character.toUpperCase(c);
      }
      sb.append(c);
    }
    return sb.toString();
  }

  private String process() throws MalformedURLException,
      UnableToCompleteException {
    // Create AST
    CssTree cssTree = createAst(inputFile.toURI().toURL(), logger);

    // Sort all names
    Set<String> names = new TreeSet<String>(NAME_COMPARATOR);

    names.addAll(new ClassNamesCollector().getClassNames(cssTree));

    CollectConstantDefinitions collectConstantDefinitionsPass = new CollectConstantDefinitions(
            cssTree);
    collectConstantDefinitionsPass.runPass();
    Collection<String> renamedDefs = renameDefs(collectConstantDefinitionsPass
            .getConstantDefinitions().getConstantsNames());
    names.addAll(renamedDefs);

    // Deduplicate method names
    Set<String> methodNames = new HashSet<String>();

    // Build the interface
    SourceWriter sw = new StringSourceWriter();

    int lastDot = interfaceName.lastIndexOf('.');
    if (standaloneFile) {
      sw.println("// DO NOT EDIT");
      sw.println("// Automatically generated by "
          + InterfaceGenerator.class.getName());
      sw.println("package " + interfaceName.substring(0, lastDot) + ";");
      sw.println("import " + CssResource.class.getCanonicalName() + ";");
      sw.println("import " + ClassName.class.getCanonicalName() + ";");
    }

    sw.println("interface " + interfaceName.substring(lastDot + 1)
        + " extends CssResource {");
    sw.indent();
    for (String className : names) {
      String methodName = methodName(className);

      while (!methodNames.add(methodName)) {
        // Unusual, handles foo-bar and foo--bar
        methodName += "_";
      }

      sw.println();
      if (!methodName.equals(className)) {
        sw.println("@ClassName(\"" + Generator.escape(className) + "\")");
      }
      sw.println("String " + methodName + "();");
    }
    sw.outdent();
    sw.println("}");

    return sw.toString();
  }

  /**
   * In GSS, constant names are defined in upper case but a method name to access a constant in
   * a CssResource interface can be written in lower camel case.
   * <p>
   * This method converts all constant names in a lower camel case identifier.
   */
  private Collection<String> renameDefs(Iterable<String> constantsNames) {
    return Lists.newArrayList(Iterables.transform(constantsNames, new Function<String, String>() {
      @Override
      public String apply(String constantName) {
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, constantName);
      }
    }));
  }

  private CssTree createAst(URL sourceFileUrl, TreeLogger logger)
          throws UnableToCompleteException {
    LoggerErrorManager errorManager = new LoggerErrorManager(logger);

    CssTree cssTree;

    try {
      cssTree = new GssParser(GssResourceGenerator.readUrlContent(sourceFileUrl, logger)).parse();
    } catch (GssParserException e) {
      logger.log(TreeLogger.ERROR, "Unable to parse CSS", e);
      throw new UnableToCompleteException();
    }

    new CreateDefinitionNodes(cssTree.getMutatingVisitController(), errorManager).runPass();
    // Can create new style classes
    new CreateForLoopNodes(cssTree.getMutatingVisitController(), errorManager).runPass();
    new UnrollLoops(cssTree.getMutatingVisitController(), errorManager).runPass();

    return cssTree;
  }
}
