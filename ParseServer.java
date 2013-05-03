import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.Iterator;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import org.xml.sax.InputSource;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.BoilerpipeExtractor;
import de.l3s.boilerpipe.document.Image;
import de.l3s.boilerpipe.document.TextDocument;
import de.l3s.boilerpipe.extractors.ArticleExtractor;
import de.l3s.boilerpipe.extractors.CommonExtractors;
import de.l3s.boilerpipe.sax.ImageExtractor;
import de.l3s.boilerpipe.sax.BoilerpipeSAXInput;
import de.l3s.boilerpipe.sax.HTMLFetcher;
import de.l3s.boilerpipe.sax.HTMLDocument;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;

public class ParseServer{
  public static void main(String[] args) throws IOException {
    InetSocketAddress addr = new InetSocketAddress(6664);
    HttpServer server = HttpServer.create(addr, 0);

    server.createContext("/", new MyHandler());

    // server.setExecutor(Executors.newCachedThreadPool());
    // Faster, less CPU
    server.setExecutor(Executors.newFixedThreadPool(20));

    server.start();
    System.out.println("Server is listening on port 6666");
  }
}

class MyHandler implements HttpHandler {
  protected void writeHTMLToFile(URL url) { 
    try {
      HTMLDocument html = HTMLFetcher.fetch(url);

      // Create file 
      InputSource in = html.toInputSource();

      String encoding = in.getEncoding();
      InputStream is = in.getByteStream();

      StringWriter writer = new StringWriter();
      IOUtils.copy(is, writer, encoding);
      String theString = writer.toString();
      FileUtils.writeStringToFile(new File("out.html"), theString);

    } catch (Exception e) {
      System.out.println("HTML doc fetch fail " + e);
    }
  }

  TextDocument getArticleTextDoc(ByteArrayOutputStream baos) throws IOException {
    // Open new InputStreams using the recorded bytes
    // Can be repeated as many times as you wish
    InputStream is1 = new ByteArrayInputStream(baos.toByteArray()); 

    InputSource inputSource = new InputSource(is1);

    // Extraction for text
    BoilerpipeExtractor extractor = CommonExtractors.ARTICLE_EXTRACTOR;
    TextDocument doc = null;
    try {
      doc = new BoilerpipeSAXInput(inputSource).getTextDocument();
      extractor.process(doc);
    } catch (Exception e) {
      System.out.println("HTML processing fail");
      System.out.println(e);
    }

    return doc;
  }

  List<Image> getImages(ByteArrayOutputStream baos) throws IOException {
    // Extraction for images
    InputStream is1 = new ByteArrayInputStream(baos.toByteArray());
    InputSource textDocSource = new InputSource(is1);
    InputStream is2 = new ByteArrayInputStream(baos.toByteArray());
    InputSource imageDocSource = new InputSource(is2);

    BoilerpipeExtractor extractor = CommonExtractors.KEEP_EVERYTHING_EXTRACTOR;
    TextDocument imageDoc = null;
    try {
      imageDoc = new BoilerpipeSAXInput(textDocSource).getTextDocument();
      extractor.process(imageDoc);
    } catch (Exception e) {
      System.out.println("HTML processing fail");
      System.out.println(e);
    }

    //FileUtils.writeStringToFile(new File("content.txt"), imageDoc.getText(true, false));

    ImageExtractor ie = ImageExtractor.INSTANCE;
    List<Image> images =  null;
    try {
      images = ie.process(imageDoc, imageDocSource);
    } catch (Exception e) {
      System.out.println("Image processing fail");
    }
    Collections.sort(images);

    return images;
  }

  public void handle(HttpExchange exchange) throws IOException {
    // setting up the response
    OutputStream responseBody = exchange.getResponseBody();
    Headers requestHeaders = exchange.getRequestHeaders();
    Headers responseHeaders = exchange.getResponseHeaders();
    responseHeaders.set("Content-Type", "application/json");
    exchange.sendResponseHeaders(200, 0);

    InputStream is = exchange.getRequestBody();

    // copy the InputStream so we can use it again!
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    int len;
    while ((len = is.read(buffer)) > -1 ) {
        baos.write(buffer, 0, len);
    }
    baos.flush();

    TextDocument doc = getArticleTextDoc(baos);
    List<Image> images = getImages(baos);

    // convert to json for sending response
    JSONObject result = new JSONObject();
    result.put("title", doc.getTitle());
    result.put("content", doc.getText(true, false));
    JSONArray list = new JSONArray();
    for (Image img : images) {
      JSONObject obj = new JSONObject();
      obj.put("src", img.getSrc());
      obj.put("alt", img.getAlt());
      obj.put("height", img.getHeight());
      obj.put("width", img.getWidth());
      list.add(obj);
    }
    result.put("images", list);

    // send response!
    responseBody.write(result.toString().getBytes());
    responseBody.close();
  }
}
