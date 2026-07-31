package com.ksefpdf;

import io.alapierre.ksef.fop.PdfGenerator;
import io.javalin.Javalin;
import io.javalin.http.ContentType;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import java.io.*;

public class Main {

    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        Javalin app = Javalin.create().start(port);

        app.get("/", ctx -> ctx.result("KSeF PDF service is running."));

        app.post("/render-invoice-pdf", ctx -> {
            byte[] xmlBytes = ctx.bodyAsBytes();
            String ksefNumber = ctx.queryParam("ksefNumber");

            if (xmlBytes == null || xmlBytes.length == 0) {
                ctx.status(400).result("Missing XML body.");
                return;
            }

            try (InputStream fopConfig = Main.class.getResourceAsStream("/fop.xconf")) {
                PdfGenerator generator = new PdfGenerator(fopConfig);

                Source src = new StreamSource(new ByteArrayInputStream(xmlBytes));
                ByteArrayOutputStream out = new ByteArrayOutputStream();

                generator.generateInvoice(src, ksefNumber, null, null, out);

                ctx.contentType(ContentType.APPLICATION_PDF);
                ctx.result(out.toByteArray());
            } catch (Exception e) {
                ctx.status(500).result("Error generating PDF: " + e.getMessage());
            }
        });
    }
}