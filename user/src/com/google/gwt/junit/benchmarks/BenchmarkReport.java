/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.junit.benchmarks;

import com.google.gwt.core.ext.typeinfo.HasMetaData;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.junit.rebind.BenchmarkGenerator;
import com.google.gwt.junit.client.TestResults;
import com.google.gwt.junit.client.Trial;

import junit.framework.TestCase;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.IProblemFactory;
import org.eclipse.jdt.internal.compiler.SourceElementParser;
import org.eclipse.jdt.internal.compiler.ISourceElementRequestor;
import org.eclipse.jdt.internal.compiler.SourceElementRequestorAdapter;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblemFactory;
import org.eclipse.jdt.internal.compiler.batch.CompilationUnit;

import java.io.IOException;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.text.DateFormat;
import java.text.BreakIterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;

/**
 * Generates a detailed report that contains the results of all of the
 * benchmark-related unit tests executed during a unit test session. The primary
 * user of this class is JUnitShell.
 *
 * The report is in XML format. To view the XML reports, use benchmarkViewer.
 *
 */
public class BenchmarkReport {

  /**
   * Converts a set of test results for a single benchmark method into XML.
   */
  private class BenchmarkXml {

    private MetaData metaData;

    private List/*<JUnitMessageQueue.TestResult>*/ results;

    private TestCase test;

    BenchmarkXml(TestCase test,
        List/*<JUnitMessageQueue.TestResult>*/ results) {
      this.test = test;
      this.results = results;
      Map/*<String,MetaData>*/ methodMetaData
          = (Map/*<String,MetaData>*/) testMetaData
          .get(test.getClass().toString());
      metaData = (MetaData) methodMetaData.get(test.getName());
    }

    Element toElement(Document doc) {
      Element benchmark = doc.createElement("benchmark");
      benchmark.setAttribute("class", test.getClass().getName());
      benchmark.setAttribute("name", metaData.getTestName());
      benchmark.setAttribute("description", metaData.getTestDescription());

      String sourceCode = metaData.getSourceCode();
      if (sourceCode != null) {
        Element sourceCodeElement = doc.createElement("source_code");
        sourceCodeElement.appendChild(doc.createTextNode(sourceCode));
        benchmark.appendChild(sourceCodeElement);
      }

      // TODO(tobyr): create target_code element

      for (Iterator it = results.iterator(); it.hasNext();) {
        TestResults result = (TestResults) it.next();
        benchmark.appendChild(toElement(doc, result));
      }

      return benchmark;
    }

    private Element toElement(Document doc, TestResults result) {
      Element resultElement = doc.createElement("result");
      resultElement.setAttribute("host", result.getHost());
      resultElement.setAttribute("agent", result.getAgent());

      List trials = result.getTrials();

      for (Iterator it = trials.iterator(); it.hasNext();) {
        Trial trial = (Trial) it.next();
        Element trialElement = toElement(doc, trial);
        resultElement.appendChild(trialElement);
      }

      return resultElement;
    }

    private Element toElement(Document doc, Trial trial) {
      Element trialElement = doc.createElement("trial");

      Map variables = trial.getVariables();

      for (Iterator it = variables.entrySet().iterator(); it.hasNext();) {
        Map.Entry entry = (Map.Entry) it.next();
        Object name = entry.getKey();
        Object value = entry.getValue();
        Element variableElement = doc.createElement("variable");
        variableElement.setAttribute("name", name.toString());
        variableElement.setAttribute("value", value.toString());
        trialElement.appendChild(variableElement);
      }

      trialElement
          .setAttribute("timing", String.valueOf(trial.getRunTimeMillis()));

      Throwable exception = trial.getException();

      if (exception != null) {
        Element exceptionElement = doc.createElement("exception");
        exceptionElement.appendChild(doc.createTextNode(exception.toString()));
        trialElement.appendChild(exceptionElement);
      }

      return trialElement;
    }
  }

  /**
   * Parses a .java source file to get the source code for methods.
   *
   * This Parser takes some shortcuts based on the fact that it's only being
   * used to locate test methods for unit tests. (For example, only requiring a
   * method name instead of a full type signature for lookup).
   *
   * TODO(tobyr) I think that I might be able to replace all this code with a
   * call to the existing metadata interface. Check declEnd/declStart in
   * JAbstractMethod.
   */
  private static class Parser {

    static class MethodBody {

      int declarationEnd;   // the character index of the end of the method

      int declarationStart; // the character index of the start of the method

