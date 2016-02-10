/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.dom.client;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.safehtml.shared.annotations.IsSafeUri;

/**
 * Provides programmatic access to properties of the style object.
 * 
 * <p>
 * Note that if a property or value is not explicitly enumerated in this
 * class, you can still access it via {@link #getProperty(String)},
 * and {@link #setProperty(String, String)}.
 * </p>
 * 
 * @see Element#getStyle()
 */
public class Style extends JavaScriptObject {

  /**
   * Interface to be implemented by enumerated CSS values.
   */
  public interface HasCssName {

    /**
     * Gets the CSS name associated with this value.
     */
    String getCssName();
  }

  /**
   * CSS length units.
   */
  public enum Unit {
    PX {
      @Override
      public String getType() {
        return UNIT_PX;
      }
    }, PCT {
      @Override
      public String getType() {
        return UNIT_PCT;
      }
    }, EM {
      @Override
      public String getType() {
        return UNIT_EM;
      }
    }, EX {
      @Override
      public String getType() {
        return UNIT_EX;
      }
    }, PT {
      @Override
      public String getType() {
        return UNIT_PT;
      }
    }, PC {
      @Override
      public String getType() {
        return UNIT_PC;
      }
    }, IN {
      @Override
      public String getType() {
        return UNIT_IN;
      }
    }, CM {
      @Override
      public String getType() {
        return UNIT_CM;
      }
    }, MM {
      @Override
      public String getType() {
        return UNIT_MM;
      }
    };

    public abstract String getType();
  }

  /**
   * Enum for the border-style property.
   */
  public enum BorderStyle implements HasCssName {
    NONE {
      @Override
      public String getCssName() {
        return BORDER_STYLE_NONE;
      }
    },
    DOTTED {
      @Override
      public String getCssName() {
        return BORDER_STYLE_DOTTED;
      }
    },
    DASHED {
      @Override
      public String getCssName() {
        return BORDER_STYLE_DASHED;
      }
    },
    HIDDEN {
      @Override
      public String getCssName() {
        return BORDER_STYLE_HIDDEN;
      }
    },
    SOLID {
      @Override
      public String getCssName() {
        return BORDER_STYLE_SOLID;
      }
    };
    @Override
    public abstract String getCssName();
  }

  /**
   * Enum for the 'clear' CSS property.
   */
  public enum Clear implements HasCssName {
    BOTH {
      @Override
      public String getCssName() {
        return CLEAR_BOTH;
      }
    },
    LEFT {
      @Override
      public String getCssName() {
        return CLEAR_LEFT;
      }
    },
    NONE {
      @Override
      public String getCssName() {
        return CLEAR_NONE;
      }
    },
    RIGHT {
      @Override
      public String getCssName() {
        return CLEAR_RIGHT;
      }
    };
    @Override
    public abstract String getCssName();
  }
  
  /**
   * Enum for the cursor property.
   */
  public enum Cursor implements HasCssName {
    DEFAULT {
      @Override
      public String getCssName() {
        return CURSOR_DEFAULT;
      }
    },
    AUTO {
      @Override
      public String getCssName() {
        return CURSOR_AUTO;
      }
    },
    CROSSHAIR {
      @Override
      public String getCssName() {
        return CURSOR_CROSSHAIR;
      }
    },
    POINTER {
      @Override
      public String getCssName() {
        return CURSOR_POINTER;
      }
    },
    MOVE {
      @Override
      public String getCssName() {
        return CURSOR_MOVE;
      }
    },
    E_RESIZE {
      @Override
      public String getCssName() {
        return CURSOR_E_RESIZE;
      }
    },
    NE_RESIZE {
      @Override
      public String getCssName() {
        return CURSOR_NE_RESIZE;
      }
    },
    NW_RESIZE {
      @Override
      public String getCssName() {
        return CURSOR_NW_RESIZE;
      }
    },
    N_RESIZE {
      @Override
      public String getCssName() {
        return CURSOR_N_RESIZE;
      }
    },
    SE_RESIZE {
      @Override
      public String getCssName() {
        return CURSOR_SE_RESIZE;
      }
    },
    SW_RESIZE {
      @Override
      public String getCssName() {
        return CURSOR_SW_RESIZE;
      }
    },
    S_RESIZE {
      @Override
      public String getCssName() {
        return CURSOR_S_RESIZE;
      }
    },
    W_RESIZE {
      @Override
      public String getCssName() {
        return CURSOR_W_RESIZE;
      }
    },
    TEXT {
      @Override
      public String getCssName() {
        return CURSOR_TEXT;
      }
    },
    WAIT {
      @Override
      public String getCssName() {
        return CURSOR_WAIT;
      }
    },
    HELP {
      @Override
      public String getCssName() {
        return CURSOR_HELP;
      }
    },
    COL_RESIZE {
      @Override
      public String getCssName() {
        return CURSOR_COL_RESIZE;
      }
    },
    ROW_RESIZE {
      @Override
      public String getCssName() {
        return CURSOR_ROW_RESIZE;
      }
    };
    @Override
    public abstract String getCssName();
  }

  /**
   * Enum for the display property.
   */
  public enum Display implements HasCssName {
    NONE {
      @Override
      public String getCssName() {
        return DISPLAY_NONE;
      }
    },
    BLOCK {
      @Override
      public String getCssName() {
        return DISPLAY_BLOCK;
      }
    },
    INLINE {
      @Override
      public String getCssName() {
        return DISPLAY_INLINE;
      }
    },
    INLINE_BLOCK {
      @Override
      public String getCssName() {
        return DISPLAY_INLINE_BLOCK;
      }
    },
    INLINE_TABLE {
      @Override
      public String getCssName() {
        return DISPLAY_INLINE_TABLE;
      }
    },
    LIST_ITEM {
      @Override
      public String getCssName() {
        return DISPLAY_LIST_ITEM;
      }
    },
    RUN_IN {
      @Override
      public String getCssName() {
        return DISPLAY_RUN_IN;
      }
    },
    TABLE {
      @Override
      public String getCssName() {
        return DISPLAY_TABLE;
      }
    },
    TABLE_CAPTION {
      @Override
      public String getCssName() {
        return DISPLAY_TABLE_CAPTION;
      }
    },
    TABLE_COLUMN_GROUP {
      @Override
      public String getCssName() {
        return DISPLAY_TABLE_COLUMN_GROUP;
      }
    },
    TABLE_HEADER_GROUP {
      @Override
      public String getCssName() {
        return DISPLAY_TABLE_HEADER_GROUP;
      }
    },
    TABLE_FOOTER_GROUP {
      @Override
      public String getCssName() {
        return DISPLAY_TABLE_FOOTER_GROUP;
      }
    },
    TABLE_ROW_GROUP {
      @Override
      public String getCssName() {
        return DISPLAY_TABLE_ROW_GROUP;
      }
    },
    TABLE_CELL {
      @Override
      public String getCssName() {
        return DISPLAY_TABLE_CELL;
      }
    },
    TABLE_COLUMN {
      @Override
      public String getCssName() {
        return DISPLAY_TABLE_COLUMN;
      }
    },
    TABLE_ROW {
      @Override
      public String getCssName() {
        return DISPLAY_TABLE_ROW;
      }
    },
    INITIAL {
      @Override
      public String getCssName() {
        return DISPLAY_INITIAL;
      }
    };
    @Override
    public abstract String getCssName();
  }

