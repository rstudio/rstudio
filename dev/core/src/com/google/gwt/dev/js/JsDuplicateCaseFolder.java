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
package com.google.gwt.dev.js;

import com.google.gwt.dev.js.ast.JsBlock;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsSwitch;
import com.google.gwt.dev.js.ast.JsSwitchMember;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Combine case labels with identical bodies. Case bodies that may fall through
 * to the following case label and case bodies following a possible fallthrough
 * are left undisturbed.
 *
 * For example, consider the following input:
 *
 * <pre>
 * switch (x) {
 *   case 0: y = 17; break;
 *   case 1: if (z == 0) { y = 18; break; } else { y = 19 } // fallthrough else
 *   case 2: return 22;
 *   case 3: if (z == 0) { y = 18; break; } else { y = 19 } // fallthrough else
 *   case 4: y = 17; break;
 *   case 5: y = 17; break;
 *   case 6: return 22;
 * }
 * </pre>
 *
 * This will be transformed into:
 *
 * <pre>
 * switch (x) {
 *   case 0: y = 17; break;
 *   case 1: if (z == 0) { y = 18; break; } else { y = 19 }
 *   case 6: case 2: return 22;
 *   case 3: if (z == 0) { y = 18; break; } else { y = 19 }
 *   case 5: case 4: y = 17; break;
 * }
 *
 * <pre>
 *
 * Cases (2, 6) and (4, 5) have been coalesced.  Note that case 0 has not been
 * combined with cases 4 and 5 since case 4 cannot be moved due to the potential
 * fallthrough from case 3, and we currently only coalesce a given cases with a
 * preceding case and so cannot move case 0 downward.
 *
 * Although this pattern is unlikely to occur frequently in hand-written code,
 * it can account for a significant amount of space in generated code.
 */
public class JsDuplicateCaseFolder {

  private class DuplicateCaseFolder extends JsModVisitor {

    public DuplicateCaseFolder() {
    }

    @Override
    public boolean visit(JsSwitch x, JsContext ctx) {
      boolean modified = false;

      // A map from case body source code to the original case label
      // in which they appeared
      Map<String, JsSwitchMember> seen = new HashMap<String, JsSwitchMember>();

      // Original list of members
      List<JsSwitchMember> cases = x.getCases();
      // Coalesced list of members
      List<JsSwitchMember> newCases = new LinkedList<JsSwitchMember>();

      // Keep track of whether the previous case can fall through
      // to the current case
      boolean hasPreviousFallthrough = false;

      // Iterate over members and locate ones with bodies identical to
      // previous members
      for (JsSwitchMember member : cases) {
        List<JsStatement> stmts = member.getStmts();

        // Don't rewrite any cases that might fall through
        if (!unconditionalControlBreak(stmts)) {
          hasPreviousFallthrough = true;
          // copy the case into the output
          newCases.add(member);
          continue;
        }

        String body = toSource(stmts);
        JsSwitchMember previousCase = seen.get(body);
        if (previousCase == null || hasPreviousFallthrough) {
          // Don't coalesce a case that can be reached via fallthrough
          // from the previous case
          newCases.add(member);
          seen.put(body, member);
        } else {
          // Locate the position of the case that this case is to be
          // coalesced with. Note: linear search in output list
          int index = newCases.indexOf(previousCase);

          // Empty the case body and insert the case label into the output
          member.getStmts().clear();
          newCases.add(index, member);
          modified = true;
        }

        hasPreviousFallthrough = false;
      }

      // Rewrite the AST if any cases have changed
      if (modified) {
        didChange = true;
        cases.clear();
        cases.addAll(newCases);
      }

      return true;
    }

    private String toSource(List<JsStatement> stmts) {
      StringBuilder sb = new StringBuilder();
      for (JsStatement stmt : stmts) {
        sb.append(stmt.toSource(true));
        sb.append("\n"); // separate statements
      }
      return sb.toString();
    }

    /**
     * See {@link JsStatement#unconditionalControlBreak()}.
     */
    private boolean unconditionalControlBreak(List<JsStatement> stmts) {
      for (JsStatement stmt : stmts) {
        if (stmt.unconditionalControlBreak()) {
          return true;
        }
      }
      return false;
    }
  }

  // Needed for OptimizerTestBase
  public static boolean exec(JsProgram program) {
    return new JsDuplicateCaseFolder().execImpl(program.getFragmentBlock(0));
  }

  public JsDuplicateCaseFolder() {
  }

  private boolean execImpl(JsBlock fragment) {
    DuplicateCaseFolder dcf = new DuplicateCaseFolder();
    dcf.accept(fragment);
    return dcf.didChange();
  }
}
