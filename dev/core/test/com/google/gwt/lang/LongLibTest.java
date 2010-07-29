package com.google.gwt.lang;

import com.google.gwt.lang.LongLibBase.LongEmul;

import junit.framework.TestCase;

import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

public class LongLibTest extends TestCase {

  private static abstract class BinaryOp extends Op {
    public BinaryOp(String name) {
      super(name);
    }

    public abstract long ref(long longVal0, long longVal1);

    public abstract LongEmul test(LongEmul longVal0, LongEmul longVal1);
  }

  private static abstract class BooleanOp extends Op {
    public BooleanOp(String name) {
      super(name);
    }

    public abstract boolean ref(long longVal0, long longVal1);

    public abstract boolean test(LongEmul longVal0, LongEmul longVal1);
  }

  private static abstract class CompareOp extends Op {
    public CompareOp(String name) {
      super(name);
    }

    public abstract int ref(long longVal0, long longVal1);

    public abstract int test(LongEmul longVal0, LongEmul longVal1);
  }

  private static abstract class Op {
    String name;

    public Op(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }

  private static abstract class ShiftOp extends Op {
    public ShiftOp(String name) {
      super(name);
    }

    public abstract long ref(long longVal, int shift);

    public abstract LongEmul test(LongEmul longVak, int shift);
  }

  private static abstract class UnaryOp extends Op {
    public UnaryOp(String name) {
      super(name);
    }

    public abstract long ref(long longVal);

    public abstract LongEmul test(LongEmul longVal);
  }

  private static final int BASE_VALUES = 128;

  private static final BinaryOp OP_ADD = new BinaryOp("ADD") {
    @Override
    public long ref(long longVal0, long longVal1) {
      return longVal0 + longVal1;
    }

    @Override
    public LongEmul test(LongEmul longVal0, LongEmul longVal1) {
      return LongLib.add(longVal0, longVal1);
    }
  };

  private static final BinaryOp OP_AND = new BinaryOp("AND") {
    @Override
    public long ref(long longVal0, long longVal1) {
      return longVal0 & longVal1;
    }

    @Override
    public LongEmul test(LongEmul longVal0, LongEmul longVal1) {
      return LongLib.and(longVal0, longVal1);
    }
  };

  private static final CompareOp OP_COMPARE = new CompareOp("COMPARE") {
    @Override
    public int ref(long longVal0, long longVal1) {
      if (longVal0 < longVal1) {
        return -1;
      }
      if (longVal0 > longVal1) {
        return 1;
      }
      return 0;
    }

    @Override
    public int test(LongEmul longVal0, LongEmul longVal1) {
      return LongLib.compare(longVal0, longVal1);
    }
  };

  private static final BinaryOp OP_DIV = new BinaryOp("DIV") {
    @Override
    public long ref(long longVal0, long longVal1) {
      return longVal0 / longVal1;
    }

    @Override
    public LongEmul test(LongEmul longVal0, LongEmul longVal1) {
      return LongLib.div(longVal0, longVal1);
    }
  };

  private static final BooleanOp OP_EQ = new BooleanOp("EQ") {
    @Override
    public boolean ref(long longVal0, long longVal1) {
      return longVal0 == longVal1;
    }

    @Override
    public boolean test(LongEmul longVal0, LongEmul longVal1) {
      return LongLib.eq(longVal0, longVal1);
    }
  };

  private static final BooleanOp OP_GT = new BooleanOp("GT") {
    @Override
    public boolean ref(long longVal0, long longVal1) {
      return longVal0 > longVal1;
    }

    @Override
    public boolean test(LongEmul longVal0, LongEmul longVal1) {
      return LongLib.gt(longVal0, longVal1);
    }
  };

  private static final BooleanOp OP_GTE = new BooleanOp("GTE") {
    @Override
    public boolean ref(long longVal0, long longVal1) {
      return longVal0 >= longVal1;
    }

    @Override
    public boolean test(LongEmul longVal0, LongEmul longVal1) {
      return LongLib.gte(longVal0, longVal1);
    }
  };

