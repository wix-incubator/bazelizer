package com.wix.incubator.mvn;

import com.google.common.io.Files;
import com.sun.org.apache.xml.internal.serializer.OutputPropertiesFactory;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
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

    public static void dumpXmlFile(File file) {
        System.out.println(prettyPrint(file));
    }

    private static String prettyPrint(File file) {
        final StringWriter writer = new StringWriter();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(file);
            trimWhitespace(doc);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            final Transformer trans = transformerFactory.newTransformer();
            trans.setOutputProperty(OutputKeys.INDENT, "yes");
            trans.setOutputProperty(OutputKeys.VERSION, "1.0");
            trans.setOutputProperty(OutputPropertiesFactory.S_KEY_INDENT_AMOUNT, "2");
            trans.transform(new DOMSource(doc), new StreamResult(writer));
        } catch (TransformerException | SAXException | ParserConfigurationException | IOException ex) {
            throw new IllegalStateException(ex);
        }
        return writer.toString();
    }


    private static void trimWhitespace(Node node) {
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); ++i) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.CDATA_SECTION_NODE) {
                System.out.println("");
            }
            if (child.getNodeType() == Node.TEXT_NODE) {
                child.setTextContent(child.getTextContent().trim());
            }
            trimWhitespace(child);
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
