# Registryless Avro Converter

This is an Avro converter for Kafka Connect that does not depend on Confluent Schema Registry. It
shares much of the same underlying code as Confluent's `AvroConverter`, and should work the same in
practice less any features that deal with the Schema Registry itself.

We developed this converter at MailChimp to facilitate R&D with Connect and use cases where pushing
the Schema Registry Avro Format through Kafka was not desirable or we couldn't justify the overhead
of a Schema Registry.

## Using the Converter

### Setup

1. You must have **Java 8** as your runtime environment.
2. **Confluent Platform**: as this plugin relies on various Confluent libraries that are
  distributed with CP (e.g. their avro converter, etc). See the chart below for the version matrix.
3. Configure a `plugin.path` in your connect setup and drop a RegistrylessAvroConverter JAR in that
  path so that its picked up with Kafka Connect starts.

Once you've confirmed that the binary is in place, then in a properties file or JSON connector
configuration you can specify this converter for keys and/or values.

### Version Matrix

| RAC Version   | Kafka Version | Confluent Version |
|---------------|---------------|-------------------|
| 1.9.0         | 2.4.1         | 5.4.2             |
| 1.8.0         | 2.3.0         | 5.3.0             |
| 1.7.0         | 2.2.0         | 5.2.0             |
| 1.6.0         | 2.1.1         | 5.1.2             |
| 1.5.0         | 2.0.1         | 5.0.3             |
| 1.4.0         | 1.1.1         | 4.1.3             |
| 1.3.0         | 1.0.0         | 4.0.0             |

### Configuration

To use the RegistrylessAvroConverter, simply provide it in the `key.converter` or `value.converter`
setting for your connector. RAC can run with or without an explicit reader or writer schema. If an
explicit schema is not provided, the schema used will be determined at runtime.

**N.B.** Schemas determined at runtime could vary depending on how your connector is implemented
and how it generates Connect Data Schemas. We recommend understanding the semantics of your
Connectors before using the schemaless configuration for sources.

Here's an example of how we might define RAC for use with keys and values without an explicit schema
in standalone mode:

```
key.converter=me.frmr.kafka.connect.RegistrylessAvroConverter
value.converter=me.frmr.kafka.connect.RegistrylessAvroConverter
```

And this is how you would define a RAC _with_ an explicit schema:

```
key.converter=me.frmr.kafka.connect.RegistrylessAvroConverter
key.converter.schema.path=/path/to/schema/file.avsc
value.converter=me.frmr.kafka.connect.RegistrylessAvroConverter
value.converter.schema.path=/path/to/schema/file.avsc
```

You can also tune the number of cached schemas we maintain in memory. By default, we store 50 but
you may need to increase that limit if your data structures have a lot of nesting or you're dealing
with a lot of different data structure. You can tune it using the `schema.cache.size` setting:

```
key.converter.schema.cache.size = 100
value.converter.schema.cache.size = 100
```

Unfortunately, the best way to _know_ you need to tune this value right now might be to hook up
YourKit or something similar.

## Building the Converter

This converter uses Gradle. Building the project is as simple as:

```
./gradlew build
```

## General Notes

* This project is a bit weird because it's designed to be run in a Kafka Connect runtime. So
  all of the dependencies are `compileOnly` because they're available on the classpath at runtime.
* If you're testing this locally, it's a bit weird in much the same way. You'll need to copy
  the JAR into an appropriate plugin path folder (as configured on your Connect worker) so the class
  is visible to Kafka Connect for local testing.

## Contributing

Pull requests and issues are welcome! If you think you've spotted a problem or you just have a
question do not hesitate to [open an issue](https://github.com/farmdawgnation/registryless-avro-converter/issues/new).
