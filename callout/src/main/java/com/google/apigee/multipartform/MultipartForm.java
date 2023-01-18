package com.google.apigee.multipartform;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

public class MultipartForm {
  private List<Part> parts;
  private String boundary;

  public MultipartForm(String boundary, List<Part> parts) {
    this.boundary = boundary;
    this.parts = parts;
  }

  private static boolean isEmptyString(String s){
    return s == null || s.trim().equals("");
  }

  public InputStream openStream() {
    // assemble content
    List<InputStream> streams = new ArrayList<InputStream>();
    for (Part part : parts) {

      String contentDisposition =
        String.format("form-data; name=\"%s\"", part.getName()) ;
      if (!isEmptyString(part.getFileName())) {
        contentDisposition += String.format("; filename=\"%s\"", part.getFileName());
      }

      String leader =
        "\r\n"
        + "--"
        + boundary
        + "\r\n"
        + "Content-Disposition: "
        + contentDisposition
        + "\r\n"
        + "Content-Type: "
        + part.getContentType()
        + "\r\n";

      if (!isEmptyString(part.getTransferEncoding())) {
        leader +=
          ""
          + "Content-Transfer-Encoding: "
          + part.getTransferEncoding()
          + "\r\n";
      }

      leader += "\r\n";

      streams.add(new ByteArrayInputStream(leader.getBytes(StandardCharsets.UTF_8)));
      streams.add(new ByteArrayInputStream(part.getPartContent()));
    }

    final String trailer =
        "\r\n"
        + "--"
        + boundary
      + "--\r\n";
      streams.add(new ByteArrayInputStream(trailer.getBytes(StandardCharsets.UTF_8)));

    SequenceInputStream contentInputStream =
        new SequenceInputStream(Collections.enumeration(streams));

    return contentInputStream;
  }

}
