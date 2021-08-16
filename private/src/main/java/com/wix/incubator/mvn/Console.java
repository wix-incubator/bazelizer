package com.wix.incubator.mvn;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.sun.org.apache.xml.internal.serializer.OutputPropertiesFactory;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.io.output.TeeOutputStream;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SuppressWarnings("Guava")
public class Console {

    private static final Supplier<PrintStream> output = Suppliers.memoize(() -> {
        String logFile = System.getProperty("tools.jvm.mvn.LogFile");
        PrintStream output;
        try {
            OutputStream tee = new NullOutputStream();
            if (logFile != null) {
                tee = new BufferedOutputStream(new FileOutputStream(logFile));
            }
            output = new TeePrintStream(System.out, tee);
        } catch (IOException e) {
            System.err.println("Problem with file init " + e.getMessage());
            output = System.out;
        }
        return output;
    });


    public static void printSeparator() {
        Console.info(" " + IntStream.range(0, 48).mapToObj(i -> "=").collect(Collectors.joining()));
    }

    public static void info(String m) {
        output.get().println("[info]  " + m);
    }

    public static void info(Object project, String m) {
        output.get().println("[info]  {" + project + "}  " + m);
    }

    public static void error(Object project, String m) {
        output.get().println("[error]  {" + project + "}  " + m);
    }

    public static void dumpXmlFile(File file) {
        output.get().println(prettyPrint(file));
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

    public static class PrintOutputHandler implements InvocationOutputHandler {
        @Override
        public void consumeLine(String line) throws IOException {
            output.get().println("\t" + line);
        }
    }


    @SuppressWarnings({"NullableProblems", "unused"})
    private static class TeePrintStream extends PrintStream {
        protected PrintStream parent;
        protected String fileName;

        /**
         * Construct a TeePrintStream given an existing PrintStream, an opened
         * OutputStream, and a boolean to control auto-flush. This is the main
         * constructor, to which others delegate via "this".
         */
        public TeePrintStream(PrintStream orig, OutputStream os, boolean flush)
                throws IOException {
            super(os, true);
            fileName = "(opened Stream)";
            parent = orig;
        }

        /**
         * Construct a TeePrintStream given an existing PrintStream and an opened
         * OutputStream.
         */
        public TeePrintStream(PrintStream orig, OutputStream os) throws IOException {
            this(orig, os, true);
        }

        /*
         * Construct a TeePrintStream given an existing Stream and a filename.
         */
        public TeePrintStream(PrintStream os, String fn) throws IOException {
            this(os, fn, true);
        }

        /*
         * Construct a TeePrintStream given an existing Stream, a filename, and a
         * boolean to control the flush operation.
         */
        public TeePrintStream(PrintStream orig, String fn, boolean flush)
                throws IOException {
            this(orig, new FileOutputStream(fn), flush);
        }

        /** Return true if either stream has an error. */
        public boolean checkError() {
            return parent.checkError() || super.checkError();
        }

        /** override write(). This is the actual "tee" operation. */
        public void write(int x) {
            parent.write(x); // "write once;
            super.write(x); // write somewhere else."
        }

        /** override write(). This is the actual "tee" operation. */
        public void write(byte[] x, int o, int l) {
            parent.write(x, o, l); // "write once;
            super.write(x, o, l); // write somewhere else."
        }

        /** Close both streams. */
        public void close() {
            parent.close();
            super.close();
        }

        /** Flush both streams. */
        public void flush() {
            parent.flush();
            super.flush();
        }
    }
}
