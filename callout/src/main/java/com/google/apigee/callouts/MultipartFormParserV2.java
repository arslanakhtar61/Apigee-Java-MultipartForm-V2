// MultipartFormParserV2.java
//
// Copyright (c) 2018-2023 Google LLC.
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

package com.google.apigee.callouts;

import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.execution.spi.Execution;
import com.apigee.flow.message.Message;
import com.apigee.flow.message.MessageContext;
import com.google.apigee.multipartform.Part;
import com.google.apigee.stream.StreamSearcher;
import java.io.BufferedInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MultipartFormParserV2 extends CalloutBase implements Execution {
  private static final String varprefix = "mpf_";
  private static final boolean wantStringDefault = true;
  private static int BUFFER_SIZE = 2048;

  public MultipartFormParserV2(Map properties) {
    super(properties);
  }

  public String getVarnamePrefix() {
    return varprefix;
  }

  private String getSource(MessageContext msgCtxt) throws Exception {
    String source = getSimpleOptionalProperty("source", msgCtxt);
    if (source == null) {
      source = "message";
    }
    return source;
  }

  public ExecutionResult execute(final MessageContext msgCtxt, final ExecutionContext execContext) {
    try {
      String source = getSource(msgCtxt);
      Message message = (Message) msgCtxt.getVariable(source);
      if (message == null) {
        throw new IllegalStateException("source message is null.");
      }

      String ctype = message.getHeader("content-type");
      if (ctype == null) {
        throw new IllegalStateException("missing content-type header");
      }
      if (!ctype.startsWith("multipart/form-data; boundary=")) {
        throw new IllegalStateException("content-type does not contain multipart/form-data");
      }
      String boundary = ctype.substring("multipart/form-data; boundary=".length());

      StreamSearcher searcher = new StreamSearcher(boundary.getBytes(StandardCharsets.UTF_8));
      List<String> names = new ArrayList<String>();
      try (BufferedInputStream bis =
          new BufferedInputStream(message.getContentAsStream(), BUFFER_SIZE)) {
        byte[] buf = null;
        int numFound = 0;
        long position = searcher.search(bis);
        if (position != -1) {
          do {
            buf = searcher.searchAndExtract(bis);
            if (buf != null) {
              numFound++;
              Part part = Part.parse(buf);
              if (part == null) {
                throw new IllegalStateException("part is null");
              }
              if (part.getName() == null) {
                throw new IllegalStateException("part.getName() is null");
              }
              String fileName = part.getName().replaceAll("[^a-zA-Z0-9_\\. ]", "");
              names.add(fileName);
              msgCtxt.setVariable(varName("item_filename_" + numFound), fileName);
              msgCtxt.setVariable(varName("item_content_" + numFound), part.getPartContent());
              msgCtxt.setVariable(varName("item_content-type_" + numFound), part.getContentType());
              msgCtxt.setVariable(varName("item_size_" + numFound), part.getSize() + "");
            }
          } while (buf != null);
        }
        msgCtxt.setVariable(varName("itemcount"), names.size() + "");
        if (names.size() > 0) {
          msgCtxt.setVariable(varName("items"), String.join(", ", names));
        }

      } finally {
      }
      return ExecutionResult.SUCCESS;
    } catch (IllegalStateException exc1) {
      setExceptionVariables(exc1, msgCtxt);
      return ExecutionResult.ABORT;
    } catch (Exception e) {
      if (getDebug()) {
        String stacktrace = getStackTraceAsString(e);
        msgCtxt.setVariable(varName("stacktrace"), stacktrace);
      }
      setExceptionVariables(e, msgCtxt);
      return ExecutionResult.SUCCESS;
    }
  }
}
