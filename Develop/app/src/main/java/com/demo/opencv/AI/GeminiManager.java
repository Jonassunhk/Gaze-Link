package com.demo.opencv.AI;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.demo.opencv.BuildConfig;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.RequestOptions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Objects;
import java.util.concurrent.Executor;

public class GeminiManager {
   // GenerativeModel english_model = new GenerativeModel( "gemini-1.5-flash", "AIzaSyCLENjeYrgVVqwmFiOQrQyikfkteZO3WWc");
    // the different prompts for different languages
    Content englishPrompt = new Content.Builder().addText(
            "You are an AI assistant for patients with motor neuron disease to communicate in a conversation. " +
                    "Your goal is to generate a grammatical and coherent sentence with keywords they typed that fits the conversation's context." +
                            "Avoid exaggerations, emojis, or irrelevant information." +
                                "Use correct punctuation, tense, grammar, capitalization.")
            .addText("For example, for the input 'Keywords: chicken, Context: What do you want to eat for dinner'," +
                    "the output should be: 'I want to chicken for dinner.'")
           .addText("For another example, for the input 'Keywords: hot, AC, two. Context: is the room temperature ok'," +
                   "the output should be: 'I am hot, can you turn the AC down by two degrees?'")
           .build();

    Content chinesePrompt = new Content.Builder().addText(
            "你是运动神经元疾病患者的人工智能助手，用来帮助他们进行对话交流。" +
                    "你的目标是使用对话的语境和用户输入的关键字拼音来生成语法连贯的句子。" +
                            "避免夸张、表情符号或不相关的信息。" +
                            "使用正确的标点符号、时态、语法、大小写。")
            .addText("例如，如果输入是：“Keywords：ji, rou，Context：你晚餐想吃什么”," +
                    "输出就应该是: '我晚餐想吃鸡肉。'").build();

    Content spanishPrompt = new Content.Builder().addText(
                    "Eres un asistente de IA para que los pacientes con enfermedades de las neuronas motoras se comuniquen en una conversación." +
                            "Su objetivo es generar una oración gramatical y coherente con las palabras clave que escribieron y que se ajuste al contexto de la conversación." +
                            "Evite exageraciones, emojis o información irrelevante." +
                            "Utilice puntuación, tiempo, gramática y mayúsculas correctos.")
            .addText("Por ejemplo, para la entrada: 'Keywords: pollo, Context: Qué quieres cenar'," +
                    "el resultado debería ser: 'Quiero pollo para cenar.'").build();


    Context mContext;
    // fine-tuned model not available currently: tunedModels/english-sentence-generation-rxye58y1awf6

    private void sendMessage(String data) {
        Log.d("GeminiGeneration", "Broadcasting Message");
        Intent intent = new Intent("textGenerationEvent");
        intent.putExtra("message", data);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }
    public void init(Context context) {
        mContext = context;
    }
    public void generate(String keywords, String context, String language) {

        Content prompt;
        if (Objects.equals(language, "Chinese")) {
            Log.d("GeminiGeneration", "Using Chinese Prompt");
            prompt = chinesePrompt;
        } else if (Objects.equals(language, "Spanish")) {
            Log.d("GeminiGeneration", "Using Spanish Prompt");
            prompt = spanishPrompt;
        } else {
            Log.d("GeminiGeneration", "Using English Prompt");
            prompt = englishPrompt;
        }

        GenerativeModel english_model = new GenerativeModel(
                /* modelName */ "gemini-1.5-flash",
                /* apiKey */ BuildConfig.MY_GEMINI_KEY, //"AIzaSyCLENjeYrgVVqwmFiOQrQyikfkteZO3WWc",
                /* generationConfig (optional) */ null,
                /* safetySettings (optional) */ null,
                /* requestOptions (optional) */ new RequestOptions(),
                /* tools (optional) */ null,
                /* toolsConfig (optional) */ null,
                /* systemInstruction (optional) */ prompt
        );

        GenerativeModelFutures english_modelFutures = GenerativeModelFutures.from(english_model);

        Content content = new Content.Builder()
                .addText("Keywords: " + keywords + ", Context: " + context)
                .build();
        Executor executor = new Executor() {
            @Override
            public void execute(Runnable command) {
                Log.d("GeminiGeneration", "Executing command: " + command);
                command.run();
            }
        };

        ListenableFuture<GenerateContentResponse> response = english_modelFutures.generateContent(content);
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String resultText = result.getText();
                Log.d("GeminiGeneration", "Success: " + resultText);
                String taggedOutput = "SP-" + resultText;
                sendMessage(taggedOutput);
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                Log.d("GeminiGeneration", "Failure: " + t.getMessage());
                sendMessage("SP-" + t.getMessage());
            }
        }, executor);
    }
}