  /**
   * Enum for the float property.
   */
  public enum Float implements HasCssName {
    LEFT {
      @Override
      public String getCssName() {
        return FLOAT_LEFT;
      }
    },
    RIGHT {
      @Override
      public String getCssName() {
        return FLOAT_RIGHT;
      }
    },
    NONE {
      @Override
      public String getCssName() {
        return FLOAT_NONE;
      }
    };
    @Override
    public abstract String getCssName();
  }

  /**
   * Enum for the font-style property.
   */
  public enum FontStyle implements HasCssName {
    NORMAL {
      @Override
      public String getCssName() {
        return FONT_STYLE_NORMAL;
      }
    },
    ITALIC {
      @Override
      public String getCssName() {
        return FONT_STYLE_ITALIC;
      }
    },
    OBLIQUE {
      @Override
      public String getCssName() {
        return FONT_STYLE_OBLIQUE;
      }
    };
    @Override
    public abstract String getCssName();
  }

  /**
   * Enum for the font-weight property.
   */
  public enum FontWeight implements HasCssName {
    NORMAL {
      @Override
      public String getCssName() {
        return FONT_WEIGHT_NORMAL;
      }
    },
    BOLD {
      @Override
      public String getCssName() {
        return FONT_WEIGHT_BOLD;
      }
    },
    BOLDER {
      @Override
      public String getCssName() {
        return FONT_WEIGHT_BOLDER;
      }
    },
    LIGHTER {
      @Override
      public String getCssName() {
        return FONT_WEIGHT_LIGHTER;
      }
    };
    @Override
    public abstract String getCssName();
  }

  /**
   * Enum for the list-style-type property.
   */
  public enum ListStyleType implements HasCssName {
    NONE {
      @Override
      public String getCssName() {
        return LIST_STYLE_TYPE_NONE;
      }
    },
    DISC {
      @Override
      public String getCssName() {
        return LIST_STYLE_TYPE_DISC;
      }
    },
    CIRCLE {
      @Override
      public String getCssName() {
        return LIST_STYLE_TYPE_CIRCLE;
      }
    },
    SQUARE {
      @Override
      public String getCssName() {
        return LIST_STYLE_TYPE_SQUARE;
      }
    },
    DECIMAL {
      @Override
      public String getCssName() {
        return LIST_STYLE_TYPE_DECIMAL;
      }
    },
    LOWER_ALPHA {
      @Override
      public String getCssName() {
        return LIST_STYLE_TYPE_LOWER_ALPHA;
      }
    },
    UPPER_ALPHA {
      @Override
      public String getCssName() {
        return LIST_STYLE_TYPE_UPPER_ALPHA;
      }
    },
    LOWER_ROMAN {
      @Override
      public String getCssName() {
        return LIST_STYLE_TYPE_LOWER_ROMAN;
      }
    },
    UPPER_ROMAN {
      @Override
      public String getCssName() {
        return LIST_STYLE_TYPE_UPPER_ROMAN;
      }
    };
    @Override
    public abstract String getCssName();
  }

  /**
   * Enum for the outline-style property.
   */
  public enum OutlineStyle implements HasCssName {
    NONE {
      @Override
      public String getCssName() {
        return OUTLINE_STYLE_NONE;
      }
    },
    DASHED {
      @Override
      public String getCssName() {
        return OUTLINE_STYLE_DASHED;
      }
    },
    DOTTED {
      @Override
      public String getCssName() {
        return OUTLINE_STYLE_DOTTED;
      }
    },
    DOUBLE {
      @Override
      public String getCssName() {
        return OUTLINE_STYLE_DOUBLE;
      }
    },
    GROOVE {
      @Override
      public String getCssName() {
        return OUTLINE_STYLE_GROOVE;
      }
    },
    INSET {
      @Override
      public String getCssName() {
        return OUTLINE_STYLE_INSET;
      }
    },
    OUTSET {
      @Override
      public String getCssName() {
        return OUTLINE_STYLE_OUTSET;
      }
    },
    RIDGE {
      @Override
      public String getCssName() {
        return OUTLINE_STYLE_RIDGE;
      }
    },
    SOLID {
      @Override
      public String getCssName() {
        return OUTLINE_STYLE_SOLID;
      }
    };
    @Override
    public abstract String getCssName();
  }

  /**
   * Enum for the overflow property.
   */
  public enum Overflow implements HasCssName {
    VISIBLE {
      @Override
      public String getCssName() {
        return OVERFLOW_VISIBLE;
      }
    },
    HIDDEN {
      @Override
      public String getCssName() {
        return OVERFLOW_HIDDEN;
      }
    },
    SCROLL {
      @Override
      public String getCssName() {
        return OVERFLOW_SCROLL;
      }
    },
    AUTO {
      @Override
      public String getCssName() {
        return OVERFLOW_AUTO;
      }
    };
    @Override
    public abstract String getCssName();
  }

  /**
   * Enum for the position property.
   */
  public enum Position implements HasCssName {
    STATIC {
      @Override
      public String getCssName() {
        return POSITION_STATIC;
      }
    },
    RELATIVE {
      @Override
      public String getCssName() {
        return POSITION_RELATIVE;
      }
    },
    ABSOLUTE {
      @Override
      public String getCssName() {
        return POSITION_ABSOLUTE;
      }
    },
    FIXED {
      @Override
      public String getCssName() {
        return POSITION_FIXED;
      }
    };
    @Override
    public abstract String getCssName();
  }

  /**
   * Enum for the table-layout property.
   */
  public enum TableLayout implements HasCssName {
    AUTO {
      @Override
      public String getCssName() {
        return TABLE_LAYOUT_AUTO;
      }
    },
    FIXED {
      @Override
      public String getCssName() {
        return TABLE_LAYOUT_FIXED;
      }
    };
    @Override
    public abstract String getCssName();
  }

  /**
   * Enum for the text-align property.
   */
  public enum TextAlign implements HasCssName {
    CENTER {
      @Override
      public String getCssName() {
        return TEXT_ALIGN_CENTER;
      }
    },
    JUSTIFY {
      @Override
      public String getCssName() {
        return TEXT_ALIGN_JUSTIFY;
      }
    },
    LEFT {
      @Override
      public String getCssName() {
        return TEXT_ALIGN_LEFT;
      }
    },
    RIGHT {
      @Override
      public String getCssName() {
        return TEXT_ALIGN_RIGHT;
      }
    };
    @Override
    public abstract String getCssName();
  }

