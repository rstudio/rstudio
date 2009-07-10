package com.google.gwt.junit;

import com.google.gwt.i18n.client.Messages;

import junit.framework.TestCase;

public class FakeMessagesMakerTest extends TestCase {
  interface MyMessages extends Messages {
    @DefaultMessage("Isn''t this the fakiest?")
    @Description("A sample message to be tested.")
    String myMessage();
    
    @DefaultMessage("Isn''t this the fakiest? Pick one: {1} or {2}?")
    @Description("A sample message with parameters.")
    String myArgumentedMessage(@Example("yes") String yes, 
        @Example("no") String no);
  }
  
  public void testSimple() {
    MyMessages messages = FakeMessagesMaker.create(MyMessages.class);
    assertEquals("myMessage", messages.myMessage());
  }
  
  public void testArgs() {
    MyMessages messages = FakeMessagesMaker.create(MyMessages.class);
    assertEquals("myArgumentedMessage[oui, non]", 
        messages.myArgumentedMessage("oui", "non"));
  }
}
