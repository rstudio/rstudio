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

import com.google.gwt.junit.client.GWTTestCase;

/**
 * Unit tests for the Javascript emulation of the Float/float autoboxed
 * fundamental type.
 */
public class FloatTest extends GWTTestCase {

  @Override
  public String getModuleName() {
    return "com.google.gwt.emultest.EmulSuite";
  }

  public void testBadStrings() {
    try {
      new Float("0.0e");
      fail("constructor");
    } catch (NumberFormatException e) {
      // Expected behavior
    }

    try {
      Float.parseFloat("0.0e");
      fail("parse");
    } catch (NumberFormatException e) {
      // Expected behavior
    }

    try {
      Float.valueOf("0x0e");
      fail("valueOf");
    } catch (NumberFormatException e) {
      // Expected behavior
    }
  }

  public void testCompare() {
    assertTrue("Float.compare failed for 1 < 2", Float.compare(1f, 2f) < 0);
    assertTrue("Float.compare failed for 2 > 1", Float.compare(2f, 1f) > 0);
    assertEquals(0, Float.compare(1f, 1f));

    assertEquals(0, Float.compare(Float.NaN, Float.NaN));
    assertTrue(Float.compare(0.0f, Float.NaN) < 0);
    assertTrue(Float.compare(Float.NaN, Float.POSITIVE_INFINITY) > 0);
    assertTrue(Float.compare(Float.NaN, 0.0f) > 0);
    assertTrue(Float.compare(Float.POSITIVE_INFINITY, Float.NaN) < 0);
  }

  public void testCompareTo() {
    Float float1 = new Float(1f);
    Float float2 = new Float(2f);
    Float floatNaN1 = new Float(Float.NaN);

    assertTrue("Float.compare failed for 1 < 2", float1.compareTo(2f) < 0);
    assertTrue("Float.compare failed for 2 > 1", float2.compareTo(1f) > 0);
    assertEquals(0, float1.compareTo(float1));

    assertEquals(0, floatNaN1.compareTo(new Float(Float.NaN)));
    assertTrue(new Float(0.0f).compareTo(new Float(Float.NaN)) < 0);
    assertTrue(floatNaN1.compareTo(new Float(Float.POSITIVE_INFINITY)) > 0);
    assertTrue(floatNaN1.compareTo(new Float(0.0f)) > 0);
    assertTrue(new Float(Float.POSITIVE_INFINITY).compareTo(new Float(Float.NaN)) < 0);
  }

  @SuppressWarnings({"SelfEquality", "EqualsNaN"})
  public void testFloatConstants() {
    assertTrue(Float.isNaN(Float.NaN));
    assertTrue(Float.isInfinite(Float.NEGATIVE_INFINITY));
    assertTrue(Float.isInfinite(Float.POSITIVE_INFINITY));
    assertTrue(Float.NEGATIVE_INFINITY < Float.POSITIVE_INFINITY);
    assertTrue(Float.MIN_VALUE < Float.MAX_VALUE);
    assertFalse(Float.NaN == Float.NaN);
    assertEquals(Float.SIZE, 32);
    // jdk1.6 assertEquals(Float.MIN_EXPONENT,
    // Math.getExponent(Float.MIN_NORMAL));
    // jdk1.6 assertEquals(Float.MAX_EXPONENT,
    // Math.getExponent(Float.MAX_VALUE));
    // issue 8073 - used to fail in prod mode
    assertFalse(Float.isInfinite(Float.NaN));
  }

  public void testIsFinite() {
    final float[] nonfiniteNumbers = {
        Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NaN,
    };
    for (float value : nonfiniteNumbers) {
      assertFalse(Float.isFinite(value));
    }

    final float[] finiteNumbers = {
        -Float.MAX_VALUE, Float.MAX_VALUE, Float.MIN_VALUE,
        -1.0f, -0.5f, -0.1f, -0.0f, 0.0f, 0.1f, 0.5f, 1.0f,
    };
    for (float value : finiteNumbers) {
      assertTrue(Float.isFinite(value));
    }
  }

  public void testIsInfinite() {
    assertTrue(Float.isInfinite(Float.NEGATIVE_INFINITY));
    assertTrue(Float.isInfinite(Float.POSITIVE_INFINITY));

    assertFalse(Float.isInfinite(Float.NaN));

    final float[] finiteNumbers = {
        -Float.MAX_VALUE, Float.MAX_VALUE, Float.MIN_VALUE,
        -1.0f, -0.5f, -0.1f, -0.0f, 0.0f, 0.1f, 0.5f, 1.0f,
    };
    for (float value : finiteNumbers) {
      assertFalse(Float.isInfinite(value));
    }
  }

