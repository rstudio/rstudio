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

package com.google.gwt.i18n.client;

import com.google.gwt.junit.client.GWTTestCase;

/**
 * GWT JUnit tests must extend GWTTestCase.
 */
public class NumberFormat_fr_Test extends GWTTestCase {

  /**
   * Must refer to a valid module that inherits from com.google.gwt.junit.JUnit.
   */
  @Override
  public String getModuleName() {
    return "com.google.gwt.i18n.I18NTest_fr";
  }

  /**
   * Add as many tests as you like.
   */
  public void testBasicFormat() {
    String str = NumberFormat.getFormat("0.0000").format(123.45789179565757f);
    assertEquals("123,4579", str);
  }

  public void testCurrency() {
    String str;

    str = NumberFormat.getFormat("\u00a4#,##0.00;-\u00a4#,##0.00").format(
        1234.56);
    assertEquals("\u20AC1\u00A0234,56", str);
    str = NumberFormat.getFormat("\u00a4#,##0.00;-\u00a4#,##0.00").format(
        -1234.56);
    assertEquals("-\u20AC1\u00A0234,56", str);

    str = NumberFormat.getFormat("\u00a4\u00a4 #,##0.00;-\u00a4\u00a4 #,##0.00").format(
        1234.56);
    assertEquals("EUR 1\u00A0234,56", str);
    str = NumberFormat.getFormat("\u00a4\u00a4 #,##0.00;\u00a4\u00a4 -#,##0.00").format(
        -1234.56);
    assertEquals("EUR -1\u00A0234,56", str);

    NumberFormat formatter = NumberFormat.getFormat(
        "\u00a4#,##0.00;-\u00a4#,##0.00", "BRL");
    str = formatter.format(1234.56);
    assertEquals("R$1\u00A0234,56", str);
    str = formatter.format(-1234.56);
    assertEquals("-R$1\u00A0234,56", str);

    formatter = NumberFormat.getFormat(
        "\u00a4\u00a4 #,##0.00;(\u00a4\u00a4 #,##0.00)", "BRL");
    str = formatter.format(1234.56);
    assertEquals("BRL 1\u00A0234,56", str);
    str = formatter.format(-1234.56);
    assertEquals("(BRL 1\u00A0234,56)", str);
  }

  public void testExponential() {
    String str;

    str = NumberFormat.getFormat("0.####E0").format(0.01234);
    assertEquals("1,234E-2", str);
    str = NumberFormat.getFormat("00.000E00").format(0.01234);
    assertEquals("12,340E-03", str);
    str = NumberFormat.getFormat("##0.######E000").format(0.01234);
    assertEquals("12,34E-003", str);
    str = NumberFormat.getFormat("0.###E0;[0.###E0]").format(0.01234);
    assertEquals("1,234E-2", str);

    str = NumberFormat.getFormat("0.####E0").format(123456789);
    assertEquals("1,2346E8", str);
    str = NumberFormat.getFormat("00.000E00").format(123456789);
    assertEquals("12,346E07", str);
    str = NumberFormat.getFormat("##0.######E000").format(123456789);
    assertEquals("123,456789E006", str);
    str = NumberFormat.getFormat("0.###E0;[0.###E0]").format(123456789);
    assertEquals("1,235E8", str);

    str = NumberFormat.getFormat("0.####E0").format(1.23e300);
    assertEquals("1,23E300", str);
    str = NumberFormat.getFormat("00.000E00").format(1.23e300);
    assertEquals("12,300E299", str);
    str = NumberFormat.getFormat("##0.######E000").format(1.23e300);
    assertEquals("1,23E300", str);
    str = NumberFormat.getFormat("0.###E0;[0.###E0]").format(1.23e300);
    assertEquals("1,23E300", str);

    str = NumberFormat.getFormat("0.####E0").format(-3.141592653e-271);
    assertEquals("-3,1416E-271", str);
    str = NumberFormat.getFormat("00.000E00").format(-3.141592653e-271);
    assertEquals("-31,416E-272", str);
    str = NumberFormat.getFormat("##0.######E000").format(-3.141592653e-271);
    assertEquals("-314,159265E-273", str);
    str = NumberFormat.getFormat("0.###E0;[0.###E0]").format(-3.141592653e-271);
    assertEquals("[3,142E-271]", str);

    str = NumberFormat.getFormat("0.####E0").format(0);
    assertEquals("0E0", str);
    str = NumberFormat.getFormat("00.000E00").format(0);
    assertEquals("00,000E00", str);
    str = NumberFormat.getFormat("##0.######E000").format(0);
    assertEquals("0E000", str);
    str = NumberFormat.getFormat("0.###E0;[0.###E0]").format(0);
    assertEquals("0E0", str);

    str = NumberFormat.getFormat("0.####E0").format(-1);
    assertEquals("-1E0", str);
    str = NumberFormat.getFormat("00.000E00").format(-1);
    assertEquals("-10,000E-01", str);
    str = NumberFormat.getFormat("##0.######E000").format(-1);
    assertEquals("-1E000", str);
    str = NumberFormat.getFormat("0.###E0;[0.###E0]").format(-1);
    assertEquals("[1E0]", str);

    str = NumberFormat.getFormat("0.####E0").format(1);
    assertEquals("1E0", str);
    str = NumberFormat.getFormat("00.000E00").format(1);
    assertEquals("10,000E-01", str);
    str = NumberFormat.getFormat("##0.######E000").format(1);
    assertEquals("1E000", str);
    str = NumberFormat.getFormat("0.###E0;[0.###E0]").format(1);
    assertEquals("1E0", str);

    str = NumberFormat.getFormat("#E0").format(12345.0);
    assertEquals("1E4", str);
    str = NumberFormat.getFormat("0E0").format(12345.0);
    assertEquals("1E4", str);
    str = NumberFormat.getFormat("##0.###E0").format(12345.0);
    assertEquals("12,345E3", str);
    str = NumberFormat.getFormat("##0.###E0").format(12345.00001);
    assertEquals("12,345E3", str);
    str = NumberFormat.getFormat("##0.###E0").format(12345);
    assertEquals("12,345E3", str);

    str = NumberFormat.getFormat("##0.####E0").format(789.12346e-9);
    assertEquals("789,1235E-9", str);
    str = NumberFormat.getFormat("##0.####E0").format(780.e-9);
    assertEquals("780E-9", str);
    str = NumberFormat.getFormat(".###E0").format(45678.0);
    assertEquals(",457E5", str);
    str = NumberFormat.getFormat(".###E0").format(0);
    assertEquals(",0E0", str);

    str = NumberFormat.getFormat("#E0").format(45678000);
    assertEquals("5E7", str);
    str = NumberFormat.getFormat("##E0").format(45678000);
    assertEquals("46E6", str);
    str = NumberFormat.getFormat("####E0").format(45678000);
    assertEquals("4568E4", str);
    str = NumberFormat.getFormat("0E0").format(45678000);
    assertEquals("5E7", str);
    str = NumberFormat.getFormat("00E0").format(45678000);
    assertEquals("46E6", str);
    str = NumberFormat.getFormat("000E0").format(45678000);
    assertEquals("457E5", str);
    str = NumberFormat.getFormat("###E0").format(0.0000123);
    assertEquals("12E-6", str);
    str = NumberFormat.getFormat("###E0").format(0.000123);
    assertEquals("123E-6", str);
    str = NumberFormat.getFormat("###E0").format(0.00123);
    assertEquals("1E-3", str);
    str = NumberFormat.getFormat("###E0").format(0.0123);
    assertEquals("12E-3", str);
    str = NumberFormat.getFormat("###E0").format(0.123);
    assertEquals("123E-3", str);
    str = NumberFormat.getFormat("###E0").format(1.23);
    assertEquals("1E0", str);
    str = NumberFormat.getFormat("###E0").format(12.3);
    assertEquals("12E0", str);
    str = NumberFormat.getFormat("###E0").format(123.0);
    assertEquals("123E0", str);
    str = NumberFormat.getFormat("###E0").format(1230.0);
    assertEquals("1E3", str);
  }

