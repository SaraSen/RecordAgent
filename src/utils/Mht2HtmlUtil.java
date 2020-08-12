/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

/**
 *
 * @author Saroj
 */
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.stream.Stream;

import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePartDataSource;

public class Mht2HtmlUtil {

    /**
     * Convert mht files to html files
     *
     * @param s_SrcMht
     * @param s_DescHtml
     */
    public static void mht2html(String s_SrcMht, String s_DescHtml, String recordname) {
        try {
            InputStream fis = new FileInputStream(s_SrcMht);
            Session mailSession = Session.getDefaultInstance(System.getProperties(), null);
            MimeMessage msg = new MimeMessage(mailSession, fis);
            Object content = msg.getContent();
            if (content instanceof Multipart) {
                MimeMultipart mp = (MimeMultipart) content;
                MimeBodyPart bp1 = (MimeBodyPart) mp.getBodyPart(0);

                // Get the code of the mht file content code
                String strEncodng = getEncoding(bp1);

                // Get the contents of the mht file
                String strText = getHtmlText(bp1, strEncodng);
                if (strText == null) {
                    return;
                }

                // Create a folder with the mht file name, mainly used to save resource files.
                File parent = null;
                if (mp.getCount() > 1) {
                    parent = new File(new File(s_DescHtml).getAbsolutePath());
                    parent.mkdirs();
                    if (!parent.exists()) { // Exit if the folder fails to be created
                        return;
                    }
                }

                // FOR code is mainly to save resource files and replace paths
                for (int i = 1; i < mp.getCount(); ++i) {
                    MimeBodyPart bp = (MimeBodyPart) mp.getBodyPart(i);
                    // Get the path to the resource file
                    // Example (Get: http://xxx.com/abc.jpg)
                    String strUrl = getResourcesUrl(bp);
                    if (strUrl == null || strUrl.length() == 0) {
                        continue;
                    }

                    DataHandler dataHandler = bp.getDataHandler();
                    MimePartDataSource source = (MimePartDataSource) dataHandler.getDataSource();

                    // Get the absolute path of the resource file
                    String FilePath = parent.getAbsolutePath() + File.separator + getName(strUrl, i);
                    File resources = new File(FilePath);

                    // save the resource file
                    if (SaveResourcesFile(resources, bp.getInputStream())) {
                        // Replace the remote address with a local address such as image, JS, CSS style,
                        // etc.
                        strText = strText.replace(strUrl, resources.getAbsolutePath());

                    }
                }

                // Finally save the HTML file
                SaveHtml(strText, s_DescHtml, strEncodng);
                //deleting unwanted files
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(parent.getAbsolutePath()))) {
                    for (Path entry : stream) {
                        File file = entry.toFile();
                        if (file.isFile()) {
                            if ((file.length() < 100000
                                    && file.getName().contains("jpg"))
                                    || file.getName().contains("mht")
                                    || file.getName().contains("etl")
                                    || file.getName().contains("zip")) { //size of MB
                                //delete the file
                                file.delete();
                            }
                        }
                    }

                    ArrayList<String> tasks = ReadXMLFile.readxml(getxml(parent.getAbsolutePath()));
                    ArrayList<String> imgs = getimgs(parent.getAbsolutePath());

                    RestAPI.start(tasks, imgs, parent.getAbsolutePath(), recordname);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the name of the resource file in the mht file content
     *
     * @param strName
     * @param ID
     * @return
     */
    public static String getName(String strName, int ID) {
        char separator1 = '/';
        char separator2 = '\\';
        // replace the newline
        strName = strName.replaceAll("\r\n", "");

        // Get the file name
        if (strName.lastIndexOf(separator1) >= 0) {
            return strName.substring(strName.lastIndexOf(separator1) + 1);
        }
        if (strName.lastIndexOf(separator2) >= 0) {
            return strName.substring(strName.lastIndexOf(separator2) + 1);
        }
        return "";
    }

    /**
     * Write the extracted html content to the saved path.
     *
     * @param strText
     * @param strHtml
     * @param strEncodng
     */
    public static boolean SaveHtml(String s_HtmlTxt, String s_HtmlPath, String s_Encode) {
        try {
            Writer out = null;
            out = new OutputStreamWriter(new FileOutputStream(s_HtmlPath, false), s_Encode);
            out.write(s_HtmlTxt);
            out.close();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * Save JS, image, CSS style and other resource files in the web page
     *
     * @param SrcFile Source File
     * @param inputStream Input stream
     * @return
     * @throws IOException
     */
    private static boolean SaveResourcesFile(File SrcFile, InputStream inputStream) throws IOException {
        if (SrcFile == null || inputStream == null) {
            return false;
        }

        BufferedInputStream in = null;
        FileOutputStream fio = null;
        BufferedOutputStream osw = null;
        try {
            in = new BufferedInputStream(inputStream);
            fio = new FileOutputStream(SrcFile + "/" + String.valueOf(System.currentTimeMillis()) + ".jpg");
            osw = new BufferedOutputStream(new DataOutputStream(fio));
            int index = 0;
            byte[] a = new byte[1024];
            while ((index = in.read(a)) != -1) {
                osw.write(a, 0, index);
            }
            osw.flush();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {

            try {
                if (osw != null) {
                    osw.close();
                }
                if (fio != null) {
                    fio.close();
                }
                if (in != null) {
                    in.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    }

    /**
     * Get the URL path of the resource file in the mht file
     *
     * @param bp
     * @return
     */
    private static String getResourcesUrl(MimeBodyPart bp) {
        if (bp == null) {
            return null;
        }
        try {
            Enumeration list = bp.getAllHeaders();
            while (list.hasMoreElements()) {
                javax.mail.Header head = (javax.mail.Header) list.nextElement();
                if (head.getName().compareTo("Content-Location") == 0) {
                    return head.getValue();
                }
            }
            return null;
        } catch (MessagingException e) {
            return null;
        }
    }

    /**
     * Get the content code in the mht file
     *
     * @param bp
     * @param strEncoding The encoding of the mht file
     * @return
     */
    private static String getHtmlText(MimeBodyPart bp, String strEncoding) {
        InputStream textStream = null;
        BufferedInputStream buff = null;
        BufferedReader br = null;
        Reader r = null;
        try {
            textStream = bp.getInputStream();
            buff = new BufferedInputStream(textStream);
            r = new InputStreamReader(buff, strEncoding);
            br = new BufferedReader(r);
            StringBuffer strHtml = new StringBuffer("");
            String strLine = null;
            while ((strLine = br.readLine()) != null) {
//                System.out.println(strLine);
//                strHtml.append(strLine + "\r\n");
            }
            br.close();
            r.close();
            textStream.close();
            return strHtml.toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
                if (buff != null) {
                    buff.close();
                }
                if (textStream != null) {
                    textStream.close();
                }
            } catch (Exception e) {
            }
        }
        return null;
    }

    /**
     * Get the encoding of the content code in the mht web file
     *
     * @param bp
     * @return
     */
    private static String getEncoding(MimeBodyPart bp) {
        if (bp == null) {
            return null;
        }
        try {
            Enumeration list = bp.getAllHeaders();
            while (list.hasMoreElements()) {
                javax.mail.Header head = (javax.mail.Header) list.nextElement();
                if (head.getName().equalsIgnoreCase("Content-Type")) {
                    String strType = head.getValue();
                    int pos = strType.indexOf("charset=");
                    if (pos >= 0) {
                        String strEncoding = strType.substring(pos + 8, strType.length());
                        if (strEncoding.startsWith("\"") || strEncoding.startsWith("\'")) {
                            strEncoding = strEncoding.substring(1, strEncoding.length());
                        }
                        if (strEncoding.endsWith("\"") || strEncoding.endsWith("\'")) {
                            strEncoding = strEncoding.substring(0, strEncoding.length() - 1);
                        }
                        if (strEncoding.toLowerCase().compareTo("gb2312") == 0) {
                            strEncoding = "gbk";
                        }
                        return strEncoding;
                    }
                }
            }
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String getxml(String parent) {
        File dir = new File(parent);
        String filepath = null;
        String[] children = dir.list();

        if (children == null) {
            System.out.println("does not exist or is not a directory");
        } else {
            for (int i = 0; i < children.length; i++) {
                if (children[i].toString().contains("RecordingXML")) {
                    filepath = children[i];
                    break;
                }
            }
        }
        return parent + "\\" + filepath;
    }

    private static ArrayList<String> getimgs(String parent) {
        File dir = new File(parent);
        ArrayList<String> filepath = new ArrayList<String>();
        String[] children = dir.list();

        if (children == null) {
            System.out.println("does not exist or is not a directory");
        } else {
            for (int i = 0; i < children.length; i++) {
                if (!(children[i].toString().contains("Recording"))) {
                    filepath.add(parent + "\\" + children[i]);
                }
            }
        }
        return filepath;
    }
}
