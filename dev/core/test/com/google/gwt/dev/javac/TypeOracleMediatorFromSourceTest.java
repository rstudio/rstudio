package com.google.gwt.dev.javac;

import com.google.gwt.core.ext.typeinfo.TypeOracleException;

public class TypeOracleMediatorFromSourceTest extends
    TypeOracleMediatorTestBase {

  @Override
  protected void buildTypeOracle() throws TypeOracleException {
    typeOracle = TypeOracleTestingUtils.buildTypeOracle(createTreeLogger(),
        resources);
    checkTypes(typeOracle.getTypes());
  }
}