  private static final BooleanOp OP_LT = new BooleanOp("LT") {
    @Override
    public boolean ref(long longVal0, long longVal1) {
      return longVal0 < longVal1;
    }

    @Override
    public boolean test(LongEmul longVal0, LongEmul longVal1) {
      return LongLib.lt(longVal0, longVal1);
    }
  };

  private static final BooleanOp OP_LTE = new BooleanOp("LTE") {
    @Override
    public boolean ref(long longVal0, long longVal1) {
      return longVal0 <= longVal1;
    }

    @Override
    public boolean test(LongEmul longVal0, LongEmul longVal1) {
      return LongLib.lte(longVal0, longVal1);
    }
  };

  private static final BinaryOp OP_MOD = new BinaryOp("MOD") {
    @Override
    public long ref(long longVal0, long longVal1) {
      return longVal0 % longVal1;
    }

    @Override
    public LongEmul test(LongEmul longVal0, LongEmul longVal1) {
      return LongLib.mod(longVal0, longVal1);
    }
  };

  private static final BinaryOp OP_MUL = new BinaryOp("MUL") {
    @Override
    public long ref(long longVal0, long longVal1) {
      return longVal0 * longVal1;
    }

    @Override
    public LongEmul test(LongEmul longVal0, LongEmul longVal1) {
      return LongLib.mul(longVal0, longVal1);
    }
  };

  private static final UnaryOp OP_NEG = new UnaryOp("NEG") {
    @Override
    public long ref(long longVal) {
      return -longVal;
    }

    @Override
    public LongEmul test(LongEmul longVal) {
      return LongLib.neg(longVal);
    }
  };

  private static final BooleanOp OP_NEQ = new BooleanOp("NEQ") {
    @Override
    public boolean ref(long longVal0, long longVal1) {
      return longVal0 != longVal1;
    }

    @Override
    public boolean test(LongEmul longVal0, LongEmul longVal1) {
      return LongLib.neq(longVal0, longVal1);
    }
  };

  private static final UnaryOp OP_NOT = new UnaryOp("NOT") {
    @Override
    public long ref(long longVal) {
      return ~longVal;
    }

    @Override
    public LongEmul test(LongEmul longVal) {
      return LongLib.not(longVal);
    }
  };

  private static final BinaryOp OP_OR = new BinaryOp("OR") {
    @Override
    public long ref(long longVal0, long longVal1) {
      return longVal0 | longVal1;
    }

    @Override
    public LongEmul test(LongEmul longVal0, LongEmul longVal1) {
      return LongLib.or(longVal0, longVal1);
    }
  };

  private static final ShiftOp OP_SHL = new ShiftOp("SHL") {
    @Override
    public long ref(long longVal, int shift) {
      return longVal << shift;
    }

    @Override
    public LongEmul test(LongEmul longVal, int shift) {
      return LongLib.shl(longVal, shift);
    }
  };

  private static final ShiftOp OP_SHR = new ShiftOp("SHR") {
    @Override
    public long ref(long longVal, int shift) {
      return longVal >> shift;
    }

    @Override
    public LongEmul test(LongEmul longVal, int shift) {
      return LongLib.shr(longVal, shift);
    }
  };

  private static final ShiftOp OP_SHRU = new ShiftOp("SHRU") {
    @Override
    public long ref(long longVal, int shift) {
      return longVal >>> shift;
    }

    @Override
    public LongEmul test(LongEmul longVal, int shift) {
      return LongLib.shru(longVal, shift);
    }
  };

  private static final BinaryOp OP_SUB = new BinaryOp("SUB") {
    @Override
    public long ref(long longVal0, long longVal1) {
      return longVal0 - longVal1;
    }

    @Override
    public LongEmul test(LongEmul longVal0, LongEmul longVal1) {
      return LongLib.sub(longVal0, longVal1);
    }
  };

