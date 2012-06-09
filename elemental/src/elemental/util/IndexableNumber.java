package elemental.util;

/**
 * Models any object which acts like a Javascript array of primitive numbers..
 */
public interface IndexableNumber {

  /**
   * Gets the value at a given index.
   *
   * @param index the index to be retrieved
   * @return the value at the given index
   */
  double numberAt(int index);

  /**
   * Gets the length of the array.
   *
   * @return the array length
   */
  int length();
}