      String source;
    }

    private MethodBody currentMethod; // Only used during the visitor

    // But it's less painful
    private Map/*<String,MethodBody>*/ methods
        = new HashMap/*<String,MethodBody>*/();

    // Contains the contents of the entire source file
    private char[] sourceContents;

    Parser(JClassType klass) throws IOException {

      Map settings = new HashMap();
      settings.put(CompilerOptions.OPTION_Source, CompilerOptions.VERSION_1_4);
      settings.put(CompilerOptions.OPTION_TargetPlatform,
          CompilerOptions.VERSION_1_4);
      settings.put(CompilerOptions.OPTION_DocCommentSupport,
          CompilerOptions.ENABLED);
      CompilerOptions options = new CompilerOptions(settings);

      IProblemFactory problemFactory = new DefaultProblemFactory(
          Locale.getDefault());

      // Save off the bounds of any method that is a test method
      ISourceElementRequestor requestor = new SourceElementRequestorAdapter() {
        public void enterMethod(MethodInfo methodInfo) {
          String name = new String(methodInfo.name);
          if (name.startsWith("test")) {
            currentMethod = new MethodBody();
            currentMethod.declarationStart = methodInfo.declarationStart;
            methods.put(name, currentMethod);
          }
        }

        public void exitMethod(int declarationEnd, int defaultValueStart,
            int defaultValueEnd) {
          if (currentMethod != null) {
            currentMethod.declarationEnd = declarationEnd;
            currentMethod = null;
          }
        }
      };

      boolean reportLocalDeclarations = true;
      boolean optimizeStringLiterals = true;

      SourceElementParser parser = new SourceElementParser(requestor,
          problemFactory, options, reportLocalDeclarations,
          optimizeStringLiterals);

      File sourceFile = findSourceFile(klass);
      sourceContents = read(sourceFile);
      CompilationUnit unit = new CompilationUnit(sourceContents,
          sourceFile.getName(), null);

      parser.parseCompilationUnit(unit, true);
    }

    /**
     * Returns the source code for the method of the given name.
     *
     * @return null if the source code for the method can not be located
     */
    public String getMethod(JMethod method) {
      /*
      MethodBody methodBody = (MethodBody)methods.get( method.getName() );
      if ( methodBody == null ) {
        return null;
      }
      if ( methodBody.source == null ) {
        methodBody.source = new String(sourceContents,
        methodBody.declarationStart, methodBody.declarationEnd - methodBody.
        declarationStart + 1);
      }
      return methodBody.source;
      */
      return new String(sourceContents, method.getDeclStart(),
          method.getDeclEnd() - method.getDeclStart() + 1);
    }
  }

  /**
   * Converts an entire report into XML.
   */
  private class ReportXml {

    private Map/*<String,Element>*/ categoryElementMap
        = new HashMap/*<String,Element>*/();

    private Date date = new Date();

    private String version = "unknown";

    /**
     * Locates or creates the category element by the specified name.
     *
     * @param doc The document to search
     * @return The matching category element
     */
    private Element getCategoryElement(Document doc, Element report,
        String name) {
      Element e = (Element) categoryElementMap.get(name);

      if (e != null) {
        return e;
      }

      Element categoryElement = doc.createElement("category");
      categoryElementMap.put(name, categoryElement);
      CategoryImpl category = (CategoryImpl) testCategories.get(name);
      categoryElement.setAttribute("name", category.getName());
      categoryElement.setAttribute("description", category.getDescription());

      report.appendChild(categoryElement);

      return categoryElement;
    }

    Element toElement(Document doc) {
      Element report = doc.createElement("gwt_benchmark_report");
      String dateString = DateFormat.getDateTimeInstance().format(date);
      report.setAttribute("date", dateString);
      report.setAttribute("gwt_version", version);

      // - Add each test result into the report.
      // - Add the category for the test result, if necessary.
      for (Iterator entryIt = testResults.entrySet().iterator();
          entryIt.hasNext();) {
        Map.Entry entry = (Map.Entry) entryIt.next();
        TestCase test = (TestCase) entry.getKey();
        List/*<JUnitMessageQueue.TestResult>*/ results
            = (List/*<JUnitMessageQueue.TestResult>*/) entry.getValue();
        BenchmarkXml xml = new BenchmarkXml(test, results);
        Element categoryElement = getCategoryElement(doc, report,
            xml.metaData.getCategory().getClassName());
        categoryElement.appendChild(xml.toElement(doc));
      }

      return report;
    }
  }

