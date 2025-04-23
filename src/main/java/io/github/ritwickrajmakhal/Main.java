package io.github.ritwickrajmakhal;

import io.github.cdimascio.dotenv.Dotenv;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatRequestMessage;
import com.azure.ai.openai.models.ChatRequestSystemMessage;
import com.azure.ai.openai.models.ChatRequestUserMessage;
import com.azure.ai.openai.models.ChatResponseMessage;
import com.azure.core.credential.AzureKeyCredential;

public class Main {
    final static Dotenv dotenv = Dotenv.load();

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("🧠 Welcome to SmartBlob Explorer!");
        System.out.println("Your intelligent CLI assistant for exploring Azure Blob Storage.");
        System.out.println("--------------------------------------------------------------");

        System.out.print("Please enter your storage acccount connection string: ");
        final String connectionString = sc.nextLine();

        System.out.print("Please enter your container name: ");
        final String containerName = sc.nextLine();

        System.out.print("Please enter your folder (path) if any otherwise press enter: ");
        final String folder = sc.nextLine();

        // Create a BlobContainerClient using the SAS URL
        AISearchClient searchClient = new AISearchClient(connectionString, containerName, folder);

        try {
            // Initialize OpenAI client
            OpenAIClient client = new OpenAIClientBuilder()
                    .credential(new AzureKeyCredential(dotenv.get("AZURE_AI_API_KEY")))
                    .endpoint(dotenv.get("AZURE_AI_ENDPOINT"))
                    .buildClient();

            // Start interactive chat
            startInteractiveChat(sc, client);
        } catch (Exception e) {
            System.err.println("❌ Error connecting to container: " + e.getMessage());
            System.exit(1);
        }
        searchClient.cleanUp();
        sc.close();
    }

    private static void startInteractiveChat(Scanner sc, OpenAIClient client) {
        // Initialize chat history with system message
        List<ChatRequestMessage> chatHistory = new ArrayList<>();
        chatHistory.add(new ChatRequestSystemMessage(
                "You are a helpful assistant."));

        // Welcome message
        System.out.println("\n✨ Interactive chat started. Type your message or commands.");
        System.out.println("Type /? for help or /exit to quit.\n");

        boolean chatting = true;

        while (chatting) {
            System.out.print(">>> ");
            String userInput = sc.nextLine().trim();

            // Handle special commands
            if (userInput.equalsIgnoreCase("/exit") || userInput.equalsIgnoreCase("/quit")) {
                chatting = false;
                System.out.println("Goodbye! 👋");
                continue;
            } else if (userInput.equalsIgnoreCase("/?") || userInput.equalsIgnoreCase("/help")) {
                displayHelp();
                continue;
            }

            // Handle regular chat messages
            try {
                // Add user message to history
                chatHistory.add(new ChatRequestUserMessage(userInput));

                // Get response from OpenAI
                ChatCompletions chatCompletions = client.getChatCompletions(
                        dotenv.get("AZURE_AI_MODEL_DEPLOYMENT"),
                        new ChatCompletionsOptions(chatHistory)
                                .setMaxTokens(4096)
                                .setTemperature(0.7)
                                .setTopP(0.95));

                // Display the response
                if (!chatCompletions.getChoices().isEmpty()) {
                    ChatResponseMessage message = chatCompletions.getChoices().get(0).getMessage();
                    System.out.println(message.getContent());

                    // Keep chat history from growing too large
                    if (chatHistory.size() > 21) { // system message + 10 exchanges
                        // Remove oldest user-assistant exchange but keep system message
                        chatHistory.subList(1, 3).clear();
                    }
                } else {
                    System.out.println("No response received.");
                }
            } catch (Exception e) {
                System.err.println("❌ Error: " + e.getMessage());
            }
        }
    }

    private static void displayHelp() {
        System.out.println("\n📚 Available commands:");
        System.out.println("  /?  or  /help   - Display this help message");
        System.out.println("  /exit or /quit  - Exit the application");
        System.out.println("\nFor any other input, I'll respond as your AI assistant.");
    }
}