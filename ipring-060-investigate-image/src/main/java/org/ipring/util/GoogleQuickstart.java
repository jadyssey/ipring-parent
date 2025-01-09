package org.ipring.util;

import java.io.IOException;

public class GoogleQuickstart {

  public static void main(String[] args) throws IOException {
    // TODO(developer): Replace these variables before running the sample.
    String projectId = "global-terrain-445809-f1";
    String location = "us-central1";
    String modelName = "gemini-1.5-flash-001";

    // String output = quickstart(projectId, location, modelName);
    // System.out.println(output);
  }

  // Analyzes the provided Multimodal input.
  /*public static String quickstart(String projectId, String location, String modelName)
      throws IOException {
    // Initialize client that will be used to send requests. This client only needs
    // to be created once, and can be reused for multiple requests.
    try (VertexAI vertexAI = new VertexAI(projectId, location)) {
      String imageUri = "gs://demo-investigate/本人签收.jpg";

      GenerativeModel model = new GenerativeModel(modelName, vertexAI);
      GenerateContentResponse response = model.generateContent(ContentMaker.fromMultiModalData(
          PartMaker.fromMimeTypeAndData("image/png", imageUri),
          "What's in this photo"
      ));

      return response.toString();
    }
  }*/
}