  private static final String GWT_BENCHMARK_CATEGORY = "gwt.benchmark.category";

  private static final String GWT_BENCHMARK_DESCRIPTION
      = "gwt.benchmark.description";

  private static final String GWT_BENCHMARK_NAME = "gwt.benchmark.name";

  private static File findSourceFile(JClassType klass) {
    final char separator = File.separator.charAt(0);
    String filePath = klass.getPackage().getName().replace('.', separator) +
        separator + klass.getSimpleSourceName() + ".java";
    String[] paths = getClassPath();

    for (int i = 0; i < paths.length; ++i) {
      File maybeSourceFile = new File(paths[i] + separator + filePath);

      if (maybeSourceFile.exists()) {
        return maybeSourceFile;
      }
    }

    return null;
  }

  private static String[] getClassPath() {
    String path = System.getProperty("java.class.path");
    return path.split(File.pathSeparator);
  }

  private static String getSimpleMetaData(HasMetaData hasMetaData,
      String name) {
    String[][] allValues = hasMetaData.getMetaData(name);

    if (allValues == null) {
      return null;
    }

    StringBuffer result = new StringBuffer();

    for (int i = 0; i < allValues.length; ++i) {
      String[] values = allValues[i];
      for (int j = 0; j < values.length; ++j) {
        result.append(values[j]);
        result.append(" ");
      }
    }

    String resultString = result.toString().trim();
    return resultString.equals("") ? null : resultString;
  }

  private static char[] read(File f) throws IOException {
    // TODO(tobyr) Can be done oh so much faster by just reading directly into
    // a char[]

    BufferedReader reader = new BufferedReader(new FileReader(f));
    StringBuffer source = new StringBuffer((int) f.length());

    while (true) {
      String line = reader.readLine();
      if (line == null) {
        break;
      }
      source.append(line);
      source.append("\n");
    }

    char[] buf = new char[ source.length() ];
    source.getChars(0, buf.length, buf, 0);

    return buf;
  }

  private Map/*<String,Map<CategoryImpl>*/ testCategories
      = new HashMap/*<String,CategoryImpl>*/();

  private Map/*<String,Map<String,MetaData>>*/ testMetaData
      = new HashMap/*<String,Map<String,MetaData>>*/();

  private Map/*<TestCase,List<JUnitMessageQueue.TestResult>>*/ testResults
      = new HashMap/*<TestCase,JUnitMessageQueue.List<TestResult>>*/();

  private TypeOracle typeOracle;

  private TreeLogger logger;

  public BenchmarkReport( TreeLogger logger ) {
    this.logger = logger;
  }

  /**
   * Adds the Benchmark to the report. All of the metadata about the benchmark
   * (category, name, description, etc...) is recorded from the TypeOracle.
   */
  public void addBenchmark(JClassType benchmarkClass, TypeOracle typeOracle) {

    this.typeOracle = typeOracle;
    String categoryType = getSimpleMetaData(benchmarkClass,
        GWT_BENCHMARK_CATEGORY);

    Map zeroArgMethods = BenchmarkGenerator
        .getNotOverloadedTestMethods(benchmarkClass);
    Map/*<String,JMethod>*/ parameterizedMethods = BenchmarkGenerator
        .getParameterizedTestMethods(benchmarkClass, TreeLogger.NULL);
    List/*<JMethod>*/ testMethods = new ArrayList/*<JMethod>*/(
        zeroArgMethods.size() + parameterizedMethods.size());
    testMethods.addAll(zeroArgMethods.values());
    testMethods.addAll(parameterizedMethods.values());

    Map/*<String,MetaData>*/ metaDataMap
        = (Map/*<String,MetaData>*/) testMetaData
        .get(benchmarkClass.toString());
    if (metaDataMap == null) {
      metaDataMap = new HashMap/*<String,MetaData>*/();
      testMetaData.put(benchmarkClass.toString(), metaDataMap);
    }

    Parser parser = null;

    try {
      parser = new Parser(benchmarkClass);
    } catch (IOException e) {
      // if we fail to create the parser for some reason, we'll have to just
      // deal with a null parser.
      logger.log(TreeLogger.WARN,
          "Unable to parse the code for " + benchmarkClass, e);
    }

    // Add all of the benchmark methods
    for (int i = 0; i < testMethods.size(); ++i) {
      JMethod method = (JMethod) testMethods.get(i);
      String methodName = method.getName();
      String methodCategoryType = getSimpleMetaData(method,
          GWT_BENCHMARK_CATEGORY);
      if (methodCategoryType == null) {
        methodCategoryType = categoryType;
      }
      CategoryImpl methodCategory = getCategory(methodCategoryType);
      String sourceCode = parser == null ? null : parser.getMethod(method);
      StringBuffer summary = new StringBuffer();
      StringBuffer comment = new StringBuffer();
      getComment(sourceCode, summary, comment);

      MetaData metaData = new MetaData(benchmarkClass.toString(), methodName,
          sourceCode, methodCategory, methodName, summary.toString());
      metaDataMap.put(methodName, metaData);
    }
  }

