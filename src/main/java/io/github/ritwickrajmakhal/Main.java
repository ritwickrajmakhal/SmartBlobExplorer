package io.github.ritwickrajmakhal;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.ritwickrajmakhal.handlers.BatchDeleteHandler;
import io.github.ritwickrajmakhal.handlers.BatchDownloadHandler;
import io.github.ritwickrajmakhal.handlers.BatchUploadHandler;
import io.github.ritwickrajmakhal.handlers.CopyBlobHandler;
import io.github.ritwickrajmakhal.handlers.CreateSnapshotHandler;
import io.github.ritwickrajmakhal.handlers.DeleteBlobHandler;
import io.github.ritwickrajmakhal.handlers.DownloadBlobHandler;
import io.github.ritwickrajmakhal.handlers.GenerateSasUrlHandler;
import io.github.ritwickrajmakhal.handlers.ListBlobsHandler;
import io.github.ritwickrajmakhal.handlers.ListLocalFilesHandler;
import io.github.ritwickrajmakhal.handlers.RenameBlobHandler;
import io.github.ritwickrajmakhal.handlers.SearchBlobsHandler;
import io.github.ritwickrajmakhal.handlers.UploadFileHandler;
import io.github.ritwickrajmakhal.handlers.UploadDirectoryHandler;

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
        final FunctionCallRegistry functionRegistry = new FunctionCallRegistry();

        // Register basic blob operation function handlers
        functionRegistry.registerHandler("search_blobs", new SearchBlobsHandler(searchClient));
        functionRegistry.registerHandler("upload_file", new UploadFileHandler(blobClient, searchClient));
        functionRegistry.registerHandler("download_blob", new DownloadBlobHandler(blobClient));
        functionRegistry.registerHandler("delete_blob", new DeleteBlobHandler(blobClient, searchClient));
        functionRegistry.registerHandler("rename_blob", new RenameBlobHandler(blobClient, searchClient));

        // Register batch operation handlers
        functionRegistry.registerHandler("batch_upload", new BatchUploadHandler(blobClient, searchClient));
        functionRegistry.registerHandler("batch_download", new BatchDownloadHandler(blobClient));
        functionRegistry.registerHandler("batch_delete", new BatchDeleteHandler(blobClient, searchClient));

        // Register additional blob management handlers
        functionRegistry.registerHandler("list_blobs", new ListBlobsHandler(blobClient));
        functionRegistry.registerHandler("copy_blob", new CopyBlobHandler(blobClient));
        functionRegistry.registerHandler("create_snapshot", new CreateSnapshotHandler(blobClient));
        functionRegistry.registerHandler("generate_sas_url", new GenerateSasUrlHandler(blobClient));
        functionRegistry.registerHandler("list_local_files", new ListLocalFilesHandler(blobClient));
        functionRegistry.registerHandler("upload_directory", new UploadDirectoryHandler(blobClient, searchClient));

        chatHistory.add(new ChatRequestSystemMessage(
                "You are an intelligent assistant for SmartBlob Explorer, a tool that enhances Azure Blob Storage with AI-powered search and document analysis capabilities. Your role is to help users manage and explore their documents through natural language interaction.\r\n"
                        + //
                        "\r\n" + //
                        "You can assist with:\r\n" + //
                        "- Uploading files to blob storage (local files or from URLs)\r\n" + //
                        "- Searching through document content using natural language queries\r\n" + //
                        "- Downloading files from blob storage to the local system\r\n" + //
                        "- Renaming or moving documents within the storage container\r\n" + //
                        "- Deleting documents from storage\r\n" + //
                        "- Batch operations for uploading, downloading, or deleting multiple files at once\r\n" + //
                        "- Listing blobs with filtering by prefix or regex patterns\r\n" + //
                        "- Copying blobs while preserving the original\r\n" + //
                        "- Creating point-in-time snapshots of blobs for version control\r\n" + //
                        "- Generating temporary shared access signature (SAS) URLs for sharing files\r\n" + //
                        "- Extracting insights from documents including:\r\n" + //
                        "  * People, organizations, and locations mentioned in documents\r\n" + //
                        "  * Key phrases and important concepts\r\n" + //
                        "  * Document summarization and content analysis\r\n" + //
                        "  * Text extracted from images through OCR\r\n" + //
                        "\r\n" + //
                        "When users ask about documents, you'll use Azure AI Search to find relevant content and present results in a helpful, organized way. If users want to manipulate files, you'll determine which function to call and execute it on their behalf.\r\n"
                        + //
                        "\r\n" + //
                        "Respond conversationally but efficiently, focusing on accurately fulfilling the user's document management needs. When appropriate, suggest related capabilities that might help them accomplish their goals more effectively."));

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
                continue;
            } else if (userInput.isEmpty()) {
                continue;
            }

            try {
                // Add user message to history
                chatHistory.add(new ChatRequestUserMessage(userInput));

                if (chatHistory.size() > 10) {
                    chatHistory = chatHistory.subList(chatHistory.size() - 10, chatHistory.size());
                }

                // Get response from OpenAI
                final ChatCompletions chatCompletions = client.getChatCompletions(
                        dotenv.get("AZURE_AI_MODEL_DEPLOYMENT"),
                        new ChatCompletionsOptions(chatHistory)
                                .setMaxTokens(4096)
                                .setTemperature(0.7)
                                .setTopP(0.95).setFunctions(functions).setFunctionCall(FunctionCallConfig.AUTO));

                if (!chatCompletions.getChoices().isEmpty()) {
                    final ChatResponseMessage responseMessage = chatCompletions.getChoices().getFirst().getMessage();

                    if (responseMessage.getFunctionCall() != null) {
                        final String functionName = responseMessage.getFunctionCall().getName();
                        final String arguments = responseMessage.getFunctionCall().getArguments();

                        logger.info("Function call detected: {}", functionName);

                        // Execute the function using the registry
                        try {
                            final String functionResponse = functionRegistry.executeFunction(functionName,
                                    arguments);

                            // Add function call and response to chat history
                            chatHistory.add(new ChatRequestAssistantMessage(responseMessage.getContent())
                                    .setFunctionCall(responseMessage.getFunctionCall()));
                            chatHistory.add(new ChatRequestFunctionMessage(functionName, functionResponse));

                            // Get a final response from the model
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
                        // Regular text response
                        final String content = responseMessage.getContent();
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
            final ChatResponseMessage message = completions.getChoices().getFirst().getMessage();

            if (message.getFunctionCall() != null) {
                final String functionName = message.getFunctionCall().getName();
                final String arguments = message.getFunctionCall().getArguments();

                logger.info("Processing function call in response: {}", functionName);

                if (functionRegistry.hasHandler(functionName)) {
                    try {
                        final String functionResponse = functionRegistry.executeFunction(functionName, arguments);

                        chatHistory.add(new ChatRequestAssistantMessage(message.getContent())
                                .setFunctionCall(message.getFunctionCall()));
                        chatHistory.add(new ChatRequestFunctionMessage(functionName, functionResponse));

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
                    final String content = message.getContent() != null ? message.getContent()
                            : "I'm not able to access that function.";
                    System.out.println(content);
                    chatHistory.add(new ChatRequestAssistantMessage(content));
                }
            } else {
                final String content = message.getContent();
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