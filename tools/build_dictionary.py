#!/usr/bin/env python3
"""Build the app dictionary from open lexicons plus curated entries."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import urllib.request
from pathlib import Path


XINHUA_URL = "https://raw.githubusercontent.com/pwxcoo/chinese-xinhua/master/data/idiom.json"
THUOCL_URL = "https://raw.githubusercontent.com/thunlp/THUOCL/master/data/THUOCL_chengyu.txt"
COMMON_LIMIT = 4950
HIGH_FREQUENCY_LIMIT = 1000


def read_source(path: str | None, url: str) -> str:
    if path:
        return Path(path).read_text(encoding="utf-8")
    with urllib.request.urlopen(url, timeout=60) as response:
        return response.read().decode("utf-8")


def clean_text(value: str) -> str:
    return re.sub(r"\s+", " ", value or "").strip()


def clean_example(value: str, word: str) -> str:
    text = clean_text(value).replace("～", word).replace("~", word)
    text = text.split("★", 1)[0].strip(" 。；;")
    if not text:
        return f'“{word}”常用于概括具有上述特点的情形。'
    return text + ("" if text[-1] in "。！？" else "。")


def stable_id(item: dict) -> str:
    abbreviation = re.sub(r"[^a-z0-9]", "", item.get("abbreviation", "").lower()) or "idiom"
    digest = hashlib.sha1(item["word"].encode("utf-8")).hexdigest()[:8]
    return f"cy-{abbreviation}-{digest}"


def unique(values: list[str]) -> list[str]:
    return list(dict.fromkeys(value for value in values if value))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--xinhua")
    parser.add_argument("--thuocl")
    parser.add_argument("--base", required=True)
    parser.add_argument("--notes", required=True)
    parser.add_argument("--output", required=True)
    parser.add_argument("--remote-output", required=True)
    args = parser.parse_args()

    xinhua_rows = json.loads(read_source(args.xinhua, XINHUA_URL))
    xinhua = {clean_text(row.get("word", "")): row for row in xinhua_rows if row.get("word")}

    ranked_words: list[str] = []
    for line in read_source(args.thuocl, THUOCL_URL).splitlines():
        word = line.split()[0].strip() if line.split() else ""
        if word and word in xinhua and word not in ranked_words:
            ranked_words.append(word)
        if len(ranked_words) >= COMMON_LIMIT:
            break

    base_rows = json.loads(Path(args.base).read_text(encoding="utf-8"))
    base = {row["text"]: row for row in base_rows}
    notes_rows = json.loads(Path(args.notes).read_text(encoding="utf-8"))
    notes: dict[str, list[dict]] = {}
    for row in notes_rows:
        notes.setdefault(row["word"], []).append(row)

    ordered_words = unique(ranked_words + list(notes) + list(base))
    result: list[dict] = []
    missing: list[str] = []

    for rank, word in enumerate(ordered_words, start=1):
        source = xinhua.get(word)
        custom = base.get(word)
        manual = next((row for row in notes.get(word, []) if row.get("meaning")), None)
        if custom:
            entry = dict(custom)
        elif source:
            meaning = clean_text(source.get("explanation", ""))
            if not meaning:
                missing.append(word)
                continue
            entry = {
                "id": stable_id(source),
                "text": word,
                "pinyin": clean_text(source.get("pinyin", "")),
                "tone": "unmarked",
                "meaning": meaning,
                "note": "",
                "example": clean_example(source.get("example", ""), word),
                "contextExample": "",
                "tags": [],
            }
        elif manual:
            entry = {
                "id": stable_id({"word": word, "abbreviation": ""}),
                "text": word,
                "pinyin": manual["pinyin"],
                "tone": "unmarked",
                "meaning": manual["meaning"],
                "note": "",
                "example": manual["example"],
                "contextExample": "",
                "tags": [],
            }
        else:
            missing.append(word)
            continue

        tags = list(entry.get("tags", []))
        if word in ranked_words:
            tags.append("常用成语")
            if ranked_words.index(word) < HIGH_FREQUENCY_LIMIT:
                tags.append("高频常用")
        if word in notes:
            tags.append("易错成语")
            tags.extend(row["category"] for row in notes[word])
            entry["note"] = " ".join(row["note"] for row in notes[word])
        entry["tags"] = unique(tags)
        result.append(entry)

    Path(args.output).write_text(
        json.dumps(result, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    Path(args.remote_output).write_text(
        json.dumps(result, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"generated={len(result)} common={sum('常用成语' in r['tags'] for r in result)} "
          f"error_prone={sum('易错成语' in r['tags'] for r in result)}")
    if missing:
        print("missing=" + ",".join(missing))


if __name__ == "__main__":
    main()
