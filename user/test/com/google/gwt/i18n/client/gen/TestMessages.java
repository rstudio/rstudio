package com.google.gwt.i18n.client.gen;


/**
 * Interface to represent the messages contained in resource  bundle:
 * 	C:/src-gwt/gwt-user/src/test/resources/com/google/gwt/i18n/client/gen/TestMessages.properties'.
 */
public interface TestMessages extends com.google.gwt.i18n.client.Messages {
  
  /**
   * Translated "{0},{1}, "a","b", "{0}", "{1}", ''a'', 'b', '{0}', ''{1}''".
   * 
   * @return translated "{0},{1}, "a","b", "{0}", "{1}", ''a'', 'b', '{0}', ''{1}''"
   * @gwt.key argsWithQuotes
   */
  String argsWithQuotes(String arg0,  String arg1);

  /**
   * Translated "{1} is the second arg, {0} is the first".
   * 
   * @return translated "{1} is the second arg, {0} is the first"
   * @gwt.key args2
   */
  String args2(String arg0,  String arg1);

  /**
   * Translated "no args".
   * 
   * @return translated "no args"
   * @gwt.key args0
   */
  String args0();

  /**
   * Translated "{0}".
   * 
   * @return translated "{0}"
   * @gwt.key simpleMessageTest
   */
  String simpleMessageTest(String arg0);

  /**
   * Translated ""~" ~~ "~~~~ """.
   * 
   * @return translated ""~" ~~ "~~~~ """
   * @gwt.key testWithXs
   */
  String testWithXs();

  /**
   * Translated "arg0arg1 arg0,arg1 {0}arg4".
   * 
   * @return translated "arg0arg1 arg0,arg1 {0}arg4"
   * @gwt.key argsTest
   */
  String argsTest(String arg0);

  /**
   * Translated "repeatedArgs: {0}, {1}, {0}, {1}, {0}, {1}, {0}, {1}".
   * 
   * @return translated "repeatedArgs: {0}, {1}, {0}, {1}, {0}, {1}, {0}, {1}"
   * @gwt.key testLotsOfUsageOfArgs
   */
  String testLotsOfUsageOfArgs(String arg0,  String arg1);

  /**
   * Translated "{0},{1},{2},{3},{4},{5},{6},{7},{8},{9}".
   * 
   * @return translated "{0},{1},{2},{3},{4},{5},{6},{7},{8},{9}"
   * @gwt.key args10
   */
  String args10(String arg0,  String arg1,  String arg2,  String arg3,  String arg4,  String arg5,  String arg6,  String arg7,  String arg8,  String arg9);

  /**
   * Translated "お{0}你{1}好".
   * 
   * @return translated "お{0}你{1}好"
   * @gwt.key unicode
   */
  String unicode(String arg0,  String arg1);

  /**
   * Translated "{0} is a arg".
   * 
   * @return translated "{0} is a arg"
   * @gwt.key args1
   */
  String args1(String arg0);
}
