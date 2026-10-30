# Kotlin client: an inline number enum is serialized as a string

Reported in https://github.com/OpenAPITools/openapi-generator/issues/25459.

With `serializationLibrary=kotlinx_serialization`, a number enum declared inline on a property becomes a nested enum class with `@SerialName("…")` and no value serializer, so kotlinx.serialization writes it as a string and can't read the number the API sends. [#25473](https://github.com/OpenAPITools/openapi-generator/pull/25473) fixed this for `type: integer` (`plays`), but it skips `type: number`. With `number` mapped to `kotlin.Double` (`--type-mappings=number=kotlin.Double`), `volume: { type: number, enum: [0.5, 1] }` is still written as `"0.5"`.

## Reproduction

Needs Node and a JDK 17 or newer.

```sh
npm install
npm run repro
```

Output with the `7.27.0-20261030.091743-151` snapshot (published after #25473 merged):

```
encoded: {"plays":1,"speed":1,"volume":"0.5"}
decoded: JsonDecodingException: Unexpected JSON token at offset 30: Expected quotation mark '"', but had '0' instead at path: $.volume
```

`npm run repro` generates the client from [openapi.yaml](openapi.yaml) (`-g kotlin`, `serializationLibrary=kotlinx_serialization`, `--type-mappings=number=kotlin.Double`), then compiles only the generated models with [Main.kt](src/main/kotlin/Main.kt), which encodes a `Recording` and decodes `{"plays":1,"speed":1,"volume":0.5}`. [openapitools.json](openapitools.json) pins the snapshot jar; its SHA256 is `aa5f6609f715edff1a8701ff8ad1eb608fb6eed9973efe339e3b15fb7659591d` (`shasum -a 256 node_modules/@openapitools/openapi-generator-cli/versions/*.jar`).

On 7.26.0, `plays` failed the same way; #25473 fixed it.

## Expected

```
encoded: {"plays":1,"speed":1,"volume":0.5}
decoded: Recording(plays=_1, speed=1, volume=_0Period5)
```

## Actual

`volume` is written as `"0.5"` and `0.5` can't be read. The generated `Recording.Volume` is a plain `@Serializable enum class Volume(val value: kotlin.Double)` with `@SerialName(value = "0.5") _0Period5(0.5)`, while `Recording.Plays` now gets `@Serializable(with = PlaysSerializer::class)`.

In `kotlin-client/data_class.mustache`, both the `(with = {{nameInPascalCase}}Serializer::class)` annotation and the serializer object are inside `{{^isNumber}}`, which #25473 added because `java.math.BigDecimal` has no `serializer()`. A mapped `kotlin.Double` keeps `isNumber`, so it's skipped too. Gating both on `{{#isPrimitiveType}}` instead of `{{^isNumber}}` gives the expected output above, and an unmapped number enum (`BigDecimal`, not a primitive) keeps the plain `@Serializable` it has now.

## Environment

- openapi-generator `7.27.0-20261030.091743-151` (snapshot), through `@openapitools/openapi-generator-cli` 2.42.0
- Kotlin 2.4.0, kotlinx-serialization-json 1.11.0, Gradle 9.8.2, JDK 25
- macOS 27
