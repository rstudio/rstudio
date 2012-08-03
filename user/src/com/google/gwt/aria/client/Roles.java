/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.gwt.aria.client;
/////////////////////////////////////////////////////////
// This is auto-generated code.  Do not manually edit! //
/////////////////////////////////////////////////////////

/**
 * <p>Class containing the ARIA roles as defined by <a href="http://www.w3.org/TR/wai-aria/">
 * W3C ARIA specification</a>. A WAI-ARIA role is set on an element using a <i>role</i> attribute.
 * An element role is constant and is not supposed to change.</p>
 *
 * <p>This is the central class in this ARIA API because it contains all defined roles which
 * can be set to HTML elements. Each role in this class implements the {@link RoletypeRole}
 * interface, which contains generic methods for getting and setting states and properties.</p>
 *
 * <p>Lets say we have an image button widget and we want to make it visible to a reader as a
 * button, accompanied with some help text for the button usage. For the purpose we need to add a
 * 'button' role to the image and set label that the reader can interpret. We set the 'button' role
 * for an image (img) with the call: Roles.getButtonRole.set(img.getElement()) and set the
 * 'aria-label' property by calling: Roles.getButtonRole().setAriaLabelProperty(img.getElement,
 * "test")</p>
 *
 * <p>ARIA states are used similarly to ARIA properties by using the
 * Roles.getButtonRole().setAriaEnabledState(img.getElement(), isEnabled) method.
 * Although States and Properties are structurally the same, they are
 * separated in 2 classes in this API because they are semantically different and have different
 * usage. There exist the concept of extra properties and for now the only
 * example is tabindex. If we want to set the tabindex to 0 for the button,
 * we need to call Roles.getButtonRole().setTabindexExtraAttribute(img.getElement(), 0).</p>
 *
 * <p>There are 4 groups of roles:
 * <ol>
 * <li>Abstract roles -- used as base types for applied roles. They are not used by Web Authors
 * and would not be exposed as role definitions for incorporation into a Web page. Base classes are
 * referenced within the taxonomy and are used to build a picture of the role taxonomy class
 * hierarchy within the taxonomy.</li>
 * <li>Widget roles -- act as standalone user interface widgets or as part of larger,
 *  composite widgets</li>
 * <li>Widget container roles -- act as composite user interface widgets. These roles typically act
 *  as containers that manage other, contained widgets</li>
 * <li>Document structure roles -- describe structures that organize content in a page. Document
 * structures are not usually interactive</li>
 * <li>Landmark Roles -- regions of the page intended as navigational landmarks</li>
 * </ol>
 * </p>
 *
 * <p>For more details about ARIA roles check <a href="http://www.w3.org/TR/wai-aria/roles"></p>
 */
