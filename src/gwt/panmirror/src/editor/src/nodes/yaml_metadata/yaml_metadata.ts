import { Node as ProsemirrorNode, Schema } from 'prosemirror-model';
import { EditorState, Transaction } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { setTextSelection } from 'prosemirror-utils';

import { Extension, extensionIfEnabled } from '../../api/extension';
import { PandocOutput, PandocTokenType } from '../../api/pandoc';
import { EditorUI } from '../../api/ui';
import { ProsemirrorCommand, EditorCommandId } from '../../api/command';
import { canInsertNode } from '../../api/node';
import { codeNodeSpec } from '../../api/code';
import { selectionIsBodyTopLevel } from '../../api/selection';
import { uuidv4 } from '../../api/util';
import { yamlMetadataTitlePlugin } from './yaml_metadata-title';

const kYamlMetadataClass = 'B95ACAB9-77D8-471B-B795-8F59AE9C0B0C';

const extension: Extension = {
  nodes: [
    {
      name: 'yaml_metadata',

      spec: {
        ...codeNodeSpec(),
        attrs: {
          navigation_id: { default: null },
        },
        parseDOM: [
          {
            tag: "div[class*='yaml-block']",
            preserveWhitespace: 'full',
          },
        ],
        toDOM(node: ProsemirrorNode) {
          return ['div', { class: 'yaml-block pm-code-block' }, 0];
        },
      },

      code_view: {
        lang: () => 'yaml-frontmatter',
        classes: ['pm-metadata-background-color', 'pm-yaml-metadata-block'],
      },

      pandoc: {
        codeBlockFilter: {
          preprocessor: (markdown: string) => {
            return markdown.replace(
              /^(?:---\s*\n)(.*?)(?:\n---|\n\.\.\.)(?:[ \t]*)$/gms,
              '```{.' + kYamlMetadataClass + '}\n---\n$1\n---\n```',
            );
          },
          class: kYamlMetadataClass,
          nodeType: schema => schema.nodes.yaml_metadata,
          getAttrs: () => ({ navigation_id: uuidv4() }),
        },

        writer: (output: PandocOutput, node: ProsemirrorNode) => {
          output.writeToken(PandocTokenType.Para, () => {
            output.writeRawMarkdown(node.content);
          });
        },
      },
    },
  ],

  commands: (_schema: Schema, ui: EditorUI) => {
    return [new YamlMetadataCommand()];
  },

  plugins: () => [yamlMetadataTitlePlugin()],
};

class YamlMetadataCommand extends ProsemirrorCommand {
  constructor() {
    super(
      EditorCommandId.YamlMetadata,
      [],
      (state: EditorState, dispatch?: (tr: Transaction) => void, view?: EditorView) => {
        const schema = state.schema;

        if (!canInsertNode(state, schema.nodes.yaml_metadata)) {
          return false;
        }

        // only allow inserting at the top level
        if (!selectionIsBodyTopLevel(state.selection)) {
          return false;
        }

        // create yaml metadata text
        if (dispatch) {
          const tr = state.tr;

          const kYamlLeading = '---\n';
          const kYamlTrailing = '\n---';
          const yamlText = schema.text(kYamlLeading + kYamlTrailing);
          const yamlNode = schema.nodes.yaml_metadata.create({}, yamlText);
          const prevSel = tr.selection.from;
          tr.replaceSelectionWith(yamlNode);
          setTextSelection(prevSel + kYamlLeading.length, -1)(tr);
          dispatch(tr);
        }

        return true;
      },
    );
  }
}

export default extensionIfEnabled(extension, 'yaml_metadata_block');
