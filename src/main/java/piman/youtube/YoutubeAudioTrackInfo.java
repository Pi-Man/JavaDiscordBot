package piman.youtube;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;

import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeTrackJsonData;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;

public class YoutubeAudioTrackInfo {
	private final Captions captions;
	private final String thumbnail;
	
	public YoutubeAudioTrackInfo(Captions captions, String thumbnail) {
		this.captions = captions;
		this.thumbnail = thumbnail;
	}
	
	public Captions getCaptions() {
		return captions;
	}
	
	public String getThumbnail() {
		return thumbnail;
	}
	
	public static JsonBrowser getVideoData(HttpInterface httpInterface, String videoID) throws IOException {
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
	
}
