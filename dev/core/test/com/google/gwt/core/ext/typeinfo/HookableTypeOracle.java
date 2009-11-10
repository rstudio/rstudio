package com.google.gwt.core.ext.typeinfo;

/**
 * A minimal TypeOracle mock that exposes the addNewType hook to subclasses.
 */
public class HookableTypeOracle extends TypeOracle {

  // Increases visibility so tests in other packages can hook this.
  @Override
  protected void addNewType(JRealClassType newType) {
    super.addNewType(newType);
  }
}
