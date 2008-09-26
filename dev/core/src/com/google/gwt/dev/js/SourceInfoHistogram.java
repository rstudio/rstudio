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
package com.google.gwt.dev.js;

import com.google.gwt.dev.jjs.HasSourceInfo;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsVisitable;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.TextOutput;
import com.google.gwt.dev.util.Util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeMap;

/**
 * This is a test reporting visitor for SOYC experiments. It will likely
 * disappear once a proper export format and viewer application are written.
 */
public class SourceInfoHistogram {
  /**
   * Stub; unused and will probably be discarded.
   */
  public static class HistogramData {
    // Use weak references to AST nodes. If the AST node gets pruned, we won't
    // need it either
  }

  private static class HSVUtils {
    private static final String[] VALUES = {
        "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d",
        "e", "f"};

    /**
     * The number of degrees to advance around H-space.
     */
    private static final int DEGREES = 53;
    private static final double SATURATION = 0.2;
    private static final double VALUE = 0.9;

    /**
     * We go around and around the HSV color space to generate colors.
     * 
     * @param color The index of the item to be colored.
     * @return An RGB in hex format for the color.
     */
    public static String color(int color) {
      final int h = (DEGREES * color) % 360;
      final double s = SATURATION;
      final double v = VALUE;

      final int hi = (int) Math.floor(h / 60.) % 6;
      final double f = (h / 60.) - hi;
      final double p = v * (1 - s);
      final double q = v * (1 - f * s);
      final double t = v * (1 - (1 - f) * s);

      final double r, g, b;
      switch (hi) {
        case 0:
          r = v;
          g = t;
          b = p;
          break;
        case 1:
          r = q;
          g = v;
          b = p;
          break;
        case 2:
          r = p;
          g = v;
          b = t;
          break;
        case 3:
          r = p;
          g = q;
          b = v;
          break;
        case 4:
          r = t;
          g = p;
          b = v;
          break;
        case 5:
          r = v;
          g = p;
          b = q;
          break;
        default:
          throw new RuntimeException("Unexpected hi of " + hi);
      }

      return numberToHex(r) + numberToHex(g) + numberToHex(b);
    }

    /**
     * Convert a number between 0 and 1 to a two-char hex value between 00 and
     * FF.
     */
    private static String numberToHex(double number) {
      number *= 255;
      if (number <= 0) {
        return "00";
      } else if (number >= 255) {
        return "FF";
      }

      String toReturn = VALUES[(int) number / 16] + VALUES[(int) number % 16];
      return toReturn;
    }

    /**
     * Utility class, no public constructor.
     */
    private HSVUtils() {
    }
  }

  private static class HtmlTextOutput implements TextOutput {
    private final DefaultTextOutput out;

    public HtmlTextOutput(boolean compact) {
      out = new DefaultTextOutput(compact);
    }

    public void indentIn() {
      out.indentIn();
    }

    public void indentOut() {
      out.indentOut();
    }

    public void newline() {
      out.newline();
    }

    public void newlineOpt() {
      out.newlineOpt();
    }

    public void print(char c) {
      print(String.valueOf(c));
    }

    public void print(char[] s) {
      print(String.valueOf(s));
    }

    public void print(String s) {
      out.print(Util.escapeXml(s));
    }

    public void printOpt(char c) {
      printOpt(String.valueOf(c));
    }

    public void printOpt(char[] s) {
      printOpt(String.valueOf(s));
    }

    public void printOpt(String s) {
      out.printOpt(Util.escapeXml(s));
    }

    public void printRaw(String s) {
      out.print(s);
    }

    @Override
    public String toString() {
      return out.toString();
    }
  }

