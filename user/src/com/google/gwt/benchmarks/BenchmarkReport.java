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
package com.google.gwt.benchmarks;

import com.google.gwt.benchmarks.client.impl.BenchmarkResults;
import com.google.gwt.benchmarks.client.impl.Trial;
import com.google.gwt.benchmarks.rebind.BenchmarkGenerator;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.HasAnnotations;
import com.google.gwt.core.ext.typeinfo.HasMetaData;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.util.Util;
import com.google.gwt.util.tools.Utility;

import junit.framework.TestCase;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.BreakIterator;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Generates a detailed report that contains the results of all of the
 * benchmark-related unit tests executed during a unit test session. The primary
 * user of this class is {@link com.google.gwt.junit.JUnitShell}.
 * 
 * The report is in XML format. To view the XML reports, use benchmarkViewer.
 */
public class BenchmarkReport {

  /**
   * Converts a set of test results for a single benchmark method into XML.
   */
  private class BenchmarkXml {

    private MetaData metaData;

    private List<BenchmarkResults> results;

    private TestCase test;

    BenchmarkXml(TestCase test, List<BenchmarkResults> results) {
      this.test = test;
      this.results = results;
      Map<String, MetaData> methodMetaData = testMetaData.get(test.getClass().toString());
      metaData = methodMetaData.get(test.getName());
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

      for (BenchmarkResults result : results) {
        benchmark.appendChild(toElement(doc, result));
      }

      return benchmark;
    }

    private Element toElement(Document doc, BenchmarkResults result) {
      Element resultElement = doc.createElement("result");
      resultElement.setAttribute("host", result.getHost());
      resultElement.setAttribute("agent", result.getAgent());

      Throwable exception = result.getException();

      if (exception != null) {
        Element exceptionElement = doc.createElement("exception");
        exceptionElement.appendChild(doc.createTextNode(exception.toString()));
        resultElement.appendChild(exceptionElement);
      }

      List<Trial> trials = result.getTrials();

      for (Trial trial : trials) {
        Element trialElement = toElement(doc, trial);
        resultElement.appendChild(trialElement);
      }

      return resultElement;
    }

    private Element toElement(Document doc, Trial trial) {
      Element trialElement = doc.createElement("trial");

      Map<String, String> variables = trial.getVariables();

      for (Map.Entry<String, String> entry : variables.entrySet()) {
        Object name = entry.getKey();
        Object value = entry.getValue();
        Element variableElement = doc.createElement("variable");
        variableElement.setAttribute("name", name.toString());
        variableElement.setAttribute("value", value.toString());
        trialElement.appendChild(variableElement);
      }

      trialElement.setAttribute("timing",
          String.valueOf(trial.getRunTimeMillis()));

      return trialElement;
    }
  }

  /**
   * Parses .java source files to get source code for methods.
   */
  private class Parser {

    /**
     * Maps classes to the contents of their source files.
     */
    private Map<JClassType, String> classSources = new HashMap<JClassType, String>();

    /**
     * Returns the source code for the method of the given name.
     * 
     * @param logger to log the process
     * @param method a not <code>null</code> method
     * @return <code>null</code> if the source code for the method can not be
     *         located
     */
    public String getMethod(TreeLogger logger, JMethod method) {
      JClassType clazz = method.getEnclosingType();

      if (!classSources.containsKey(clazz)) {
        char[] sourceContents = null;
        File sourceFile = findSourceFile(clazz);
        if (sourceFile != null) {
          sourceContents = Util.readFileAsChars(sourceFile);
          classSources.put(clazz, new String(sourceContents));
        }

        if (sourceContents == null) {
          classSources.put(clazz, null);
          String msg = "An unknown I/O exception occured while trying to read "
              + sourceFile.getAbsolutePath();
          logger.log(TreeLogger.WARN, msg, null);
        } else {
          classSources.put(clazz, new String(sourceContents));
          String msg = "BenchmarkReport read the contents of "
              + sourceFile.getAbsolutePath();
          TreeLogger branch = logger.branch(TreeLogger.DEBUG, msg, null);
          if (logger.isLoggable(TreeLogger.SPAM)) {
            branch.log(TreeLogger.SPAM, new String(sourceContents), null);
          }
        }
      }

      String source = classSources.get(clazz);

      if (source == null) {
        return source;
      }

      try {
        return source.substring(method.getDeclStart(), method.getDeclEnd() + 1);
      } catch (IndexOutOfBoundsException e) {
        logger.log(TreeLogger.WARN, "Unable to parse " + method.getName(), e);
        // Have seen this happen when the compiler read the source using one
        // character encoding, and then this Parser read it in a different
        // encoding. I don't know if there are other cases in which this can
        // occur.
        return null;
      }
    }
  }

