#!/usr/bin/env python3
"""Flyway 마이그레이션 버전 충돌/역행 검사.

사용법: check_flyway_versions.py <base-ref>
  base-ref: PR 타겟 브랜치(예: origin/develop) - "이미 머지돼 배포된 상태"로 간주한다.

검사 2가지:
  1. 중복 - 현재(HEAD) 트리 전체에 같은 버전 번호가 2개 이상 있으면 실패.
     (여러 모듈이 db/migration/<module>/ 아래에 독립적으로 파일을 두므로,
      깃 레벨 merge conflict로는 안 걸러진다 - 그래서 이 스크립트가 필요함)
  2. 역행 - 이번 PR이 "새로" 추가한 마이그레이션의 버전이, base-ref에 이미
     있던 마이그레이션 중 최댓값보다 작거나 같으면 실패. (Flyway가 나중에
     "Detected resolved migration not applied to database"로 거부하는 것과
     동일한 조건을 머지 전에 미리 잡는다)
"""
import re
import subprocess
import sys

VERSION_RE = re.compile(r"db/migration/.*/V([0-9_]+)__")


def versions_in(ref: str) -> dict[str, str]:
    """ref(커밋/브랜치)의 전체 트리에서 '파일경로 -> 버전문자열' 맵을 만든다."""
    out = subprocess.run(
        ["git", "ls-tree", "-r", "--name-only", ref],
        capture_output=True, text=True, check=True,
    ).stdout.splitlines()
    result = {}
    for path in out:
        m = VERSION_RE.search(path)
        if m:
            result[path] = m.group(1).replace("_", ".")
    return result


def version_key(v: str) -> tuple[int, ...]:
    """'20261120.1201' -> (20261120, 1201) - Flyway와 동일한 점 구분 숫자 비교."""
    return tuple(int(p) for p in v.split("."))


def main() -> int:
    if len(sys.argv) != 2:
        print("usage: check_flyway_versions.py <base-ref>", file=sys.stderr)
        return 2
    base_ref = sys.argv[1]

    base_versions = versions_in(base_ref)
    head_versions = versions_in("HEAD")

    base_max = max((version_key(v) for v in base_versions.values()), default=(0,))
    new_files = {p: v for p, v in head_versions.items() if p not in base_versions}

    errors: list[str] = []

    # 1. 중복 검사 (HEAD 전체 기준)
    seen: dict[str, str] = {}
    for path, v in head_versions.items():
        if v in seen and seen[v] != path:
            errors.append(f"버전 중복: {v}\n    - {seen[v]}\n    - {path}")
        else:
            seen[v] = path

    # 2. 역행 검사 (이번 PR이 새로 추가한 파일만 대상)
    for path, v in new_files.items():
        if version_key(v) <= base_max:
            errors.append(
                f"버전 역행: {path} (버전 {v})이 {base_ref}에 이미 있던 "
                f"최신 버전({'.'.join(map(str, base_max))})보다 작거나 같습니다. "
                f"머지 직전 기준으로 버전 번호를 다시 매겨주세요."
            )

    if errors:
        print("::error::Flyway 마이그레이션 버전 문제 발견:\n")
        for e in errors:
            print(f"- {e}\n")
        return 1

    print(f"OK: 총 {len(head_versions)}개 마이그레이션 검증 완료 (이번 PR 신규 {len(new_files)}개)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())