  private static class JavaNormalReportVisitor extends
      JsSourceGenerationVisitor {
    Stack<HasSourceInfo> stack = new Stack<HasSourceInfo>();
    int total = 0;
    Map<String, Integer> totalsByFileName = new HashMap<String, Integer>();

    private final SortedMap<SourceInfo, StringBuilder> infoToSource = new TreeMap<SourceInfo, StringBuilder>(
        SOURCE_INFO_COMPARATOR);
    private final SwitchTextOutput out;

    public JavaNormalReportVisitor(SwitchTextOutput out) {
      super(out);
      this.out = out;
    }

    @Override
    protected <T extends JsVisitable<T>> T doAccept(T node) {
      boolean openContext = node instanceof HasSourceInfo;
      SourceInfo sourceInfo = null;
      try {
        if (openContext) {
          sourceInfo = ((HasSourceInfo) node).getSourceInfo();
          if (!stack.isEmpty()) {
            int count = commit(stack.peek(), true);
            accumulateTotal(sourceInfo, count);
          }
          stack.push((HasSourceInfo) node);
          out.begin();
        }
        return super.doAccept(node);
      } finally {
        if (openContext) {
          int count = commit((HasSourceInfo) node, false);
          accumulateTotal(sourceInfo, count);
          if (stack.pop() != node) {
            throw new RuntimeException("Unexpected node popped");
          }
        }
      }
    }

    @Override
    protected <T extends JsVisitable<T>> void doAcceptList(List<T> collection) {
      for (T node : collection) {
        doAccept(node);
      }
    }

    @Override
    protected JsExpression doAcceptLvalue(JsExpression expr) {
      return doAccept(expr);
    }

    @Override
    protected <T extends JsVisitable<T>> void doAcceptWithInsertRemove(
        List<T> collection) {
      doAcceptList(collection);
    }

    private void accumulateTotal(SourceInfo sourceInfo, int count) {
      for (SourceInfo root : sourceInfo.getRoots()) {
        String fileName = root.getFileName();
        Integer sourceTotal = totalsByFileName.get(fileName);
        if (sourceTotal == null) {
          totalsByFileName.put(fileName, count);
        } else {
          totalsByFileName.put(fileName, sourceTotal + count);
        }
      }
      total += count;
    }

    private int commit(HasSourceInfo x, boolean expectMore) {
      StringBuilder builder = infoToSource.get(x.getSourceInfo());
      if (builder == null) {
        builder = new StringBuilder();
        infoToSource.put(x.getSourceInfo(), builder);
      }
      if (expectMore) {
        return out.flush(builder);
      } else {
        return out.commit(builder);
      }
    }
  }

  private static class JsNormalReportVisitor extends JsSourceGenerationVisitor {
    private final HtmlTextOutput out;
    private final Stack<SourceInfo> context = new Stack<SourceInfo>();

    public JsNormalReportVisitor(HtmlTextOutput out) {
      super(out);
      this.out = out;
    }

    @Override
    protected <T extends JsVisitable<T>> T doAccept(T node) {
      boolean openNode = false;
      if (node instanceof HasSourceInfo) {
        SourceInfo info = ((HasSourceInfo) node).getSourceInfo();
        openNode = context.isEmpty()
            || SOURCE_INFO_COMPARATOR.compare(context.peek(), info) != 0;
        if (openNode) {
          String color;
          if (context.contains(info)) {
            color = HSVUtils.color(context.indexOf(info));
          } else {
            color = HSVUtils.color(context.size());
          }
          context.push(info);
          out.printRaw("<div class=\"node\" style=\"background:#" + color
              + ";\">");
          out.printRaw("<div class=\"story\">");
          out.print(info.getStory());
          out.printRaw("</div>");
        }
      }
      T toReturn = super.doAccept(node);
      if (openNode) {
        out.printRaw("</div>");
        context.pop();
      }
      return toReturn;
    }

    @Override
    protected <T extends JsVisitable<T>> void doAcceptList(List<T> collection) {
      for (T node : collection) {
        doAccept(node);
      }
    }

    @Override
    protected JsExpression doAcceptLvalue(JsExpression expr) {
      return doAccept(expr);
    }

    @Override
    protected <T extends JsVisitable<T>> void doAcceptWithInsertRemove(
        List<T> collection) {
      doAcceptList(collection);
    }
  }

  private static class SwitchTextOutput implements TextOutput {
    private Stack<DefaultTextOutput> outs = new Stack<DefaultTextOutput>();

    public void begin() {
      outs.push(new DefaultTextOutput(true));
    }

    public int commit(StringBuilder build) {
      String string = outs.pop().toString();
      build.append(string);
      return string.length();
    }

    public int flush(StringBuilder build) {
      int toReturn = commit(build);
      begin();
      return toReturn;
    }

    public void indentIn() {
      // outs.peek().indentIn();
    }

    public void indentOut() {
      // outs.peek().indentOut();
    }

    public void newline() {
      outs.peek().newline();
    }

    public void newlineOpt() {
      outs.peek().newlineOpt();
    }

    public void print(char c) {
      outs.peek().print(c);
    }

    public void print(char[] s) {
      outs.peek().print(s);
    }

    public void print(String s) {
      outs.peek().print(s);
    }

    public void printOpt(char c) {
      outs.peek().printOpt(c);
    }

    public void printOpt(char[] s) {
      outs.peek().printOpt(s);
    }

    public void printOpt(String s) {
      outs.peek().printOpt(s);
    }
  }

  private static final Comparator<SourceInfo> SOURCE_INFO_COMPARATOR = new Comparator<SourceInfo>() {
    public int compare(SourceInfo o1, SourceInfo o2) {
      int toReturn = o1.getFileName().compareTo(o2.getFileName());
      if (toReturn != 0) {
        return toReturn;
      }

      toReturn = o1.getStartLine() - o2.getStartLine();
      if (toReturn != 0) {
        return toReturn;
      }

      // TODO need a counter in SourceInfos
      return o1.getStory().compareTo(o2.getStory());
    }
  };

