/*
 * Copyright 2007 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.jjs.test;

import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.Widget;

import java.io.Serializable;

/**
 * Test type casts and <code>instanceof</code>.
 */
@SuppressWarnings("unused")
public class ClassCastTest extends GWTTestCase {

  static class Apple extends Food implements CanEatRaw {
  }

  static interface CanEatRaw {
  }

  static abstract class Food {
  }
  
  static class Drink {
  }

  private volatile Object arrayOfInt = new int[3];
  private volatile Object arrayOfWidget = new Widget[4];
  private volatile Object arrayOfApple = new Apple[5];
  private volatile Food foodItem = new Apple();
  private volatile CanEatRaw rawFoodItem = new Apple();
  private volatile Object drinkItem = new Drink();

  @Override
  public String getModuleName() {
    return "com.google.gwt.dev.jjs.CompilerSuite";
  }

  public void testArrayInterfaces() {
    assertTrue(arrayOfInt instanceof Serializable);
    assertTrue(arrayOfWidget instanceof Serializable);

    assertTrue(arrayOfInt instanceof Cloneable);
    assertTrue(arrayOfWidget instanceof Cloneable);

    assertFalse(arrayOfInt instanceof Food);
    assertFalse(arrayOfWidget instanceof Food);
  }
 
  public void testBaseToInterface() {
    CanEatRaw canEatRaw = (CanEatRaw) foodItem;
  }
  
  public void testBaseToInterfaceArrayElement() {
    CanEatRaw[] canEatRawArray = new CanEatRaw[3];
    canEatRawArray[1] = (CanEatRaw) foodItem;
  }
  
  public void testBaseToInterfaceMethod() {
    CanEatRaw canEatRaw = (CanEatRaw) getFoodItem();
  }
 
  @SuppressWarnings("cast")
  public void testBaseToInterfaceToConcreteCrazyInline() {
    Apple apple = (Apple) (CanEatRaw) (Food) new Apple();
  }

  public void testBaseToInterfaceToConcreteField() {
    Apple apple = (Apple) getFoodAsRawFoodField();
  }

  public void testBaseToInterfaceToConcreteInline() {
    Apple apple = (Apple) (CanEatRaw) foodItem;
  }

  public void testBaseToInterfaceToConcreteMethod() {
    Apple apple = (Apple) getFoodAsRawFoodMethod();
  }

  public void testDownCastClass() {
    Apple apple = (Apple) foodItem;
  }

  public void testDownCastToArrayElement() {
    Apple[] appleArray = new Apple[3];
    appleArray[2] = (Apple) foodItem;
  }
  
  public void testDownCastToArrayArrayElement() {
    Apple[][] appleArrayArray = new Apple[3][4];
    appleArrayArray[2][2] = (Apple) foodItem;
  }
  
  public void testArrayToArrayArrayElement() {
    Apple[][] appleArrayArray = new Apple[3][5];
    appleArrayArray[2] = (Apple[]) arrayOfApple;
  }
  
  public void testDownCastClassMethod() {
    Apple apple = (Apple) getFoodItem();
  }

  public void testDownCastInterface() {
    Apple apple = (Apple) rawFoodItem;
  }

  public void testDownCastInterfaceToArrayElement() {
    Apple[] appleArray = new Apple[3];
    appleArray[2] = (Apple) rawFoodItem;
  }
  
  public void testDownCastInterfaceMethod() {
    Apple apple = (Apple) getRawFoodItem();
  }

  public void testInterfaceToBaseToConcreteField() {
    Apple apple = (Apple) getRawFoodAsFoodField();
  }

  public void testInterfaceToBaseToConcreteInline() {
    Apple apple = (Apple) (Food) rawFoodItem;
  }

  public void testInterfaceToBaseToConcreteMethod() {
    Apple apple = (Apple) getRawFoodAsFoodMethod();
  }
  
  public void testClassCastExceptionObjectToConcrete() {
    try {
      Apple apple = (Apple) drinkItem;
      fail("Expected ClassCastException");
    } catch (ClassCastException e) {
      // expected
    }
  }

  public void testClassCastExceptionToBase() {
    try {
      Food food = (Food) drinkItem;
      fail("Expected ClassCastException");
    } catch (ClassCastException e) {
      // expected
    }
  }

  public void testClassCastExceptionToInterface() {
    try {
      CanEatRaw canEatRaw = (CanEatRaw) drinkItem;
      fail("Expected ClassCastException");
    } catch (ClassCastException e) {
      // expected
    }
  }

  public void testClassCastExceptionArrayToConcrete() {
    try {
      Apple apple = (Apple) arrayOfWidget;
      fail("Expected ClassCastException");
    } catch (ClassCastException e) {
      // expected
    }
  }
  
  public void testClassCastExceptionObjectToArray() {
    try {
      Apple[] appleArray = (Apple[]) drinkItem;
      fail("Expected ClassCastException");
    } catch (ClassCastException e) {
      // expected
    }
  }
  
  public void testClassCastExceptionObjectToConcreteArrayElement() {
    try {
      Apple[] appleArray = new Apple[3];
      appleArray[0] = (Apple) drinkItem;
      fail("Expected ClassCastException");
    } catch (ClassCastException e) {
      // expected
    }
  }
  
  public void testArrayStoreExceptionObjectToConcreteArrayElement() {
    try {
      Object[] appleArray = new Apple[3];
      appleArray[0] = drinkItem;
      fail("Expected ArrayStoreException");
    } catch (ArrayStoreException e) {
      // expected
    }
  }
  
  public void testClassCastExceptionArrayToArrayArrayElement() {
    try {
      Apple[][] appleArrayArray = new Apple[3][4];
      appleArrayArray[2] = (Apple[]) arrayOfWidget;
      fail("Expected ClassCastException");
    } catch (ClassCastException e) {
      // expected
    }
  }
  
  public void testArrayStoreExceptionArrayToArrayArrayElement() {
    try {
      Object[][] appleArrayArray = new Apple[3][4];
      appleArrayArray[2] = (Object[]) arrayOfWidget;
      fail("Expected ArrayStoreException");
    } catch (ArrayStoreException e) {
      // expected
    }
  }
  
  private CanEatRaw getFoodAsRawFoodField() {
    return (CanEatRaw) foodItem;
  }

  private CanEatRaw getFoodAsRawFoodMethod() {
    return (CanEatRaw) getFoodItem();
  }

  private Food getFoodItem() {
    return foodItem;
  }

  private Food getRawFoodAsFoodField() {
    return (Food) rawFoodItem;
  }

  private Food getRawFoodAsFoodMethod() {
    return (Food) getRawFoodItem();
  }

  private CanEatRaw getRawFoodItem() {
    return rawFoodItem;
  }

}
