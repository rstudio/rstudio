/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.i18n.client;

import com.google.gwt.dom.client.Element;
import com.google.gwt.i18n.client.HasDirection.Direction;

/**
 * A set of bidi-related utility methods. 
 */
public class BidiUtils {

  /**
   * The name of the element property which controls element directionality.  
   */
  public static final String DIR_PROPERTY_NAME = "dir";
  
  /**
   * The value for the directionality property which will set the element directionality
   * to right-to-left.  
   */
  private static final String DIR_PROPERTY_VALUE_RTL = "rtl";
  
  /**
   * The value for the directionality property which will set the element directionality
   * to left-to-right.  
   */
  private static final String DIR_PROPERTY_VALUE_LTR = "ltr";
  
  /**
   * Gets the directionality of an element.
   *
   * @param  elem  the element on which to check the directionality property 
   * @return <code>RTL</code> if the directionality is right-to-left,
   *         <code>LTR</code> if the directionality is left-to-right, or
   *         <code>DEFAULT</code> if the directionality is not explicitly set
   */
  public static HasDirection.Direction getDirectionOnElement(Element elem) {
    String dirPropertyValue = elem.getPropertyString(DIR_PROPERTY_NAME);

    if (DIR_PROPERTY_VALUE_RTL.equalsIgnoreCase(dirPropertyValue)) {
      return HasDirection.Direction.RTL;
    } else if (DIR_PROPERTY_VALUE_LTR.equalsIgnoreCase(dirPropertyValue)) {
      return HasDirection.Direction.LTR;
    }

    return HasDirection.Direction.DEFAULT;
  }

  /**
   * Sets the directionality property for an element.
   *
   * @param elem  the element on which to set the property
   * @param direction <code>RTL</code> if the directionality should be set to right-to-left, 
   *                  <code>LTR</code> if the directionality should be set to left-to-right
   *                  <code>DEFAULT</code> if the directionality should be removed from the element   
   */
  public static void setDirectionOnElement(Element elem, Direction direction) {
    switch (direction) {            
      case RTL: {
        elem.setPropertyString(DIR_PROPERTY_NAME, DIR_PROPERTY_VALUE_RTL);
        break;
      }
      
      case LTR: {
        elem.setPropertyString(DIR_PROPERTY_NAME, DIR_PROPERTY_VALUE_LTR);
        break;        
      }
      
      case DEFAULT: {
        if (getDirectionOnElement(elem) != HasDirection.Direction.DEFAULT) {
          // only clear out the the dir property if it has already been set to something
          // explicitly
          elem.setPropertyString(DIR_PROPERTY_NAME, "");
        }
        break;        
      }     
    }    
  }
}
