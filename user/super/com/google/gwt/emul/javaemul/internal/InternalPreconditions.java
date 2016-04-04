/*
 * Copyright 2014 Google Inc.
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
package javaemul.internal;

import java.util.NoSuchElementException;

/**
 * A utility class that provides utility functions to do precondition checks inside GWT-SDK.
 */
// Some parts adapted from Guava
public final class InternalPreconditions {
  private static final boolean CHECKED_MODE =
      System.getProperty("jre.checkedMode", "ENABLED").equals("ENABLED");
  private static final boolean TYPE_CHECK =
      System.getProperty("jre.checks.type", "ENABLED").equals("ENABLED");
  private static final boolean API_CHECK =
      System.getProperty("jre.checks.api", "ENABLED").equals("ENABLED");
  private static final boolean BOUND_CHECK =
      System.getProperty("jre.checks.bounds", "ENABLED").equals("ENABLED");

  /**
   * This method reports if the code is compiled with type checks.
   * It must be used in places where code can be replaced with a simpler one
   * when we know that no checks will occur.
   * See {@link System#arraycopy(Object, int, Object, int, int)} for example.
   * Please note that {@link #checkType(boolean)} should be preferred where feasible.
   */
  public static boolean isTypeChecked() {
    return TYPE_CHECK || CHECKED_MODE;
  }

