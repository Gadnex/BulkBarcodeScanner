/*
 * Copyright 2015 William Gadney <gadnex@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.binarypaper.barcodescanner.worker;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.GenericMultipleBarcodeReader;
import com.google.zxing.qrcode.QRCodeReader;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import net.binarypaper.barcodescanner.entity.Barcode;
import net.binarypaper.barcodescanner.entity.BarcodeType;
import net.binarypaper.barcodescanner.entity.Document;
import net.binarypaper.barcodescanner.entity.Page;
import org.eclipse.persistence.jaxb.JAXBContextFactory;

/**
 *
 * @author William Gadney <gadnex@gmail.com>
 */
public class BarcodeScanner {

    private static final Logger LOGGER = Logger.getLogger(BarcodeScanner.class.getName());
    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle("config");

    public static void main(String[] args) {
        setLogFileHandler();
        File inputFolder = new File(resourceBundle.getString("inputFolder"));
        while (true) {
            List<File> files = getFilesInFolder(inputFolder);
            for (File file : files) {
                try {
                    String fileNameLoweCase = file.getName().toLowerCase();
//                    if (fileNameLoweCase.endsWith(".pdf")) {
//                        BarcodeScanner barcodeScanner = new BarcodeScanner();
//                        Document document = barcodeScanner.scanPdfDocument(file);
//                        String outputFolder = resourceBundle.getString("outputFolder");
//                        barcodeScanner.marshalDocument(document, outputFolder, file.getName());
//                        file.delete();
//                    } else 
                    if (fileNameLoweCase.endsWith(".tif") || fileNameLoweCase.endsWith(".tiff")) {
                        BarcodeScanner barcodeScanner = new BarcodeScanner();
                        Document document = barcodeScanner.scanTiffDocument(file);
                        String outputFolder = resourceBundle.getString("outputFolder");
                        barcodeScanner.marshalDocument(document, outputFolder, file.getName());
                        file.delete();
                    } else {
                        LOGGER.log(Level.WARNING, "The input file \"{0}\" does not have the file extension .pdf, .tiff or .tif", file.getName());
                        moveInvalidInputFile(file.getName());
                    }
                } catch (FileNotFoundException ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, null, ex);
                    moveInvalidInputFile(file.getName());
                }
            }
        }
    }

    private static void setLogFileHandler() {
        // Set the Log File Handler
        try {
            String logFilePath = resourceBundle.getString("logFolder");
            File logDirectory = new File(logFilePath);
            if (!logDirectory.exists()) {
                logDirectory.mkdir();
            }
            FileHandler fileHandler = new FileHandler(logFilePath + "LogFile.%u.%g.log", 1024 * 1024, 100);
            fileHandler.setFormatter(new SimpleFormatter());
            Level level = Level.parse(resourceBundle.getString("logLevel"));
            fileHandler.setLevel(level);
            LOGGER.addHandler(fileHandler);
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        } catch (SecurityException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    private static List<File> getFilesInFolder(final File folder) {
        List<File> files = new ArrayList<File>();
        for (final File file : folder.listFiles()) {
            if (file.isFile()) {
                files.add(file);
            }
        }
        return files;
    }

    public String readTiffBarcodes(String fileName, String base64InputFile) {
        // Set log file handler
        setLogFileHandler();
        // Create new output Document
        Document document = new Document();
        document.setFileName(fileName);
        // Convert base64InputFile to byte[]
        byte[] inputFile = DatatypeConverter.parseBase64Binary(base64InputFile);
        File tiffFile = new File("");
        try {
            FileOutputStream fos = new FileOutputStream(tiffFile);
//            FileOutputStream fos = new FileOutputStream("C:\\TEMP\\BarcodeScanner\\temp.tif");
            fos.write(inputFile);
            fos.close();
        } catch (IOException iOException) {
            System.err.println("Bad");
        }
        List<Page> pages = new ArrayList<Page>();
        // Read barcodes from tiff file
        try {
            Iterator iterator = ImageIO.getImageReadersByFormatName("tiff");
            ImageReader reader = (ImageReader) iterator.next();
//            ByteArrayInputStream iis = new ByteArrayInputStream(inputFile);
            ImageInputStream iis = new FileImageInputStream(tiffFile);
            reader.setInput(iis, false, true);
            int pageCount = reader.getNumImages(true);
            for (int i = 0; i < pageCount; i++) {
                Page page = new Page();
                page.setPageNumber(i + 1);
                BufferedImage bufferedImage = reader.read(i);
                List<Barcode> barcodes = scanImage(bufferedImage);
                if (barcodes.isEmpty()) {
                    LOGGER.log(Level.WARNING,
                            "No barcodes found on page {0} of the input file.",
                            i + 1);
                }
                page.setBarcodes(barcodes);
                pages.add(page);
            }
            iis.close();
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        document.setPages(pages);
        // Marshall document to output String
        StringWriter outputStringWriter = new StringWriter();
        try {
            JAXBContext jaxbContext = JAXBContextFactory.createContext(new Class[]{Document.class}, null);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            // output pretty printed
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(document, outputStringWriter);
        } catch (JAXBException ex) {
            LOGGER.log(Level.SEVERE, "The output xml string could not be created.");
            LOGGER.log(Level.SEVERE, null, ex);
        }
        return outputStringWriter.toString();
    }

//    private Document scanPdfDocument(File pdfFile) throws IOException {
//        Document document = new Document();
//        document.setFileName(pdfFile.getName());
//        List<Page> pages = new ArrayList<Page>();
//        PDDocument pdfDocument = PDDocument.load(pdfFile);
//        List<PDPage> pdfPages = pdfDocument.getDocumentCatalog().getAllPages();
//        for (int i = 0; i < pdfPages.size(); i++) {
//            Page page = new Page();
//            page.setPageNumber(i + 1);
//            PDPage pdfPage = pdfPages.get(i);
//            BufferedImage bufferedImage = pdfPage.convertToImage();
//            List<Barcode> barcodes = scanImage(bufferedImage);
//            if (barcodes.isEmpty()) {
//                LOGGER.log(Level.WARNING,
//                        "No barcodes found on page {0} of the input file {1}.",
//                        new Object[]{i, pdfFile.getAbsolutePath()});
//            }
//            page.setBarcodes(barcodes);
//            pages.add(page);
//        }
//        pdfDocument.close();
//        document.setPages(pages);
//        return document;
//    }
    private Document scanTiffDocument(File tiffFile) throws IOException {
        Document document = new Document();
        document.setFileName(tiffFile.getName());
        List<Page> pages = new ArrayList<Page>();
        Iterator iterator = ImageIO.getImageReadersByFormatName("tiff");
        ImageReader reader = (ImageReader) iterator.next();
        ImageInputStream iis = new FileImageInputStream(tiffFile);
        reader.setInput(iis, false, true);
        int pageCount = reader.getNumImages(true);
        for (int i = 0; i < pageCount; i++) {
            Page page = new Page();
            page.setPageNumber(i + 1);
            BufferedImage bufferedImage = reader.read(i);
            List<Barcode> barcodes = scanImage(bufferedImage);
            if (barcodes.isEmpty()) {
                LOGGER.log(Level.WARNING,
                        "No barcodes found on page {0} of the input file {1}.",
                        new Object[]{i + 1, tiffFile.getAbsolutePath()});
            }
            page.setBarcodes(barcodes);
            pages.add(page);
        }
        iis.close();
        document.setPages(pages);
        return document;
    }

    private List<Barcode> scanImage(BufferedImage bufferedImage) {
        List<Barcode> barcodes = new ArrayList<Barcode>();
        LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

        QRCodeReader qrCodeReader = new QRCodeReader();
        HashMap<DecodeHintType, Object> hints = new HashMap<DecodeHintType, Object>();
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        GenericMultipleBarcodeReader reader = new GenericMultipleBarcodeReader(qrCodeReader);
        Result[] results;
        try {
            results = reader.decodeMultiple(bitmap, hints);
            for (Result result : results) {
                Barcode barcode = new Barcode();
                barcode.setType(BarcodeType.QR);
                barcode.setContent(result.toString());
                barcodes.add(barcode);
            }
        } catch (NotFoundException ex) {
            // No barcodes found in image
        }

        return barcodes;
    }

    private void marshalDocument(Document document, String outputFolder, String inputFileName) {
        File outputFile = new File(outputFolder + inputFileName + ".xml");
        if (outputFile.exists() && !outputFile.isDirectory()) {
            LOGGER.log(Level.INFO,
                    "The output xml file {0} already exists. A Unique output file name will be created.",
                    outputFile.getAbsolutePath());
            boolean fileExists = true;
            int i = 1;
            while (fileExists) {
                outputFile = new File(outputFolder + inputFileName + "_" + i + ".xml");
                if (outputFile.exists()) {
                    i++;
                } else {
                    fileExists = false;
                }
            }
        }
        try {
            JAXBContext jaxbContext = JAXBContextFactory.createContext(new Class[]{Document.class}, null);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            // output pretty printed
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            jaxbMarshaller.marshal(document, outputFile);
        } catch (JAXBException ex) {
            LOGGER.log(Level.SEVERE, "The output xml file {0} could not be created.", outputFile.getAbsolutePath());
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    private static void moveInvalidInputFile(String inputFileName) {
        String errorFolderPath = resourceBundle.getString("errorFolder");
        File errorFolder = new File(errorFolderPath);
        if (!errorFolder.exists() || !errorFolder.isDirectory()) {
            LOGGER.log(Level.INFO, "Creating error folder: {0}", errorFolder);
            errorFolder.mkdir();
        }
        File inputFile = new File(resourceBundle.getString("inputFolder") + inputFileName);
        LOGGER.log(Level.INFO, "Moving input file {0} to the error folder.", resourceBundle.getString("inputFolder") + inputFileName);
        File newFileName = new File(errorFolderPath + inputFileName);
        if (newFileName.exists() && !newFileName.isDirectory()) {
            boolean fileExists = true;
            int i = 1;
            while (fileExists) {
                newFileName = new File(errorFolderPath + inputFileName + "_" + i);
                if (newFileName.exists()) {
                    i++;
                } else {
                    fileExists = false;
                }
            }
        }
        inputFile.renameTo(newFileName);
    }
}
