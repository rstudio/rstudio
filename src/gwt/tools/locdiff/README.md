# locdiff

Locdiff is a command-line utility to generate a CSV showing all English UI strings that
have been added or modified between two commit hashes of the RStudio IDE repository.

The tool compares two cloned instances of the RStudio repo; one named "old" and one "new".
You must create these yourself and checkout the desired commit hashes in each.

It also shows the most recent French translation (if any) for each string to make it easier to
identify what work is needed.

Some useful commit hashes:

- 9f796939 - Prairie Trillium release (2022.02.0+443)
- 8aaa5d47 - Prairie Trillium patch 1 (2022.02.1+461)
- 8acbd38b - Prairie Trillium patch 2 (2022.02.2+485)
- 1db809b8 - Prairie Trillium patch 3 (2022.02.3+492)
- c0935c0f - Prairie Trillium patch 4 (2022.02.4+500)
- 34ea3031 - Spotted Wakerobin release (2022.07.0+548)
- 7872775e - Spotted Wakerobin patch 1 (2022.07.1+554)
- e7373ef8 - Spotted Wakerobin patch 2 (2022.07.2+576)

## Usage

The tool requires `node.js` and works on Mac, Windows, or Linux. Node 16 was used
during development.

```bash
cd rstudio/src/gwt/tools/locdiff
git clone git@github.com:rstudio/rstudio
mv rstudio old
git clone git@github.com:rstudio/rstudio
mv rstudio new
cd old
git checkout 9f796939
cd ../new
git checkout main
cd ..
npm i
node start
```

Results are written to `locdiff.csv`, which can be loaded into Excel, etc.
To view the French strings in Excel, you will need to import (rather than open) the csv and
specify that it is comma-separated and stored as Unicode / UTF-8, otherwise the French strings
may be garbled.

The file will load directly into Apple Numbers if you have that handy.
