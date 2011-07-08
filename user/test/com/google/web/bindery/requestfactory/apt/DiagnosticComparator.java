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
package com.google.web.bindery.requestfactory.apt;

import java.util.Comparator;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * Orders Diagnostic objects by filename, position, and message.
 */
class DiagnosticComparator implements Comparator<Diagnostic<? extends JavaFileObject>> {
  @Override
  public int compare(Diagnostic<? extends JavaFileObject> o1,
      Diagnostic<? extends JavaFileObject> o2) {
    int c;
    if (o1.getSource() != null && o2.getSource() != null) {
      c = o1.getSource().toUri().toString().compareTo(o2.getSource().toUri().toString());
      if (c != 0) {
        return c;
      }
    }
    long p = o1.getPosition() - o2.getPosition();
    if (p != 0) {
      return Long.signum(p);
    }
    return o1.getMessage(null).compareTo(o2.getMessage(null));
  }
}
