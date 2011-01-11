// Modified by Google
// Removed xml classes
package org.testng;

import java.io.Serializable;


/**
 * <code>IClass</code> represents a test class and a collection of its instances.
 * 
 * <p>
 * Modified by Google
 * <ul><li>Removed xml classes</li>
 * </ul>
 *
 * @author <a href = "mailto:cedric&#64;beust.com">Cedric Beust</a>
 */
public interface IClass extends Serializable {

  /**
   * @return this test class name.  This is the name of the
   * corresponding Java class.
   */
  String getName();

  /**
   * @return the &lt;test&gt; tag this class was found in.
   */
  Object getXmlTest();

  /**
   * @return the *lt;class&gt; tag this class was found in.
   */
  Object getXmlClass();

  /**
   * If this class implements ITest, returns its test name, otherwise returns null.
   */
  String getTestName();

  /**
   * @return the Java class corresponding to this IClass.
   */
  Class getRealClass();

  Object[] getInstances(boolean create);

  int getInstanceCount();

  long[] getInstanceHashCodes();

  void addInstance(Object instance);
}
