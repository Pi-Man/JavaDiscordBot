package piman.youtube;

import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubePlaylistLoader;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeTrackJsonData;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.Units;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON;
import static com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.SUSPICIOUS;

public class CustomYoutubePlaylistLoader implements YoutubePlaylistLoader {
  private static final Logger log = LoggerFactory.getLogger(CustomYoutubePlaylistLoader.class);

  private static final String REQUEST_URL = "https://www.youtube.com/youtubei/v1/browse?key=AIzaSyAO_FJ2SlqU8Q4STEHLGCilw_Y9_11qcW8";
  private static final String REQUEST_PAYLOAD = "{\"context\":{\"client\":{\"clientName\":\"WEB\",\"clientVersion\":\"2.20210302.07.01\"}},\"continuation\":\"%s\"}";
  private volatile int playlistPageCount = 6;

  @Override
  public void setPlaylistPageCount(int playlistPageCount) {
    this.playlistPageCount = playlistPageCount;
  }

  @Override
  public AudioPlaylist load(HttpInterface httpInterface, String playlistId, String selectedVideoId,
                            Function<AudioTrackInfo, AudioTrack> trackFactory) {

    HttpGet request = new HttpGet(getPlaylistUrl(playlistId) + "&pbj=1&hl=en");

    try (CloseableHttpResponse response = httpInterface.execute(request)) {
      HttpClientTools.assertSuccessWithContent(response, "playlist response");
      HttpClientTools.assertJsonContentType(response);
      
      JsonBrowser json = JsonBrowser.parse(response.getEntity().getContent());
      return buildPlaylist(httpInterface, json, selectedVideoId, trackFactory);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private AudioPlaylist buildPlaylist(HttpInterface httpInterface, JsonBrowser json, String selectedVideoId,
                                      Function<AudioTrackInfo, AudioTrack> trackFactory) throws IOException {

    JsonBrowser jsonResponse = json.index(1).get("response");

    JsonBrowser alerts = jsonResponse.get("alerts");

    if (!alerts.isNull()) {
      throw new FriendlyException(alerts.index(0).get("alertRenderer").get("text").get("simpleText").text(), COMMON, null);
    }

    JsonBrowser info = jsonResponse
        .get("sidebar")
        .get("playlistSidebarRenderer")
        .get("items")
        .index(0)
        .get("playlistSidebarPrimaryInfoRenderer");

    String playlistName = info
        .get("title")
        .get("runs")
        .index(0)
        .get("text")
        .text();

    JsonBrowser playlistVideoList = jsonResponse
        .get("contents")
        .get("twoColumnBrowseResultsRenderer")
        .get("tabs")
        .index(0)
        .get("tabRenderer")
        .get("content")
        .get("sectionListRenderer")
        .get("contents")
        .index(0)
        .get("itemSectionRenderer")
        .get("contents")
        .index(0)
        .get("playlistVideoListRenderer")
        .get("contents");

    List<AudioTrack> tracks = new ArrayList<>();
    String continuationsToken = extractPlaylistTracks(httpInterface, playlistVideoList, tracks, trackFactory);
    int loadCount = 0;
    int pageCount = playlistPageCount;

    // Also load the next pages, each result gives us a JSON with separate values for list html and next page loader html
    while (continuationsToken != null && ++loadCount < pageCount) {
      HttpPost post = new HttpPost(REQUEST_URL);
      StringEntity payload = new StringEntity(String.format(REQUEST_PAYLOAD, continuationsToken), "UTF-8");
      post.setEntity(payload);
      try (CloseableHttpResponse response = httpInterface.execute(post)) {
        HttpClientTools.assertSuccessWithContent(response, "playlist response");

        JsonBrowser continuationJson = JsonBrowser.parse(response.getEntity().getContent());

        JsonBrowser playlistVideoListPage = continuationJson.index(1)
            .get("response")
            .get("continuationContents")
            .get("playlistVideoListContinuation");

        if (playlistVideoListPage.isNull()) {
          playlistVideoListPage = continuationJson.get("onResponseReceivedActions")
            .index(0)
            .get("appendContinuationItemsAction")
            .get("continuationItems");
        }

        continuationsToken = extractPlaylistTracks(httpInterface, playlistVideoListPage, tracks, trackFactory);
      }
    }

    return new BasicAudioPlaylist(playlistName, tracks, findSelectedTrack(tracks, selectedVideoId), false);
  }

  private AudioTrack findSelectedTrack(List<AudioTrack> tracks, String selectedVideoId) {
    if (selectedVideoId != null) {
      for (AudioTrack track : tracks) {
        if (selectedVideoId.equals(track.getIdentifier())) {
          return track;
        }
      }
    }

    return null;
  }

  private String extractPlaylistTracks(HttpInterface httpInterface, JsonBrowser playlistVideoList, List<AudioTrack> tracks,
                                       Function<AudioTrackInfo, AudioTrack> trackFactory) {

    if (playlistVideoList.isNull()) return null;

    final List<JsonBrowser> playlistTrackEntries = playlistVideoList.values();
    for (JsonBrowser track : playlistTrackEntries) {
      JsonBrowser item = track.get("playlistVideoRenderer");

      JsonBrowser shortBylineText = item.get("shortBylineText");

      // If the isPlayable property does not exist, it means the video is removed or private
      // If the shortBylineText property does not exist, it means the Track is Region blocked
      if (!item.get("isPlayable").isNull() && !shortBylineText.isNull()) {
        String videoId = item.get("videoId").text();
        JsonBrowser titleField = item.get("title");
        String title = Optional.ofNullable(titleField.get("simpleText").text())
                .orElse(titleField.get("runs").index(0).get("text").text());
        String author = shortBylineText.get("runs").index(0).get("text").text();
        JsonBrowser lengthSeconds = item.get("lengthSeconds");
        long duration = Units.secondsToMillis(lengthSeconds.asLong(Units.DURATION_SEC_UNKNOWN));
        
        Captions captions = Captions.CreateCaptions();
//        try {
//	        JsonBrowser videoData = getVideoInfo(httpInterface, videoId);
//	        captions = getCaptions(httpInterface, videoData);
//        }
//        catch (FriendlyException e) {
//        	throw e;
//        }
//        catch (Exception e) {
//            throw throwWithDebugInfo(log, e, "Error when extracting data", "captionsXML", videoId);
//        }

        AudioTrackInfo info = new AudioTrackInfo(title, author, duration, videoId, false,
            "https://www.youtube.com/watch?v=" + videoId);

        tracks.add(trackFactory.apply(info));
      }
    }

    JsonBrowser continuations = playlistTrackEntries.get(playlistTrackEntries.size() - 1)
        .get("continuationItemRenderer")
        .get("continuationEndpoint")
        .get("continuationCommand");

    String continuationsToken;
    if (!continuations.isNull()) {
      continuationsToken = continuations.get("token").text();
      return continuationsToken;
    }

    return null;
  }
  
  private JsonBrowser getVideoInfo(HttpInterface httpInterface, String videoID) throws IOException {
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
  
  private Captions getCaptions(HttpInterface httpInterface, JsonBrowser finalData) throws IOException {
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
      return null;
  }

  private static String getPlaylistUrl(String playlistId) {
    return "https://www.youtube.com/playlist?list=" + playlistId;
  }
}