  /**
   * Enum for the 'text-decoration' CSS property.
   */
  public enum TextDecoration implements HasCssName {
    BLINK {
      @Override
      public String getCssName() {
        return TEXT_DECORATION_BLINK;
      }
    },
    LINE_THROUGH {
      @Override
      public String getCssName() {
        return TEXT_DECORATION_LINE_THROUGH;
      }
    },
    NONE {
      @Override
      public String getCssName() {
        return TEXT_DECORATION_NONE;
      }
    },
    OVERLINE {
      @Override
      public String getCssName() {
        return TEXT_DECORATION_OVERLINE;
      }
    },
    UNDERLINE {
      @Override
      public String getCssName() {
        return TEXT_DECORATION_UNDERLINE;
      }
    };
    @Override
    public abstract String getCssName();
  }



  /**
   * Enum for the 'text-justify' CSS3 property.
   */
  public enum TextJustify implements HasCssName {
    AUTO {
      @Override
      public String getCssName() {
        return TEXT_JUSTIFY_AUTO;
      }
    },
    DISTRIBUTE {
      @Override
      public String getCssName() {
        return TEXT_JUSTIFY_DISTRIBUTE;
      }
    },
    INTER_CLUSTER {
      @Override
      public String getCssName() {
        return TEXT_JUSTIFY_INTER_CLUSTER;
      }
    },
    INTER_IDEOGRAPH {
      @Override
      public String getCssName() {
        return TEXT_JUSTIFY_INTER_IDEOGRAPH;
      }
    },
    INTER_WORD {
      @Override
      public String getCssName() {
        return TEXT_JUSTIFY_INTER_WORD;
      }
    },
    KASHIDA {
      @Override
      public String getCssName() {
        return TEXT_JUSTIFY_KASHIDA;
      }
    },
    NONE {
      @Override
      public String getCssName() {
        return TEXT_JUSTIFY_NONE;
      }
    };
    @Override
    public abstract String getCssName();
  }
  
  /**
   * Enum for the 'text-overflow' CSS3 property.
   */
  public enum TextOverflow implements HasCssName {
    CLIP {
      @Override
      public String getCssName() {
        return TEXT_OVERFLOW_CLIP;
      }
    },
    ELLIPSIS {
      @Override
      public String getCssName() {
        return TEXT_OVERFLOW_ELLIPSIS;
      }
    };
    @Override
    public abstract String getCssName();
  }

  /**
   * Enum for the 'text-transform' CSS property.
   */
  public enum TextTransform implements HasCssName {
    CAPITALIZE {
      @Override
      public String getCssName() {
        return TEXT_TRANSFORM_CAPITALIZE;
      }
    },
    NONE {
      @Override
      public String getCssName() {
        return TEXT_TRANSFORM_NONE;
      }
    },
    LOWERCASE {
      @Override
      public String getCssName() {
        return TEXT_TRANSFORM_LOWERCASE;
      }
    },
    UPPERCASE {
      @Override
      public String getCssName() {
        return TEXT_TRANSFORM_UPPERCASE;
      }
    };
    @Override
    public abstract String getCssName();
  }

  /**
   * Enum for the vertical-align property.
   */
  public enum VerticalAlign implements HasCssName {
    BASELINE {
      @Override
      public String getCssName() {
        return VERTICAL_ALIGN_BASELINE;
      }
    },
    SUB {
      @Override
      public String getCssName() {
        return VERTICAL_ALIGN_SUB;
      }
    },
    SUPER {
      @Override
      public String getCssName() {
        return VERTICAL_ALIGN_SUPER;
      }
    },
    TOP {
      @Override
      public String getCssName() {
        return VERTICAL_ALIGN_TOP;
      }
    },
    TEXT_TOP {
      @Override
      public String getCssName() {
        return VERTICAL_ALIGN_TEXT_TOP;
      }
    },
    MIDDLE {
      @Override
      public String getCssName() {
        return VERTICAL_ALIGN_MIDDLE;
      }
    },
    BOTTOM {
      @Override
      public String getCssName() {
        return VERTICAL_ALIGN_BOTTOM;
      }
    },
    TEXT_BOTTOM {
      @Override
      public String getCssName() {
        return VERTICAL_ALIGN_TEXT_BOTTOM;
      }
    };
    @Override
    public abstract String getCssName();
  }

  /**
   * Enum for the visibility property.
   */
  public enum Visibility implements HasCssName {
    VISIBLE {
      @Override
      public String getCssName() {
        return VISIBILITY_VISIBLE;
      }
    },
    HIDDEN {
      @Override
      public String getCssName() {
        return VISIBILITY_HIDDEN;
      }
    };
    @Override
    public abstract String getCssName();
  }

  /**
   * Enum for the 'white-space' CSS property.
   */
  public enum WhiteSpace implements HasCssName {
    NORMAL {
      @Override
      public String getCssName() {
        return WHITE_SPACE_NORMAL;
      }
    },
    NOWRAP {
      @Override
      public String getCssName() {
        return WHITE_SPACE_NOWRAP;
      }
    },
    PRE {
      @Override
      public String getCssName() {
        return WHITE_SPACE_PRE;
      }
    },
    PRE_LINE {
      @Override
      public String getCssName() {
        return WHITE_SPACE_PRE_LINE;
      }
    },
    PRE_WRAP {
      @Override
      public String getCssName() {
        return WHITE_SPACE_PRE_WRAP;
      }
    };
    @Override
    public abstract String getCssName();
  }

  private static final String BORDER_STYLE_SOLID = "solid";
  private static final String BORDER_STYLE_DASHED = "dashed";
  private static final String BORDER_STYLE_DOTTED = "dotted";
  private static final String BORDER_STYLE_HIDDEN = "hidden";
  private static final String BORDER_STYLE_NONE = "none";
  
  private static final String CLEAR_BOTH = "both";
  private static final String CLEAR_LEFT = "left";
  private static final String CLEAR_NONE = "none";
  private static final String CLEAR_RIGHT = "right";

  private static final String CURSOR_ROW_RESIZE = "row-resize";
  private static final String CURSOR_COL_RESIZE = "col-resize";
  private static final String CURSOR_HELP = "help";
  private static final String CURSOR_WAIT = "wait";
  private static final String CURSOR_TEXT = "text";
  private static final String CURSOR_W_RESIZE = "w-resize";
  private static final String CURSOR_S_RESIZE = "s-resize";
  private static final String CURSOR_SW_RESIZE = "sw-resize";
  private static final String CURSOR_SE_RESIZE = "se-resize";
  private static final String CURSOR_N_RESIZE = "n-resize";
  private static final String CURSOR_NW_RESIZE = "nw-resize";
  private static final String CURSOR_NE_RESIZE = "ne-resize";
  private static final String CURSOR_E_RESIZE = "e-resize";
  private static final String CURSOR_MOVE = "move";
  private static final String CURSOR_POINTER = "pointer";
  private static final String CURSOR_CROSSHAIR = "crosshair";
  private static final String CURSOR_AUTO = "auto";
  private static final String CURSOR_DEFAULT = "default";

