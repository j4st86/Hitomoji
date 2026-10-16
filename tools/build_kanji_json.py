#!/usr/bin/env python3
"""Build assets/kanji.json from KANJIDIC2 and the handwritten sentence table."""

import gzip
import json
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(Path(__file__).resolve().parent))
from sentences import SENTENCES  # noqa: E402

KANA = set(
    "ぁあぃいぅうぇえぉおかがきぎくぐけげこごさざしじすずせぜそぞただちぢっつづてでとどなにぬねのはばぱひびぴふぶぷへべぺほぼぽまみむめもゃやゅゆょよらりるれろわをんー"
    "ァアィイゥウェエォオカガキギクグケゲコゴサザシジスズセゼソゾタダチヂッツヅテデトドナニヌネノハバパヒビピフブプヘベペホボポマミムメモャヤュユョヨラリルレロワヲンヴー"
    "、。・「」『』（）！？…ー　 "
)


def load_kanjidic(path: Path):
    with gzip.open(path, "rb") as handle:
        root = ET.parse(handle).getroot()
    rows = {}
    for character in root.findall("character"):
        literal = character.findtext("literal")
        misc = character.find("misc")
        freq = misc.findtext("freq") if misc is not None else None
        if not freq:
            continue
        rank = int(freq)
        if rank < 1 or rank > 500:
            continue
        strokes = int(misc.findtext("stroke_count"))
        on_readings = []
        kun_readings = []
        group = character.find("./reading_meaning/rmgroup")
        if group is not None:
            for reading in group.findall("reading"):
                kind = reading.get("r_type")
                if kind == "ja_on":
                    on_readings.append(reading.text)
                elif kind == "ja_kun":
                    kun_readings.append(reading.text)
        rows[literal] = {
            "rank": rank,
            "character": literal,
            "onReadings": on_readings,
            "kunReadings": kun_readings,
            "strokeCount": strokes,
        }
    return rows


def main():
    source = load_kanjidic(ROOT / "tools" / "kanjidic2.xml.gz")
    missing = [char for char in source if char not in SENTENCES]
    extra = [char for char in SENTENCES if char not in source]
    if missing or extra:
        raise SystemExit(f"missing={missing} extra={extra}")
    records = []
    errors = []
    for char, fields in source.items():
        meanings, sentence_jp, sentence_kana, sentence_ru, review = SENTENCES[char]
        if not 1 <= len(meanings) <= 3:
            raise SystemExit(f"{char}: meanings {meanings}")
        if any(not item.strip() for item in meanings):
            raise SystemExit(f"{char}: empty meaning")
        if char not in sentence_jp:
            errors.append(f"{char}: sentence does not contain the kanji: {sentence_jp}")
        if not sentence_jp.strip() or not sentence_kana.strip() or not sentence_ru.strip():
            errors.append(f"{char}: empty sentence field")
        bad = [ch for ch in sentence_kana if ch not in KANA]
        if bad:
            errors.append(f"{char}: kana has non-kana {bad}: {sentence_kana}")
        if len(sentence_jp) > 40:
            errors.append(f"{char}: sentence is {len(sentence_jp)} chars: {sentence_jp}")
        record = {
            "rank": fields["rank"],
            "character": char,
            "meaningsRu": meanings,
            "onReadings": fields["onReadings"],
            "kunReadings": fields["kunReadings"],
            "strokeCount": fields["strokeCount"],
            "sentenceJp": sentence_jp,
            "sentenceKana": sentence_kana,
            "sentenceRu": sentence_ru,
        }
        if review:
            record["reviewNeeded"] = True
        records.append(record)
    if errors:
        raise SystemExit("\n".join(errors))
    records.sort(key=lambda item: item["rank"])
    ranks = [item["rank"] for item in records]
    if ranks != list(range(1, 501)):
        raise SystemExit(f"rank gaps {ranks[:5]}..")
    if len({item["character"] for item in records}) != 500:
        raise SystemExit("duplicate characters")
    out = ROOT / "app" / "src" / "main" / "assets" / "kanji.json"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(records, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    flagged = sum(1 for item in records if item.get("reviewNeeded"))
    print(f"wrote {out} ({len(records)} kanji, reviewNeeded={flagged})")


if __name__ == "__main__":
    main()
