package com.demo.opencv;

import android.content.Context;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.util.Log;

import com.github.scribejava.apis.TwitterApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth10aService;

import org.opencv.core.Mat;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

public class TwitterClient {
    private static final String API_KEY = "LMBdIxtET2HNzYRdiXQcqMWqd";
    private static final String API_SECRET = "6n68iFVBaZO6JAflA2uTIBF9m17VgeWlw2Bu3U4xZQqJvMysm9";
    private static final String ACCESS_TOKEN = "1772801260426842112-307U1uZN8QLqu6WxYUVGln9NbiclK7";
    private static final String ACCESS_TOKEN_SECRET = "ZFTuDJ00TEu64aRy6UQ4Kt7KrMCk6p1kptbW9ptYbrbrW";
    OAuth10aService service;
    OAuth1AccessToken accessToken;
    private static final String TWEET_ENDPOINT = "https://api.twitter.com/2/tweets";

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public void init() { // init the client with the user's access token and secret
        // Initialize OAuth service
        service = new ServiceBuilder(API_KEY)
                .apiSecret(API_SECRET)
                .build(TwitterApi.instance());
        // Create access token
        accessToken = new OAuth1AccessToken(ACCESS_TOKEN, ACCESS_TOKEN_SECRET);
    }

    public void uploadTweetWithPhoto(Context mContext, Mat image, String text) {

        Bitmap bitmap = ImageFormatUtils.matToBitmap(image);
        File imageFile = ImageFormatUtils.saveBitmapAsPng(mContext, bitmap, "TwitterPhoto");

        executorService.submit(() -> {
            ConfigurationBuilder cb = new ConfigurationBuilder();
            cb.setDebugEnabled(true)
                    .setOAuthConsumerKey(API_KEY)
                    .setOAuthConsumerSecret(API_SECRET)
                    .setOAuthAccessToken(ACCESS_TOKEN)
                    .setOAuthAccessTokenSecret(ACCESS_TOKEN_SECRET);

            TwitterFactory tf = new TwitterFactory(cb.build());
            Twitter twitter = tf.getInstance();
            try {
                long MediaId = twitter.uploadMedia(imageFile).getMediaId();
                Log.d("SocialMediaPost", "Obtained image ID: " + MediaId);
                sendTweet(text, String.valueOf(MediaId));
            } catch (TwitterException e) {
                Log.d("SocialMediaPost", "Failed to obtain image ID: " + e);
                throw new RuntimeException(e);
            }
        });
    }

    public void sendTweet(String tweetText, String mediaID) {
        executorService.submit(() -> {
            try {
                // Prepare request
                OAuthRequest request = new OAuthRequest(Verb.POST, TWEET_ENDPOINT);
                request.addHeader("Content-Type", "application/json");
                if (!Objects.equals(mediaID, "")) {
                    String jsonPayload = "{"
                            + "\"text\":\"" + tweetText + "\","
                            + "\"media\":{\"media_ids\":[\"" + mediaID + "\"]}"
                            + "}";
                    request.setPayload(jsonPayload);
                } else {
                    request.setPayload("{\"text\":\"" + tweetText + "\"}");
                }

                service.signRequest(accessToken, request);

                // Send request
                Response response = service.execute(request);
                Log.d("SocialMediaPost", "Post successful: " + response);
            } catch (Exception e) {
                e.printStackTrace();
                Log.d("SocialMediaPost", "Post failed: " + e);
            }
        });
    }
}