  private static final BinaryOp OP_XOR = new BinaryOp("XOR") {
    @Override
    public long ref(long longVal0, long longVal1) {
      return longVal0 ^ longVal1;
    }

    @Override
    public LongEmul test(LongEmul longVal0, LongEmul longVal1) {
      return LongLib.xor(longVal0, longVal1);
    }
  };

  private static final Random rand = new Random(1);

  private static final int RANDOM_TESTS = 1000;

  private static long[] TEST_VALUES;

  static {
    LongLibBase.RUN_IN_JVM = true;
  }

  static {
    Set<Long> testSet = new TreeSet<Long>();
    for (long i = 0; i < BASE_VALUES; i++) {
      testSet.add(i);
      testSet.add(-i);

      testSet.add(Long.MIN_VALUE + i);
      testSet.add(Long.MAX_VALUE - i);

      testSet.add(i << LongLibBase.BITS / 2);
      testSet.add(i << LongLibBase.BITS);
      testSet.add(i << (3 * LongLibBase.BITS) / 2);
      testSet.add(i << 2 * LongLibBase.BITS);
      testSet.add(i << (5 * LongLibBase.BITS) / 2);
    }

    for (int i = 0; i < 16; i++) {
      testSet.add(0x1111111111111111L * i);
      testSet.add(~(0x1111111111111111L * i));
      testSet.add(-(0x1111111111111111L * i));
      testSet.add(0x1010101010101010L * i);
      testSet.add(~(0x1010101010101010L * i));
      testSet.add(-(0x1010101010101010L * i));
      testSet.add(0x0101010101010101L * i);
      testSet.add(~(0x0101010101010101L * i));
      testSet.add(-(0x0101010101010101L * i));
      testSet.add(0x123456789ABCDEFFL * i);
      testSet.add(~(0x123456789ABCDEFFL * i));
      testSet.add(-(0x123456789ABCDEFFL * i));
    }

    for (int i = 0; i < 64; i += 4) {
      testSet.add(0x1L << i);
      testSet.add(~(0x1L << i));
      testSet.add(0x123456789ABCDEFFL >> i);
      testSet.add(-(0x123456789ABCDEFFL >> i));

      // Powers of two and nearby numbers
      testSet.add(0x1L << i);
      for (int j = 1; j <= 16; j++) {
        testSet.add((0x1L << i) + j);
        testSet.add(-((0x1L << i) + j));
        testSet.add(~((0x1L << i) + j));
        testSet.add((0x1L << i) - j);
        testSet.add(-((0x1L << i) - j));
        testSet.add(~((0x1L << i) - j));
        testSet.add((0x3L << i) + j);
        testSet.add(-((0x3L << i) + j));
        testSet.add(~((0x3L << i) + j));
        testSet.add((0x3L << i) - j);
        testSet.add(-((0x3L << i) - j));
        testSet.add(~((0x3L << i) - j));
      }
    }

    for (int a = 0; a < 19; a++) {
      testSet.add((long) Math.pow(10, a));
    }

    TEST_VALUES = new long[testSet.size()];
    int index = 0;
    for (long val : testSet) {
      TEST_VALUES[index++] = val;
    }

    System.out.println("VALUES.length = " + index);
  }

  public static void testAdd() {
    doTestBinary(OP_ADD);
  }

  public static void testAnd() {
    doTestBinary(OP_AND);
  }

