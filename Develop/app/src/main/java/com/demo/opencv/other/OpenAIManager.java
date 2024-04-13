package com.demo.opencv.other;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public class OpenAIManager { // extends appCompatActivity
    private final static String OPENAI_API_KEY = ""; // do not push to github, openAI key
    HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    OpenAIService textService;
    Context mContext;
    Mp3Player mp3Player = new Mp3Player();

    public void initialize(Context context) {
        mContext = context;
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);


        // Create OkHttpClient and add interceptor
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build();

        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.openai.com/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(okHttpClient)
                .build();

        textService = retrofit.create(OpenAIService.class);
       // speech = createTempFile(mContext, "textToSpeech", ".mp3");
    }

    private void sendMessage(String data) {
        Log.d("TextGeneration", "Broadcasting Message");
        Intent intent = new Intent("textGenerationEvent");
        intent.putExtra("message", data);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }

    public void generateText(String tag, String prompt, String language) { // TODO: add tag to keep track of the usage

        // Prepare the request body
        Message messageStructure = new Message("user", prompt);
        Message messageStructure2;
        if (Objects.equals(language, "Spanish")) { // spanish system instruction
            messageStructure2 = new Message("system", "You are an AI assistant for Spanish patients with motor neuron disease to communicate in a conversation. Your goal is to generate a grammatical and coherent Spanish sentence with Spanish keywords they typed that fits the conversation's context.");
        } else if (Objects.equals(language, "Chinese")) { // chinese
            messageStructure2 = new Message("system", "你是一个帮助渐冻症病人沟通的AI助手。你的目的是通过语境和关键词的拼音生成一个完整的句子。");
        } else { // english system instruction
            messageStructure2 = new Message("system", "You are an AI assistant for patients with motor neuron disease to communicate in a conversation. Your goal is to generate a grammatical and coherent sentence with keywords they typed that fits the conversation's context.");
        }

        TextGenerationInput requestBody;
        if (Objects.equals(tag, "SP")) { // use sentence prediction model
            Message[] messages = new Message[2];
            messages[0] = messageStructure;
            messages[1] = messageStructure2;
            if (Objects.equals(language, "Spanish")) { // spanish
                Log.d("TextGeneration", "Using Spanish LLM");
                requestBody = new TextGenerationInput(messages, "ft:gpt-3.5-turbo-1106:mci::99uTQef1");
            } else if (Objects.equals(language, "Chinese")) {
                Log.d("TextGeneration", "Using Chinese LLM");
                requestBody = new TextGenerationInput(messages, "ft:gpt-3.5-turbo-1106:mci:chinese:9AGC19e1");
            } else { // english
                Log.d("TextGeneration", "Using English LLM");
                requestBody = new TextGenerationInput(messages, "ft:gpt-3.5-turbo-1106:mci::8b0Kx0tj");
            }
        } else { // Context aware, Chinese sentence generation, or next word prediction
            Message[] messages = new Message[1];
            messages[0] = messageStructure;
            requestBody = new TextGenerationInput(messages,"gpt-3.5-turbo");
        }

        textService.generateText(requestBody).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<TextGenerationOutput> call, @NonNull Response<TextGenerationOutput> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null && response.body().choices().size() != 0) {
                        String output = tag + "-" + response.body().choices().get(0).message.content;
                        sendMessage(output);
                        Log.d("TextGeneration", output);
                    } else {
                        Log.d("TextGeneration", "Error: No contents");
                    }
                } else {
                    Log.d("TextGeneration", "Error: response not successful");
                }
            }
            @Override
            public void onFailure(@NonNull Call<TextGenerationOutput> call, @NonNull Throwable t) {
                Log.d("TextGeneration", "Failure: " + t.getMessage());
            }
        });
    }

    public void speechGenerationService(String text) {

        JSONObject json = new JSONObject();
        try {
            json.put("voice", "onyx");
            json.put("input", text);
            json.put("model", "tts-1");
            json.put("speed", 0.9);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json");

        RequestBody body = RequestBody.create(json.toString(), mediaType);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/audio/speech")
                .post(body)
                .addHeader("Authorization", "Bearer " + OPENAI_API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        executorService.submit(() -> {
            try (okhttp3.Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    Log.d("SpeechGeneration", "Failed " + response);
                    throw new IOException("Unexpected code " + response);
                } else {
                    if (response.body() != null) {
                        byte[] audioFile = response.body().bytes();
                        Log.d("SpeechGeneration", "Successful");
                        mp3Player.playMp3FromBytes(audioFile, mContext);
                    } else {
                        Log.d("SpeechGeneration", "Response Body is Empty");
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    // Retrofit service interface for OpenAI
    public interface OpenAIService {
        @Headers({
                "Content-Type: application/json",
                "Authorization: Bearer " + OPENAI_API_KEY
        })
        @POST("https://api.openai.com/v1/chat/completions")
        Call<TextGenerationOutput> generateText(@Body TextGenerationInput body);
    }

    public record Message(String role, String content) {} // container for the output from the model
    public record TextGenerationInput(Message[] messages, String model) {} // container for call request to the model
    public record Choice(String finish_reason, int index, Message message) {} // Response model for the OpenAI API
    public record TextGenerationOutput(List<Choice> choices) {} // container for callback from the model

}
