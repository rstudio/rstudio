/*
 * Copyright 2017 Google Inc.
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
package java.lang.reflect;

import static javaemul.internal.InternalPreconditions.checkArgument;
import static javaemul.internal.InternalPreconditions.checkNotNull;

import javaemul.internal.ArrayHelper;

/**
 * See <a
 * href="http://java.sun.com/javase/6/docs/api/java/lang/reflect/Array.html">the
 * official Java API doc</a> for details.
 */
public final class Array {

  public static Object get(Object array, int index) {
    if (array instanceof boolean[]) {
      return getBooleanImpl(array, index);
    } else if (array instanceof byte[]) {
      return getByteImpl(array, index);
    } else if (array instanceof char[]) {
      return getCharImpl(array, index);
    } else if (array instanceof double[]) {
      return getDoubleImpl(array, index);
    } else if (array instanceof float[]) {
      return getFloatImpl(array, index);
    } else if (array instanceof int[]) {
      return getIntImpl(array, index);
    } else if (array instanceof long[]) {
      return getLongImpl(array, index);
    } else if (array instanceof short[]) {
      return getShortImpl(array, index);
    } else  {
      checkArgument(array instanceof Object[]);
      Object[] typedArray = (Object[]) array;
      return typedArray[index];
    }
  }

  public static boolean getBoolean(Object array, int index) {
    checkArgument(array instanceof boolean[]);
    return getBooleanImpl(array, index);
  }

  private static boolean getBooleanImpl(Object array, int index) {
    boolean[] typedArray = (boolean[]) array;
    return typedArray[index];
  }

  public static byte getByte(Object array, int index) {
    checkArgument(array instanceof byte[]);
    return getByteImpl(array, index);
  }

  private static byte getByteImpl(Object array, int index) {
    byte[] typedArray = (byte[]) array;
    return typedArray[index];
  }

  public static char getChar(Object array, int index) {
    checkArgument(array instanceof char[]);
    return getCharImpl(array, index);
  }

  private static char getCharImpl(Object array, int index) {
    char[] typedArray = (char[]) array;
    return typedArray[index];
  }

  public static double getDouble(Object array, int index) {
    if (array instanceof double[]) {
      return getDoubleImpl(array, index);
    } else if (array instanceof byte[]) {
      return getByteImpl(array, index);
    } else if (array instanceof char[]) {
      return getCharImpl(array, index);
    } else if (array instanceof float[]) {
      return getFloatImpl(array, index);
    } else if (array instanceof int[]) {
      return getIntImpl(array, index);
    } else if (array instanceof long[]) {
      return getLongImpl(array, index);
    } else if (array instanceof short[]) {
      return getShortImpl(array, index);
    } else {
      checkArgument(false);
      return 0;
    }
  }

  private static double getDoubleImpl(Object array, int index) {
    double[] typedArray = (double[]) array;
    return typedArray[index];
  }

  public static float getFloat(Object array, int index) {
    if (array instanceof float[]) {
      return getFloatImpl(array, index);
    } else if (array instanceof byte[]) {
      return getByteImpl(array, index);
    } else if (array instanceof char[]) {
      return getCharImpl(array, index);
    } else if (array instanceof int[]) {
      return getIntImpl(array, index);
    } else if (array instanceof long[]) {
      return getLongImpl(array, index);
    } else if (array instanceof short[]) {
      return getShortImpl(array, index);
    } else {
      checkArgument(false);
      return 0;
    }
  }

  private static float getFloatImpl(Object array, int index) {
    float[] typedArray = (float[]) array;
    return typedArray[index];
  }

  public static int getInt(Object array, int index) {
    if (array instanceof int[]) {
      return getIntImpl(array, index);
    } else if (array instanceof byte[]) {
      return getByteImpl(array, index);
    } else if (array instanceof char[]) {
      return getCharImpl(array, index);
    } else if (array instanceof short[]) {
      return getShortImpl(array, index);
    } else {
      checkArgument(false);
      return 0;
    }
  }

  private static int getIntImpl(Object array, int index) {
    int[] typedArray = (int[]) array;
    return typedArray[index];
  }

  public static int getLength(Object array) {
    checkNotNull(array);
    return ArrayHelper.getLength(array);
  }

  public static long getLong(Object array, int index) {
    if (array instanceof long[]) {
      return getLongImpl(array, index);
    } else if (array instanceof byte[]) {
      return getByteImpl(array, index);
    } else if (array instanceof char[]) {
      return getCharImpl(array, index);
    } else if (array instanceof int[]) {
      return getIntImpl(array, index);
    } else if (array instanceof short[]) {
      return getShortImpl(array, index);
    } else {
      checkArgument(false);
      return 0;
    }
  }

  private static long getLongImpl(Object array, int index) {
    long[] typedArray = (long[]) array;
    return typedArray[index];
  }

  public static short getShort(Object array, int index) {
    if (array instanceof short[]) {
      return getShortImpl(array, index);
    } else if (array instanceof byte[]) {
      return getByteImpl(array, index);
    } else {
      checkArgument(false);
      return 0;
    }
  }

  private static short getShortImpl(Object array, int index) {
    short[] typedArray = (short[]) array;
    return typedArray[index];
  }