  public static void testBase64() {
    assertEquals("A", LongLib.toBase64(0x0L));
    assertEquals(0x0L, LongLib.longFromBase64("A"));

    assertEquals("B", LongLib.toBase64(0x1L));
    assertEquals(0x1L, LongLib.longFromBase64("B"));

    assertEquals("BA", LongLib.toBase64(0x40L));
    assertEquals(0x40L, LongLib.longFromBase64("BA"));

    assertEquals("P_________A", LongLib.toBase64(-0x40L));
    assertEquals(-0x40L, LongLib.longFromBase64("P_________A"));

    assertEquals("P__________", LongLib.toBase64(-1L));
    assertEquals(-1L, LongLib.longFromBase64("P__________"));

    // Use all types of base 64 chars
    long value = 0L;
    value |= 15L << 60; // 'P'
    value |= 35L << 54; // 'j'
    value |= 44L << 48; // 's'
    value |= 62L << 42; // '$'
    value |= 26L << 36; // 'a'
    value |=  9L << 30; // 'J'
    value |= 18L << 24; // 'S'
    value |= 25L << 18; // 'Z'
    value |= 52L << 12; // '0'
    value |= 57L << 6;  // '5'
    value |= 63L;       // '_'

    String s = "Pjs$aJSZ05_";
    assertEquals(s, LongLib.toBase64(value));
    assertEquals(value, LongLib.longFromBase64(s));
  }

  public static void testCompare() {
    doTestCompare(OP_COMPARE);
  }

  public static void testDiv() {
    doTestBinary(OP_DIV);
  }

  public static void testEq() {
    doTestBoolean(OP_EQ);
  }

  public static void testGetAsIntArray() {
    long longVal = 0x123456789abcdef0L;
    int[] array = LongLib.getAsIntArray(longVal);
    assertEquals(0x12345, array[2]);
    assertEquals(0x19e26a, array[1]);
    assertEquals(0x3cdef0, array[0]);

    longVal = -longVal;
    array = LongLib.getAsIntArray(longVal);
    assertEquals(0xedcba, array[2]);
    assertEquals(0x261d95, array[1]);
    assertEquals(0x32110, array[0]);
  }

  public static void testGt() {
    doTestBoolean(OP_GT);
  }

  public static void testGte() {
    doTestBoolean(OP_GTE);
  }

  public static void testLt() {
    doTestBoolean(OP_LT);
  }

  public static void testLte() {
    doTestBoolean(OP_LTE);
  }

  public static void testMod() {
    doTestBinary(OP_MOD);
  }

  public static void testMul() {
    doTestBinary(OP_MUL);
  }

  public static void testNeg() {
    doTestUnary(OP_NEG);
  }

  public static void testNeq() {
    doTestBoolean(OP_NEQ);
  }

  public static void testNot() {
    doTestUnary(OP_NOT);
  }

  public static void testNumberOfLeadingZeros() {
    LongEmul longVal0 = fromLong(0xfedcba9876543210L);
    for (int i = 0; i <= 64; i++) {
      assertEquals(i, LongLibBase.numberOfLeadingZeros(longVal0));
      longVal0 = LongLib.shru(longVal0, 1);
    }
  }

  public static void testOr() {
    doTestBinary(OP_OR);
  }

