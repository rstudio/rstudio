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
package com.google.doctool;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 * Orchestrates the behavior of {@link Booklet}, {@link SplitterJoiner} and
 * other tools to create user documentation and API documentation.
 */
public class DocTool {

  private class ImageCopier extends DefaultHandler {

    private final File htmlDir;

    private ImageCopier(File htmlDir) {
      this.htmlDir = htmlDir;
    }

    public void startElement(String uri, String localName, String qName,
        Attributes attributes) throws SAXException {
      if (qName.equalsIgnoreCase("img")) {
        String imgSrc = attributes.getValue("src");
        if (imgSrc != null) {
          boolean found = false;
          for (int i = 0, n = imagePath.length; i < n; ++i) {
            File dir = imagePath[i];
            File inFile = new File(dir, imgSrc);
            if (inFile.exists()) {
              // Copy it over.
              //
              found = true;
              File outFile = new File(htmlDir, imgSrc);

              if (outFile.exists()) {
                if (outFile.lastModified() > inFile.lastModified()) {
                  // Already up to date.
                  break;
                }
              } else {
                File outFileDir = outFile.getParentFile();
                if (!outFileDir.exists() && !outFileDir.mkdirs()) {
                  err.println("Unable to create image output dir " + outFileDir);
                  break;
                }
              }
              if (!copyFile(inFile, outFile)) {
                err.println("Unable to copy image file " + outFile);
              }
            }
          }
          if (!found) {
            err.println("Unable to find image " + imgSrc);
          }
        }
      }
    }
  }

  private static final Pattern IN_XML_FILENAME = Pattern.compile(
      "(.+)\\.([^\\.]+)\\.xml", Pattern.CASE_INSENSITIVE);

  public static void main(String[] args) {
    DocToolFactory factory = new DocToolFactory();
    String arg;
    String pathSep = System.getProperty("path.separator");
    for (int i = 0, n = args.length; i < n; ++i) {
      if (tryParseFlag(args, i, "-help")) {
        printHelp();
        return;
      } else if (null != (arg = tryParseArg(args, i, "-out"))) {
        ++i;
        factory.setOutDir(arg);
      } else if (null != (arg = tryParseArg(args, i, "-html"))) {
        ++i;
        factory.setGenerateHtml(true);
        factory.setTitle(arg);

        // Slurp every arg not prefixed with "-".
        for (; i + 1 < n && !args[i + 1].startsWith("-"); ++i) {
          factory.addHtmlFileBase(args[i + 1]);
        }
      } else if (null != (arg = tryParseArg(args, i, "-overview"))) {
        ++i;
        factory.setOverviewFile(arg);
      } else if (null != (arg = tryParseArg(args, i, "-sourcepath"))) {
        ++i;
        String[] entries = arg.split("\\" + pathSep);
        for (int entryIndex = 0; entryIndex < entries.length; entryIndex++) {
          factory.addToSourcePath(entries[entryIndex]);
        }
      } else if (null != (arg = tryParseArg(args, i, "-classpath"))) {
        ++i;
        String[] entries = arg.split("\\" + pathSep);
        for (int entryIndex = 0; entryIndex < entries.length; entryIndex++) {
          factory.addToClassPath(entries[entryIndex]);
        }
      } else if (null != (arg = tryParseArg(args, i, "-packages"))) {
        ++i;
        String[] entries = arg.split("\\" + pathSep);
        for (int entryIndex = 0; entryIndex < entries.length; entryIndex++) {
          factory.addToPackages(entries[entryIndex]);
        }
      } else if (null != (arg = tryParseArg(args, i, "-imagepath"))) {
        ++i;
        String[] entries = arg.split("\\" + pathSep);
        for (int entryIndex = 0; entryIndex < entries.length; entryIndex++) {
          factory.addToImagePath(entries[entryIndex]);
        }
      } else {
        if (factory.getFileType() == null) {
          factory.setFileType(args[i]);
        } else {
          factory.setFileBase(args[i]);
        }
      }
    }

    DocTool docTool = factory.create(System.out, System.err);
    if (docTool != null) {
      docTool.process();
    }
  }

