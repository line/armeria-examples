package com.github.shalk.armeria.tom4j;

import org.dom4j.DocumentHelper;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.StringWriter;
import java.util.function.Function;

public class XmlFormat implements Function<String, String> {


  public static String prettyPrintByDom4j(String xmlString, int indent, boolean skipDeclaration) {
    try {
      OutputFormat format = OutputFormat.createPrettyPrint();
      format.setIndentSize(indent);
      format.setSuppressDeclaration(skipDeclaration);
      format.setEncoding("UTF-8");

      org.dom4j.Document document = DocumentHelper.parseText(xmlString);
      StringWriter sw = new StringWriter();
      XMLWriter writer = new XMLWriter(sw, format);
      writer.write(document);
      return sw.toString();
    } catch (Exception e) {
      throw new RuntimeException("Error occurs when pretty-printing xml:\n" + xmlString, e);
    }
  }

  @Override
  public String apply(String xml) {
    return prettyPrintByDom4j(xml, 2, false);
  }
}
