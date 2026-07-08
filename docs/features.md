# Apple Reminders feature map

Checklist of everything Apple Reminders does, with a proposed priority.
`[MVP]` = needed for the first usable version, `[v2]` = addable later,
`[skip]` = deliberately excluded with a reason.

## Organization

- [MVP] Lists — base container for tasks
- [MVP] Sub-tasks (nested under a reminder)
- [v2] Folders — group multiple lists (only useful once lists become numerous)
- [v2] List groups in the sidebar — Apple keeps them distinct from folders, this
  separation is copied for now; if they turn out redundant in practice, consider
  merging them later
- [MVP] Tags — cross-list, to find tasks without depending on which list they're in
- [MVP] Priority (low/medium/high)
- [MVP] Flag — an importance axis separate from priority (e.g. priority = urgency,
  flag = "look here"), kept as a distinct feature
- [MVP] Default smart lists: Today, Scheduled, Flagged, All, Completed
- [v2] Custom smart lists (combined filters: tag + date + priority + location)
- [v2] Sections within a list (To Do / In Progress / Done style columns) — useful
  for projects, less so for daily reminders; verify if actually needed
- [v2] Column (kanban) view for sections

## Dates and recurrence

- [MVP] Due date/time + notification
- [MVP] Notification snooze — quick delay (e.g. 1h) and "smart" delay
  (context-aware: a morning reminder snoozed goes to afternoon, an afternoon
  one to evening, mirroring iOS's smart snooze)
- [MVP] Recurrence (daily/weekly/monthly/custom) — rescheduling a single
  occurrence (changing its date/time) only moves that occurrence, it does not
  shift the recurrence rule. E.g. a "every Thursday 10am" reminder rescheduled
  to Friday still fires the following Thursday at 10am, not Friday (behavior
  copied from Google Tasks)
- [MVP] Location-based reminders (arriving/leaving a place — requires always-on GPS
  permissions, battery usage to monitor, needs careful handling)
- [skip] Contact-based reminders (triggers when messaging a person) — niche feature,
  deep integration with iOS's messaging system that has no clean direct equivalent
  on Android

## Content

- [MVP] Text notes/description on the task
- [v2] Attachments (photos, document scans, links)
- [skip] Notes app integration — there's no dedicated Notes app in this project, it
  would be disconnected from everything; reconsider only if that app gets built too

## Collaboration

- [v2] Self-hosted webapp sync — a self-hostable webapp companion, with a
  per-device setting to use either local-only storage or sync with the
  user's own self-hosted server. Single-user (this device ↔ the user's own
  server), not multi-user sharing — see the skip below for that
- [skip] Multi-user shared lists with realtime updates — different problem
  from self-hosted sync above (this is about other people accessing the same
  list, not one user's own devices). Reconsider if it becomes needed later
- [skip] Assigning tasks to another user ("Assigned to Me") — depends on the point
  above, same reason

## Data and backup

- [MVP] Local export/backup (e.g. JSON file on phone storage) — **caution**: with
  local-only storage, losing/breaking the phone means losing all reminders. Without
  cloud sync there needs to be at least a manual backup/restore path, otherwise
  there's a real risk of data loss that Apple Reminders (with iCloud) doesn't have
- [v2] Import from other apps (CSV/JSON) — useful if migrating from TickTick/other
  someday

## AI / smart

- [MVP] Natural language in the title → date/time/recurrence (already in
  requirements.md)
- [MVP] Voice → structured task via Gemini API (already decided)
- [v2] ML auto-categorization (automatically puts a task in "Work"/"Personal"/
  "Groceries" based on keywords) — nice but not essential, and with NLU already
  going through Gemini this could come almost free by also asking for the category
  in the same prompt instead of building a separate classifier
- [v2] Grocery lists with automatic grouping by department (dairy, produce...) —
  same reasoning: with Gemini already in the loop, ask it in the prompt instead of
  a dedicated system

## Other

- [MVP] Full-text search across all reminders
- [MVP] Home screen widget (today's list)
- [skip] Siri / external assistant — decided in requirements.md: no reliable system
  assistant, in-app microphone is used instead

## Decided in this iteration

- Sync: local storage only for v1, no backend (see "Data and backup" for the data
  loss risk this implies); self-hosted webapp sync with a local/server toggle
  planned for v2 (see "Collaboration")
- Notification snooze: smart (context-aware, iOS-style) snooze promoted to MVP,
  alongside a plain fixed-delay snooze
- Flag: kept as a separate MVP feature from Priority
- Location-based reminders: promoted to MVP
- Folders and List groups: both copied as Apple does, customize later if they turn
  out redundant in practice
