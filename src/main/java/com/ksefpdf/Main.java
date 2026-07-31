package com.ksefpdf;

import io.alapierre.ksef.fop.InvoiceGenerationParams;
import io.alapierre.ksef.fop.InvoiceQRCodeGeneratorRequest;
import io.alapierre.ksef.fop.InvoiceSchema;
import io.alapierre.ksef.fop.Language;
import io.alapierre.ksef.fop.PdfGenerator;
import io.javalin.Javalin;
import io.javalin.http.ContentType;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

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

                String verificationLink = "https://ksef.mf.gov.pl/web/verify/" + ksefNumber;
                InvoiceQRCodeGeneratorRequest qrRequest =
                        InvoiceQRCodeGeneratorRequest.onlineQrBuilder(verificationLink);

                InvoiceGenerationParams params = InvoiceGenerationParams.builder()
                        .schema(InvoiceSchema.FA3_1_0_E)
                        .ksefNumber(ksefNumber)
                        .invoiceQRCodeGeneratorRequest(qrRequest)
                        .language(Language.PL)
                        .build();

                ByteArrayOutputStream out = new ByteArrayOutputStream();

                generator.generateInvoice(xmlBytes, params, out);

                ctx.contentType(ContentType.APPLICATION_PDF);
                ctx.result(out.toByteArray());
            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result("Error generating PDF: " + e.getMessage());
            }
        });
    }
}
