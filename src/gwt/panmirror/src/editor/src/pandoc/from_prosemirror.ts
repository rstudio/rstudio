/*
 * from_prosemirror.ts
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

import { Node as ProsemirrorNode, Fragment, MarkType, Mark } from 'prosemirror-model';

import {
  PandocAst,
  PandocToken,
  PandocOutput,
  PandocNodeWriterFn,
  PandocNodeWriter,
  PandocMarkWriter,
  PandocApiVersion,
  PandocTokenType,
  PandocOutputOption,
  PandocExtensions,
} from '../api/pandoc';

import { PandocFormat } from '../api/pandoc_format';
import { PandocAttr } from '../api/pandoc_attr';

export function pandocFromProsemirror(
  doc: ProsemirrorNode,
  apiVersion: PandocApiVersion,
  format: PandocFormat,
  nodeWriters: readonly PandocNodeWriter[],
  markWriters: readonly PandocMarkWriter[],
) {
  const bodyNode = doc.child(0);
  const notesNode = doc.child(1);
  const writer = new PandocWriter(apiVersion, format, nodeWriters, markWriters, notesNode);
  writer.writeNodes(bodyNode);
  return writer.output();
}

class PandocWriter implements PandocOutput {
  private readonly ast: PandocAst;
  private readonly format: PandocFormat;
  private readonly nodeWriters: { [key: string]: PandocNodeWriterFn };
  private readonly markWriters: { [key: string]: PandocMarkWriter };
  private readonly notes: { [key: string]: ProsemirrorNode };
  private readonly containers: any[][];
  private readonly activeMarks: MarkType[];
  private options: { [key: string]: boolean };

  public readonly extensions: PandocExtensions;

  private readonly escapeCharacters: string[] = [];
  private readonly preventEscapeCharacters: string[] = [];

  constructor(
    apiVersion: PandocApiVersion,
    format: PandocFormat,
    nodeWriters: readonly PandocNodeWriter[],
    markWriters: readonly PandocMarkWriter[],
    notes: ProsemirrorNode,
  ) {
    // save format and extensions
    this.format = format;
    this.extensions = format.extensions;
    // compute escape characters based on format
    this.initEscapeCharacters();
    // create maps of node and mark writers
    this.nodeWriters = {};
    nodeWriters.forEach((writer: PandocNodeWriter) => {
      this.nodeWriters[writer.name] = writer.write;
    });
    this.markWriters = {};
    markWriters.forEach((writer: PandocMarkWriter) => {
      this.markWriters[writer.name] = writer;
    });
    // create map of notes
    this.notes = {};
    notes.forEach((note: ProsemirrorNode) => {
      this.notes[note.attrs.ref] = note;
    });

    this.ast = {
      blocks: [],
      'pandoc-api-version': apiVersion,
      meta: {},
    };
    this.containers = [this.ast.blocks];
    this.activeMarks = [];
    this.options = {
      writeSpaces: true,
      citationEscaping: false,
    };
  }

  public output() {
    return {
      ast: this.ast,
    };
  }

  public write(value: any) {
    const container = this.containers[this.containers.length - 1];
    container.push(value);
  }

  public writeToken(type: PandocTokenType, content?: (() => void) | any) {
    const token: PandocToken = {
      t: type,
    };
    if (content) {
      if (typeof content === 'function') {
        token.c = [];
        this.fill(token.c, content);
      } else {
        token.c = content;
      }
    }
    this.write(token);
  }

  public writeMark(type: PandocTokenType, parent: Fragment, expelEnclosingWhitespace = false) {
    if (expelEnclosingWhitespace) {
      // build output spec
      const output = {
        spaceBefore: false,
        nodes: new Array<ProsemirrorNode>(),
        spaceAfter: false,
      };

      // if we see leading or trailing spaces we need to output them as tokens
      // and substitute text nodes
      parent.forEach((node: ProsemirrorNode, offset: number, index: number) => {
        // check for leading/trailing space in first/last nodes
        if (node.isText) {
          let outputText = node.textContent;

          // checking for leading space in first node
          if (index === 0 && node.textContent.match(/^\s+/)) {
            output.spaceBefore = true;
            outputText = outputText.trimLeft();
          }

          // check for trailing space in last node
          if (index === parent.childCount - 1 && node.textContent.match(/\s+$/)) {
            output.spaceAfter = true;
            outputText = outputText.trimRight();
          }

          // if we modified the node's text then create a new node
          if (outputText !== node.textContent) {
            output.nodes.push(node.type.schema.text(outputText, node.marks));
          } else {
            output.nodes.push(node);
          }
        } else {
          output.nodes.push(node);
        }
      });

      // output space tokens before/after mark as necessary
      if (output.spaceBefore) {
        this.writeToken(PandocTokenType.Space);
      }
      this.writeToken(type, () => {
        this.writeInlines(Fragment.from(output.nodes));
      });
      if (output.spaceAfter) {
        this.writeToken(PandocTokenType.Space);
      }

      // normal codepath (not expelling existing whitespace)
    } else {
      this.writeToken(type, () => {
        this.writeInlines(parent);
      });
    }
  }

  public writeArray(content: () => void) {
    const arr: any[] = [];
    this.fill(arr, content);
    this.write(arr);
  }

  public writeAttr(id?: string, classes?: string[], keyvalue?: string[]) {
    this.write([id || '', classes || [], keyvalue || []]);
  }

  public writeText(text: string | null) {
    // determine which characters we shouldn't escape
    const preventEscapeCharacters = this.preventEscapeCharacters;
    if (this.options.citationEscaping) {
      preventEscapeCharacters.push('[', ']', '@');
    }

    if (text) {
      let textRun = '';
      const flushTextRun = () => {
        if (textRun) {
          this.writeToken(PandocTokenType.Str, textRun);
          textRun = '';
        }
      };
      for (let i = 0; i < text.length; i++) {
        const ch = text.charAt(i);
        if (this.options.writeSpaces && ch === ' ') {
          flushTextRun();
          this.writeToken(PandocTokenType.Space);
        } else if (preventEscapeCharacters.includes(ch)) {
          flushTextRun();
          this.writeRawMarkdown(ch);
        } else {
          textRun += ch;
        }
      }
      if (textRun) {
        this.writeToken(PandocTokenType.Str, textRun);
      }
    }
  }

  public writeLink(href: string, title: string, attr: PandocAttr | null, content: VoidFunction) {
    this.writeToken(PandocTokenType.Link, () => {
      // write attr if provided
      if (attr) {
        this.writeAttr(attr.id, attr.classes, attr.keyvalue);
      } else {
        this.writeAttr();
      }
      // write content
      this.writeArray(() => {
        content();
      });

      // write href
      this.write([href || '', title || '']);
    });
  }

  public writeNote(note: ProsemirrorNode) {
    const noteBody = this.notes[note.attrs.ref];
    this.writeToken(PandocTokenType.Note, () => {
      this.writeNodes(noteBody);
    });
  }

  public writeNode(node: ProsemirrorNode) {
    this.nodeWriters[node.type.name](this, node);
  }

  public writeNodes(parent: ProsemirrorNode) {
    parent.forEach(this.writeNode.bind(this));
  }

  public writeInlines(fragment: Fragment) {
    // get the marks from a node that are not already on the stack of active marks
    const nodeMarks = (node: ProsemirrorNode) => {
      // get marks -- order marks by priority (code lowest so that we never include
      // other markup inside code)
      let marks: Mark[] = node.marks.sort((a: Mark, b: Mark) => {
        const aPriority = this.markWriters[a.type.name].priority;
        const bPriority = this.markWriters[b.type.name].priority;
        if (aPriority < bPriority) {
          return -1;
        } else if (bPriority < aPriority) {
          return 1;
        } else {
          return 0;
        }
      });

      // remove active marks
      for (const activeMark of this.activeMarks) {
        marks = activeMark.removeFromSet(marks);
      }

      // return marks
      return marks;
    };

    // helpers to iterate through the nodes (sans any marks already on the stack)
    let currentChild = 0;
    const nextNode = () => {
      const childIndex = currentChild;
      currentChild++;
      return {
        node: fragment.child(childIndex),
        marks: nodeMarks(fragment.child(childIndex)),
      };
    };
    const putBackNode = () => {
      currentChild--;
    };

    // iterate through the nodes
    while (currentChild < fragment.childCount) {
      // get the next node
      let next = nextNode();

      // if there are active marks then collect them up and call the mark handler
      // with all nodes that it contains, otherwise just process it as a plain
      // unmarked node
      if (next.marks.length > 0) {
        // get the mark and start building a list of marked nodes
        const mark = next.marks[0];
        const markedNodes: ProsemirrorNode[] = [next.node];

        // inner iteration to find nodes that have this mark
        while (currentChild < fragment.childCount) {
          next = nextNode();
          if (mark.type.isInSet(next.marks)) {
            markedNodes.push(next.node);
          } else {
            // no mark found, "put back" the node
            putBackNode();
            break;
          }
        }

        // call the mark writer after noting that this mark is active (which
        // will cause subsequent recursive invocations of this function to
        // not re-process this mark)
        this.activeMarks.push(mark.type);
        this.markWriters[mark.type.name].write(this, mark, Fragment.from(markedNodes));
        this.activeMarks.pop();
      } else {
        // ordinary unmarked node, call the node writer
        this.nodeWriters[next.node.type.name](this, next.node);
      }
    }
  }

  public writeRawMarkdown(markdown: Fragment | string, escapeSymbols?: boolean) {
    // collect markdown text if necessary
    let md = '';
    if (markdown instanceof Fragment) {
      markdown.forEach((node: ProsemirrorNode) => (md = md + node.textContent));
      markdown = md;
    } else {
      md = markdown;
    }

    // escape symbols if requested
    if (escapeSymbols) {
      const escaped: string[] = [];
      for (let i = 0; i < md.length; i++) {
        const ch = markdown.charAt(i);
        if (this.escapeCharacters.includes(ch)) {
          escaped.push('\\' + ch);
        } else {
          escaped.push(ch);
        }
      }
      md = escaped.join('');
    }

    this.writeToken(PandocTokenType.RawInline, () => {
      this.write('markdown');
      this.write(md);
    });
  }

  public withOption(option: PandocOutputOption, value: boolean, f: () => void) {
    const previousValue = this.options[option];
    this.options[option] = value;
    f();
    this.options[option] = previousValue;
  }

  private fill(container: any[], content: () => void) {
    this.containers.push(container);
    content();
    this.containers.pop();
  }

  private initEscapeCharacters() {
    // gfm disallows [] escaping so that MediaWiki style page links (e.g. [[MyPage]]) work as expected
    // tex_math_single_backslash does not allow escaping of [] or () (as that conflicts with the math syntax)
    if (this.format.baseName === 'gfm' || this.format.extensions.tex_math_single_backslash) {
      this.preventEscapeCharacters.push('[', ']');
    }
    // tex_math_single_backslash does not allow escaping of [] or () (as that conflicts with the math syntax)
    if (this.format.extensions.tex_math_single_backslash) {
      this.preventEscapeCharacters.push('(', ')');
    }

    // filter standard escape characters w/ preventEscapeCharacters
    const allEscapeCharacters = ['\\', '`', '*', '_', '{', '}', '[', ']', '(', ')', '>', '#', '+', '-', '.', '!'];
    this.escapeCharacters.push(...allEscapeCharacters.filter(ch => !this.preventEscapeCharacters.includes(ch)));
  }
}
