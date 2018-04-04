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
import org.junit.jupiter.api.Test;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

class RegistrylessAvroConverterTest {
  @Test
  void configureRequiresSchemaPath() {
    RegistrylessAvroConverter sut = new RegistrylessAvroConverter();
    Map<String, Object> settings = new HashMap<String, Object>();
    settings.put("some.random.setting", "bacon");

    Throwable resultingException = assertThrows(IllegalStateException.class, () -> sut.configure(settings, false));
    assertEquals("The schema.path configuration setting is required to use the RegistrylessAvroConverter.", resultingException.getMessage());
  }

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
  void fromConnectDataWorks() {
    // todo
  }

  @Test
  void toConnectDataWorks() {
    // todo
  }
}