public final class Roles {
  private static final AlertdialogRole ALERTDIALOG = new AlertdialogRoleImpl("alertdialog");
  private static final AlertRole ALERT = new AlertRoleImpl("alert");
  private static final ApplicationRole APPLICATION = new ApplicationRoleImpl("application");
  private static final ArticleRole ARTICLE = new ArticleRoleImpl("article");
  private static final BannerRole BANNER = new BannerRoleImpl("banner");
  private static final ButtonRole BUTTON = new ButtonRoleImpl("button");
  private static final CheckboxRole CHECKBOX = new CheckboxRoleImpl("checkbox");
  private static final ColumnheaderRole COLUMNHEADER = new ColumnheaderRoleImpl("columnheader");
  private static final ComboboxRole COMBOBOX = new ComboboxRoleImpl("combobox");
  private static final ComplementaryRole COMPLEMENTARY = new ComplementaryRoleImpl("complementary");
  private static final ContentinfoRole CONTENTINFO = new ContentinfoRoleImpl("contentinfo");
  private static final DefinitionRole DEFINITION = new DefinitionRoleImpl("definition");
  private static final DialogRole DIALOG = new DialogRoleImpl("dialog");
  private static final DirectoryRole DIRECTORY = new DirectoryRoleImpl("directory");
  private static final DocumentRole DOCUMENT = new DocumentRoleImpl("document");
  private static final FormRole FORM = new FormRoleImpl("form");
  private static final GridcellRole GRIDCELL = new GridcellRoleImpl("gridcell");
  private static final GridRole GRID = new GridRoleImpl("grid");
  private static final GroupRole GROUP = new GroupRoleImpl("group");
  private static final HeadingRole HEADING = new HeadingRoleImpl("heading");
  private static final ImgRole IMG = new ImgRoleImpl("img");
  private static final LinkRole LINK = new LinkRoleImpl("link");
  private static final ListboxRole LISTBOX = new ListboxRoleImpl("listbox");
  private static final ListitemRole LISTITEM = new ListitemRoleImpl("listitem");
  private static final ListRole LIST = new ListRoleImpl("list");
  private static final LogRole LOG = new LogRoleImpl("log");
  private static final MainRole MAIN = new MainRoleImpl("main");
  private static final MarqueeRole MARQUEE = new MarqueeRoleImpl("marquee");
  private static final MathRole MATH = new MathRoleImpl("math");
  private static final MenubarRole MENUBAR = new MenubarRoleImpl("menubar");
  private static final MenuitemcheckboxRole MENUITEMCHECKBOX = new MenuitemcheckboxRoleImpl("menuitemcheckbox");
  private static final MenuitemradioRole MENUITEMRADIO = new MenuitemradioRoleImpl("menuitemradio");
  private static final MenuitemRole MENUITEM = new MenuitemRoleImpl("menuitem");
  private static final MenuRole MENU = new MenuRoleImpl("menu");
  private static final NavigationRole NAVIGATION = new NavigationRoleImpl("navigation");
  private static final NoteRole NOTE = new NoteRoleImpl("note");
  private static final OptionRole OPTION = new OptionRoleImpl("option");
  private static final PresentationRole PRESENTATION = new PresentationRoleImpl("presentation");
  private static final ProgressbarRole PROGRESSBAR = new ProgressbarRoleImpl("progressbar");
  private static final RadiogroupRole RADIOGROUP = new RadiogroupRoleImpl("radiogroup");
  private static final RadioRole RADIO = new RadioRoleImpl("radio");
  private static final RegionRole REGION = new RegionRoleImpl("region");
  private static final RowgroupRole ROWGROUP = new RowgroupRoleImpl("rowgroup");
  private static final RowheaderRole ROWHEADER = new RowheaderRoleImpl("rowheader");
  private static final RowRole ROW = new RowRoleImpl("row");
  private static final ScrollbarRole SCROLLBAR = new ScrollbarRoleImpl("scrollbar");
  private static final SearchRole SEARCH = new SearchRoleImpl("search");
  private static final SeparatorRole SEPARATOR = new SeparatorRoleImpl("separator");
  private static final SliderRole SLIDER = new SliderRoleImpl("slider");
  private static final SpinbuttonRole SPINBUTTON = new SpinbuttonRoleImpl("spinbutton");
  private static final StatusRole STATUS = new StatusRoleImpl("status");
  private static final TablistRole TABLIST = new TablistRoleImpl("tablist");
  private static final TabpanelRole TABPANEL = new TabpanelRoleImpl("tabpanel");
  private static final TabRole TAB = new TabRoleImpl("tab");
  private static final TextboxRole TEXTBOX = new TextboxRoleImpl("textbox");
  private static final TimerRole TIMER = new TimerRoleImpl("timer");
  private static final ToolbarRole TOOLBAR = new ToolbarRoleImpl("toolbar");
  private static final TooltipRole TOOLTIP = new TooltipRoleImpl("tooltip");
  private static final TreegridRole TREEGRID = new TreegridRoleImpl("treegrid");
  private static final TreeitemRole TREEITEM = new TreeitemRoleImpl("treeitem");
  private static final TreeRole TREE = new TreeRoleImpl("tree");

  public static AlertdialogRole getAlertdialogRole() {
    return ALERTDIALOG;
  }

  public static AlertRole getAlertRole() {
    return ALERT;
  }

  public static ApplicationRole getApplicationRole() {
    return APPLICATION;
  }

  public static ArticleRole getArticleRole() {
    return ARTICLE;
  }

  public static BannerRole getBannerRole() {
    return BANNER;
  }

  public static ButtonRole getButtonRole() {
    return BUTTON;
  }

  public static CheckboxRole getCheckboxRole() {
    return CHECKBOX;
  }

  public static ColumnheaderRole getColumnheaderRole() {
    return COLUMNHEADER;
  }

