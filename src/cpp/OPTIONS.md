# Generating options
The `generate-options.R` script within this directory can be used to automatically generate C++ options code and corresponding Rmd documentation for any option `json` files specified on the command line. By default, the following json files are loaded, generating code and documentation for each:

* server/server-options.json
* server/server-options-overlay.json
* session/session-options.json
* session/session-options-overlay.json

The overlay options are only picked up if they exist, which they will not in the open source repo.

To generate for all options file, simply invoke the script `./generate-options.R` from within the `src/cpp` directory. If you only want to generate specific options files, you can specify each one on the command line. Note that you should only specify the non-overlay file - the overlay files are **always** pulled in if they exist.

*Note: When merging new open source options to Pro, you will need to rerun the generation script to ensure the documentation is generated, as the documentation only exists in the Pro repo.*

## Options JSON documentation
The following sections list all of the properties that can be specified within an options json file.

### Root document

| Property   |      Description     | 
|----------|-------------|
| metadata |  Metadata field which contains meta information about file generation, etc. |
| options | List of option categories which contain the actual options to generate/document. |

### metadata

| Property   |      Description     | 
|----------|-------------|
| generatorType |  The type of code generator to use. |
| namespace | The C++ namespace name to use. |
| includeGuard | The C++ include guard to use for the header file. |
| configFile | The name of the config file that these options are documenting. |
| docDescription | The document description to use for the list of options being generated. This will appear verbatim as the text in the admin guide appendix preceeding the options documentation. |
| outputSourceFile | The output file of the generated source. |
| outputHeaderFile | The output file of the generated source header. Only necessary for generators that output a source and header file. Regular ProgramOptions generation only generates a header file, so `outputSourceFile` is used instead. Overlay options require both a source and a header file. |
| outputDocFile | The output file of the generated Rmd documentation. |
| additionalIncludes | A string array of additional C++ includes that are needed to build the options. |
| additionalConstants | Additional constants to define. This is only used for overlay options to specify constants that should be generated with the rest of the auto generated constants, so that external source files can reference all constants from one file. |

### options

The options property is a string dictionary, where each entry is a category of options to specify. Within each category, you may specfy multiple `option` objects that may have the following properties.

| Property   |      Description     | 
|----------|-------------|
| name |  The name of the option. A constant may also be specified by setting this to an object and specifying the `constant` (for the name of the constant) and `value` properties (to document the actual value of the constant). Constants are generated for overlay options, and are expected to be pulled in as an additional include for regular options.
| type | The type of the option. Can be one of `string`, `bool`, `int`, `double`, `core::FilePath`, or `stringList` (which maps to std::vector). |
| defaultValue | Indicates the default value for the option, should one not be provided. This can also be specified as raw C++ code by setting it to an object containing the properties `code` (containing the raw code) and `description` (indicating a human readable string that describes what the code does). |
| description | The description of the option. This is the text, verbatim, that will be documented in the Rmd documentation. |
| isDeprecated | A boolean indicating whether or not the option is deprecated and should no longer be used. |
| isMultitoken | A boolean indicating whether or not the option is multitoken, meaning it can be specified multiple times with multiple values to be built as part of a list. |
| skipAccessorGeneration | A boolean indicating if the C++ accessor should not be generated. Use this for times when you need to specify your own complicated accessor, or simply do not need an accessor to be available (such as with deprecated options). |
| tempName | The name of the temp variable that the option will be read into. Only necessary if you are reading the option into a temporary variable, transforming the variable into another type, then surfacing that final transform to callers within the C++ code. Use this if you need to do some preprocessing on the option before it is made available by the accessor. |
| tempType| The type of the temporary variable which the option will be read into. |
| isHidden | A boolean indicating whether or not the option should be documented. This should only be used for options that are passed through the system that need to be hidden from the user, such as derivative options. |
| shortName | Specifies the short option name to use, as an alternative to the full name. Generally used for command line switches. |
