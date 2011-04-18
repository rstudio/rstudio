/*
 * Copyright 2009 Google Inc.
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

import com.google.web.bindery.event.shared.Event;

/**
 * Root of all GWT widget and dom events sourced by a {@link HandlerManager}.
 * All GWT events are considered dead and should no longer be accessed once the
 * {@link HandlerManager} which originally fired the event finishes with it.
 * That is, don't hold on to event objects outside of your handler methods.
 * <p>
 * There is no need for an application's custom event types to extend GwtEvent.
 * Prefer {@link Event} instead.
 * 
 * @param <H> handler type
 */
public abstract class GwtEvent<H extends EventHandler> extends Event<H> {
  /**
   * Type class used to register events with the {@link HandlerManager}.
   * <p>
   * Type is parameterized by the handler type in order to make the addHandler
   * method type safe.
   * </p>
   * 
   * @param <H> handler type
   */
  public static class Type<H> extends com.google.web.bindery.event.shared.Event.Type<H> {
  }

  private boolean dead;

  /**
   * Constructor.
   */
  protected GwtEvent() {
  }

  @Override
  public abstract GwtEvent.Type<H> getAssociatedType();

  @Override
  public Object getSource() {
    assertLive();
    return super.getSource();
  }

  /**
   * Asserts that the event still should be accessed. All events are considered
   * to be "dead" after their original handler manager finishes firing them. An
   * event can be revived by calling {@link GwtEvent#revive()}.
   */
  protected void assertLive() {
    assert (!dead) : "This event has already finished being processed by its original handler manager, so you can no longer access it";
  }

  /**
   * Should only be called by {@link HandlerManager}. In other words, do not use
   * or call.
   * 
   * @param handler handler
   */
  protected abstract void dispatch(H handler);

  /**
   * Is the event current live?
   * 
   * @return whether the event is live
   */
  protected final boolean isLive() {
    return !dead;
  }

  /**
   * Kill the event. After the event has been killed, users cannot really on its
   * values or functions being available.
   */
  protected void kill() {
    dead = true;
    setSource(null);
  }

  /**
   * Revives the event. Used when recycling event instances.
   */
  protected void revive() {
    dead = false;
    setSource(null);
  }

  void overrideSource(Object source) {
    super.setSource(source);
  }
}
