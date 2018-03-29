# Registryless Avro Converter

This is an Avro converter for Kafka Connect that does not depend on Confluent Schema Registry. It
shares much of the same underlying code as Confluent's `AvroConverter`, and should work the same in
practice less any features that deal with the Schema Registry itself.

We developed this converter to facilitate cases where our Kafka Connect instance isn't opinionated
about _what_ it's actually processing and should largely be able to rely on the consistency of
schemas by design. However, even if your project doesn't meet those requirements you may find
this converter useful for doing R&D work or just testing the running of Avro through Connect without
the extra Schema Registry overhead.

## Using the Converter

To use the converter, you'll want to download a binary from GitHub and put it in the plugins folder
for your Connect instance. You'll need to consult your Kafka Connect configuration to determine
where this is for your instance.

Once you've confirmed that the binary is in place, then in a properties file or JSON connector
configuration you can specify this converter for keys and/or values. You are currently required
to provide a reader schema for the converter to use, as well, under the `schema.path` setting.

Here's an example of how we might define RAC for use with keys and values in standalone mode:

```
key.converter=me.frmr.kafka.connect.RegistrylessAvroConverter
key.converter.schema.path=/path/to/schema/file.avsc
value.converter=me.frmr.kafka.connect.RegistrylessAvroConverter
value.converter.schema.path=/path/to/schema/file.avsc
```

## Building the Converter

This converter uses Gradle. Building the project is as simple as:

```
./gradlew build
```

## General Notes

* This project is a bit weird because it's designed to be run in a Kafka Connect runtime. So
  all of the dependencies are `compileOnly` because they're available on the classpath at runtime.
* If you're testing this locally, it's a bit weird in much the same way. You'll need to copy
  the JAR into an appropriate `lib/` folder so the class is visible to Kafka Connect for local
  testing.

## Contributing

Pull requests and issues are welcome! If you think you've spotted a problem or you just have a
question do not hesitate to [open an issue][https://github.com/farmdawgnation/registryless-avro-converter/issues/new].
