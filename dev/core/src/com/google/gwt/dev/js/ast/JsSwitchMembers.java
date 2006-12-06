/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.dev.js.ast;

/**
 * Each member is a {@link com.google.gwt.compiler.jjs.jsc.JsSwitchMember}.
 */
public class JsSwitchMembers extends JsCollection {

  public void add(JsSwitchMember member) {
    super.addNode(member);
  }

  public void add(int index, JsSwitchMember member) {
    super.addNode(index, member);
  }

  public JsSwitchMember get(int index) {
    return (JsSwitchMember) super.getNode(index);
  }

  public void set(int index, JsSwitchMember member) {
    super.setNode(index, member);
  }
}
