# Registryless Avro Converter

This is an Avro Converter for Kafka Connect that does not depend on Confluent Schema Registry. It
shares much of the same underlying code, and should work mostly the same as their existing Avro
Converter, however.

## Using the Converter

To use the converter, you'll want to download a binary from GitHub and put it on the classpath
for your Connect instance. Then in your properties file for your connector you can specify this
converter for keys and/or values in your `worker.properties`:

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

## Notes

* This project is a bit weird because it's designed to be run in a Kafka Connect runtime. So
  all of the dependencies are `compileOnly` because they're available on the classpath at runtime.
* If you're testing this locally, it's a bit weird in much the same way. You'll need to copy
  the JAR into an appropriate `lib/` folder so the class is visible to Kafka Connect for local
  testing.
* We're planning to automate the above once we have a Docker image of the Confluent Platform version
  we depend on.
