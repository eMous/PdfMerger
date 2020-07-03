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

public class AnnotationRemover {
    public void remove(String srcPdfName, String outputPdfName) {
        try {
            PdfReader pdfReader = new PdfReader(srcPdfName);
            PdfWriter pdfWriter = new PdfWriter(outputPdfName);
            PdfDocument pdfDocument = new PdfDocument(pdfReader,pdfWriter);
            int numberOfPages = pdfDocument.getNumberOfPages();
            for (int i = 1; i <= numberOfPages; ++i){
                pdfDocument.getPage(i).getPdfObject().remove(PdfName.Annots);
            }
            pdfDocument.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void remove(String srcPdfName){
        String newName = srcPdfName.substring(0,srcPdfName.length()-4)+"_AnnotationCleared.pdf";
        remove(srcPdfName,newName);
    }
    public static void main(String[] args) {
        new AnnotationRemover().remove("/Users/tom/Desktop/1.pdf");
        new AnnotationRemover().remove("/Users/tom/Desktop/2.pdf");

        new PdfComparor().compare("/Users/tom/Desktop/1_AnnotationCleared.pdf","/Users/tom/Desktop/2_AnnotationCleared.pdf");
    }
}
