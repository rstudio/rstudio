/*
 * r-command-evaluator.ts
 *
 * Copyright (C) 2021 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

import { MainWindow } from './main-window';

export class RCommandEvaluator {
  static window: MainWindow|null;
  
  static setMainWindow(window: MainWindow|null): void {
    RCommandEvaluator.window = window;
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  static evaluate(rCmd: string): void {
    if (RCommandEvaluator.window === null) {
      return;
    }

    //rCmd = core::string_utils::jsLiteralEscape(rCmd);
    //std::string js = "window.desktopHooks.evaluateRCmd(\"" + rCmd + "\")";
    //window_->webPage()->runJavaScript(QString::fromStdString(js));
    console.log('RCommandEvaluator.evaluate NYI');
  }
}
