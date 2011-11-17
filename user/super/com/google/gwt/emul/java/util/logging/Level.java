/*
 * Copyright 2010 Google Inc.
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

package java.util.logging;

import com.google.gwt.core.client.GWT;
import com.google.gwt.logging.impl.LevelImpl;
import com.google.gwt.logging.impl.LevelImplNull;

import java.io.Serializable;

/**
 *  An emulation of the java.util.logging.Level class. See 
 *  <a href="http://java.sun.com/j2se/1.4.2/docs/api/java/util/logging/Level.html"> 
 *  The Java API doc for details</a>
 */
public class Level implements Serializable {
  private static LevelImpl staticImpl = GWT.create(LevelImplNull.class); 
  public static Level ALL = GWT.create(LevelAll.class);
  public static Level CONFIG = GWT.create(LevelConfig.class); 
  public static Level FINE = GWT.create(LevelFine.class); 
  public static Level FINER = GWT.create(LevelFiner.class); 
  public static Level FINEST = GWT.create(LevelFinest.class); 
  public static Level INFO = GWT.create(LevelInfo.class); 
  public static Level OFF = GWT.create(LevelOff.class); 
  public static Level SEVERE = GWT.create(LevelSevere.class); 
  public static Level WARNING = GWT.create(LevelWarning.class);
  
  private static class LevelAll extends Level {
    @Override public String getName() { return "ALL"; }
    @Override public int intValue() { return Integer.MIN_VALUE; }
  }

  private static class LevelConfig extends Level {
    @Override public String getName() { return "CONFIG"; }
    @Override public int intValue() { return 700; }
  }
  
  private static class LevelFine extends Level {
    @Override public String getName() { return "FINE"; }
    @Override public int intValue() { return 500; }
  }

  private static class LevelFiner extends Level {
    @Override public String getName() { return "FINER"; }
    @Override public int intValue() { return 400; }
  }

  private static class LevelFinest extends Level {
    @Override public String getName() { return "FINEST"; }
    @Override public int intValue() { return 300; }
  }  
  
  private static class LevelInfo extends Level {
    @Override public String getName() { return "INFO"; }
    @Override public int intValue() { return 800; }
  }
  
  private static class LevelOff extends Level {
    @Override public String getName() { return "OFF"; }
    @Override public int intValue() { return Integer.MAX_VALUE; }
  }

  private static class LevelNull extends Level {
    @Override public String getName() { return null; }
    @Override public int intValue() { return -1; }
  }
  
  private static class LevelSevere extends Level {
    @Override public String getName() { return "SEVERE"; }
    @Override public int intValue() { return 1000; }
  }

  private static class LevelWarning extends Level {
    @Override public String getName() { return "WARNING"; }
    @Override public int intValue() { return 900; }
  }

  public static Level parse(String name) {
    return staticImpl.parse(name);
  } 

  protected Level() { }
  
  public String getName() {
    return "DUMMY";
  }
  
  public int intValue() {
    return -1;
  }
    
  @Override
  public String toString() {
    return getName();
  }
  
  /* Not Implemented */
  // public boolean equals(Object ox) {} 
  // protected Level(String name, int value, String resourceBundleName) {} 
  // public String getLocalizedName() {}
  // public String getResourceBundleName() {} 
  // public int  hashCode() {}
}
