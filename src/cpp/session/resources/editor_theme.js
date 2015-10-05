(function() {

// default operator colors 
var dark_theme_operator = "#AAAAAA";
var light_theme_operator = "#888888";

// theme-specific overrides ---------------------------------------------------

var operator_theme_map = {
   "solarized_light": "#93A1A1",
   "solarized_dark": "#B58900",
   "twilight": "#7587A6",
   "idle_fingers": "#6892B2",
   "clouds": light_theme_operator,
   "clouds_midnight": "#A53553",
   "cobalt": "#BED6FF",
   "dawn": light_theme_operator,
   "eclipse": light_theme_operator,
   "katzenmilch": light_theme_operator,
   "kr_theme": "#A56464",
   "merbivore": dark_theme_operator,
   "merbivore_soft": dark_theme_operator,
   "monokai": dark_theme_operator,
   "pastel_on_dark": dark_theme_operator,
   "vibrant_ink": dark_theme_operator,
   "xcode": light_theme_operator };

var keyword_theme_map = {
   "eclipse": "#800080",
   "clouds": "#800080",
};

var chunk_bg_proportion_map = {
   "tomorrow_night_bright": 0.85
};

var editor_fg_map = {
   "pastel_on_dark": "#EAEAEA"
};

// helpers --------------------------------------------------------------------

var rgb_from_color = function(color) {
  // TODO: deal with named colors and RGBA
  var rgb = [];
  for (var i = 1; i < color.length; i += 2) {
    rgb.push(parseInt(color.substr(i, i+1), 16));
  }
  return rgb;
};

var is_dark = function(color) {
  var color_sum = 0;
  var rgb = rgb_from_color(color); 
  for (var i = 0; i < rgb.length; i++) {
    color_some += rgb[i];
  }
  // let a "dark" theme be one with a color below netural gray 
  // (128 + 128 + 128)
  return color_sum < 384;
};

var style_from_selector = function(style_sheet, theme_name, selector) {
  for (var i = 0; i < style_sheet.length; i++) {
    if (style_sheet.item(i).selectorText === ".ace-" + theme_name + " " + 
        selector) {
      return style_sheet.item(i).style;
    }
  }
  return null;
};

var add_override_style = function(old_style_sheet, new_style_sheet, theme_name,
    selector, style) {
  // if there's already a selector with this rule, don't override
  // it (this allows theme authors to write their own
  // RStudio-specific colors if desired)
  var existing = style_from_selector(old_style_sheet, theme_name, selector);
  if (existing !== null)
    return;

  // TODO: add style
  new_style_sheet.createRule(CSSRule(selector, style));
};

//
// rule generators ------------------------------------------------------------
var create_operator_color_rule = function(style_sheet, theme_name) {

  if (typeof operator_theme_map[theme_name] !== "undefined")  {
    operator_color = operator_theme_map[theme_name];
    return new CSSRule(
        ".ace_keyword.ace_operator {" +
        "  color: " + operator_color + " !important;" +
        "}");
  }
  return null;
};

var create_line_marker_rule = function(marker_name, marker_color) {
  return new CSSRule(
    ".ace_marker-layer " + markerName + " {" +
    "  position: absolute;" +
    "  z-index: -1;" +
    "  background-color: " + markerColor +";" +
    "}");
};

// attempts to mutate editor theme; returns an error string, or null on success
var mutate_editor_theme = function(theme_name) {
  // find the stylesheet containing the filename 
  var style_sheet = null;
  var new_style_sheet = new CSSRuleList();
  var css_filename = theme_name + ".css";
  for (var i = 0; i < document.styleSheets.length; i++) {
    var href = document.styleSheets[i].href;
    if (href.substr(href.length - css_filename.length) === css_filename) {
      style_sheet = document.styleSheets[i].cssRuleList;
      break;
    }
  }

  // if we didn't find a stylesheet, bail out
  if (style_sheet === null)
    return "No style sheet found with name '" + theme_name + "'";

  // extract the name of the theme from the first rule that looks like it has
  // one
  var theme_css_name = null;
  for (var j = 0; j < style_sheet.length; j++) {
    var prefix = "ace-";
    var selector = style_sheet.item(j).selector;
    if (selector.substr(0, prefix.length) === prefix) {
      var idx = selector.indexOf(" ");
      if (idx === -1)
        idx = selector.length;
      theme_css_name = selector.substr(0, idx);
      break;
    }
  }

  if (theme_css_name === null) 
    return "Could not infer theme name from CSS file; add a rule to the top " +
      "of the file that includes the theme name, such as .ace-my-theme " + 
      "{ ... }";

  // apply theme style to ace_editor
  var theme_style = style_from_selector(style_sheet, theme_name, 
      "." + theme_name);
  if (theme_style === null)
    return "Could not find root theme style (expected to find a style named " +
      "." + theme_css_name;
  else
  {
    if (typeof(editor_fg_map[theme_name]) !== "undefined")
      theme_style.color = editor_fg_map[theme_name];
    add_override_style(style_sheet, new_style_sheet, theme_name,
        ".ace_editor", theme_style);
  }

  // extract keyword color
  var keyword_style = style_from_selector(style_sheet, theme_name, 
      ".ace_keyword");
  if (keyword_style === null)
    return "Could not find keyword style (expected to find a style named " +
      "." + theme_css_name + " .ace_keyword)";
  else
    add_override_style(style_sheet, new_style_sheet, theme_name,
        ".nocolor.ace_editor .ace_line span",
        "color: " + keyword_color + "!important");

  // override bracket style 
  var is_dark_theme = is_dark(theme_style.backgroundColor);
  var operator_bg_color = is_dark_theme ? 
      "rgba(128, 128, 128, 0.5)" :
      "rgba(192, 192, 192, 0.5)";
  add_override_style(style_sheet, new_style_sheet, theme_name, ".ace_bracket",
      "  margin: 0 !important;" +
      "  border: 0 !important;" +
      "  background-color: " + operator_bg_color + ";");
};

// export theme mutation function
window.mutate_editor_theme = mutate_editor_theme;
})();
