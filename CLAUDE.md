# Kengine Development Guidelines

## Build Commands
- Build project: `./gradlew clean build`
- Run all tests: `./gradlew allTests`
- Run native tests: `./gradlew nativeTest`
- Run single test: `./gradlew nativeTest --tests "com.kengine.test.YourTestClass.yourTestMethod"`

## Code Style Guidelines
- **Naming**: Use camelCase for variables/functions, PascalCase for classes, UPPER_SNAKE_CASE for constants
- **Organization**: Group related functionality in packages (entity, graphics, input, sound)
- **Error Handling**: Use logger for reporting errors, return null or default values when appropriate
- **Documentation**: Document public APIs with KDoc comments
- **Imports**: Avoid wildcard imports, organize imports by stdlib first, then third-party
- **Functional Style**: Prefer functional hooks (useContext, useState) over inheritance where appropriate
- **Nullability**: Favor non-nullable types, use safe call operators (`?.`) when needed
- **Testing**: All new functionality should have corresponding tests

## Code Comments
- Comments should explain WHY code exists, not WHAT it does (the code itself should be clear)
- Never add comments describing changes made to the code (use git history for that)
- Configuration files should not include change tracking comments
- Do not add trivial comments that repeat what the code already clearly states
- Write documentation comments (KDoc) for public APIs, focusing on usage, not implementation details