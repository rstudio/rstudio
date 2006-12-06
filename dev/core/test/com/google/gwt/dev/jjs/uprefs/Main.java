// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.uprefs;

public class Main {
  
  interface I1 {
    public void a(int noprune);
    public void b(int prune);
    public void c(int prune);
    public void d(int prune);
    public void e(int prune);
    public void f(int prune);
    public void g(int prune);
    public void h(int prune);
    public void i(int noprune);
    public void j(int prune);
    public void k(int noprune);
    public void l(int prune);
    public void m(int prune);
    public void n(int prune);
    public void o(int prune);
    public void p(int prune);
  }

  interface I2 {
    public void a(int prune);
    public void b(int noprune);
    public void c(int prune);
    public void d(int noprune);
    public void e(int prune);
    public void f(int prune);
    public void g(int prune);
    public void h(int prune);
    public void i(int prune);
    public void j(int noprune);
    public void k(int prune);
    public void l(int prune);
    public void m(int prune);
    public void n(int prune);
    public void o(int prune);
    public void p(int prune);
  }

  interface I3 extends I1 {
    public void a(int noprune);
    public void b(int prune);
    public void c(int noprune);
    public void d(int prune);
    public void e(int prune);
    public void f(int prune);
    public void g(int prune);
    public void h(int prune);
  }

  interface I4 extends I2 {
    public void j(int noprune);
    public void k(int prune);
    public void l(int noprune);
    public void m(int prune);
    public void n(int prune);
    public void o(int prune);
    public void p(int prune);
  }
  

  static abstract class A1 {
    public void a(int prune){ }
    public void c(int prune){ }
    public void e(int noprune){ }
    public void g(int prune){ }
    abstract public void i(int prune);
    abstract public void k(int prune);
    abstract public void m(int noprune);
    abstract public void o(int prune);
  }
  
  static abstract class A2 extends A1 implements I3 {
    abstract public void b(int prune);
    abstract public void d(int prune);
    abstract public void f(int noprune);
    abstract public void h(int prune);
    public void j(int prune){ }
    public void l(int prune){ }
    public void n(int noprune){ }
    public void p(int prune){ }
  }
  
  static class A3 extends A2 implements I3 {
    public void a(int noprune){ }
    public void b(int noprune){ }
    public void c(int noprune){ }
    public void d(int noprune){ }
    public void e(int noprune){ }
    public void f(int noprune){ }
    public void g(int noprune){ }
    public void h(int prune){ }
    public void i(int noprune){ }
    public void j(int noprune){ }
    public void k(int noprune){ }
    public void l(int noprune){ }
    public void m(int noprune){ }
    public void n(int noprune){ }
    public void o(int noprune){ }
    public void p(int prune){ }
  }
  
  static class A4 extends A2 implements I4 {
    public void a(int noprune){ }
    public void b(int noprune){ }
    public void c(int noprune){ }
    public void d(int noprune){ }
    public void e(int noprune){ }
    public void f(int noprune){ }
    public void g(int prune){ }
    public void h(int noprune){ }
    public void i(int noprune){ }
    public void j(int noprune){ }
    public void k(int noprune){ }
    public void l(int noprune){ }
    public void m(int noprune){ }
    public void n(int noprune){ }
    public void o(int prune){ }
    public void p(int noprune){ }
  }
  
  
  public void onModuleLoad() {
    A4 a4 = new A4();
    A3 a3 = new A3();
    A2 a2 = a4;
    A1 a1 = a4;
    I1 i1 = a4;
    I2 i2 = a4;
    I3 i3 = a4;
    I4 i4 = a4;
    
    i1.a(0);
    i2.b(0);
    i3.c(0);
    i4.d(0);
    a1.e(0);
    a2.f(0);
    a3.g(0);
    a4.h(0);

    i1.i(0);
    i2.j(0);
    i3.k(0);
    i4.l(0);
    a1.m(0);
    a2.n(0);
    a3.o(0);
    a4.p(0);
}
  
}
