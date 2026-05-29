#!/usr/bin/env python3

from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path
from typing import Any
from urllib import error, request


REPO_ROOT = Path(__file__).resolve().parents[2]
CONFIG_PATH = REPO_ROOT / ".moonrise" / "changelog.config.json"


def load_json(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def set_output(name: str, value: str) -> None:
    output_path = os.environ.get("GITHUB_OUTPUT")
    if not output_path:
        return

    with Path(output_path).open("a", encoding="utf-8") as handle:
        handle.write(f"{name}={value}\n")


def require_string(payload: dict[str, Any], key: str) -> str:
    value = payload.get(key)
    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"Field '{key}' must be non-empty string.")
    return value.strip()


def require_body(payload: dict[str, Any]) -> str:
    value = payload.get("body")
    if not isinstance(value, str) or not value.strip():
        raise ValueError("Field 'body' must be a non-empty string.")
    return value.strip()


def load_payload() -> tuple[dict[str, Any], dict[str, Any], Path]:
    config = load_json(CONFIG_PATH)
    output_path = REPO_ROOT / str(config.get("outputPath", ".moonrise/changelog/latest.json"))
    payload = load_json(output_path)
    return config, payload, output_path


def validate_ready_payload(config: dict[str, Any], payload: dict[str, Any]) -> None:
    if payload.get("productSlug") != config.get("productSlug"):
        raise ValueError("Tracked changelog productSlug does not match config.")

    require_string(payload, "productName")
    require_string(payload, "version")
    require_string(payload, "title")
    require_string(payload, "summary")
    require_body(payload)

    source = payload.get("source")
    if not isinstance(source, dict):
        raise ValueError("Field 'source' must be object.")

    for key in ("repository", "branch", "commit", "compare"):
        require_string(source, key)


def command_inspect(_: argparse.Namespace) -> int:
    config, payload, output_path = load_payload()
    ready = bool(payload.get("ready"))
    publish_enabled = bool(config.get("publish", True))
    should_publish = ready and publish_enabled

    if should_publish:
        validate_ready_payload(config, payload)

    set_output("ready", "true" if ready else "false")
    set_output("publish", "true" if should_publish else "false")
    set_output("output_path", str(output_path.relative_to(REPO_ROOT)))
    set_output("product_slug", str(config.get("productSlug", "")))
    return 0


def command_publish(_: argparse.Namespace) -> int:
    config, payload, _ = load_payload()
    if not bool(config.get("publish", True)):
        print("Config publish disabled.", file=sys.stderr)
        return 1
    if not bool(payload.get("ready")):
        print("Tracked changelog not ready.", file=sys.stderr)
        return 1

    validate_ready_payload(config, payload)

    api_base_url = os.environ.get("MOONRISE_API_BASE_URL", "").rstrip("/")
    token = os.environ.get("MOONRISE_INTEGRATION_TOKEN", "").strip()

    if not api_base_url:
        raise RuntimeError("MOONRISE_API_BASE_URL is required.")
    if not token:
        raise RuntimeError("MOONRISE_INTEGRATION_TOKEN is required.")

    request_payload = {
        "version": payload["version"],
        "title": payload["title"],
        "summary": payload["summary"],
        "body": payload["body"],
        "publish": True,
    }

    publish_request = request.Request(
        url=f"{api_base_url}/api/external/products/{config['productSlug']}/changelog",
        data=json.dumps(request_payload).encode("utf-8"),
        headers={
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json",
        },
        method="POST",
    )

    try:
        with request.urlopen(publish_request) as response:
            print(response.read().decode("utf-8").strip())
    except error.HTTPError as exc:
        body = exc.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"Moonrise API publish failed: {exc.code} {body}") from exc

    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Validate and publish tracked Moonrise changelog payload.")
    subparsers = parser.add_subparsers(dest="command", required=True)

    inspect_parser = subparsers.add_parser("inspect")
    inspect_parser.set_defaults(func=command_inspect)

    publish_parser = subparsers.add_parser("publish")
    publish_parser.set_defaults(func=command_publish)
    return parser


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()

    try:
        return args.func(args)
    except Exception as exc:  # noqa: BLE001
        print(str(exc), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