  private static final String DISPLAY_INLINE_BLOCK = "inline-block";
  private static final String DISPLAY_INLINE = "inline";
  private static final String DISPLAY_BLOCK = "block";
  private static final String DISPLAY_NONE = "none";
  private static final String DISPLAY_INLINE_TABLE = "inline-table";
  private static final String DISPLAY_LIST_ITEM = "list-item";
  private static final String DISPLAY_RUN_IN = "run-in";
  private static final String DISPLAY_TABLE = "table";
  private static final String DISPLAY_TABLE_CAPTION = "table-caption";
  private static final String DISPLAY_TABLE_COLUMN_GROUP = "table-column-group";
  private static final String DISPLAY_TABLE_HEADER_GROUP = "table-header-group";
  private static final String DISPLAY_TABLE_FOOTER_GROUP = "table-footer-group";
  private static final String DISPLAY_TABLE_ROW_GROUP = "table-row-group";
  private static final String DISPLAY_TABLE_CELL = "table-cell";
  private static final String DISPLAY_TABLE_COLUMN = "table-column";
  private static final String DISPLAY_TABLE_ROW = "table-row";
  private static final String DISPLAY_INITIAL = "initial";

  private static final String FLOAT_LEFT = "left";
  private static final String FLOAT_RIGHT = "right";
  private static final String FLOAT_NONE = "none";

  private static final String FONT_STYLE_OBLIQUE = "oblique";
  private static final String FONT_STYLE_ITALIC = "italic";
  private static final String FONT_STYLE_NORMAL = "normal";

  private static final String FONT_WEIGHT_LIGHTER = "lighter";
  private static final String FONT_WEIGHT_BOLDER = "bolder";
  private static final String FONT_WEIGHT_BOLD = "bold";
  private static final String FONT_WEIGHT_NORMAL = "normal";

  private static final String LIST_STYLE_TYPE_UPPER_ROMAN = "upper-roman";
  private static final String LIST_STYLE_TYPE_LOWER_ROMAN = "lower-roman";
  private static final String LIST_STYLE_TYPE_UPPER_ALPHA = "upper-alpha";
  private static final String LIST_STYLE_TYPE_LOWER_ALPHA = "lower-alpha";
  private static final String LIST_STYLE_TYPE_DECIMAL = "decimal";
  private static final String LIST_STYLE_TYPE_SQUARE = "square";
  private static final String LIST_STYLE_TYPE_CIRCLE = "circle";
  private static final String LIST_STYLE_TYPE_DISC = "disc";
  private static final String LIST_STYLE_TYPE_NONE = "none";

  private static final String OUTLINE_STYLE_DASHED = "dashed";
  private static final String OUTLINE_STYLE_DOTTED = "dotted";
  private static final String OUTLINE_STYLE_DOUBLE = "double";
  private static final String OUTLINE_STYLE_GROOVE = "groove";
  private static final String OUTLINE_STYLE_INSET = "inset";
  private static final String OUTLINE_STYLE_NONE = "none";
  private static final String OUTLINE_STYLE_OUTSET = "outset";
  private static final String OUTLINE_STYLE_RIDGE = "ridge";
  private static final String OUTLINE_STYLE_SOLID = "solid";

  private static final String OVERFLOW_AUTO = "auto";
  private static final String OVERFLOW_SCROLL = "scroll";
  private static final String OVERFLOW_HIDDEN = "hidden";
  private static final String OVERFLOW_VISIBLE = "visible";

  private static final String POSITION_FIXED = "fixed";
  private static final String POSITION_ABSOLUTE = "absolute";
  private static final String POSITION_RELATIVE = "relative";
  private static final String POSITION_STATIC = "static";

  private static final String STYLE_Z_INDEX = "zIndex";
  private static final String STYLE_WIDTH = "width";
  private static final String STYLE_VISIBILITY = "visibility";
  private static final String STYLE_TOP = "top";
  private static final String STYLE_TEXT_DECORATION = "textDecoration";
  private static final String STYLE_RIGHT = "right";
  private static final String STYLE_POSITION = "position";
  private static final String STYLE_PADDING_TOP = "paddingTop";
  private static final String STYLE_PADDING_RIGHT = "paddingRight";
  private static final String STYLE_PADDING_LEFT = "paddingLeft";
  private static final String STYLE_PADDING_BOTTOM = "paddingBottom";
  private static final String STYLE_PADDING = "padding";
  private static final String STYLE_OVERFLOW = "overflow";
  private static final String STYLE_OVERFLOW_X = "overflowX";
  private static final String STYLE_OVERFLOW_Y = "overflowY";
  private static final String STYLE_OPACITY = "opacity";
  private static final String STYLE_MARGIN_TOP = "marginTop";
  private static final String STYLE_MARGIN_RIGHT = "marginRight";
  private static final String STYLE_MARGIN_LEFT = "marginLeft";
  private static final String STYLE_MARGIN_BOTTOM = "marginBottom";
  private static final String STYLE_MARGIN = "margin";
  private static final String STYLE_LIST_STYLE_TYPE = "listStyleType";
  private static final String STYLE_LEFT = "left";
  private static final String STYLE_HEIGHT = "height";
  private static final String STYLE_FONT_WEIGHT = "fontWeight";
  private static final String STYLE_FONT_STYLE = "fontStyle";
  private static final String STYLE_FONT_SIZE = "fontSize";
  private static final String STYLE_DISPLAY = "display";
  private static final String STYLE_CURSOR = "cursor";
  private static final String STYLE_COLOR = "color";
  private static final String STYLE_CLEAR = "clear";
  private static final String STYLE_BOTTOM = "bottom";
  private static final String STYLE_BORDER_WIDTH = "borderWidth";
  private static final String STYLE_BORDER_STYLE = "borderStyle";
  private static final String STYLE_BORDER_COLOR = "borderColor";
  private static final String STYLE_BACKGROUND_IMAGE = "backgroundImage";
  private static final String STYLE_BACKGROUND_COLOR = "backgroundColor";
  private static final String STYLE_VERTICAL_ALIGN = "verticalAlign";
  private static final String STYLE_TABLE_LAYOUT = "tableLayout";
  private static final String STYLE_TEXT_ALIGN = "textAlign";
  private static final String STYLE_TEXT_INDENT = "textIndent";
  private static final String STYLE_TEXT_JUSTIFY = "textJustify";
  private static final String STYLE_TEXT_OVERFLOW = "textOverflow";
  private static final String STYLE_TEXT_TRANSFORM = "textTransform";
  private static final String STYLE_OUTLINE_WIDTH = "outlineWidth";
  private static final String STYLE_OUTLINE_STYLE = "outlineStyle";
  private static final String STYLE_OUTLINE_COLOR = "outlineColor";
  private static final String STYLE_LINE_HEIGHT = "lineHeight";
  private static final String STYLE_WHITE_SPACE = "whiteSpace";

  private static final String TABLE_LAYOUT_AUTO = "auto";
  private static final String TABLE_LAYOUT_FIXED = "fixed";

