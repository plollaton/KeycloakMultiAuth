---
name: task-execution-authoring
description: >
  Rules of conduct for executing a task from a spec: where tasks live,
  which files make up the spec, how to respect the domain boundary, the
  quality-checks policy, handling drift between documentation and code,
  updating tasks.md, and when to stop and ask the human. Use whenever
  you are implementing a task from a spec in `.agents/specs/`.
---

# Executing tasks from a spec

You implement a batch of one or more tasks from a work unit (spec),
selected by the user and sent together in the same session. The order in which they
arrive follows the `tasks.md` and is only a **suggestion**: you decide what to implement
together or separately and in which sequence, respecting the dependencies between them. Each task
says **what** to deliver; the **how** you discover by reading the spec artifacts and the
authoritative documentation of the domain. Do not invent scope, do not anticipate tasks outside
the batch, and do not close a task with open work.

## Where the tasks live

Each work unit lives in `.agents/specs/<NNN>-<slug>/`, where `<NNN>` is the
sequential number (e.g., `001`, `015`) and `<slug>` identifies the domain (and the
feature, when there is one).

The `tasks.md` is the spec checklist. Each task appears as a checkbox item
preceded by a stable identifier assigned when the spec was generated:

```markdown
- [ ] **T3. REST endpoints and contracts**
```

Locate each task in the batch by the identifiers you received (e.g., `T3`, `T5`).
Mark `- [ ]` → `- [x]` for each one only when its implementation is actually
complete.

## The spec files

Before implementing, consolidate what you know about creating specs, plans and tasks to help you understand what to expect in each artifact.

## Inputs from the target project

Beyond the spec, consider:

- **The domain authoritative documentation** referenced by the spec — the source of truth
  for the business. It may be a local skill (discovered by its frontmatter, without depending on a
  cited path) or an external source accessed via MCP (e.g., Confluence, Azure
  DevOps wiki). Consult it through the reference the spec indicates.
- **Project instructions** — the agent instructions file at the root (e.g., `AGENTS.md`,
  `CLAUDE.md`), loaded automatically: stack, conventions, directory structure and
  check commands.
- **`.agents/context/discovery-answers.md`**, when present — constraints
  declared by the user; they take precedence over assumptions.
- **Existing code** — read the related code to understand the style, the
  conventions and the solutions already adopted. Do not copy code from other areas of the project
  that are unrelated to the task, to avoid drift between domains.

If something essential for the task is not accessible or is ambiguous, **that
becomes a question to the human**, not an inference. Do not fill gaps by guessing.

## Recognize the current state before implementing

Before writing code, check what of this task already exists in the repository:
related files, modules, functions, endpoints or screens. The task describes a
target state; deliver the delta between what already exists and that target — integrate with
what already meets it, without recreating it, and transform what needs to change. Refactoring,
rewriting, migrating or fixing existing code is legitimate work when that is
what the task asks for; what you avoid is reimplementing from scratch what already meets the
target.

If the task is already entirely satisfied by the current code — and you confirm
that it meets what the task asks, including the applicable checks —, complete it without
reimplementing: mark the checkbox and report in the summary what you found. If there is
doubt about whether the existing code meets it, escalate as a decision instead of redoing it
to be safe.

If the recognition indicates that the code the task orders you to change has no
real use in the application — it is dead code (no references or calls, a route
or endpoint turned off, a feature permanently disabled) —, do not change it
blindly: escalate as a decision to the human (change it anyway, remove it, or
handle it another way), instead of investing effort on code that maybe
should be removed.

## Domain boundary during implementation

The spec records two lists: what **this** unit implements and what belongs
to **other** domains. Respect them:

- Implement only what is within the boundary of this spec.
- Do not "fix in passing" code from another domain, even if it looks trivial or
  wrong. If it is blocking, escalate as a question.
- Dependencies on another domain are preconditions (already delivered) or blockers
  (to escalate) — never work to do within this task.

## Quality checks before closing

There is no fixed set of checks: discover which ones the project declares in its
instruction files (e.g., `AGENTS.md`) and run the ones that apply to the task — they may be lint,
type-check, tests, build or any other name the project adopts.
Durable policy:

- **Do not invent commands.** If the project instructions do not declare a check, it does not
  apply — report it as skipped, do not improvise.
- **`skipped` is a valid result**, as long as it comes with a justification (e.g., there is no
  e2e suite in the project; the check does not cover the area touched).
- On a check that fails, make **up to 3 targeted attempts** to fix it before
  escalating the question to the human. Do not loop indefinitely.
- **Pre-existing failures** in the project (that already failed before your change)
  are reported separately and stay outside the task scope; do not mix them
  with failures you introduced.
- Never silence a check failure nor close the task with a red check that
  you caused.

## Reporting the impacted test cases

