#!/usr/bin/env python3
"""Offline AI-assisted question drafting tool for Naija Trivia Blitz.

Uses the questions already in data/questions/<category>.json as
few-shot style/format examples, and asks Claude to draft NEW candidate
questions in the same schema. Drafts are written to
data/questions/drafts/<category>.json for human review — this script
never writes directly to the live question files that ship in the app.
See docs/QUESTION_GENERATION.md for the full workflow and why that
separation matters.

Setup:
    pip install -r tools/requirements.txt
    export ANTHROPIC_API_KEY=sk-ant-...

Usage:
    python3 tools/generate_questions.py naija_music --count 15
    python3 tools/generate_questions.py --all --count 15   # every category

After reviewing and fact-checking a draft, merge it into the live file:
    python3 tools/merge_draft.py naija_music
"""

import argparse
import json
import os
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
QUESTIONS_DIR = REPO_ROOT / "data" / "questions"
DRAFTS_DIR = QUESTIONS_DIR / "drafts"
CATEGORIES_PATH = REPO_ROOT / "data" / "categories.json"

# Adjust to whatever Claude model you have API access to.
MODEL = "claude-sonnet-5"

# A representative sample is enough to convey style/schema — the prompt
# doesn't need every existing question, and keeping it capped bounds the
# request size as categories grow past a few dozen questions.
MAX_EXAMPLES = 25

PROMPT_TEMPLATE = """You are drafting new trivia questions for a Nigerian-themed mobile trivia \
game, category "{display_name}" ({category_id}).

Below are {example_count} EXISTING questions in this category, as style/format examples. \
Match their tone, length, and JSON schema exactly. Do NOT repeat any of these questions or \
their underlying facts.

EXISTING QUESTIONS:
{examples_json}

Generate {count} NEW questions for this category as a JSON array, following this exact \
schema per question:
{{
  "question": "...",
  "options": ["...", "...", "...", "..."],
  "correct_index": 0,
  "difficulty": "easy" | "medium" | "hard"
}}

Rules:
- Exactly 4 options per question, no duplicates among them.
- Only one option is unambiguously correct.
- Prefer well-established, easily-verifiable facts over obscure or contested claims — \
specific dates, award years, and records are the most common source of trivia errors, so \
only use them when you are highly confident.
- Keep question and option text short enough to read comfortably in a 15-second countdown \
on a phone screen.
- Roughly balance difficulty across the batch: ~40% easy, ~40% medium, ~20% hard.
- Do not duplicate any existing question's text or underlying fact.
- Output ONLY the JSON array. No commentary, no markdown code fence.
"""


def load_categories():
    return json.loads(CATEGORIES_PATH.read_text())


def load_existing_questions(category_id):
    path = QUESTIONS_DIR / f"{category_id}.json"
    if not path.exists():
        return []
    return json.loads(path.read_text()).get("questions", [])


def build_prompt(category_id, display_name, existing, count):
    examples = existing[:MAX_EXAMPLES]
    return PROMPT_TEMPLATE.format(
        display_name=display_name,
        category_id=category_id,
        example_count=len(examples),
        examples_json=json.dumps(examples, indent=2, ensure_ascii=False),
        count=count,
    )


def call_claude(prompt):
    try:
        import anthropic
    except ImportError:
        sys.exit("Missing dependency — run: pip install -r tools/requirements.txt")

    api_key = os.environ.get("ANTHROPIC_API_KEY")
    if not api_key:
        sys.exit("Set ANTHROPIC_API_KEY before running this script.")

    client = anthropic.Anthropic(api_key=api_key)
    response = client.messages.create(
        model=MODEL,
        max_tokens=4096,
        messages=[{"role": "user", "content": prompt}],
    )
    text = response.content[0].text.strip()

    # Models sometimes wrap JSON in a code fence despite instructions not
    # to — strip it defensively rather than failing the whole batch.
    if text.startswith("```"):
        text = text.split("```")[1]
        if text.startswith("json"):
            text = text[4:]
        text = text.strip()

    return json.loads(text)


def validate_draft(questions, existing_texts):
    """Same checks QuestionBank.gd enforces at load time, run here so
    problems surface before a human even starts reviewing — not a
    substitute for reading every question, just a floor."""
    errors = []
    seen_texts = set()
    for i, q in enumerate(questions):
        label = f"question {i}"

        text = q.get("question")
        if not isinstance(text, str) or not text.strip():
            errors.append(f"{label}: missing/empty question text")
            continue
        if text in existing_texts:
            errors.append(f"{label}: duplicates an existing shipped question")
        if text in seen_texts:
            errors.append(f"{label}: duplicate within this draft batch")
        seen_texts.add(text)

        options = q.get("options")
        if not isinstance(options, list) or len(options) != 4:
            errors.append(f"{label}: needs exactly 4 options")
        elif len(set(options)) != 4:
            errors.append(f"{label}: duplicate options")

        correct_index = q.get("correct_index")
        if not isinstance(correct_index, int) or not (0 <= correct_index <= 3):
            errors.append(f"{label}: correct_index must be 0-3")

        if q.get("difficulty") not in ("easy", "medium", "hard"):
            errors.append(f"{label}: invalid difficulty")

    return errors


def generate_for_category(category, count):
    category_id = category["id"]
    display_name = category.get("display_name", category_id)
    existing = load_existing_questions(category_id)
    existing_texts = {q["question"] for q in existing}

    print(f"Requesting {count} draft questions for {display_name}...")
    prompt = build_prompt(category_id, display_name, existing, count)
    draft = call_claude(prompt)

    errors = validate_draft(draft, existing_texts)
    if errors:
        print(f"  {len(errors)} validation issue(s) — also saved into the draft file, fix or remove before merging:")
        for e in errors:
            print(f"    - {e}")

    DRAFTS_DIR.mkdir(parents=True, exist_ok=True)
    draft_path = DRAFTS_DIR / f"{category_id}.json"
    draft_path.write_text(json.dumps({
        "category": category_id,
        "display_name": display_name,
        "_review_status": "UNREVIEWED — fact-check every question before merging",
        "_validation_errors": errors,
        "questions": draft,
    }, indent="\t", ensure_ascii=False) + "\n")
    print(f"  Wrote {len(draft)} draft questions to {draft_path.relative_to(REPO_ROOT)}")


def main():
    parser = argparse.ArgumentParser(
        description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter
    )
    parser.add_argument("category", nargs="?", help="Category id, e.g. naija_music. Omit with --all.")
    parser.add_argument("--all", action="store_true", help="Generate for every category in categories.json")
    parser.add_argument("--count", type=int, default=15, help="Questions to draft per category (default 15)")
    args = parser.parse_args()

    categories = load_categories()
    if args.all:
        targets = categories
    elif args.category:
        targets = [c for c in categories if c["id"] == args.category]
        if not targets:
            sys.exit(f"Unknown category id: {args.category}")
    else:
        parser.error("Provide a category id or --all")

    for category in targets:
        generate_for_category(category, args.count)


if __name__ == "__main__":
    main()
