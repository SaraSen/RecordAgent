/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package hcmrecorder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import org.apache.commons.io.FilenameUtils;
import utils.Mht2HtmlUtil;
import com.jfoenix.controls.JFXTextField;
import java.util.Optional;
import javafx.scene.control.ButtonType;

/**
 *
 * @author Saroj
 */
public class FXMLDocumentController implements Initializable {

    String filename = String.valueOf(System.currentTimeMillis());
    File file;
    String[] pathnames;

    @FXML
    private Label label;

    @FXML
    private JFXTextField recordname;

    @FXML
    private void handleRecordButton(ActionEvent event) {
        if (recordname.getText().equals("")) {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("HCM");
            alert.setHeaderText("HCM Recorder");
            alert.setContentText("Plesae fillout the recordname");
        } else {
            file = new File("C:\\PSRRecordings\\"
                    + filename);
            boolean bool = file.mkdir();
            if (bool) {
                Logger.getLogger(FXMLDocumentController.class.getName())
                        .log(Level.INFO, "Dircetory was created", "");
            } else {
                Logger.getLogger(FXMLDocumentController.class.getName())
                        .log(Level.SEVERE, "Error occured while"
                                + " creating directory", "");
            }
            System.out.println(file);
            try {
                Runtime.getRuntime().exec("cmd.exe /c start psr.exe /start"
                        + " /output C:\\PSRRecordings\\temp_PSR_UI.zip /sc 1 /gui 0"
                        + " /arcetl 1 /arcxml 1 /sketch 1 /slides 1");
                Logger.getLogger(FXMLDocumentController.class.getName())
                        .log(Level.INFO, "Recording started", "");
                label.setText("Recording started");
                HCMRecorder.minimize();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    @FXML
    private void handleStopButton(ActionEvent event) {
        try {
            Runtime.getRuntime().exec("cmd.exe /c start psr.exe /stop ");
            Logger.getLogger(FXMLDocumentController.class.getName())
                    .log(Level.INFO, "Recording Stopped", "");
            label.setText("Recording stopped");

            Thread.sleep(3000);
            Path temp = Files.move(Paths.get("C:\\PSRRecordings\\temp_PSR_UI.zip"),
                    Paths.get(String.valueOf(file) + "\\" + "temp_PSR_UI.zip"));

            if (temp != null) {
                System.out.println("File renamed and moved successfully");
            } else {
                System.out.println("Failed to move the file");
            }

            unzip(String.valueOf(file) + "\\" + "temp_PSR_UI.zip", String.valueOf(file));
        } catch (IOException ex) {
            Logger.getLogger(FXMLDocumentController.class.getName())
                    .log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @FXML
    private void handlePauseButton(ActionEvent event) {
        System.out.println(recordname.getText());
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }

    private void unzip(String zipFilePath, String destDir) {
        try {
            Thread.sleep(3000);
            File dir = new File(destDir);
            Alert alert = new Alert(AlertType.INFORMATION);
            // create output directory if it doesn't exist
            if (!dir.exists()) {
                dir.mkdirs();
            }
            FileInputStream fis;
            //buffer for read and write data to file
            byte[] buffer = new byte[1024];
            try {
                fis = new FileInputStream(zipFilePath);
                ZipInputStream zis = new ZipInputStream(fis);
                ZipEntry ze = zis.getNextEntry();
                while (ze != null) {
                    String fileName = ze.getName();
                    File newFile
                            = new File(destDir + File.separator + fileName);
                    Logger.getLogger(FXMLDocumentController.class.getName())
                            .log(Level.INFO, "Unzipping to "
                                    + "" + newFile.getAbsolutePath(), "");
                    //create directories for sub directories in zip
                    new File(newFile.getParent()).mkdirs();
                    FileOutputStream fos = new FileOutputStream(newFile);
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                    //close this ZipEntry
                    zis.closeEntry();
                    ze = zis.getNextEntry();
                }
                //close last ZipEntry
                zis.closeEntry();
                zis.close();
                fis.close();

                alert.setTitle("HCM");
                alert.setHeaderText("HCM Recorder");
                alert.setContentText("Recording has been completed");

                System.gc();

            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (InterruptedException ex) {
            Logger.getLogger(FXMLDocumentController.class.getName())
                    .log(Level.SEVERE, null, ex);
        }

        //take whatever latest mht file currently there
        Mht2HtmlUtil.mht2html(listfilese(String.valueOf(file)),
                String.valueOf(file), recordname.getText());

    }

    public String listfilese(String filepath) {

        File f = new File(filepath);
        pathnames = f.list();
        for (String pathname : pathnames) {
            if (FilenameUtils.getExtension(pathname).equalsIgnoreCase("mht")) {
                return filepath + "\\" + pathname;
            } else {
                continue;
            }
        }
        return null;
    }

}
