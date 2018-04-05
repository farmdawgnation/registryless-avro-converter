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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.avro.io.DatumReader;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.SeekableByteArrayInput;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.kafka.connect.data.*;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

class RegistrylessAvroConverterTest {
  @Test
  void configureWorksOnParsableSchema() {
    // This only has to work in the project directory because this is a test. I'm not particularly
    // concerned if it works when the tests are packaged in JAR form right now. If we start doing
    // that then we'll do something clever-er.
    String validSchemaPath = new File("src/test/resources/schema/dog.avsc").getAbsolutePath();

    RegistrylessAvroConverter sut = new RegistrylessAvroConverter();
    Map<String, Object> settings = new HashMap<String, Object>();
    settings.put("schema.path", validSchemaPath);

    sut.configure(settings, false);
  }

  @Test
  void configureThrowsOnInvalidSchema() {
    // This only has to work in the project directory because this is a test. I'm not particularly
    // concerned if it works when the tests are packaged in JAR form right now. If we start doing
    // that then we'll do something clever-er.
    String invalidSchemaPath = new File("src/test/resources/schema/invalid.avsc").getAbsolutePath();

    RegistrylessAvroConverter sut = new RegistrylessAvroConverter();
    Map<String, Object> settings = new HashMap<String, Object>();
    settings.put("schema.path", invalidSchemaPath);

    Throwable resultingException = assertThrows(IllegalStateException.class, () -> sut.configure(settings, false));
    assertEquals("Unable to parse Avro schema when starting RegistrylessAvroConverter", resultingException.getMessage());
  }


  @Test
  void fromConnectDataWorksWithWriterSchema() throws Exception {
    // This only has to work in the project directory because this is a test. I'm not particularly
    // concerned if it works when the tests are packaged in JAR form right now. If we start doing
    // that then we'll do something clever-er.
    String validSchemaPath = new File("src/test/resources/schema/dog.avsc").getAbsolutePath();

    RegistrylessAvroConverter sut = new RegistrylessAvroConverter();
    Map<String, Object> settings = new HashMap<String, Object>();
    settings.put("schema.path", validSchemaPath);
    sut.configure(settings, false);

    Schema dogSchema = SchemaBuilder.struct()
      .name("dog")
      .field("name", Schema.STRING_SCHEMA)
      .field("breed", Schema.STRING_SCHEMA)
      .build();

    Struct dogStruct = new Struct(dogSchema)
      .put("name", "Beamer")
      .put("breed", "Boarder Collie");

    byte[] result = sut.fromConnectData("test_topic", dogSchema, dogStruct);
    FileUtils.writeByteArrayToFile(new File("example.avro"), result);

    // This is a bit annoying but because of the way avro works - the resulting byte array isn't
    // deterministic - so we need to read it back using the avro tools.
    DatumReader<GenericRecord> datumReader = new GenericDatumReader<>();
    GenericRecord instance = null;
    try (
      SeekableByteArrayInput sbai = new SeekableByteArrayInput(result);
      DataFileReader<GenericRecord> dataFileReader = new DataFileReader<>(sbai, datumReader);
    ) {
      instance = dataFileReader.next();

      assertEquals("Beamer", instance.get("name").toString());
      assertEquals("Boarder Collie", instance.get("breed").toString());
    } catch (IOException ioe) {
      throw new Exception("Failed to deserialize Avro data", ioe);
    }
  }

  @Test
  void fromConnectDataWorksWithoutWriterSchema() throws Exception {
    RegistrylessAvroConverter sut = new RegistrylessAvroConverter();
    Map<String, Object> settings = new HashMap<String, Object>();
    sut.configure(settings, false);

    Schema dogSchema = SchemaBuilder.struct()
      .name("dog")
      .field("name", Schema.STRING_SCHEMA)
      .field("breed", Schema.STRING_SCHEMA)
      .build();

    Struct dogStruct = new Struct(dogSchema)
      .put("name", "Beamer")
      .put("breed", "Boarder Collie");

    byte[] result = sut.fromConnectData("test_topic", dogSchema, dogStruct);
    FileUtils.writeByteArrayToFile(new File("example.avro"), result);

    // This is a bit annoying but because of the way avro works - the resulting byte array isn't
    // deterministic - so we need to read it back using the avro tools.
    DatumReader<GenericRecord> datumReader = new GenericDatumReader<>();
    GenericRecord instance = null;
    try (
      SeekableByteArrayInput sbai = new SeekableByteArrayInput(result);
      DataFileReader<GenericRecord> dataFileReader = new DataFileReader<>(sbai, datumReader);
    ) {
      instance = dataFileReader.next();

      assertEquals("Beamer", instance.get("name").toString());
      assertEquals("Boarder Collie", instance.get("breed").toString());
    } catch (IOException ioe) {
      throw new Exception("Failed to deserialize Avro data", ioe);
    }
  }

  @Test
  void toConnectDataWorksWithReaderSchema() throws IOException {
    InputStream dogDataStream = this.getClass().getClassLoader().getResourceAsStream("data/binary/beamer.avro");
    byte[] dogData = IOUtils.toByteArray(dogDataStream);

    // This only has to work in the project directory because this is a test. I'm not particularly
    // concerned if it works when the tests are packaged in JAR form right now. If we start doing
    // that then we'll do something clever-er.
    String validSchemaPath = new File("src/test/resources/schema/dog.avsc").getAbsolutePath();

    RegistrylessAvroConverter sut = new RegistrylessAvroConverter();
    Map<String, Object> settings = new HashMap<String, Object>();
    settings.put("schema.path", validSchemaPath);
    sut.configure(settings, false);

    SchemaAndValue sav = sut.toConnectData("test_topic", dogData);

    Schema dogSchema = sav.schema();
    assertEquals(Schema.Type.STRUCT, dogSchema.type());
    assertEquals(Schema.Type.STRING, dogSchema.field("name").schema().type());
    assertEquals(Schema.Type.STRING, dogSchema.field("breed").schema().type());

    Struct dogStruct = (Struct)sav.value();
    assertEquals("Beamer", dogStruct.getString("name"));
    assertEquals("Border Collie", dogStruct.getString("breed"));
  }

  @Test
  void toConnectDataWorksWithoutReaderSchema() throws IOException {
    InputStream dogDataStream = this.getClass().getClassLoader().getResourceAsStream("data/binary/beamer.avro");
    byte[] dogData = IOUtils.toByteArray(dogDataStream);

    RegistrylessAvroConverter sut = new RegistrylessAvroConverter();
    Map<String, Object> settings = new HashMap<String, Object>();
    sut.configure(settings, false);

    SchemaAndValue sav = sut.toConnectData("test_topic", dogData);

    Schema dogSchema = sav.schema();
    assertEquals(Schema.Type.STRUCT, dogSchema.type());
    assertEquals(Schema.Type.STRING, dogSchema.field("name").schema().type());
    assertEquals(Schema.Type.STRING, dogSchema.field("breed").schema().type());

    Struct dogStruct = (Struct)sav.value();
    assertEquals("Beamer", dogStruct.getString("name"));
    assertEquals("Border Collie", dogStruct.getString("breed"));
  }
}
