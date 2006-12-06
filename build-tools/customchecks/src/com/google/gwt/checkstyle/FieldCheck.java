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
