package tool.pdf;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.utils.CompareTool;

import java.io.File;

public class PdfComparor {
    public static void main(String[] args) {
        new PdfComparor().compare("/Users/tom/Desktop/1_AnnotationCleared.pdf","/Users/tom/Desktop/2_AnnotationCleared.pdf");
    }

    public float compare(String pdfPathA, String pdfPathB) {
        try {
            PdfReader pdfReaderA = new PdfReader(pdfPathA);
            PdfReader pdfReaderB = new PdfReader(pdfPathB);
            PdfDocument pdfDocumentA = new PdfDocument(pdfReaderA);
            PdfDocument pdfDocumentB = new PdfDocument(pdfReaderB);
            String kk = (new File(pdfPathA)).getAbsolutePath();
            CompareTool compareTool = new CompareTool();

            compareTool.compareByContent((new File(pdfPathA)).getAbsolutePath()
                    , (new File(pdfPathB)).getAbsolutePath(), "/Users/tom/Desktop/PdfMerge");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 1;
    }
}