  public static boolean recursiveDelete(File file) {
    if (file.isDirectory()) {
      File[] children = file.listFiles();
      if (children != null) {
        for (int i = 0; i < children.length; i++) {
          if (!recursiveDelete(children[i])) {
            return false;
          }
        }
      }
    }
    if (!file.delete()) {
      System.err.println("Unable to delete " + file.getAbsolutePath());
      return false;
    }
    return true;
  }

  private static void printHelp() {
    String s = "";
    s += "DocTool (filetype filebase)? [docset-creation-options] [html-creation-options]\n";
    s += "    Creates structured javadoc xml output from Java source and/or\n";
    s += "    a table of contents and a set of cross-linked html files.\n";
    s += "    Specifying filebase/filetype produces output file \"filebase.filetype.xml\".\n";
    s += "    Specifying -html produces output files in ${out}/html.\n";
    s += "\n";
    s += "[docset-creation-options] are\n";
    s += "  -out\n";
    s += "    The output directory\n";
    s += "  -overview\n";
    s += "    The overview html file for this doc set\n";
    s += "  -sourcepath path\n";
    s += "    The path to find Java source for this doc set\n";
    s += "  -classpath path\n";
    s += "    The path to find imported classes for this doc set\n";
    s += "  -packages package-names\n";
    s += "    The command-separated list of package names to include in this doc set\n";
    s += "\n";
    s += "[html-creation-options] are\n";
    s += "  -html title filebase+\n";
    s += "    Causes topics in the named filebase(s) to be merged and converted into html\n";
    s += "  -imagepath\n";
    s += "    The semicolon-separated path to find images for html\n";
    System.out.println(s);
  }

  /**
   * Parse a flag with a argument.
   */
  private static String tryParseArg(String[] args, int i, String name) {
    if (i < args.length) {
      if (args[i].equals(name)) {
        if (i + 1 < args.length) {
          String arg = args[i + 1];
          if (arg.startsWith("-")) {
            System.out.println("Warning: arg to " + name
                + " looks more like a flag: " + arg);
          }
          return arg;
        } else {
          throw new IllegalArgumentException("Expecting an argument after "
              + name);
        }
      }
    }
    return null;
  }

  /**
   * Parse just a flag with no subsequent argument.
   */
  private static boolean tryParseFlag(String[] args, int i, String name) {
    if (i < args.length) {
      if (args[i].equals(name)) {
        return true;
      }
    }
    return false;
  }

  private final File[] classPath;

  private final String[] packages;

  private final PrintStream err;

  private final String base;

  private final String fileType;

  private final boolean generateHtml;

  private final String[] htmlFileBases;

  private final File[] imagePath;

  private final PrintStream out;

  private final File outDir;

  private final File overviewFile;

  private final File[] sourcePath;

  private final String title;

  DocTool(PrintStream out, PrintStream err, File outDir, boolean generateHtml,
      String title, String[] htmlFileBases, String fileType, String fileBase,
      File overviewFile, File[] sourcePath, File[] classPath,
      String[] packages, File[] imagePath) {
    this.out = out;
    this.err = err;
    this.outDir = outDir;
    this.generateHtml = generateHtml;
    this.base = fileBase;
    this.fileType = fileType;
    this.overviewFile = overviewFile;
    this.sourcePath = sourcePath;
    this.classPath = classPath;
    this.packages = packages;
    this.imagePath = imagePath;
    this.title = title;
    this.htmlFileBases = htmlFileBases.clone();
  }

  public boolean copyFile(File in, File out) {
    FileInputStream fis = null;
    FileOutputStream fos = null;
    try {
      fis = new FileInputStream(in);
      fos = new FileOutputStream(out);
      byte[] buf = new byte[4096];
      int i = 0;
      while ((i = fis.read(buf)) != -1) {
        fos.write(buf, 0, i);
      }
      return true;
    } catch (IOException e) {
      return false;
    } finally {
      close(fis);
      close(fos);
    }
  }

