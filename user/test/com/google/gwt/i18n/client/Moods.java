//Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.i18n.client;

import java.util.Map;

/** Testing class to represent Moods. */
public interface Moods extends Constants {
  /**
   * The word for 'Happy'.
   * 
   * @gwt.key 123
   * @return 'happy'
   */
  public String getHappy();

  /**
   * Convenience method to get all key/value pairs associated with the mood
   * array.
   * 
   * @gwt.key moods
   * @return returnType of moods
   */
  public Map moodMap();

  /**
   * Gets the keys associated with moods. However note that this will not
   * display well as the values are "Sad", "123".
   * 
   * @gwt.key moods
   * @return array of moods
   */
  public String[] moodArray();
}
