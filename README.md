# Kotlin client: an inline number enum is serialized as a string

With `serializationLibrary=kotlinx_serialization`, a number enum declared inline on a property (`plays: { type: integer, enum: [1, 2] }`) becomes a nested enum class with `@SerialName("1")` and no value serializer. kotlinx.serialization then writes it as the string `"1"` and can't read the number `1` the API sends. The same enum declared as its own schema (`Speed`) works, since #21746 gave `enum_class.mustache` a value serializer for every non-string enum; `data_class.mustache`, which renders inline enums, only adds one when `enumUnknownDefaultCase` is on.

## Reproduction

Needs Node and a JDK 17 or newer.

```sh
npm install
npm run repro
```

Output with openapi-generator 7.26.0:

```
encoded: {"plays":"1","speed":1}
decoded: JsonDecodingException: Unexpected JSON token at offset 9: Expected quotation mark '"', but had '1' instead at path: $.plays
```

`npm run repro` generates the client from [openapi.yaml](openapi.yaml) (`-g kotlin`, `serializationLibrary=kotlinx_serialization`), then compiles only the generated models with [Main.kt](src/main/kotlin/Main.kt), which encodes a `Recording` and decodes `{"plays":1,"speed":1}`.

## Expected

```
encoded: {"plays":1,"speed":1}
decoded: Recording(plays=_1, speed=_1)
```

## Actual

`plays` is written as `"1"` and `1` can't be read. The generated `Recording.Plays` is `@Serializable enum class Plays(val value: kotlin.Int)` with `@SerialName(value = "1") _1(1)`; `Speed` gets `@Serializable(with = SpeedSerializer::class)`.

In [`kotlin-client/data_class.mustache`](https://github.com/OpenAPITools/openapi-generator/blob/v7.26.0/modules/openapi-generator/src/main/resources/kotlin-client/data_class.mustache), the inner enum's annotation is `@Serializable{{#enumUnknownDefaultCase}}(with = …Serializer::class){{/enumUnknownDefaultCase}}` and its serializer object is inside `{{#enumUnknownDefaultCase}}`, while [`enum_class.mustache`](https://github.com/OpenAPITools/openapi-generator/blob/v7.26.0/modules/openapi-generator/src/main/resources/kotlin-client/enum_class.mustache) also adds both under `{{^enumUnknownDefaultCase}}{{^isString}}`.

## Environment

- openapi-generator 7.26.0 (latest release; `master` has the same template), through `@openapitools/openapi-generator-cli` 2.42.0
- Kotlin 2.4.0, kotlinx-serialization-json 1.11.0, Gradle 9.8.2, JDK 25
- macOS 27