  private static final String TEXT_ALIGN_CENTER = "center";
  private static final String TEXT_ALIGN_JUSTIFY = "justify";
  private static final String TEXT_ALIGN_LEFT = "left";
  private static final String TEXT_ALIGN_RIGHT = "right";

  private static final String TEXT_DECORATION_BLINK = "blink";
  private static final String TEXT_DECORATION_LINE_THROUGH = "line-through";
  private static final String TEXT_DECORATION_NONE = "none";
  private static final String TEXT_DECORATION_OVERLINE = "overline";
  private static final String TEXT_DECORATION_UNDERLINE = "underline";
  
  private static final String TEXT_JUSTIFY_AUTO = "auto";
  private static final String TEXT_JUSTIFY_DISTRIBUTE = "distribute";
  private static final String TEXT_JUSTIFY_INTER_CLUSTER = "inter-cluster";
  private static final String TEXT_JUSTIFY_INTER_IDEOGRAPH = "inter-ideograph";
  private static final String TEXT_JUSTIFY_INTER_WORD = "inter-word";
  private static final String TEXT_JUSTIFY_KASHIDA = "kashida";
  private static final String TEXT_JUSTIFY_NONE = "none";
  
  private static final String TEXT_OVERFLOW_CLIP = "clip";
  private static final String TEXT_OVERFLOW_ELLIPSIS = "ellipsis";

  private static final String TEXT_TRANSFORM_CAPITALIZE = "capitalize";
  private static final String TEXT_TRANSFORM_NONE = "none";
  private static final String TEXT_TRANSFORM_LOWERCASE = "lowercase";
  private static final String TEXT_TRANSFORM_UPPERCASE = "uppercase";

  private static final String UNIT_MM = "mm";
  private static final String UNIT_CM = "cm";
  private static final String UNIT_IN = "in";
  private static final String UNIT_PC = "pc";
  private static final String UNIT_PT = "pt";
  private static final String UNIT_EX = "ex";
  private static final String UNIT_EM = "em";
  private static final String UNIT_PCT = "%";
  private static final String UNIT_PX = "px";

  private static final String VERTICAL_ALIGN_BASELINE = "baseline";
  private static final String VERTICAL_ALIGN_SUB = "sub";
  private static final String VERTICAL_ALIGN_SUPER = "super";
  private static final String VERTICAL_ALIGN_TOP = "top";
  private static final String VERTICAL_ALIGN_TEXT_TOP = "text-top";
  private static final String VERTICAL_ALIGN_MIDDLE = "middle";
  private static final String VERTICAL_ALIGN_BOTTOM = "bottom";
  private static final String VERTICAL_ALIGN_TEXT_BOTTOM = "text-bottom";

  private static final String VISIBILITY_HIDDEN = "hidden";
  private static final String VISIBILITY_VISIBLE = "visible";
  
  private static final String WHITE_SPACE_NORMAL = "normal";
  private static final String WHITE_SPACE_NOWRAP = "nowrap";
  private static final String WHITE_SPACE_PRE = "pre";
  private static final String WHITE_SPACE_PRE_LINE = "pre-line";
  private static final String WHITE_SPACE_PRE_WRAP = "pre-wrap"; 

  protected Style() {
  }

  /**
   * Clear the background-color css property.
   */
  public final void clearBackgroundColor() {
     clearProperty(STYLE_BACKGROUND_COLOR);
  }

  /**
   * Clear the background-image css property.
   */
  public final void clearBackgroundImage() {
     clearProperty(STYLE_BACKGROUND_IMAGE);
  }

  /**
   * Clear the border-color css property.
   */
  public final void clearBorderColor() {
     clearProperty(STYLE_BORDER_COLOR);
  }

  /**
   * Clears the border-style CSS property.
   */
  public final void clearBorderStyle() {
    clearProperty(STYLE_BORDER_STYLE);
  }

  /**
   * Clear the border-width css property.
   */
  public final void clearBorderWidth() {
     clearProperty(STYLE_BORDER_WIDTH);
  }

  /**
   * Clear the bottom css property.
   */
  public final void clearBottom() {
     clearProperty(STYLE_BOTTOM);
  }

  /**
   * Clear the 'clear' CSS property.
   */
  public final void clearClear() {
     clearProperty(STYLE_CLEAR);
  }

  /**
   * Clear the color css property.
   */
  public final void clearColor() {
     clearProperty(STYLE_COLOR);
  }

  /**
   * Clears the cursor CSS property.
   */
  public final void clearCursor() {
    clearProperty(STYLE_CURSOR);
  }

  /**
   * Clears the display CSS property.
   */
  public final void clearDisplay() {
    clearProperty(STYLE_DISPLAY);
  }

  /**
   * Clear the float css property.
   */
  public final void clearFloat() {
    clearProperty(DOMImpl.impl.cssFloatPropertyName());
  }

  /**
   * Clear the font-size css property.
   */
  public final void clearFontSize() {
    clearProperty(STYLE_FONT_SIZE);
  }

  /**
   * Clears the font-style CSS property.
   */
  public final void clearFontStyle() {
    clearProperty(STYLE_FONT_STYLE);
  }

  /**
   * Clears the font-weight CSS property.
   */
  public final void clearFontWeight() {
    clearProperty(STYLE_FONT_WEIGHT);
  }

  /**
   * Clear the height css property.
   */
  public final void clearHeight() {
     clearProperty(STYLE_HEIGHT);
  }

  /**
   * Clear the left css property.
   */
  public final void clearLeft() {
     clearProperty(STYLE_LEFT);
  }

  /**
   * Clear the line-height css property.
   */
  public final void clearLineHeight() {
     clearProperty(STYLE_LINE_HEIGHT);
  }

  /**
   * Clears the list-style-type CSS property.
   */
  public final void clearListStyleType() {
    clearProperty(STYLE_LIST_STYLE_TYPE);
  }

  /**
   * Clear the margin css property.
   */
  public final void clearMargin() {
     clearProperty(STYLE_MARGIN);
  }

  /**
   * Clear the margin-bottom css property.
   */
  public final void clearMarginBottom() {
     clearProperty(STYLE_MARGIN_BOTTOM);
  }

  /**
   * Clear the margin-left css property.
   */
  public final void clearMarginLeft() {
     clearProperty(STYLE_MARGIN_LEFT);
  }

  /**
   * Clear the margin-right css property.
   */
  public final void clearMarginRight() {
     clearProperty(STYLE_MARGIN_RIGHT);
  }

  /**
   * Clear the margin-top css property.
   */
  public final void clearMarginTop() {
     clearProperty(STYLE_MARGIN_TOP);
  }

  /**
   * Clear the opacity css property.
   */
  public final void clearOpacity() {
    DOMImpl.impl.cssClearOpacity(this);
  }

  /**
   * Clear the outline-color css property.
   */
  public final void clearOutlineColor() {
     clearProperty(STYLE_OUTLINE_COLOR);
  }

  /**
   * Clears the outline-style CSS property.
   */
  public final void clearOutlineStyle() {
    clearProperty(STYLE_OUTLINE_STYLE);
  }

