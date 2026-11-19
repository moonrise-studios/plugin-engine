"""Check release coordinates and confirm their visibility on Maven Central."""

import argparse
import os
from pathlib import Path
import re
import time
from urllib.error import HTTPError
from urllib.request import Request, urlopen
import xml.etree.ElementTree as ET


def publication(path):
    pom = ET.parse(path).getroot()
    ns = {"m": "http://maven.apache.org/POM/4.0.0"}
    return tuple(pom.findtext(f"m:{field}", namespaces=ns) for field in ("groupId", "artifactId", "version"))


def on_central(coordinate, suffix=".pom"):
    group, artifact, version = coordinate
    base = "https://repo.maven.apache.org/maven2"
    url = f"{base}/{group.replace('.', '/')}/{artifact}/{version}/{artifact}-{version}{suffix}"
    # The CDN can retain a 404 from before the publication became visible.
    url = f"{url}?central-check={time.time_ns()}"
    try:
        with urlopen(Request(url, method="HEAD"), timeout=15) as response:
            return response.status == 200
    except HTTPError as error:
        if error.code == 404:
            return False
        raise


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--published", action="store_true")
    parser.add_argument("poms", nargs=4, type=Path)
    args = parser.parse_args()

    coordinates = [publication(path) for path in args.poms]
    versions = {version for _, _, version in coordinates}
    if len(versions) != 1 or not re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+", next(iter(versions)) or ""):
        raise SystemExit(f"Expected one stable version across all four modules, got {versions}")
    version = versions.pop()
    tag = os.environ.get("CENTRAL_RELEASE_TAG", "")
    if tag and tag.removeprefix("v") != version:
        raise SystemExit(f"Release tag {tag} does not match publication version {version}")

    if args.published:
        files = (".pom", ".jar", "-sources.jar", "-javadoc.jar")
        for attempt in range(13):
            missing = [(gav, suffix) for gav in coordinates for suffix in files if not on_central(gav, suffix)]
            if not missing:
                print(f"All Maven Central publications are visible for {version}.")
                return
            if attempt < 12:
                time.sleep(10)
        raise SystemExit(f"Maven Central has yet to expose {missing}")

    existing = [gav for gav in coordinates if on_central(gav)]
    if existing and len(existing) != len(coordinates):
        raise SystemExit(f"Partial release on Maven Central for {version}: {existing}")
    if existing:
        print(f"All four publications already exist on Maven Central for {version}.")
    with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as output:
        print(f"publish={'false' if existing else 'true'}", file=output)


if __name__ == "__main__":
    main()
