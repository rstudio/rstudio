package org.rstudio.studio.client.workbench.prefs.views;

public interface CompiledPdfPreferencesPaneConstants extends com.google.gwt.i18n.client.Constants {

    /**
     * Translated "PDF Generation".
     *
     * @return translated "PDF Generation"
     */
    @DefaultStringValue("PDF Generation")
    @Key("headerPDFGenerationLabel")
    String headerPDFGenerationLabel();

    /**
     * Translated "NOTE: The Rnw weave and LaTeX compilation options are also set on a ".
     *
     * @return translated "NOTE: The Rnw weave and LaTeX compilation options are also set on a "
     */
    @DefaultStringValue("NOTE: The Rnw weave and LaTeX compilation options are also set on a ")
    @Key("perProjectNoteLabel")
    String perProjectNoteLabel();

    /**
     * Translated "per-project (and optionally per-file) basis. Click the help".
     *
     * @return translated "per-project (and optionally per-file) basis. Click the help "
     */
    @DefaultStringValue("per-project (and optionally per-file) basis. Click the help ")
    @Key("perProjectHelpLabel")
    String perProjectHelpLabel();

    /**
     * Translated "icons above for more details.".
     *
     * @return translated "icons above for more details."
     */
    @DefaultStringValue("icons above for more details.")
    @Key("perProjectIconsAboveLabel")
    String perProjectIconsAboveLabel();

    /**
     * Translated "LaTeX Editing and Compilation".
     *
     * @return translated "LaTeX Editing and Compilation"
     */
    @DefaultStringValue("LaTeX Editing and Compilation")
    @Key("perProjectHeaderLabel")
    String perProjectHeaderLabel();

    /**
     * Translated "Use tinytex when compiling .tex files".
     *
     * @return translated "Use tinytex when compiling .tex files"
     */
    @DefaultStringValue("Use tinytex when compiling .tex files")
    @Key("chkUseTinytexLabel")
    String chkUseTinytexLabel();

    /**
     * Translated "Clean auxiliary output after compile".
     *
     * @return translated "Clean auxiliary output after compile"
     */
    @DefaultStringValue("Clean auxiliary output after compile")
    @Key("chkCleanTexi2DviOutputLabel")
    String chkCleanTexi2DviOutputLabel();

    /**
     * Translated "Enable shell escape commands".
     *
     * @return translated "Enable shell escape commands"
     */
    @DefaultStringValue("Enable shell escape commands")
    @Key("chkEnableShellEscapeLabel")
    String chkEnableShellEscapeLabel();

    /**
     * Translated "Insert numbered sections and subsections".
     *
     * @return translated "Insert numbered sections and subsections"
     */
    @DefaultStringValue("Insert numbered sections and subsections")
    @Key("insertNumberedLatexSectionsLabel")
    String insertNumberedLatexSectionsLabel();

    /**
     * Translated "PDF Preview".
     *
     * @return translated "PDF Preview"
     */
    @DefaultStringValue("PDF Preview")
    @Key("previewingOptionsHeaderLabel")
    String previewingOptionsHeaderLabel();

    /**
     * Translated "Always enable Rnw concordance (required for synctex)".
     *
     * @return translated "Always enable Rnw concordance (required for synctex)"
     */
    @DefaultStringValue("Always enable Rnw concordance (required for synctex)")
    @Key("alwaysEnableRnwConcordanceLabel")
    String alwaysEnableRnwConcordanceLabel();

    /**
     * Translated "Preview PDF after compile using:".
     *
     * @return translated "Preview PDF after compile using:"
     */
    @DefaultStringValue("Preview PDF after compile using:")
    @Key("pdfPreviewSelectWidgetLabel")
    String pdfPreviewSelectWidgetLabel();

    /**
     * Translated "Help on previewing PDF files".
     *
     * @return translated "Help on previewing PDF files"
     */
    @DefaultStringValue("Help on previewing PDF files")
    @Key("pdfPreviewHelpButtonTitle")
    String pdfPreviewHelpButtonTitle();

    /**
     * Translated "Sweave".
     *
     * @return translated "Sweave"
     */
    @DefaultStringValue("Sweave")
    @Key("preferencesPaneTitle")
    String preferencesPaneTitle();

    /**
     * Translated "(No Preview)".
     *
     * @return translated "(No Preview)"
     */
    @DefaultStringValue("(No Preview)")
    @Key("pdfNoPreviewOption")
    String pdfNoPreviewOption();

    /**
     * Translated "(Recommended)".
     *
     * @return translated "(Recommended)"
     */
    @DefaultStringValue("(Recommended)")
    @Key("pdfPreviewSumatraOption")
    String pdfPreviewSumatraOption();

    /**
     * Translated "RStudio Viewer".
     *
     * @return translated "RStudio Viewer"
     */
    @DefaultStringValue("RStudio Viewer")
    @Key("pdfPreviewRStudioViewerOption")
    String pdfPreviewRStudioViewerOption();

    /**
     * Translated "System Viewer".
     *
     * @return translated "System Viewer"
     */
    @DefaultStringValue("System Viewer")
    @Key("pdfPreviewSystemViewerOption")
    String pdfPreviewSystemViewerOption();

    /**
     * Translated "Weave Rnw files using:".
     *
     * @return translated "Weave Rnw files using:"
     */
    @DefaultStringValue("Weave Rnw files using:")
    @Key("rnwWeaveSelectLabel")
    String rnwWeaveSelectLabel();

    /**
     * Translated "Help on weaving Rnw files".
     *
     * @return translated "Help on weaving Rnw files"
     */
    @DefaultStringValue("Help on weaving Rnw files")
    @Key("helpButtonLabel")
    String helpButtonLabel();

    /**
     * Translated "Typeset LaTeX into PDF using:".
     *
     * @return translated "Typeset LaTeX into PDF using:"
     */
    @DefaultStringValue("Typeset LaTeX into PDF using:")
    @Key("latexProgramSelectLabel")
    String latexProgramSelectLabel();

    /**
     * Translated "Help on customizing LaTeX options".
     *
     * @return translated "Help on customizing LaTeX options"
     */
    @DefaultStringValue("Help on customizing LaTeX options")
    @Key("helpLaxtexButtonLabel")
    String helpLaxtexButtonLabel();
}
