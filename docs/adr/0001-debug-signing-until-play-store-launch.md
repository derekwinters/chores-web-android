# Debug-sign all CI builds until Play Store launch is planned

Status: superseded by [ADR-0008](0008-stable-release-signing-keystore.md) (#52). Release APKs are now signed with one stable keystore; PR builds stay debug-signed. In practice no workflow pinned a debug keystore, so each CI runner generated a fresh random one and every release was signed by a different key.

CI builds (PR dev packages, release-candidate packages on Release Please PRs, and final tagged-release packages) are all signed with the Android debug key rather than a real release keystore. A production signing key introduces key-custody and secret-management concerns (where the keystore lives, who can access it, rotation) that have no payoff yet since there's no Play Store listing to install against. We chose to defer real signing to a future issue, triggered when Play Store publishing is actually planned, rather than set it up speculatively now.

## Consequences

- Tagged-release APKs attached to GitHub Releases are debug-signed and cannot be installed as an update over a future Play-Store-signed install — anyone sideloading pre-launch builds will need to uninstall/reinstall once real signing is introduced.
