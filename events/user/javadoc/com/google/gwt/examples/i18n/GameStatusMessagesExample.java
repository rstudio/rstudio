package com.google.gwt.examples.i18n;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;

public class GameStatusMessagesExample implements EntryPoint {

  private static native void showMessage(String msg) /*-{
   var el = $doc.createElement("div"); 
   el.innerHTML = msg;
   $doc.body.appendChild(el);
   }-*/;

  public void beginNewGameRound(String username) {
    GameStatusMessages messages = (GameStatusMessages) GWT.create(GameStatusMessages.class);

    // Tell the new player how many turns he or she has left.
    int turnsLeft = computeTurnsLeftForPlayer(username);
    showMessage(messages.turnsLeft(username, turnsLeft));

    // Tell the current player his or her score.
    int currentScore = computeScore(username);
    setCurrentPlayer(username);
    showMessage(messages.currentScore(currentScore));
  }

  public void onModuleLoad() {
    beginNewGameRound("bogus");
  }

  private int computeScore(String username) {
    // bogus
    return 3152;
  }

  private int computeTurnsLeftForPlayer(String username) {
    // bogus
    return 2;
  }

  private void setCurrentPlayer(String username) {
    // bogus
  }
}
