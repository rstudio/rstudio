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

import com.google.gwt.dev.jjs.Correlation;
import com.google.gwt.dev.jjs.HasSourceInfo;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.Correlation.Axis;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsValueLiteral;
import com.google.gwt.dev.js.ast.JsVisitable;
import com.google.gwt.dev.js.ast.JsVisitor;
import com.google.gwt.dev.util.DefaultTextOutput;
import com.google.gwt.dev.util.HtmlTextOutput;
import com.google.gwt.dev.util.TextOutput;
import com.google.gwt.dev.util.Util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * This is a test reporting visitor for SOYC experiments. It will likely
 * disappear once a proper export format and viewer application are written.
 */
public class SourceInfoHistogram {
  private static class DependencyReportVisitor extends JsVisitor {
    private final Map<Correlation, Set<Correlation>> deps = new TreeMap<Correlation, Set<Correlation>>(Correlation.AXIS_IDENT_COMPARATOR);
    private final Stack<HasSourceInfo> currentContext = new Stack<HasSourceInfo>();

    @Override
    protected <T extends JsVisitable<T>> T doAccept(T node) {
      /*
       * The casts to Object here are because javac 1.5.0_16 doesn't think T
       * could ever be coerced to JsNode.
       */
      boolean createScope = ((Object) node) instanceof JsProgram
          || ((Object) node) instanceof JsFunction;

      if (createScope) {
        currentContext.push((HasSourceInfo) node);
      }

      // JsValueLiterals are shared AST nodes and distort dependency info
      if (!(((Object) node) instanceof JsValueLiteral)
          && !currentContext.isEmpty()) {
        Set<Correlation> toAdd = ((HasSourceInfo) node).getSourceInfo().getAllCorrelations();

        HasSourceInfo context = currentContext.peek();
        for (Correlation c : context.getSourceInfo().getAllCorrelations()) {
          Set<Correlation> set = deps.get(c);
          if (set == null) {
            deps.put(c, set = new TreeSet<Correlation>(Correlation.AXIS_IDENT_COMPARATOR));
          }
          set.addAll(toAdd);
        }
      }

      T toReturn = super.doAccept(node);
      if (createScope) {
        currentContext.pop();
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
    protected <T extends JsVisitable<T>> void doAcceptWithInsertRemove(
        List<T> collection) {
      for (T node : collection) {
        doAccept(node);
      }
    }
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

  private static class JavaNormalReportVisitor extends
      JsSourceGenerationVisitor {
    Stack<HasSourceInfo> stack = new Stack<HasSourceInfo>();
    int total = 0;
    private final Map<Correlation, StringBuilder> sourceByCorrelation = new TreeMap<Correlation, StringBuilder>(Correlation.AXIS_IDENT_COMPARATOR);
    private final Map<Correlation, StringBuilder> sourceByAllCorrelation = new TreeMap<Correlation, StringBuilder>(Correlation.AXIS_IDENT_COMPARATOR);
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
      } catch (RuntimeException e) {
        e.printStackTrace();
        throw e;
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
      total += count;
    }

    private int commit(HasSourceInfo x, boolean expectMore) {
      SourceInfo info = x.getSourceInfo();
      List<StringBuilder> builders = new ArrayList<StringBuilder>();

      // This should be an accurate count
      for (Correlation c : info.getPrimaryCorrelations()) {
        StringBuilder builder = sourceByCorrelation.get(c);
        if (builder == null) {
          builder = new StringBuilder();
          sourceByCorrelation.put(c, builder);
        }
        builders.add(builder);
      }

      /*
       * This intentionally overcounts base classes, methods in order to show
       * aggregate based on subtypes.
       */
      for (Correlation c : info.getAllCorrelations()) {
        StringBuilder builder = sourceByAllCorrelation.get(c);
        if (builder == null) {
          builder = new StringBuilder();
          sourceByAllCorrelation.put(c, builder);
        }
        builders.add(builder);
      }

      if (expectMore) {
        return out.flush(builders);
      } else {
        return out.commit(builders);
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
            || SourceInfo.LOCATION_COMPARATOR.compare(context.peek(), info) != 0;
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

    public int commit(Collection<StringBuilder> builders) {
      String string = outs.pop().toString();
      for (StringBuilder builder : builders) {
        builder.append(string);
      }
      return string.length();
    }

    public int flush(Collection<StringBuilder> builders) {
      int toReturn = commit(builders);
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

  public static void exec(JsProgram program, String outputPath) {
    writeDependencyReport(program, outputPath);
    writeJavaNormalReport(program, outputPath);
    writeJsNormalReport(program, outputPath);
  }

  private static void writeDependencyReport(JsProgram program, String outputPath) {
    DependencyReportVisitor v = new DependencyReportVisitor();
    v.accept(program);

    Map<Correlation, Integer> idents = new HashMap<Correlation, Integer>();
    TreeMap<String, Set<Integer>> clusters = new TreeMap<String, Set<Integer>>();
    for (Correlation c : v.deps.keySet()) {
      if (c.getAxis().equals(Axis.CLASS)) {
        clusters.put(c.getIdent(), new TreeSet<Integer>());
      }
    }

    EnumSet<Axis> toShow = EnumSet.of(Axis.METHOD, Axis.FIELD);

    StringBuilder edges = new StringBuilder();
    StringBuilder nodes = new StringBuilder();
    StringBuilder subgraphs = new StringBuilder();

    for (Map.Entry<Correlation, Set<Correlation>> entry : v.deps.entrySet()) {
      Correlation key = entry.getKey();

      if (!toShow.contains(key.getAxis())
          || key.getIdent().startsWith("java.lang")) {
        continue;
      }

      Set<Integer> keyClusterSet;
      if (!idents.containsKey(key)) {
        idents.put(key, idents.size());
        nodes.append(idents.get(key) + " [label=\"" + key + "\"];\n");
      }
      if (key.getAxis().isJava()) {
        keyClusterSet = clusters.get(clusters.headMap(key.getIdent()).lastKey());
        keyClusterSet.add(idents.get(key));
      } else {
        keyClusterSet = null;
      }

      for (Correlation c : entry.getValue()) {
        if (!toShow.contains(c.getAxis())
            || c.getIdent().startsWith("java.lang")) {
          continue;
        }

        Set<Integer> cClusterSet;
        if (!idents.containsKey(c)) {
          idents.put(c, idents.size());
          nodes.append(idents.get(c) + " [label=\"" + c + "\"];\n");
        }
        if (c.getAxis().isJava()) {
          cClusterSet = clusters.get(clusters.headMap(c.getIdent()).lastKey());
          cClusterSet.add(idents.get(c));
        } else {
          cClusterSet = null;
        }

        edges.append(idents.get(key) + " -> " + idents.get(c));
        if (keyClusterSet == cClusterSet) {
          edges.append(" constraint=false");
        }
        edges.append(";\n");
      }
    }
    int clusterNumber = 0;
    for (Map.Entry<String, Set<Integer>> entry : clusters.entrySet()) {
      Set<Integer> set = entry.getValue();
      if (set.isEmpty()) {
        continue;
      }

      subgraphs.append("subgraph cluster" + clusterNumber++ + " {");
      subgraphs.append("label=\"" + entry.getKey() + "\";");
      for (Integer i : set) {
        subgraphs.append(i + "; ");
      }
      subgraphs.append("};\n");
    }

    PrintWriter out;
    try {
      File outputPathDir = new File(outputPath);
      outputPathDir.mkdirs();
      out = new PrintWriter(new FileWriter(File.createTempFile("soyc",
          "-deps.dot", outputPathDir)));
    } catch (IOException e) {
      out = null;
    }

    out.println("digraph soyc {");
    out.println(subgraphs.toString());
    out.println(nodes.toString());
    out.println(edges.toString());
    out.println("}");
    out.close();
  }

  private static void writeJavaNormalReport(JsProgram program, String outputPath) {
    JavaNormalReportVisitor v = new JavaNormalReportVisitor(
        new SwitchTextOutput());
    v.accept(program);

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
        + ".file .fileHeader, .file .fileHeader * {display: block; cursor: pointer;}"
        + ".fileOpen .fileHeader {clear: both;}"
        + ".fileOpen .javaLine {clear: both; float: left; white-space: pre; background: #efe;}"
        + ".fileOpen .jsLine {outline: thin solid black; float: right; clear: right; white-space: pre; background: #ddd;}"
        + ".story {display:none;}"
        + "div.jsLine:hover .story{display:block; position: absolute; left:0; background: #eef;}"
        + "</style>");

    out.println("</head><body>");

    out.println(String.format("<h1>Total bytes: %d</h1>", v.total));
    Map<Axis, Integer> totalsByAxis = new EnumMap<Axis, Integer>(Axis.class);
    for (Map.Entry<Correlation, StringBuilder> entry : v.sourceByCorrelation.entrySet()) {
      Correlation c = entry.getKey();
      StringBuilder builder = entry.getValue();
      int count = builder.length();
      out.println("<div class=\"file\" onclick=\"this.className=(this.className=='file'?'fileOpen':'file')\">");
      out.println("<div class=\"fileHeader\">" + Util.escapeXml(c.toString())
          + " : " + count + "</div>");
      out.print("<div class=\"jsLine\">");
      out.print(Util.escapeXml(builder.toString()));
      out.print("</div></div>");

      Axis axis = c.getAxis();
      Integer t = totalsByAxis.get(axis);
      if (t == null) {
        totalsByAxis.put(axis, count);
      } else {
        totalsByAxis.put(axis, t + count);
      }
    }

    out.println("<h1>Axis totals</h1>");
    for (Map.Entry<Axis, Integer> entry : totalsByAxis.entrySet()) {
      out.println("<div>" + entry.getKey() + " : " + entry.getValue()
          + "</div>");
    }

    out.println("<h1>Cost of polymorphism</h1>");
    for (Map.Entry<Correlation, StringBuilder> entry : v.sourceByAllCorrelation.entrySet()) {
      Correlation c = entry.getKey();
      StringBuilder builder = entry.getValue();
      int count = builder.length();

      StringBuilder uniqueOutput = v.sourceByCorrelation.get(c);
      int uniqueCount = uniqueOutput == null ? 0 : uniqueOutput.length();
      boolean bold = count != uniqueCount;

      out.println("<div class=\"file\" onclick=\"this.className=(this.className=='file'?'fileOpen':'file')\">");
      out.println("<div class=\"fileHeader\">" + (bold ? "<b>" : "")
          + Util.escapeXml(c.toString()) + " : " + count + " versus "
          + uniqueCount + "(" + (count - uniqueCount) + ")"
          + (bold ? "</b>" : "") + "</div>");
      out.print("<div class=\"jsLine\">");
      out.print(Util.escapeXml(builder.toString()));
      out.print("</div></div>");
    }

    out.println("<h1>Done</h1>");
    out.println("</body></html>");
    out.close();
  }

  private static void writeJsNormalReport(JsProgram program, String outputPath) {

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

    HtmlTextOutput htmlOut = new HtmlTextOutput(out, false);
    JsNormalReportVisitor v = new JsNormalReportVisitor(htmlOut);
    v.accept(program);

    out.println("</body></html>");
    out.close();
  }
}
