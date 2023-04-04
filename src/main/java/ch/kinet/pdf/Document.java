/*
 * Copyright (C) 2022 by Stefan Rothe
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY); without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ch.kinet.pdf;

import ch.kinet.Binary;
import ch.kinet.http.Data;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.UnitValue;
import com.itextpdf.layout.property.VerticalAlignment;

public final class Document {

    private final ByteArrayOutputStream buffer;
    private final PdfDocument pdfDoc;
    private final String fileName;
    private float borderWidth;
    private float fontSize;
    private boolean bold;
    private com.itextpdf.layout.Document page;
    private Table table;
    private com.itextpdf.layout.borders.Border border;

    public static final Document createPortrait(String fileName) {
        try {
            return new Document(fileName, false);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static final Document createLandscape(String fileName) {
        try {
            return new Document(fileName, true);
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private Document(String fileName, boolean rotate) throws Exception {
        buffer = new ByteArrayOutputStream();
        this.fileName = fileName;
        PdfWriter writer = new PdfWriter(buffer);
        pdfDoc = new PdfDocument(writer);
        PageSize pageSize = PageSize.A4;
        if (rotate) {
            pageSize = pageSize.rotate();
        }

        pdfDoc.setDefaultPageSize(pageSize);
        borderWidth = 1f;
        fontSize = 10f;
        bold = false;
        border = new SolidBorder(borderWidth);
    }

    public Data toData() {
        if (page != null) {
            page.close();
        }
        else {
            System.out.println("page is null");
        }

        return Data.pdf(Binary.from(buffer.toByteArray()), fileName);
    }

    public void addCell(Binary image, float maxWidth, float maxHeight) {
        if (table == null) {
            return;
        }

        Cell cell = new Cell();
        cell.setVerticalAlignment(VerticalAlignment.BOTTOM);
        cell.setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);
        cell.setPadding(0.5f);
        if (image != null && !image.isNull()) {
            cell.add(createImage(image, maxWidth, maxHeight));
        }

        table.addCell(cell);
    }

    public void addCell(String text, Alignment alignment, Border... borders) {
        if (table == null) {
            return;
        }

        Cell cell = new Cell();
        cell.setVerticalAlignment(VerticalAlignment.TOP);
        cell.setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);
        cell.setPadding(0.5f);
        for (Border b : borders) {
            switch (b) {
                case Bottom:
                    cell.setBorderBottom(border);
                    break;
                case Left:
                    cell.setBorderLeft(border);
                    break;
                case Right:
                    cell.setBorderRight(border);
                    break;
                case Top:
                    cell.setBorderTop(border);
                    break;
            }
        }

        cell.add(createParagraph(text, alignment));
        table.addCell(cell);
    }

    public void addImage(Binary image, float maxWidth, float maxHeight) {
        if (image == null || image.isNull()) {
            return;
        }

        page.add(createImage(image, maxWidth, maxHeight));
    }

    public void addPage(float marginLeftRight, float marginTopBottom) {
        if (page == null) {
            page = new com.itextpdf.layout.Document(pdfDoc);
        }
        else {
            page.add(new AreaBreak());
        }

        marginLeftRight = cmToPoint(marginLeftRight);
        marginTopBottom = cmToPoint(marginTopBottom);
        page.setMargins(marginTopBottom, marginLeftRight, marginTopBottom, marginLeftRight);
    }

    public void addParagraph(String text, Alignment alignment) {
        page.add(createParagraph(text, alignment));
    }

    public void beginTable(float... columnWidths) {
        table = new Table(UnitValue.createPercentArray(columnWidths));
        table.setWidth(UnitValue.createPercentValue(100));
    }

    public void endTable() {
        page.add(table);
        table = null;
    }

    public void setBold() {
        this.bold = true;
    }

    public void setNormal() {
        this.bold = false;
    }

    public void setFontSize(float fontSize) {
        this.fontSize = fontSize;
    }

    private Image createImage(Binary image, float maxWidth, float maxHeight) {
        ImageData data = ImageDataFactory.create(image.toBytes());
        Image img = new Image(data);
        img.scaleToFit(maxWidth, maxHeight);
        return img;
    }

    private Paragraph createParagraph(String text, Alignment alignment) {
        if (text == null) {
            text = "";
        }

        Paragraph result = new Paragraph(text);
        result.setTextAlignment(translate(alignment));
        result.setFontSize(fontSize);
        if (bold) {
            result.setBold();
        }

        return result;
    }

    private TextAlignment translate(Alignment align) {
        switch (align) {
            case Center:
                return TextAlignment.CENTER;
            case Left:
                return TextAlignment.LEFT;
            case Right:
                return TextAlignment.RIGHT;
            default:
                return TextAlignment.LEFT;
        }
    }

    private static float cmToPoint(float cm) {
        return cm * 28.3464566929f;
    }
}