  public static void testRoundTrip() {
    System.out.println("ROUND_TRIP");

    for (int i = 0; i < TEST_VALUES.length + RANDOM_TESTS; i++) {
      long longVal0 = (i < TEST_VALUES.length) ? TEST_VALUES[i]
          : rand.nextLong();
      LongEmul longVal1 = fromLong(longVal0);
      long l2 = toLong(longVal1);
      if (longVal0 != l2) {
        fail("longVal0 = " + longVal0 + ", l2 = " + l2);
      }
      if (!toHex(longVal0).equals(toHex(longVal1))) {
        fail("toHex(longVal0) = " + toHex(longVal0) + ", toHex(longVal1) = "
            + toHex(longVal1));
      }
      if (!toHex(longVal0).equals(toHex(l2))) {
        fail("toHex(longVal0) = " + toHex(longVal0) + ", toHex(l2) = "
            + toHex(l2));
      }
      if (!LongLib.toString(longVal1).equals(Long.toString(longVal0))) {
        fail("toString(longVal0) = " + Long.toString(longVal0)
            + ", toString(longVal1) = " + LongLib.toString(longVal1));
      }
    }

    for (int i = 0; i < TEST_VALUES.length + RANDOM_TESTS; i++) {
      double d0 = (i < TEST_VALUES.length) ? TEST_VALUES[i]
          : (rand.nextDouble() - 0.5) * 3.0 * Long.MAX_VALUE;
      long longVal0 = (long) d0;
      LongEmul longVal1 = LongLib.fromDouble(d0);
      long l2 = toLong(longVal1);
      if (longVal0 != l2) {
        fail("d0 = " + d0 + ", longVal0 = " + longVal0 + ", l2 = " + l2);
      }
    }

    for (int i = 0; i < TEST_VALUES.length + RANDOM_TESTS; i++) {
      long longVal0 = i < TEST_VALUES.length ? TEST_VALUES[i] : rand.nextLong();
      // Find a round-trip capable value
      double d0 = longVal0;
      longVal0 = (long) d0;

      LongEmul longVal1 = fromLong(longVal0);
      double d1 = LongLib.toDouble(longVal1);
      LongEmul l2 = LongLib.fromDouble(d1);
      long l3 = toLong(l2);

      if (longVal0 != l3) {
        fail("longVal0 = " + longVal0 + ", d1 = " + d1 + ", l3 = " + l3);
      }
    }

    for (int i = 0; i < TEST_VALUES.length + RANDOM_TESTS; i++) {
      int i0 = i < TEST_VALUES.length ? (int) TEST_VALUES[i] : rand.nextInt();
      long longVal0 = i0;
      LongEmul longVal1 = LongLib.fromInt(i0);
      long l2 = toLong(longVal1);
      if (longVal0 != l2) {
        fail("i0 = " + i0 + ", longVal0 = " + longVal0 + ", l2 = " + l2);
      }
    }

    if (toLong(LongLib.fromDouble(Double.NaN)) != 0) {
      fail("fromDouble(Nan) != 0");
    }
    if (toLong(LongLib.fromDouble(Double.NEGATIVE_INFINITY)) != Long.MIN_VALUE) {
      fail("fromDouble(-Inf) != MIN_VALUE");
    }
    if (toLong(LongLib.fromDouble(Double.POSITIVE_INFINITY)) != Long.MAX_VALUE) {
      fail("fromDouble(+Inf) != MAX_VALUE");
    }
    if (LongLib.toDouble(fromLong(Long.MIN_VALUE)) != -9223372036854775808.0) {
      fail("toDouble(Long.MIN_VALUE) != -9223372036854775808.0");
    }
    if (LongLib.toDouble(fromLong(Long.MAX_VALUE)) != 9223372036854775807.0) {
      fail("toDouble(Long.MAX_VALUE) != 9223372036854775807.0");
    }
  }

  public static void testShl() {
    doTestShift(OP_SHL);
  }

  public static void testShr() {
    doTestShift(OP_SHR);
  }

  public static void testShru() {
    doTestShift(OP_SHRU);
  }

  public static void testSub() {
    doTestBinary(OP_SUB);
  }

  public static void testXor() {
    doTestBinary(OP_XOR);
  }

  private static LongEmul copy(LongEmul longVal) {
    LongEmul result = new LongEmul();
    result.l = longVal.l;
    result.m = longVal.m;
    result.h = longVal.h;
    return result;
  }

  private static void doTestBinary(BinaryOp op) {
    System.out.println(op.getName());
    for (int i = 0; i < TEST_VALUES.length; i++) {
      long randomLong = rand.nextLong();
      doTestBinary(op, TEST_VALUES[i], randomLong);
      doTestBinary(op, randomLong, TEST_VALUES[i]);
      for (int j = 0; j < TEST_VALUES.length; j++) {
        doTestBinary(op, TEST_VALUES[i], TEST_VALUES[j]);
      }
    }
    for (int i = 0; i < RANDOM_TESTS; i++) {
      long longVal0 = rand.nextLong();
      long longVal1 = rand.nextLong();
      if (rand.nextInt(20) == 0) {
        if (rand.nextInt(2) == 0) {
          longVal1 = longVal0;
        } else {
          longVal1 = -longVal0;
        }
      }
      doTestBinary(op, longVal0, longVal1);
    }
  }

