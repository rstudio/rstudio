// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for any reference type.
 */
public abstract class JReferenceType extends JType implements CanBeAbstract,
    CanBeFinal {

  public List/*<JField>*/ fields = new ArrayList/*<JField>*/();
  public List/*<JMethod>*/ methods = new ArrayList/*<JMethod>*/();
  public JClassType extnds;
  public List/*<JInterfaceType>*/ implments = new ArrayList/*<JInterfaceType>*/();

  public JReferenceType(JProgram program, String name) {
    super(program, name, program.getLiteralNull());
  }

  public String getJavahSignatureName() {
    return "L" + name.replaceAll("_", "_1").replace('.', '_') + "_2";
  }

  public String getJsniSignatureName() {
    return "L" + name.replace('.', '/') + ';';
  }

  public JProgram getProgram() {
    return fProgram;
  }

  public String getShortName() {
    int dotpos = name.lastIndexOf('.');
    return name.substring(dotpos + 1);
  }

}
