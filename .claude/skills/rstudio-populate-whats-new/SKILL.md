---
name: rstudio-populate-whats-new
description: Use when filling in the RStudio Desktop "What's New" page for a release from NEWS.md — writing, updating, or replacing the placeholder in src/node/desktop/src/assets/whats-new/<release-slug>/index.html. Trigger on phrases like "populate What's New", "add the What's New content", "update the What's New page", "What's New for <release name>", or any request to turn this release's NEWS.md entries into the window users see on first launch after an update.
---

# Populate the What's New content

Turns the current release's `NEWS.md` entries into the "What's New" page shown once per
release on RStudio Desktop startup, then commits it and opens a PR.

The work is mostly editorial: NEWS.md is written for everyone who touches the product,
and this page is written for one audience only. Most of this skill is about who that
audience is and what therefore gets cut.

## Steps

### 1. Verify the starting branch

```bash
git branch --show-current
```

If the output is `main`, `master`, or `rel-*`, create a feature branch before editing
(`feature/<slug>-whats-new`). If the user is already on a working branch, stay on it.

### 2. Locate the release and its content directory

```bash
cat version/RELEASE version/CALENDAR_VERSION version/PATCH
```

`version/RELEASE` holds the flower name (e.g. `Autumn Hawkbit`). Slugify it — lowercase,
drop apostrophes, hyphenate the rest — to get the directory:

    src/node/desktop/src/assets/whats-new/<slug>/index.html

That file usually already exists with a placeholder entry, seeded when the release was
branched. If it doesn't, create it from the template in
`src/node/desktop/src/assets/whats-new/README.md`, which is the source of truth for the
directory layout, the required `<head>` tags, and the available CSS classes.

Read the two or three most recent releases' `index.html` files before writing. They are
the real style guide — better than any description of the style here — and they show how
much detail an entry carries.

### 3. Read this release's NEWS.md

`NEWS.md` at the repository root holds only the in-progress release; earlier ones are
archived under `version/news/os/`. Read the whole file: entries worth showing are not
confined to the `### New` heading.

### 4. Select the entries

This is the part that takes judgment. The page lists **new features and enhancements
that an end user of RStudio Desktop would care about** — nothing else. Four filters, in
order of how often they change the answer:

**The audience is Desktop users, not administrators.** The What's New window ships only
in RStudio Desktop; RStudio Server never displays it. So drop anything whose audience is
someone configuring a deployment: session options, `rserver`/`rsession` flags, server
configuration files, launcher behavior, and fixes that only manifest under Server. These
are often the largest entries under `### New`, and they are exactly the ones to cut.

**The audience is users, not us.** Drop build tooling, CI, code signing, packaging
internals, crash-reporting plumbing, and test infrastructure.

**Features and enhancements, not fixes.** Ordinary bug fixes belong in the release notes,
which the page's closing Fixes section links to (see step 5). The exception is an entry
that is *worded* as a fix but reads as an improvement — a performance pass, or work that
extends a feature shipped last release. Judge by what the user experiences: "searching
and scrolling are faster" is an enhancement no matter which heading it sits under, while
"fixed a crash when …" is not.

**Deprecations and removals count.** If `### Deprecated / Removed` names something a user
relies on — a publishing destination going away, a workflow being retired — give it its
own short `Deprecated` section after the features. Users need more warning about
something disappearing than about something being added, and the release-notes link is
easy to miss.

Two smaller calls:

- **Bundled dependencies.** A component bump is worth a bare line only when users would
  notice it — a new Quarto minor, say, not a patch release of a language server. Confirm
  it is actually new this cycle by checking the previous release's `### Dependencies`
  list in `version/news/os/`; those lists are pruned to what changed, so the absence of a
  line does not mean the version held steady.
- **Renames.** A user-visible rename of a product or service belongs on the page, so
  people aren't confused by the new name. One line is enough.

### 5. Write the entries

Lead each item with a bolded short label, then one to three sentences in plain present
tense. Match the surrounding prose density of the previous releases' pages.

For anything headline-sized, **read the implementing commit rather than paraphrasing the
NEWS entry**. Find it with `git log --oneline --grep=<issue-number>` or by searching for
the feature's name. NEWS entries are written to be scanned by people who already know the
product; the commit message and the diff have what a user actually needs — the menu path,
the command names, the toolbar button, and the limits of the feature. The first question
a user asks is "where is it and when does it not work", and answering that inside the
entry is what makes the page worth reading.

Keep it factual. Avoid marketing adjectives — the house style in NEWS.md and past pages
is flat description, and it reads better than superlatives.

The page always ends with a Fixes section linking to the release notes, whether or not
you touched anything under `### Fixed`:

```html
  <div class="feature-section">
    <h2>Fixes</h2>
    <p>
      For a full list of bug fixes in this release, see the
      <a href="https://www.rstudio.org/links/release_notes#rstudio-<CALENDAR_VERSION>.<PATCH>">release notes</a>.
    </p>
  </div>
```

Every release's page carries it verbatim apart from the anchor, and it is what makes the
editing in step 4 defensible: the fixes are not being hidden from users, they are one
click away, so the page itself is free to carry only the things worth announcing. The
seeded placeholder normally has the section already — keep it, and check the anchor
against `version/` rather than assuming it was updated when the release branched.

So the finished page is, in order: `New Features`, then `Deprecated` if this release has
anything to deprecate, then `Fixes`.

Mechanical points that will otherwise bite:

- Leave the `<head>` untouched. The Content-Security-Policy meta tag and the
  `whats-new-base.css` link are enforced by a unit test.
- Escape markup in prose: `&gt;` in menu paths like `View &gt; Split Editor`, `&mdash;`
  for em dashes, and `<code>` for option names and code.
- Do not add an external link you have not verified resolves. A dead link in shipped
  desktop content is worse than no link — this page is baked into the installer and can't
  be corrected without a release.

### 6. Verify

```bash
cd src/node/desktop && npm test
```

The suite includes a "whats-new content validation" case that checks the required tags in
every release's page. Run `npm run lint` and `npm run typecheck` too, per the desktop
guidance in CLAUDE.md; with only an HTML asset changed they should pass untouched, so
treat any failure as pre-existing and report it rather than fixing it here.

To look at the page in a running IDE, launch RStudio Desktop with
`RSTUDIO_SHOW_WHATS_NEW=1`, or use **Help > What's New**. A developer build reads the
release name from `version/RELEASE` at runtime, so it shows the in-progress release's
folder.

### 7. Commit and open a PR

- **Commit message**: `Populate What's New content for <CALENDAR_VERSION> (<Release Name>)`
- **PR body**: the entries included, and — because it is the least obvious decision and
  the one a reviewer will second-guess — which NEWS entries were left out and why.
- **Milestone**: match `version/RELEASE` against existing milestones
  (`gh api repos/rstudio/rstudio/milestones --jq '.[].title'`) and set it if one matches.
  Never create a milestone.

No `NEWS.md` entry: this page *is* release-notes content, and adding an entry describing
it would be circular. No GitHub issue reference is needed either.
