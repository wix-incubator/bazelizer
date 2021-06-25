package com.wix.incubator.mvn;

import com.google.common.io.Files;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.MavenInvocationException;

import javax.xml.XMLConstants;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class Log {
    public static void info(String m) {
        System.out.println("[info]  " + m);
    }

    public static void info(Object project, String m) {
        System.out.println("[info]  {" + project + "}  " + m);
    }

    @SuppressWarnings("UnstableApiUsage")
    public static void dumpXmlFile(File file) {
        try (Reader r = Files.newReader(file, StandardCharsets.UTF_8)) {
            Source xmlInput = new StreamSource(r);
            StringWriter stringWriter = new StringWriter();
            StreamResult xmlOutput = new StreamResult(stringWriter);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", 2);
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "" + 2);
            transformer.transform(xmlInput, xmlOutput);
            String xml = xmlOutput.getWriter().toString();
            System.out.println(xml);
        } catch (Exception e) {
            throw new RuntimeException(e); // simple exception handling, please review it
        }

    }


    public interface Invoc {
        InvocationResult exec(InvocationOutputHandler handler) throws IOException, MavenInvocationException;
    }

    public static InvocationResult invokeWithLogging(Invoc cons) throws IOException, MavenInvocationException {
        InvocationOutputHandler handler = new InvocationOutputHandler() {
            @Override
            public void consumeLine(String line) throws IOException {
                System.out.println("\t" + line);
            }
        };
        return cons.exec(handler);
    }
}
