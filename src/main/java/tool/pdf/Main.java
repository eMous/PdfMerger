package tool.pdf;
import com.itextpdf.io.source.RandomAccessSourceFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.annot.PdfAnnotation;
import com.itextpdf.kernel.utils.CompareTool;
import com.itextpdf.pdfcleanup.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        String sourceFileName = "test.pdf";
        String targetFileName = "test1_al.pdf";

        String sourceFileName1 = "test.pdf";
        String sourceFileName2 = "test1.pdf";
        String targetFileName1 = "test_al.pdf";
        String targetFileName2 = "test1_al.pdf";

        PdfReader reader1 = new PdfReader(sourceFileName1);
        PdfWriter writer1 = new PdfWriter(targetFileName1);
        PdfReader reader2 = new PdfReader(sourceFileName2);
        PdfWriter writer2 = new PdfWriter(targetFileName2);

        PdfDocument document1 = new PdfDocument(reader1);
        PdfDocument document2 = new PdfDocument(reader2,writer2);

        PdfPage page1 = document1.getPage(1);
        PdfPage page2 = document2.getPage(1);
//        document2.addNewPage();
        //document2.close();
        PdfObject annotObject0 = page1.getAnnotations().get(0).getPdfObject().copyTo(document2);
        document2.getPage(1).addAnnotation(PdfAnnotation.makeAnnotation(annotObject0));


        PdfAnnotation kkkkk = page1.getAnnotations().get(0);
        PdfObject annotObject1=page1.getAnnotations().get(0).getPdfObject();
        PdfObject annotObject2=page2.getAnnotations().get(0).getPdfObject();

        String a = annotObject1.toString();
        String b = annotObject2.toString();
        System.out.println(a);
        boolean k = a.equals(b);
        document2.close();


        reader2 = new PdfReader(targetFileName2);
        writer2 = new PdfWriter("123.pdf");
        document2 = new PdfDocument(reader2,writer2);

        PdfCleanUpTool pdfCleanUpTool = new PdfCleanUpTool(document2,true);
        pdfCleanUpTool.cleanUp();
        document2.close();

        CompareTool compareTool = new CompareTool();
        try {
            compareTool.compareNames(document1.getPdfVersion().toPdfName(), document2.getPdfVersion().toPdfName());

            compareTool.compareByContent("/Users/tom/Desktop/1.pdf","/Users/tom/Desktop/2.pdf","/Users/tom/Desktop/PdfMerge");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

//        page2.addAnnotation(page1.getAnnotations().get(0));
//        document2.addNewPage(1);

        PdfWriter writer1888 = new PdfWriter(targetFileName + "_MergedVersion.pdf");
        PdfDocument writeDocument1 = new PdfDocument(writer1);
        writeDocument1.addNewPage();
        int kkk = writeDocument1.getNumberOfPages();
        writer1.close();

        PdfReader reader = new PdfReader(sourceFileName);

        PdfDocument document = new PdfDocument(reader);

        PdfReader toMergeReader = new PdfReader(new RandomAccessSourceFactory().createBestSource(targetFileName), null);
        PdfDocument toMergeDocument = new PdfDocument(toMergeReader);

        PdfWriter writer = new PdfWriter(targetFileName + "_MergedVersion.pdf");
        PdfDocument writeDocument = new PdfDocument(writer);

        int pageCount = toMergeDocument.getNumberOfPages();
        for (int i = 1; i <= pageCount; i++) {
            PdfPage page = document.getPage(i);
            writeDocument.addPage(page.copyTo(writeDocument));
            PdfPage pdfPage = toMergeDocument.getPage(i);
            List<PdfAnnotation> pageAnnots = pdfPage.getAnnotations();
            if (pageAnnots != null) {
                for (PdfAnnotation pdfAnnotation : pageAnnots) {
                    PdfObject annotObject = pdfAnnotation.getPdfObject().copyTo(writeDocument);
                    writeDocument.getPage(i).addAnnotation(PdfAnnotation.makeAnnotation(annotObject));
                }
            }
        }
        reader.close();
        toMergeReader.close();
        toMergeDocument.close();
        document.close();
        writeDocument.close();
        writer.close();

    }
}
