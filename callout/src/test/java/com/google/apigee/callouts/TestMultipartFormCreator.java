// Copyright 2016 Apigee Corp, 2017-2023 Google LLC.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// ------------------------------------------------------------------

package com.google.apigee.callouts;

import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.message.Message;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestMultipartFormCreator extends TestBase {

  @Test
  public void create_Simple_SinglePart() throws Exception {
    byte[] imageBytes = loadImageBytes("Logs_512px.png.b64");
    msgCtxt.setVariable("base64EncodedImageData", new String(imageBytes, StandardCharsets.UTF_8));
    String descriptorJson =
        "{\n"
            + "  \"image.png\" : {\n"
            + "    \"content-var\" :  \"base64EncodedImageData\",\n"
            + "    \"content-type\" : \"image/png\",\n"
            + "    \"want-b64-decode\": true,\n"
            + "    \"file-name\": \"Logs_512px.png\"\n"
            + "  }\n"
            + "}\n";
    msgCtxt.setVariable("descriptor-json", descriptorJson);

    Properties props = new Properties();
    props.put("descriptor", descriptorJson);
    // props.put("destination", "destination");
    props.put("debug", "true");

    MultipartFormCreatorV2 callout = new MultipartFormCreatorV2(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    ExecutionResult expectedResult = ExecutionResult.SUCCESS;
    Assert.assertEquals(actualResult, expectedResult, "ExecutionResult");

    // check result and output
    Object error = msgCtxt.getVariable("mpf_error");
    Assert.assertNull(error, "error");

    Object stacktrace = msgCtxt.getVariable("mpf_stacktrace");
    Assert.assertNull(stacktrace, "stacktrace");

    // cannot directly reference message.content with the mocked MessageContext
    // Object output = msgCtxt.getVariable("message.content");
    Message msg = msgCtxt.getMessage();
    Object output = msg.getContent();
    Assert.assertNotNull(output, "no output");
  }

  private static void copyInputStreamToFile(InputStream inputStream, File file) throws IOException {
    Boolean wantAppend = false;
    try (FileOutputStream outputStream = new FileOutputStream(file, wantAppend)) {
      int read;
      byte[] bytes = new byte[1024];
      while ((read = inputStream.read(bytes)) != -1) {
        outputStream.write(bytes, 0, read);
      }
    }
  }

  @Test
  public void create_Json_MultipleParts() throws Exception {
    String descriptorJson =
        "{\n"
            + "  \"part1.json\" : {\n"
            + "    \"content-var\" :  \"descriptor-json\",\n"
            + "    \"content-type\" : \"application/json\",\n"
            + "    \"want-b64-decode\": false\n"
            + "  },\n"
            + "  \"part2.png\" : {\n"
            + "    \"content-var\" :  \"imageBytes\",\n"
            + "    \"content-type\" : \"image/png\",\n"
            + "    \"want-b64-decode\": false,\n"
            + "    \"file-name\": \"Logs_512px.png\"\n"
            + "  }\n"
            + "}\n";

    byte[] imageBytes = loadImageBytes("Logs_512px.png");
    msgCtxt.setVariable("imageBytes", imageBytes);
    msgCtxt.setVariable("descriptor-json", descriptorJson);

    Properties props = new Properties();
    props.put("descriptor", descriptorJson);
    // props.put("destination", "destination");
    props.put("debug", "true");

    MultipartFormCreatorV2 callout = new MultipartFormCreatorV2(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    ExecutionResult expectedResult = ExecutionResult.SUCCESS;
    Assert.assertEquals(actualResult, expectedResult, "ExecutionResult");

    // check result and output
    Object error = msgCtxt.getVariable("mpf_error");
    Assert.assertNull(error, "error");

    Object stacktrace = msgCtxt.getVariable("mpf_stacktrace");
    Assert.assertNull(stacktrace, "stacktrace");

    // cannot directly reference message.content with the mocked MessageContext
    // Object output = msgCtxt.getVariable("message.content");
    Message msg = msgCtxt.getVariable("message");
    InputStream is = msg.getContentAsStream();
    Assert.assertNotNull(is, "no stream");

    copyInputStreamToFile(is, new File("./create_Json_MultipleParts.out"));
  }

}
