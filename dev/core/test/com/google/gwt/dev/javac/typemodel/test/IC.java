package com.google.gwt.dev.javac.typemodel.test;

public interface IC extends IB, IA {

  @Override
  void ib();

  @Override
  void ib(int x, Object y);

  void ic();

  void ic(int x, Object y);

}
