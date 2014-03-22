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
package com.google.gwt.i18n.client;

import com.google.gwt.junit.client.GWTTestCase;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Test {@link NumberFormat} in the {@code en} locale.
 */
public class NumberFormat_en_Test extends GWTTestCase {

  /**
   * Must refer to a valid module that inherits from com.google.gwt.junit.JUnit.
   */
  @Override
  public String getModuleName() {
    return "com.google.gwt.i18n.I18NTest_en";
  }

  public void testAPIs() {
    NumberFormat formatter;
    String str;

    formatter = NumberFormat.getFormat("\u00a4#,###.000");
    str = formatter.format(123456.7899);
    assertEquals("$123,456.790", str);

    formatter = NumberFormat.getFormat("\u00a4#,###.000", "BRL");
    str = formatter.format(123456.7899);
    assertEquals("R$123,456.790", str);

    NumberFormat currencyFormat = NumberFormat.getCurrencyFormat();
    str = currencyFormat.format(123456.7899);
    assertEquals("$123,456.79", str);

    formatter = NumberFormat.getFormat(currencyFormat.getPattern(), "BRL");
    str = formatter.format(123456.7899);
    assertEquals("R$123,456.79", str);
  }

  public void testBasicFormat() {
    String str = NumberFormat.getFormat("0.0000").format(123.45789179565757f);
    assertEquals("123.4579", str);

    // tests for overflow of mantissa bits during formatting
    // Low bits are not dependable across browsers, so use .41 instead of
    // .5 and only one decimal digits.
    str = NumberFormat.getFormat("#,##0.0").format(112589990684262.41);
    assertEquals("112,589,990,684,262.4", str);
  }

  public void testBigDecimal() {
    BigDecimal decVal = new BigDecimal("1000000000000000000000000");
    String str = NumberFormat.getFormat("0.000").format(decVal);
    assertEquals("1000000000000000000000000.000", str);

    decVal = decVal.add(new BigDecimal(".1"));
    str = NumberFormat.getFormat("#,##0.000").format(decVal);
    assertEquals("1,000,000,000,000,000,000,000,000.100", str);

    decVal = new BigDecimal(".1499999999999999999999");
    str = NumberFormat.getFormat(".0").format(decVal);
    assertEquals(".1", str);
}

  public void testBigInteger() {
    BigInteger intVal = new BigInteger("1000000000000000000000000");
    String str = NumberFormat.getFormat("#,##0").format(intVal);
    assertEquals("1,000,000,000,000,000,000,000,000", str);

    intVal = intVal.add(BigInteger.ONE);
    str = NumberFormat.getFormat("#,##0").format(intVal);
    assertEquals("1,000,000,000,000,000,000,000,001", str);
  }

