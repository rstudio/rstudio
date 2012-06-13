package elemental.util;

/**
 * Models any object which acts like a Javascript array of primitive integers.
 */
public interface IndexableInt extends IndexableNumber {

  /**
   * Gets the value at a given index.
   *
   * @param index the index to be retrieved
   * @return the value at the given index
   */
  int intAt(int index);

  /**
   * Gets the length of the array.
   *
   * @return the array length
   */
  int length();
}
