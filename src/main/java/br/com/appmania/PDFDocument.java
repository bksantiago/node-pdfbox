package br.com.appmania;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.COSObjectable;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionJavaScript;
import org.apache.pdfbox.pdmodel.interactive.action.PDAnnotationAdditionalActions;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.pdmodel.interactive.form.*;
import sun.misc.BASE64Decoder;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by PedroLucas on 3/9/16.
 */
public class PDFDocument {

    private PDDocument document;
    private String path;

    public PDFDocument(PDDocument doc) {
        this.document = doc;
    }

    public PDFDocument(String path, boolean isUrl) throws IOException {
        this.path = path;
        if (!isUrl) {
            this.document = PDDocument.load(new File(path));
        } else {
            InputStream is = null;
            try {
                is = new URL(path).openStream();
                this.document = PDDocument.load(is);
            } finally {
                is.close();
            }
        }
    }

    public static PDFDocument load(String path) throws IOException {
        return new PDFDocument(path, false);
    }

    public static PDFDocument load(String path, boolean isUrl) throws IOException {
        return new PDFDocument(path, isUrl);
    }

    public int pagesCount() {
        return document.getNumberOfPages();
    }

    public PDFPage getPage(int index) {
        return new PDFPage(document, index);
    }

    public PDDocument getDocument() {
        return document;
    }

    public String getInformation(String name) {
        Object value = document.getDocumentInformation().getPropertyStringValue(name);
        return value == null ? "" : value.toString();
    }

    public PDDocumentInformation getInformation() {
        return document.getDocumentInformation();
    }

    public void addPage(String file, int page, int insertAt) throws IOException {
        addPage(PDFDocument.load(file).getPage(page), insertAt);
    }

    public void addPage(PDFPage page) throws IOException {
        document.addPage(page.getPage());
    }

    public void addPage(PDFPage page, int insertAt) {
        int pagesCount = document.getNumberOfPages();
        if(insertAt >= pagesCount) {
            document.addPage(page.getPage());
        }else{
            document.getPages().insertBefore(page.getPage(), document.getPage(insertAt));
        }
    }

    public void addPages(String file) throws IOException {
        addPages(PDFDocument.load(file));
    }
    public void addPages(String file, int insertAt) throws IOException {
        addPages(PDFDocument.load(file), insertAt);
    }

    public void addPages(PDFDocument doc) {
        addPages(doc, doc.pagesCount());
    }

    public void addPages(PDFDocument doc, int insertAt) {
        addPages(doc, 0, doc.pagesCount(), insertAt);
    }

    public void addPages(PDFDocument doc, int start, int end, int insertAt) {
        int inc = insertAt;
        for(int i=start;i<end;i++) {
            addPage(doc.getPage(i), inc);
            inc++;
        }
    }

    public void save() throws IOException {
        document.save(new File(path));
    }

    public void save(String path) throws IOException {
        document.save(new File(path));
    }

    public String getPath() {
        return path;
    }

    public void close() throws IOException {
        document.close();
    }

    public void flatten() throws IOException {

        PDDocumentCatalog docCatalog = document.getDocumentCatalog();
        PDAcroForm acroForm = docCatalog.getAcroForm();

        Iterator<PDField> fields = acroForm.getFieldIterator();

        while (fields.hasNext()) {
            PDField field = fields.next();
            field.setReadOnly(true);
        }

        AccessPermission ap = new AccessPermission();
        ap.setCanModify(false);
        ap.setReadOnly();
        StandardProtectionPolicy spp = new StandardProtectionPolicy(null, null, ap);

        document.protect(spp);

    }

    public String loadFontToForm(String fontLocation) throws IOException {
        PDType0Font font = PDType0Font.load(document, new File(fontLocation));
        PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
        COSName cos = acroForm.getDefaultResources().add(font);
        return cos.getName();
    }

    public void fillField(String key, String value, String cosName) throws IOException {
        PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
        PDField field = acroForm.getField(key);

        if (field instanceof PDTextField && cosName != null) {
            PDTextField textField = ((PDTextField) field);
            String defaultAppearance = textField.getDefaultAppearance();

            if (defaultAppearance != null && defaultAppearance.contains("/")) {
                textField.setDefaultAppearance("/" + cosName + defaultAppearance.substring(defaultAppearance.indexOf(" ")));
            }
        }

        if (field instanceof PDPushButton) {
            this.fillSignature(key, value);
            return;
        }

        field.setValue(value);
    }

    public void fillSignature(String fieldId, String base64Img) throws IOException {
        PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
        PDField field = acroForm.getField(fieldId);
        if (field instanceof PDPushButton) {
            PDPushButton btn = (PDPushButton) field;

            List<PDAnnotationWidget> widgets = btn.getWidgets();

            PDAnnotationWidget annotationWidget = widgets.get(0);

            BASE64Decoder decoder = new BASE64Decoder();
            byte[] imageByte = decoder.decodeBuffer(base64Img);
            ByteArrayInputStream bis = new ByteArrayInputStream(imageByte);
            BufferedImage bufferedImage = ImageIO.read(bis);
            bis.close();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", baos);
            byte[] bytes = baos.toByteArray();
            baos.close();

            PDImageXObject pdImageXObject = PDImageXObject.createFromByteArray(document, bytes, "signature");
            float imageScaleRatio = (float) pdImageXObject.getHeight() / (float) pdImageXObject.getWidth();

            PDRectangle buttonPosition = this.getFieldArea(btn);
            float height = buttonPosition.getHeight();
            float width = height / imageScaleRatio;
            float x = buttonPosition.getLowerLeftX();
            float y = buttonPosition.getLowerLeftY();

            PDAppearanceStream pdAppearanceStream = new PDAppearanceStream(document);
            pdAppearanceStream.setResources(new PDResources());
            try (PDPageContentStream pdPageContentStream = new PDPageContentStream(document, pdAppearanceStream)) {
                pdPageContentStream.drawImage(pdImageXObject, x, y, width, height);
            }
            pdAppearanceStream.setBBox(new PDRectangle(x, y, width, height));

            PDAppearanceDictionary pdAppearanceDictionary = annotationWidget.getAppearance();
            if (pdAppearanceDictionary == null) {
                pdAppearanceDictionary = new PDAppearanceDictionary();
                annotationWidget.setAppearance(pdAppearanceDictionary);
            }

            pdAppearanceDictionary.setNormalAppearance(pdAppearanceStream);
        }
    }

    private PDRectangle getFieldArea(PDField field) {
        COSDictionary fieldDict = field.getCOSObject();
        COSArray fieldAreaArray = (COSArray) fieldDict.getDictionaryObject(COSName.RECT);
        return new PDRectangle(fieldAreaArray);
    }
}
