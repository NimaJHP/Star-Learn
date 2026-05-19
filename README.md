# Star Learn (Android)

A dark neon-blue themed Android app with:

- **Version 1 (flavor `v1`)**
  - English-to-English dictionary lookup (Cambridge Dictionary webpage)
  - Save word + definition as flashcard
  - Flashcard list (word only), open card for word + definition, optional delete

- **Version 2 (flavor `v2`)**
  - Everything from v1
  - Export flashcards as **TSV** file for import into apps like **AnkiDroid**

## Build APKs

From project root:

```bash
./gradlew assembleV1Debug
./gradlew assembleV2Debug
```

Expected outputs:

- `app/build/outputs/apk/v1/debug/app-v1-debug.apk`
- `app/build/outputs/apk/v2/debug/app-v2-debug.apk`

## Notes

- Dictionary lookups require internet access.
- Exported file is saved in device **Downloads** as `starlearn_flashcards.tsv`.
- TSV format is `word<TAB>definition` (one card per line), compatible with AnkiDroid import mapping.