The spec folder has a `test-cases.md`: the catalog of test cases (each with a
stable id like `TC-3` and a title), grouped by user story, that doubles as the
manual validation runbook. For each task in the batch, identify which test cases
it exercises — the success and exception cases whose behavior the task delivers or
changes — and report them by id and title, so the human can look them up in
`test-cases.md` and validate the delivery. Report only references (id + title),
not the steps: the steps live in the file, which is the single source of truth.

Do not edit `test-cases.md` to add or adjust cases while executing a task. If a
task delivers observable behavior with no corresponding test case in the catalog,
that is a coverage gap in the spec — escalate it as a question to the human, do
not invent the case here.

When E2E testing is part of what the task delivers, the automated E2E tests it
produces implement the cases from this catalog — the same ids anchor the manual
runbook and the automated tests.

## Handling drift between the authoritative documentation and the code

Drift is a divergence between the domain authoritative documentation (a local skill
or an external source via MCP) and the code. There are two paths:

1. **Explicit documentation task.** When the current task is itself a task
   to create, update or fix the authoritative documentation, execute it normally —
   updating the documentation IS the work of the task.
2. **Drift discovered during implementation.** If, while implementing, you notice that the
   authoritative documentation omitted something, described it wrong or is incomplete, **do not
   update it on your own**. That is a question to escalate to the human,
   not a silent fix.

On an "implementation bug", if while investigating you conclude that the authoritative documentation
**is also** incorrect, that is emergent drift — escalate as a question, do not fix it in
silence.

Update the authoritative documentation **only** when the task explicitly asks for it
or when the human authorizes it while answering a question.

## Do not leave silent debt between tasks

Two situations leave debt that the checks do not catch and that gets lost between batches
of tasks running in separate sessions: a deliberate temporary construct that a
later task replaces, and unintended leftovers when closing (an unused dependency or
file). Neither of them may pass in silence.

### Forward-dependency: deliberate temporary construct

Sometimes a task needs something that will only be delivered definitively by
a later task — a hardcoded value embedded before the task that delivers the
configuration exists, a stub in place of a service that another task implements,
or a simplified adapter before the definitive one. Creating a deliberately
temporary construct is acceptable, but **never
silently**: the destination task may run in another session, so without a trace
this debt gets lost between one session and another and the temporary construct becomes
definitive by oversight.

The trace has two parts, with distinct roles:

- **`forward-deps.md`, in the spec folder — the index and the source of truth.** It is
  what the destination task reads to know what it needs to replace. Each entry has
  a stable id, the origin task, the destination task, a `status` and the constructs
  involved:

  ```markdown
  # Forward-dependencies — 012-reporting

  Intentional debts between tasks: temporary constructs created by one task
  to be replaced by another. The spec does not close with `open` or
  `planned` items.

  ## FD-1 — provisional stub of the configuration provider
  - origin: T2
  - destination: T5
  - status: open
  - constructs: `ConfigProviderStub`, `DEFAULT_LIMITS`
  - expected resolution: replace with the definitive configuration provider delivered in T5.
  ```

- **A marker in the code — a local signal, not the source of truth.** Next to the
  temporary construct, point to the ledger entry, so that whoever edits
  that file for another reason does not treat the placeholder as definitive. Use the
  comment syntax of the project language:

  ```
  // FORWARD-DEP(012-reporting/FD-1): provisional stub of the configuration provider;
  // replace with the definitive one delivered in T5. See forward-deps.md.
  const config = ConfigProviderStub.withDefaults(DEFAULT_LIMITS);
  ```

Lifecycle:

- **When creating the temporary construct** (origin task): record an
  `open` entry in `forward-deps.md` (creating the file if it does not exist yet) and leave the
  marker in the code. If the spec already anticipated the debt, a `planned` entry
  may exist — promote it to `open`.
- **When executing the destination task:** before closing, read `forward-deps.md` and
  resolve every entry addressed to it — remove the temporary construct and the
  marker, and change the `status` to `resolved`. Resolving the open entries
  addressed to the current task is part of "done"; do not mark the checkbox
  leaving an `open` or `planned` entry addressed to it.
- **`resolved` entries stay in the file** as an auditable trace of which
  task created and which resolved the debt.

When to escalate instead of recording:

- If the temporary construct **has no clear destination task** (no later
  task will replace it), that is a scope gap — escalate as a question to the
  human instead of creating an orphan entry.
- If the marker in the code and the ledger **diverge** (a marker without a
  corresponding entry, or a `resolved` entry with the marker still in the code), that is
  drift — escalate as a question, do not fix it by guessing.

### Delivery coherence before closing

Green checks do not guarantee a coherent delivery. Lint, type-check, tests and build
usually do **not** reject an installed and unused dependency, a file
created and not referenced, or two concurrent implementations of the same thing
with one of them dead. These are silent debt: they pass the checks and do not surface
on their own. Before marking the task as complete, review the coherence of what
you delivered:

