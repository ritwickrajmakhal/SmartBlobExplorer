# SmartBlobExplorer

A conversational AI assistant for exploring Azure Blob Storage with advanced search capabilities powered by Azure AI Search and Azure OpenAI.

## Overview

SmartBlobExplorer is a Java-based command-line interface that combines Azure Blob Storage, Azure AI Search, and Azure OpenAI to provide an intelligent document exploration experience. It allows you to:

* Upload documents to Azure Blob Storage
* Search document content using natural language queries
* Extract insights from documents using AI enrichment
* Download, rename, and delete documents through natural language commands
* Perform advanced searches with pagination support

The application uses AI cognitive skills to extract text, key phrases, entities, and other insights from your documents, making them fully searchable through a conversational interface.

## Features

- **Natural Language Interface**: Interact with your documents using natural language commands
- **Document Upload**: Upload local files or files from URLs to Azure Blob Storage
- **Smart Search**: Search through document content using Azure AI Search with pagination support
- **AI Enrichment**: Extract entities (people, organizations, locations), key phrases, and other insights
- **Cognitive Skills**: Use OCR to extract text from images within documents
- **Document Management**: Download, delete, or rename documents with simple commands
- **Batch Operations**: Support for batch uploads, downloads, and deletions

## Architecture

SmartBlobExplorer integrates several Azure services:

- **Azure Blob Storage**: Stores the documents
- **Azure AI Search**: Indexes document content with cognitive enrichment
- **Azure OpenAI**: Provides natural language understanding and function calling

The application sets up a data source, skillset, index, and indexer in Azure AI Search to process and index documents stored in Azure Blob Storage. It then uses Azure OpenAI to translate natural language commands into specific operations.

### Block Diagram

```
┌───────────────────────────────────────────────────────────────────────────┐
│                            SmartBlobExplorer                              │
└───────────────────────────────────────────────────────────────────────────┘
                                     │
          ┌────────────────────┬─────┴─────────┬────────────────┐
          │                    │               │                │
          ▼                    ▼               ▼                ▼
┌─────────────────┐  ┌──────────────────┐ ┌─────────┐ ┌────────────────┐
│     Main        │  │   BlobClient     │ │ Azure   │ │ Function       │
│ (CLI Interface) │  │                  │ │ Search  │ │ Infrastructure │
└────────┬────────┘  │ - Upload/Download│ │ Client  │ └────┬───────────┘
         │           │ - Delete/Rename  │ │         │      │
         │           │ - Batch Ops      │ └────┬────┘      │
         │           │ - List/Search    │      │           │
         │           │ - SAS Generation │      │           │
         │           │ - Snapshots      │      │           │
         │           └────────┬─────────┘      │           │
         │                    │                │           │
         │           ┌────────▼─────────┐      │           │
         │           │  Azure Blob      │      │           │
         │           │  Storage API     │      │           │
         │           └──────────────────┘      │           │
         │                                     │           │
         │                   ┌─────────────────▼─┐         │
         │                   │ Azure AI Search   │         │
         │                   │ API               │         │
         │                   └───────────────────┘         │
         │                                                 │
         │                                                 │
  ┌──────▼─────────────────────────────────────────────────▼───────┐
  │                      OpenAI Integration                        │
  │                                                                │
  │  ┌─────────────────┐      ┌────────────────┐     ┌──────────┐  │
  │  │ FunctionRegistry│      │FunctionCall    │     │ Function │  │
  │  │                 │◄────►│Registry        │◄───►│ Handlers │  │
  │  └─────────────────┘      └────────────────┘     └──────────┘  │
  │          ▲                                              ▲      │
  │          │                                              │      │
  │          │         ┌──────────────────┐                 │      │
  │          └─────────┤ OpenAI API Client├─────────────────┘      │
  │                    └──────────────────┘                        │
  └────────────────────────────────────────────────────────────────┘
                               │
                               │
                               ▼
                     ┌──────────────────┐
                     │     User CLI     │
                     │    Interface     │
                     └──────────────────┘
```

### Search Components

- **Data Source**: Connects to your Azure Blob Storage container
- **Skillset**: Defines cognitive skills for document processing and content extraction
- **Index**: Stores searchable document content and metadata
- **Indexer**: Manages the extraction, enrichment, and indexing process

## Prerequisites

