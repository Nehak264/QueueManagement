package com.tkiet.qms.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {

    public byte[] generateBonafidePdf(String name, String rollNumber, String className, 
                                     String division, String purpose, String academicYear, 
                                     String refNumber) {
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, out);
            
            // Add watermark event
            writer.setPageEvent(new PdfPageEventHelper() {
                @Override
                public void onEndPage(PdfWriter writer, Document document) {
                    PdfContentByte canvas = writer.getDirectContentUnder();
                    Phrase watermark = new Phrase("DIGITALLY VERIFIED — TKIET", 
                                       new Font(Font.FontFamily.HELVETICA, 40, Font.BOLD, new BaseColor(220, 220, 220)));
                    ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, watermark, 297, 421, 45);
                }
            });

            document.open();

            // Header
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
            Font subHeaderFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL);
            Font boldFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);

            Paragraph collegeName = new Paragraph("Tatyasaheb Kore Institute of Engineering and Technology", headerFont);
            collegeName.setAlignment(Element.ALIGN_CENTER);
            document.add(collegeName);

            Paragraph deptName = new Paragraph("Department: Computer Science and Engineering", subHeaderFont);
            deptName.setAlignment(Element.ALIGN_CENTER);
            document.add(deptName);

            Paragraph address = new Paragraph("Warananangar, Tal. Peth, Dist. Kolhapur, Maharashtra - 416113", normalFont);
            address.setAlignment(Element.ALIGN_CENTER);
            document.add(address);

            // Double Horizontal Line
            document.add(new Paragraph(" "));
            PdfContentByte cb = writer.getDirectContent();
            cb.setLineWidth(1f);
            cb.moveTo(36, document.getPageSize().getTop() - 110);
            cb.lineTo(559, document.getPageSize().getTop() - 110);
            cb.stroke();
            cb.moveTo(36, document.getPageSize().getTop() - 112);
            cb.lineTo(559, document.getPageSize().getTop() - 112);
            cb.stroke();

            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));

            // Title
            Paragraph title = new Paragraph("BONAFIDE CERTIFICATE", new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD | Font.UNDERLINE));
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph(" "));

            // Ref and Date
            PdfPTable refDateTable = new PdfPTable(2);
            refDateTable.setWidthPercentage(100);
            refDateTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);
            
            refDateTable.addCell(new Phrase("Ref: " + refNumber, normalFont));
            PdfPCell dateCell = new PdfPCell(new Phrase("Date: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), normalFont));
            dateCell.setBorder(Rectangle.NO_BORDER);
            dateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            refDateTable.addCell(dateCell);
            
            document.add(refDateTable);
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));

            // Body
            Paragraph body = new Paragraph();
            body.setLeading(20f);
            body.add(new Chunk("This is to certify that Mr./Ms. ", normalFont));
            body.add(new Chunk(name.toUpperCase(), boldFont));
            body.add(new Chunk(" is a bonafide student of this institute, studying in ", normalFont));
            body.add(new Chunk(className + " (" + division + ")", boldFont));
            body.add(new Chunk(" class of ", normalFont));
            body.add(new Chunk("Computer Science and Engineering", boldFont));
            body.add(new Chunk(" department during the academic year ", normalFont));
            body.add(new Chunk(academicYear, boldFont));
            body.add(new Chunk(". His/Her Roll Number is ", normalFont));
            body.add(new Chunk(rollNumber, boldFont));
            body.add(new Chunk(".", normalFont));
            document.add(body);

            document.add(new Paragraph(" "));
            Paragraph purposePara = new Paragraph("This certificate is issued for the purpose of " + purpose + ".", normalFont);
            document.add(purposePara);

            // Signature Blocks
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));
            document.add(new Paragraph(" "));

            PdfPTable signTable = new PdfPTable(3);
            signTable.setWidthPercentage(100);
            signTable.getDefaultCell().setBorder(Rectangle.NO_BORDER);
            signTable.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);

            signTable.addCell(new Phrase("Class Teacher", boldFont));
            signTable.addCell(new Phrase("Head of Department", boldFont));
            signTable.addCell(new Phrase("Principal", boldFont));

            document.add(signTable);

            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
