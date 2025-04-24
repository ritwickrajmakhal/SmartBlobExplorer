package io.github.ritwickrajmakhal;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.ritwickrajmakhal.handlers.DeleteBlobHandler;
import io.github.ritwickrajmakhal.handlers.DownloadBlobHandler;
import io.github.ritwickrajmakhal.handlers.RenameBlobHandler;
import io.github.ritwickrajmakhal.handlers.SearchBlobsHandler;
import io.github.ritwickrajmakhal.handlers.UploadFileHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 * Main entry point class for the SmartBlob Explorer application.
 * <p>
 * This application provides a conversational AI assistant interface to Azure
 * Blob Storage with enhanced capabilities for document search and analysis
 * through Azure AI Search. It integrates the following technologies:
 * <ul>
 * <li>Azure Blob Storage for document storage</li>
 * <li>Azure AI Search for intelligent document indexing and search</li>
 * <li>Azure OpenAI for natural language interaction</li>
 * </ul>
 * <p>
 * The application presents a command-line interface where users can interact
 * with the assistant to perform operations like:
 * <ul>
 * <li>Uploading files to blob storage</li>
 * <li>Downloading files from blob storage</li>
 * <li>Renaming or deleting blobs</li>
 * <li>Searching for documents using natural language queries</li>
 * </ul>
 * <p>
 * The application uses function calling with OpenAI to translate natural
 * language
 * requests into specific API calls to the underlying storage and search
 * services.
 */
public class Main {
    /** Logger for this class */
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    /** Environment variable loader */
    final static Dotenv dotenv = Dotenv.load();

    /**
     * Main entry point for the SmartBlob Explorer application.
     * <p>
     * This method:
     * <ol>
     * <li>Collects connection information from the user</li>
     * <li>Initializes the blob and search clients</li>
     * <li>Sets up the OpenAI client</li>
     * <li>Starts the interactive chat session</li>
     * <li>Cleans up resources when the session ends</li>
     * </ol>
     *
     * @param args Command-line arguments (not used)
     */
    public static void main(String[] args) {
        final Scanner sc = new Scanner(System.in);
        System.out.println("🧠 Welcome to SmartBlob Explorer!");
        System.out.println("Your intelligent CLI assistant for exploring Azure Blob Storage.");
        System.out.println("--------------------------------------------------------------");

        logger.info("Starting SmartBlob Explorer application");

        System.out.print("Please enter your storage account connection string: ");
        final String connectionString = sc.nextLine();

        System.out.print("Please enter your container name: ");
        final String containerName = sc.nextLine();

        System.out.print("Please enter your folder (path) if any otherwise press enter: ");
        final String folder = sc.nextLine();

        // Initialize BlobClient
        logger.info("Initializing BlobClient with container: {}", containerName);
        final BlobClient blobClient = new BlobClient(connectionString, containerName);

        // Initialize Azure Search client
        logger.info("Initializing AzureSearchClient with container: {} and folder: {}", containerName, folder);
        final AzureSearchClient searchClient = new AzureSearchClient(connectionString, containerName, folder);

        try {
            // Initialize OpenAI client
            logger.info("Initializing OpenAI client");
            final OpenAIClient client = new OpenAIClientBuilder()
                    .credential(new AzureKeyCredential(dotenv.get("AZURE_AI_API_KEY")))
                    .endpoint(dotenv.get("AZURE_AI_ENDPOINT"))
                    .buildClient();

            // Start interactive chat
            logger.info("Starting interactive chat session");
            startInteractiveChat(sc, client, searchClient, blobClient);
        } catch (Exception e) {
            logger.error("Error during application execution", e);
            System.err.println("❌ Error: " + e.getMessage());
            System.exit(1);
        } finally {
            logger.info("Cleaning up resources");
            searchClient.cleanUp();
            sc.close();
            logger.info("SmartBlob Explorer application terminated");
        }
    }

