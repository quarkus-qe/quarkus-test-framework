# Working Preferences

This file captures how we work on the Quarkus QE Test Framework — coding standards, communication style, and AI interaction guidelines.

> **Note:** This is a starting template. Update it as preferences become clearer.

## Coding standards

- **Language:** Java (JDK 17+), Maven build
- **Style:** Checkstyle is enforced — configuration in [`checkstyle.xml`](../../checkstyle.xml) with suppressions in [`checkstyle-suppressions.xml`](../../checkstyle-suppressions.xml)
- **Change scope:** Prefer minimal, targeted changes — only modify what is necessary to solve the problem at hand
- **Avoid:** Unnecessary refactors, added abstractions, or cleanup of unrelated surrounding code

## Communication preferences

- Terse and technical — skip preamble, get to the point
- No filler phrases ("Great!", "Certainly!", etc.)
- Use code and concrete examples over prose explanations where possible

## How to work with AI

- Read relevant files before making claims or suggestions about the code
- Make the minimal change that solves the problem — do not gold-plate
- Always run relevant validation (build, checkstyle, tests) before declaring work done
- Consult [`_context/wiki/index.md`](index.md) at the start of each task to determine if project context is needed
- After completing a task that yields durable knowledge, offer to update this wiki (wait for approval before writing)

## Areas of focus

- Framework reliability is the current top priority — changes that improve stability are welcome
- Be cautious with changes to platform modules (`quarkus-test-openshift`, `quarkus-test-kubernetes`) as they affect cloud deployment behaviour
