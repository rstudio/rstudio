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
package com.google.gwt.user.client.ui;

import java.util.HashSet;
import java.util.Set;

/**
 * An exception that is thrown when the panel fails to attach or detach its
 * children.
 */
public class AttachDetachException extends RuntimeException {

  /**
   * The singleton command used to attach widgets.
   */
  static final AttachDetachException.Command attachCommand = new AttachDetachException.Command() {
    public void execute(Widget w) {
      w.onAttach();
    }
  };

  /**
   * The singleton command used to detach widgets.
   */
  static final AttachDetachException.Command detachCommand = new AttachDetachException.Command() {
    public void execute(Widget w) {
      w.onDetach();
    }
  };

  /**
   * The command to execute when iterating through child widgets.
   */
  public static interface Command {
    void execute(Widget w);
  }

  /**
   * <p>
   * Iterator through all child widgets, trying to perform the specified
   * {@link Command} for each. All widgets will be visited even if the Command
   * throws an exception. If one or more exceptions occur, they will be combined
   * and thrown as a single {@link AttachDetachException}.
   * </p>
   * <p>
   * Use this method when attaching or detaching a widget with children to
   * ensure that the logical and physical state of all children match the
   * logical and physical state of the parent.
   * </p>
   * 
   * @param hasWidgets children to iterate
   * @param c the {@link Command} to try on all children
   */
  public static void tryCommand(Iterable<Widget> hasWidgets, Command c) {
    Set<Throwable> caught = null;
    for (Widget w : hasWidgets) {
      try {
        c.execute(w);
      } catch (Throwable e) {
        // Catch all exceptions to prevent some children from being attached
        // while others are not.
        if (caught == null) {
          caught = new HashSet<Throwable>();
        }
        caught.add(e);
      }
    }

    // Throw the combined exceptions now that all children are attached.
    if (caught != null) {
      throw new AttachDetachException(caught);
    }
  }

  /**
   * The causes of the exception.
   */
  private Set<Throwable> causes;

  /**
   * Construct a new {@link AttachDetachException}.
   * 
   * @param causes the causes of the exception
   */
  public AttachDetachException(Set<Throwable> causes) {
    super(
        "One or more exceptions caught, see full set in AttachDetachException#getCauses",
        causes.size() == 0 ? null : causes.toArray(new Throwable[0])[0]);
    this.causes = causes;
  }

  /**
   * Get the set of exceptions that cause {@link Panel#doAttachChildren()} or
   * {@link Panel#doDetachChildren()} to fail.
   * 
   * @return the set of causes
   */
  public Set<Throwable> getCauses() {
    return causes;
  }
}
