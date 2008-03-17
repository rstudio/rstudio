package com.google.gwt.examples.i18n;

import com.google.gwt.i18n.client.Messages;

public interface GameStatusMessagesAnnot extends Messages {
  /**
   * @param username the name of a player
   * @param numTurns the number of turns remaining
   * @return a message specifying the remaining turns for a player
   */
  @DefaultMessage("Turns left for player ''{0}'': {1}")
  String turnsLeft(String username, int numTurns);

  /**
   * @param numPoints the number of points
   * @return a message describing the current score for the current player
   */
  @DefaultMessage("Current score: {0}")
  String currentScore(int numPoints);
}
