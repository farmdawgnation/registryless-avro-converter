/**
 * Copyright 2018 Matt Farmer (github.com/farmdawgnation)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.frmr.kafka.connect;

import io.confluent.connect.avro.AvroData;
import java.io.File;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import org.apache.avro.SchemaParseException;
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
      } catch (SchemaParseException spe) {
        throw new IllegalStateException("Unable to parse Avro schema when starting RegistrylessAvroConverter", spe);
      } catch (IOException ioe) {
        throw new IllegalStateException("Unable to parse Avro schema when starting RegistrylessAvroConverter", ioe);
      }
    }
  }

  @Override
  public byte[] fromConnectData(String topic, Schema schema, Object value) {
    DatumWriter<GenericRecord> datumWriter;
    if (avroSchema != null) {
      datumWriter = new GenericDatumWriter<GenericRecord>(avroSchema);
    } else {
      datumWriter = new GenericDatumWriter<GenericRecord>();
    }
    GenericRecord avroInstance = (GenericRecord)avroDataHelper.fromConnectData(schema, value);

    try (
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      DataFileWriter<GenericRecord> dataFileWriter = new DataFileWriter<GenericRecord>(datumWriter);
    ) {
      dataFileWriter.setCodec(CodecFactory.nullCodec());

      if (avroSchema != null) {
        dataFileWriter.create(avroSchema, baos);
      } else {
        dataFileWriter.create(avroInstance.getSchema(), baos);
      }

      dataFileWriter.append(avroInstance);
      dataFileWriter.flush();

      return baos.toByteArray();
    } catch (IOException ioe) {
      throw new DataException("Error serializing Avro", ioe);
    }
  }

  @Override
  public SchemaAndValue toConnectData(String topic, byte[] value) {
    DatumReader<GenericRecord> datumReader;
    if (avroSchema != null) {
      datumReader = new GenericDatumReader<>(avroSchema);
    } else {
      datumReader = new GenericDatumReader<>();
    }
    GenericRecord instance = null;

    try (
      SeekableByteArrayInput sbai = new SeekableByteArrayInput(value);
      DataFileReader<GenericRecord> dataFileReader = new DataFileReader<>(sbai, datumReader);
    ) {
      instance = dataFileReader.next(instance);
      if (instance == null) {
        logger.warn("Instance was null");
      }

      if (avroSchema != null) {
        return avroDataHelper.toConnectData(avroSchema, instance);
      } else {
        return avroDataHelper.toConnectData(instance.getSchema(), instance);
      }
    } catch (IOException ioe) {
      throw new DataException("Failed to deserialize Avro data from topic %s :".format(topic), ioe);
    }
  }
}
