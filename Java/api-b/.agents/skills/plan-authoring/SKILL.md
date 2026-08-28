---
name: plan-authoring
description: >
  Standard for authoring the plan.md and the auxiliary technical artifacts
  (data-model.md, research.md, contracts/*, ui/*) in
  spec-driven development. Use when generating or reviewing the technical view of a
  work unit.
---

# Authoring plan.md

The `plan.md` answers **how**, in a general technical view. It references technical
decisions, not business rules (those live in the `spec.md` and in the domain source
documentation). It lives in the same unit folder,
`.agents/specs/<NNN>-<slug>/`. When a section grows, migrate the content to
an auxiliary artifact and link from here.

## Current code state

Start from what the repository already has. Stack, structure and technical decisions
reuse what is already implemented, except when the target itself is
to transform it (refactoring, rewriting, migration); the plan traces the path from the
current state to the target, not from a zero point. Acknowledge modules, patterns and
infrastructure already present before proposing new ones.

## Technical dependencies declared by the domain

When `spec.md` included, in "what this spec implements", a technical dependency from the
domain's `.agents/skills/<domain-slug>/references/technical-dependencies.md` (as cross-
cutting infrastructure this unit creates), detail HOW it is set up here, under "Technical
decisions" — same as any other technical decision. The decision (build it now vs. leave it
explicitly pending for a future unit) is made here, in `plan.md` — never deferred to
`tasks.md`, which only turns an already-made decision into the named prerequisite step of
the first task that needs it.

## Technology only settled on a firm basis

A choice of technology, library, framework, dependency or relevant technical
pattern only enters the plan as a SETTLED decision when it has a concrete basis: it is already
in use in the code, or it was stated WITHOUT RESERVATION by the user or by the
source artifacts (`functional-map.md`, `discovery-answers.md`, the unit
description, `AGENTS.md`). If the source carries a reservation ("to be confirmed", "or similar",
"suggested", "likely") or would be your own choice without backing, it is a
pending confirmation: escalate as a gap with the concrete alternatives, instead of
settling the decision and pushing the confirmation to the implementation.

For each technological decision you settle, record the provenance of the
confirmation in `research.md` ("in use in the code", "confirmed by the user in
`discovery-answers`", "`AGENTS.md`") and cite it, if useful, in `plan.md`. This trail
is what allows the implementation to adopt or install without reopening the decision.

## Expected sections

- **Stack and structure** — aligned with the project conventions (`AGENTS.md`), without
  repeating them.
- **Technical decisions** — overview; tradeoffs and confirmation provenance
  detailed in `research.md`.
- **Data model** — summary; details in `data-model.md`.
- **External contracts** — summary; specifications in `contracts/*`.
- **Interface** — summary; details in `ui/*`.
- **Testing strategy** — which levels (unit, integration, e2e) fit in
  which tasks; and which test infrastructure already exists versus what needs
  to be introduced by this unit (runner, libs). This is a technical
  prerequisite, not a business rule — it lives here, never in a user story.
- **Impact on the authoritative documentation** — which source documentation is
  affected and becomes a task in `tasks.md`. The authoritative documentation can be a
  local skill (`.agents/skills/<slug>/`) or an external source accessed via MCP
  (Confluence, Azure DevOps, etc.). Record here ONLY the **deliberate drift**: the
  unit changes the behavior on purpose and there is a recorded decision that backs it
  (unit description, `discovery-answers.md`, `AGENTS.md`, or an already-resolved gap).
  Say what diverges and why — it is this record that generates the task that updates the
  **doc** (the target is always the authoritative doc, never the `spec.md`). Without deliberate
  drift, declare "no impact" and no doc task appears. A divergence
  between the doc and the code (or the spec) without a recorded decision that resolves it does NOT enter
  here as if it were decided nor does it become a warning: it is an open question (gap), escalated
  to the human — even when it seems obvious which side is right, and including when
  the doc itself seems to be the wrong part.

## Optionals with consolidated vocabulary

They live in the unit folder. Use these names when the content applies:

- `data-model.md` — entities, attributes, relations, indexes, constraints,
  ENUMs. In frontend, consumed stores/state shapes.
- `research.md` — for each non-trivial decision: context, alternatives,
  decision, rationale, confirmation basis (provenance) and consequences.

## Open folders

The file names are your decision, reflecting the content:

- `contracts/` — formal contracts. The extension reflects the technology:
  `*.openapi.yaml` (REST), `*.graphql`, `*.proto` (gRPC),
  `*.asyncapi.yaml` (events), `*.md` (protocols without a formal standard).
- `ui/` — interface description. The decomposition varies according to the nature
  (mobile, web admin, dashboard, public site). Examples: `screens.md`,
  `forms.md`, `flows.md`, `accessibility.md`, `states.md`.

## Deciding on optional artifacts

Do not generate only the ones you remember: decide explicitly, for EACH canonical
optional, whether you generate or waive it, with a one-line reason. Record the decision
in `plan.md` — in the corresponding section when it exists (e.g.: the verdict on
`data-model.md` in the "Data model" section) or in a short subsection
"Optional artifacts" for those without a section of their own (typically
`research.md`).

Canonical optionals to always evaluate: `data-model.md`, `research.md`,
`contracts/`, `ui/`.

Single criterion: generate it if it helps avoid ambiguity in executing the tasks. If the
content fits in a short section of `plan.md`, it does not become a separate file — but the
decision to waive it still needs to be recorded.

## External dependencies

When a dependency requires a formal technical contract, point to the file in
`contracts/`. Do not detail the implementation of another domain here.

## Complete plan.md example

The example uses the fictional domain **Cost Centers** (whole domain, pure
backend), the same as the spec.

```markdown
# Plan: Cost Centers

## Stack and structure

Node/TypeScript backend in a hexagonal architecture per `AGENTS.md`. The
`cost-centers` module lives in `src/modules/cost-centers/` following the project
organization pattern.

## Technical decisions

- **Hierarchy**: adjacency list (`parent_id`) with reads via a recursive CTE.
  Tradeoffs in `research.md`.
- **Soft delete vs status enum**: status enum (`active`/`inactive`) with
  a record in `cost_center_history`. There is no physical deletion — it preserves
  the accounting history.
- **Cycle validation**: done in the application during a parent change,
  querying the current tree via CTE.
- **Event emission**: synchronous, within the same persistence transaction,
  via the outbox pattern to guarantee delivery.

## Data model

Summary: tables `cost_centers` and `cost_center_history`, self-reference FK on
`cost_centers.parent_id`, indexes for hierarchical listing and history.
Details in [`data-model.md`](./data-model.md).

## External contracts

REST API documented in
[`contracts/cost-centers.openapi.yaml`](./contracts/cost-centers.openapi.yaml).
Published events (format in
[`contracts/cost-center-events.md`](./contracts/cost-center-events.md)):
`cost-center.created`, `cost-center.updated`, `cost-center.deactivated`.

## Interface

Not applicable to this spec — purely backend domain. The CRUD will be consumed by
the administrative interface of the `admin-ui` domain (separate spec).

## Testing strategy

- **Unit** — validators (unique code, cycle, validity period) and use cases
  (registration, editing, deactivation, tree listing). Each task that delivers
  testable code includes the tests in the task itself.
- **Integration** — REST endpoints against a real database (sandbox), covering the
  acceptance criteria of the affected flows.
- **E2E** — out of scope for this spec; depends on UI.

## Impact on the authoritative documentation

No impact. In the whole-domain scenario, the
`cost-centers` skill already reflects the expected behavior and the implementation does
what is written. A doc task would only appear with deliberate drift recorded here —
which was not the case. (If the skill diverged from the code without a decision that resolved it, that
would be a gap, not this section.)
```

## data-model.md example

Entities, attributes, relations, indexes, constraints, ENUMs. In frontend,
it describes consumed stores/state shapes.

```markdown
# Data model: Cost Centers

## Entities

### `cost_centers`

| Column            | Type         | Null | Notes                            |
|-------------------|--------------|------|----------------------------------|
| `id`              | uuid         | no   | PK, generated by the application |
| `code`            | varchar(20)  | no   | globally unique                  |
| `name`            | varchar(100) | no   |                                  |
| `accounting_code` | varchar(30)  | no   | indexed, external ERP key        |
| `parent_id`       | uuid         | yes  | FK → `cost_centers.id`           |
| `status`          | enum         | no   | `active` \| `inactive`           |
| `valid_from`      | date         | no   |                                  |
| `valid_until`     | date         | yes  | must be > `valid_from`           |

### `cost_center_history`

| Column           | Type        | Null | Notes                                    |
|------------------|-------------|------|------------------------------------------|
| `id`             | uuid        | no   | PK                                       |
| `cost_center_id` | uuid        | no   | FK → `cost_centers.id`                   |
| `event`          | enum        | no   | `created` \| `updated` \| `deactivated` |
| `payload`        | jsonb       | no   | snapshot of the changed fields           |
| `actor_id`       | uuid        | no   | FK → `users.id`                          |
| `occurred_at`    | timestamptz | no   | default `now()`                          |

## Constraints

- `UNIQUE (cost_centers.code)` — globally unique code.
- `CHECK (valid_until IS NULL OR valid_until > valid_from)`.
- Absence of a cycle in the `parent_id` graph — validated in the application.

## Indexes

- `cost_centers (parent_id)` — for hierarchical listing.
- `cost_centers (status, code)` — for filters + text search.
- `cost_center_history (cost_center_id, occurred_at DESC)` — for history.
```

## research.md example

For each non-trivial technical decision: context, considered alternatives,
the decision made, rationale, consequences. When the decision adopts a
technology, library or dependency, also record the confirmation basis
(provenance) that makes it firm.

```markdown
# Research: Cost Centers

## Input validation library

**Context.** The inputs need to be validated by a declarative schema,
reusable between the API and the application layer.

**Alternatives:**

- **Dedicated schema library** — declarative schemas with type
  inference; adds a dependency.
- **Manual validation** — no new dependency; verbose and prone to drift.

**Decision:** dedicated schema library (the same one already adopted in the project).

**Confirmation basis:** in use in the code — it is already a dependency of other
modules; settled decision, no gap. Without this basis (a "to be confirmed"/"or
similar" reservation or an unbacked choice), the adoption would become a gap.

**Consequences:** the schemas follow the already existing pattern; no new
dependency is introduced.
```

## Contract example (contracts/cost-centers.openapi.yaml)

Formal contracts live in `contracts/`, with the extension reflecting the technology.
OpenAPI snippet for REST:

```yaml
openapi: 3.0.3
info:
  title: Cost Centers API
  version: 1.0.0
paths:
  /cost-centers:
    post:
      summary: Registers a new cost center
      responses:
        "201": { description: Created }
        "409": { description: Code already registered }
        "422": { description: Validation failed }
components:
  schemas:
    CostCenterCreate:
      type: object
      required: [code, name, accounting_code, valid_from]
      properties:
        code:            { type: string, maxLength: 20 }
        name:            { type: string, maxLength: 100 }
        accounting_code: { type: string, maxLength: 30 }
        parent_id:       { type: string, format: uuid }
        valid_from:      { type: string, format: date }
```