- **Installed dependency = used dependency.** Every dependency you
  add must be effectively configured and exercised by code in this task.
  If you installed something that only a later task will configure, that is a
  forward-dependency (record it in `forward-deps.md` and mark it in the code) or a
  decision to escalate — never install and leave it unused.
- **Created file = referenced file.** Every file, asset or catalog
  you create must be loaded or referenced by code in this task. An
  orphan file is removed or, if it was created for a future task, recorded
  as a forward-dependency.
- **One source of truth per piece of data.** Do not leave two concurrent implementations
  of the same thing (e.g., a catalog embedded in the code and the same catalog in an
  external file that nobody loads), with one of them dead. Choose one; if the
  other is the target of a later task, that is a forward-dependency.

If you cannot resolve the incoherence within the scope of the task, escalate the
decision to the human instead of closing the task with the inconsistent state.

## Updating the `tasks.md`

The `tasks.md` is a Markdown checklist. For each task in the batch:

- Locate the entry by the identifier you received.
- Mark the checkbox as `[x]` when you complete the implementation of that task.
- Apply changes only when the user explicitly asks for it (change the
  description, the order or a note of a task).

Do not write audit metadata in the `tasks.md` (files touched, check results,
execution summary). Those execution data are reported to the tool
that drives the session, never written to the file.

## When to escalate a decision to the human

Anything that requires a **choice from the human** for the unit to proceed correctly is a
decision to escalate — you ask and wait for the answer, instead of deciding alone or
leaving it implicit. Escalate when there is:

- technical ambiguity that the artifacts do not resolve;
- a design decision that affects the domain boundary;
- drift between the authoritative documentation and the code, discovered during implementation;
- checks persistently failing after reasonable attempts;
- a cross-domain dependency not declared in the spec;
- a missing precondition (environment variable, configuration, local service);
- **installing a dependency without a concrete basis**: before installing or adopting
  any dependency absent from the project, confirm there is a concrete traceable
  basis (already in use in the code, asserted by the user, confirmed in the
  `functional-map.md`/`discovery-answers.md`, firm in the `AGENTS.md`, or with
  provenance recorded in the spec `research.md`). A hedged basis ("to be
  confirmed", "or similar") or an absent one is a choice for the human — escalate before
  installing; the dependency appearing in the `tasks.md` is not, by itself, that basis;
- **dead code in the task target**: the code the task asks you to change
  is not actually used by the application (no references/calls, a route
  turned off, a feature permanently disabled) — changing, removing or
  preserving it is a choice for the human, not your decision;
- **a scope gap**: you notice that a feature that should have been anticipated
  was left out of the documented scope. Do not include it nor implement it on your own,
  and do not demote it to a passing note — it is a choice for the human (include it in the
  scope now, treat it as future work, or consciously accept its absence).

Do not escalate when the choice is 100% internal to the module, when it is a
stylistic adjustment with no behavior impact, or when the artifacts answer the
question clearly. Escalating what is already answered only delays the delivery.

Important distinction: a **decision** blocks a choice that belongs to the human and is therefore
escalated for them to answer. Whereas **deferrable, out-of-scope** work that does not depend
on any choice (heavy global validations, minor improvements and technical debt)
is not a decision — it is merely reported for awareness, without interrupting. When in doubt between the
two, prefer to escalate as a decision.

## Anti-patterns to refuse

- Marking a task as complete with open questions or work still pending.
- Updating the authoritative documentation silently upon identifying drift.
- Ignoring a check failure under the justification of an "irrelevant reason".
- Fixing in passing code from another domain.
- Implementing behavior different from what the authoritative documentation defines without escalating a question.
- Adding new tasks to the `tasks.md` silently.
- Including in scope or implementing a feature not anticipated in the spec without escalating the decision.
- Demoting a choice that belongs to the human (e.g., a scope gap) to a mere note.
- Marking the checkbox before the work is actually complete.
- Moving logic between tasks on your own initiative.
- Creating a temporary construct without recording it in `forward-deps.md` and without a marker in the code.
- Closing the destination task leaving an open `forward-deps.md` entry addressed to it.
- Installing a new dependency without concrete traceable confirmation, just because the task asks for it.
- Installing a dependency and not configuring or using it in this task.
- Creating a file, asset or catalog that no code in this task references.
- Keeping two concurrent implementations of the same data source with one of them dead.
- Changing dead code blindly instead of escalating the decision to the human.

## Maintaining this skill

Update this skill whenever the expected behavior during task execution changes
— the checks policy, the boundary rules, drift handling or the
criteria for escalating a question. Keeping it aligned with practice avoids drift in
the execution conduct itself.
