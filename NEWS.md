
## v0.99 - Release Notes

### Source Editing

* Improvements to C/C++ editing mode:
    - Code completion
    - F2 code navigation (go to definition)
    - Scope tree for quick intra-file navigation
    
* Fuzzy matching for go to file/function

### Workspace

* Improved handling of objects containing or consisting of NULL `externalptr`

### Debugging

* Allow 'debugSource' to be executed in user-specified environment
* Improved heuristics for locating the stack frame where errors originated

### Packages

* Improved checking for supported protocol with packrat package
* Escape backslashes in library names when loading packages