  /**
   * Converts an entire report into XML.
   */
  private class ReportXml {

    private Map<String, Element> categoryElementMap = new HashMap<String, Element>();

    private Date date = new Date();

    private String version = "unknown";

    Element toElement(Document doc) {
      Element report = doc.createElement("gwt_benchmark_report");
      String dateString = DateFormat.getDateTimeInstance().format(date);
      report.setAttribute("date", dateString);
      report.setAttribute("gwt_version", version);

      // Add each test result into the report.
      // Add the category for the test result, if necessary.
      for (Map.Entry<TestCase, List<BenchmarkResults>> entry : testResults.entrySet()) {
        TestCase test = entry.getKey();
        List<BenchmarkResults> results = entry.getValue();
        BenchmarkXml xml = new BenchmarkXml(test, results);
        Element categoryElement = getCategoryElement(doc, report,
            xml.metaData.getCategory().getClassName());
        categoryElement.appendChild(xml.toElement(doc));
      }

      return report;
    }

    /**
     * Locates or creates the category element by the specified name.
     * 
     * @param doc The document to search
     * @param report The report to which the category belongs
     * @param name The name of the category
     * 
     * @return The matching category element
     */
    private Element getCategoryElement(Document doc, Element report, String name) {
      Element e = categoryElementMap.get(name);

      if (e != null) {
        return e;
      }

      Element categoryElement = doc.createElement("category");
      categoryElementMap.put(name, categoryElement);
      CategoryImpl category = testCategories.get(name);
      categoryElement.setAttribute("name", category.getName());
      categoryElement.setAttribute("description", category.getDescription());

      report.appendChild(categoryElement);

      return categoryElement;
    }
  }

  private static final String GWT_BENCHMARK_CATEGORY = "gwt.benchmark.category";

  private static final String GWT_BENCHMARK_DESCRIPTION = "gwt.benchmark.description";

  private static final String GWT_BENCHMARK_NAME = "gwt.benchmark.name";

