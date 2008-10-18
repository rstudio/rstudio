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

package com.google.gwt.checkstyle;

import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;
import com.puppycrawl.tools.checkstyle.checks.naming.MemberNameCheck;

/**
 * Override MemberNameCheck to correctly use match rather than find.
 */
public class FieldCheck extends MemberNameCheck {
  public FieldCheck() {
    // Specifically stopping fields such as fMainWnd from being allowed.
    setFormat("([a-eg-z]|(f[a-z0-9]))[a-zA-Z0-9]*");
  }

  public void visitToken(DetailAST aAST) {
    if (mustCheckName(aAST)) {
      final DetailAST nameAST = aAST.findFirstToken(TokenTypes.IDENT);
      if (!getRegexp().matcher(nameAST.getText()).matches()) {
        log(
            nameAST.getLineNo(),
            nameAST.getText()
                + ": Field names must start with [a-z], may not start with f[A-Z], and should not contain '_''s.");
      }
    }
  }
}
