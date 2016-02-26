package io.github.voidc.np4ilr;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.text.Html;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import io.github.voidc.np4ilr.model.ILRChannel;
import io.github.voidc.np4ilr.model.ILRTrack;

public class InternetUtils {
    private static final String CHANNEL_LIST_URL = "http://www.iloveradio.de/fileadmin/config/player_5chn.xml";
    private static final String TRACK_INFO_URL = "http://www.iloveradio.de/xmlparser.php?datei=track";
    private static final String PLAYLIST_URL = "http://www.iloveradio.de/playlist.php?from=00:00&till=23:59&date=%s&channel=%s";
    private static final String COLOR_URL = "http://www.iloveradio.de/fileadmin/templates/css/ilr3.main.wide.css";
    private static List<ILRChannel> channelCache = new ArrayList<>();
    private static Map<String, Bitmap> coverCache = new HashMap<>();
    private static Map<ILRChannel, ILRTrack> trackCache = new HashMap<>();
    private static int[] colorCache;


    public static List<ILRChannel> fetchChannels() throws IOException {
        channelCache.clear();
        try {
            URL url = new URL(CHANNEL_LIST_URL);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(url.openStream()));
            doc.getDocumentElement().normalize();

            NodeList channelList = doc.getFirstChild().getChildNodes();

            for (int i = 0; i < channelList.getLength(); i++) {
                if (!(channelList.item(i) instanceof Element)) continue;
                Element channelElement = (Element) channelList.item(i);
                int id = Integer.parseInt(channelElement.getAttribute("id"));
                String name = channelElement.getElementsByTagName("name").item(0).getTextContent();
                String description = channelElement.getElementsByTagName("description").item(0).getTextContent();
                String streamURI = channelElement.getElementsByTagName("ip").item(0).getTextContent();
                channelCache.add(new ILRChannel(id, name, description, streamURI));
            }
        } catch (ParserConfigurationException | SAXException e) {
            e.printStackTrace();
        }
        return channelCache;
    }

    public static ILRChannel getChannelById(int channelId) {
        for (ILRChannel c : channelCache) {
            if (c.getId() == channelId) return c;
        }
        return null;
    }

    public static ILRTrack fetchTrack(ILRChannel channel) throws IOException {
        ILRTrack track = null;
        try {
            URL url = new URL(TRACK_INFO_URL + (channel.getId() == 1 ? "" : channel.getId()));
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(url.openStream()));

            Element trackElement = doc.getDocumentElement();
            trackElement.normalize();

            String artist = Html.fromHtml(trackElement.getElementsByTagName("artist").item(0).getTextContent()).toString();
            String title = Html.fromHtml(trackElement.getElementsByTagName("title").item(0).getTextContent()).toString();
            String image = ((Element) trackElement.getElementsByTagName("image").item(0)).getAttribute("src");

            track = new ILRTrack(artist, title, image);
        } catch (ParserConfigurationException | SAXException e) {
            e.printStackTrace();
        }
        trackCache.put(channel, track);
        return track;
    }

    public static List<ILRTrack> fetchPlaylist(ILRChannel channel, Date date) throws IOException {
        List<ILRTrack> playlist = new ArrayList<>();
        String dateString = new SimpleDateFormat("yyyy-MM-dd").format(date);
        String playlistName = URLEncoder.encode(getPlaylistName(channel.getName()), "utf-8");
        try {
            URL url = new URL(String.format(PLAYLIST_URL, dateString, playlistName));
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(url.openStream()));
            doc.getDocumentElement().normalize();

            NodeList trackList = doc.getFirstChild().getChildNodes();
            SimpleDateFormat timeFormat = new SimpleDateFormat("yyy-MM-dd HH:mm");

            for (int i = 0; i < trackList.getLength(); i++) {
                if (!(trackList.item(i) instanceof Element)) continue;
                Element trackElement = (Element) trackList.item(i);
                String artist = Html.fromHtml(trackElement.getElementsByTagName("artist").item(0).getTextContent()).toString();
                String title = Html.fromHtml(trackElement.getElementsByTagName("title").item(0).getTextContent()).toString();
                String cover = trackElement.getElementsByTagName("cover").item(0).getTextContent();
                String time = trackElement.getElementsByTagName("time").item(0).getTextContent();
                Date timeStamp = timeFormat.parse(dateString + " " + time);
                playlist.add(new ILRTrack(artist, title, cover, timeStamp));
            }
        } catch (ParserConfigurationException | SAXException | ParseException e) {
            e.printStackTrace();
        }
        return playlist;
    }

    private static String getPlaylistName(String channelName) {
        switch (channelName) {
            case "BRAVO PARTY":
                return "MYPARTY";
            case "BRAVO TUBESTARS":
                return "THE BATTLE";
            case "THE BATTLE":
                return "YOU";
            default:
                return channelName;
        }
    }

    /*
    TODO:
    - test
    - improve bitmap handling (chaching, small icons for channel selection)
    - fetch bitmaps after channels loaded and displayed
     */

    public static Bitmap fetchCoverArt(ILRTrack track) throws IOException {
        if (coverCache.keySet().contains(track.getImageURI())) {
            return coverCache.get(track.getImageURI());
        }
        URL url = new URL(track.getImageURI());
        Bitmap cover = null;
        InputStream in = null;
        try {
            in = url.openStream();
            cover = BitmapFactory.decodeStream(in);
        } finally {
            if (in != null) in.close();
        }
        coverCache.put(track.getImageURI(), cover);
        return cover;
    }

    public static void fetchChannelColors() throws IOException {
        colorCache = new int[channelCache.size()];
        BufferedReader in = null;
        try {
            URL url = new URL(COLOR_URL);
            in = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;
            int colorIndex = 0;
            boolean readColor = false;
            for (int i = 0; (line = in.readLine()) != null; i++) {
                if (readColor) {
                    String colorString = line.substring(14, line.length() - 1);
                    if (colorString.length() == 4) {
                        colorString = new String(new char[]{'#',
                                colorString.charAt(1), colorString.charAt(1),
                                colorString.charAt(2), colorString.charAt(2),
                                colorString.charAt(3), colorString.charAt(3)
                        });
                    }
                    colorCache[colorIndex++] = Color.parseColor(colorString);
                    readColor = false;
                } else if (line.startsWith(".channel.channel") && line.length() <= 20) {
                    readColor = true;
                }

                if (i > 400 && line.equals("}")) {
                    break;
                }
            }
        } catch (Exception e) {
            if (e instanceof IOException)
                throw e;
            e.printStackTrace();
        } finally {
            if (in != null) in.close();
        }
    }

    /**
     * The cached track may be obsolete
     */
    public static ILRTrack getCachedTrack(ILRChannel c) {
        if (trackCache.keySet().contains(c)) {
            return trackCache.get(c);
        } else return null;
    }

    public static Bitmap getCachedCoverArt(ILRTrack track) {
        if (coverCache.keySet().contains(track.getImageURI())) {
            return coverCache.get(track.getImageURI());
        } else return null;
    }


    public static int getChannelColor(int channelId) {
        if (channelId > colorCache.length)
            return Color.WHITE;
        return colorCache[channelId - 1];
    }

}
