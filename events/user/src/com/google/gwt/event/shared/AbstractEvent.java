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
package com.google.gwt.event.shared;

/**
 * Root of all gwt events. All gwt events are considered dead and should no
 * longer be accessed once the {@link HandlerManager} which originally fired the
 * event finishes with it.
 * 
 */
public abstract class AbstractEvent {
  /**
   * Type class used to register events with the {@link HandlerManager}.
   * <p>
   * Type is parameterized by the event and handler type in order to make the
   * addHandler method type safe and to avoid type-casts when implementing
   * {@link AbstractEvent.Type#fire(EventHandler, AbstractEvent)}.
   * </p>
   * 
   * @param <EventType> event type
   * @param <HandlerType> handler type
   */
  public abstract static class Type<EventType extends AbstractEvent, HandlerType extends EventHandler> {

    private int index;

    /**
     * Constructor.
     */
    public Type() {
      index = HandlerManager.createKeyIndex();
    }

    // We override hash code to make it as efficient as possible.
    @Override
    public final int hashCode() {
      return index;
    }

    @Override
    public String toString() {
      return "Event type";
    }

    /**
     * Fires the given handler on the supplied event.
     * 
     * @param handler the handler to fire
     * @param event the event
     */
    protected abstract void fire(HandlerType handler, EventType event);
  }

  private boolean dead;

  private Object source;

  /**
   * Constructor.
   */
  protected AbstractEvent() {
  }

  /**
   * Returns the source that last fired this event.
   * 
   * @return object representing the source of this event
   */
  public Object getSource() {
    assertLive();
    return source;
  }

  /**
   * This is a method used primarily for debugging. It gives a string
   * representation of the event details. This does not override the toString
   * method because the compiler cannot always optimize toString out correctly.
   * Event types should override as desired.
   * 
   * @return a string representing the event's specifics.
   */
  public String toDebugString() {
    String name = this.getClass().getName();
    name = name.substring(name.lastIndexOf("."));
    return name + ": source = " + source;
  }

  /**
   * The toString() for abstract event is overridden to avoid accidently
   * including class literals in the the compiled output. Use
   * {@link AbstractEvent} #toDebugString to get more information about the
   * event.
   */
  @Override
  public String toString() {
    return "An event type";
  }

  /**
   * Asserts that the event still should be accessed. All events are considered
   * to be "dead" after their original handler manager finishes firing them. An
   * event can be revived by calling {@link AbstractEvent#revive()}.
   */
  protected void assertLive() {
    assert (!dead) : "This event has already finished being processed by its original handler manager, so you can no longer access it";
  }

  /**
   * Returns the type used to register this event.
   * 
   * @return the type
   */
  protected abstract Type getType();

  /**
   * Is the event current live?
   * 
   * @return whether the event is live
   */
  protected boolean isLive() {
    return !dead;
  }

  /**
   * Revives the event. Used when recycling event instances.
   */
  protected void revive() {
    dead = false;
  }

  /**
   * Called after the event manager has finished processing the event.
   */
  void onRelease() {
    dead = true;
    source = null;
  }

  /**
   * Set the source that triggered this event.
   * 
   * @param source the source of this event, should only be set by a
   *          {@link HandlerManager}
   */
  void setSource(Object source) {
    this.source = source;
  }
}
