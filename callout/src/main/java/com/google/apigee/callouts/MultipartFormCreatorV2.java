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
import com.google.apigee.json.JavaxJson;
import com.google.apigee.multipartform.MultipartForm;
import com.google.apigee.multipartform.Part;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class MultipartFormCreatorV2 extends CalloutBase implements Execution {
  private static final String varprefix = "mpf_";
  private static final boolean wantStringDefault = true;

  public MultipartFormCreatorV2(Map properties) {
    super(properties);
  }

  public String getVarnamePrefix() {
    return varprefix;
  }

  private boolean getWantDecode(MessageContext msgCtxt) throws Exception {
    String wantDecode = getSimpleOptionalProperty("want-base64-decode", msgCtxt);
    if (wantDecode == null) {
      return false;
    }
    return Boolean.parseBoolean(wantDecode.toLowerCase());
  }

  private String getDestination(MessageContext msgCtxt) throws Exception {
    String destination = getSimpleOptionalProperty("destination", msgCtxt);
    if (destination == null) {
      destination = "message";
    }
    return destination;
  }

  private String getDescriptor(MessageContext msgCtxt) throws Exception {
    return getSimpleRequiredProperty("descriptor", msgCtxt);
  }

  private String getPartContentVar(MessageContext msgCtxt) throws Exception {
    return getSimpleRequiredProperty("contentVar", msgCtxt);
  }

  private String getPartContentType(MessageContext msgCtxt) throws Exception {
    return getSimpleRequiredProperty("contentType", msgCtxt);
  }

  private String getPartFileName(MessageContext msgCtxt) throws Exception {
    return getSimpleOptionalProperty("fileName", msgCtxt);
  }

  private String getPartName(MessageContext msgCtxt) throws Exception {
    return getSimpleRequiredProperty("part-name", msgCtxt);
  }

  public ExecutionResult execute(final MessageContext msgCtxt, final ExecutionContext execContext) {
    try {
      String descriptor = getDescriptor(msgCtxt);

      @SuppressWarnings("unchecked")
      Map<String, Object> descriptorMap = JavaxJson.fromJson(descriptor, Map.class);
      // Map<String, Object> map = gson.fromJson(new StringReader(), Map.class);
      // eg
      // {
      //   "part1.txt" : {
      //     "content-var" :  "variable-name-here",
      //     "content-type" : "content-type-here",
      //     "want-b64-decode": false
      //   },
      //   "part2.png" : {
      //     "content-var" :  "variable-name-here",
      //     "content-type" : "content-type-here",
      //     "want-b64-decode": false
      //   }
      // }

      String boundary = "--------------------" + randomAlphanumeric(14);
      msgCtxt.setVariable(varName("boundary"), boundary);
      String destination = getDestination(msgCtxt);
      Message message = (Message) msgCtxt.getVariable(destination);
      if (message == null) {
        throw new IllegalStateException(String.format("message <%s> does not exist", destination));
        // mustSetDestination = true;
        // message =
        //     msgCtxt.createMessage(
        //         msgCtxt.getClientConnection().getMessageFactory().createRequest(msgCtxt));
      }
      message.setHeader("content-type", "multipart/form-data; boundary=" + boundary);
      msgCtxt.setVariable(varName("ctype"), "multipart/form-data; boundary=" + boundary);

      List<Part> parts = new ArrayList<Part>();
      for (Map.Entry<String, Object> entry : descriptorMap.entrySet()) {
        String partName = entry.getKey();
        @SuppressWarnings("unchecked")
        Map<String, Object> partDefinition = (Map<String, Object>) entry.getValue();

        Object partContent = msgCtxt.getVariable((String) partDefinition.get("content-var"));
        if (partContent == null) {
          throw new IllegalStateException(String.format("part %s has missing content", partName));
        } else if (partContent instanceof String) {
          partContent = ((String) partContent).getBytes(StandardCharsets.UTF_8);
          if ((Boolean) partDefinition.get("want-b64-decode")) {
            partContent = Base64.getDecoder().decode((byte[]) partContent);
          }
        } else if (!(partContent instanceof byte[])) {
          throw new IllegalStateException(String.format("part %s not of supported type", partName));
        }

        Part part =
            new Part(partName)
                .withPartContent((byte[]) partContent)
                .withContentType((String) partDefinition.get("content-type"));

        if (partDefinition.get("file-name") != null
            && !partDefinition.get("file-name").equals("")) {
          part.setFileName((String) partDefinition.get("file-name"));
        }

        if (partDefinition.get("transfer-encoding") != null) {
          part.setTransferEncoding((String) partDefinition.get("transfer-encoding"));
        }

        parts.add(part);
      }

      MultipartForm mpf = new MultipartForm(boundary, parts);
      byte[] payload = streamToByteArray(mpf.openStream());
      msgCtxt.setVariable(varName("payload_length"), payload.length);
      message.setContent(new ByteArrayInputStream(payload));
      // if (mustSetDestination) {
      //   msgCtxt.setVariable(destination, message);
      // }

      msgCtxt.setVariable(destination + ".header.modified", "true");
      return ExecutionResult.SUCCESS;
    } catch (IllegalStateException exc1) {
      setExceptionVariables(exc1, msgCtxt);
      return ExecutionResult.SUCCESS;
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