  private void close(InputStream is) {
    if (is != null) {
      try {
        is.close();
      } catch (IOException e) {
        e.printStackTrace(err);
      }
    }
  }

  private void close(OutputStream os) {
    if (os != null) {
      try {
        os.close();
      } catch (IOException e) {
        e.printStackTrace(err);
      }
    }
  }

  private boolean copyImages(File htmlDir, File mergedTopicsFile) {
    FileReader fileReader = null;
    Throwable caught = null;
    try {
      fileReader = new FileReader(mergedTopicsFile);
      SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
      InputSource inputSource = new InputSource(fileReader);
      XMLReader xmlReader = parser.getXMLReader();
      xmlReader.setContentHandler(new ImageCopier(htmlDir));
      xmlReader.parse(inputSource);
      return true;
    } catch (SAXException e) {
      caught = e;
      Exception inner = e.getException();
      if (inner != null) {
        caught = inner;
      }
    } catch (ParserConfigurationException e) {
      caught = e;
    } catch (IOException e) {
      caught = e;
    } finally {
      try {
        if (fileReader != null) {
          fileReader.close();
        }
      } catch (IOException e) {
        e.printStackTrace(err);
      }
    }
    caught.printStackTrace(err);
    return false;
  }

  private Set findSourcePackages() {
    Set results = new HashSet();
    for (int i = 0, n = sourcePath.length; i < n; ++i) {
      File srcDir = sourcePath[i];
      findSourcePackages(results, srcDir, "");
    }
    return results;
  }

  private void findSourcePackages(Set results, File dir, String parentPackage) {
    File[] children = dir.listFiles();
    if (children != null) {
      for (int i = 0, n = children.length; i < n; ++i) {
        File child = children[i];
        String childName = parentPackage
            + (parentPackage.length() > 0 ? "." : "") + child.getName();
        if (child.isDirectory()) {
          // Recurse
          findSourcePackages(results, child, childName);
        } else if (child.getName().endsWith(".java")) {
          // Only include this dir as a result if there's at least one java file
          results.add(parentPackage);
        }
      }
    }
  }

  private String flattenPath(File[] entries) {
    String pathSep = System.getProperty("path.separator");
    String path = "";
    for (int i = 0, n = entries.length; i < n; ++i) {
      File entry = entries[i];
      if (i > 0) {
        path += pathSep;
      }
      path += entry.getAbsolutePath();
    }
    return path;
  }

  private void freshenIf(File file) {
    if (!file.isFile()) {
      return;
    }

    String name = file.getName();
    Matcher matcher = IN_XML_FILENAME.matcher(name);
    if (matcher.matches()) {
      String suffix = "." + matcher.group(2) + ".xml";
      File topicFile = tryReplaceSuffix(file, suffix, ".topics.xml");
      if (topicFile != null) {
        if (file.lastModified() > topicFile.lastModified()) {
          String xsltFileName = matcher.group(2) + "-" + "topics.xslt";
          String xslt = getFileFromClassPath(xsltFileName); // yucky slow
          out.println(file + " -> " + topicFile);
          transform(xslt, file, topicFile, null);
        }
      }
    }
  }

