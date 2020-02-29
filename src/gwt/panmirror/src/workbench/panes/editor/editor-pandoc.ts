/*
 * editor-pandoc.ts
 *
 * Copyright (C) 2019-20 by RStudio, PBC
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

import axios from 'axios';

import { PandocEngine } from 'editor/src/api/pandoc';

export default class implements PandocEngine {
  public async markdownToAst(markdown: string, format: string, options: string[]) {
    const result = await axios.post('/pandoc/ast', { markdown, format, options });
    if (result.data.ast) {
      return result.data.ast;
    } else {
      return Promise.reject(new Error(result.data.error));
    }
  }

  public async astToMarkdown(ast: any, format: string, options: string[]) {
    const result = await axios.post('/pandoc/markdown', { ast, format, options });
    if (result.data.markdown) {
      return result.data.markdown;
    } else {
      return Promise.reject(new Error(result.data.error));
    }
  }

  public async listExtensions(format: string) {
    const result = await axios.post('/pandoc/extensions', { format });
    if (result.data.extensions) {
      return result.data.extensions;
    } else {
      return Promise.resolve(new Error(result.data.error));
    }
  }
}
