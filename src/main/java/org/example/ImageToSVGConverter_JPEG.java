package org.example;


import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageToSVGConverter_JPEG {

    private String IMAGE_PNG =  "image/jpg";
    private static final String API_URL = "https://vectorizer.ai/api/v1/vectorize";
    private static final String API_KEY = "Your API Key";
    //    private static final String folderPath = "I:\\My Drive\\Generated-Midjouney\\Upscaled_photo\\Test"; // TODO: Replace with your folder path
    private static final String folderPath =  "I:\\My Drive\\Generated-Midjouney\\2_Leo_Upscaled\\Leo_1";

    public static void main(String[] args) throws IOException, InterruptedException {
        int numThreads = 5;
        long backOffTime = 5000; // in milliseconds
        int backOffCount = 0;

        File inputFolder = new File(folderPath);

        File[] files = inputFolder.listFiles((dir, name) -> name.endsWith(".jpg"));
        if (files == null || files.length == 0) {
            System.out.println("No PNG files found in input folder.");
            return;
        }

        for (File file : files) {

            boolean success = false;
            while (!success) {
                try {

                    Request postRequest = Request.post("https://vectorizer.ai/api/v1/vectorize")
                            .addHeader("Authorization", "Basic " + API_KEY)
                            .body(MultipartEntityBuilder.create()
                                    .addBinaryBody("image", file, ContentType.create("image/jpg"), file.getName())
                                    .build());

                    ClassicHttpResponse response = (ClassicHttpResponse) postRequest.execute().returnResponse();


                    if (response.getCode() == 200) {
                        File output = new File(inputFolder, file.getName().replace(".jpg", ".svg"));
                        try (FileOutputStream out = new FileOutputStream(output)) {
                            response.getEntity().writeTo(out);
                        }
                        System.out.println("File converted: " + file.getName());
                        success = true;
                        backOffCount = 0; // Reset back off count
                    } else if (response.getCode() == 429) {
                        System.out.println("Too many requests, backing off...");
                        Thread.sleep(backOffTime);
                        backOffTime += 5000; // Increase back off time for next request
                        backOffCount++;
                    } else {
                        System.out.println("Request failed: " + response.getCode() + " " + response.getReasonPhrase());
                        success = true; // Don't retry on other errors
                        backOffCount = 0; // Reset back off count
                    }
                } catch (IOException e) {
                    System.out.println("IOException occurred: " + e.getMessage());
                    success = true; // Don't retry on IOException
                    backOffCount = 0; // Reset back off count
                }
            }

            if (backOffCount > 0) {
                System.out.println("Backed off " + backOffCount + " times for file: " + file.getName());
            }

            if (Thread.activeCount() >= numThreads) {
                System.out.println("Reached max threads, waiting...");
                while (Thread.activeCount() >= numThreads) {
                    Thread.sleep(1000);
                }
                System.out.println("Continuing...");
            }
        }
    }
}