  public static void set(Object array, int index, Object value) {
    if (array instanceof Object[]) {
      Object[] typedArray = (Object[]) array;
      typedArray[index] = value;
    } else {
      if (value instanceof Boolean) {
        setBoolean(array, index, ((Boolean) value).booleanValue());
      } else if (value instanceof Byte) {
        setByte(array, index, ((Byte) value).byteValue());
      } else if (value instanceof Character) {
        setChar(array, index, ((Character) value).charValue());
      } else if (value instanceof Short) {
        setShort(array, index, ((Short) value).shortValue());
      } else if (value instanceof Integer) {
        setInt(array, index, ((Integer) value).intValue());
      } else if (value instanceof Long) {
        setLong(array, index, ((Long) value).longValue());
      } else if (value instanceof Float) {
        setFloat(array, index, ((Float) value).floatValue());
      } else if (value instanceof Double) {
        setDouble(array, index, ((Double) value).doubleValue());
      } else {
        checkArgument(false);
      }
    }
  }

  public static void setBoolean(Object array, int index, boolean value) {
    checkArgument(array instanceof boolean[]);
    setBooleanImpl(array, index, value);
  }

  private static void setBooleanImpl(Object array, int index, boolean value) {
    boolean[] typedArray = (boolean[]) array;
    typedArray[index] = value;
  }

  public static void setByte(Object array, int index, byte value) {
    if (array instanceof byte[]) {
      setByteImpl(array, index, value);
    } else if (array instanceof double[]) {
      setDoubleImpl(array, index, value);
    } else if (array instanceof float[]) {
      setFloatImpl(array, index, value);
    } else if (array instanceof int[]) {
      setIntImpl(array, index, value);
    } else if (array instanceof long[]) {
      setLongImpl(array, index, value);
    } else if (array instanceof short[]) {
      setShortImpl(array, index, value);
    } else {
      checkArgument(false);
    }
  }

  private static void setByteImpl(Object array, int index, byte value) {
    byte[] typedArray = (byte[]) array;
    typedArray[index] = value;
  }

  public static void setChar(Object array, int index, char value) {
    if (array instanceof char[]) {
      setCharImpl(array, index, value);
    } else if (array instanceof double[]) {
      setDoubleImpl(array, index, value);
    } else if (array instanceof float[]) {
      setFloatImpl(array, index, value);
    } else if (array instanceof int[]) {
      setIntImpl(array, index, value);
    } else if (array instanceof long[]) {
      setLongImpl(array, index, value);
    } else {
      checkArgument(false);
    }
  }

  private static void setCharImpl(Object array, int index, char value) {
    char[] typedArray = (char[]) array;
    typedArray[index] = value;
  }

  public static void setDouble(Object array, int index, double value) {
    checkArgument(array instanceof double[]);
    setDoubleImpl(array, index, value);
  }

  private static void setDoubleImpl(Object array,int index, double value) {
    double[] typedArray = (double[]) array;
    typedArray[index] = value;
  }

  public static void setFloat(Object array, int index, float value) {
    if (array instanceof float[]) {
      setFloatImpl(array, index, value);
    } else if (array instanceof double[]) {
      setDoubleImpl(array, index, value);
    } else {
      checkArgument(false);
    }
  }

  private static void setFloatImpl(Object array, int index, float value) {
    float[] typedArray = (float[]) array;
    typedArray[index] = value;
  }

  public static void setInt(Object array, int index, int value) {
    if (array instanceof int[]) {
      setIntImpl(array, index, value);
    } else if (array instanceof double[]) {
      setDoubleImpl(array, index, value);
    } else if (array instanceof float[]) {
      setFloatImpl(array, index, value);
    } else if (array instanceof long[]) {
      setLongImpl(array, index, value);
    } else {
      checkArgument(false);
    }
  }

  private static void setIntImpl(Object array, int index, int value) {
    int[] typedArray = (int[]) array;
    typedArray[index] = value;
  }

  public static void setLong(Object array, int index, long value) {
    if (array instanceof long[]) {
      setLongImpl(array, index, value);
    } else if (array instanceof double[]) {
      setDoubleImpl(array, index, value);
    } else if (array instanceof float[]) {
      setFloatImpl(array, index, value);
    } else {
      checkArgument(false);
    }
  }

  private static void setLongImpl(Object array, int index, long value) {
    long[] typedArray = (long[]) array;
    typedArray[index] = value;
  }

  public static void setShort(Object array, int index, short value) {
    if (array instanceof short[]) {
      setShortImpl(array, index, value);
    } else if (array instanceof double[]) {
      setDoubleImpl(array, index, value);
    } else if (array instanceof float[]) {
      setFloatImpl(array, index, value);
    } else if (array instanceof int[]) {
      setIntImpl(array, index, value);
    } else if (array instanceof long[]) {
      setLongImpl(array, index, value);
    } else {
      checkArgument(false);
    }
  }

  private static void setShortImpl(Object array, int index, short value) {
    short[] typedArray = (short[]) array;
    typedArray[index] = value;
  }

  // There is a good reason to not implement the generic newInstance methods:
  //
  // In GWT we would need access from a class literal to the castable type map, which
  // could be done, but would require some serious work.
  // But it would also mean that we can not compute all Array types being instantiated anymore and
  // thus would have to retain more castable type information (bloat).
  //
  // In J2CL the problem is slightly different. Most checking is deferred to runtime in J2CL and
  // we retain constructor functions of leaf types. To ensure that we are not just retaining
  // all constructors in a program we make sure that access to these always gets inlined with
  // array creation. By allowing a generic method we would not be able to ensure this anymore
  // and might cause serious bloat in a program.
  // A middle ground for J2CL could be to treat this method special in the compiler and fail the
  // compile if its not called with a compile time constant for the class literal.

  // Not implemented:
  // public static Object newInstance(Class<?> componentType, int... dimensions)
  // public static Object newInstance(Class<?> componentType, int length)

  private Array() {
  }
}
