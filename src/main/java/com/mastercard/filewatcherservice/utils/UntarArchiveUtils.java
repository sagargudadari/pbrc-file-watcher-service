package com.mastercard.filewatcherservice.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.zip.GZIPInputStream;

@Slf4j
public class UntarArchiveUtils {

    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static void unTarFile(String tarFile, File destFile) {
        TarArchiveInputStream tis = null;
        String destFilepath = null;
        try {
            FileInputStream fis = new FileInputStream(tarFile);
            // .gz
            GZIPInputStream gzipInputStream = new GZIPInputStream(new BufferedInputStream(fis));
            //.tar.gz
            tis = new TarArchiveInputStream(gzipInputStream);
            TarArchiveEntry tarEntry;
            while ((tarEntry = tis.getNextTarEntry()) != null) {
                if(tarEntry.isDirectory()){
                    continue;
                }else {
                    // In case entry is for file ensure parent directory is in place
                    // and write file content to Output Stream
                    String splitName = tarEntry.getName().split("_")[0];
                    destFilepath = destFile + File.separator
                            + "temp" + File.separator
                            + dtf.format(LocalDate.now()) + File.separator
                            + splitName + File.separator
                            + tarEntry.getName();
                    File outputFile = new File(destFilepath);
                    outputFile.getParentFile().mkdirs();
                    FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
                    IOUtils.copy(tis, fileOutputStream);
                    fileOutputStream.close();
                }
            }
            log.info("Completed successfully location is {}", destFilepath);
            fis.close();
            gzipInputStream.close();
        }catch(IOException ex) {
            System.out.println("Error while untarring a file- " + ex.getMessage());
        }finally {
            if(tis != null) {
                try {
                    tis.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }    
    }
}