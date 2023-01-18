// Copyright 2016 Apigee Corp, 2017-2022 Google LLC.
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

import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.message.Message;
import com.apigee.flow.message.MessageContext;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import mockit.Mock;
import mockit.MockUp;
import org.testng.annotations.BeforeMethod;

public abstract class TestBase {
  protected static final String testDataDir = "src/test/resources/test-data";
  protected static final boolean verbose = true;

  MessageContext msgCtxt;
  InputStream messageContentStream;
  Message message;
  ExecutionContext exeCtxt;

  @BeforeMethod()
  public void beforeMethod() {

    msgCtxt =
        new MockUp<MessageContext>() {
          private Map<String, Object> variables;

          public void $init() {
            variables = new HashMap<String, Object>();
          }

          @Mock()
          public Object getVariable(final String name) {
            if (variables == null) {
              variables = new HashMap<String, Object>();
            }
            // T value = null;
            // if (name.equals("message")) {
            //     value = (T) message;
            // }
            // value = (T) variables.get(name);
            // if (verbose)
            //   System.out.printf(
            //                     "getVariable(%s) <= %s\n", name, (value != null) ? value :
            // "(null)");
            // return value;

            if (name.equals("message")) {
              return message;
            }
            return variables.get(name);
          }

          @Mock()
          public boolean setVariable(final String name, final Object value) {
            if (verbose)
              System.out.printf(
                  "setVariable(%s) <= %s\n", name, (value != null) ? value : "(null)");
            if (variables == null) {
              variables = new HashMap<String, Object>();
            }
            if (name.equals("message.content")) {
              if (value instanceof String) {
                messageContentStream =
                    new ByteArrayInputStream(((String) value).getBytes(StandardCharsets.UTF_8));
              } else if (value instanceof InputStream) {
                messageContentStream = (InputStream) value;
              }
            }
            variables.put(name, value);
            return true;
          }

          @Mock()
          public boolean removeVariable(final String name) {
            if (verbose) System.out.printf("removeVariable(%s)\n", name);
            if (variables == null) {
              variables = new HashMap<String, Object>();
            }
            if (variables.containsKey(name)) {
              variables.remove(name);
            }
            return true;
          }

          @Mock()
          public Message getMessage() {
            return message;
          }
        }.getMockInstance();

    exeCtxt = new MockUp<ExecutionContext>() {}.getMockInstance();

    message =
        new MockUp<Message>() {
          private Map<String, String> headers;
          @Mock()
          public InputStream getContentAsStream() {
            return messageContentStream;
          }

          @Mock()
          public void setContent(InputStream is) {
            // System.out.printf("\n** setContent(Stream)\n");
            messageContentStream = is;
          }

          @Mock()
          public void setContent(String content) {
            // System.out.printf("\n** setContent(String)\n");
            messageContentStream =
                new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
          }

          @Mock()
          public String getContent() {
            // System.out.printf("\n** getContent()\n");
            return new String(readAll(messageContentStream), StandardCharsets.UTF_8);
          }

          @Mock()
          public boolean setHeader(final String name, final Object value) {
            if (verbose)
              System.out.printf(
                  "setHeader(%s) <= %s\n", name, (value != null) ? value : "(null)");
            if (headers == null) {
              headers = new HashMap<String, String>();
            }
            headers.put(name, value.toString());
            return true;
          }

          @Mock()
          public String getHeader(final String name) {
            if (headers == null) {
              headers = new HashMap<String, String>();
            }
            String value = headers.get(name);

            if (verbose)
              System.out.printf(
                  "getHeader(%s) => %s\n", name, (value != null) ? value : "(null)");
            return value;
          }
        }.getMockInstance();
  }

  private static byte[] readAll(InputStream is) {
    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      int nRead;
      byte[] data = new byte[1024];
      while ((nRead = is.read(data, 0, data.length)) != -1) {
        os.write(data, 0, nRead);
      }
      os.flush();
      byte[] b = os.toByteArray();
      return b;
    }
    catch(Exception ex1) {
      return null;
    }
  }

  protected static byte[] loadImageBytes(String filename) throws IOException {
    Path path = Paths.get(testDataDir, filename);
    if (!Files.exists(path)) {
      throw new IOException("file(" + path.toString() + ") not found");
    }
    InputStream imageInputStream = Files.newInputStream(path);
    byte[] imageBytes = readAll(imageInputStream);
    return imageBytes;
  }
}
