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
  public String getModuleName() {
    return "com.google.gwt.i18n.I18NTest_fr";
  }

  /**
   * Add as many tests as you like.
   */
  public void testBasicFormat() {
    String str = NumberFormat.getFormat("0.0000").format(
        123.45789179565757f);
    assertTrue(str.equals("123,4579"));
  }

  public void testCurrency() {
    String str;

    str = NumberFormat.getFormat("\u00a4#,##0.00;-\u00a4#,##0.00").format(
        1234.56);
    assertTrue(str.equals("\u20AC1\u00A0234,56"));
    str = NumberFormat.getFormat("\u00a4#,##0.00;-\u00a4#,##0.00").format(
        -1234.56);
    assertTrue(str.equals("-\u20AC1\u00A0234,56"));

    str = NumberFormat.getFormat(
        "\u00a4\u00a4 #,##0.00;-\u00a4\u00a4 #,##0.00").format(1234.56);
    assertTrue(str.equals("EUR 1\u00A0234,56"));
    str = NumberFormat.getFormat(
        "\u00a4\u00a4 #,##0.00;\u00a4\u00a4 -#,##0.00").format(-1234.56);
    assertTrue(str.equals("EUR -1\u00A0234,56"));

    NumberFormat formatter = NumberFormat.getFormat(
        "\u00a4#,##0.00;-\u00a4#,##0.00", "BRL");
    str = formatter.format(1234.56);
    assertTrue(str.equals("R$1\u00A0234,56"));
    str = formatter.format(-1234.56);
    assertTrue(str.equals("-R$1\u00A0234,56"));

    formatter = NumberFormat.getFormat(
        "\u00a4\u00a4 #,##0.00;(\u00a4\u00a4 #,##0.00)", "BRL");
    str = formatter.format(1234.56);
    assertTrue(str.equals("BRL 1\u00A0234,56"));
    str = formatter.format(-1234.56);
    assertTrue(str.equals("(BRL 1\u00A0234,56)"));
  }

  public void testExponential() {
    String str;

    str = NumberFormat.getFormat("0.####E0").format(0.01234);
    assertTrue(str.equals("1,234E-2"));
    str = NumberFormat.getFormat("00.000E00").format(0.01234);
    assertTrue(str.equals("12,340E-03"));
    str = NumberFormat.getFormat("##0.######E000").format(0.01234);
    assertTrue(str.equals("12,34E-003"));
    str = NumberFormat.getFormat("0.###E0;[0.###E0]").format(0.01234);
    assertTrue(str.equals("1,234E-2"));

    str = NumberFormat.getFormat("0.####E0").format(123456789);
    assertTrue(str.equals("1,2346E8"));
    str = NumberFormat.getFormat("00.000E00").format(123456789);
    assertTrue(str.equals("12,346E07"));
    str = NumberFormat.getFormat("##0.######E000").format(123456789);
    assertTrue(str.equals("123,456789E006"));
    str = NumberFormat.getFormat("0.###E0;[0.###E0]").format(123456789);
    assertTrue(str.equals("1,235E8"));

    str = NumberFormat.getFormat("0.####E0").format(1.23e300);
    assertTrue(str.equals("1,23E300"));
    str = NumberFormat.getFormat("00.000E00").format(1.23e300);
    assertTrue(str.equals("12,300E299"));
    str = NumberFormat.getFormat("##0.######E000").format(1.23e300);
    assertTrue(str.equals("1,23E300"));
    str = NumberFormat.getFormat("0.###E0;[0.###E0]").format(1.23e300);
    assertTrue(str.equals("1,23E300"));

    str = NumberFormat.getFormat("0.####E0").format(-3.141592653e-271);
    assertTrue(str.equals("-3,1416E-271"));
    str = NumberFormat.getFormat("00.000E00").format(-3.141592653e-271);
    assertTrue(str.equals("-31,416E-272"));
    str = NumberFormat.getFormat("##0.######E000").format(
        -3.141592653e-271);
    assertTrue(str.equals("-314,159265E-273"));
    str = NumberFormat.getFormat("0.###E0;[0.###E0]").format(
        -3.141592653e-271);
    assertTrue(str.equals("[3,142E-271]"));

    str = NumberFormat.getFormat("0.####E0").format(0);
    assertTrue(str.equals("0E0"));
    str = NumberFormat.getFormat("00.000E00").format(0);
    assertTrue(str.equals("00,000E00"));
    str = NumberFormat.getFormat("##0.######E000").format(0);
    assertTrue(str.equals("0E000"));
    str = NumberFormat.getFormat("0.###E0;[0.###E0]").format(0);
    assertTrue(str.equals("0E0"));

    str = NumberFormat.getFormat("0.####E0").format(-1);
    assertTrue(str.equals("-1E0"));
    str = NumberFormat.getFormat("00.000E00").format(-1);
    assertTrue(str.equals("-10,000E-01"));
    str = NumberFormat.getFormat("##0.######E000").format(-1);
    assertTrue(str.equals("-1E000"));
    str = NumberFormat.getFormat("0.###E0;[0.###E0]").format(-1);
    assertTrue(str.equals("[1E0]"));

    str = NumberFormat.getFormat("0.####E0").format(1);
    assertTrue(str.equals("1E0"));
    str = NumberFormat.getFormat("00.000E00").format(1);
    assertTrue(str.equals("10,000E-01"));
    str = NumberFormat.getFormat("##0.######E000").format(1);
    assertTrue(str.equals("1E000"));
    str = NumberFormat.getFormat("0.###E0;[0.###E0]").format(1);
    assertTrue(str.equals("1E0"));

    str = NumberFormat.getFormat("#E0").format(12345.0);
    assertTrue(str.equals("1E4"));
    str = NumberFormat.getFormat("0E0").format(12345.0);
    assertTrue(str.equals("1E4"));
    str = NumberFormat.getFormat("##0.###E0").format(12345.0);
    assertTrue(str.equals("12,345E3"));
    str = NumberFormat.getFormat("##0.###E0").format(12345.00001);
    assertTrue(str.equals("12,345E3"));
    str = NumberFormat.getFormat("##0.###E0").format(12345);
    assertTrue(str.equals("12,345E3"));

    str = NumberFormat.getFormat("##0.####E0").format(789.12345e-9);
    assertTrue(str.equals("789,1235E-9"));
    str = NumberFormat.getFormat("##0.####E0").format(780.e-9);
    assertTrue(str.equals("780E-9"));
    str = NumberFormat.getFormat(".###E0").format(45678.0);
    assertTrue(str.equals(",457E5"));
    str = NumberFormat.getFormat(".###E0").format(0);
    assertTrue(str.equals(",0E0"));

    str = NumberFormat.getFormat("#E0").format(45678000);
    assertTrue(str.equals("5E7"));
    str = NumberFormat.getFormat("##E0").format(45678000);
    assertTrue(str.equals("46E6"));
    str = NumberFormat.getFormat("####E0").format(45678000);
    assertTrue(str.equals("4568E4"));
    str = NumberFormat.getFormat("0E0").format(45678000);
    assertTrue(str.equals("5E7"));
    str = NumberFormat.getFormat("00E0").format(45678000);
    assertTrue(str.equals("46E6"));
    str = NumberFormat.getFormat("000E0").format(45678000);
    assertTrue(str.equals("457E5"));
    str = NumberFormat.getFormat("###E0").format(0.0000123);
    assertTrue(str.equals("12E-6"));
    str = NumberFormat.getFormat("###E0").format(0.000123);
    assertTrue(str.equals("123E-6"));
    str = NumberFormat.getFormat("###E0").format(0.00123);
    assertTrue(str.equals("1E-3"));
    str = NumberFormat.getFormat("###E0").format(0.0123);
    assertTrue(str.equals("12E-3"));
    str = NumberFormat.getFormat("###E0").format(0.123);
    assertTrue(str.equals("123E-3"));
    str = NumberFormat.getFormat("###E0").format(1.23);
    assertTrue(str.equals("1E0"));
    str = NumberFormat.getFormat("###E0").format(12.3);
    assertTrue(str.equals("12E0"));
    str = NumberFormat.getFormat("###E0").format(123.0);
    assertTrue(str.equals("123E0"));
    str = NumberFormat.getFormat("###E0").format(1230.0);
    assertTrue(str.equals("1E3"));
  }

  public void testGrouping() {
    String str;

    str = NumberFormat.getFormat("#,###").format(1234567890);
    assertTrue(str.equals("1\u00a0234\u00a0567\u00a0890"));
    str = NumberFormat.getFormat("#,####").format(1234567890);
    assertTrue(str.equals("12\u00a03456\u00a07890"));

    str = NumberFormat.getFormat("#").format(1234567890);
    assertTrue(str.equals("1234567890"));
  }

  public void testPerMill() {
    String str;

    str = NumberFormat.getFormat("###.###\u2030").format(0.4857);
    assertTrue(str.equals("485,7\u2030"));
  }

  public void testQuotes() {
    String str;

    str = NumberFormat.getFormat("a'fo''o'b#").format(123);
    assertTrue(str.equals("afo'ob123"));

    str = NumberFormat.getFormat("a''b#").format(123);
    assertTrue(str.equals("a'b123"));
  }

  public void testStandardFormat() {
    String str;

    str = NumberFormat.getCurrencyFormat().format(1234.579);
    assertTrue(str.equals("1\u00A0234,58 \u20AC"));
    str = NumberFormat.getDecimalFormat().format(1234.579);
    assertTrue(str.equals("1\u00A0234,579"));
    str = NumberFormat.getPercentFormat().format(1234.579);
    assertTrue(str.equals("123\u00A0458\u00A0%"));
    str = NumberFormat.getScientificFormat().format(1234.579);
    assertTrue(str.equals("1,235E3"));
  }

  public void testZeros() {
    String str;

    str = NumberFormat.getFormat("#.#").format(0);
    assertTrue(str.equals("0"));
    str = NumberFormat.getFormat("#.").format(0);
    assertTrue(str.equals("0,"));
    str = NumberFormat.getFormat(".#").format(0);
    assertTrue(str.equals(",0"));
    str = NumberFormat.getFormat("#").format(0);
    assertTrue(str.equals("0"));

    str = NumberFormat.getFormat("#0.#").format(0);
    assertTrue(str.equals("0"));
    str = NumberFormat.getFormat("#0.").format(0);
    assertTrue(str.equals("0,"));
    str = NumberFormat.getFormat("#.0").format(0);
    assertTrue(str.equals(",0"));
    str = NumberFormat.getFormat("#").format(0);
    assertTrue(str.equals("0"));
  }
}
