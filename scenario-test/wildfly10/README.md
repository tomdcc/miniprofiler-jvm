# WildFly 10 Scenario

Runs the EE integration tests against **WildFly 10.1** (Java EE 7, javax namespace).

## Modules under test

- `javax-ee`
- `javax-servlet`
- `hibernate`

## Why WildFly 10

WildFly 10.1.0.Final requires Java 8, matching the compiled bytecode. WildFly 8.1.0.Final
shipped with Java 7 which cannot load Java 8 class files. WildFly 10 still provides the
ExampleDS H2 datasource and supports Java EE 7.
