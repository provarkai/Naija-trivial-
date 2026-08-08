# AI-Assisted Question Generation

An offline content pipeline that uses the existing question bank as
few-shot examples to draft new questions in the same style/schema —
**not** a runtime feature of the game. Nothing about the app changes:
`QuestionBank` still just loads static JSON from `data/questions/`, the
same as always. This tooling only speeds up *writing* that JSON.

## Why offline, not in-app

This was a deliberate choice over having the app itself call an LLM at
runtime:

- **Security** — an API key embedded in a shipped APK can be extracted
  and abused by anyone. A key only ever needs to live on a developer's
  machine running this script, never on a player's device.
- **Offline play** — the brief's own retention design targets 60-90
  second sessions "waiting in traffic" — exactly when a phone's
  connection is weakest. A question bank that's just local JSON keeps
  working regardless; a runtime API dependency wouldn't.
- **Accuracy** — this is a trivia game; a wrong "correct answer" is a
  real bug, not a minor quality issue. An LLM drafting questions still
  hallucinates, especially on specific dates/awards/records — see the
  Yamile Aldama mistake caught and fixed while expanding `sports.json`
  by hand. Putting unreviewed model output directly in front of players
  removes the one thing that's caught errors so far: a human reading
  every question before it ships.

## Workflow

1. **Draft.**
   ```
   pip install -r tools/requirements.txt
   export ANTHROPIC_API_KEY=sk-ant-...
   python3 tools/generate_questions.py naija_music --count 15
   ```
   This reads the category's existing questions from
   `data/questions/naija_music.json` as style/format examples, asks
   Claude for `count` new ones in the same schema, runs them through the
   same structural checks `QuestionBank.gd` and `merge_draft.py` enforce
   (4 options, valid `correct_index`, valid `difficulty`, no duplicate
   question text), and writes the result to
   `data/questions/drafts/naija_music.json` — **never** to the live file.
   Use `--all` to draft every category in one pass.

2. **Review.** Open the draft file and read every question. Structural
   validation (already run automatically) catches malformed JSON; it
   cannot catch a wrong fact. This is the step that actually matters —
   check anything with a specific date, award, record, or attribution
   against what you actually know, and delete or fix anything you're not
   confident about. Delete the `_review_status`/`_validation_errors`
   fields once you're done — they're notes for this step, not part of
   the shipped schema (`merge_draft.py` reads `questions` regardless, so
   leaving them in is harmless if you forget, but they don't belong in
   `data/questions/<category>.json`, which `merge_draft.py` also won't
   copy in).

3. **Merge.**
   ```
   python3 tools/merge_draft.py naija_music
   ```
   Re-validates against the *live* file (catches anything that became a
   duplicate since drafting) and appends the reviewed questions to
   `data/questions/naija_music.json`, then deletes the draft so it can't
   be merged twice. Add `--dry-run` to check without writing.

4. **Commit** `data/questions/<category>.json` like any other content
   change. Drafts themselves are gitignored — they're working files, not
   something to track through the review process in git history.

## Alternative: just ask Claude Code directly

If you're already in a Claude Code session (like the one that built this
project), you don't need the script or an extra API key at all — ask
directly, e.g. "draft 15 more naija_music questions, same style as the
existing ones, and write them to `data/questions/drafts/naija_music.json`
for me to review." That's the same workflow this tooling formalizes,
done conversationally instead of via a standalone script. The review and
merge steps above are unchanged either way — nothing skips the human
fact-check regardless of which "AI" drafted the batch.

## Tuning generation quality

- `PROMPT_TEMPLATE` in `tools/generate_questions.py` is the whole prompt
  — edit it directly to change tone, difficulty balance, or add
  category-specific guidance (e.g. steering `pidgin_proverbs` toward
  vocabulary vs. proverbs).
- `MAX_EXAMPLES` caps how many existing questions get sent as few-shot
  examples per request. 25 is enough to convey style without the prompt
  growing unbounded as a category's question count grows.
- `MODEL` in the same file — set to whatever Claude model you have API
  access to.
