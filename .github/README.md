# cloud-minestom

A minestom implementation of the [Cloud Command Framework](https://github.com/Incendo/cloud).

## Usage

### Gradle
```
repositories {
    maven { url "https://repo.jorisg.com/snapshots" }
}

dependencies {
    implementation 'com.guflimc.cloud:cloud-minestom:+'
}
```

### Examples

Creating the command manager. You can also use the `AsyncCommandExecutionCoordinator`.
And custom command sender mapper functions.
```java
MinestomCommandManager<CommandSender> commandManager = new MinestomCommandManager<>(
        CommandExecutionCoordinator.simpleCoordinator(),
        Function.identity(),
        Function.identity()
);
```

