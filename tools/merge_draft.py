#!/usr/bin/env python3
"""Merges a human-reviewed draft into the live question file that ships
in the app.

Reads data/questions/drafts/<category>.json (produced by
generate_questions.py, or hand-written in the same shape) and appends
its questions to data/questions/<category>.json — the file QuestionBank
actually loads at runtime.

This is a deliberately separate, manual step from generation: nothing
from a draft reaches the shipped game until this is run, and this
should only be run after a human has read every question in the draft
and fixed or removed anything wrong. See docs/QUESTION_GENERATION.md.

Usage:
    python3 tools/merge_draft.py naija_music
    python3 tools/merge_draft.py naija_music --dry-run
"""

import argparse
import json
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
QUESTIONS_DIR = REPO_ROOT / "data" / "questions"
DRAFTS_DIR = QUESTIONS_DIR / "drafts"


def validate(questions, existing_texts):
    errors = []
    seen = set()
    for i, q in enumerate(questions):
        preview = q.get("question", "?")[:50]
        label = f"question {i} ({preview!r})"

        options = q.get("options")
        if not isinstance(options, list) or len(options) != 4 or len(set(options)) != 4:
            errors.append(f"{label}: options must be exactly 4 unique strings")

        correct_index = q.get("correct_index")
        if not isinstance(correct_index, int) or not (0 <= correct_index <= 3):
            errors.append(f"{label}: correct_index must be 0-3")

        if q.get("difficulty") not in ("easy", "medium", "hard"):
            errors.append(f"{label}: invalid difficulty")

        text = q.get("question", "").strip()
        if not text:
            errors.append(f"{label}: empty question text")
        elif text in existing_texts:
            errors.append(f"{label}: duplicates an existing shipped question")
        elif text in seen:
            errors.append(f"{label}: duplicate within the draft")
        seen.add(text)

    return errors


def main():
    parser = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter
    )
    parser.add_argument("category", help="Category id, e.g. naija_music")
    parser.add_argument("--dry-run", action="store_true", help="Validate only, don't write")
    args = parser.parse_args()

    draft_path = DRAFTS_DIR / f"{args.category}.json"
    live_path = QUESTIONS_DIR / f"{args.category}.json"

    if not draft_path.exists():
        sys.exit(f"No draft found at {draft_path}")
    if not live_path.exists():
        sys.exit(f"No live question file at {live_path}")

    draft = json.loads(draft_path.read_text())
    live = json.loads(live_path.read_text())

    existing_texts = {q["question"] for q in live["questions"]}
    new_questions = draft["questions"]

    errors = validate(new_questions, existing_texts)
    if errors:
        print(f"{len(errors)} validation error(s) — fix the draft before merging:")
        for e in errors:
            print(f"  - {e}")
        sys.exit(1)

    print(f"{len(new_questions)} questions pass validation.")
    if args.dry_run:
        print("Dry run — nothing written.")
        return

    live["questions"].extend(new_questions)
    live_path.write_text(json.dumps(live, indent="\t", ensure_ascii=False) + "\n")
    print(f"Merged into {live_path.relative_to(REPO_ROOT)} — now {len(live['questions'])} questions.")

    # Consume the draft so it can't be accidentally merged twice.
    draft_path.unlink()
    print(f"Removed {draft_path.relative_to(REPO_ROOT)}")


if __name__ == "__main__":
    main()
