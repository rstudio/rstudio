package org.testng;


import java.io.Serializable;
import java.util.List;

/**
 * Describes a TestNG annotated method and the instance on which it will be invoked.
 *
 * This interface is not meant to be implemented by users.
 * 
 * <p>
 * Modified by Google
 * <ul>
 * <li>removed java.reflect.Method</li>
 * </ul>
 *
 * @author Cedric Beust, May 3, 2004
 */
public interface ITestNGMethod extends Comparable, Serializable, Cloneable {

  /**
   * @return The real class on which this method was declared
   * (can be different from getMethod().getDeclaringClass() if
   * the test method was defined in a superclass).
   */
  Class getRealClass();

  ITestClass getTestClass();

  /**
   * Sets the test class having this method. This is not necessarily the declaring class.
   *
   * @param cls The test class having this method.
   */
  void setTestClass(ITestClass cls);

  /**
   * Returns the corresponding Java test method.
   * @return the corresponding Java test method.
   */
  Object getMethod();

  /**
   * Returns the method name. This is needed for serialization because
   * methods are not Serializable.
   * @return the method name.
   */
  String getMethodName();

  /**
   * @return All the instances the methods will be invoked upon.
   * This will typically be an array of one object in the absence
   * of an @Factory annotation.
   */
  Object[] getInstances();

  /**
   * Needed for serialization.
   */
  long[] getInstanceHashCodes();

  /**
   * @return The groups this method belongs to, possibly added to the groups
   * declared on the class.
   */
  String[] getGroups();

  /**
   * @return The groups this method depends on, possibly added to the groups
   * declared on the class.
   */
  String[] getGroupsDependedUpon();

  /**
   * If a group was not found.
   */
  String getMissingGroup();
  public void setMissingGroup(String group);

  /**
   * Before and After groups
   */
  public String[] getBeforeGroups();
  public String[] getAfterGroups();

  /**
   * @return The methods  this method depends on, possibly added to the methods
   * declared on the class.
   */
  String[] getMethodsDependedUpon();
  void addMethodDependedUpon(String methodName);

  /**
   * @return true if this method was annotated with @Test
   */
  boolean isTest();

  /**
   * @return true if this method was annotated with @Configuration
   * and beforeTestMethod = true
   */
  boolean isBeforeMethodConfiguration();

  /**
   * @return true if this method was annotated with @Configuration
   * and beforeTestMethod = false
   */
  boolean isAfterMethodConfiguration();

  /**
   * @return true if this method was annotated with @Configuration
   * and beforeClassMethod = true
   */
  boolean isBeforeClassConfiguration();

  /**
   * @return true if this method was annotated with @Configuration
   * and beforeClassMethod = false
   */
  boolean isAfterClassConfiguration();

  /**
   * @return true if this method was annotated with @Configuration
   * and beforeSuite = true
   */
  boolean isBeforeSuiteConfiguration();

  /**
   * @return true if this method was annotated with @Configuration
   * and afterSuite = true
   */
  boolean isAfterSuiteConfiguration();

  /**
   * @return <tt>true</tt> if this method is a @BeforeTest (@Configuration beforeTest=true)
   */
  boolean isBeforeTestConfiguration();

  /**
   * @return <tt>true</tt> if this method is an @AfterTest (@Configuration afterTest=true)
   */
  boolean isAfterTestConfiguration();

  boolean isBeforeGroupsConfiguration();

  boolean isAfterGroupsConfiguration();

  /**
   * @return The timeout in milliseconds.
   */
  long getTimeOut();
  void setTimeOut(long timeOut);

  /**
   * @return the number of times this method needs to be invoked.
   */
  int getInvocationCount();
  void setInvocationCount(int count);

  /**
   * @return the success percentage for this method (between 0 and 100).
   */
  int getSuccessPercentage();

  /**
   * @return The id of the thread this method was run in.
   */
  String getId();

  void setId(String id);

  long getDate();

  void setDate(long date);

  /**
   * Returns if this ITestNGMethod can be invoked from within IClass.
   */
  boolean canRunFromClass(IClass testClass);

  /**
   * @return true if this method is alwaysRun=true
   */
  boolean isAlwaysRun();

  /**
   * @return the number of threads to be used when invoking the method on parallel
   */
  int getThreadPoolSize();

  void setThreadPoolSize(int threadPoolSize);

  public String getDescription();

  public void incrementCurrentInvocationCount();
  public int getCurrentInvocationCount();
  public void setParameterInvocationCount(int n);
  public int getParameterInvocationCount();

  public ITestNGMethod clone();

  public IRetryAnalyzer getRetryAnalyzer();
  public void setRetryAnalyzer(IRetryAnalyzer retryAnalyzer);

  public boolean skipFailedInvocations();
  public void setSkipFailedInvocations(boolean skip);

  /**
   * The time under which all invocationCount methods need to complete by.
   */
  public long getInvocationTimeOut();

  public boolean ignoreMissingDependencies();
  public void setIgnoreMissingDependencies(boolean ignore);

  /**
   * Which invocation numbers of this method should be used (only applicable
   * if it uses a data provider). If this value is an empty list, use all the values
   * returned from the data provider.  These values are read from the XML file in
   * the <include invocationNumbers="..."> tag.
   */
  public List<Integer> getInvocationNumbers();
  public void setInvocationNumbers(List<Integer> numbers);

  /**
   * The list of invocation numbers that failed, which is only applicable for
   * methods that have a data provider.
   */
  public void addFailedInvocationNumber(int number);
  public List<Integer> getFailedInvocationNumbers();

  /**
   * The scheduling priority. Lower priorities get scheduled first.
   */
  public int getPriority();
  public void setPriority(int priority);
}
