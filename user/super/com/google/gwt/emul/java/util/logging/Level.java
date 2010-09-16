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
  public static Level ALL = staticImpl.all();
  public static Level CONFIG = staticImpl.config(); 
  public static Level FINE = staticImpl.fine(); 
  public static Level FINER = staticImpl.finer(); 
  public static Level FINEST = staticImpl.finest(); 
  public static Level INFO = staticImpl.info(); 
  public static Level OFF = staticImpl.off(); 
  public static Level SEVERE = staticImpl.severe(); 
  public static Level WARNING = staticImpl.warning();
  
  public static Level parse(String name) {
    return staticImpl.parse(name);
  } 
  
  private LevelImpl impl;

  protected Level(String name, int value) {
    impl = GWT.create(LevelImplNull.class);
    impl.setName(name);
    impl.setValue(value);
  }

  public String getName() {
    return impl.getName();
  }
  
  public int intValue() {
    return impl.intValue();
  }
    
  @Override
  public String toString() {
    return impl.toString();
  }
  
  /* Not Implemented */
  // public boolean equals(Object ox) {} 
  // protected Level(String name, int value, String resourceBundleName) {} 
  // public String getLocalizedName() {}
  // public String getResourceBundleName() {} 
  // public int  hashCode() {}
  
}
