/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import java.awt.*;
import java.awt.event.*;
import java.awt.TrayIcon.MessageType;

/**
 *
 * @author Saroj
 */
public class RestAPI {

    private static String postToURL(String url, String message, DefaultHttpClient httpClient)
            throws IOException, IllegalStateException, UnsupportedEncodingException, RuntimeException {
        HttpPost postRequest = new HttpPost(url);

        StringEntity input = new StringEntity(message);
        input.setContentType("application/json");
        postRequest.setEntity(input);

        HttpResponse response = httpClient.execute(postRequest);

        if (response.getStatusLine().getStatusCode() != 200) {
            throw new RuntimeException("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
        }

        BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));

        String output;
        StringBuffer totalOutput = new StringBuffer();
        System.out.println("Output from Server .... \n");
        while ((output = br.readLine()) != null) {
            System.out.println(output);
            totalOutput.append(output);

            notifyuser(output);

        }
        return totalOutput.toString();
    }

    public static void setSlide(String message) throws IOException {
        String url = "http://localhost:8080/addrecording";
        DefaultHttpClient httpClient = new DefaultHttpClient();
        String response = postToURL(url, message, httpClient);
        System.out.println(response);
        httpClient.getConnectionManager().shutdown();
    }

    public static void start(ArrayList<String> tasks, ArrayList<String> images, String parentfolder, String recordname) throws InterruptedException {
        RestAPI p = new RestAPI();
        try {
            StringBuilder message = new StringBuilder();
            message.append(generateJSON(tasks, images, parentfolder, recordname).toString());
            p.setSlide(message.toString());

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static JsonObject generateJSON(ArrayList<String> tasks, ArrayList<String> images, String parentfolder, String recordname) {
        //Create Main jSon object
        JsonObject jsonParams = new JsonObject();

        try {
            //Add string params
            jsonParams.addProperty("id", String.valueOf(ThreadLocalRandom.current().nextInt()));
            jsonParams.addProperty("folderlocation", parentfolder);
        } catch (JsonIOException e) {
            e.printStackTrace();
        }
        //Create json array for filter
        JsonArray array = new JsonArray();

        //Create json objects for two filter Ids
        JsonObject jsonParam1 = new JsonObject();
        JsonObject jsonParam2 = new JsonObject();
        try {

            for (int i = 1; i < tasks.size(); i++) {
                jsonParam2.addProperty("description", tasks.get(i));
                jsonParam2.addProperty("image", images.get(i));
                jsonParam1.add("step" + i, jsonParam2);
                jsonParam2 = new JsonObject();
            }

        } catch (JsonIOException e) {
            e.printStackTrace();
        }

        //Add the filter Id object to array
        array.add(jsonParam1);
        //Add array to main json object
        try {
            jsonParams.add("events", array);
            jsonParams.addProperty("recordname", recordname);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonParser jp = new JsonParser();
            JsonElement je = jp.parse(jsonParams.toString());
            String prettyJsonString = gson.toJson(je);
            System.out.println(prettyJsonString);
        } catch (JsonIOException e) {
            e.printStackTrace();
        }
        return jsonParams;
    }

    private static void notifyuser(String output) {
        try {
            SystemTray tray = SystemTray.getSystemTray();
            Image image = Toolkit.getDefaultToolkit().createImage("../assets/icon.png");          
            TrayIcon trayIcon = new TrayIcon(image, "HCMRecorder");            
            trayIcon.setImageAutoSize(true);            
            trayIcon.setToolTip("HCMRecorder");
            tray.add(trayIcon);

            // Display info notification:
            trayIcon.displayMessage("HCMRecorder", output, MessageType.INFO);
            System.exit(0);
        } catch (Exception ex) {
            System.err.print(ex);
        }
    }

}
