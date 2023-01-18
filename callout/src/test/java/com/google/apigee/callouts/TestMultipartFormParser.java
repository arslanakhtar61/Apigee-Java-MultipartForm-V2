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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestMultipartFormParser extends TestBase {

  private static void writeToFile(byte[] bytes, File file) throws IOException {
    Boolean wantAppend = false;
    try (FileOutputStream outputStream = new FileOutputStream(file, wantAppend)) {
      outputStream.write(bytes, 0, bytes.length);
    }
  }

  @Test
  public void parse_Simple() throws Exception {
    Message msg = msgCtxt.getMessage();
    byte[] payloadBytes = loadImageBytes("MultiPart-payload.out");
    msg.setContent(new ByteArrayInputStream(payloadBytes));
    msg.setHeader(
        "content-type", "multipart/form-data; boundary=----------------------QCN1DGMIPH8GPY");

    Properties props = new Properties();
    props.put("source", "message");
    props.put("debug", "true");

    MultipartFormParserV2 callout = new MultipartFormParserV2(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    ExecutionResult expectedResult = ExecutionResult.SUCCESS;
    Assert.assertEquals(actualResult, expectedResult, "ExecutionResult");

    // check result and output
    Object error = msgCtxt.getVariable("mpf_error");
    Assert.assertNull(error, "error");

    Object stacktrace = msgCtxt.getVariable("mpf_stacktrace");
    Assert.assertNull(stacktrace, "stacktrace");

    Object content2 = msgCtxt.getVariable("mpf_item_content_2");
    Assert.assertTrue(content2 instanceof byte[]);
    String filename = (String) msgCtxt.getVariable("mpf_item_filename_2");

    writeToFile((byte[]) content2, new File("output-" + filename));
  }
}