  public void testCurrency() {
    String str;

    str = NumberFormat.getFormat("\u00a4#,##0.00;-\u00a4#,##0.00").format(
        1234.56);
    assertEquals("$1,234.56", str);
    str = NumberFormat.getFormat("\u00a4#,##0.00;-\u00a4#,##0.00").format(
        -1234.56);
    assertEquals("-$1,234.56", str);

    str = NumberFormat.getFormat("\u00a4\u00a4 #,##0.00;-\u00a4\u00a4 #,##0.00").format(
        1234.56);
    assertEquals("USD 1,234.56", str);
    str = NumberFormat.getFormat("\u00a4\u00a4 #,##0.00;\u00a4\u00a4 -#,##0.00").format(
        -1234.56);
    assertEquals("USD -1,234.56", str);

    NumberFormat formatter = NumberFormat.getFormat(
        "\u00a4#,##0.00;-\u00a4#,##0.00", "BRL");
    str = formatter.format(1234.56);
    assertEquals("R$1,234.56", str);
    str = formatter.format(-1234.56);
    assertEquals("-R$1,234.56", str);

    formatter = NumberFormat.getFormat(
        "\u00a4\u00a4 #,##0.00;(\u00a4\u00a4 #,##0.00)", "BRL");
    str = formatter.format(1234.56);
    assertEquals("BRL 1,234.56", str);
    str = formatter.format(-1234.56);
    assertEquals("(BRL 1,234.56)", str);

    // Test using a deprecated currency.
    formatter = NumberFormat.getCurrencyFormat("ITL");
    str = formatter.format(1234.556);
    assertEquals("ITL1,235", str);

    // Test simple currency formatting
    str = NumberFormat.getSimpleCurrencyFormat().format(1234.75);
    assertEquals("$1,234.75", str);
    str = NumberFormat.getSimpleCurrencyFormat().format(-1234.75);
    assertEquals("-$1,234.75", str);
    str = NumberFormat.getSimpleCurrencyFormat("CAD").format(1234.75);
    assertEquals("$1,234.75", str);
    str = NumberFormat.getSimpleCurrencyFormat("AUD").format(1234.75);
    assertEquals("$1,234.75", str);
    str = NumberFormat.getSimpleCurrencyFormat("JPY").format(1234.75);
    assertEquals("¥1,235", str);
    str = NumberFormat.getSimpleCurrencyFormat("CNY").format(1234.75);
    assertEquals("¥1,234.75", str);

    // Test global currency formatting
    str = NumberFormat.getGlobalCurrencyFormat().format(1234.75);
    assertEquals("$1,234.75 USD", str);
    str = NumberFormat.getGlobalCurrencyFormat().format(-1234.75);
    assertEquals("-$1,234.75 USD", str);
    str = NumberFormat.getGlobalCurrencyFormat("CAD").format(1234.75);
    assertEquals("$1,234.75 CAD", str);
    str = NumberFormat.getGlobalCurrencyFormat("AUD").format(1234.75);
    assertEquals("$1,234.75 AUD", str);
    str = NumberFormat.getGlobalCurrencyFormat("JPY").format(1234.75);
    assertEquals("¥1,235 JPY", str);
    str = NumberFormat.getGlobalCurrencyFormat("CNY").format(1234.75);
    assertEquals("¥1,234.75 CNY", str);

    // Test overriding the number of fractional digits
    formatter = NumberFormat.getCurrencyFormat("USD").overrideFractionDigits(0);
    str = formatter.format(1234.556);
    assertEquals("$1,235", str);
    formatter = NumberFormat.getCurrencyFormat("USD").overrideFractionDigits(3, 4);
    str = formatter.format(1234.556);
    assertEquals("$1,234.556", str);
    str = formatter.format(1234.55637);
    assertEquals("$1,234.5564", str);
  }

