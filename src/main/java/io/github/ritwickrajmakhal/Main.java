package io.github.ritwickrajmakhal;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.ritwickrajmakhal.handlers.SearchBlobsHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatRequestAssistantMessage;
import com.azure.ai.openai.models.ChatRequestFunctionMessage;
import com.azure.ai.openai.models.ChatRequestMessage;
import com.azure.ai.openai.models.ChatRequestSystemMessage;
import com.azure.ai.openai.models.ChatRequestUserMessage;
import com.azure.ai.openai.models.ChatResponseMessage;
import com.azure.ai.openai.models.FunctionCallConfig;
import com.azure.ai.openai.models.FunctionDefinition;
import com.azure.core.credential.AzureKeyCredential;

public class Main {
    final static Dotenv dotenv = Dotenv.load();

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("🧠 Welcome to SmartBlob Explorer!");
        System.out.println("Your intelligent CLI assistant for exploring Azure Blob Storage.");
        System.out.println("--------------------------------------------------------------");

        System.out.print("Please enter your storage account connection string: ");
        final String connectionString = sc.nextLine();

        System.out.print("Please enter your container name: ");
        final String containerName = sc.nextLine();

        System.out.print("Please enter your folder (path) if any otherwise press enter: ");
        final String folder = sc.nextLine();

        // BlobClient blobClient = new BlobClient(connectionString, containerName);

        // Initialize Azure Search client
        AzureSearchClient searchClient = new AzureSearchClient(connectionString, containerName, folder);

