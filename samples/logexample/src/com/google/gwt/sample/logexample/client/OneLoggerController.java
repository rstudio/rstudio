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

package com.google.gwt.sample.logexample.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Panel;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A section allowing the user to change the level of a logger and log messages
 * to that logger.
 */
public class OneLoggerController {
  interface MyUiBinder extends UiBinder<HTMLPanel, OneLoggerController> { }
  private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);
  @UiField ListBox levelTextBox;
  @UiField SpanElement loggerName;
  @UiField ListBox logTextBox;
  private Logger logger;
  private Panel panel;

  public OneLoggerController(Logger logger, String name) {
    this.logger = logger;
    panel = uiBinder.createAndBindUi(this);
    loggerName.setInnerText(name);
    addLevelButtons();
    addLogButtons();
  }

  public Panel getPanel() {
    return panel;
  }

  @UiHandler("levelTextBox")
  void handleClick(ChangeEvent e) {
    Level level = Level.parse(levelTextBox.getItemText(
        levelTextBox.getSelectedIndex()));
    logger.log(Level.SEVERE,
        "Setting level to: " + level.getName());
    logger.setLevel(level);
  }

  @UiHandler("exceptionButton")
  void handleExceptionClick(ClickEvent e) {
      logger.log(Level.SEVERE, "Fake Null Exception Hit",
          new NullPointerException());
  }
  
  @UiHandler("logButton")
  void handleLogClick(ClickEvent e) {
    Level level = Level.parse(logTextBox.getItemText(
        logTextBox.getSelectedIndex()));
    logger.log(level, "This is a client log message");
  }

  private void addLevelButtons() {
    levelTextBox.addItem("OFF");
    levelTextBox.addItem("SEVERE");
    levelTextBox.addItem("WARNING");
    levelTextBox.addItem("INFO");
    levelTextBox.addItem("CONFIG");
    levelTextBox.addItem("FINE");
    levelTextBox.addItem("FINER");
    levelTextBox.addItem("FINEST");
    levelTextBox.addItem("ALL");
    String currentLevel = this.getLevel(logger);
    for (int i = 0; i < levelTextBox.getItemCount(); i++) {
      if (currentLevel.equalsIgnoreCase(levelTextBox.getItemText(i))) {
        levelTextBox.setSelectedIndex(i);
      }
    }
  }

  private void addLogButtons() {
    logTextBox.addItem("SEVERE");
    logTextBox.addItem("WARNING");
    logTextBox.addItem("INFO");
    logTextBox.addItem("CONFIG");
    logTextBox.addItem("FINE");
    logTextBox.addItem("FINER");
    logTextBox.addItem("FINEST");
    String currentLevel = this.getLevel(logger);
    for (int i = 0; i < logTextBox.getItemCount(); i++) {
      if (currentLevel.equalsIgnoreCase(logTextBox.getItemText(i))) {
        logTextBox.setSelectedIndex(i);
      }
    }
  }

  private String getLevel(Logger logger) {
    if (logger != null) {
      if (logger.getLevel() != null) {
        return logger.getLevel().getName();
      }
      return getLevel(logger.getParent());
    }
    return "";
  }
}