  public void testParse() {
    /*
     * Note: we must use appropriate deltas for a somewhat subtle reason.
     * Parsing a string like "1.4e-45" in JS will return the closest DOUBLE
     * rather than the closest float. The value of the parse will not be the
     * same as the value of the same string literal interpreted as a float in
     * Java.
     */
    assertEquals(0f, Float.parseFloat("0"), 0.0);
    assertEquals(-1.5f, Float.parseFloat("-1.5"), 0.0);
    assertEquals(3.0f, Float.parseFloat("3."), 0.0);
    assertEquals(0.5f, Float.parseFloat(".5"), 0.0);

    // Test that a float/double type suffix is allowed
    assertEquals(1.0f, Float.parseFloat("1.0f"), 0.0);
    assertEquals(1.0f, Float.parseFloat("1.0F"), 0.0);
    assertEquals(1.0f, Float.parseFloat("1.0d"), 0.0);
    assertEquals(1.0f, Float.parseFloat("1.0D"), 0.0);

    // TODO(jat): it isn't safe to parse MAX/MIN_VALUE because we also want to
    // be able to get POSITIVE/NEGATIVE_INFINITY for out-of-range values, and
    // since all math in JS is done in double we can't rely on getting the
    // exact value back.
//    assertEquals("Can't parse MAX_VALUE", Float.MAX_VALUE,
//        Float.parseFloat(String.valueOf(Float.MAX_VALUE)), 1e31);
//    assertEquals("Can't parse MIN_VALUE", Float.MIN_VALUE,
//        Float.parseFloat(String.valueOf(Float.MIN_VALUE)), Float.MIN_VALUE);

    // Test NaN/Infinity - issue 7713
    assertTrue(Float.isNaN(Float.parseFloat("+NaN")));
    assertTrue(Float.isNaN(Float.parseFloat("NaN")));
    assertTrue(Float.isNaN(Float.parseFloat("-NaN")));
    assertEquals(Float.POSITIVE_INFINITY, Float.parseFloat("+Infinity"));
    assertEquals(Float.POSITIVE_INFINITY, Float.parseFloat("Infinity"));
    assertEquals(Float.NEGATIVE_INFINITY, Float.parseFloat("-Infinity"));

    // check for parsing some invalid values
    try {
      Float.parseFloat("nan");
      fail("Expected NumberFormatException");
    } catch (NumberFormatException expected) {
    }
    try {
      Float.parseFloat("infinity");
      fail("Expected NumberFormatException");
    } catch (NumberFormatException expected) {
    }
    try {
      Float.parseFloat("1.2.3");
      fail("Expected NumberFormatException");
    } catch (NumberFormatException expected) {
    }
    try {
      Float.parseFloat("+-1.2");
      fail("Expected NumberFormatException");
    } catch (NumberFormatException expected) {
    }
    try {
      Float.parseFloat("1e");
      fail("Expected NumberFormatException");
    } catch (NumberFormatException expected) {
    }
  }

