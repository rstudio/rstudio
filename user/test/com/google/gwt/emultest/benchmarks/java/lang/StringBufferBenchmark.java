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
package com.google.gwt.emultest.benchmarks.java.lang;

import com.google.gwt.benchmarks.client.Benchmark;
import com.google.gwt.benchmarks.client.IntRange;
import com.google.gwt.benchmarks.client.Operator;
import com.google.gwt.benchmarks.client.RangeField;
import com.google.gwt.core.client.JavaScriptObject;

/**
 * A {@link Benchmark} for {@link StringBuilder StringBuilders}. This includes
 * a benchmark from Ray Cromwell that builds display commands in one of two
 * ways. One version uses a StringBuilder, and the other uses raw pushes with
 * JavaScript. Note that there is no actual DisplayList interface, because
 * otherwise the benchmark might have some dynamic dispatch involved. By default
 * this benchmarks only the standard <code>StringBuilder</code> and
 * <code>StringBuffer</code>. To run the full suite, comment in the alternate
 * version of {@link #appendKindsRange}.
 */
public class StringBufferBenchmark extends Benchmark {
  /**
   * The type of StringBuilder to use for a test.
   */
  protected enum SBType {
    JS("raw JavaScrpt"), STRBLD("StringBuilder"), STRBUF("StringBuffer");

    public String description;

    private SBType(String description) {
      this.description = description;
    }
  }

  /**
   * A DisplayList represented using a native JavaScript array, and updated via
   * the JavaScript push() method.
   */
  @SuppressWarnings("unused")
  private static class JSArrayDisplayList {
    private JavaScriptObject jso = JavaScriptObject.createArray();

    public void begin() {
      jso = JavaScriptObject.createArray();
    }

    public native void cmd(String cmd) /*-{
      this.@com.google.gwt.emultest.benchmarks.java.lang.StringBufferBenchmark$JSArrayDisplayList::jso.push(cmd, 0);
    }-*/;

    public native void cmd(String cmd, int a) /*-{
      this.@com.google.gwt.emultest.benchmarks.java.lang.StringBufferBenchmark$JSArrayDisplayList::jso.push(cmd, 1, a);
    }-*/;

    public native void cmd(String cmd, int a, int b) /*-{
      this.@com.google.gwt.emultest.benchmarks.java.lang.StringBufferBenchmark$JSArrayDisplayList::jso.push(cmd, 2, a, b);
    }-*/;

    public native void cmd(String cmd, int a, int b, int c) /*-{
      this.@com.google.gwt.emultest.benchmarks.java.lang.StringBufferBenchmark$JSArrayDisplayList::jso.push(cmd, 3, a, b, c);
    }-*/;

    public native String end() /*-{
      return this.@com.google.gwt.emultest.benchmarks.java.lang.StringBufferBenchmark$JSArrayDisplayList::jso.join('');
    }-*/;

    public void fill() {
      cmd("F");
    }

    public void lineTo(int x, int y) {
      cmd("L", 0, 0);
    }

    public void moveTo(int x, int y) {
      cmd("M", 0, 0);
    }

    public void rotate(int angle) {
      cmd("R", angle);
    }

    public void stroke() {
      cmd("S");
    }

    public void translate(int x, int y) {
      cmd("T", x, y);
    }
  }

  /**
   * A DisplayList represented as a {@link StringBuffer} of commands and
   * arguments. To contrast, see {@link JSArrayDisplayList}.
   */
  @SuppressWarnings("unused")
  private static class StringBufferDisplayList {
    private StringBuffer strbld = new StringBuffer();

    public void begin() {
      strbld = new StringBuffer();
    }

    public void cmd(String cmd) {
      strbld.append(cmd);
      strbld.append(0);
    }

    public void cmd(String cmd, int a) {
      strbld.append(cmd);
      strbld.append(1);
      strbld.append(a);
    }

    public void cmd(String cmd, int a, int b) {
      strbld.append(cmd);
      strbld.append(2);
      strbld.append(a);
      strbld.append(b);
    }

    public void cmd(String cmd, int a, int b, int c) {
      strbld.append(cmd);
      strbld.append(3);
      strbld.append(a);
      strbld.append(b);
      strbld.append(c);
    }

    public String end() {
      return strbld.toString();
    }

    public void fill() {
      cmd("F");
    }

    public void lineTo(int x, int y) {
      cmd("L", 0, 0);
    }

    public void moveTo(int x, int y) {
      cmd("M", 0, 0);
    }

    public void rotate(int angle) {
      cmd("R", angle);
    }

    public void stroke() {
      cmd("S");
    }

    public void translate(int x, int y) {
      cmd("T", x, y);
    }
  }

  /**
   * A DisplayList represented as a {@link StringBuilder} of commands and
   * arguments. To contrast, see {@link JSArrayDisplayList}.
   */
  @SuppressWarnings("unused")
  private static class StringBuilderDisplayList {
    private StringBuilder strbld = new StringBuilder();

    public void begin() {
      strbld = new StringBuilder();
    }

    public void cmd(String cmd) {
      strbld.append(cmd);
      strbld.append(0);
    }

    public void cmd(String cmd, int a) {
      strbld.append(cmd);
      strbld.append(1);
      strbld.append(a);
    }