  /**
   * Clear the outline-width css property.
   */
  public final void clearOutlineWidth() {
     clearProperty(STYLE_OUTLINE_WIDTH);
  }

  /**
   * Clears the overflow CSS property.
   */
  public final void clearOverflow() {
    clearProperty(STYLE_OVERFLOW);
  }

  /**
   * Clears the overflow-x CSS property.
   */
  public final void clearOverflowX() {
    clearProperty(STYLE_OVERFLOW_X);
  }
  
  /**
   * Clears the overflow-y CSS property.
   */
  public final void clearOverflowY() {
    clearProperty(STYLE_OVERFLOW_Y);
  }
  
  /**
   * Clear the padding css property.
   */
  public final void clearPadding() {
     clearProperty(STYLE_PADDING);
  }

  /**
   * Clear the padding-bottom css property.
   */
  public final void clearPaddingBottom() {
     clearProperty(STYLE_PADDING_BOTTOM);
  }

  /**
   * Clear the padding-left css property.
   */
  public final void clearPaddingLeft() {
     clearProperty(STYLE_PADDING_LEFT);
  }

  /**
   * Clear the padding-right css property.
   */
  public final void clearPaddingRight() {
     clearProperty(STYLE_PADDING_RIGHT);
  }

  /**
   * Clear the padding-top css property.
   */
  public final void clearPaddingTop() {
     clearProperty(STYLE_PADDING_TOP);
  }

  /**
   * Clears the position CSS property.
   */
  public final void clearPosition() {
    clearProperty(STYLE_POSITION);
  }

  /**
   * Clears the value of a named property, causing it to revert to its default.
   */
  public final void clearProperty(String name) {
    setProperty(name, "");
  }

  /**
   * Clear the right css property.
   */
  public final void clearRight() {
     clearProperty(STYLE_RIGHT);
  }

  /**
   * Clear the table-layout css property.
   */
  public final void clearTableLayout() {
    clearProperty(STYLE_TABLE_LAYOUT);
  }

  /**
   * Clear the 'text-align' CSS property.
   */
  public final void clearTextAlign() {
    clearProperty(STYLE_TEXT_ALIGN);
  }

  /**
   * Clears the text-decoration CSS property.
   */
  public final void clearTextDecoration() {
    clearProperty(STYLE_TEXT_DECORATION);
  }
  
  /**
   * Clear the 'text-indent' CSS property.
   */
  public final void clearTextIndent() {
    clearProperty(STYLE_TEXT_INDENT);
  }
  
  /**
   * Clear the 'text-justify' CSS3 property.
   */
  public final void clearTextJustify() {
    clearProperty(STYLE_TEXT_JUSTIFY);
  }
  
  /**
   * Clear the 'text-overflow' CSS3 property.
   */
  public final void clearTextOverflow() {
    clearProperty(STYLE_TEXT_OVERFLOW);
  }
  
  /**
   * Clear the 'text-transform' CSS property.
   */
  public final void clearTextTransform() {
    clearProperty(STYLE_TEXT_TRANSFORM);
  }

  /**
   * Clear the top css property.
   */
  public final void clearTop() {
     clearProperty(STYLE_TOP);
  }

  /**
   * Clears the visibility CSS property.
   */
  public final void clearVisibility() {
    clearProperty(STYLE_VISIBILITY);
  }

  /**
   * Clear the 'white-space' CSS property.
   */
  public final void clearWhiteSpace() {
    clearProperty(STYLE_WHITE_SPACE);
  }

  /**
   * Clear the width css property.
   */
  public final void clearWidth() {
     clearProperty(STYLE_WIDTH);
  }

  /**
   * Clear the z-index css property.
   */
  public final void clearZIndex() {
     clearProperty(STYLE_Z_INDEX);
  }

  /**
   * Get the background-color css property.
   */
  public final String getBackgroundColor() {
    return getProperty(STYLE_BACKGROUND_COLOR);
  }

  /**
   * Get the background-image css property.
   */
  public final String getBackgroundImage() {
    return getProperty(STYLE_BACKGROUND_IMAGE);
  }

  /**
   * Get the border-color css property.
   */
  public final String getBorderColor() {
    return getProperty(STYLE_BORDER_COLOR);
  }

  /**
   * Gets the border-style CSS property.
   */
  public final String getBorderStyle() {
    return getProperty(STYLE_BORDER_STYLE);
  }

  /**
   * Get the border-width css property.
   */
  public final String getBorderWidth() {
    return getProperty(STYLE_BORDER_WIDTH);
  }

  /**
   * Get the bottom css property.
   */
  public final String getBottom() {
    return getProperty(STYLE_BOTTOM);
  }

  /**
   * Get the 'clear' CSS property.
   */
  public final String getClear() {
    return getProperty(STYLE_CLEAR);
  }

  /**
   * Get the color css property.
   */
  public final String getColor() {
    return getProperty(STYLE_COLOR);
  }

  /**
   * Gets the cursor CSS property.
   */
  public final String getCursor() {
    return getProperty(STYLE_CURSOR);
  }

  /**
   * Gets the display CSS property.
   */
  public final String getDisplay() {
    return getProperty(STYLE_DISPLAY);
  }

  /**
   * Get the font-size css property.
   */
  public final String getFontSize() {
    return getProperty(STYLE_FONT_SIZE);
  }

  /**
   * Gets the font-style CSS property.
   */
  public final String getFontStyle() {
    return getProperty(STYLE_FONT_STYLE);
  }

  /**
   * Gets the font-weight CSS property.
   */
  public final String getFontWeight() {
    return getProperty(STYLE_FONT_WEIGHT);
  }

  /**
   * Get the height css property.
   */
  public final String getHeight() {
    return getProperty(STYLE_HEIGHT);
  }

  /**
   * Get the left css property.
   */
  public final String getLeft() {
    return getProperty(STYLE_LEFT);
  }

  /**
   * Get the line-height css property.
   */
  public final String getLineHeight() {
    return getProperty(STYLE_LINE_HEIGHT);
  }

  /**
   * Gets the list-style-type CSS property.
   */
  public final String getListStyleType() {
    return getProperty(STYLE_LIST_STYLE_TYPE);
  }

  /**
   * Get the margin css property.
   */
  public final String getMargin() {
    return getProperty(STYLE_MARGIN);
  }

  /**
   * Get the margin-bottom css property.
   */
  public final String getMarginBottom() {
    return getProperty(STYLE_MARGIN_BOTTOM);
  }

  /**
   * Get the margin-left css property.
   */
  public final String getMarginLeft() {
    return getProperty(STYLE_MARGIN_LEFT);
  }

  /**
   * Get the margin-right css property.
   */
  public final String getMarginRight() {
    return getProperty(STYLE_MARGIN_RIGHT);
  }

  /**
   * Get the margin-top css property.
   */
  public final String getMarginTop() {
    return getProperty(STYLE_MARGIN_TOP);
  }

  /**
   * Get the opacity css property.
   */
  public final String getOpacity() {
    return getProperty(STYLE_OPACITY);
  }

