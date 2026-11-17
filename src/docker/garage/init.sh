#!/bin/sh
# 로컬 Garage 초기 설정: 노드 배치 · 접근 키 · 버킷. 여러 번 실행해도 된다.
set -eu
export MSYS_NO_PATHCONV=1

cd "$(dirname "$0")/../.."

BUCKET=bakku-local
KEY_ID=GK62616b6b752d6c6f63616c31
SECRET_KEY=62616b6b752d6c6f63616c2d6f6e6c792d7365637265742d6b65792d30303031

garage() {
    docker compose exec -T garage /garage "$@"
}

if garage layout show | grep -q "version: 0"; then
    NODE_ID=$(garage node id -q | cut -d@ -f1)
    garage layout assign -z local -c 1G "$NODE_ID"
    garage layout apply --version 1
fi

if ! garage key info "$KEY_ID" > /dev/null 2>&1; then
    garage key import --yes -n bakku-local "$KEY_ID" "$SECRET_KEY"
fi

if ! garage bucket info "$BUCKET" > /dev/null 2>&1; then
    garage bucket create "$BUCKET"
fi

garage bucket allow --read --write --owner "$BUCKET" --key "$KEY_ID"

echo "Garage 준비 완료: http://localhost:3900 bucket=$BUCKET"
