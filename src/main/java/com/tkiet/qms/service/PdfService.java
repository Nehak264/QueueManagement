package com.tkiet.qms.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Map;

@Service
public class PdfService {

    // ── Load image from classpath ────────────────────────────────────────────
    private Image loadImage(String classpathPath) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(classpathPath)) {
            if (is == null) {
                System.err.println("Image not found: " + classpathPath);
                return null;
            }
            return Image.getInstance(is.readAllBytes());
        } catch (Exception e) {
            System.err.println("Failed to load image " + classpathPath + ": " + e.getMessage());
            return null;
        }
    }

    // ── Generate QR Code as PNG bytes ────────────────────────────────────────
    private byte[] generateQrCode(String content) throws WriterException, IOException {
        QRCodeWriter writer = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, 250, 250, hints);
        ByteArrayOutputStream pngOut = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", pngOut);
        return pngOut.toByteArray();
    }

    // ── Helper: borderless centred cell ─────────────────────────────────────
    private PdfPCell noBorderCell(Phrase phrase) {
        PdfPCell c = new PdfPCell(phrase);
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(3f);
        return c;
    }

    private PdfPCell noBorderCell(Image img) {
        PdfPCell c = new PdfPCell(img, false);
        c.setBorder(Rectangle.NO_BORDER);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(3f);
        return c;
    }

    // ── Main PDF generator ───────────────────────────────────────────────────
    public byte[] generateBonafidePdf(String name, String rollNumber, String className,
                                      String division, String purpose, String academicYear,
                                      String refNumber) {

        Document document = new Document(PageSize.A4, 50, 50, 40, 40);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter pdfWriter = PdfWriter.getInstance(document, out);

            // ── Diagonal anti-tamper watermark ───────────────────────────────
            pdfWriter.setPageEvent(new PdfPageEventHelper() {
                @Override
                public void onEndPage(PdfWriter w, Document doc) {
                    PdfContentByte canvas = w.getDirectContentUnder();
                    Phrase wm = new Phrase("DIGITALLY VERIFIED — TKIET",
                            new Font(Font.FontFamily.HELVETICA, 42, Font.BOLD,
                                    new BaseColor(235, 235, 245)));
                    ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER,
                            wm, 297, 421, 45);
                }
            });

            document.open();

            // ─────────────────────────────────────────────────────────────────
            // FONTS
            // ─────────────────────────────────────────────────────────────────
            BaseColor navyBlue     = new BaseColor(15, 40, 100);
            Font swvsmFont        = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, new BaseColor(60, 60, 60));
            Font collegeBigFont   = new Font(Font.FontFamily.HELVETICA, 17, Font.BOLD, navyBlue);
            Font deptFont         = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, new BaseColor(30, 30, 30));
            Font addressFont      = new Font(Font.FontFamily.HELVETICA,  9, Font.NORMAL, new BaseColor(80, 80, 80));
            Font headerFont       = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, navyBlue);
            Font subHeaderFont    = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, new BaseColor(50,50,50));
            Font normalFont       = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL);
            Font boldFont         = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);
            Font smallFont        = new Font(Font.FontFamily.HELVETICA,  9, Font.NORMAL, BaseColor.DARK_GRAY);
            Font smallBoldBlue    = new Font(Font.FontFamily.HELVETICA,  9, Font.BOLD, navyBlue);
            Font tinyFont         = new Font(Font.FontFamily.HELVETICA,  8, Font.NORMAL, BaseColor.GRAY);
            Font tinyBoldBlue     = new Font(Font.FontFamily.HELVETICA,  7, Font.BOLD, navyBlue);
            Font principalFont    = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, navyBlue);
            Font designationFont  = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD);

            // ─────────────────────────────────────────────────────────────────
            // 1. HEADER  —  [LOGO] | [College name block (centered)] | [spacer]
            //    3-column: logo left, text truly centred, spacer mirrors logo
            // ─────────────────────────────────────────────────────────────────
            Image logo = loadImage("static/tkiet_logo.png");
            if (logo == null) logo = loadImage("images/tkiet_logo.png");

            PdfPTable headerTable = new PdfPTable(logo != null ? 3 : 1);
            headerTable.setWidthPercentage(100);
            if (logo != null) {
                headerTable.setWidths(new float[]{1.6f, 6f, 1.6f});
                logo.scaleToFit(90, 90);
                PdfPCell logoCell = new PdfPCell(logo, false);
                logoCell.setBorder(Rectangle.NO_BORDER);
                logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                logoCell.setPadding(4f);
                headerTable.addCell(logoCell);
            }

            // ── Centre text column ──
            PdfPCell textCell = new PdfPCell();
            textCell.setBorder(Rectangle.NO_BORDER);
            textCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            textCell.setPaddingTop(4f);
            textCell.setPaddingBottom(6f);

            Paragraph p1 = new Paragraph("SWVSM's", swvsmFont);
            p1.setAlignment(Element.ALIGN_CENTER);
            p1.setSpacingAfter(1f);
            textCell.addElement(p1);

            Paragraph p2 = new Paragraph(
                    "Tatyasaheb Kore Institute of Engineering and Technology",
                    collegeBigFont);
            p2.setAlignment(Element.ALIGN_CENTER);
            p2.setSpacingAfter(3f);
            textCell.addElement(p2);

            Paragraph p3 = new Paragraph(
                    "Department of Computer Science and Engineering", deptFont);
            p3.setAlignment(Element.ALIGN_CENTER);
            p3.setSpacingAfter(3f);
            textCell.addElement(p3);

            Paragraph p4 = new Paragraph(
                    "Warananagar, Tal. Peth, Dist. Kolhapur, Maharashtra \u2013 416113  |  Autonomous", addressFont);
            p4.setAlignment(Element.ALIGN_CENTER);
            textCell.addElement(p4);

            headerTable.addCell(textCell);

            if (logo != null) {
                // ── Right spacer mirrors logo width to keep text truly centred ──
                PdfPCell spacerCell = new PdfPCell(new Phrase(""));
                spacerCell.setBorder(Rectangle.NO_BORDER);
                headerTable.addCell(spacerCell);
            }

            document.add(headerTable);

            // ─────────────────────────────────────────────────────────────────
            // Double rule below header  (thick navy + thin navy)
            // ─────────────────────────────────────────────────────────────────
            PdfContentByte cb = pdfWriter.getDirectContent();
            float lineY = document.getPageSize().getTop() - 122;
            cb.setLineWidth(3f);
            cb.setColorStroke(navyBlue);
            cb.moveTo(50, lineY);       cb.lineTo(545, lineY);       cb.stroke();
            cb.setLineWidth(1f);
            cb.moveTo(50, lineY - 4);   cb.lineTo(545, lineY - 4);   cb.stroke();

            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));

            // ─────────────────────────────────────────────────────────────────
            // 2. TITLE
            // ─────────────────────────────────────────────────────────────────
            Paragraph title = new Paragraph("BONAFIDE CERTIFICATE",
                    new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD | Font.UNDERLINE, navyBlue));
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            // ─────────────────────────────────────────────────────────────────
            // 3. REF + DATE row
            // ─────────────────────────────────────────────────────────────────
            String issueDateStr = LocalDate.now()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

            PdfPTable refDateTable = new PdfPTable(2);
            refDateTable.setWidthPercentage(100);
            refDateTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);
            refDateTable.addCell(new Phrase("Ref No: " + refNumber, normalFont));

            PdfPCell dateCell = new PdfPCell(new Phrase("Date: " + issueDateStr, normalFont));
            dateCell.setBorder(Rectangle.NO_BORDER);
            dateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            refDateTable.addCell(dateCell);
            document.add(refDateTable);
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));

            // ─────────────────────────────────────────────────────────────────
            // 4. BODY TEXT
            // ─────────────────────────────────────────────────────────────────
            Paragraph body = new Paragraph();
            body.setLeading(22f);
            body.add(new Chunk("       This is to certify that Mr./Ms. ", normalFont));
            body.add(new Chunk(name.toUpperCase(), boldFont));
            body.add(new Chunk(" bearing Roll Number ", normalFont));
            body.add(new Chunk(rollNumber, boldFont));
            body.add(new Chunk(" is a bonafide student of this institute. He/She is studying in ", normalFont));
            body.add(new Chunk(className + " (Division " + division + ")", boldFont));
            body.add(new Chunk(" of the ", normalFont));
            body.add(new Chunk("Computer Science and Engineering", boldFont));
            body.add(new Chunk(" department during the academic year ", normalFont));
            body.add(new Chunk(academicYear, boldFont));
            body.add(new Chunk(".", normalFont));
            document.add(body);

            document.add(new Paragraph(" "));

            Paragraph purposePara = new Paragraph(
                    "       This certificate is issued for the purpose of " + purpose + ".", normalFont);
            purposePara.setLeading(20f);
            document.add(purposePara);

            document.add(new Paragraph(" "));

            Paragraph noObj = new Paragraph(
                    "       This institute has no objection to the student using this certificate for the above mentioned purpose.",
                    normalFont);
            noObj.setLeading(20f);
            document.add(noObj);

            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));

            // ─────────────────────────────────────────────────────────────────
            // 5. SIGNATURE SECTION
            //    LEFT  → Official Stamp image (no background)
            //    RIGHT → [✦ DIGITALLY SIGNED ✦]
            //             [Signature image]
            //             [_______line______]
            //             [Dr. D. N. Mane]
            //             [Principal]
            //             [TKIET, Warananagar]
            // ─────────────────────────────────────────────────────────────────
            Image stampImg = loadImage("images/tkiet_stamp.png");
            Image signImg  = loadImage("images/principal_sign.png");

            PdfPTable signSection = new PdfPTable(2);
            signSection.setWidthPercentage(100);
            signSection.setWidths(new float[]{1f, 1f});

            // ── LEFT: stamp image (PNG - transparent background) ──
            PdfPCell stampCell = new PdfPCell();
            stampCell.setBorder(Rectangle.NO_BORDER);
            stampCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            stampCell.setVerticalAlignment(Element.ALIGN_BOTTOM);
            stampCell.setPadding(5f);

            if (stampImg != null) {
                // Scale stamp to reasonable size; PNG alpha preserves transparency
                stampImg.scaleToFit(115, 115);
                stampImg.setAlignment(Element.ALIGN_CENTER);
                stampCell.addElement(stampImg);

                Paragraph stampLabel = new Paragraph("Official Stamp", tinyFont);
                stampLabel.setAlignment(Element.ALIGN_CENTER);
                stampCell.addElement(stampLabel);
            } else {
                stampCell.addElement(new Paragraph("[ Official Stamp ]", tinyFont));
            }
            signSection.addCell(stampCell);

            // ── RIGHT: signature then name ──
            PdfPCell principalCell = new PdfPCell();
            principalCell.setBorder(Rectangle.NO_BORDER);
            principalCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            principalCell.setPadding(5f);

            // "DIGITALLY SIGNED" tag
            Paragraph dsTag = new Paragraph("✦  DIGITALLY SIGNED  ✦", tinyBoldBlue);
            dsTag.setAlignment(Element.ALIGN_CENTER);
            principalCell.addElement(dsTag);

            // Signature image ABOVE the name
            if (signImg != null) {
                signImg.scaleToFit(140, 60);
                signImg.setAlignment(Element.ALIGN_CENTER);
                principalCell.addElement(signImg);
            }

            // Sign line
            Paragraph line = new Paragraph("_________________________________",
                    new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, BaseColor.GRAY));
            line.setAlignment(Element.ALIGN_CENTER);
            principalCell.addElement(line);

            // Name BELOW signature image
            Paragraph principalName = new Paragraph("Dr. D. N. Mane", principalFont);
            principalName.setAlignment(Element.ALIGN_CENTER);
            principalCell.addElement(principalName);

            Paragraph designation = new Paragraph("Principal", designationFont);
            designation.setAlignment(Element.ALIGN_CENTER);
            principalCell.addElement(designation);

            Paragraph institution = new Paragraph("TKIET, Warananagar, Kolhapur", tinyFont);
            institution.setAlignment(Element.ALIGN_CENTER);
            principalCell.addElement(institution);

            signSection.addCell(principalCell);
            document.add(signSection);

            document.add(new Paragraph(" "));

            // ─────────────────────────────────────────────────────────────────
            // 6. QR CODE SECTION
            //    LEFT  → Verification text
            //    RIGHT → QR Code image (visible picture)
            // ─────────────────────────────────────────────────────────────────
            String qrContent = String.join("\n",
                    "TKIET BONAFIDE CERTIFICATE",
                    "REF    : " + refNumber,
                    "NAME   : " + name.toUpperCase(),
                    "ROLL   : " + rollNumber,
                    "CLASS  : " + className + " (Div " + division + ")",
                    "PURPOSE: " + purpose,
                    "YEAR   : " + academicYear,
                    "ISSUED : " + issueDateStr,
                    "PRINCIPAL: Dr. D. N. Mane",
                    "VERIFY : CSE Dept, TKIET Warananagar, Kolhapur 416113"
            );

            byte[] qrBytes = generateQrCode(qrContent);
            Image qrImage  = Image.getInstance(qrBytes);
            qrImage.scaleToFit(100, 100);  // visible picture size

            PdfPTable qrTable = new PdfPTable(2);
            qrTable.setWidthPercentage(100);
            qrTable.setWidths(new float[]{3f, 1.2f});

            // LEFT: verification note
            PdfPCell verifyCell = new PdfPCell();
            verifyCell.setBorder(Rectangle.TOP);
            verifyCell.setBorderColor(new BaseColor(180, 180, 180));
            verifyCell.setPadding(8f);

            Paragraph verifyTitle = new Paragraph("Digital Verification", smallBoldBlue);
            verifyCell.addElement(verifyTitle);

            Paragraph verifyMsg = new Paragraph(
                    "This certificate has been digitally generated by the TKIET Bonafide Management System "
                  + "and bears the digital signature of the Principal, Dr. D. N. Mane. "
                  + "For manual verification contact: CSE Department, TKIET Warananagar, Kolhapur – 416113.",
                    tinyFont);
            verifyMsg.setLeading(12f);
            verifyCell.addElement(verifyMsg);
            qrTable.addCell(verifyCell);

            // RIGHT: QR code as a visible picture
            PdfPCell qrCell = new PdfPCell();
            qrCell.setBorder(Rectangle.TOP);
            qrCell.setBorderColor(new BaseColor(180, 180, 180));
            qrCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            qrCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            qrCell.setPadding(6f);

            // QR code picture
            qrImage.setAlignment(Element.ALIGN_CENTER);
            qrCell.addElement(qrImage);

            Paragraph scanLabel = new Paragraph("📷 Scan to Verify", tinyBoldBlue);
            scanLabel.setAlignment(Element.ALIGN_CENTER);
            qrCell.addElement(scanLabel);

            qrTable.addCell(qrCell);
            document.add(qrTable);

            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