  /**
   * Gets the overflow CSS property.
   */
  public final String getOverflow() {
    return getProperty(STYLE_OVERFLOW);
  }

  /**
   * Gets the overflow-x CSS property. 
   */
  public final String getOverflowX() {
    return getProperty(STYLE_OVERFLOW_X);
  }

  /**
   * Gets the overflow-y CSS property. 
   */
  public final String getOverflowY() {
    return getProperty(STYLE_OVERFLOW_Y);
  }

  /**
   * Get the padding css property.
   */
  public final String getPadding() {
    return getProperty(STYLE_PADDING);
  }

  /**
   * Get the padding-bottom css property.
   */
  public final String getPaddingBottom() {
    return getProperty(STYLE_PADDING_BOTTOM);
  }

  /**
   * Get the padding-left css property.
   */
  public final String getPaddingLeft() {
    return getProperty(STYLE_PADDING_LEFT);
  }

  /**
   * Get the padding-right css property.
   */
  public final String getPaddingRight() {
    return getProperty(STYLE_PADDING_RIGHT);
  }

  /**
   * Get the padding-top css property.
   */
  public final String getPaddingTop() {
    return getProperty(STYLE_PADDING_TOP);
  }

  /**
   * Gets the position CSS property.
   */
  public final String getPosition() {
    return getProperty(STYLE_POSITION);
  }

  /**
   * Gets the value of a named property.
   */
  public final String getProperty(String name) {
    assertCamelCase(name);
    return getPropertyImpl(name);
  }

  /**
   * Get the right css property.
   */
  public final String getRight() {
    return getProperty(STYLE_RIGHT);
  }

  /**
   * Gets the table-layout property.
   */
  public final String getTableLayout() {
    return getProperty(STYLE_TABLE_LAYOUT);
  }

  /**
   * Get the 'text-align' CSS property.
   */
  public final String getTextAlign() {
    return getProperty(STYLE_TEXT_ALIGN);
  }

  /**
   * Gets the text-decoration CSS property.
   */
  public final String getTextDecoration() {
    return getProperty(STYLE_TEXT_DECORATION);
  }
  
  /**
   * Get the 'text-indent' CSS property.
   */
  public final String getTextIndent() {
    return getProperty(STYLE_TEXT_INDENT);
  }
  
  /**
   * Get the 'text-justify' CSS3 property.
   */
  public final String getTextJustify() {
    return getProperty(STYLE_TEXT_JUSTIFY);
  }

  /**
   * Get the 'text-overflow' CSS3 property.
   */
  public final String getTextOverflow() {
    return getProperty(STYLE_TEXT_OVERFLOW);
  }

  /**
   * Get the 'text-transform' CSS property.
   */
  public final String getTextTransform() {
    return getProperty(STYLE_TEXT_TRANSFORM);
  }

  /**
   * Get the top css property.
   */
  public final String getTop() {
    return getProperty(STYLE_TOP);
  }

  /**
   * Gets the vertical-align CSS property.
   */
  public final String getVerticalAlign() {
    return getProperty(STYLE_VERTICAL_ALIGN);
  }

  /**
   * Gets the visibility CSS property.
   */
  public final String getVisibility() {
    return getProperty(STYLE_VISIBILITY);
  }

  /**
   * Get the 'white-space' CSS property.
   */
  public final String getWhiteSpace() {
    return getProperty(STYLE_WHITE_SPACE);
  }

  /**
   * Get the width css property.
   */
  public final String getWidth() {
    return getProperty(STYLE_WIDTH);
  }

  /**
   * Get the z-index css property.
   */
  public final String getZIndex() {
    return DOMImpl.impl.getNumericStyleProperty(this, STYLE_Z_INDEX);
  }

  /**
   * Set the background-color css property.
   */
  public final void setBackgroundColor(String value) {
    setProperty(STYLE_BACKGROUND_COLOR, value);
  }

  /**
   * Set the background-image css property.
   */
  public final void setBackgroundImage(@IsSafeUri String value) {
    setProperty(STYLE_BACKGROUND_IMAGE, value);
  }

  /**
   * Set the border-color css property.
   */
  public final void setBorderColor(String value) {
    setProperty(STYLE_BORDER_COLOR, value);
  }

  /**
   * Sets the border-style CSS property.
   */
  public final void setBorderStyle(BorderStyle value) {
    setProperty(STYLE_BORDER_STYLE, value.getCssName());
  }

  /**
   * Set the border-width css property.
   */
  public final void setBorderWidth(double value, Unit unit) {
    setProperty(STYLE_BORDER_WIDTH, value, unit);
  }

  /**
   * Set the bottom css property.
   */
  public final void setBottom(double value, Unit unit) {
    setProperty(STYLE_BOTTOM, value, unit);
  }

  /**
   * Sets the 'clear' CSS property.
   */
  public final void setClear(Clear value) {
    setProperty(STYLE_CLEAR, value.getCssName());
  }

  /**
   * Sets the color CSS property.
   */
  public final void setColor(String value) {
    setProperty(STYLE_COLOR, value);
  }

  /**
   * Sets the cursor CSS property.
   */
  public final void setCursor(Cursor value) {
    setProperty(STYLE_CURSOR, value.getCssName());
  }

  /**
   * Sets the display CSS property.
   */
  public final void setDisplay(Display value) {
    setProperty(STYLE_DISPLAY, value.getCssName());
  }

  /**
   * Set the float css property.
   */
  public final void setFloat(Float value) {
    setProperty(DOMImpl.impl.cssFloatPropertyName(), value.getCssName());
  }

  /**
   * Set the font-size css property.
   */
  public final void setFontSize(double value, Unit unit) {
    setProperty(STYLE_FONT_SIZE, value, unit);
  }

  /**
   * Sets the font-style CSS property.
   */
  public final void setFontStyle(FontStyle value) {
    setProperty(STYLE_FONT_STYLE, value.getCssName());
  }

  /**
   * Sets the font-weight CSS property.
   */
  public final void setFontWeight(FontWeight value) {
    setProperty(STYLE_FONT_WEIGHT, value.getCssName());
  }

  /**
   * Set the height css property.
   */
  public final void setHeight(double value, Unit unit) {
    setProperty(STYLE_HEIGHT, value, unit);
  }

  /**
   * Set the left css property.
   */
  public final void setLeft(double value, Unit unit) {
    setProperty(STYLE_LEFT, value, unit);
  }

  /**
   * Set the line-height css property.
   */
  public final void setLineHeight(double value, Unit unit) {
    setProperty(STYLE_LINE_HEIGHT, value, unit);
  }

  /**
   * Sets the list-style-type CSS property.
   */
  public final void setListStyleType(ListStyleType value) {
    setProperty(STYLE_LIST_STYLE_TYPE, value.getCssName());
  }

  /**
   * Set the margin css property.
   */
  public final void setMargin(double value, Unit unit) {
    setProperty(STYLE_MARGIN, value, unit);
  }