    /**
     * Starts an interactive chat session with the OpenAI model.
     * <p>
     * This method:
     * <ol>
     * <li>Sets up the chat history and function definitions</li>
     * <li>Registers function handlers for blob operations</li>
     * <li>Processes user input in a continuous loop</li>
     * <li>Handles function call responses from the model</li>
     * </ol>
     * <p>
     * The chat session continues until the user types "/exit" or "/quit".
     *
     * @param sc           The scanner for reading user input
     * @param client       The OpenAI client for generating completions
     * @param searchClient The Azure Search client for search operations
     * @param blobClient   The Blob Storage client for blob operations
     */
    private static void startInteractiveChat(
            final Scanner sc,
            final OpenAIClient client,
            final AzureSearchClient searchClient,
            final BlobClient blobClient) {
        List<ChatRequestMessage> chatHistory = new ArrayList<>();
        final List<FunctionDefinition> functions = FunctionRegistry.getBlobFunctionDefinitions();

        // Initialize function registry
        logger.debug("Initializing function registry");
        final FunctionCallRegistry functionRegistry = new FunctionCallRegistry();

        // Register function handlers
        logger.debug("Registering function handlers");
        functionRegistry.registerHandler("search_blobs", new SearchBlobsHandler(searchClient));
        functionRegistry.registerHandler("upload_file", new UploadFileHandler(blobClient, searchClient));
        functionRegistry.registerHandler("download_blob", new DownloadBlobHandler(blobClient));
        functionRegistry.registerHandler("delete_blob", new DeleteBlobHandler(blobClient, searchClient));
        functionRegistry.registerHandler("rename_blob", new RenameBlobHandler(blobClient, searchClient));

        chatHistory.add(new ChatRequestSystemMessage(
                "You are a helpful assistant. You can answer questions about Azure Blob Storage using the knowledge base."));

        System.out.println("\n✨ Interactive chat started. Type your message or commands.");
        System.out.println("Type /? for help or /exit to quit.\n");

        boolean chatting = true;

        while (chatting) {
            System.out.print(">>> ");
            final String userInput = sc.nextLine().trim();

            if (userInput.equalsIgnoreCase("/exit") || userInput.equalsIgnoreCase("/quit")) {
                chatting = false;
                System.out.println("Goodbye! 👋");
                logger.info("User requested to exit the chat");
                continue;
            } else if (userInput.equalsIgnoreCase("/?") || userInput.equalsIgnoreCase("/help")) {
                displayHelp();
                logger.debug("Help information displayed");
                continue;
            }

            try {
                // Add user message to history
                logger.debug("Processing user input: {}", userInput);
                chatHistory.add(new ChatRequestUserMessage(userInput));

                if (chatHistory.size() > 10) {
                    logger.debug("Trimming chat history to last 10 messages");
                    chatHistory = chatHistory.subList(chatHistory.size() - 10, chatHistory.size());
                }

                // Get response from OpenAI
                logger.debug("Requesting completion from OpenAI");
                final ChatCompletions chatCompletions = client.getChatCompletions(
                        dotenv.get("AZURE_AI_MODEL_DEPLOYMENT"),
                        new ChatCompletionsOptions(chatHistory)
                                .setMaxTokens(4096)
                                .setTemperature(0.7)
                                .setTopP(0.95).setFunctions(functions).setFunctionCall(FunctionCallConfig.AUTO));

                if (!chatCompletions.getChoices().isEmpty()) {
                    final ChatResponseMessage responseMessage = chatCompletions.getChoices().get(0).getMessage();

                    if (responseMessage.getFunctionCall() != null) {
                        final String functionName = responseMessage.getFunctionCall().getName();
                        final String arguments = responseMessage.getFunctionCall().getArguments();

                        logger.info("Function call detected: {}", functionName);
                        System.out.println("🔧 Calling function: " + functionName);

                        // Execute the function using the registry
                        if (functionRegistry.hasHandler(functionName)) {
                            try {
                                logger.debug("Executing function: {} with arguments: {}", functionName, arguments);
                                final String functionResponse = functionRegistry.executeFunction(functionName,
                                        arguments);
                                logger.debug("Function executed successfully");

                                // Add function call and response to chat history
                                chatHistory.add(new ChatRequestAssistantMessage(responseMessage.getContent())
                                        .setFunctionCall(responseMessage.getFunctionCall()));
                                chatHistory.add(new ChatRequestFunctionMessage(functionName, functionResponse));

                                // Get a final response from the model
                                logger.debug("Requesting follow-up completion from OpenAI");
                                final ChatCompletions followUpCompletions = client.getChatCompletions(
                                        dotenv.get("AZURE_AI_MODEL_DEPLOYMENT"),
                                        new ChatCompletionsOptions(chatHistory)
                                                .setMaxTokens(4096)
                                                .setTemperature(0.7)
                                                .setFunctions(functions)
                                                .setFunctionCall(FunctionCallConfig.AUTO));

                                // Process the follow-up response (recursive)
                                processCompletionResponse(followUpCompletions, chatHistory, client, functionRegistry,
                                        functions, 0);
                            } catch (Exception e) {
                                logger.error("Error executing function: {}", functionName, e);
                                System.err.println("❌ Error executing function: " + e.getMessage());
                            }
                        } else {
                            logger.warn("No handler registered for function: {}", functionName);
                            System.err.println("⚠️ No handler registered for function: " + functionName);
                        }
                    } else {
                        // Regular text response
                        final String content = responseMessage.getContent();
                        logger.debug("Received text response from model");
                        System.out.println(content);
                        chatHistory.add(new ChatRequestAssistantMessage(content));
                    }
                } else {
                    logger.warn("No response received from OpenAI");
                    System.out.println("No response received.");
                }
            } catch (Exception e) {
                logger.error("Error during chat processing", e);
                System.err.println("❌ Error: " + e.getMessage());
            }
        }
    }

