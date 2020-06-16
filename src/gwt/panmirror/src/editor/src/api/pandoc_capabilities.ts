/*
 * pandoc_capabilities.ts
 *
 * Copyright (C) 2020 by RStudio, PBC
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

import { PandocServer, parsePandocListOutput, PandocApiVersion } from './pandoc';

export interface PandocCapabilitiesResult {
  version: string;
  api_version: PandocApiVersion;
  output_formats: string;
  highlight_languages: string;
}

export interface PandocCapabilities {
  version: string;
  api_version: PandocApiVersion;
  output_formats: string[];
  highlight_languages: string[];
}

export async function getPandocCapabilities(server: PandocServer) {
  const result = await server.getCapabilities();
  return {
    version: result.version,
    api_version: result.api_version,
    output_formats: parsePandocListOutput(result.output_formats),
    highlight_languages: parsePandocListOutput(result.highlight_languages),
  };
}