  public static void checkType(boolean expression) {
    if (TYPE_CHECK) {
      checkCriticalType(expression);
    } else if (CHECKED_MODE) {
      try {
        checkCriticalType(expression);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  public static void checkCriticalType(boolean expression) {
    if (!expression) {
      throw new ClassCastException();
    }
  }

  /**
   * Ensures the truth of an expression that verifies array type.
   */
  public static void checkArrayType(boolean expression) {
    if (TYPE_CHECK) {
      checkCriticalArrayType(expression);
    } else if (CHECKED_MODE) {
      try {
        checkCriticalArrayType(expression);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  public static void checkCriticalArrayType(boolean expression) {
    if (!expression) {
      throw new ArrayStoreException();
    }
  }

  /**
   * Ensures the truth of an expression that verifies array type.
   */
  public static void checkArrayType(boolean expression, Object errorMessage) {
    if (TYPE_CHECK) {
      checkCriticalArrayType(expression, errorMessage);
    } else if (CHECKED_MODE) {
      try {
        checkCriticalArrayType(expression, errorMessage);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  public static void checkCriticalArrayType(boolean expression, Object errorMessage) {
    if (!expression) {
      throw new ArrayStoreException(String.valueOf(errorMessage));
    }
  }

  /**
   * Ensures the truth of an expression involving existence of an element.
   */
  public static void checkElement(boolean expression) {
    if (API_CHECK) {
      checkCriticalElement(expression);
    } else if (CHECKED_MODE) {
      try {
        checkCriticalElement(expression);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  /**
   * Ensures the truth of an expression involving existence of an element.
   * <p>
   * For cases where failing fast is pretty important and not failing early could cause bugs that
   * are much harder to debug.
   */
  public static void checkCriticalElement(boolean expression) {
    if (!expression) {
      throw new NoSuchElementException();
    }
  }

  /**
   * Ensures the truth of an expression involving existence of an element.
   */
  public static void checkElement(boolean expression, Object errorMessage) {
    if (API_CHECK) {
      checkCriticalElement(expression, errorMessage);
    } else if (CHECKED_MODE) {
      try {
        checkCriticalElement(expression, errorMessage);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  /**
   * Ensures the truth of an expression involving existence of an element.
   * <p>
   * For cases where failing fast is pretty important and not failing early could cause bugs that
   * are much harder to debug.
   */
  public static void checkCriticalElement(boolean expression, Object errorMessage) {
    if (!expression) {
      throw new NoSuchElementException(String.valueOf(errorMessage));
    }
  }

  /**
   * Ensures the truth of an expression involving one or more parameters to the calling method.
   */
  public static void checkArgument(boolean expression) {
    if (API_CHECK) {
      checkCriticalArgument(expression);
    } else if (CHECKED_MODE) {
      try {
        checkCriticalArgument(expression);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  /**
   * Ensures the truth of an expression involving one or more parameters to the calling method.
   * <p>
   * For cases where failing fast is pretty important and not failing early could cause bugs that
   * are much harder to debug.
   */
  public static void checkCriticalArgument(boolean expression) {
    if (!expression) {
      throw new IllegalArgumentException();
    }
  }

  /**
   * Ensures the truth of an expression involving one or more parameters to the calling method.
   */
  public static void checkArgument(boolean expression, Object errorMessage) {
    if (API_CHECK) {
      checkCriticalArgument(expression, errorMessage);
    } else if (CHECKED_MODE) {
      try {
        checkCriticalArgument(expression, errorMessage);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  /**
   * Ensures the truth of an expression involving one or more parameters to the calling method.
   * <p>
   * For cases where failing fast is pretty important and not failing early could cause bugs that
   * are much harder to debug.
   */
  public static void checkCriticalArgument(boolean expression, Object errorMessage) {
    if (!expression) {
      throw new IllegalArgumentException(String.valueOf(errorMessage));
    }
  }

  /**
   * Ensures the truth of an expression involving one or more parameters to the calling method.
   */
  public static void checkArgument(boolean expression, String errorMessageTemplate,
      Object... errorMessageArgs) {
    if (API_CHECK) {
      checkCriticalArgument(expression, errorMessageTemplate, errorMessageArgs);
    } else if (CHECKED_MODE) {
      try {
        checkCriticalArgument(expression, errorMessageTemplate, errorMessageArgs);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  /**
   * Ensures the truth of an expression involving one or more parameters to the calling method.
   * <p>
   * For cases where failing fast is pretty important and not failing early could cause bugs that
   * are much harder to debug.
   */
  public static void checkCriticalArgument(boolean expression, String errorMessageTemplate,
      Object... errorMessageArgs) {
    if (!expression) {
      throw new IllegalArgumentException(format(errorMessageTemplate, errorMessageArgs));
    }
  }

  /**
   * Ensures the truth of an expression involving the state of the calling instance, but not
   * involving any parameters to the calling method.
   *
   * @param expression a boolean expression
   * @throws IllegalStateException if {@code expression} is false
   */
  public static void checkState(boolean expression) {
    if (API_CHECK) {
      checkCriticalState(expression);
    } else if (CHECKED_MODE) {
      try {
        checkCriticalState(expression);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  /**
   * Ensures the truth of an expression involving the state of the calling instance, but not
   * involving any parameters to the calling method.
   * <p>
   * For cases where failing fast is pretty important and not failing early could cause bugs that
   * are much harder to debug.
   */
  public static void checkCriticalState(boolean expression) {
    if (!expression) {
      throw new IllegalStateException();
    }
  }

  /**
   * Ensures the truth of an expression involving the state of the calling instance, but not
   * involving any parameters to the calling method.
   */
  public static void checkState(boolean expression, Object errorMessage) {
    if (API_CHECK) {
      checkCriticalState(expression, errorMessage);
    } else if (CHECKED_MODE) {
      try {
        checkCriticalState(expression, errorMessage);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  /**
   * Ensures the truth of an expression involving the state of the calling instance, but not
   * involving any parameters to the calling method.
   */
  public static void checkCriticalState(boolean expression, Object errorMessage) {
    if (!expression) {
      throw new IllegalStateException(String.valueOf(errorMessage));
    }
  }

  /**
   * Ensures that an object reference passed as a parameter to the calling method is not null.
   */
  public static <T> T checkNotNull(T reference) {
    if (API_CHECK) {
      checkCriticalNotNull(reference);
    } else if (CHECKED_MODE) {
      try {
        checkCriticalNotNull(reference);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }

    return reference;
  }

  public static <T> T checkCriticalNotNull(T reference) {
    if (reference == null) {
      throw new NullPointerException();
    }
    return reference;
  }

  /**
   * Ensures that an object reference passed as a parameter to the calling method is not null.
   */
  public static void checkNotNull(Object reference, Object errorMessage) {
    if (API_CHECK) {
      checkCriticalNotNull(reference, errorMessage);
    } else if (CHECKED_MODE) {
      try {
        checkCriticalNotNull(reference, errorMessage);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  public static void checkCriticalNotNull(Object reference, Object errorMessage) {
    if (reference == null) {
      throw new NullPointerException(String.valueOf(errorMessage));
    }
  }

  /**
   * Ensures that {@code size} specifies a valid array size (i.e. non-negative).
   */
  public static void checkArraySize(int size) {
    if (API_CHECK) {
      checkCriticalArraySize(size);
    } else if (CHECKED_MODE) {
      try {
        checkCriticalArraySize(size);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  public static void checkCriticalArraySize(int size) {
    if (size < 0) {
      throw new NegativeArraySizeException("Negative array size: " + size);
    }
  }

  /**
   * Ensures that {@code index} specifies a valid <i>element</i> in an array, list or string of size
   * {@code size}. An element index may range from zero, inclusive, to {@code size}, exclusive.
   */
  public static void checkElementIndex(int index, int size) {
    if (BOUND_CHECK) {
      checkCriticalElementIndex(index, size);
    } else if (CHECKED_MODE) {
      try {
        checkCriticalElementIndex(index, size);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  public static void checkCriticalElementIndex(int index, int size) {
    if (index < 0 || index >= size) {
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
    }
  }

  /**
   * Ensures that {@code index} specifies a valid <i>position</i> in an array, list or string of
   * size {@code size}. A position index may range from zero to {@code size}, inclusive.
   */
  public static void checkPositionIndex(int index, int size) {
    if (BOUND_CHECK) {
      checkCriticalPositionIndex(index, size);
    } else if (CHECKED_MODE) {
      try {
        checkCriticalPositionIndex(index, size);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  public static void checkCriticalPositionIndex(int index, int size) {
    if (index < 0 || index > size) {
      throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
    }
  }

  /**
   * Ensures that {@code start} and {@code end} specify a valid <i>positions</i> in an array, list
   * or string of size {@code size}, and are in order. A position index may range from zero to
   * {@code size}, inclusive.
   */
  public static void checkPositionIndexes(int start, int end, int size) {
    if (BOUND_CHECK) {
      checkCriticalPositionIndexes(start, end, size);
    } else if (CHECKED_MODE) {
      try {
        checkCriticalPositionIndexes(start, end, size);
      } catch (Exception e) {
        throw new AssertionError(e);
      }
    }
  }

  /**
   * Ensures that {@code start} and {@code end} specify a valid <i>positions</i> in an array, list
   * or string of size {@code size}, and are in order. A position index may range from zero to
   * {@code size}, inclusive.
   */
  public static void checkCriticalPositionIndexes(int start, int end, int size) {
    if (start < 0) {
      throw new IndexOutOfBoundsException("fromIndex: " + start + " < 0");
    }
    if (end > size) {
      throw new IndexOutOfBoundsException("toIndex: " + end + " > size " + size);
    }
    if (start > end) {
      throw new IllegalArgumentException("fromIndex: " + start + " > toIndex: " + end);
    }
  }

  /**
   * Checks that bounds are correct.
   *
   * @throws StringIndexOutOfBoundsException if the range is not legal
   */
  public static void checkStringBounds(int start, int end, int size) {
    if (start < 0) {
      throw new StringIndexOutOfBoundsException("fromIndex: " + start + " < 0");
    }
    if (end > size) {
      throw new StringIndexOutOfBoundsException("toIndex: " + end + " > size " + size);
    }
    if (end < start) {
      throw new StringIndexOutOfBoundsException("fromIndex: " + start + " > toIndex: " + end);
    }
  }

  /**
   * Substitutes each {@code %s} in {@code template} with an argument. These are matched by
   * position: the first {@code %s} gets {@code args[0]}, etc.  If there are more arguments than
   * placeholders, the unmatched arguments will be appended to the end of the formatted message in
   * square braces.
   */
  private static String format(String template, Object... args) {
    template = String.valueOf(template); // null -> "null"

    // start substituting the arguments into the '%s' placeholders
    StringBuilder builder = new StringBuilder(template.length() + 16 * args.length);
    int templateStart = 0;
    int i = 0;
    while (i < args.length) {
      int placeholderStart = template.indexOf("%s", templateStart);
      if (placeholderStart == -1) {
        break;
      }
      builder.append(template.substring(templateStart, placeholderStart));
      builder.append(args[i++]);
      templateStart = placeholderStart + 2;
    }
    builder.append(template.substring(templateStart));

    // if we run out of placeholders, append the extra args in square braces
    if (i < args.length) {
      builder.append(" [");
      builder.append(args[i++]);
      while (i < args.length) {
        builder.append(", ");
        builder.append(args[i++]);
      }
      builder.append(']');
    }

    return builder.toString();
  }

  // Hides the constructor for this static utility class.
  private InternalPreconditions() { }
}
