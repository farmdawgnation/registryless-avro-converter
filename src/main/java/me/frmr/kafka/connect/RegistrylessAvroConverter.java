package me.frmr.kafka.connect;

import io.confluent.connect.avro.AvroData;
import java.io.File;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.file.CodecFactory;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.file.SeekableByteArrayInput;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaAndValue;
import org.apache.kafka.connect.errors.DataException;
import org.apache.kafka.connect.storage.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of Converter that uses Avro schemas and objects without
 * using an external schema registry. Requires that a `schema.path` configuration
 * option is provided that tells the converter where to find its Avro schema.
 */
public class RegistrylessAvroConverter implements Converter {
  private static Logger logger = LoggerFactory.getLogger(RegistrylessAvroConverter.class);

  /**
   * The default schema cache size. We pick 50 so that there's room in the cache for some recurring
   * nested types in a complex schema.
   */
  private Integer schemaCacheSize = 50;

  private org.apache.avro.Schema avroSchema = null;
  private Schema connectSchema = null;
  private AvroData avroDataHelper = null;

  @Override
  public void configure(Map<String, ?> configs, boolean isKey) {
    if (configs.get("schema.cache.size") instanceof Integer) {
      schemaCacheSize = (Integer) configs.get("schema.cache.size");
    }

    avroDataHelper = new AvroData(schemaCacheSize);

    if (configs.get("schema.path") instanceof String) {
      String avroSchemaPath = (String) configs.get("schema.path");
      org.apache.avro.Schema.Parser parser = new org.apache.avro.Schema.Parser();

      File avroSchemaFile = null;
      try {
        avroSchemaFile = new File(avroSchemaPath);
        avroSchema = parser.parse(avroSchemaFile);
        connectSchema = avroDataHelper.toConnectSchema(avroSchema);
      } catch (IOException ioe) {
        throw new IllegalStateException("Unable to parse Avro schema when starting RegistrylessAvroConverter", ioe);
      }
    } else {
      throw new IllegalStateException("The schema.path configuration setting is required to use the RegistrylessAvroConverter.");
    }
  }

  @Override
  public byte[] fromConnectData(String topic, Schema schema, Object value) {
    DatumWriter<Object> datumWriter = new GenericDatumWriter<Object>(avroSchema);
    Object avroInstance = avroDataHelper.fromConnectData(schema, value);

    try (
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      DataFileWriter<Object> dataFileWriter = new DataFileWriter<Object>(datumWriter);
    ) {
      dataFileWriter.setCodec(CodecFactory.nullCodec());
      dataFileWriter.create(avroSchema, baos);
      dataFileWriter.append(avroInstance);

      return baos.toByteArray();
    } catch (IOException ioe) {
      throw new DataException("Error serializing Avro", ioe);
    }
  }

  @Override
  public SchemaAndValue toConnectData(String topic, byte[] value) {
    DatumReader<GenericRecord> datumReader = new GenericDatumReader<>(avroSchema);
    GenericRecord instance = null;

    try (
      SeekableByteArrayInput sbai = new SeekableByteArrayInput(value);
      DataFileReader<GenericRecord> dataFileReader = new DataFileReader<>(sbai, datumReader);
    ) {
      instance = dataFileReader.next(instance);
      if (instance == null) {
        logger.warn("Instance was null");
      }
      return avroDataHelper.toConnectData(avroSchema, instance);
    } catch (IOException ioe) {
      throw new DataException("Failed to deserialize Avro data from topic %s :".format(topic), ioe);
    }
  }
}