  private static File findSourceFile(JClassType clazz) {
    final char separator = File.separator.charAt(0);
    String filePath = clazz.getPackage().getName().replace('.', separator)
        + separator + clazz.getSimpleSourceName() + ".java";
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

  private static String getSimpleMetaData(HasMetaData hasMetaData, String name) {
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

  private Parser parser = new Parser();

  private Map<String, CategoryImpl> testCategories = new HashMap<String, CategoryImpl>();

  private Map<String, Map<String, MetaData>> testMetaData = new HashMap<String, Map<String, MetaData>>();

  private Map<TestCase, List<BenchmarkResults>> testResults = new HashMap<TestCase, List<BenchmarkResults>>();

  private TypeOracle typeOracle;

  public BenchmarkReport() {
  }

  /**
   * Adds the Benchmark to the report. All of the metadata about the benchmark
   * (category, name, description, etc...) is recorded from the TypeOracle.
   * 
   * @param logger to log the process
   * @param deprecationBranch to log usages of old-style metadata
   * @param benchmarkClass The benchmark class to record. Must not be
   *          <code>null</code>.
   * @param typeOracle The <code>TypeOracle</code> for the compilation session
   *          must not be <code>null</code>.
   */
  public void addBenchmark(TreeLogger logger, TreeLogger deprecationBranch,
      JClassType benchmarkClass, TypeOracle typeOracle) {

    this.typeOracle = typeOracle;
    String categoryType = getBenchmarkCategory(deprecationBranch,
        benchmarkClass);

    Map<String, JMethod> zeroArgMethods = BenchmarkGenerator.getNotOverloadedTestMethods(benchmarkClass);
    Map<String, JMethod> parameterizedMethods = BenchmarkGenerator.getParameterizedTestMethods(
        benchmarkClass, TreeLogger.NULL);
    List<JMethod> testMethods = new ArrayList<JMethod>(zeroArgMethods.size()
        + parameterizedMethods.size());
    testMethods.addAll(zeroArgMethods.values());
    testMethods.addAll(parameterizedMethods.values());

    Map<String, MetaData> metaDataMap = testMetaData.get(benchmarkClass.toString());
    if (metaDataMap == null) {
      metaDataMap = new HashMap<String, MetaData>();
      testMetaData.put(benchmarkClass.toString(), metaDataMap);
    }

    // Add all of the benchmark methods
    for (JMethod method : testMethods) {
      String methodName = method.getName();
      String methodCategoryType = getBenchmarkCategory(deprecationBranch,
          method);
      if (methodCategoryType == null) {
        methodCategoryType = categoryType;
      }
      CategoryImpl methodCategory = getCategory(deprecationBranch,
          methodCategoryType);

      String methodSource = parser.getMethod(logger, method);
      StringBuffer sourceBuffer = (methodSource == null) ? null
          : new StringBuffer(methodSource);
      StringBuffer summary = new StringBuffer();
      StringBuffer comment = new StringBuffer();
      getComment(sourceBuffer, summary, comment);

      MetaData metaData = new MetaData(benchmarkClass.toString(), methodName,
          (sourceBuffer != null) ? sourceBuffer.toString() : null,
          methodCategory, methodName, summary.toString());
      metaDataMap.put(methodName, metaData);
    }
  }

  public void addBenchmarkResults(TestCase test, BenchmarkResults results) {
    List<BenchmarkResults> currentResults = testResults.get(test);
    if (currentResults == null) {
      currentResults = new ArrayList<BenchmarkResults>();
      testResults.put(test, currentResults);
    }
    currentResults.add(results);
  }

  /**
   * Generates reports for all of the benchmarks which were added to the
   * generator.
   * 
   * @param outputPath The path to write the reports to.
   * @throws ParserConfigurationException If an error occurs during xml parsing
   * @throws IOException If anything goes wrong writing to outputPath
   */
  public void generate(String outputPath) throws ParserConfigurationException,
      IOException {

    // Don't generate a new report if no tests were actually run.
    if (testResults.size() == 0) {
      return;
    }

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.newDocument();
    doc.appendChild(new ReportXml().toElement(doc));
    byte[] xmlBytes = Util.toXmlUtf8(doc);
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(outputPath);
      fos.write(xmlBytes);
    } finally {
      Utility.close(fos);
    }

    // TODO(bruce) The code below is commented out because of GWT Issue 958.

    // // TODO(tobyr) Looks like indenting is busted
    // // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6296446
    // // Not a big deal, since we don't intend to read the XML by hand anyway
    // TransformerFactory transformerFactory = TransformerFactory.newInstance();
    // // Think this can be used with JDK 1.5
    // // transformerFactory.setAttribute( "indent-number", new Integer(2) );
    // Transformer serializer = transformerFactory.newTransformer();
    // serializer.setOutputProperty(OutputKeys.METHOD, "xml");
    // serializer.setOutputProperty(OutputKeys.INDENT, "yes");
    // serializer
    // .setOutputProperty("{ http://xml.apache.org/xslt }indent-amount", "2");
    // BufferedOutputStream docOut = new BufferedOutputStream(
    // new FileOutputStream(outputPath));
    // serializer.transform(new DOMSource(doc), new StreamResult(docOut));
    // docOut.close();
  }

  private <T extends HasMetaData & HasAnnotations> String getBenchmarkCategory(
      TreeLogger deprecationBranch, T element) {
    String category = getSimpleMetaData(element, GWT_BENCHMARK_CATEGORY);
    if (category != null) {
      deprecationBranch.log(TreeLogger.WARN, GWT_BENCHMARK_CATEGORY + " has "
          + "been deprecated with no replacement.", null);
    }
    return category;
  }

  private CategoryImpl getCategory(TreeLogger deprecationBranch, String name) {
    CategoryImpl c = testCategories.get(name);

    if (c != null) {
      return c;
    }

    c = getCategoryMetaData(deprecationBranch, name);
    testCategories.put(name, c);
    return c;
  }

  private CategoryImpl getCategoryMetaData(TreeLogger deprecationBranch,
      String typeName) {
    if (typeName == null) {
      return new CategoryImpl(null, "", "");
    }

    JClassType categoryType = typeOracle.findType(typeName);

    if (categoryType == null) {
      return new CategoryImpl(typeName, "", "");
    }

    String categoryName = getSimpleMetaData(categoryType, GWT_BENCHMARK_NAME);
    String categoryDescription = getSimpleMetaData(categoryType,
        GWT_BENCHMARK_DESCRIPTION);

    if (categoryName != null || categoryDescription != null) {
      deprecationBranch.log(TreeLogger.WARN, GWT_BENCHMARK_NAME + " and "
          + GWT_BENCHMARK_DESCRIPTION + " have been deprecated with no "
          + "replacement", null);
    }

    categoryName = categoryName == null ? "" : categoryName;
    categoryDescription = categoryDescription == null ? ""
        : categoryDescription;

    return new CategoryImpl(typeName, categoryName, categoryDescription);
  }

  /**
   * Parses out the JavaDoc comment from a string of source code. Returns the
   * first sentence summary in <code>summary</code> and the body of the entire
   * comment (including the summary) in <code>comment</code>.
   * 
   * @param sourceCode The source code of a function, including its comment.
   *          Modified to remove leading whitespace.
   * @param summary Modified to contain the first sentence of the comment.
   * @param comment Modified to contain the entire comment.
   */
  private void getComment(StringBuffer sourceCode, StringBuffer summary,
      StringBuffer comment) {

    if (sourceCode == null) {
      return;
    }

    summary.setLength(0);
    comment.setLength(0);

    String regex = "/\\*\\*(.(?!}-\\*/))*\\*/";

    Pattern p = Pattern.compile(regex, Pattern.DOTALL);
    Matcher m = p.matcher(sourceCode);

    // Early out if there is no javadoc comment.
    if (!m.find()) {
      return;
    }

    String commentStr = m.group();

    p = Pattern.compile("(/\\*\\*\\s*)" + // The comment header
        "(((\\s*\\**\\s*)([^\n\r]*)[\n\r]+)*)" // The comment body
    );

    m = p.matcher(commentStr);

    if (!m.find()) {
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

    // Measure the indentation width on the second line to infer what the
    // first line indent should be.
    p = Pattern.compile("[^\\r\\n]+[\\r\\n]+(\\s+)\\*", Pattern.MULTILINE);
    m = p.matcher(sourceCode);
    int indentLen = 0;
    if (m.find()) {
      String indent = m.group(1);
      indentLen = indent.length() - 1;
    }
    StringBuffer leadingIndent = new StringBuffer();
    for (int i = 0; i < indentLen; ++i) {
      leadingIndent.append(' ');
    }

    // By inserting at 0 here, we are assuming that sourceCode begins with
    // /**, which is actually a function of how JDT sees a declaration start.
    // If in the future, you see bogus indentation here, it means that this
    // assumption is bad.
    sourceCode.insert(0, leadingIndent);
  }
}