    /**
     * Processes a completion response from the OpenAI model, handling any function
     * calls recursively.
     * <p>
     * This method:
     * <ol>
     * <li>Extracts the response message from the completions</li>
     * <li>If the message includes a function call, executes the function</li>
     * <li>Adds the function call and response to the chat history</li>
     * <li>Makes a follow-up request to the model with the updated chat history</li>
     * <li>Recursively processes any further function calls in the response</li>
     * </ol>
     * <p>
     * The method includes a recursion limit to prevent infinite function call
     * chains.
     *
     * @param completions      The completions response from the OpenAI API
     * @param chatHistory      The chat history to update with new messages
     * @param client           The OpenAI client for making additional API calls
     * @param functionRegistry The registry of function handlers
     * @param functions        The function definitions for the OpenAI API
     * @param recursionDepth   The current recursion depth (to limit potential
     *                         infinite loops)
     */
    private static void processCompletionResponse(
            final ChatCompletions completions,
            final List<ChatRequestMessage> chatHistory,
            final OpenAIClient client,
            final FunctionCallRegistry functionRegistry,
            final List<FunctionDefinition> functions,
            final int recursionDepth) {

        // Add a recursion depth limit
        final int MAX_RECURSION_DEPTH = 5;
        if (recursionDepth >= MAX_RECURSION_DEPTH) {
            logger.warn("Maximum function call recursion depth reached: {}", recursionDepth);
            System.out.println("Maximum function call chain depth reached.");
            chatHistory.add(new ChatRequestAssistantMessage(
                    "I've reached the maximum depth of function calls. Let me summarize what I found so far."));
            return;
        }

        if (!completions.getChoices().isEmpty()) {
            final ChatResponseMessage message = completions.getChoices().get(0).getMessage();

            if (message.getFunctionCall() != null) {
                final String functionName = message.getFunctionCall().getName();
                final String arguments = message.getFunctionCall().getArguments();

                logger.info("Processing function call in response: {}", functionName);
                System.out.println("🔧 Calling function: " + functionName);

                if (functionRegistry.hasHandler(functionName)) {
                    try {
                        logger.debug("Executing function: {} with arguments: {}", functionName, arguments);
                        final String functionResponse = functionRegistry.executeFunction(functionName, arguments);
                        logger.debug("Function executed successfully");

                        chatHistory.add(new ChatRequestAssistantMessage(message.getContent())
                                .setFunctionCall(message.getFunctionCall()));
                        chatHistory.add(new ChatRequestFunctionMessage(functionName, functionResponse));

                        logger.debug("Requesting follow-up completion from OpenAI at recursion depth: {}",
                                recursionDepth + 1);
                        final ChatCompletions followUpCompletions = client.getChatCompletions(
                                dotenv.get("AZURE_AI_MODEL_DEPLOYMENT"),
                                new ChatCompletionsOptions(chatHistory)
                                        .setMaxTokens(4096)
                                        .setTemperature(0.7)
                                        .setFunctions(functions)
                                        .setFunctionCall(FunctionCallConfig.AUTO));

                        processCompletionResponse(followUpCompletions, chatHistory, client, functionRegistry, functions,
                                recursionDepth + 1);
                    } catch (Exception e) {
                        logger.error("Error executing function: {}", functionName, e);
                        System.err.println("❌ Error executing function: " + e.getMessage());
                    }
                } else {
                    logger.warn("No handler registered for function: {}", functionName);
                    System.err.println("⚠️ No handler registered for function: " + functionName);
                    final String content = message.getContent() != null ? message.getContent()
                            : "I'm not able to access that function.";
                    System.out.println(content);
                    chatHistory.add(new ChatRequestAssistantMessage(content));
                }
            } else {
                final String content = message.getContent();
                logger.debug("Received text response from model at recursion depth: {}", recursionDepth);
                System.out.println(content);
                chatHistory.add(new ChatRequestAssistantMessage(content));
            }
        } else {
            logger.warn("No response received from OpenAI at recursion depth: {}", recursionDepth);
            System.out.println("No response received.");
        }
    }

    /**
     * Displays help information about available commands.
     * <p>
     * This method prints a list of the special commands that the user can type
     * to control the application.
     */
    private static void displayHelp() {
        System.out.println("\n📚 Available commands:");
        System.out.println("  /?  or  /help   - Display this help message");
        System.out.println("  /exit or /quit  - Exit the application");
        System.out.println("\nFor any other input, I'll respond as your AI assistant.");
    }
}