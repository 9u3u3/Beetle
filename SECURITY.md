# Security policy

## Supported versions

Beetle is in alpha. We fix security problems only in the latest release and on the `main` branch.

## Report a vulnerability

Do not open a public issue for a security problem.

Report it privately through GitHub:

1. Go to the [Security tab](https://github.com/9u3u3/bananaSpaghetti/security) of this repository.
2. Click **Report a vulnerability**.
3. Describe the problem, the steps that show it, and the effect.

We try to reply within 7 days. We tell you when we fix the problem, and we credit you in the release notes if you want.

## Areas of special interest

- Storage of API keys, SSH keys, and Git credentials
- The local servers on `localhost` ports 8080, 8081–8082, and 5500
- The JavaScript bridges between the WebViews and Kotlin
- The `proot` environment and its file system bindings