  public void testGrouping() {
    String str;

    str = NumberFormat.getFormat("#,###").format(1234567890);
    assertEquals("1\u00a0234\u00a0567\u00a0890", str);
    str = NumberFormat.getFormat("#,####").format(1234567890);
    assertEquals("12\u00a03456\u00a07890", str);

    str = NumberFormat.getFormat("#").format(1234567890);
    assertEquals("1234567890", str);
  }

  public void testForceLatin() {
    assertFalse(NumberFormat.forcedLatinDigits());
    NumberFormat.setForcedLatinDigits(true);
    assertTrue(NumberFormat.forcedLatinDigits());
    NumberFormat decLatin = NumberFormat.getDecimalFormat();
    assertEquals("1\u00A0003,14", decLatin.format(1003.14));
    assertEquals("-1\u00A0003,14", decLatin.format(-1003.14));
    NumberFormat.setForcedLatinDigits(false);
    assertFalse(NumberFormat.forcedLatinDigits());
    assertEquals("3,14", decLatin.format(3.14));
    NumberFormat unforced = NumberFormat.getDecimalFormat();
    assertEquals("3,14", unforced.format(3.14));
  }

  public void testPerMill() {
    String str;

    str = NumberFormat.getFormat("###.###\u2030").format(0.4857);
    assertEquals("485,7\u2030", str);
  }

  public void testQuotes() {
    String str;

    str = NumberFormat.getFormat("a'fo''o'b#").format(123);
    assertEquals("afo'ob123", str);

    str = NumberFormat.getFormat("a''b#").format(123);
    assertEquals("a'b123", str);
  }

  public void testStandardFormat() {
    String str;

    str = NumberFormat.getCurrencyFormat().format(1234.579);
    assertEquals("1\u00A0234,58\u00A0\u20AC", str);
    str = NumberFormat.getDecimalFormat().format(1234.579);
    assertEquals("1\u00A0234,579", str);
    str = NumberFormat.getPercentFormat().format(1234.579);
    assertEquals("123\u00A0458\u00A0%", str);
    str = NumberFormat.getScientificFormat().format(1234.579);
    assertEquals("1E3", str);
  }

  public void testZeros() {
    String str;

    str = NumberFormat.getFormat("#.#").format(0);
    assertEquals("0", str);
    str = NumberFormat.getFormat("#.").format(0);
    assertEquals("0,", str);
    str = NumberFormat.getFormat(".#").format(0);
    assertEquals(",0", str);
    str = NumberFormat.getFormat("#").format(0);
    assertEquals("0", str);

    str = NumberFormat.getFormat("#0.#").format(0);
    assertEquals("0", str);
    str = NumberFormat.getFormat("#0.").format(0);
    assertEquals("0,", str);
    str = NumberFormat.getFormat("#.0").format(0);
    assertEquals(",0", str);
    str = NumberFormat.getFormat("#").format(0);
    assertEquals("0", str);
  }
}
