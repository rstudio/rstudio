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

package com.google.gwt.emultest.java.lang;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.junit.client.GWTTestCase;

/**
 * Unit tests for the emulated-in-Javascript Double/double autoboxed types.
 */
public class DoubleTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  public void testBadStrings() {
    try {
      new Double("0.0e");
      fail("constructor");
    } catch (NumberFormatException e) {
      // Expected behavior
    }

    try {
      Double.parseDouble("0.0e");
      fail("parse");
    } catch (NumberFormatException e) {
      // Expected behavior
    }

    try {
      Double.parseDouble(".");
      fail("parse");
    } catch (NumberFormatException e) {
      // Expected behavior
    }

    try {
      Double.parseDouble(".e");
      fail("parse");
    } catch (NumberFormatException e) {
      // Expected behavior
    }

    try {
      Double.parseDouble("e5");
      fail("parse");
    } catch (NumberFormatException e) {
      // Expected behavior
    }

    try {
      Double.parseDouble(".e5");
      fail("parse");
    } catch (NumberFormatException e) {
      // Expected behavior
    }

    try {
      Double.valueOf("0x0e");
      fail("valueOf");
    } catch (NumberFormatException e) {
      // Expected behavior
    }
  }

  public void testCompare() {
    assertTrue(Double.compare(Double.NaN, Double.NaN) == 0);
    assertTrue(Double.compare(0.0, Double.NaN) < 0);
    assertTrue(Double.compare(Double.NaN, Double.POSITIVE_INFINITY) > 0);
    assertTrue(Double.compare(Double.NaN, 0.0) > 0);
    assertTrue(Double.compare(Double.POSITIVE_INFINITY, Double.NaN) < 0);
    assertTrue(Double.compare(3.0, 500.0) < 0);
    assertTrue(Double.compare(500.0, 3.0) > 0);
    assertTrue(Double.compare(500.0, 500.0) == 0);
  }

  public void testNPE() {
    Double d = Math.random() < 0 ? 42.0 : null;
    try {
      assertEquals(null, d.doubleValue());
      fail("Should have thrown exception");
    } catch (Exception e) {
    }

    try {
      double dd = d;
      fail("Should have thrown exception" + dd);
    } catch (Exception e) {
    }
  }

  public void testDoesNotCastToJso() {
    try {
      Double d = 1.2;
      Object o = d;
      JavaScriptObject jso = (JavaScriptObject) o;
      fail("Double should fail to cast to a JSO");
    } catch (ClassCastException e) {
    }
  }

  public void testEqualityNormalizer() {
    Double d = 0.0;
    if (d != null) {
      assertEquals(d.doubleValue(), 0.0);
    } else {
      fail("0.0 should not evaluate to null");
    }
    Object s = "0.0";
    assertTrue(d != s);

    Object b = Boolean.FALSE;
    assertTrue(b != s);
  }

  public void testCompareTo() {
    Double zero = new Double(0.0);
    Double three = new Double(3.0);
    Double fiveHundred = new Double(500.0);
    Double infinity = new Double(Double.POSITIVE_INFINITY);
    Double nan = new Double(Double.NaN);

    assertTrue(nan.compareTo(nan) == 0);
    assertTrue(zero.compareTo(nan) < 0);
    assertTrue(nan.compareTo(infinity) > 0);
    assertTrue(nan.compareTo(zero) > 0);
    assertTrue(infinity.compareTo(nan) < 0);
    assertTrue(three.compareTo(fiveHundred) < 0);
    assertTrue(fiveHundred.compareTo(three) > 0);
    assertTrue(fiveHundred.compareTo(fiveHundred) == 0);
  }

  @SuppressWarnings({"SelfEquality", "EqualsNaN"})
  public void testDoubleConstants() {
    assertTrue(Double.isNaN(Double.NaN));
    assertTrue(Double.isInfinite(Double.NEGATIVE_INFINITY));
    assertTrue(Double.isInfinite(Double.POSITIVE_INFINITY));
    assertTrue(Double.NEGATIVE_INFINITY < Double.POSITIVE_INFINITY);
    assertTrue(Double.MIN_VALUE < Double.MAX_VALUE);
    assertFalse(Double.NaN == Double.NaN);
    assertEquals(64, Double.SIZE);
    // jdk1.6 assertEquals(Math.getExponent(Double.MAX_VALUE),
    // Double.MAX_EXPONENT);
    // jdk1.6 assertEquals(Math.getExponent(Double.MIN_NORMAL),
    // Double.MIN_EXPONENT);
    // issue 8073 - used to fail in prod mode
    assertFalse(Double.isInfinite(Double.NaN));
  }

  public void testIsFinite() {
    final double[] nonfiniteNumbers = {
        Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.NaN,
    };
    for (double value : nonfiniteNumbers) {
      assertFalse(Double.isFinite(value));
    }

    final double[] finiteNumbers = {
        -Double.MAX_VALUE, Double.MAX_VALUE, Double.MIN_VALUE,
        -1.0, -0.5, -0.1, -0.0, 0.0, 0.1, 0.5, 1.0,
    };
    for (double value : finiteNumbers) {
      assertTrue(Double.isFinite(value));
    }
  }

  public void testIsInfinite() {
    assertTrue(Double.isInfinite(Double.NEGATIVE_INFINITY));
    assertTrue(Double.isInfinite(Double.POSITIVE_INFINITY));

    assertFalse(Double.isInfinite(Double.NaN));

    final double[] finiteNumbers = {
        -Double.MAX_VALUE, Double.MAX_VALUE, Double.MIN_VALUE,
        -1.0, -0.5, -0.1, -0.0, 0.0, 0.1, 0.5, 1.0,
    };
    for (double value : finiteNumbers) {
      assertFalse(Double.isInfinite(value));
    }
  }

  public void testParse() {
    assertTrue(0 == Double.parseDouble("0"));
    assertTrue(100 == Double.parseDouble("1e2"));
    assertTrue(-100 == Double.parseDouble("-1e2"));
    assertTrue(-1.5 == Double.parseDouble("-1.5"));
    assertTrue(3.0 == Double.parseDouble("3."));
    assertTrue(0.5 == Double.parseDouble(".5"));
    assertTrue(2.98e8 == Double.parseDouble("2.98e8"));
    assertTrue(-2.98e-8 == Double.parseDouble("-2.98e-8"));
    assertTrue(+2.98E+8 == Double.parseDouble("+2.98E+8"));
    assertTrue(
        "Can't parse MIN_VALUE",
        Double.MIN_VALUE == Double.parseDouble(String.valueOf(Double.MIN_VALUE)));
    assertTrue(
        "Can't parse MAX_VALUE",
        Double.MAX_VALUE == Double.parseDouble(String.valueOf(Double.MAX_VALUE)));

    // Test that leading and trailing whitespace is ignored
    // Test that both 'e' and 'E' may be used as the exponent delimiter
    assertTrue(2.56789e1 == Double.parseDouble("2.56789e1"));
    assertTrue(2.56789e1 == Double.parseDouble("  2.56789E+1"));
    assertTrue(2.56789e1 == Double.parseDouble("2.56789e1   "));
    assertTrue(2.56789e1 == Double.parseDouble("   2.56789E01   "));
    assertTrue(2.56789e1 == Double.parseDouble("+2.56789e1"));
    assertTrue(2.56789e1 == Double.parseDouble("  +2.56789E+01"));
    assertTrue(2.56789e1 == Double.parseDouble("+2.56789e1   "));
    assertTrue(2.56789e1 == Double.parseDouble("   +2.56789E1   "));
    assertTrue(-2.56789e1 == Double.parseDouble("-2.56789e+1"));
    assertTrue(-2.56789e1 == Double.parseDouble("  -2.56789E1"));
    assertTrue(-2.56789e1 == Double.parseDouble("-2.56789e+01   "));
    assertTrue(-2.56789e1 == Double.parseDouble("   -2.56789E1   "));

    // Test that a float/double type suffix is allowed
    assertEquals(1.0d, Double.parseDouble("1.0f"), 0.0);
    assertEquals(1.0d, Double.parseDouble("1.0F"), 0.0);
    assertEquals(1.0d, Double.parseDouble("1.0d"), 0.0);
    assertEquals(1.0d, Double.parseDouble("1.0D"), 0.0);

    // Test NaN/Infinity - issue 7713
    assertTrue(Double.isNaN(Double.parseDouble("+NaN")));
    assertTrue(Double.isNaN(Double.parseDouble("NaN")));
    assertTrue(Double.isNaN(Double.parseDouble("-NaN")));
    assertEquals(Double.POSITIVE_INFINITY, Double.parseDouble("+Infinity"));
    assertEquals(Double.POSITIVE_INFINITY, Double.parseDouble("Infinity"));
    assertEquals(Double.NEGATIVE_INFINITY, Double.parseDouble("-Infinity"));

    // check for parsing some invalid values
    try {
      Double.parseDouble("nan");
      fail("Expected NumberFormatException");
    } catch (NumberFormatException expected) {
    }
    try {
      Double.parseDouble("infinity");
      fail("Expected NumberFormatException");
    } catch (NumberFormatException expected) {
    }
    try {
      Double.parseDouble("1.2.3");
      fail("Expected NumberFormatException");
    } catch (NumberFormatException expected) {
    }
    try {
      Double.parseDouble("+-1.2");
      fail("Expected NumberFormatException");
    } catch (NumberFormatException expected) {
    }
    try {
      Double.parseDouble("1e");
      fail("Expected NumberFormatException");
    } catch (NumberFormatException expected) {
    }
  }

  public void testDoubleBits() {
    // special values
    compareDoubleBits(0x0000000000000000L, 0.0);
    compareDoubleBits(0x8000000000000000L, -0.0);
    compareDoubleBits(0x8000000000000000L, 1.0 / Double.NEGATIVE_INFINITY);
    compareDoubleBits(0x7ff8000000000000L, Double.NaN);
    compareDoubleBits(0x7ff0000000000000L, Double.POSITIVE_INFINITY);
    compareDoubleBits(0xfff0000000000000L, Double.NEGATIVE_INFINITY);

    // values around 1.0 and 2.0
    compareDoubleBits(0x3feffffffffffff7L, 0.999999999999999);
    compareDoubleBits(0x3ff0000000000000L, 1.0);
    compareDoubleBits(0x3ff0000000000005L, 1.000000000000001);
    compareDoubleBits(0x3ffffffffffffffbL, 1.999999999999999);
    compareDoubleBits(0x4000000000000000L, 2.0);
    compareDoubleBits(0x4000000000000002L, 2.000000000000001);
    
    // basic tests
    compareDoubleBits(0x3fb999999999999aL, 0.1);
    compareDoubleBits(0xbfb999999999999aL, -0.1);
    compareDoubleBits(0x017527e6d48c1653L, 0.1234e-300);
    compareDoubleBits(0x817527e6d48c1653L, -0.1234e-300);
    compareDoubleBits(0x7e0795f2d9000b3fL, 0.1234e300);
    compareDoubleBits(0xfe0795f2d9000b3fL, -0.1234e300);
    compareDoubleBits(0x3fc999999999999aL, 0.2);
    compareDoubleBits(0x4272c3598dd61e72L, 1289389399393.902892);
    compareDoubleBits(0x405edd3c07ee0b0bL, 123.456789);
    compareDoubleBits(0xc05edd3c07ee0b0bL, -123.456789);

    // max value
    compareDoubleBits(0x7fefffffffffffffL, 1.7976931348623157E308);
    compareDoubleBits(0xffefffffffffffffL, -1.7976931348623157E308);
    // min normalized value
    compareDoubleBits(0x0010000000000000L, 2.2250738585072014E-308);
    compareDoubleBits(0x8010000000000000L, -2.2250738585072014E-308);
    
    // denormalized values
    compareDoubleBits(0x000ff6a8ebe79958L, 2.22E-308);
    compareDoubleBits(0x000199999999999aL, 2.2250738585072014E-309);
    compareDoubleBits(0x800016b9f3c0e51dL, -1.234567E-310);
    compareDoubleBits(0x000016b9f3c0e51dL, 1.234567E-310);
    compareDoubleBits(0x00000245cb934a1cL, 1.234567E-311);
    compareDoubleBits(0x0000003a2df52103L, 1.234567E-312);
    compareDoubleBits(0x00000005d165501aL, 1.234567E-313);
    compareDoubleBits(0x0000000094f08803L, 1.234567E-314);
    compareDoubleBits(0x000000000ee4da67L, 1.234567E-315);

    compareDoubleBits(0x00000000017d490aL, 1.234567E-316);
    compareDoubleBits(0x00000000002620e7L, 1.234567E-317);
    compareDoubleBits(0x000000000003d017L, 1.234567E-318);
    compareDoubleBits(0x000000000000619cL, 1.234567E-319);
    compareDoubleBits(0x00000000000009c3L, 1.234567E-320);
    compareDoubleBits(0x00000000000000faL, 1.234567E-321);
    compareDoubleBits(0x0000000000000019L, 1.234567E-322);
    compareDoubleBits(0x0000000000000002L, 1.234567E-323);
    compareDoubleBits(0x0000000000000001L, 4.9E-324);
    compareDoubleBits(0x8000000000000001L, -4.9E-324);
    
    // random values between 0 and 1
    compareDoubleBits(0x3fe9b9bcd3c39dabL, 0.8039230476396616);
    compareDoubleBits(0x3fe669d4a374efc4L, 0.700418776752024);
    compareDoubleBits(0x3fd92b7ca312ca7eL, 0.39327922749649946);
    compareDoubleBits(0x3fbc74aa296b7e18L, 0.11115516196468211);
    compareDoubleBits(0x3feea888cdfcb13dL, 0.95807304603435);
    compareDoubleBits(0x3fd88b23cfa7eadaL, 0.3834924247636714);
    compareDoubleBits(0x3fd62865167eb9bcL, 0.3462155074766107);
    compareDoubleBits(0x3fe5772b57e62b3fL, 0.6707970349101301);
    compareDoubleBits(0x3fbd09988fb96be0L, 0.11342767247099017);
    compareDoubleBits(0x3fb643296d7fa050L, 0.08696230815223882);
    compareDoubleBits(0x3fde7f76986c1bd4L, 0.4765297401904125);
    compareDoubleBits(0x3fef4b4433f8efacL, 0.9779377951704098);
    compareDoubleBits(0x3fd374e530a19278L, 0.3040097212708939);
    compareDoubleBits(0x3fc17adf98fc3368L, 0.1365622994420861);
    compareDoubleBits(0x3fd6beb0e5a5055aL, 0.355388855230634);
    compareDoubleBits(0x3fc3d8128b76ba20L, 0.1550315075850941);
    compareDoubleBits(0x3fc47c5027f58900L, 0.16004373503808011);
    compareDoubleBits(0x3fe0ba6a91eeb5a7L, 0.522755894684157);
    compareDoubleBits(0x3fe68f019034c7b9L, 0.704956800129586);
    compareDoubleBits(0x3fe3990dbaf329c4L, 0.6124333049167991);
    compareDoubleBits(0x3faded6423ccf8f0L, 0.058451775903891945);
    compareDoubleBits(0x3fe51aebf7ee4537L, 0.6595363466641747);
    compareDoubleBits(0x3fe937bb75080f7dL, 0.7880532537242143);
    compareDoubleBits(0x3fc693d447054de4L, 0.17638638942535956);
    compareDoubleBits(0x3fd95091de22548eL, 0.39554259007247417);
    compareDoubleBits(0x3fe93b21f50b1a41L, 0.788468340492564);
    compareDoubleBits(0x3fd77d9f7da868b8L, 0.36704242011331756);
    compareDoubleBits(0x3fcb8abae3f1c05cL, 0.2151712048539619);
    compareDoubleBits(0x3feec9ed025ddcf3L, 0.9621491476272283);
    compareDoubleBits(0x3fda1ac9bf0e59c0L, 0.4078850141317183);
    compareDoubleBits(0x3fe66e66602de93eL, 0.7009765509131183);
    compareDoubleBits(0x3fe6da2963aecb21L, 0.714131064122146);
    compareDoubleBits(0x3fb306bb648e4ae0L, 0.07432147221542662);
    compareDoubleBits(0x3fd06b9877b9b50eL, 0.25656711284575884);
    compareDoubleBits(0x3fce870599f3a28cL, 0.2384955407826691);
    compareDoubleBits(0x3fe14a5c59d8ce59L, 0.5403272394964446);
    compareDoubleBits(0x3fb118ac2dc6a700L, 0.06678272359445359);
    compareDoubleBits(0x3fafb0e23ecbc770L, 0.06189639107277756);
    compareDoubleBits(0x3fe4475a31a9723aL, 0.633710000034234);
    compareDoubleBits(0x3fdd5e0f4a0296f6L, 0.4588659498934783);
    compareDoubleBits(0x3fefbc13bb0b2b44L, 0.991708627051914);
    compareDoubleBits(0x3fde5c601db8b162L, 0.4743881502388573);
    compareDoubleBits(0x3fdda64289b8cde6L, 0.4632726998272275);
    compareDoubleBits(0x3fea18660f99c86bL, 0.8154783539487317);
    compareDoubleBits(0x3fec39460d8a8808L, 0.8819914116359096);
    compareDoubleBits(0x3fd6a29437ecfad4L, 0.3536730333470761);
    compareDoubleBits(0x3fe1c31fcb975395L, 0.5550688721074147);
    compareDoubleBits(0x3fc784448f1277b0L, 0.18372399316734578);
    compareDoubleBits(0x3fe78d52a1f7d63cL, 0.7360013163985921);
    compareDoubleBits(0x3feb0d9bee281702L, 0.8454112674232592);
    compareDoubleBits(0x3fc382ec2f0ee738L, 0.15243294046177325);
    compareDoubleBits(0x3fe616577bf4b8d5L, 0.6902272625937039);
    compareDoubleBits(0x3fdd6ffcb6caedacL, 0.4599601540646414);
    compareDoubleBits(0x3fdfa267b07ca0e4L, 0.49428741679231636);
    compareDoubleBits(0x3fcdc3688fcb9f34L, 0.23252589246043842);
    compareDoubleBits(0x3fc6bd1204233708L, 0.1776449699595377);
    compareDoubleBits(0x3fd75236cfc8fafeL, 0.364392950930707);
    compareDoubleBits(0x3fef34680bd4ce47L, 0.9751472693519155);
    compareDoubleBits(0x3fc634b5d386b93cL, 0.17348358944350106);
    compareDoubleBits(0x3feaf69abdedcf4bL, 0.8426030835675901);
    compareDoubleBits(0x3fdcf973748a67e0L, 0.45272528057978256);
    compareDoubleBits(0x3fec8f6155ecd410L, 0.8925024679398366);
    compareDoubleBits(0x3fe3e8d8466d453aL, 0.6221734405063792);
    compareDoubleBits(0x3fdfa7ff50fced6aL, 0.4946287432573714);
    compareDoubleBits(0x3fe536d9d49d33beL, 0.6629456665628977);
    compareDoubleBits(0x3fdfdff0e8e048aeL, 0.4980432771855118);
    compareDoubleBits(0x3feb4abc3a80aeacL, 0.8528729574804479);
    compareDoubleBits(0x3fbf44d011fd7950L, 0.12214374961101737);
    compareDoubleBits(0x3fdb59c21a7ecd6aL, 0.4273534067862871);
    compareDoubleBits(0x3fbb4128fb635888L, 0.10646301400569957);
    compareDoubleBits(0x3fc03e9c906fa23cL, 0.12691075375120586);
    compareDoubleBits(0x3f976c3738766d00L, 0.022873747655109078);
    compareDoubleBits(0x3fd9a62096187b4eL, 0.4007646051194812);
    compareDoubleBits(0x3fdcea7def3c0528L, 0.45181225168933503);
    compareDoubleBits(0x3fe20ea23d703cb2L, 0.5642863464326153);
    compareDoubleBits(0x3fd6f2c3edcf5bf4L, 0.35856722091324333);
    compareDoubleBits(0x3fef2c3c9e7a6dc0L, 0.9741499991682119);
    compareDoubleBits(0x3fcc2142c7ab8c28L, 0.21976504086987458);
    compareDoubleBits(0x3fea41a3e626ff58L, 0.8205127234614151);
    compareDoubleBits(0x3fe4162d28c9abe8L, 0.6277070805202785);
    compareDoubleBits(0x3fce8826a9ca117cL, 0.23852999964232058);
    compareDoubleBits(0x3fe07fbd24b88c67L, 0.5155931203083924);
    compareDoubleBits(0x3fdb39c66484189aL, 0.4254013043977324);
    compareDoubleBits(0x3fcb830d50fac7b0L, 0.21493689016420836);
    compareDoubleBits(0x3fd927ccb62342c8L, 0.3930541781128736);
    compareDoubleBits(0x3fb553b2448dd6d8L, 0.08330835508044332);
    compareDoubleBits(0x3fef870d8a9f527bL, 0.9852359492748087);
    compareDoubleBits(0x3febe929c4bac429L, 0.8722122995733389);
    compareDoubleBits(0x3fc9cc2d6286a01cL, 0.20154349623521817);
    compareDoubleBits(0x3fe5b506615ab5c6L, 0.6783477689220312);
    compareDoubleBits(0x3fe26c9e0a02bdfeL, 0.5757589526673994);
    compareDoubleBits(0x3fe6daf54806b05cL, 0.7142282873878787);
    compareDoubleBits(0x3fefc9bbb28f362eL, 0.9933756339539224);
    compareDoubleBits(0x3fbd455782e84968L, 0.1143393225286552);
    compareDoubleBits(0x3fe1f09744a3d0b7L, 0.5606190052626719);
    compareDoubleBits(0x3fb1c833b64b5470L, 0.06946109009307277);
    compareDoubleBits(0x3fec140750dfb23bL, 0.8774448947493235);
    compareDoubleBits(0x3fc3b30746f5e6bcL, 0.15390101399298384);
    compareDoubleBits(0x3fe844deac963963L, 0.7584069605671114);
    compareDoubleBits(0x3fd45d6e91a9989eL, 0.31820263123371173);

    // random values throughout the double range
    compareDoubleBits(0xcdcde6aa7873b572L, -6.297893811982062E66);
    compareDoubleBits(0xb34ea52b6e9df882L, -1.4898867990306772E-61);
    compareDoubleBits(0x64adf2aa312ca7e1L, 9.480996430600118E176);
    compareDoubleBits(0x1c74aa06a5adf865L, 1.3367811675349397E-171);
    compareDoubleBits(0xf5444671bf9627b5L, -7.610810186261922E256);
    compareDoubleBits(0x622c8f3efa7eadb0L, 8.223166382138422E164);
    compareDoubleBits(0x58a1947167eb9bccL, 8.86632413276402E118);
    compareDoubleBits(0xabb95a84fcc567f2L, -4.6366137067352683E-98);
    compareDoubleBits(0x1d0998843ee5af9aL, 8.477749983935152E-169);
    compareDoubleBits(0x1643296db5fe8140L, 1.9557345103545524E-201);
    compareDoubleBits(0x79fdda7486c1bd5fL, 4.2335912871234087E279);
    compareDoubleBits(0xfa5a219d7f1df591L, -2.3716857301453343E281);
    compareDoubleBits(0x4dd394c40a192798L, 8.248528934271815E66);
    compareDoubleBits(0x22f5bf1fc7e19b49L, 2.853336355041256E-140);
    compareDoubleBits(0x5afac3aa5a5055b7L, 1.8552153438817665E130);
    compareDoubleBits(0x27b025195bb5d113L, 1.6005805986130906E-117);
    compareDoubleBits(0x28f8a05a3fac4815L, 2.5600128085455218E-111);
    compareDoubleBits(0x85d354a03dd6b4f3L, -1.3311552586579328E-280);
    compareDoubleBits(0xb4780c890698f727L, -6.129954074000813E-56);
    compareDoubleBits(0x9cc86de85e65389aL, -5.057128159830759E-170);
    compareDoubleBits(0x0ef6b2304799f1f0L, 1.3941634112607635E-236);
    compareDoubleBits(0xa8d75f9ffdc8a6fcL, -6.0744368561933565E-112);
    compareDoubleBits(0xc9bddb8fa101efb6L, -1.7045710739729275E47);
    compareDoubleBits(0x2d27a89b382a6f20L, 3.6294490400055576E-91);
    compareDoubleBits(0x65424775e22548f8L, 5.925748939163494E179);
    compareDoubleBits(0xc9d90fb1a1634836L, -5.722990169624538E47);
    compareDoubleBits(0x5df67dbfda868b9eL, 4.3882436353810935E144);
    compareDoubleBits(0x371575e91f8e02eeL, 2.4058151686870744E-43);
    compareDoubleBits(0xf64f681f4bbb9e72L, -7.726253195850651E261);
    compareDoubleBits(0x686b26cff0e59c06L, 9.910208820012368E194);
    compareDoubleBits(0xb373331d05bd27ceL, -7.467486715472904E-61);
    compareDoubleBits(0xb6d14b2b75d96425L, -1.211676991363774E-44);
    compareDoubleBits(0x1306bb6092392b97L, 5.1516895067444726E-217);
    compareDoubleBits(0x41ae61d47b9b50e7L, 2.548639338033516E8);
    compareDoubleBits(0x3d0e0b31cf9d1469L, 1.3342095786138988E-14);
    compareDoubleBits(0x8a52e2e03b19cb38L, -6.141707139267237E-259);
    compareDoubleBits(0x1118ac38b71a9c13L, 2.603758248081968E-226);
    compareDoubleBits(0x0fd871017d978ee5L, 2.459857504627046E-232);
    compareDoubleBits(0xa23ad18a352e4745L, -8.590863325618324E-144);
    compareDoubleBits(0x75783d29a0296f70L, 7.278962913424277E257);
    compareDoubleBits(0xfde09de561656888L, -2.1734648688870485E298);
    compareDoubleBits(0x79718077db8b1623L, 9.695260031659742E276);
    compareDoubleBits(0x76990a379b8cde6dL, 1.971192366484015E263);
    compareDoubleBits(0xd0c3304ff3390d67L, -1.1376138095726657E81);
    compareDoubleBits(0xe1ca3067b1510109L, -1.1782242328468048E163);
    compareDoubleBits(0x5a8a50c47ecfad55L, 1.425081729881992E128);
    compareDoubleBits(0x8e18fe5072ea72a0L, -9.37063623070425E-241);
    compareDoubleBits(0x2f08893a7893bd8dL, 4.0416231167192477E-82);
    compareDoubleBits(0xbc6a95093efac79fL, -1.1528179597119854E-17);
    compareDoubleBits(0xd86cdf4ec502e047L, -9.101010985774022E117);
    compareDoubleBits(0x2705d86b787739cfL, 1.0574785023773003E-120);
    compareDoubleBits(0xb0b2bbf87e971aa7L, -4.141881119170908E-74);
    compareDoubleBits(0x75bff2d46caedac7L, 1.5350675559647406E259);
    compareDoubleBits(0x7e899eea07ca0e54L, 3.4316079097255617E301);
    compareDoubleBits(0x3b86d1277e5cf9a3L, 6.039611960807865E-22);
    compareDoubleBits(0x2d7a243c2119b84aL, 1.2833127546095387E-89);
    compareDoubleBits(0x5d48db16fc8faff2L, 2.3679693140335752E141);
    compareDoubleBits(0xf9a340497a99c8efL, -8.531434157588162E277);
    compareDoubleBits(0x2c696ba49c35c9f4L, 9.520836999385529E-95);
    compareDoubleBits(0xd7b4d5dabdb9e964L, -3.206856786877124E114);
    compareDoubleBits(0x73e5cde748a67e04L, 1.9514110919532497E250);
    compareDoubleBits(0xe47b0a84bd9a821eL, -1.0700933026823467E176);
    compareDoubleBits(0x9f46c209cda8a753L, -5.179951037444452E-158);
    compareDoubleBits(0x7e9ffd4f0fced6b4L, 8.569252835514251E301);
    compareDoubleBits(0xa9b6ce9893a677d6L, -9.711135665099199E-108);
    compareDoubleBits(0x7f7fc3978e048af9L, 1.3940913327465408E306);
    compareDoubleBits(0xda55e1fe5015d581L, -1.4812924721906427E127);
    compareDoubleBits(0x1f44d03847f5e54dL, 4.737338914555982E-158);
    compareDoubleBits(0x6d670848a7ecd6b5L, 1.016307848543568E219);
    compareDoubleBits(0x1b4128e3ed8d6230L, 2.117302740808736E-177);
    compareDoubleBits(0x207d392f837d11f6L, 3.4873269549337505E-152);
    compareDoubleBits(0x05db0dd638766d17L, 1.8630149420804414E-280);
    compareDoubleBits(0x669882566187b4e0L, 1.6662694932683939E186);
    compareDoubleBits(0x73a9f7acf3c05283L, 1.452500477811838E249);
    compareDoubleBits(0x907511ceae07965bL, -2.1713947206680943E-229);
    compareDoubleBits(0x5bcb0f90dcf5bf4eL, 1.5366281556927236E134);
    compareDoubleBits(0xf961e4e2cf4db80dL, -4.956276753857123E276);
    compareDoubleBits(0x3842859a3d5c6147L, 1.0886185451514296E-37);
    compareDoubleBits(0xd20d1f11c4dfeb10L, -1.8103414291013452E87);
    compareDoubleBits(0xa0b1697219357d01L, -3.32451488002257E-151);
    compareDoubleBits(0x3d104d584e508be4L, 1.44791988988344E-14);
    compareDoubleBits(0x83fde93797118ce8L, -1.9182940419362316E-289);
    compareDoubleBits(0x6ce719b2484189a7L, 3.9816763934214935E216);
    compareDoubleBits(0x37061aa687d63d94L, 1.238977854703573E-43);
    compareDoubleBits(0x649f32e062342c83L, 4.938493427207422E176);
    compareDoubleBits(0x1553b27712375b61L, 6.135160266378285E-206);
    compareDoubleBits(0xfc386c6b53ea4f67L, -2.3801480044660797E290);
    compareDoubleBits(0xdf494e029758853eL, -1.0354031977895994E151);
    compareDoubleBits(0x33985ae9143500f8L, 3.7890569411337005E-60);
    compareDoubleBits(0xada8332b2b56b8c5L, -9.503956824431546E-89);
    compareDoubleBits(0x9364f07f4057bfcaL, -3.0370938126636166E-215);
    compareDoubleBits(0xb6d7aa7b00d60b8cL, -1.6581522255623348E-44);
    compareDoubleBits(0xfe4ddda651e6c5caL, -2.500115798151869E300);
    compareDoubleBits(0x1d45578c0ba125baL, 1.1310118263643858E-167);
    compareDoubleBits(0x8f84ba31947a16ffL, -6.518932884318998E-234);
    compareDoubleBits(0x11c833a9d92d51dcL, 5.230715225679756E-223);
    compareDoubleBits(0xe0a03a901bf64771L, -2.7851790876803525E157);
    compareDoubleBits(0x27660eb637af35f9L, 6.833565753907854E-119);
    compareDoubleBits(0xc226f54e92c72c68L, -4.930242390758673E10);
    compareDoubleBits(0x5175ba4e1a9989e3L, 2.6381145200142355E84);
  }

  private void compareDoubleBits(long bits, double value) {
    assertEquals(bits, Double.doubleToLongBits(value));
    compareDoubles(value, Double.longBitsToDouble(bits));

    assertEquals(bits, Double.doubleToLongBits(Double.longBitsToDouble(bits)));
    compareDoubles(value, Double.longBitsToDouble(Double.doubleToLongBits(value)));
  }

  private void compareDoubles(double expected, double actual) {
    if (Double.isNaN(expected) || Double.isNaN(actual)) {
      if (Double.isNaN(expected) && Double.isNaN(actual)) {
        return;
      }
      fail("NaN");
    }
    // Do we need a special check for +inf == +inf and -inf == -inf?
    assertEquals(expected, actual);
  }
}

