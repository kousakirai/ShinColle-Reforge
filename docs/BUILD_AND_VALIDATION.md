# Build and Validation

## Build commands

Prefer the repository Gradle Wrapper.

Windows:

```powershell
.\gradlew.bat compileJava
```

Linux/macOS:

```bash
./gradlew compileJava
```

Full build when appropriate:

```powershell
.\gradlew.bat build
```

or:

```bash
./gradlew build
```

## Existing test infrastructure

Current source includes Forge GameTests:

- `ShinColleEntityRegistryGameTests`
- `ShinColleShipStateGameTests`
- `ShinColleCombatGameTests`

and `ShinColleGameTestRegistration`.

Use them when the behavior is deterministic enough to test.

## Validation ladder

1. compile affected Java
2. run relevant deterministic tests
3. run broader build
4. run GameTests where relevant
5. perform manual/in-game parity scenario

## Compiler failures

Classify failures instead of hiding them:

- caused by this change
- pre-existing
- dependency/network
- Java/toolchain
- environment
- test-only
- unrelated

## UX caveat

A successful `compileJava` proves only that the Java code compiled.

It does not prove:

- Goal scheduling parity
- movement feel
- attack timing parity
- GUI layout parity
- audiovisual parity
- multiplayer sync feel

State clearly what each check proves and does not prove.
