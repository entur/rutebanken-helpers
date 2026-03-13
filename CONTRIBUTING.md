# Contributing to Rutebanken-helpers

Thank you for considering contributing! Here is everything you need to get started.

## Prerequisites

- Java 17+
- Maven 3.8+

## Building locally

```bash
mvn package
```

This compiles, runs tests, and checks code formatting (Prettier).

## Code style

The project enforces code formatting via [Prettier for Java](https://github.com/jhipster/prettier-java). Before opening a PR, format your code:

```bash
mvn prettier:write
```

The CI build will fail if formatting is not applied (`-PprettierCheck`).

## Making changes

1. Fork the repository and create a branch from `master`.
2. Make your changes in the relevant module(s).
3. Add or update tests to cover your change.
4. Run `mvn package` locally to verify everything passes.
5. Open a pull request against `master`.

## Pull requests

- Keep PRs focused — one concern per PR.
- Describe what the change does and why in the PR description.
- CI runs automatically on every PR (build + Sonar scan).
- A maintainer will review and merge once CI is green.

## Releases

Releases are published to [Maven Central](https://central.sonatype.com/artifact/org.entur.ror.helpers/helper) automatically when a commit lands on `master`. The version is bumped automatically (minor increment) by the release workflow — you do not need to update the version manually.

## Reporting issues

Please use the [GitHub issue tracker](https://github.com/entur/rutebanken-helpers/issues).

## License

By contributing you agree that your changes will be licensed under the [EUPL v1.2](https://joinup.ec.europa.eu/software/page/eupl).