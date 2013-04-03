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

public class ParseServer{
  public static void main(String[] args) throws IOException {
    InetSocketAddress addr = new InetSocketAddress(6666);
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
  public void handle(HttpExchange exchange) throws IOException {
    // headers stuff
    OutputStream responseBody = exchange.getResponseBody();
    Headers requestHeaders = exchange.getRequestHeaders();
    Headers responseHeaders = exchange.getResponseHeaders();
    responseHeaders.set("Content-Type", "text/plain");
    exchange.sendResponseHeaders(200, 0);

    InputStream is = exchange.getRequestBody();
    BufferedReader br = new BufferedReader(new InputStreamReader(is));
    // build url string from input stream
    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = br.readLine()) != null) {
      sb.append(line);
    } 
    URL url = new URL(sb.toString());
    br.close();

    // get the image from the provided url
    BoilerpipeExtractor extractor = CommonExtractors.ARTICLE_EXTRACTOR;
    ImageExtractor ie = ImageExtractor.INSTANCE;
    List<Image> imgUrls =  null;
    try {
      imgUrls = ie.process(url, extractor);
    } catch (Exception e) {
      System.out.println("Image processing fail");
    }

    Collections.sort(imgUrls);

    //System.out.println("Images?" + " " + imgUrls.size());
    //for(Image img : imgUrls) {
    //  System.out.println("* " + img.getSrc());
    //}

    // send the first image back
    if (imgUrls.size() > 0) {
      responseBody.write(imgUrls.get(0).getSrc().getBytes());
    }
    responseBody.close();
  }
}