  private static void doTestBinary(BinaryOp op, long longVal0, long longVal1) {
    boolean refException = false;
    long ref = -1;
    try {
      ref = op.ref(longVal0, longVal1);
    } catch (ArithmeticException e) {
      refException = true;
    }
    boolean testException = false;
    long result = -2;
    try {
      LongEmul llongVal0 = fromLong(longVal0);
      LongEmul llongVal1 = fromLong(longVal1);
      LongEmul save_llongVal0 = copy(llongVal0);
      LongEmul save_llongVal1 = copy(llongVal1);

      result = toLong(op.test(llongVal0, llongVal1));
      if (!LongLib.eq(llongVal0, save_llongVal0)) {
        System.out.println("Test altered first argument");
      }
      if (!LongLib.eq(llongVal1, save_llongVal1)) {
        System.out.println("Test altered second argument");
      }
    } catch (ArithmeticException e) {
      testException = true;
    }
    if (testException && refException) {
      return;
    }
    if (testException != refException) {
      fail(op.getName() + ": longVal0 = " + longVal0 + ", longVal1 = "
          + longVal1 + ", testException = " + testException
          + ", refException = " + refException);
      return;
    }
    if (ref != result) {
      fail(op.getName() + ": longVal0 = " + longVal0 + ", longVal1 = "
          + longVal1);
      toLong(op.test(fromLong(longVal0), fromLong(longVal1)));
    }
  }

  private static void doTestBoolean(BooleanOp op) {
    System.out.println(op.getName());
    for (int i = 0; i < TEST_VALUES.length; i++) {
      long randomLong = rand.nextLong();
      doTestBoolean(op, TEST_VALUES[i], randomLong);
      doTestBoolean(op, randomLong, TEST_VALUES[i]);
      for (int j = 0; j < TEST_VALUES.length; j++) {
        doTestBoolean(op, TEST_VALUES[i], TEST_VALUES[j]);
      }
    }
    for (int i = 0; i < RANDOM_TESTS; i++) {
      long longVal0 = rand.nextLong();
      long longVal1 = rand.nextLong();
      if (rand.nextInt(20) == 0) {
        if (rand.nextInt(2) == 0) {
          longVal1 = longVal0;
        } else {
          longVal1 = -longVal0;
        }
      }
      doTestBoolean(op, longVal0, longVal1);
    }
  }

  private static void doTestBoolean(BooleanOp op, long longVal0, long longVal1) {
    boolean ref = op.ref(longVal0, longVal1);
    boolean result = op.test(fromLong(longVal0), fromLong(longVal1));
    if (ref != result) {
      fail(op.getName() + ": longVal0 = " + longVal0 + ", longVal1 = "
          + longVal1);
    }
  }

  private static void doTestCompare(CompareOp op) {
    System.out.println(op.getName());
    for (int i = 0; i < TEST_VALUES.length; i++) {
      long randomLong = rand.nextLong();
      doTestCompare(op, TEST_VALUES[i], randomLong);
      doTestCompare(op, randomLong, TEST_VALUES[i]);
      for (int j = 0; j < TEST_VALUES.length; j++) {
        doTestCompare(op, TEST_VALUES[i], TEST_VALUES[j]);
      }
    }
    for (int i = 0; i < RANDOM_TESTS; i++) {
      long longVal0 = rand.nextLong();
      long longVal1 = rand.nextLong();
      if (rand.nextInt(20) == 0) {
        if (rand.nextInt(2) == 0) {
          longVal1 = longVal0;
        } else {
          longVal1 = -longVal0;
        }
      }
      doTestCompare(op, longVal0, longVal1);
    }
  }

