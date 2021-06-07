/*
 * r.ts
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

import { execSync } from "child_process";
import { existsSync } from "fs";
import { platform } from "os";

export default function scanForR(): string {

  // if RSTUDIO_WHICH_R is set, then prefer using that
  let rstudioWhichR = process.env.RSTUDIO_WHICH_R || "";
  if (rstudioWhichR.length !== 0) {
    return rstudioWhichR;
  }

  // first look for R on the PATH
  let R = execSync("/usr/bin/which R", { encoding: "utf-8" }).trim();
  if (R.length !== 0) {
    return R;
  }

  // otherwise, look in some hard-coded locations
  let defaultLocations = [
    "/opt/local/bin/R",
    "/usr/local/bin/R",
    "/usr/bin/R",
  ];

  // also check framework directory for macOS
  if (platform() === "darwin") {
    defaultLocations.push("/Library/Frameworks/R.framework/Resources/bin/R");
  }

  for (const location of defaultLocations) {
    if (existsSync(location)) {
      return location;
    }
  }

  // nothing found; return empty string
  return ""

}