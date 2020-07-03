package tool.pdf;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.annot.PdfAnnotation;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import info.debatty.java.stringsimilarity.Cosine;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class PdfTools {
    final static int YES = 1;
    final static int NO = -1;
    final static int UNCERTAIN = 0;

    final static String UNCERTAIN_PREFIX = "UNCERTAIN_";


    final static String TK_ROOT_DIR = "/tmp/PdfTool/";
    final static String DISTINGUISH_DIR = TK_ROOT_DIR + "/Distinguish/";

    final static String MAPPING_JSON_FILE_NAME = "mapping.json";

    final static String COMBINE_OUTPUT_DIR_NAME = "/output/";

    // command line config
    static int file_size_threshold = 1024 * 5;
    static double similarity_threshold = 0.95;
    // if similarity result < similarity_threshold && similarity result > uncertain_factor * similarity_threshold,
    // it is viewed as uncertain.
    static double uncertain_factor = 0.8;
    static boolean enable_uncertain = false;
    static int maxNumberOfPagesCounted = 4;
    static final int minNumberOfPagesCounted = 1;


    public static int getPdfNumberOfPages(String pdfFilePath) {
        int num = -1;
        if (new File(pdfFilePath).length() < file_size_threshold) {
            return num;
        }
        try {
            PdfReader pdfReader = new PdfReader(pdfFilePath);
            PdfDocument pdfDocument = new PdfDocument(pdfReader);
            num = pdfDocument.getNumberOfPages();
            pdfDocument.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return num;
    }

    public static ArrayList<String> extractAllPagesText(String pdfFilePath) {
        ArrayList<String> ret = new ArrayList<>();
        if (new File(pdfFilePath).length() < file_size_threshold) {
            ret.add("");
            return ret;
        }
        try {
            PdfReader pdfReader = new PdfReader(pdfFilePath);
            PdfDocument pdfDocument = new PdfDocument(pdfReader);
            int numberOfPages = pdfDocument.getNumberOfPages();
            for (int i = 1; i <= numberOfPages; ++i) {
                String text = PdfTextExtractor.getTextFromPage(pdfDocument.getPage(i));
                ret.add(text);
            }
            pdfDocument.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static String extractPageText(String pdfFilePath, int pageNumber) throws Exception {
        String ret = "";
        if (new File(pdfFilePath).length() < file_size_threshold) {
            return ret;
        }
        try {
            PdfReader pdfReader = new PdfReader(pdfFilePath);
            PdfDocument pdfDocument = new PdfDocument(pdfReader);
            int numberOfPages = pdfDocument.getNumberOfPages();
            if (pageNumber <= 0 || pageNumber > numberOfPages) {
                pdfDocument.close();
                throw new Exception("Wrong Page Number.");
            }
            ret = PdfTextExtractor.getTextFromPage(pdfDocument.getPage(pageNumber));
            pdfDocument.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static void main(String[] args) {
        HashSet<String> dirs = new HashSet<>();
        String path = "";
        Scanner scanner = new Scanner(System.in);
        System.out.println("Input the dir pdfs in:");
        path=scanner.nextLine();
        dirs.add(path);
        HashMap groups = groupPdfs(dirs);
        simplifyGroupHashMap(groups);
        userToDistinguishHold(groups, DISTINGUISH_DIR);
        HashMap humanResult = readUserOperation(DISTINGUISH_DIR);
        HashMap toDelete = combinePdfs(groupHashMapToHashSet(humanResult), TK_ROOT_DIR);
        //commitChangeToOriginalFilesReadPath(TK_ROOT_DIR);
    }

    public static void userToDistinguishHold(HashMap<Integer, HashSet<HashSet<String>>> machineGroupResult, String softLinkDir) {
        userToDistinguish(machineGroupResult, softLinkDir);
        System.out.printf("Plz go to %s to view the similarity result, and make some modification.\n", softLinkDir);
        System.out.printf("After modification, press any key and a enter key if agreeing to merge. \n");
        Scanner scanner = new Scanner(System.in);
        scanner.next();
    }

    public static double similarityOfContent(String pdfFilePathA, String pdfFilePathB) {
        int numberOfPagesCountedMax = maxNumberOfPagesCounted;
        int numberOfPagesCountedMin = minNumberOfPagesCounted;

        int maxPageNumber = Math.min(getPdfNumberOfPages(pdfFilePathA), getPdfNumberOfPages(pdfFilePathB));
        int number = maxPageNumber / 5;

        if (number > numberOfPagesCountedMax) {
            number = numberOfPagesCountedMax;
        } else if (number < numberOfPagesCountedMin) {
            number = numberOfPagesCountedMin;
        }
        return similarityOfContent(pdfFilePathA, pdfFilePathB, number, true);
    }


    public static double similarityOfContent(String pdfFilePathA, String pdfFilePathB, int numberOfPagesCounted, boolean random) {
        if (numberOfPagesCounted <= 0) {
            return 0.0000000000001F;
        }
        int maxPageNumber = Math.min(getPdfNumberOfPages(pdfFilePathA), getPdfNumberOfPages(pdfFilePathB));
        int numberOfPageToTest = Math.min(maxPageNumber, numberOfPagesCounted);


        ArrayList<Integer> pageIndexes = new ArrayList<>();
        for (int i = 1; i <= maxPageNumber; ++i) {
            pageIndexes.add(i);
        }
        if (random) {
            Collections.shuffle(pageIndexes);
        }

        double results = 0;

        for (int i = 1; i <= numberOfPageToTest; ++i) {
            try {
                String strA = extractPageText(pdfFilePathA, pageIndexes.get(i - 1));
                String strB = extractPageText(pdfFilePathB, pageIndexes.get(i - 1));
                if (strA.length() < 100 || strB.length() < 100) {
                    return 0.0000000000001F;
                }
                Cosine instance = new Cosine();
                double similarity = instance.similarity(strA, strB);
                results += similarity;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        results /= numberOfPageToTest;
        return results;
    }


    public static double similarityOfContent(String pdfFilePathA, String pdfFilePathB, int numberOfPagesCounted) {
        return similarityOfContent(pdfFilePathA, pdfFilePathB, numberOfPagesCounted, true);
    }

    public static String removeAnnotation(String srcPdfName, String outputPdfName) {
        try {
            PdfReader pdfReader = new PdfReader(srcPdfName);
            PdfWriter pdfWriter = new PdfWriter(outputPdfName);
            PdfDocument pdfDocument = new PdfDocument(pdfReader, pdfWriter);
            int numberOfPages = pdfDocument.getNumberOfPages();
            for (int i = 1; i <= numberOfPages; ++i) {
                pdfDocument.getPage(i).getPdfObject().remove(PdfName.Annots);
            }
            pdfDocument.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputPdfName;
    }

    public static String removeAnnotation(String srcPdfName) {
        String newName = "/tmp/PdfTool/ClearAnnotation/" + srcPdfName;
        newName = newName.substring(0, newName.length() - 4) + "_AnnotationCleared.pdf";
        new File(newName).getAbsoluteFile().getParentFile().mkdirs();
        return removeAnnotation(srcPdfName, new File(newName).getAbsolutePath());
    }

    public static int samePdf(String pdfFilePathA, String pdfFilePathB, double threshold, double uncertainFactor) {
        if (getPdfNumberOfPages(pdfFilePathA) != getPdfNumberOfPages(pdfFilePathB)) {
            return NO;
        }
        String pdfFilePathAClean = removeAnnotation(pdfFilePathA);
        String pdfFilePathBClean = removeAnnotation(pdfFilePathB);
        double similarity = similarityOfContent(pdfFilePathAClean, pdfFilePathBClean);
        new File(pdfFilePathAClean).delete();
        new File(pdfFilePathBClean).delete();
        if (threshold > similarity) {
            if (threshold * uncertainFactor > similarity) {
                return NO;
            } else {
                return UNCERTAIN;
            }
        }
        return YES;
    }

    public static int samePdf(String pdfFilePathA, String pdfFilePathB) {
        return samePdf(pdfFilePathA, pdfFilePathB, similarity_threshold, uncertain_factor);
    }

    // [k]: combinedPdfPath
    // [v]: HashSet<String> filesToDelete
    public static HashMap<String, HashSet<String>> combinePdfs(HashSet<HashSet<String>> arrayListOfSimilarPdfPaths, String outputPathAsArg) {
        HashMap<String, HashSet<String>> retHash = new HashMap<String, HashSet<String>>();
        File outputDir = new File(outputPathAsArg + "/" + COMBINE_OUTPUT_DIR_NAME);
        outputDir.getAbsoluteFile().mkdirs();
        try {
            FileUtils.forceDelete(outputDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        outputDir.getAbsoluteFile().mkdirs();

        final String SUFFIX = "_Combined.pdf";
        for (HashSet<String> setToCombine : arrayListOfSimilarPdfPaths) {
            //setToCombine.removeIf(str -> str.startsWith(UNCERTAIN_PREFIX));
            if (setToCombine.size() <= 1) {
                continue;
            }
            Iterator it = setToCombine.iterator();
            String firstOriginalPathToCombine = (String) it.next();
            String firstBaseFileNameToCombine = new File(firstOriginalPathToCombine).getName();
            String combinedPath = outputDir + "/" + firstBaseFileNameToCombine.substring(0, firstBaseFileNameToCombine.length() - 4) + SUFFIX;
            while (new File(combinedPath).exists()) {
                combinedPath = combinedPath.substring(0, firstBaseFileNameToCombine.length() - 4) + "(other).pdf";
            }


            new File(combinedPath).getAbsoluteFile().getParentFile().mkdirs();
            combinedPath = new File(combinedPath).getAbsolutePath();
            try {
                PdfReader reader1 = new PdfReader(firstOriginalPathToCombine);
                PdfWriter writer1 = new PdfWriter(combinedPath);
                PdfDocument resultDocument = new PdfDocument(reader1, writer1);
                int resultDocumentNumberOfPages = resultDocument.getNumberOfPages();
                ArrayList<PdfDocument> otherToMergePdfsDocuments = new ArrayList<>();
                boolean samePageNumbers = true;
                // first src is assigned to resultDocument
                while (it.hasNext()) {
                    PdfReader otherToMergeReader = new PdfReader((String) it.next());
                    PdfDocument otherToMergePdfsDocument = new PdfDocument(otherToMergeReader);
                    int otherToMergePdfsDocumentPageNumber = otherToMergePdfsDocument.getNumberOfPages();
                    if (otherToMergePdfsDocumentPageNumber != resultDocumentNumberOfPages) {
                        samePageNumbers = false;
                        break;
                    }
                    otherToMergePdfsDocuments.add(otherToMergePdfsDocument);
                }
                if (!samePageNumbers) {
                    resultDocument.close();
                    new File(combinedPath).delete();
                    for (PdfDocument eachOtherToMergePdfsDocument : otherToMergePdfsDocuments) {
                        eachOtherToMergePdfsDocument.close();
                    }
                    continue;
                }
                // Iterate pages
                for (int j = 1; j <= resultDocumentNumberOfPages; j++) {
                    PdfPage resultDocumentPage = resultDocument.getPage(j);
                    HashSet<String> resultDocumentPageAnnotationsStr = new HashSet<>();
                    for (PdfAnnotation eachAnnotation :
                            resultDocumentPage.getAnnotations()) {
                        resultDocumentPageAnnotationsStr.add(eachAnnotation.getPdfObject().toString());
                        //System.out.println(eachAnnotation.getPdfObject().toString());
                    }
                    //System.out.println("-------");

                    for (int k = 0; k < otherToMergePdfsDocuments.size(); k++) {
                        PdfPage eachToMergePdfsDocumentPage = otherToMergePdfsDocuments.get(k).getPage(j);
                        List<PdfAnnotation> otherToMergePdfsPageAnnotations = eachToMergePdfsDocumentPage.getAnnotations();
                        for (PdfAnnotation eachAnnotation :
                                otherToMergePdfsPageAnnotations) {
                            String eachToMergeDocumentPageAnnotationStr = eachAnnotation.getPdfObject().toString();
                            //System.out.println(eachToMergeDocumentPageAnnotationStr);
                            if (!resultDocumentPageAnnotationsStr.contains(eachToMergeDocumentPageAnnotationStr)) {
                                resultDocumentPageAnnotationsStr.add(eachToMergeDocumentPageAnnotationStr);
                                PdfObject annotationObject = eachAnnotation.getPdfObject().copyTo(resultDocument);
                                resultDocumentPage.addAnnotation(PdfAnnotation.makeAnnotation(annotationObject));
                            }
                        }
                    }
                }
                resultDocument.close();
                for (PdfDocument eachOtherToMergePdfsDocument : otherToMergePdfsDocuments) {
                    eachOtherToMergePdfsDocument.close();
                }
                retHash.put(combinedPath, setToCombine);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            FileWriter fileWriter = new FileWriter(outputDir + "/" + MAPPING_JSON_FILE_NAME);
            GsonBuilder gsonBuilder = new GsonBuilder();
            gsonBuilder.create().toJson(retHash, fileWriter);
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.printf("Pdf files has finished being combined. Plz go to %s to view the results.\n", outputDir);

        return retHash;
    }

    public static HashMap<Integer, HashSet<HashSet<String>>> groupPdfs(HashSet<String> srcPaths) {
        HashSet<String> allPdfFiles = new HashSet<>();
        for (String path :
                srcPaths) {
            Collection<File> files = FileUtils.listFiles(
                    new File(path),
                    new RegexFileFilter("^.*[.][Pp][Dd][Ff]$"),
                    DirectoryFileFilter.DIRECTORY
            );
            for (File file :
                    files) {
                allPdfFiles.add(file.getAbsolutePath());
            }
        }
        // [k] -> numberOfPage
        // [v] -> pathSet
        HashMap<Integer, HashSet<String>> pageNumberMap = new HashMap<>();
        HashSet<String> badFilePath = new HashSet<>();
        for (String filePath :
                allPdfFiles) {
            System.out.println(filePath);
            int numberOfPage = getPdfNumberOfPages(filePath);
            if (numberOfPage == -1) {
                badFilePath.add(filePath);
                continue;
            }
            if (pageNumberMap.containsKey(numberOfPage)) {
                pageNumberMap.get(numberOfPage).add(filePath);
            } else {
                HashSet<String> specificPageNumberSet = new HashSet<>();
                specificPageNumberSet.add(filePath);
                pageNumberMap.put(numberOfPage, specificPageNumberSet);
            }
        }


        HashMap<Integer, HashSet<HashSet<String>>> contentMapByPageNumber = new HashMap<>();
        Set<Integer> allPageNumbers = pageNumberMap.keySet();
        for (Integer pageNumber :
                allPageNumbers) {

            HashSet<String> paths = pageNumberMap.get(pageNumber);
            //........
//            if (paths.size() <= 1) {
//                continue;
//            }
            HashSet<HashSet<String>> contentSetsSamePageNumber = new HashSet<>();
            contentMapByPageNumber.put(pageNumber, contentSetsSamePageNumber);

            for (String path :
                    paths) {
                if (contentSetsSamePageNumber.isEmpty()) {
                    HashSet<String> theFirstContentSet = new HashSet<>();
                    theFirstContentSet.add(path);
                    contentSetsSamePageNumber.add(theFirstContentSet);
                } else {
                    boolean findCommon = false;
                    for (HashSet<String> eachContentSet :
                            contentSetsSamePageNumber
                    ) {
                        Iterator<String> setIterator = eachContentSet.iterator();
                        String randomPdfPathInSet = setIterator.next();
                        while (randomPdfPathInSet.startsWith(UNCERTAIN_PREFIX) && setIterator.hasNext()) {
                            randomPdfPathInSet = setIterator.next();
                        }
                        System.out.printf("Compare %s with %s \n", path, randomPdfPathInSet);
                        int result = samePdf(path, randomPdfPathInSet);
                        if (result == YES) {
                            findCommon = true;
                            eachContentSet.add(path);
                            break;
                        } else if (result == UNCERTAIN) {
                            if (enable_uncertain) {
                                eachContentSet.add(UNCERTAIN_PREFIX + path);
                            }
                        }
                    }
                    if (!findCommon) {
                        HashSet<String> anotherContentSet = new HashSet<>();
                        anotherContentSet.add(path);
                        contentSetsSamePageNumber.add(anotherContentSet);
                    }
                }
            }

            // Delete redundant uncertain path, if the uncertain paths have already gotten common files.
            for (HashSet<String> eachSet : contentSetsSamePageNumber
            ) {
                Iterator<String> setIterator = eachSet.iterator();
                while (setIterator.hasNext()) {
                    String path = setIterator.next();
                    if (!path.startsWith(UNCERTAIN_PREFIX)) {
                        continue;
                    }
                    for (HashSet<String> otherEachSet : contentSetsSamePageNumber
                    ) {
                        if (eachSet.equals(otherEachSet)) {
                            continue;
                        }
                        if (!otherEachSet.contains(path.substring(UNCERTAIN_PREFIX.length()))) {
                            continue;
                        }
                        boolean uncertainIsRedundant = false;
                        int num = 0;
                        for (String pathInOtherEachSet : otherEachSet
                        ) {
                            if (!pathInOtherEachSet.startsWith(UNCERTAIN_PREFIX)) {
                                num++;
                                if (num == 2) {
                                    uncertainIsRedundant = true;
                                    break;
                                }
                            }
                        }
                        if (uncertainIsRedundant) {
                            setIterator.remove();
                            break;
                        }
                    }
                }
            }
        }
        HashSet<HashSet<String>> badPdfSets = new HashSet<>();
        if (badFilePath.size() > 0) {
            badPdfSets.add(badFilePath);
            contentMapByPageNumber.put(-1, badPdfSets);
        }
        return contentMapByPageNumber;
    }

    public static HashMap<Integer, HashSet<HashSet<String>>> jsonOutPutMachineGroupResult(HashMap<Integer, HashSet<HashSet<String>>> machineGroupResult, String jsonOutputPath, boolean multi) {

        if (multi) {
            simplifyGroupHashMap(machineGroupResult);
        }
        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        String fileName = jsonOutputPath + "/groupResult.json";
        File file = new File(fileName);
        file.getParentFile().getAbsoluteFile().mkdirs();
        try {
            FileWriter fileWriter = new FileWriter(file);
            gson.toJson(machineGroupResult, fileWriter);
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return machineGroupResult;
    }

    public static void simplifyGroupHashMap(HashMap<Integer, HashSet<HashSet<String>>> machineGroupResult) {
        Iterator<Integer> itEachPageNumber = machineGroupResult.keySet().iterator();
        while (itEachPageNumber.hasNext()) {
            Integer pageNumber = itEachPageNumber.next();
            machineGroupResult.get(pageNumber).removeIf(set -> set.size() <= 1);
            if (machineGroupResult.get(pageNumber).isEmpty()) {
                itEachPageNumber.remove();
            }
        }
    }

    public static HashSet<HashSet<String>> groupHashMapToHashSet(HashMap<Integer, HashSet<HashSet<String>>> groupResult) {
        HashSet<HashSet<String>> ret = new HashSet<>();
        for (Integer numberOfPage :
                groupResult.keySet()) {
            if (numberOfPage <= 0) {
                continue;
            }
            for (HashSet<String> set :
                    groupResult.get(numberOfPage)) {
                ret.add(set);
            }
        }
        return ret;
    }

    public static void userToDistinguish(HashMap<Integer, HashSet<HashSet<String>>> machineGroupResult, String softLinkDir) {
        int folderNum = 0;
        for (Integer numberOfPage :
                machineGroupResult.keySet()) {
            if (numberOfPage <= 0) {
                continue;
            }
            HashSet<HashSet<String>> setsOfThisPageNumber = machineGroupResult.get(numberOfPage);
            for (HashSet<String> commonSet : setsOfThisPageNumber
            ) {
                boolean findUncertainFlag = false;
                for (String path : commonSet
                ) {
                    if (path.startsWith(UNCERTAIN_PREFIX)) {
                        findUncertainFlag = true;
                        break;
                    }
                }
                folderNum++;
                String dirName = "";
                if (findUncertainFlag) {
                    dirName = softLinkDir + "/" + "***_" + folderNum;
                } else {
                    dirName = softLinkDir + "/" + folderNum;
                }
                File dir = new File(dirName);
                dir.getAbsoluteFile().mkdirs();
                try {
                    FileUtils.forceDelete(dir);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                dir.getAbsoluteFile().mkdirs();

                dirName = dir.getAbsolutePath();
                int counter = 0;
                HashMap<String, HashMap<String, String>> toReadJson = new HashMap<>();
                toReadJson.put("Certain", new HashMap<String, String>());
                toReadJson.put("Uncertain", new HashMap<String, String>());
                for (String path : commonSet
                ) {
                    ++counter;
                    String baseNameOfLink = "" + counter;
                    String linkPath = "";

                    if (path.startsWith(UNCERTAIN_PREFIX)) {
                        baseNameOfLink = UNCERTAIN_PREFIX + baseNameOfLink;
                        linkPath = dirName + "/" + baseNameOfLink + ".pdf";
                        toReadJson.get("Uncertain").put(linkPath, path.substring(UNCERTAIN_PREFIX.length()));

                    } else {
                        linkPath = dirName + "/" + baseNameOfLink + ".pdf";
                        toReadJson.get("Certain").put(linkPath, path);
                    }
                    try {
                        new File(linkPath).delete();
                        if (path.startsWith(UNCERTAIN_PREFIX)) {
                            Files.createSymbolicLink(Paths.get(linkPath), Paths.get(path.substring(UNCERTAIN_PREFIX.length())));
                        } else {
                            Files.createSymbolicLink(Paths.get(linkPath), Paths.get(path));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                GsonBuilder gsonBuilder = new GsonBuilder();
                Gson gson = gsonBuilder.create();
                File file = new File(dirName + "/" + MAPPING_JSON_FILE_NAME);
                try {
                    FileWriter fileWriter = new FileWriter(file);
                    gson.toJson(toReadJson, fileWriter);
                    fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static HashMap<Integer, HashSet<HashSet<String>>> readUserOperation(String softLinkDir) {
        HashMap<Integer, HashSet<HashSet<String>>> ret = new HashMap<>();
        File dir = new File(softLinkDir);
        for (File eachSetDir : dir.listFiles()) {
            File jsonFile = new File(eachSetDir.getAbsolutePath() + "/" + MAPPING_JSON_FILE_NAME);
            if (!jsonFile.exists()) {
                continue;
            }
            File[] files = eachSetDir.listFiles((directory, name) -> name.toLowerCase().endsWith(".pdf"));
            if (files.length <= 1) {
                continue;
            }
            HashSet<String> commonSet = new HashSet<>();
            Integer pageNum = -1;
            for (File file : files) {
                try {
                    Scanner scanner = new Scanner(jsonFile);
                    String json = scanner.nextLine();
                    GsonBuilder gsonBuilder = new GsonBuilder();
                    HashMap<String, HashMap<String, String>> groupInfo = gsonBuilder.create().fromJson(json, new TypeToken<HashMap<String, HashMap<String, String>>>() {
                    }.getType());
                    String fileName = file.getName();
                    String linkAbsolutePath = file.getAbsolutePath();
                    String realPath = "";
                    if (fileName.startsWith(UNCERTAIN_PREFIX)) {
                        realPath = groupInfo.get("Uncertain").get(linkAbsolutePath);
                    } else {
                        realPath = groupInfo.get("Certain").get(file.getAbsolutePath());
                    }
                    commonSet.add(realPath);
                    System.out.println(realPath);
                    pageNum = getPdfNumberOfPages(realPath);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
            if (!ret.containsKey(pageNum)) {
                ret.put(pageNum, new HashSet<>());
            }
            ret.get(pageNum).add(commonSet);
        }
        for (File eachSetDir : dir.listFiles()) {
            try {
                FileUtils.forceDelete(eachSetDir);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("User modifications is read.");
        return ret;
    }

    public static void commitChangeToOriginalFilesReadPath(String outputPathAsArg) {

        File[] files = new File(outputPathAsArg + "/" + COMBINE_OUTPUT_DIR_NAME).listFiles((directory, name) -> name.equals(MAPPING_JSON_FILE_NAME));
        if (files.length != 1) {
            System.out.println("Commit failed, " + MAPPING_JSON_FILE_NAME + " json file not found.");
            return;
        }
        try {
            Scanner scanner = new Scanner(files[0]);
            String json = scanner.nextLine();
            GsonBuilder gsonBuilder = new GsonBuilder();
            HashMap<String, HashSet<String>> hashMapToDelete = gsonBuilder.create().fromJson(json, new TypeToken<HashMap<String, HashSet<String>>>() {
            }.getType());
            moveAndDelete(hashMapToDelete);
            files[0].delete();
            System.out.println("Changes Committed.");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void moveAndDelete(HashMap<String, HashSet<String>> hashMapToDelete) {
        for (String combinedPath : hashMapToDelete.keySet()
        ) {
            boolean hasMoved = false;
            for (String pathToDelete : hashMapToDelete.get(combinedPath)
            ) {
                new File(pathToDelete).delete();
                new File(pathToDelete).getAbsoluteFile().getParentFile().mkdirs();

                if (!hasMoved) {
                    try {
                        Files.move(Paths.get(combinedPath), Paths.get(pathToDelete));
                        hasMoved = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}