  public void testExponential() {
    String str;

    str = NumberFormat.getFormat("0.####E0").format(0.01234);
    assertEquals("1.234E-2", str);
    str = NumberFormat.getFormat("00.000E00").format(0.01234);
    assertEquals("12.340E-03", str);
    str = NumberFormat.getFormat("##0.######E000").format(0.01234);
    assertEquals("12.34E-003", str);
    str = NumberFormat.getFormat("0.###E0;[0.###E0]").format(0.01234);
    assertEquals("1.234E-2", str);

    str = NumberFormat.getFormat("0.####E0").format(123456789);
    assertEquals("1.2346E8", str);
    str = NumberFormat.getFormat("00.000E00").format(123456789);
    assertEquals("12.346E07", str);
    str = NumberFormat.getFormat("##0.######E000").format(123456789);
    assertEquals("123.456789E006", str);
    str = NumberFormat.getFormat("0.###E0;[0.###E0]").format(123456789);
    assertEquals("1.235E8", str);

    str = NumberFormat.getFormat("0.####E0").format(1.23e300);
    assertEquals("1.23E300", str);
    str = NumberFormat.getFormat("00.000E00").format(1.23e300);
    assertEquals("12.300E299", str);
    str = NumberFormat.getFormat("##0.######E000").format(1.23e300);
    assertEquals("1.23E300", str);
    str = NumberFormat.getFormat("0.###E0;[0.###E0]").format(1.23e300);
    assertEquals("1.23E300", str);

    str = NumberFormat.getFormat("0.####E0").format(-3.141592653e-271);
    assertEquals("-3.1416E-271", str);
    str = NumberFormat.getFormat("00.000E00").format(-3.141592653e-271);
    assertEquals("-31.416E-272", str);
    str = NumberFormat.getFormat("##0.######E000").format(-3.141592653e-271);
    assertEquals("-314.159265E-273", str);
    str = NumberFormat.getFormat("0.###E0;[0.###E0]").format(-3.141592653e-271);
    assertEquals("[3.142E-271]", str);

    str = NumberFormat.getFormat("0.####E0").format(0);
    assertEquals("0E0", str);
    str = NumberFormat.getFormat("00.000E00").format(0);
    assertEquals("00.000E00", str);
    str = NumberFormat.getFormat("##0.######E000").format(0);
    assertEquals("0E000", str);
    str = NumberFormat.getFormat("0.###E0;[0.###E0]").format(0);
    assertEquals("0E0", str);

    str = NumberFormat.getFormat("0.####E0").format(-1);
    assertEquals("-1E0", str);
    str = NumberFormat.getFormat("00.000E00").format(-1);
    assertEquals("-10.000E-01", str);
    str = NumberFormat.getFormat("##0.######E000").format(-1);
    assertEquals("-1E000", str);
    str = NumberFormat.getFormat("0.###E0;[0.###E0]").format(-1);
    assertEquals("[1E0]", str);

    str = NumberFormat.getFormat("0.####E0").format(1);
    assertEquals("1E0", str);
    str = NumberFormat.getFormat("00.000E00").format(1);
    assertEquals("10.000E-01", str);
    str = NumberFormat.getFormat("##0.######E000").format(1);
    assertEquals("1E000", str);
    str = NumberFormat.getFormat("0.###E0;[0.###E0]").format(1);
    assertEquals("1E0", str);

    str = NumberFormat.getFormat("#E0").format(12345.0);
    assertEquals("1E4", str);
    str = NumberFormat.getFormat("0E0").format(12345.0);
    assertEquals("1E4", str);
    str = NumberFormat.getFormat("##0.###E0").format(12345.0);
    assertEquals("12.345E3", str);
    str = NumberFormat.getFormat("##0.###E0").format(12345.00001);
    assertEquals("12.345E3", str);
    str = NumberFormat.getFormat("##0.###E0").format(12345);
    assertEquals("12.345E3", str);

    str = NumberFormat.getFormat("##0.####E0").format(789.12346e-9);
    assertEquals("789.1235E-9", str);
    str = NumberFormat.getFormat("##0.####E0").format(780.e-9);
    assertEquals("780E-9", str);
    str = NumberFormat.getFormat(".###E0").format(45678.0);
    assertEquals(".457E5", str);
    str = NumberFormat.getFormat(".###E0").format(0);
    assertEquals(".0E0", str);

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

  public void testExponentParse() {
    double value;

    value = NumberFormat.getFormat("#E0").parse("1.234E3");
    assertTrue(value == 1.234E+3);

    value = NumberFormat.getFormat("0.###E0").parse("1.234E3");
    assertTrue(value == 1.234E+3);

    value = NumberFormat.getFormat("#E0").parse("1.2345E4");
    assertTrue(value == 12345.0);

    value = NumberFormat.getFormat("0E0").parse("1.2345E4");
    assertTrue(value == 12345.0);

    value = NumberFormat.getFormat("0E0").parse("1.2345E+4");
    assertTrue(value == 12345.0);
  }

  public void testForceLatin() {
    assertFalse(NumberFormat.forcedLatinDigits());
    NumberFormat.setForcedLatinDigits(true);
    assertTrue(NumberFormat.forcedLatinDigits());
    NumberFormat decLatin = NumberFormat.getDecimalFormat();
    assertEquals("1,003.14", decLatin.format(1003.14));
    assertEquals("-1,003.14", decLatin.format(-1003.14));
    NumberFormat.setForcedLatinDigits(false);
    assertFalse(NumberFormat.forcedLatinDigits());
    assertEquals("3.14", decLatin.format(3.14));
    NumberFormat unforced = NumberFormat.getDecimalFormat();
    assertEquals("3.14", unforced.format(3.14));
  }

  public void testGrouping() {
    String str;

    str = NumberFormat.getFormat("#,###").format(1234567890);
    assertEquals("1,234,567,890", str);
    str = NumberFormat.getFormat("#,####").format(1234567890);
    assertEquals("12,3456,7890", str);

    str = NumberFormat.getFormat("#").format(1234567890);
    assertEquals("1234567890", str);
  }

  public void testIssue4598() {
    String str = NumberFormat.getFormat("#,##0.######").format(1.3);
    assertStartsWith("1.3", str);
  }

  public void testLargeNumber() {
    // issue 4473
    String str = NumberFormat.getFormat("0.00").format(
        222222222222222222222222222222.0);
    // we can't test the exact value, because different browsers round
    // differently, so we just check the prefix and that it isn't in exponential
    // format
    assertStartsWith("22222222222", str);
    assertTrue(str.indexOf('e') < 0);
  }

  // See external issue 3140
  public void testLeadingZeros() {
    String str;

    str = NumberFormat.getFormat("0,000,000,000.#").format(123456789.489123);
    assertEquals("0,123,456,789.5", str);

    str = NumberFormat.getFormat("#,###.####").format(0.414014);
    assertEquals("0.414", str); // why leading 0?

    str = NumberFormat.getFormat("#.####").format(0.414014);
    assertEquals("0.414", str); // why leading 0?

    str = NumberFormat.getFormat("#.0###").format(0.414014);
    assertEquals(".414", str);

    str = NumberFormat.getFormat("0.0###").format(0.414014);
    assertEquals("0.414", str);

    str = NumberFormat.getFormat("0.####").format(0.414014);
    assertEquals("0.414", str);

    str = NumberFormat.getFormat("0.0000").format(0.414014);
    assertEquals("0.4140", str);

    str = NumberFormat.getFormat("#.0000").format(0.414014);
    assertEquals(".4140", str);
  }

  public void testNegative() {
    String str;

    // verify default negative pattern
    str = NumberFormat.getFormat("#,##0.0").format(-0.15);
    assertEquals("-0.2", str);
    str = NumberFormat.getFormat("#,##0.0%").format(-0.1534);
    assertEquals("-15.3%", str);
    str = NumberFormat.getFormat("X #,##0.0%").format(-0.1534);
    assertEquals("-X 15.3%", str);

    // verify we can specify percent/permille suffixes in both parts
    str = NumberFormat.getFormat("#,##0.0%;#,##0.0%-").format(-0.152);
    assertEquals("15.2%-", str);
    str = NumberFormat.getFormat("#,##0.0\u2030;#,##0.0\u2030-").format(-0.152);
    assertEquals("152.0\u2030-", str);

    // verify negative portion is only used for prefix/suffix
    str = NumberFormat.getFormat("#,##0.00;-# X").format(-1014.336);
    assertEquals("-1,014.34 X", str);
  }

  public void testParseNotANumber() {
    try {
      NumberFormat.getDecimalFormat().parse("blue");
      fail("Expected a NumberFormatException");
    } catch (NumberFormatException e) {
      assertEquals("blue", e.getMessage());
    }
  }

  public void testPercent() {
    String str;

    str = NumberFormat.getFormat("###.###%").format(0.4857);
    assertEquals("48.57%", str);
    str = NumberFormat.getFormat("###.### %").format(0.4857);
    assertEquals("48.57 %", str);
    str = NumberFormat.getFormat("0.00%").format(0.485);
    assertEquals("48.50%", str);
  }

  public void testPerMill() {
    String str;

    str = NumberFormat.getFormat("###.###\u2030").format(0.4857);
    assertEquals("485.7\u2030", str);
  }

  public void testQuotes() {
    String str;

    str = NumberFormat.getFormat("a'fo''o'b#").format(123);
    assertEquals("afo'ob123", str);

    str = NumberFormat.getFormat("a''b#").format(123);
    assertEquals("a'b123", str);
  }

  public void testRounding() {
    String str;

    str = NumberFormat.getFormat("#0.##").format(0.555);
    assertEquals("0.56", str);

    str = NumberFormat.getFormat("#.##").format(30.555);
    assertEquals("30.56", str);

    str = NumberFormat.getFormat("#.00").format(0.997);
    assertEquals("1.00", str);

    str = NumberFormat.getFormat("#.00").format(-0.997);
    assertEquals("-1.00", str);

    str = NumberFormat.getFormat("#.00").format(27.997);
    assertEquals("28.00", str);

    str = NumberFormat.getFormat("#.00").format(-27.997);
    assertEquals("-28.00", str);

    str = NumberFormat.getFormat("#0.00000").format(1.23456789E-03);
    assertEquals("0.00123", str);

    str = NumberFormat.getFormat("#0.0000000").format(1.23456789E-03);
    assertEquals("0.0012346", str);

    str = NumberFormat.getFormat("#0.0000").format(1.2E-03);
    assertEquals("0.0012", str);

    str = NumberFormat.getFormat("#0.000").format(1.2E-03);
    assertEquals("0.001", str);

    str = NumberFormat.getFormat("#0.00").format(1.2E-03);
    assertEquals("0.00", str);

    str = NumberFormat.getFormat("#0.0").format(1.2E-03);
    assertEquals("0.0", str);

    str = NumberFormat.getFormat("#0.00").format(11.2E-03);
    assertEquals("0.01", str);

    str = NumberFormat.getFormat("#0.00").format(111.2E-03);
    assertEquals("0.11", str);

    str = NumberFormat.getFormat("#0.00").format(1111.2E-03);
    assertEquals("1.11", str);

    str = NumberFormat.getFormat("#0.00000").format(1.23456789E-05);
    assertEquals("0.00001", str);

    str = NumberFormat.getFormat("#0.0000000").format(1.23456789E-05);
    assertEquals("0.0000123", str);

    str = NumberFormat.getFormat("#0.0000000").format(1.23756789E-05);
    assertEquals("0.0000124", str);

    str = NumberFormat.getFormat("#,##,###,##0.00000000000").format(111.18);
    assertEquals("111.18000000000", str);
  }

  public void testStandardFormat() {
    String str;

    str = NumberFormat.getCurrencyFormat().format(1234.579);
    assertEquals("$1,234.58", str);
    str = NumberFormat.getDecimalFormat().format(1234.579);
    assertEquals("1,234.579", str);
    str = NumberFormat.getPercentFormat().format(1234.579);
    assertEquals("123,458%", str);
    str = NumberFormat.getScientificFormat().format(1234.579);
    assertEquals("1E3", str);
  }

  public void testToScaledString() {
    StringBuilder buf = new StringBuilder();
    int scale = NumberFormat.toScaledString(buf, .1);
    String str = buf.toString();
    assertStartsWith("100", str.substring(str.length() + scale));
    assertAllZeros(str, str.length() + scale);
    buf = new StringBuilder();
    scale = NumberFormat.toScaledString(buf, 12345e38);
    str = buf.toString();
    assertStartsWith("12345", str);
    assertEquals(43, scale + str.length());
  }

  public void testZeros() {
    String str;

    str = NumberFormat.getFormat("#.#").format(0);
    assertEquals("0", str);
    str = NumberFormat.getFormat("#.").format(0);
    assertEquals("0.", str);
    str = NumberFormat.getFormat(".#").format(0);
    assertEquals(".0", str);
    str = NumberFormat.getFormat("#").format(0);
    assertEquals("0", str);

    str = NumberFormat.getFormat("#0.#").format(0);
    assertEquals("0", str);
    str = NumberFormat.getFormat("#0.").format(0);
    assertEquals("0.", str);
    str = NumberFormat.getFormat("#.0").format(0);
    assertEquals(".0", str);
    str = NumberFormat.getFormat("#").format(0);
    assertEquals("0", str);
  }

  private void assertAllZeros(String str, int prefixLen) {
    if (prefixLen > str.length()) {
      prefixLen = str.length();
    }
    for (int i = 0; i < prefixLen; ++i) {
      assertEquals('0', str.charAt(i));
    }
  }

  private void assertStartsWith(String prefix, String str) {
    assertTrue(str + " does not start with " + prefix, str.startsWith(prefix));
  }
}
