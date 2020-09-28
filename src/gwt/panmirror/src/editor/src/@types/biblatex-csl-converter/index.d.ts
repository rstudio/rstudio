


declare module 'biblatex-csl-converter'
{

  export class BibLatexExporter {
    constructor(bibDb: BibDB, pks?: string[] | boolean, options?: ConfigObject);
    parse(): string;
  };

  export type ConfigObject = {
    traditionalNames?: boolean;
  };

  export class EntryObject {
    bib_type: string;
    entry_key: string;
    fields: Record<string, any>;
    incomplete?: boolean;
    unexpected_fields?: Object;
    unknown_fields?: UnknownFieldsObject;
  };

  export type MarkObject = {
    type: string;
  }

  type OtherNodeObject = {
    type: string;
    marks?: Array<MarkObject>;
    attrs?: Object;
  }

  export type TextNodeObject = {
    type: 'text';
    text: string;
    marks?: Array<MarkObject>;
    attrs?: Object;
  }

  export type NodeObject = OtherNodeObject | TextNodeObject;
  export type NodeArray = Array<NodeObject>;

  export type NameDictObject = {
    literal?: NodeArray;
    family?: NodeArray;
    given?: NodeArray;
    prefix?: NodeArray;
    suffix?: NodeArray;
    useprefix?: boolean;
  }

  export type GroupObject = {
    name: string;
    references: Array<string>;
    groups: Array<GroupObject>;
  }

  export type RangeArray = [NodeArray, NodeArray] | [NodeArray];

  export const BibFieldTypes: {
    [key: string]: BibField
  };

  export const BibTypes: {
    [key: string]: BibType
  }

  export type BibType = {
    order: number,
    biblatex: string,
    csl: string,
    required: string[],
    eitheror: string[],
    optional: string[]
  }

  export type BibField = {
    type: string;
    biblatex: string;
    csl: string | undefined;
    options?: string[] | undefined;
  }

  export type MarkObject = {
    type: string;
  }
}
