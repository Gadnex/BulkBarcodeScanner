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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import net.binarypaper.barcodescanner.entity.Barcode;
import net.binarypaper.barcodescanner.entity.BarcodeType;
import net.binarypaper.barcodescanner.entity.Document;
import net.binarypaper.barcodescanner.entity.Page;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.eclipse.persistence.jaxb.JAXBContextFactory;

/**
 *
 * @author William Gadney <gadnex@gmail.com>
 */
public class BarcodeScanner {

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle("config");

    public static void main(String[] args) {
        File inputFolder = new File(resourceBundle.getString("inputFolder"));
        while (true) {
            List<File> files = getFilesInFolder(inputFolder);
            for (File file : files) {
                try {
                    String fileNameLoweCase = file.getName().toLowerCase();
                    if (fileNameLoweCase.endsWith(".pdf")) {
                        BarcodeScanner barcodeScanner = new BarcodeScanner();
                        Document document = barcodeScanner.scanPdfDocument(file);
                        String outputFolder = resourceBundle.getString("outputFolder");
                        barcodeScanner.marshalDocument(document, outputFolder, file.getName());
                        file.delete();
                    } else if (fileNameLoweCase.endsWith(".tif") || fileNameLoweCase.endsWith(".tiff")) {
                        BarcodeScanner barcodeScanner = new BarcodeScanner();
                        Document document = barcodeScanner.scanTiffDocument(file);
                        String outputFolder = resourceBundle.getString("outputFolder");
                        barcodeScanner.marshalDocument(document, outputFolder, file.getName());
                        file.delete();
                    } else {
                        moveInvalidInputFile(file.getName());
                    }
                } catch (FileNotFoundException ex) {
                    // Do nothing
                } catch (Exception ex) {
                    moveInvalidInputFile(file.getName());
                }
            }
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

    public Document scanPdfDocument(File pdfFile) throws IOException {
        Document document = new Document();
        document.setFileName(pdfFile.getName());
        List<Page> pages = new ArrayList<Page>();
        PDDocument pdfDocument = PDDocument.load(pdfFile);
        List<PDPage> pdfPages = pdfDocument.getDocumentCatalog().getAllPages();
        for (int i = 0; i < pdfPages.size(); i++) {
            Page page = new Page();
            page.setPageNumber(i + 1);
            PDPage pdfPage = pdfPages.get(i);
            BufferedImage bufferedImage = pdfPage.convertToImage();
            page.setBarcodes(scanImage(bufferedImage));
            pages.add(page);
        }
        pdfDocument.close();
        document.setPages(pages);
        return document;
    }

    public Document scanTiffDocument(File tiffFile) throws IOException {
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
            BufferedImage bufferedImage = reader.read(i);;
            page.setBarcodes(scanImage(bufferedImage));
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
        } catch (JAXBException e) {
            e.printStackTrace();
        }
    }

    private static void moveInvalidInputFile(String inputFileName) {
        String invalidFileDestination = resourceBundle.getString("inputFolder") + "_Invalid\\";
        File invalidDestinationFolder = new File(invalidFileDestination);
        if (!invalidDestinationFolder.exists() || !invalidDestinationFolder.isDirectory()) {
            System.out.println("Creating invalid file directory: " + invalidFileDestination);
            invalidDestinationFolder.mkdir();
        }
        File inputFile = new File(resourceBundle.getString("inputFolder") + inputFileName);
        File newFileName = new File(invalidFileDestination + inputFileName);
        if (newFileName.exists() && !newFileName.isDirectory()) {
            boolean fileExists = true;
            int i = 1;
            while (fileExists) {
                newFileName = new File(invalidFileDestination + inputFileName + "_" + i);
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
