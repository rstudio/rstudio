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
