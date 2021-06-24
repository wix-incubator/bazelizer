package com.wix.incubator.mvn;

import com.google.common.io.CharSource;

import javax.xml.XMLConstants;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import java.io.File;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

public class Logs {
    public static void info(String m) {
        System.out.println("    [info]  " + m);
    }
    public static void info(Object project, String m) {
        System.out.println("    [info]  {" + project + "}  " + m);
    }

    public static void logXML(File file) {
        String xml = Logs.prettyFormat(com.google.common.io.Files.asCharSource(file, StandardCharsets.UTF_8));
        System.out.println("    [error] build failed. POM xml:");
        System.out.println(xml);
    }

    public static String prettyFormat(String input) {
        return prettyFormat(CharSource.wrap(input), 2);
    }

    public static String prettyFormat(CharSource input) {
        return prettyFormat(input, 2);
    }

    private static String prettyFormat(CharSource input, int indent) {
        try (Reader r = input.openStream()) {
            Source xmlInput = new StreamSource(r);
            StringWriter stringWriter = new StringWriter();
            StreamResult xmlOutput = new StreamResult(stringWriter);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", indent);
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(xmlInput, xmlOutput);
            return xmlOutput.getWriter().toString();
        } catch (Exception e) {
            throw new RuntimeException(e); // simple exception handling, please review it
        }
    }


}