  private boolean genHtml() {
    // Make sure the html directory exists.
    //
    File htmlDir = new File(outDir, "html");
    if (!htmlDir.exists() && !htmlDir.mkdirs()) {
      err.println("Cannot create html output directory "
          + htmlDir.getAbsolutePath());
      return false;
    }

    // Merge all *.topics.xml into one topics.xml file.
    //
    File mergedTopicsFile = new File(outDir, "topics.xml");
    if (!mergeTopics(mergedTopicsFile)) {
      return false;
    }

    // Parse it all to find the images and copy them over.
    //
    copyImages(htmlDir, mergedTopicsFile);

    // Transform to merged topics into merged htmls.
    //
    File mergedHtmlsFile = new File(htmlDir, "topics.htmls");
    long lastModifiedHtmls = mergedHtmlsFile.lastModified();
    long lastModifiedTopics = mergedTopicsFile.lastModified();
    if (!mergedHtmlsFile.exists() || lastModifiedHtmls < lastModifiedTopics) {
      String xsltHtmls = getFileFromClassPath("topics-htmls.xslt");

      Map params = new HashMap();
      params.put("title", title);

      transform(xsltHtmls, mergedTopicsFile, mergedHtmlsFile, params);

      // Split the merged htmls into many html files.
      //
      if (!splitHtmls(mergedHtmlsFile)) {
        return false;
      }

      // Create a table of contents.
      //
      File tocFile = new File(htmlDir, "contents.html");
      String xsltToc = getFileFromClassPath("topics-toc.xslt");
      transform(xsltToc, mergedTopicsFile, tocFile, params);

      // Copy the CSS file over.
      //
      String css = getFileFromClassPath("doc.css");
      try {
        FileWriter cssWriter = new FileWriter(new File(htmlDir, "doc.css"));
        cssWriter.write(css);
        cssWriter.close();
      } catch (IOException e) {
        e.printStackTrace(err);
      }
    } else {
      out.println("Skipping html creation since nothing seems to have changed since "
          + mergedHtmlsFile.getAbsolutePath());
    }

    return true;
  }