  private static void doTestCompare(CompareOp op, long longVal0, long longVal1) {
    int ref = op.ref(longVal0, longVal1);
    int result = op.test(fromLong(longVal0), fromLong(longVal1));
    if (ref < 0) {
      ref = -1;
    } else if (ref > 0) {
      ref = 1;
    }
    if (result < 0) {
      result = -1;
    } else if (result > 0) {
      result = 1;
    }

    if (ref != result) {
      fail(op.getName() + ": longVal0 = " + longVal0 + ", longVal1 = "
          + longVal1 + ", ref = " + ref + ", result = " + result);
    }
  }

  private static void doTestShift(ShiftOp op) {
    System.out.println(op.getName());
    for (int i = 0; i < TEST_VALUES.length; i++) {
      for (int shift = -64; shift <= 64; shift++) {
        doTestShift(op, TEST_VALUES[i], shift);
      }
    }
    for (int i = 0; i < RANDOM_TESTS; i++) {
      long randomLong = rand.nextLong();
      for (int shift = -64; shift <= 64; shift++) {
        doTestShift(op, randomLong, shift);
      }
    }
  }

  private static void doTestShift(ShiftOp op, long longVal, int shift) {
    long ref = op.ref(longVal, shift);
    long result = toLong(op.test(fromLong(longVal), shift));
    if (ref != result) {
      fail(op.getName() + ": longVal = " + longVal + ", shift = " + shift);
    }
  }

  private static void doTestUnary(UnaryOp op) {
    System.out.println(op.getName());
    for (int i = 0; i < TEST_VALUES.length; i++) {
      doTestUnary(op, TEST_VALUES[i]);
    }
    for (int i = 0; i < RANDOM_TESTS; i++) {
      long randomLong = rand.nextLong();
      doTestUnary(op, randomLong);
    }
  }

  private static void doTestUnary(UnaryOp op, long longVal) {
    long ref = op.ref(longVal);
    long result = toLong(op.test(fromLong(longVal)));
    if (ref != result) {
      fail(op.getName() + ": longVal = " + longVal);
    }
  }

  private static LongEmul fromLong(long longVal) {
    LongEmul result = new LongEmul();
    result.l = (int) (longVal & LongLibBase.MASK);
    result.m = (int) ((longVal >> LongLibBase.BITS) & LongLibBase.MASK);
    result.h = (int) ((longVal >> (2 * LongLibBase.BITS)) & LongLibBase.MASK_2);
    return result;
  }

  private static int getBit(LongEmul longVal, int bit) {
    if (bit < LongLibBase.BITS) {
      return (longVal.l >> bit) & 0x1;
    }
    if (bit < 2 * LongLibBase.BITS) {
      return (longVal.m >> (bit - LongLibBase.BITS)) & 0x1;
    }
    return (longVal.h >> (bit - (2 * LongLibBase.BITS))) & 0x1;
  }

  private static String toHex(long longVal) {
    String result = Long.toHexString(longVal);
    while (result.length() < 16) {
      result = "0" + result;
    }
    return result;
  }

  private static String toHex(LongEmul longVal) {
    StringBuilder sb = new StringBuilder();
    for (int i = 60; i >= 0; i -= 4) {
      int b0 = getBit(longVal, i);
      int b1 = getBit(longVal, i + 1);
      int b2 = getBit(longVal, i + 2);
      int b3 = getBit(longVal, i + 3);
      int b = (b3 << 3) + (b2 << 2) + (b1 << 1) + b0;
      if (b < 10) {
        sb.append(b);
      } else {
        sb.append((char) ('a' + b - 10));
      }
    }
    return sb.toString();
  }

  private static long toLong(LongEmul longVal) {
    long b2 = ((long) longVal.h) << (2 * LongLibBase.BITS);
    long b1 = ((long) longVal.m) << LongLibBase.BITS;
    return b2 | b1 | longVal.l;
  }

  public LongLibTest() {
  }
}
