/*
 * Copyright 2011 Google Inc.
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
package com.google.gwt.sample.ui.client;

import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.HasAttachHandlers;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.web.bindery.event.shared.HandlerRegistration;

import java.util.HashSet;

/**
 * OrientationHelper implementation that works by by monitoring window size
 * changes, and so works on both mobile devices and desktop browsers.
 * <p>
 * Expected to be used as an app-wide singleton.
 */
public class WindowBasedOrientationHelper implements OrientationHelper {
  private class CommandSet implements AttachEvent.Handler, HandlerRegistration {
    final Command portraitCommand;
    final Command landscapeCommand;
    final HandlerRegistration attachEventReg;

    boolean active;

    CommandSet(HasAttachHandlers widget, Command portrait, Command landscape) {
      this.portraitCommand = portrait;
      this.landscapeCommand = landscape;

      attachEventReg = widget.addAttachHandler(this);
      active = widget.isAttached();
    }

    @Override
    public void onAttachOrDetach(AttachEvent event) {
      active = event.isAttached();
      fire();
    }

    @Override
    public void removeHandler() {
      commandSets.remove(this);
      attachEventReg.removeHandler();
    }

    void fire() {
      if (active) {
        if (isPortrait) {
          portraitCommand.execute();
        } else {
          landscapeCommand.execute();
        }
      }
    }
  }

  private static boolean calculateIsPortrait() {
    return Window.getClientHeight() > Window.getClientWidth();
  }

  private boolean isPortrait;
  private HandlerRegistration windowResizeReg;
  private HashSet<CommandSet> commandSets = new HashSet<CommandSet>();

  public WindowBasedOrientationHelper() {
    isPortrait = calculateIsPortrait();
    windowResizeReg = Window.addResizeHandler(new ResizeHandler() {
      public void onResize(ResizeEvent event) {
        update();
      }
    });
  }

  @Override
  public boolean isPortrait() {
    assertLive();
    return isPortrait;
  }

  @Override
  public HandlerRegistration setCommands(HasAttachHandlers widget, Command forPortrait,
      Command forLandscape) {
    assertLive();
    CommandSet commandSet = new CommandSet(widget, forPortrait, forLandscape);
    commandSets.add(commandSet);
    commandSet.fire();
    return commandSet;
  }

  public void stop() {
    assertLive();
    windowResizeReg.removeHandler();
    windowResizeReg = null;

    for (CommandSet commandSet : commandSets) {
      commandSet.attachEventReg.removeHandler();
    }
    commandSets = null;
  }

  private void assertLive() {
    assert windowResizeReg != null : "Cannot do this after stop() is called";
  }

  private void update() {
    boolean was = isPortrait;
    isPortrait = calculateIsPortrait();
    if (was != isPortrait) {
      for (CommandSet helper : commandSets) {
        helper.fire();
      }
    }
  }
}
