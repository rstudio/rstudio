// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.emultest.java.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CollectionsTest extends EmulTestBase{
  
  public static List createSortedList(){
      ArrayList l = new ArrayList();
      l.add("a");
      l.add("b");
      l.add("c");
      return l;
 
  }
 
  public static List createRandomList(){
    ArrayList l = new ArrayList();
    l.add(new Integer(5));
    l.add(new Integer(2));
    l.add(new Integer(3));
    l.add(new Integer(1));
    l.add(new Integer(4));
    return l;
  }
  
   public void testReverse() {
     List a =createSortedList();
     Collections.reverse(a);
     Object[] x = {"c","b","a"};
     assertEquals(x,a);
     
     List b = createRandomList();
     Collections.reverse(b);
     Collections.reverse(b);
     assertEquals(b, createRandomList());
   }

  public  void testSort() {
    List a = createSortedList();
    Collections.reverse(a);
    Collections.sort(a);
    assertEquals(createSortedList(),a);
  }

  public static void testSortWithComparator() {
    Comparator x = new Comparator(){
      
      public int compare(Object o1, Object o2) {
        Object[] schema = {"b", new Integer(5), "c", new Integer(4)};
        List l = Arrays.asList(schema);
        int first = l.indexOf(o1);
        int second = l.indexOf(o2);
        if(first < second){
          return -1;
        } else if (first == second){
          return 0;
        } else{
          return 1;
        } 
      }
    };
    List a = createSortedList();
    Collections.sort(a,x);
    Object[] expected = {"b", "c","a"};
    assertEquals(expected,a);
    
  }
 
}