  public void testFloatBits() {
    compareFloatBits(0x1, 1.401298464324817E-45F);
    compareFloatBits(0x2, 1.401298464324817E-45F * 2.0F);
    compareFloatBits(0x3, 1.401298464324817E-45F * 3.0F);
    compareFloatBits(0x00ba98, 1.401298464324817E-45F * 0x00ba98);
    compareFloatBits(8034422, 1.401298464324817E-45F * 8034422);
    compareFloatBits(0x7fffff, 1.401298464324817E-45F * 0x7fffff);
    compareFloatBits(0x80000001, -1.401298464324817E-45F);
    compareFloatBits(0x80000002, -1.401298464324817E-45F * 2.0F);
    compareFloatBits(0x80000003, -1.401298464324817E-45F * 3.0F);
    compareFloatBits(0x8000ba98, -1.401298464324817E-45F * 0x00ba98);
    compareFloatBits(0x807a9876, -1.401298464324817E-45F * 0x7a9876);
    compareFloatBits(0x807fffff, -1.401298464324817E-45F * 0x7fffff);

    // Very small non-denorms
    compareFloatBits(0x00800000, 1.1754943508222875E-38F);
    compareFloatBits(0x00800001, 1.175494490952134E-38F);
    compareFloatBits(0x00801234, 1.176147355906663E-38F);
    compareFloatBits(0x80800000, -1.1754943508222875E-38F);
    compareFloatBits(0x80800001, -1.175494490952134E-38F);
    compareFloatBits(0x80801234, -1.176147355906663E-38F);

    compareFloatBits(0x0, 0.0F);
    compareFloatBits(0x80000000, -0.0F);
    compareFloatBits(0x80000000, 1.0F / Float.NEGATIVE_INFINITY);
    compareFloatBits(0x7fc00000, Float.NaN);
    compareFloatBits(0x7f800000, Float.POSITIVE_INFINITY);
    compareFloatBits(0xff800000, Float.NEGATIVE_INFINITY);
    compareFloatBits(0x3f800000, 1.0F);
    compareFloatBits(0x40000000, 2.0F);
    compareFloatBits(0x3f7ffffe, 0.9999998807907104F);
    compareFloatBits(0x3f800001, 1.0000001192092896F);
    compareFloatBits(0x3fffffff, 1.9999998807907104F);
    compareFloatBits(0x40000000, 2.0F);
    compareFloatBits(0x3dcccccd, 0.10000000149011612F);
    compareFloatBits(0xbdcccccd, -0.10000000149011612F);
    compareFloatBits(0x3e4ccccd, 0.20000000298023224F);
    compareFloatBits(0xbe4ccccd, -0.20000000298023224F);
    compareFloatBits(0x42f6e9e0, 123.456787109375F);
    compareFloatBits(0xc2f6e9e0, -123.456787109375F);
    // Max float
    compareFloatBits(0x7f7fffff, 3.4028234663852886E38F);
    compareFloatBits(0xff7fffff, -3.4028234663852886E38F);
    compareFloatBits(0x80000001, -1.401298464324817E-45F);
    compareFloatBits(0x3e4cdcd4, 0.2000611424446106F);
    compareFloatBits(0x3f4ef68e, 0.8084496259689331F);
    compareFloatBits(0x3dd77088, 0.10519510507583618F);
    compareFloatBits(0x3e16156c, 0.14656609296798706F);
    compareFloatBits(0x3ea3776c, 0.3192704916000366F);
    compareFloatBits(0x3f510cbb, 0.816600501537323F);
    compareFloatBits(0x3ed6e3d6, 0.4197070002555847F);
    compareFloatBits(0x3f2209e6, 0.6329635381698608F);
    compareFloatBits(0x3f20fdd3, 0.6288730502128601F);
    compareFloatBits(0x3ecd6df2, 0.4012294411659241F);
    compareFloatBits(0x3f1a107a, 0.6018139123916626F);
    compareFloatBits(0x3f47e6d3, 0.7808658480644226F);
    compareFloatBits(0x3da82010, 0.08209240436553955F);
    compareFloatBits(0x3d1c0c20, 0.038097500801086426F);
    compareFloatBits(0x3f0adc42, 0.5424233675003052F);
    compareFloatBits(0x3f5fae9f, 0.8737582564353943F);
    compareFloatBits(0x3f4eba38, 0.8075289726257324F);
    compareFloatBits(0x3f23d86a, 0.6400209665298462F);
    compareFloatBits(0x3ea11a1a, 0.3146522641181946F);
    compareFloatBits(0x3e7a8824, 0.24465996026992798F);
    compareFloatBits(0x3ef758b2, 0.483098566532135F);
    compareFloatBits(0x3e8d1874, 0.275577187538147F);
    compareFloatBits(0x3dbc6968, 0.09199792146682739F);
    compareFloatBits(0x3e940d00, 0.28916168212890625F);
    compareFloatBits(0x3edd7ba2, 0.43258386850357056F);
    compareFloatBits(0x3edf10da, 0.4356754422187805F);
    compareFloatBits(0x3e9a3f84, 0.3012658357620239F);
    compareFloatBits(0x3f21db08, 0.6322484016418457F);
    compareFloatBits(0x3f10f0c8, 0.5661740303039551F);
    compareFloatBits(0x3f7b5bc9, 0.9818692803382874F);
    compareFloatBits(0x3f786c68, 0.9704041481018066F);
    compareFloatBits(0x3f3b3106, 0.7312167882919312F);
    compareFloatBits(0x3eef40e6, 0.46729201078414917F);
    compareFloatBits(0x3f2120ea, 0.6294084787368774F);
    compareFloatBits(0x3ece201c, 0.40258872509002686F);
    compareFloatBits(0x3f26e082, 0.6518632173538208F);
    compareFloatBits(0x3e1edd60, 0.15514135360717773F);
    compareFloatBits(0x3d2c6760, 0.042090773582458496F);
    compareFloatBits(0x3f1c99e3, 0.6117231249809265F);
    compareFloatBits(0x3f62a5de, 0.8853434324264526F);
    compareFloatBits(0x3f3ca39f, 0.7368716597557068F);
    compareFloatBits(0x3f2890bd, 0.6584585309028625F);
    compareFloatBits(0x3d7568a0, 0.059914231300354004F);
    compareFloatBits(0x3e96620e, 0.2937168478965759F);
    compareFloatBits(0x3d358bb0, 0.044322669506073F);
    compareFloatBits(0x3e9e2728, 0.30889248847961426F);
    compareFloatBits(0x3e887622, 0.2665262818336487F);
    compareFloatBits(0x3ec71942, 0.38886457681655884F);
    compareFloatBits(0x3f3ecf0c, 0.7453467845916748F);
    compareFloatBits(0x3f1d8b64, 0.615408182144165F);
    compareFloatBits(0x3f22e45e, 0.6362971067428589F);
    compareFloatBits(0x3f1bc5c0, 0.6084861755371094F);
    compareFloatBits(0x3ef2ce7c, 0.4742316007614136F);
    compareFloatBits(0x3ee6d16a, 0.45081645250320435F);
    compareFloatBits(0x3e22dbf4, 0.15904217958450317F);
    compareFloatBits(0x3ec8462e, 0.39116042852401733F);
    compareFloatBits(0x3eed4110, 0.46338701248168945F);
    compareFloatBits(0x3e7d46f0, 0.24734091758728027F);
    compareFloatBits(0x3ee4ed1a, 0.44712144136428833F);
    compareFloatBits(0x3e171310, 0.14753365516662598F);
    compareFloatBits(0x3f07ee13, 0.5309764742851257F);
    compareFloatBits(0x3ea82356, 0.3283945918083191F);
    compareFloatBits(0x3eaad676, 0.33366745710372925F);
    compareFloatBits(0x3f0b7415, 0.5447400212287903F);
    compareFloatBits(0x3e5da494, 0.2164481282234192F);
    compareFloatBits(0x3eb24b98, 0.3482329845428467F);
    compareFloatBits(0x3dbcf808, 0.09226995706558228F);
    compareFloatBits(0x3ebff9ec, 0.37495362758636475F);
    compareFloatBits(0x3ea1c5c6, 0.315962016582489F);
    compareFloatBits(0x3e922946, 0.2854711413383484F);
    compareFloatBits(0x3eb24736, 0.3481995463371277F);
    compareFloatBits(0x3d870700, 0.06593132019042969F);
    compareFloatBits(0x3db58dc0, 0.08864927291870117F);
    compareFloatBits(0x3f2fbba4, 0.6864569187164307F);
    compareFloatBits(0x3e67b5b4, 0.22627907991409302F);
    compareFloatBits(0x3e1b35d8, 0.151572585105896F);
    compareFloatBits(0x3eb18776, 0.3467366099357605F);
    compareFloatBits(0x3e4a1108, 0.19733059406280518F);
    compareFloatBits(0x3f77debb, 0.968242347240448F);
    compareFloatBits(0x3f2f3f2c, 0.6845576763153076F);
    compareFloatBits(0x3ee68150, 0.45020532608032227F);
    compareFloatBits(0x3da1ca40, 0.07899904251098633F);
    compareFloatBits(0x3f1a6205, 0.6030581593513489F);
    compareFloatBits(0x3e596a8c, 0.2123205065727234F);
    compareFloatBits(0x3f2b9b3d, 0.6703374981880188F);
    compareFloatBits(0x3f5a41df, 0.8525676131248474F);
    compareFloatBits(0x3f2ba95b, 0.6705529093742371F);
    compareFloatBits(0x3c636740, 0.013879597187042236F);
    compareFloatBits(0x3ea13618, 0.3148658275604248F);
    compareFloatBits(0x3ef32f54, 0.4749704599380493F);
    compareFloatBits(0x3db49fd8, 0.08819550275802612F);
    compareFloatBits(0x3ed2654e, 0.4109291434288025F);
    compareFloatBits(0x3f18e527, 0.5972465872764587F);
    compareFloatBits(0x3e86438e, 0.2622341513633728F);
    compareFloatBits(0x3d94d468, 0.07267075777053833F);
    compareFloatBits(0x3dec0730, 0.11524808406829834F);
    compareFloatBits(0x3e746c68, 0.23869478702545166F);
    compareFloatBits(0x3f7176bc, 0.9432179927825928F);
    compareFloatBits(0x3eb06baa, 0.34457141160964966F);
    compareFloatBits(0x3ec7873e, 0.3897036910057068F);

    compareFloatBits(0x3337354c, 4.2656481014091696E-8F);
    compareFloatBits(0xcef68e86, -2.068267776E9F);
    compareFloatBits(0x1aee11a3, 9.846298654970688E-23F);
    compareFloatBits(0x25855b49, 2.313367945844274E-16F);
    compareFloatBits(0x51bbb6d8, 1.0077831168E11F);
    compareFloatBits(0xd10cbbd1, -3.7777903616E10F);
    compareFloatBits(0x6b71ebcc, 2.9246464178639103E26F);
    compareFloatBits(0xa209e607, -1.868873766564279E-18F);
    compareFloatBits(0xa0fdd3be, -4.299998635695525E-19F);
    compareFloatBits(0x66b6f9c2, 4.320389591649362E23F);
    compareFloatBits(0x9a107a3f, -2.987725166002456E-23F);
    compareFloatBits(0xc7e6d303, -118182.0234375F);
    compareFloatBits(0x1504020d, 2.6658805490381716E-26F);
    compareFloatBits(0x9c0c256, 4.640507130264806E-33F);
    compareFloatBits(0x8adc428e, -2.1210264479196232E-32F);
    compareFloatBits(0xdfae9f63, -2.516576947109521E19F);
    compareFloatBits(0xceba38f7, -1.562147712E9F);
    compareFloatBits(0xa3d86a36, -2.346374900739753E-17F);
    compareFloatBits(0x508d0d6d, 1.8931738624E10F);
    compareFloatBits(0x3ea209d0, 0.3164811134338379F);
    compareFloatBits(0x7bac59a0, 1.7897857412574353E36F);
    compareFloatBits(0x468c3af4, 17949.4765625F);
    compareFloatBits(0x178d2da8, 9.123436692979724E-25F);
    compareFloatBits(0x4a068058, 2203670.0F);
    compareFloatBits(0x6ebdd138, 2.937279840252836E28F);
    compareFloatBits(0x6f886d95, 8.444487576529374E28F);
    compareFloatBits(0x4d1fc258, 1.67519616E8F);
    compareFloatBits(0xa1db0894, -1.48422878466768E-18F);
    compareFloatBits(0x90f0c84b, -9.497190880745409E-29F);
    compareFloatBits(0xfb5bc94f, -1.1411960353742999E36F);
    compareFloatBits(0xf86c6851, -1.9179653854596293E34F);
    compareFloatBits(0xbb31060d, -0.00270116631872952F);
    compareFloatBits(0x77a07357, 6.508647400938524E33F);
    compareFloatBits(0xa120ea93, -5.452056501780286E-19F);
    compareFloatBits(0x67100ede, 6.802950247361373E23F);
    compareFloatBits(0xa6e082db, -1.5578590790627281E-15F);
    compareFloatBits(0x27b7589e, 5.088878232380797E-15F);
    compareFloatBits(0xac6764c, 1.9111204788084013E-32F);
    compareFloatBits(0x9c99e3b0, -1.0183546536936767E-21F);
    compareFloatBits(0xe2a5de8f, -1.5298749044800828E21F);
    compareFloatBits(0xbca39f3d, -0.0199733916670084F);
    compareFloatBits(0xa890bd69, -1.6069355115761082E-14F);
    compareFloatBits(0xf568a12, 1.0577605982258498E-29F);
    compareFloatBits(0x4b310752, 1.1601746E7F);
    compareFloatBits(0xb58bb7e, 4.1741140243391215E-32F);
    compareFloatBits(0x4f139499, 2.475989248E9F);
    compareFloatBits(0x443b1161, 748.2715454101562F);
    compareFloatBits(0x638ca14d, 5.188334233065301E21F);
    compareFloatBits(0xbecf0c69, -0.4043915569782257F);
    compareFloatBits(0x9d8b6455, -3.689673453519375E-21F);
    compareFloatBits(0xa2e45ebb, -6.189982361684887E-18F);
    compareFloatBits(0x9bc5c04c, -3.2715185077444394E-22F);
    compareFloatBits(0x79673ea0, 7.504317251393587E34F);
    compareFloatBits(0x7368b523, 1.8436992802490676E31F);
    compareFloatBits(0x28b6fdf6, 2.031619704824343E-14F);
    compareFloatBits(0x6423179d, 1.2034083200995491E22F);
    compareFloatBits(0x76a0886c, 1.627996995533634E33F);
    compareFloatBits(0x3f51bc17, 0.8192762732505798F);
    compareFloatBits(0x72768dd4, 4.883505414291899E30F);
    compareFloatBits(0x25c4c43e, 3.4133559007908424E-16F);
    compareFloatBits(0x87ee1312, -3.582146842575625E-34F);
    compareFloatBits(0x5411ab9d, 2.502597804032E12F);
    compareFloatBits(0x556b3b74, 1.616503635968E13F);
    compareFloatBits(0x8b741536, -4.700864797886097E-32F);
    compareFloatBits(0x3769256b, 1.3896594282414299E-5F);
    compareFloatBits(0x5925cc76, 2.916761145966592E15F);
    compareFloatBits(0x179f010a, 1.0275396467808127E-24F);
    compareFloatBits(0x5ffcf643, 3.6455660418215444E19F);
    compareFloatBits(0x50e2e3b3, 3.0452586496E10F);
    compareFloatBits(0x4914a300, 608816.0F);
    compareFloatBits(0x59239bd2, 2.878234215579648E15F);
    compareFloatBits(0x10e0e0b4, 8.869863136638123E-29F);
    compareFloatBits(0x16b1b8a0, 2.8712407025600733E-25F);
    compareFloatBits(0xafbba4b9, -3.413214433312106E-10F);
    compareFloatBits(0x39ed6d41, 4.528556310106069E-4F);
    compareFloatBits(0x26cd7698, 1.4256877403358119E-15F);
    compareFloatBits(0x58c3bbb6, 1.721687838031872E15F);
    compareFloatBits(0x32844205, 1.53968446880981E-8F);
    compareFloatBits(0xf7debb7d, -9.035098568054132E33F);
    compareFloatBits(0xaf3f2c18, -1.738701405074039E-10F);
    compareFloatBits(0x7340a8d9, 1.5264063021291595E31F);
    compareFloatBits(0x1439482f, 9.354348821593712E-27F);
    compareFloatBits(0x9a62050f, -4.673979090873511E-23F);
    compareFloatBits(0x365aa321, 3.257948492318974E-6F);
    compareFloatBits(0xab9b3d23, -1.1030381252483124E-12F);
    compareFloatBits(0xda41dfad, -1.3642651156873216E16F);
    compareFloatBits(0xaba95bd7, -1.2033662911623E-12F);
    compareFloatBits(0x38d9d45, 8.323342486879323E-37F);
    compareFloatBits(0x509b0cf8, 2.08105472E10F);
    compareFloatBits(0x7997aaa7, 9.843725829681495E34F);
    compareFloatBits(0x1693fb40, 2.3907691910171116E-25F);
    compareFloatBits(0x6932a7e3, 1.3498851156559247E25F);
    compareFloatBits(0x98e52756, -5.923483173240392E-24F);
    compareFloatBits(0x4321c7ba, 161.78018188476562F);
    compareFloatBits(0x129a8db3, 9.753697906689189E-28F);
    compareFloatBits(0x1d80e684, 3.411966546003519E-21F);
    compareFloatBits(0x3d1b1a71, 0.03786701336503029F);
    compareFloatBits(0xf176bcf8, -1.221788185872419E30F);
    compareFloatBits(0x5835d550, 7.99711099355136E14F);
    compareFloatBits(0x63c39f88, 7.217221064844452E21F);
  }

  private void compareFloatBits(int bits, float value) {
    assertEquals(bits, Float.floatToIntBits(value));
    compareFloats(value, Float.intBitsToFloat(bits));

    assertEquals(bits, Float.floatToIntBits(Float.intBitsToFloat(bits)));
    compareFloats(value, Float.intBitsToFloat(Float.floatToIntBits(value)));
  }
 
  private void compareFloats(float expected, float actual) {
    if (Float.isNaN(expected) || Float.isNaN(actual)) {
      if (Float.isNaN(expected) && Float.isNaN(actual)) {
        return;
      }
      fail("NaN");
    }
    assertEquals(expected, actual);
  }
}
