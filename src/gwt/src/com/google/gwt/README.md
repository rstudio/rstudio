GWT Patches
------------
This folder contains selected GWT source files pulled from the GWT 2.8.2
release, then modified to fix issues or add needed functionality.

To edit a previously modified GWT source file:

- make the changes
- execute `updatepatches.sh` from this folder
- commit both the modified source file and the updated .diff file

To modify a previously unmodified GWT source file:

- update `updatesources.sh` and `updatepatches.sh` to reference the source file
- execute `updatesources.sh` to extract the source file (ignore warning about 
  missing diff file)
- make the changes
- execute `updatepatches.sh` from this folder
- add and commit the source file and .diff file

To upgrade to a new GWT version:

- update `updatesources.sh`, and `updatespatches.sh` to refer to new GWT
- execute `updatesources.sh` to stomp over the modified GWT files with the
  latest copies and reapply patches
- resolve any conflicts, test, etc.
- run `updatepatches.sh` to create updated copies of the .diff files