  /**
   * Set the margin-bottom css property.
   */
  public final void setMarginBottom(double value, Unit unit) {
    setProperty(STYLE_MARGIN_BOTTOM, value, unit);
  }

  /**
   * Set the margin-left css property.
   */
  public final void setMarginLeft(double value, Unit unit) {
    setProperty(STYLE_MARGIN_LEFT, value, unit);
  }

  /**
   * Set the margin-right css property.
   */
  public final void setMarginRight(double value, Unit unit) {
    setProperty(STYLE_MARGIN_RIGHT, value, unit);
  }

  /**
   * Set the margin-top css property.
   */
  public final void setMarginTop(double value, Unit unit) {
    setProperty(STYLE_MARGIN_TOP, value, unit);
  }

  /**
   * Set the opacity css property.
   */
  public final void setOpacity(double value) {
    DOMImpl.impl.cssSetOpacity(this, value);
  }

  /**
   * Set the outline-color css property.
   */
  public final void setOutlineColor(String value) {
    setProperty(STYLE_OUTLINE_COLOR, value);
  }

  /**
   * Sets the outline-style CSS property.
   */
  public final void setOutlineStyle(OutlineStyle value) {
    setProperty(STYLE_OUTLINE_STYLE, value.getCssName());
  }

  /**
   * Set the outline-width css property.
   */
  public final void setOutlineWidth(double value, Unit unit) {
    setProperty(STYLE_OUTLINE_WIDTH, value, unit);
  }

  /**
   * Sets the overflow CSS property.
   */
  public final void setOverflow(Overflow value) {
    setProperty(STYLE_OVERFLOW, value.getCssName());
  }

  /**
   * Sets the overflow-x CSS property.
   */
  public final void setOverflowX(Overflow value) {
    setProperty(STYLE_OVERFLOW_X, value.getCssName());
  }

  /**
   * Sets the overflow-y CSS property.
   */
  public final void setOverflowY(Overflow value) {
    setProperty(STYLE_OVERFLOW_Y, value.getCssName());
  }

  /**
   * Set the padding css property.
   */
  public final void setPadding(double value, Unit unit) {
    setProperty(STYLE_PADDING, value, unit);
  }

  /**
   * Set the padding-bottom css property.
   */
  public final void setPaddingBottom(double value, Unit unit) {
    setProperty(STYLE_PADDING_BOTTOM, value, unit);
  }

  /**
   * Set the padding-left css property.
   */
  public final void setPaddingLeft(double value, Unit unit) {
    setProperty(STYLE_PADDING_LEFT, value, unit);
  }

  /**
   * Set the padding-right css property.
   */
  public final void setPaddingRight(double value, Unit unit) {
    setProperty(STYLE_PADDING_RIGHT, value, unit);
  }

  /**
   * Set the padding-top css property.
   */
  public final void setPaddingTop(double value, Unit unit) {
    setProperty(STYLE_PADDING_TOP, value, unit);
  }

  /**
   * Sets the position CSS property.
   */
  public final void setPosition(Position value) {
    setProperty(STYLE_POSITION, value.getCssName());
  }

  /**
   * Sets the value of a named property.
   */
  public final void setProperty(String name, String value) {
    assertCamelCase(name);
    setPropertyImpl(name, value);
  }

  /**
   * Sets the value of a named property in the specified units.
   */
  public final void setProperty(String name, double value, Unit unit) {
    assertCamelCase(name);
    setPropertyImpl(name, value + unit.getType());
  }

  /**
   * Sets the value of a named property, in pixels.
   * 
   * This is shorthand for <code>value + "px"</code>.
   */
  public final void setPropertyPx(String name, int value) {
    setProperty(name, value, Unit.PX);
  }

  /**
   * Set the right css property.
   */
  public final void setRight(double value, Unit unit) {
    setProperty(STYLE_RIGHT, value, unit);
  }

  /**
   * Set the table-layout CSS property.
   */
  public final void setTableLayout(TableLayout value) {
    setProperty(STYLE_TABLE_LAYOUT, value.getCssName());
  }

  /**
   * Set the 'text-align' CSS property.
   */
  public final void setTextAlign(TextAlign value) {
    setProperty(STYLE_TEXT_ALIGN, value.getCssName());
  }

  /**
   * Sets the text-decoration CSS property.
   */
  public final void setTextDecoration(TextDecoration value) {
    setProperty(STYLE_TEXT_DECORATION, value.getCssName());
  }
  
  /**
   * Set the 'text-indent' CSS property.
   */
  public final void setTextIndent(double value, Unit unit) {
    setProperty(STYLE_TEXT_INDENT, value, unit);
  }

  /**
   * Set the 'text-justify' CSS3 property.
   */
  public final void setTextJustify(TextJustify value) {
    setProperty(STYLE_TEXT_JUSTIFY, value.getCssName());
  }

  /**
   * Set the 'text-overflow' CSS3 property.
   */
  public final void setTextOverflow(TextOverflow value) {
    setProperty(STYLE_TEXT_OVERFLOW, value.getCssName());
  }

  /**
   * Set the 'text-transform' CSS property.
   */
  public final void setTextTransform(TextTransform value) {
    setProperty(STYLE_TEXT_TRANSFORM, value.getCssName());
  }

  /**
   * Set the top css property.
   */
  public final void setTop(double value, Unit unit) {
    setProperty(STYLE_TOP, value, unit);
  }

  /**
   * Sets the vertical-align CSS property.
   */
  public final void setVerticalAlign(VerticalAlign value) {
    setProperty(STYLE_VERTICAL_ALIGN, value.getCssName());
  }

  /**
   * Sets the vertical-align CSS property.
   */
  public final void setVerticalAlign(double value, Unit unit) {
    setProperty(STYLE_VERTICAL_ALIGN, value, unit);
  }

  /**
   * Sets the visibility CSS property.
   */
  public final void setVisibility(Visibility value) {
    setProperty(STYLE_VISIBILITY, value.getCssName());
  }

  /**
   * Set the 'white-space' CSS property.
   */
  public final void setWhiteSpace(WhiteSpace value) {
    setProperty(STYLE_WHITE_SPACE, value.getCssName());
  }

  /**
   * Set the width css property.
   */
  public final void setWidth(double value, Unit unit) {
    setProperty(STYLE_WIDTH, value, unit);
  }

  /**
   * Set the z-index css property.
   */
  public final void setZIndex(int value) {
    setProperty(STYLE_Z_INDEX, value + "");
  }

  /**
   * Assert that the specified property does not contain a hyphen.
   * 
   * @param name the property name
   */
  private void assertCamelCase(String name) {
    assert !name.contains("-") : "The style name '" + name
        + "' should be in camelCase format";
  }

  /**
   * Gets the value of a named property.
   */
  private String getPropertyImpl(String name) {
    return DOMImpl.impl.getStyleProperty(this, name);
  }

  /**
   * Sets the value of a named property.
   */
  private native void setPropertyImpl(String name, String value) /*-{
    this[name] = value;
  }-*/;
}