  public static ComboboxRole getComboboxRole() {
    return COMBOBOX;
  }

  public static ComplementaryRole getComplementaryRole() {
    return COMPLEMENTARY;
  }

  public static ContentinfoRole getContentinfoRole() {
    return CONTENTINFO;
  }

  public static DefinitionRole getDefinitionRole() {
    return DEFINITION;
  }

  public static DialogRole getDialogRole() {
    return DIALOG;
  }

  public static DirectoryRole getDirectoryRole() {
    return DIRECTORY;
  }

  public static DocumentRole getDocumentRole() {
    return DOCUMENT;
  }

  public static FormRole getFormRole() {
    return FORM;
  }

  public static GridcellRole getGridcellRole() {
    return GRIDCELL;
  }

  public static GridRole getGridRole() {
    return GRID;
  }

  public static GroupRole getGroupRole() {
    return GROUP;
  }

  public static HeadingRole getHeadingRole() {
    return HEADING;
  }

  public static ImgRole getImgRole() {
    return IMG;
  }

  public static LinkRole getLinkRole() {
    return LINK;
  }

  public static ListboxRole getListboxRole() {
    return LISTBOX;
  }

  public static ListitemRole getListitemRole() {
    return LISTITEM;
  }

  public static ListRole getListRole() {
    return LIST;
  }

  public static LogRole getLogRole() {
    return LOG;
  }

  public static MainRole getMainRole() {
    return MAIN;
  }

  public static MarqueeRole getMarqueeRole() {
    return MARQUEE;
  }

  public static MathRole getMathRole() {
    return MATH;
  }

  public static MenubarRole getMenubarRole() {
    return MENUBAR;
  }

  public static MenuitemcheckboxRole getMenuitemcheckboxRole() {
    return MENUITEMCHECKBOX;
  }

  public static MenuitemradioRole getMenuitemradioRole() {
    return MENUITEMRADIO;
  }

  public static MenuitemRole getMenuitemRole() {
    return MENUITEM;
  }

  public static MenuRole getMenuRole() {
    return MENU;
  }

  public static NavigationRole getNavigationRole() {
    return NAVIGATION;
  }

  public static NoteRole getNoteRole() {
    return NOTE;
  }

  public static OptionRole getOptionRole() {
    return OPTION;
  }

  public static PresentationRole getPresentationRole() {
    return PRESENTATION;
  }

  public static ProgressbarRole getProgressbarRole() {
    return PROGRESSBAR;
  }

  public static RadiogroupRole getRadiogroupRole() {
    return RADIOGROUP;
  }

  public static RadioRole getRadioRole() {
    return RADIO;
  }

  public static RegionRole getRegionRole() {
    return REGION;
  }

  public static RowgroupRole getRowgroupRole() {
    return ROWGROUP;
  }

  public static RowheaderRole getRowheaderRole() {
    return ROWHEADER;
  }

  public static RowRole getRowRole() {
    return ROW;
  }

  public static ScrollbarRole getScrollbarRole() {
    return SCROLLBAR;
  }

  public static SearchRole getSearchRole() {
    return SEARCH;
  }

  public static SeparatorRole getSeparatorRole() {
    return SEPARATOR;
  }

  public static SliderRole getSliderRole() {
    return SLIDER;
  }

  public static SpinbuttonRole getSpinbuttonRole() {
    return SPINBUTTON;
  }

  public static StatusRole getStatusRole() {
    return STATUS;
  }

  public static TablistRole getTablistRole() {
    return TABLIST;
  }

  public static TabpanelRole getTabpanelRole() {
    return TABPANEL;
  }

  public static TabRole getTabRole() {
    return TAB;
  }

  public static TextboxRole getTextboxRole() {
    return TEXTBOX;
  }

  public static TimerRole getTimerRole() {
    return TIMER;
  }

  public static ToolbarRole getToolbarRole() {
    return TOOLBAR;
  }

  public static TooltipRole getTooltipRole() {
    return TOOLTIP;
  }

  public static TreegridRole getTreegridRole() {
    return TREEGRID;
  }

  public static TreeitemRole getTreeitemRole() {
    return TREEITEM;
  }

  public static TreeRole getTreeRole() {
    return TREE;
  }
}
