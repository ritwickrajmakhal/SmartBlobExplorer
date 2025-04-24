# SmartBlobExplorer

A conversational AI assistant for exploring Azure Blob Storage with advanced search capabilities powered by Azure AI Search and Azure OpenAI.

## Overview

SmartBlobExplorer is a Java-based command-line interface that combines Azure Blob Storage, Azure AI Search, and Azure OpenAI to provide an intelligent document exploration experience. It allows you to:

* Upload documents to Azure Blob Storage
* Search document content using natural language queries
* Extract insights from documents using AI enrichment
* Download, rename, and delete documents through natural language commands

The application uses AI cognitive skills to extract text, key phrases, entities, and other insights from your documents, making them fully searchable through a conversational interface.

## Features

- **Natural Language Interface**: Interact with your documents using natural language commands
- **Document Upload**: Upload local files or files from URLs to Azure Blob Storage
- **Smart Search**: Search through document content using Azure AI Search
- **AI Enrichment**: Extract entities (people, organizations, locations), key phrases, and other insights
- **Cognitive Skills**: Use OCR to extract text from images within documents
- **Document Management**: Download, delete, or rename documents with simple commands

## Architecture

SmartBlobExplorer integrates several Azure services:

- **Azure Blob Storage**: Stores the documents
- **Azure AI Search**: Indexes document content with cognitive enrichment
- **Azure OpenAI**: Provides natural language understanding and function calling

The application sets up a data source, skillset, index, and indexer in Azure AI Search to process and index documents stored in Azure Blob Storage. It then uses Azure OpenAI to translate natural language commands into specific operations.

## Prerequisites

- Java 11 or higher
- Maven
- An Azure account with:
  - Azure Blob Storage account
  - Azure AI Search service
  - Azure OpenAI service or Microsoft OpenAI service

## Setup

### 1. Clone the repository

```bash
git clone https://github.com/ritwickrajmakhal/SmartBlobExplorer.git
cd SmartBlobExplorer
```

### 2. Create a .env file

Create a `.env` file in the project root with the following environment variables:

```
AZURE_AI_API_KEY=
AZURE_AI_ENDPOINT=
AZURE_AI_MODEL=
AZURE_AI_MODEL_DEPLOYMENT=
AZURE_AI_SEARCH_API_KEY=
AZURE_AI_SEARCH_ENDPOINT=
COGNITIVE_SERVICE_ACCOUNT_KEY=
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
>>> Upload the document C:\Users\Documents\report.pdf
🔧 Calling function: upload_file
File uploaded successfully: C:\Users\Documents\report.pdf
Search index updated successfully

>>> Search for documents about climate change
🔧 Calling function: search_blobs
I found 3 documents related to climate change:
1. climate_report_2024.pdf - Contains key phrases: global warming, carbon emissions, climate policy
2. environmental_impact.docx - Mentions organizations: EPA, IPCC, UN Environment Programme
3. sustainability_plan.pptx - Contains content discussing climate change mitigation strategies

>>> Download the climate report
🔧 Calling function: download_blob
I've downloaded climate_report_2024.pdf to your current directory.
```

### Available Commands

Besides natural language commands, you can use the following special commands:

- `/help` or `/?`: Display help information
- `/exit` or `/quit`: Exit the application

## Functions

The application supports the following operations through natural language commands:

- **Upload Files**: Upload documents to Azure Blob Storage
- **Download Blobs**: Download documents from Azure Blob Storage
- **Delete Blobs**: Remove documents from Azure Blob Storage
- **Rename Blobs**: Change the names of documents in Azure Blob Storage
- **Search**: Search document content with natural language queries

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Azure AI Search for document indexing and cognitive enrichment
- Azure OpenAI for natural language processing
- Azure Blob Storage for document storage