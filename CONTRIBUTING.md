# Contributing to SimpMusic

Thanks for wanting to help! The short version:

1. **Start from an issue.** Every PR needs an accepted issue behind it — open one first
   (or pick an existing one) so the change is agreed before the code exists. PRs with no
   linked issue from new contributors get flagged by the triage bot.
2. **Fork and branch from `dev`.** `dev` is the default and integration branch; `main`
   tracks releases.
3. **Follow the code around you.** Kotlin official conventions, Compose single-source-of-truth,
   Clean Architecture layer rules — match what the neighbouring files already do.
4. **Fill in the PR template.** All of it, including the checkboxes — one of them is
   machine-checked.
5. **Translations** go through [Crowdin](https://crowdin.com/project/simpmusic), not PRs
   that edit the string files directly.

## AI policy

AI-assisted contributions are welcome. AI-*driven* ones are not:

- A human must have **written or personally reviewed every line** of the PR and be able
  to answer review comments about it. "The agent wrote it, I skimmed it" does not count.
- **Unattended agent submissions** — PRs fired at this repository by coding agents
  (Jules, Devin, OpenHands, and friends) without a human shaping and checking the
  result — are **closed automatically** by the triage workflow, on sight, without
  individual discussion. Their fingerprints (agent names, session ids, or bare commit
  hashes in the title) are matched by `.github/workflows/pr-triage.yml`.
- **Commits carrying AI co-author trailers** (`Co-Authored-By: Claude/Copilot/…`) or
  "Generated with …" markers are rejected the same way — a leftover trailer is the tell
  that nobody proofread the output. Squash them out before opening the PR.
- Repeat offenders get blocked from the repository.

This is not hostility toward AI tooling — half this project is built with it. It is the
difference between a contribution someone stands behind and unreviewed output pointed at
volunteer maintainers. Review time is the scarcest resource this project has; spending it
on machine-generated PRs nobody proofread takes it away from contributors who did the work.

## Code of conduct

See [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).
