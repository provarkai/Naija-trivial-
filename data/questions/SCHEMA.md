# Question Bank JSON Schema

Each category lives in its own file under `data/questions/`, named
`<category_id>.json`. `QuestionBank` (the autoload) loads every `*.json`
file in this folder at startup, so adding a new category is just adding a
new file here — no script changes needed.

## File shape

```json
{
  "category": "naija_music",
  "display_name": "Naija Music",
  "questions": [
    {
      "question": "Which artist released 'Essence' in 2021?",
      "options": ["Wizkid", "Burna Boy", "Davido", "Rema"],
      "correct_index": 0,
      "difficulty": "easy"
    }
  ]
}
```

## Field reference

| Field                 | Type            | Notes                                                              |
|-----------------------|-----------------|----------------------------------------------------------------------|
| `category`            | string          | Stable machine id, snake_case. Must match the `id` in `categories.json`. |
| `display_name`        | string          | Human-readable label (redundant with categories.json, kept for readability when editing this file standalone). |
| `questions`            | array of object | See below.                                                         |
| `questions[].question` | string          | The question text.                                                 |
| `questions[].options`  | array[4] string | Exactly 4 answer choices, in the order shown to the player.        |
| `questions[].correct_index` | int (0-3) | Index into `options` of the correct answer.                        |
| `questions[].difficulty` | string        | One of `"easy"`, `"medium"`, `"hard"`. Affects the score multiplier in `GameStateManager`. |

## Validation

`QuestionBank._validate_question()` drops (with a `push_warning`, not a
crash) any question that:
- has empty/missing `question` text,
- doesn't have exactly 4 `options`,
- has a `correct_index` outside `0..3`,

and silently defaults an unknown/missing `difficulty` to `"easy"`.

## Content guidelines

- Target 50-80 questions per category at launch, roughly balanced across
  difficulties (e.g. ~40% easy / 40% medium / 20% hard).
- Keep questions and options short enough to read comfortably in a 15-second
  countdown on a phone screen.
- No duplicate `options` within a single question, and only one option
  should be defensibly correct.
- This is content, not code — edit these JSON files directly; nothing in
  `scripts/` needs to change to add, edit, or remove questions.