- Java 11 or higher
- Maven
- An Azure account with:
  - Azure Blob Storage account
  - Azure AI Search service
  - Azure OpenAI service or Microsoft OpenAI service
  - Azure Cognitive Services (for document analysis)

> [!NOTE]  
> Ensure that the **Azure AI Search service** and **Azure Cognitive Services** are configured in the same region.

## Setup

### 1. Clone the repository

```bash
git clone https://github.com/ritwickrajmakhal/SmartBlobExplorer.git
cd SmartBlobExplorer
```

### 2. Create a .env file

Create a `.env` file in the project root with the following environment variables:

```
AZURE_AI_API_KEY=your-azure-openai-api-key-here
AZURE_AI_ENDPOINT=your-azure-openai-endpoint-url
AZURE_AI_MODEL=your-azure-ai-model-name
AZURE_AI_MODEL_DEPLOYMENT=your-azure-ai-model-deployment-name
AZURE_AI_SEARCH_ENDPOINT=your-azure-search-endpoint-url
AZURE_AI_SEARCH_API_KEY=your-azure-search-api-key
COGNITIVE_SERVICE_ACCOUNT_KEY=your-cognitive-service-account-key
```

### 3. Build the project

```bash
mvn clean package
```

## Running the Application

Run the application with:

```bash
java -jar target/SmartBlobExplorer-1.0-SNAPSHOT.jar
```

On startup, the application will prompt you for:
1. Your Azure Storage account connection string
2. Your container name
3. An optional folder path in the container (press Enter if you want to use the root)

## Usage Examples

Once the application is running, you can interact with it using natural language:

```
🧠 Welcome to SmartBlob Explorer!
Your intelligent CLI assistant for exploring Azure Blob Storage.
--------------------------------------------------------------
Please enter your storage account connection string: DefaultEndpointsProtocol=https;AccountName=**
Please enter your container name: test
Please enter your folder (path) if any otherwise press enter:

✨ Interactive chat started. Type your message or commands.
Type /? for help or /exit to quit.

>>> List all files in the container
There is currently one file in the container:

- **Photograph.jpg**
  - Size: 7.459 KB
  - Last Modified: April 29, 2025

Let me know if you need to perform any actions with this file!
>>> Upload 10th Admit.pdf from C:\Users\ritwi\Downloads demo files.
The file **10th Admit.pdf** has been successfully uploaded to the container, and the search index has been updated. Let me know if you'd like to search, organize, or perform other actions with your files!
>>>


```

### Available Commands

Besides natural language commands, you can use the following special commands:

- `/help` or `/?`: Display help information
- `/exit` or `/quit`: Exit the application

## Functions

The application supports the following operations through natural language commands:

- **Upload Files**: Upload documents to Azure Blob Storage
  - Single file upload
  - Directory upload
  - Batch upload
- **Download Blobs**: Download documents from Azure Blob Storage
  - Single blob download
  - Batch download
- **Delete Blobs**: Remove documents from Azure Blob Storage
  - Single blob deletion
  - Batch deletion
- **Rename Blobs**: Change the names of documents in Azure Blob Storage
- **Copy Blobs**: Copy blobs within or between containers
- **Create Snapshots**: Create point-in-time snapshots of blobs
- **Generate SAS URLs**: Create shared access signature URLs for secure blob access
- **Search**: Search document content with natural language queries
  - Basic search
  - Paginated search
  - Get pagination information
- **List Blobs**: View all blobs in a container or folder
- **List Local Files**: View files in a local directory

## Advanced Search Features

The search functionality includes:

- **Pagination**: Navigate through large result sets with page size control
- **Total Count**: Get the total number of matching documents
- **Result Metadata**: Access document metadata including extracted entities and key phrases
- **Suggester Support**: Enable autocomplete functionality for search queries

## Resource Management

By default, the application creates unique resource identifiers for the search components:
- Data source: `datasource-{uuid}`
- Index: `index-{uuid}`
- Skillset: `skillset-{uuid}`
- Indexer: `indexer-{uuid}`

These resources can be automatically cleaned up by calling the cleanup function when exiting the application.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Azure AI Search for document indexing and cognitive enrichment
- Azure OpenAI for natural language processing
- Azure Blob Storage for document storage
- Azure Cognitive Services for document analysis and content extraction
