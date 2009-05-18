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

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Takes an input stream and splits it into multiple files. A new file begins
 * when a line in the input stream begins with a specific prefix followed by
 * whitespace then an absolute or relative file name to create.
 */
public class SplitterJoiner {

  public static void main(String[] args) throws IOException {
    if (args.length < 2) {
      help();
      return;
    } else if (args[0].equals("split")) {
      String[] files = new String[args.length - 1];
      System.arraycopy(args, 1, files, 0, args.length - 1);
      split(files);
    } else if (args[0].equals("join")) {
      if (args.length < 4) {
        help();
        return;
      }
      String[] files = new String[args.length - 3];
      System.arraycopy(args, 3, files, 0, args.length - 3);
      merge(args[1], args[2], files);
    } else {
      if (!args[0].equals("-h") && !args[0].equals("-?")) {
        System.err.println("Error: don't know '" + args[0] + "'");
      }
      help();
      return;
    }
  }

  private static void emitFile(PrintWriter out, File outputFile, File inputFile)
      throws IOException, ParserConfigurationException, SAXException,
      TransformerException {
    if (!inputFile.exists()) {
      System.err.println("Error: Cannot find input file " + inputFile.getPath());
      return;
    }

    if (inputFile.getCanonicalFile().equals(outputFile)) {
      // skip
      return;
    }

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    Document doc = builder.parse(inputFile);
    writeTopLevelChildren(doc, out);
  }

  private static void help() {
    System.out.println("Usage: SplitterJoiner split infile+");
    System.out.println("Usage: SplitterJoiner join tag outfile (infile|dir)+");
    System.out.println("\tsplit         indicates that inputs file should be split into multiple output files");
    System.out.println("\tjoin          indicates xml files (or directories) should be merged into one big xml file (on stdout)");
    System.out.println("\ttag           when joining, the outermost xml tag name");
    System.out.println("\toutfile       when joining, the file to write the joined output into");
    System.out.println("\tinfile        if splitting, an input file to split");
    System.out.println("\t              if joining, a file whose contents should be merged in");
    System.out.println("\tdir           when joining, a directory whose xml files' contents should be merged in");
  }

  private static boolean isNewerThan(File file, long lastModified) {
    if (file.isFile()) {
      return file.lastModified() > lastModified;
    }

    File[] children = file.listFiles();
    if (children != null) {
      for (int i = 0, n = children.length; i < n; ++i) {
        File child = children[i];
        if (isNewerThan(child, lastModified)) {
          return true;
        }
      }
    }

    return false;
  }

  private static void merge(String tag, String outputPath, String files[]) {
    File outputFile = null;
    try {
      outputFile = new File(outputPath).getCanonicalFile();
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    // Maybe we don't need to do anything.
    //
    boolean skipMerge = true;
    if (!outputFile.exists()) {
      skipMerge = false;
    } else {
      long outputFileLastModified = outputFile.lastModified();
      for (int i = 0, n = files.length; i < n; ++i) {
        if (isNewerThan(new File(files[i]), outputFileLastModified)) {
          skipMerge = false;
          break;
        }
      }
    }

    if (skipMerge) {
      // Nothing to do.
      //
      return;
    }

    try {
      PrintWriter out = new PrintWriter(new FileWriter(outputFile), true);

      out.println("<?xml version='1.0'?>");
      out.println("<" + tag + ">");

      for (int i = 0; i < files.length; i++) {
        File file = new File(files[i]);
        if (file.isFile()) {
          emitFile(out, outputFile, file);
        } else {
          File[] children = file.listFiles();
          if (children != null) {
            for (int j = 0; j < children.length; ++j) {
              if (children[j].isFile()
                  && children[j].getPath().endsWith(".xml")) {
                emitFile(out, outputFile, children[j]);
              }
            }
          }
        }
      }
      out.println("</" + tag + ">");
      out.close();
    } catch (IOException e) {
      outputFile.deleteOnExit();
      e.printStackTrace();
    } catch (ParserConfigurationException e) {
      outputFile.deleteOnExit();
      e.printStackTrace();
    } catch (SAXException e) {
      outputFile.deleteOnExit();
      e.printStackTrace();
    } catch (TransformerException e) {
      e.printStackTrace();
    }
  }

  private static void split(String[] files) throws IOException {
    String prefix = null;
    File inputFile = null;

    for (int i = 0; i < files.length; i++) {
      BufferedReader reader = null;
      try {
        // Open the reader.
        //                
        String file = files[i];
        inputFile = new File(file);
        if (!inputFile.exists()) {
          System.err.println("Error: Cannot find input file "
              + inputFile.getPath());
          return;
        }
        reader = new BufferedReader(new FileReader(inputFile));

        // Parse the input
        //
        File outFile = null;
        PrintWriter writer = null;
        String line = reader.readLine();
        while (line != null) {
          if (prefix == null) {
            // Learn the prefix.
            //
            prefix = line.trim();
            if (prefix.length() == 0) {
              // The first line with anything on it counts as the prefix.
              // 
              prefix = null;
            }
          } else if (line.startsWith(prefix)) {
            // Close the current writer.
            //
            if (writer != null) {
              writer.close();
            }

            // Create the next writer.
            //
            String outPath = line.substring(prefix.length()).trim();
            outFile = new File(outPath);
            if (!outFile.isAbsolute()) {
              // Make the created file relative to the input file.
              //
              File absoluteParentDir = inputFile.getCanonicalFile().getParentFile();
              outFile = new File(absoluteParentDir, outPath);
              // Ignore result since the next line will fail if the directory
              // doesn't exist.
              outFile.getParentFile().mkdirs();
            }

            writer = new PrintWriter(new FileWriter(outFile), true);

            writer.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");

          } else if (writer != null) {
            // Write this line to the current file.
            //
            writer.println(line);
          } else {
            // Ignored -- haven't yet seen a starting prefix.
            //
          }

          line = reader.readLine();
        }

        if (writer != null) {
          writer.close();
        }
      } finally {
        // Close the current reader, if any.
        if (reader != null) {
          reader.close();
        }
      }
    }
  }

  private static void writeTopLevelChildren(Document doc, PrintWriter out)
      throws TransformerException {
    StreamResult result = new StreamResult(out);
    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    Transformer transformer = transformerFactory.newTransformer();
    transformer.setOutputProperty(
        javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
    transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount",
        "4");

    Node child = doc.getDocumentElement().getFirstChild();
    while (child != null) {
      DOMSource domSource = new DOMSource(child);
      transformer.transform(domSource, result);
      child = child.getNextSibling();
    }
  }

}