  private String getFileFromClassPath(String filename) {
    InputStream in = null;
    try {
      in = getClass().getClassLoader().getResourceAsStream(filename);
      try {
        if (in == null) {
          throw new RuntimeException("Cannot find file: " + filename);
        }
        StringWriter sw = new StringWriter();
        int ch;
        while ((ch = in.read()) != -1) {
          sw.write(ch);
        }
        return sw.toString();
      } finally {
        if (in != null) {
          in.close();
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private boolean mergeTopics(File mergedTopicsFile) {
    try {
      List args = new ArrayList();
      args.add("join"); // what to do
      args.add("topics"); // the outer element is <topics>
      args.add(mergedTopicsFile.getAbsolutePath());

      // For each of the htmlFileBases, try to find a file having that name to
      // merge into the big topics doc.
      //
      boolean foundAny = false;
      for (int i = 0, n = htmlFileBases.length; i < n; ++i) {
        String filebase = htmlFileBases[i];
        File fileToMerge = new File(outDir, filebase + ".topics.xml");
        if (fileToMerge.exists()) {
          foundAny = true;
          args.add(fileToMerge.getAbsolutePath());
        } else {
          err.println("Unable to find " + fileToMerge.getName());
        }
      }

      if (foundAny) {
        String[] argArray = (String[]) args.toArray(new String[0]);
        traceCommand("SplitterJoiner", argArray);
        SplitterJoiner.main(argArray);
      } else {
        err.println("No topics found");
        return false;
      }
    } catch (IOException e) {
      e.printStackTrace(err);
      return false;
    }
    return true;
  }

  /**
   * Runs the help process.
   */
  private boolean process() {
    if (fileType != null) {
      // Produce XML from JavaDoc.
      //
      String fileName = base + "." + fileType + ".xml";
      if (!runBooklet(new File(outDir, fileName))) {
        return false;
      }
    }

    // Process existing files to get them into topics format.
    // Done afterwards for convenience when debugging your doc.
    //
    transformExistingIntoTopicXml();

    if (generateHtml) {
      // Merge into HTML.
      if (!genHtml()) {
        return false;
      }
    }

    return true;
  }

  private boolean runBooklet(File bkoutFile) {
    // Write out the list of packages that can be found on the source path.
    out.println("Creating " + bkoutFile.getAbsolutePath());
    Set srcPackages = findSourcePackages();
    if (srcPackages.isEmpty()) {
      err.println("No input files found");
      return false;
    }

    List args = new ArrayList();

    // For now, harded-coded, but could be passed through
    args.add("-source");
    args.add("1.5");

    // The doclet
    args.add("-doclet");
    args.add(Booklet.class.getName());

    // Class path
    args.add("-classpath");
    args.add(flattenPath(classPath));

    // Source path
    args.add("-sourcepath");
    args.add(flattenPath(sourcePath));
    
    // Encoding is always UTF-8
    args.add("-encoding");
    args.add("UTF-8");

    // Overview file
    if (overviewFile != null) {
      args.add("-overview");
      args.add(overviewFile.getAbsolutePath());
    }

    // Output file
    args.add("-bkout");
    args.add(bkoutFile.getAbsolutePath());

    if (packages != null) {
      // Specify the packages to actually emit doc for
      StringBuffer bkdocpkg = new StringBuffer();
      for (int i = 0; i < packages.length; i++) {
        String pkg = packages[i];
        bkdocpkg.append(pkg);
        bkdocpkg.append(";");
      }
      args.add("-bkdocpkg");
      args.add(bkdocpkg.toString());
    }

    args.add("-breakiterator");

    // Specify the set of input packages (needed by JavaDoc)
    args.addAll(srcPackages);

    String[] argArray = (String[]) args.toArray(new String[0]);
    traceCommand("Booklet", argArray);
    Booklet.main(argArray);

    return bkoutFile.exists();
  }

  private boolean splitHtmls(File mergedHtmlsFile) {
    try {
      List args = new ArrayList();
      args.add("split"); // what to do
      args.add(mergedHtmlsFile.getAbsolutePath());
      String[] argArray = (String[]) args.toArray(new String[0]);
      traceCommand("SplitterJoiner", argArray);
      SplitterJoiner.main(argArray);
    } catch (IOException e) {
      e.printStackTrace(err);
      return false;
    }
    return true;
  }

  private void traceCommand(String cmd, String[] args) {
    out.print(cmd);
    for (int i = 0, n = args.length; i < n; ++i) {
      String arg = args[i];
      out.print(" ");
      out.print(arg);
    }
    out.println();
  }

  private void transform(String xslt, File inFile, File outFile, Map params) {
    Throwable caught = null;
    try {
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      StreamSource xsltSource = new StreamSource(new StringReader(xslt));
      Transformer transformer = transformerFactory.newTransformer(xsltSource);
      transformer.setOutputProperty(
          javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
      transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT,
          "yes");
      transformer.setOutputProperty(
          "{http://xml.apache.org/xslt}indent-amount", "4");

      if (params != null) {
        for (Iterator iter = params.entrySet().iterator(); iter.hasNext();) {
          Map.Entry entry = (Map.Entry) iter.next();
          transformer.setParameter((String) entry.getKey(), entry.getValue());
        }
      }

      FileOutputStream fos = new FileOutputStream(outFile);
      StreamResult result = new StreamResult(fos);
      StreamSource xmlSource = new StreamSource(new FileReader(inFile));
      transformer.transform(xmlSource, result);
      fos.close();
      return;
    } catch (TransformerConfigurationException e) {
      caught = e;
    } catch (TransformerException e) {
      caught = e;
    } catch (IOException e) {
      caught = e;
    }
    throw new RuntimeException("Unable to complete the xslt tranform", caught);
  }

  private void transformExistingIntoTopicXml() {
    File[] children = outDir.listFiles();
    if (children != null) {
      for (int i = 0, n = children.length; i < n; ++i) {
        File file = children[i];
        freshenIf(file);
      }
    }
  }

  private File tryReplaceSuffix(File file, String oldSuffix, String newSuffix) {
    String name = file.getName();
    if (name.endsWith(oldSuffix)) {
      String baseName = name.substring(0, name.length() - oldSuffix.length());
      return new File(file.getParent(), baseName + newSuffix);
    } else {
      return null;
    }
  }
}