  public static HistogramData exec(JProgram program) {
    return new HistogramData();
  }

  public static void exec(JsProgram program, HistogramData data,
      String outputPath) {
    writeJavaNormalReport(program, outputPath);
    writeJsNormalReport(program, outputPath);
  }

  private static void writeJavaNormalReport(JsProgram program, String outputPath) {
    JavaNormalReportVisitor v = new JavaNormalReportVisitor(
        new SwitchTextOutput());
    v.accept(program);

    // Concatenate the per-SourceInfo data into per-file contents
    Map<String, StringBuffer> contentsByFile = new TreeMap<String, StringBuffer>();
    for (Map.Entry<SourceInfo, StringBuilder> contents : v.infoToSource.entrySet()) {
      SourceInfo sourceInfo = contents.getKey();
      String currentFile = sourceInfo.getFileName();
      StringBuffer buffer = contentsByFile.get(currentFile);
      if (buffer == null) {
        buffer = new StringBuffer();
        contentsByFile.put(currentFile, buffer);
        buffer.append("<div class=\"fileHeader\">\n");
        buffer.append(Util.escapeXml(String.format("%s : %2.1f%%", currentFile,
            (100.0 * v.totalsByFileName.get(currentFile) / v.total))));
        buffer.append("</div>\n");
      }

      buffer.append("<div class=\"jsLine\">");
      buffer.append("<div class=\"story\">");
      buffer.append(Util.escapeXml(sourceInfo.getStory()));
      buffer.append("</div>");
      buffer.append(Util.escapeXml(contents.getValue().toString()));
      buffer.append("</div>\n");
    }

    // Order the contents based on file size
    Map<Integer, StringBuffer> orderedContents = new TreeMap<Integer, StringBuffer>();
    for (Map.Entry<String, StringBuffer> entry : contentsByFile.entrySet()) {
      int size = -v.totalsByFileName.get(entry.getKey());
      StringBuffer appendTo = orderedContents.get(size);
      if (appendTo != null) {
        appendTo.append(entry.getValue());
      } else {
        orderedContents.put(size, entry.getValue());
      }
    }

    PrintWriter out;
    try {
      File outputPathDir = new File(outputPath);
      outputPathDir.mkdirs();
      out = new PrintWriter(new FileWriter(File.createTempFile("soyc",
          "-java.html", outputPathDir)));
    } catch (IOException e) {
      out = null;
    }

    out.println("<html><head>");
    out.println("<style>"
        + "* {font-family: monospace;}"
        + ".file {clear: both;}"
        + ".file * {display: none;}"
        + ".file .fileHeader {display: block; cursor: pointer;}"
        + ".fileOpen .fileHeader {clear: both;}"
        + ".fileOpen .javaLine {clear: both; float: left; white-space: pre; background: #efe;}"
        + ".fileOpen .jsLine {outline: thin solid black; float: right; clear: right; white-space: pre; background: #ddd;}"
        + ".story {display:none;}"
        + "div.jsLine:hover .story{display:block; position: absolute; left:0; background: #eef;}"
        + "</style>");

    out.println("</head><body>");

    out.println(String.format("<h1>Total bytes: %d</h1>", v.total));
    for (StringBuffer buffer : orderedContents.values()) {
      out.println("<div class=\"file\" onclick=\"this.className=(this.className=='file'?'fileOpen':'file')\">");
      out.println(buffer.toString());
      out.println("</div>");
    }

    out.println("<h1>Done</h1>");
    out.println("</body></html>");
    out.close();
  }

  private static void writeJsNormalReport(JsProgram program, String outputPath) {
    HtmlTextOutput htmlOut = new HtmlTextOutput(false);
    JsNormalReportVisitor v = new JsNormalReportVisitor(htmlOut);
    v.accept(program);

    PrintWriter out;
    try {
      File outputPathDir = new File(outputPath);
      outputPathDir.mkdirs();
      out = new PrintWriter(new FileWriter(File.createTempFile("soyc",
          "-js.html", outputPathDir)));
    } catch (IOException e) {
      out = null;
    }

    out.println("<html><head>");
    out.println("<style>" + "* {white-space: pre; font-family: monospace;}"
        + ".node {display:inline; z-index: 0;}" + ".story {display: none;}"
        + "div.node:hover > .story {"
        + "  display:block; float:right; clear: right; background: inherit; "
        + "  position: relative; border-left: 8px solid white; z-index: 1;}"
        + "</style>");
    out.println("</head><body>");
    out.println(htmlOut.toString());
    out.println("</body></html>");
    out.close();
  }
}
