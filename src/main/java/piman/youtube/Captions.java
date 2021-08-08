package piman.youtube;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeTrackJsonData;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;

public class Captions {
	
	private List<Event> events = new ArrayList<>();
	
	private Captions() {}
	
	public static Captions CreateCaptions() {
		return new Captions();
	}
	
	public static Captions CreateCaptions(HttpInterface httpInterface, JsonBrowser videoInfo) {
		
		Captions captions = CreateCaptions();
		
		try {
	        captions = getCaptions(httpInterface, videoInfo);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return captions;
	}
	
	public static Captions CreateCaptions(Document document) {
		
		Captions captions = new Captions();
		
		NodeList nodes = document.getElementsByTagName("text");
		
		for (int i = 0; i < nodes.getLength(); i++) {
			Node node = nodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				
				Element element = (Element) node;
				
				long start = (long) (Double.valueOf(element.getAttribute("start")) * 1000);
				long end = start + (long) (Double.valueOf(element.getAttribute("dur")) * 1000);
				
				Text text = (Text) element.getFirstChild();
				String caption = StringEscapeUtils.unescapeHtml4(text.getData());
				
				StringBuilder sb = new StringBuilder();
				StringBuilder mod = new StringBuilder();
				int j = 0;
				for (char c : caption.toCharArray()) {
					if (c == '<') j++;
					else if (c == '>') {
						j--;
						String s = mod.toString();
						if (s.equals("b")) {
							sb.append("**");
						}
						else if (s.equals("/b")) {
							sb.append("**\u200b");
						}
						else if (s.equals("i")) {
							sb.append("*");
						}
						else if (s.equals("/i")) {
							sb.append("*\u200b");
						}
						mod.setLength(0);
					}
					else if (j == 0 && canPrint(c)) {
						sb.append(c);
					}
					else if (j == 1 && canPrint(c)) {
						mod.append(c);
					}
				}
				captions.events.add(new Event(start, end, sb.toString()));
			}
		}
		
		return captions;
	}
	
	public static boolean canPrint(char c) {
		//return Character.isAlphabetic(c) || Character.isWhitespace(c) || Character.isDigit(c) || Character.isIdeographic(c);
		return Character.compare(c, '\u200b') != 0;
	}
	
	  private static JsonBrowser getVideoInfo(HttpInterface httpInterface, String videoID) throws IOException {
		  String url = "https://www.youtube.com/watch?v=" + videoID + "&pbj=1&hl=en";
		  
		  try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(url))) {
			  HttpClientTools.assertSuccessWithContent(response, "Video Data");
			  
		      String responseText = EntityUtils.toString(response.getEntity(), UTF_8);
			  
			  try {
				  JsonBrowser data = JsonBrowser.parse(responseText);
				  return YoutubeTrackJsonData.fromMainResult(data).playerResponse;
			  }
			  catch (FriendlyException e) {
				  throw e;
			  }
			  catch (Exception e) {
				  throw new FriendlyException("Unable to Parse Data", SUSPICIOUS,
						  new RuntimeException("Failed to parse: " + responseText, e));
			  }
		  }
	  }
	
	  private static Captions getCaptions(HttpInterface httpInterface, JsonBrowser finalData) throws IOException {
	      if (!finalData.get("captions").isNull()) {
			JsonBrowser captions = finalData.get("captions").get("playerCaptionsTracklistRenderer");
			int defaultAudioTrack = (int) captions.get("defaultAudioTrackIndex").asLong(0);
			int defaultCaptionTrack = (int) captions.get("audioTracks").index(defaultAudioTrack).get("defaultCaptionTrackIndex").asLong(0);
			JsonBrowser captionTrack = captions.get("captionTracks").index(defaultCaptionTrack);
			String captionUrl = captionTrack.get("baseUrl").text();
			
		    try (CloseableHttpResponse response = httpInterface.execute(new HttpGet(captionUrl))) {
		      HttpClientTools.assertSuccessWithContent(response, "caption response");

		      String responseText = EntityUtils.toString(response.getEntity(), UTF_8);

		      try {
		    	  DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		    	  Document captionsDocument = builder.parse(IOUtils.toInputStream(responseText, UTF_8));
		    	  return Captions.CreateCaptions(captionsDocument);
		      } catch (FriendlyException e) {
		    	  throw e;
		      } catch (Exception e) {
		    	  throw new FriendlyException("Received unexpected response from YouTube.", SUSPICIOUS,
		    			  new RuntimeException("Failed to parse: " + responseText, e));
		      }
		    }
	      }
	      return CreateCaptions();
	  }
	
	public List<Event> getEvents() {
		return events;
	}
	
	public static class Event {
		private final long start, stop;
		private final String text;
		
		Event(long start, long stop, String text) {
			this.start = start;
			this.stop = stop;
			this.text = text;
		}
		
		public boolean isInRange(long time, long frameDuration) {
			return time >= start - frameDuration/2 && time < stop + frameDuration/2;
		}
		
		public String getText() {
			return text;
		}
		
	}
	
}