        try {
            // Initialize OpenAI client
            OpenAIClient client = new OpenAIClientBuilder()
                    .credential(new AzureKeyCredential(dotenv.get("AZURE_AI_API_KEY")))
                    .endpoint(dotenv.get("AZURE_AI_ENDPOINT"))
                    .buildClient();

            // Start interactive chat
            startInteractiveChat(sc, client, searchClient);
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            System.exit(1);
        } finally {
            searchClient.cleanUp();
            sc.close();
        }
    }

    private static void startInteractiveChat(Scanner sc, OpenAIClient client, AzureSearchClient searchClient) {
        List<ChatRequestMessage> chatHistory = new ArrayList<>();
        List<FunctionDefinition> functions = FunctionRegistry.getBlobFunctionDefinitions();

        // Initialize function registry
        FunctionCallRegistry functionRegistry = new FunctionCallRegistry();

        // Register function handlers
        functionRegistry.registerHandler("search_blobs", new SearchBlobsHandler(searchClient));

        chatHistory.add(new ChatRequestSystemMessage(
                "You are a helpful assistant. You can answer questions about Azure Blob Storage using the knowledge base."));

        System.out.println("\n✨ Interactive chat started. Type your message or commands.");
        System.out.println("Type /? for help or /exit to quit.\n");

        boolean chatting = true;

        while (chatting) {
            System.out.print(">>> ");
            String userInput = sc.nextLine().trim();

            if (userInput.equalsIgnoreCase("/exit") || userInput.equalsIgnoreCase("/quit")) {
                chatting = false;
                System.out.println("Goodbye! 👋");
                continue;
            } else if (userInput.equalsIgnoreCase("/?") || userInput.equalsIgnoreCase("/help")) {
                displayHelp();
                continue;
            }

            try {
                // Add user message to history
                chatHistory.add(new ChatRequestUserMessage(userInput));

                if (chatHistory.size() > 10) {
                    chatHistory = chatHistory.subList(chatHistory.size() - 10, chatHistory.size());
                }

                // Get response from OpenAI
                ChatCompletions chatCompletions = client.getChatCompletions(
                        dotenv.get("AZURE_AI_MODEL_DEPLOYMENT"),
                        new ChatCompletionsOptions(chatHistory)
                                .setMaxTokens(4096)
                                .setTemperature(0.7)
                                .setTopP(0.95).setFunctions(functions).setFunctionCall(FunctionCallConfig.AUTO));

                if (!chatCompletions.getChoices().isEmpty()) {
                    ChatResponseMessage responseMessage = chatCompletions.getChoices().get(0).getMessage();

                    if (responseMessage.getFunctionCall() != null) {
                        String functionName = responseMessage.getFunctionCall().getName();
                        String arguments = responseMessage.getFunctionCall().getArguments();

                        System.out.println("🔧 Calling function: " + functionName);

                        // Execute the function using the registry
                        if (functionRegistry.hasHandler(functionName)) {
                            try {
                                String functionResponse = functionRegistry.executeFunction(functionName, arguments);

                                // Add function call and response to chat history
                                chatHistory.add(new ChatRequestAssistantMessage(responseMessage.getContent())
                                        .setFunctionCall(responseMessage.getFunctionCall()));
                                chatHistory.add(new ChatRequestFunctionMessage(functionName, functionResponse));

                                // Get a final response from the model
                                ChatCompletions followUpCompletions = client.getChatCompletions(
                                        dotenv.get("AZURE_AI_MODEL_DEPLOYMENT"),
                                        new ChatCompletionsOptions(chatHistory)
                                                .setMaxTokens(4096)
                                                .setTemperature(0.7)
                                                .setFunctions(functions)
                                                .setFunctionCall(FunctionCallConfig.AUTO));

                                // Process the follow-up response (recursive)
                                processCompletionResponse(followUpCompletions, chatHistory, client, functionRegistry,
                                        functions);
                            } catch (Exception e) {
                                System.err.println("❌ Error executing function: " + e.getMessage());
                            }
                        } else {
                            System.err.println("⚠️ No handler registered for function: " + functionName);
                        }
                    } else {
                        // Regular text response
                        String content = responseMessage.getContent();
                        System.out.println(content);
                        chatHistory.add(new ChatRequestAssistantMessage(content));
                    }
                } else {
                    System.out.println("No response received.");
                }
            } catch (Exception e) {
                System.err.println("❌ Error: " + e.getMessage());
            }
        }
    }

    private static void processCompletionResponse(
            ChatCompletions completions,
            List<ChatRequestMessage> chatHistory,
            OpenAIClient client,
            FunctionCallRegistry functionRegistry,
            List<FunctionDefinition> functions) {

        if (!completions.getChoices().isEmpty()) {
            ChatResponseMessage message = completions.getChoices().get(0).getMessage();

            if (message.getFunctionCall() != null) {
                String functionName = message.getFunctionCall().getName();
                String arguments = message.getFunctionCall().getArguments();

                System.out.println("🔧 Calling function: " + functionName);

                if (functionRegistry.hasHandler(functionName)) {
                    try {
                        String functionResponse = functionRegistry.executeFunction(functionName, arguments);

                        chatHistory.add(new ChatRequestAssistantMessage(message.getContent())
                                .setFunctionCall(message.getFunctionCall()));
                        chatHistory.add(new ChatRequestFunctionMessage(functionName, functionResponse));

                        ChatCompletions followUpCompletions = client.getChatCompletions(
                                dotenv.get("AZURE_AI_MODEL_DEPLOYMENT"),
                                new ChatCompletionsOptions(chatHistory)
                                        .setMaxTokens(4096)
                                        .setTemperature(0.7)
                                        .setFunctions(functions)
                                        .setFunctionCall(FunctionCallConfig.AUTO));

                        processCompletionResponse(followUpCompletions, chatHistory, client, functionRegistry,
                                functions);
                    } catch (Exception e) {
                        System.err.println("❌ Error executing function: " + e.getMessage());
                    }
                } else {
                    System.err.println("⚠️ No handler registered for function: " + functionName);
                    String content = message.getContent() != null ? message.getContent()
                            : "I'm not able to access that function.";
                    System.out.println(content);
                    chatHistory.add(new ChatRequestAssistantMessage(content));
                }
            } else {
                String content = message.getContent();
                System.out.println(content);
                chatHistory.add(new ChatRequestAssistantMessage(content));
            }
        } else {
            System.out.println("No response received.");
        }
    }

    private static void displayHelp() {
        System.out.println("\n📚 Available commands:");
        System.out.println("  /?  or  /help   - Display this help message");
        System.out.println("  /exit or /quit  - Exit the application");
        System.out.println("\nFor any other input, I'll respond as your AI assistant.");
    }
}