    public void cmd(String cmd, int a, int b) {
      strbld.append(cmd);
      strbld.append(2);
      strbld.append(a);
      strbld.append(b);
    }

    public void cmd(String cmd, int a, int b, int c) {
      strbld.append(cmd);
      strbld.append(3);
      strbld.append(a);
      strbld.append(b);
      strbld.append(c);
    }

    public String end() {
      return strbld.toString();
    }

    public void fill() {
      cmd("F");
    }

    public void lineTo(int x, int y) {
      cmd("L", 0, 0);
    }

    public void moveTo(int x, int y) {
      cmd("M", 0, 0);
    }

    public void rotate(int angle) {
      cmd("R", angle);
    }

    public void stroke() {
      cmd("S");
    }

    public void translate(int x, int y) {
      cmd("T", x, y);
    }
  }

  private static final String P_CLOSE = "</p>";
  private static final String P_OPEN = "<p>";

  public final SBType[] appendKindsRange = new SBType[] {
      SBType.STRBUF, SBType.STRBLD};
  public final IntRange appendTimesRange = new IntRange(32, 4096,
      Operator.MULTIPLY, 2);
  public final SBType[] displayListKindsRange = new SBType[] {
      SBType.STRBUF, SBType.STRBLD, SBType.JS};
  public final IntRange displayListTimesRange = new IntRange(32, 4096,
      Operator.MULTIPLY, 2);

  private volatile String abcde = "abcde";

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuiteBenchmarks";
  }

  /**
   * Needed for JUnit.
   */
  public void testAppend() {
  }

  public void testAppend(@RangeField("appendTimesRange")
  Integer times, @RangeField("appendKindsRange")
  SBType sbtype) {
    int outerTimes = 1;
    switch (sbtype) {
      case STRBLD:
        for (int i = 0; i < outerTimes; i++) {
          appendWithStringBuilder(times);
        }
        break;

      case STRBUF:
        for (int i = 0; i < outerTimes; i++) {
          appendWithStringBuffer(times);
        }
        break;
    }
  }

  /**
   * Needed for JUnit.
   */
  public void testDisplayList() {
  }

  /**
   * Test creating a display list command sequence.
   */
  public void testDisplayList(@RangeField("displayListTimesRange")
  Integer times, @RangeField("displayListKindsRange")
  SBType sbtype) {
    switch (sbtype) {
      case JS:
        drawWithJSArrayDisplayList(times);
        break;

      case STRBUF:
        drawWithStringBufferDisplayList(times);
        break;

      case STRBLD:
        drawWithStringBuilderDisplayList(times);
        break;
    }
  }

  private void appendWithStringBuffer(int times) {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < times; i++) {
      sb.append(P_OPEN);
      sb.append(abcde);
      sb.append(P_CLOSE);
    }
    pretendToUse(sb.toString().length());
  }

  private void appendWithStringBuilder(int times) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < times; i++) {
      sb.append(P_OPEN);
      sb.append(abcde);
      sb.append(P_CLOSE);
    }
    pretendToUse(sb.toString().length());
  }

  /**
   * Test drawing commands using JSArrayDisplayList.
   */
  private void drawWithJSArrayDisplayList(int times) {
    JSArrayDisplayList dl = new JSArrayDisplayList();
    dl.begin();
    for (int i = 0; i < times; i++) {
      // draw a box
      dl.translate(50, 50);
      dl.rotate(45);
      dl.moveTo(0, 0);
      dl.lineTo(100, 0);
      dl.lineTo(100, 100);
      dl.lineTo(0, 100);
      dl.lineTo(0, 0);
      dl.stroke();
      dl.fill();
    }
    pretendToUse(dl.end().length());
  }

  /**
   * Test drawing commands using {@link StringBufferDisplayList}.
   */
  private void drawWithStringBufferDisplayList(int times) {
    final StringBufferDisplayList dl = new StringBufferDisplayList();
    dl.begin();
    for (int i = 0; i < times; i++) {
      // draw a box
      dl.translate(50, 50);
      dl.rotate(45);
      dl.moveTo(0, 0);
      dl.lineTo(100, 0);
      dl.lineTo(100, 100);
      dl.lineTo(0, 100);
      dl.lineTo(0, 0);
      dl.stroke();
      dl.fill();
    }
    pretendToUse(dl.end().length());
  }

  /**
   * Test drawing commands using {@link StringBufferDisplayList}.
   */
  private void drawWithStringBuilderDisplayList(int times) {
    final StringBuilderDisplayList dl = new StringBuilderDisplayList();
    dl.begin();
    for (int i = 0; i < times; i++) {
      // draw a box
      dl.translate(50, 50);
      dl.rotate(45);
      dl.moveTo(0, 0);
      dl.lineTo(100, 0);
      dl.lineTo(100, 100);
      dl.lineTo(0, 100);
      dl.lineTo(0, 0);
      dl.stroke();
      dl.fill();
    }
    pretendToUse(dl.end().length());
  }

  /**
   * Make a value appear to be live, so that dead code elimination will not
   * strip it out.
   */
  private native void pretendToUse(int x) /*-{
    $wnd.completelyUselessField = x
  }-*/;
}