  public void addBenchmarkResults(TestCase test, TestResults results) {
    List/*<TestResults>*/ currentResults = (List/*<TestResults>*/) testResults
        .get(test);
    if (currentResults == null) {
      currentResults = new ArrayList/*<TestResults>*/();
      testResults.put(test, currentResults);
    }
    currentResults.add(results);
  }

  /**
   * Generates reports for all of the benchmarks which were added to the
   * generator.
   *
   * @param outputPath The path to write the reports to.
   * @throws IOException If anything goes wrong writing to outputPath
   */
  public void generate(String outputPath)
      throws IOException, TransformerException, ParserConfigurationException {

    // Don't generate a new report if no tests were actually run.
    if (testResults.size() == 0) {
      return;
    }

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();

    Document doc = builder.newDocument();
    doc.appendChild(new ReportXml().toElement(doc));

    // TODO(tobyr) Looks like indenting is busted
    // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6296446
    // Not a big deal, since we don't intend to read the XML by hand anyway
    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    // Think this can be used with JDK 1.5
    // transformerFactory.setAttribute( "indent-number", new Integer(2) );
    Transformer serializer = transformerFactory.newTransformer();
    serializer.setOutputProperty(OutputKeys.METHOD, "xml");
    serializer.setOutputProperty(OutputKeys.INDENT, "yes");
    serializer
        .setOutputProperty("{ http://xml.apache.org/xslt }indent-amount", "2");
    BufferedOutputStream docOut = new BufferedOutputStream(
        new FileOutputStream(outputPath));
    serializer.transform(new DOMSource(doc), new StreamResult(docOut));
    docOut.close();
  }

  private CategoryImpl getCategory(String name) {
    CategoryImpl c = (CategoryImpl) testCategories.get(name);

    if (c != null) {
      return c;
    }

    String categoryName = "";
    String categoryDescription = "";

    if (name != null) {
      JClassType categoryType = typeOracle.findType(name);

      if (categoryType != null) {
        categoryName = getSimpleMetaData(categoryType, GWT_BENCHMARK_NAME);
        categoryDescription = getSimpleMetaData(categoryType,
            GWT_BENCHMARK_DESCRIPTION);
      }
    }

    c = new CategoryImpl(name, categoryName, categoryDescription);
    testCategories.put(name, c);
    return c;
  }

  /**
   * Parses out the JavaDoc comment from a string of source code. Returns the
   * first sentence summary in <code>summary</code> and the body of the entire
   * comment (including the summary) in <code>comment</code>.
   */
  private void getComment(String sourceCode, StringBuffer summary,
      StringBuffer comment) {

    if (sourceCode == null) {
      return;
    }

    summary.setLength(0);
    comment.setLength(0);

    String regex = "/\\*\\*(.(?!}-\\*/))*\\*/";

    Pattern p = Pattern.compile(regex, Pattern.DOTALL);
    Matcher m = p.matcher(sourceCode);

    if (! m.find()) {
      return;
    }

    String commentStr = m.group();

    p = Pattern.compile(
        "(/\\*\\*\\s*)" +  // The comment header
        "(((\\s*\\**\\s*)([^\n\r]*)[\n\r]+)*)" // The comment body
    );

    m = p.matcher(commentStr);

    if (! m.find()) {
      return;
    }

    String stripped = m.group(2);

    p = Pattern.compile("^\\p{Blank}*\\**\\p{Blank}*", Pattern.MULTILINE);
    String bareComment = p.matcher(stripped).replaceAll("");

    BreakIterator iterator = BreakIterator.getSentenceInstance();
    iterator.setText(bareComment);
    int firstSentenceEnd = iterator.next();
    if (firstSentenceEnd == BreakIterator.DONE) {
      summary.append(bareComment);
    } else {
      summary.append(bareComment.substring(0, firstSentenceEnd));
    }

    comment.append(bareComment);
  }
}