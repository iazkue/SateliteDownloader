# Satellite Downloader

Satellite Downloader is a robust backend application built with **Java** and **Dropwizard** for querying, previewing, and downloading satellite imagery from various providers (such as Copernicus, Landsat, and MODIS). 

This project was developed as a Final Degree Project (TFG).

## Features

- **Multi-Provider Support**: Integrates with Copernicus, Landsat, and MODIS satellite data providers.
- **Asynchronous Downloads**: Implements a Producer-Consumer architecture using a `LinkedBlockingQueue` and background Workers (`DownloadWorker`) to handle heavy image downloads without blocking the API.
- **Fast Previews**: Quickly download lightweight preview images (`_preview.png`) before committing to large data sets.
- **Database Integration**: Stores tile metadata and records in the database using **Hibernate** (`CopernicusTileDAO`, `CopernicusTileEntity`).
- **GeoJSON parsing**: Translates GeoJSON geographic coordinates to WKT or Bounding Box formats as required by the satellite providers.
- **RESTful API**: Clean REST JSON endpoints built with Jersey (JAX-RS).

## Architecture

1. **REST Resource**: `SateliteDownloaderResource` handles incoming HTTP requests.
2. **Provider Interfaces**: `CopernicusProvider`, `LandsatProvider`, and `ModisProvider` handle specific logic for contacting external satellite services.
3. **Task Queue**: Heavy tasks (like downloading raw datasets) are encapsulated into requests and pushed to a thread-safe `LinkedBlockingQueue`.
4. **Download Worker**: Background consumer threads continuously poll the queue and perform the downloads asynchronously, ensuring system stability under high load.
5. **Database**: Managed by Hibernate through Dropwizard, acting as the storage layer for downloaded tile metadata.

## Prerequisites

- **Java 17+** (Recommended)
- **Gradle** (or use the provided `gradlew` wrapper)
- A configured properties file with necessary API tokens for Copernicus.

## Setup and Installation

1. **Clone the repository**:
   ```bash
   git clone <repository_url>
   cd SateliteDownloader
   ```

2. **Configure Environment**:
   Ensure you have a `config.yml` at the root folder for Dropwizard (HTTP and Database configs) and any necessary environment variables or `.env` files for properties (`COPERNICUS_PREVIEW_FOLDER`, etc.).

3. **Build the Application**:
   Use Gradle to compile and build the shadowed JAR:
   ```bash
   ./gradlew clean build
   ```

## Running the Application

To run the server locally:

```bash
java -jar build/libs/SateliteDownloader-all.jar server config.yml
```

The application will start the Dropwizard server and HTTP endpoints will become available. By default, Dropwizard listens on port `8080`.

## API Endpoints

### 1. Health and Status
- `GET /` 
  - **Description**: Verifies if the API is up.
- `GET /health`
  - **Description**: Returns the health status of the application.

### 2. Previewing Satellite Tiles
- `POST /downloadPreviews`
  - **Description**: Searches for tiles in the given dates and area, and downloads small preview images synchronously.
  - **Payload**:
    ```json
    {
      "initialDay": "2023-01-01T00:00:00.000Z",
      "finalDay": "2023-01-31T00:00:00.000Z",
      "geoJson": { /* Standard GeoJSON Geometry */ }
    }
    ```
  - **Response**: Returns the number of downloaded previews and the image file names.

### 3. Retrieving Previews
- `GET /previews/{filename}`
  - **Description**: Returns the downloaded PNG preview image.

### 4. Downloading Satellite Datasets (Asynchronous)
- `POST /downloadImages`
  - **Description**: Puts a heavy download request into the background queue to be processed by the Producer-Consumer workers.
  - **Payload**: Same as `/downloadPreviews`
  - **Response**: Returns immediately, confirming the task was queued for background processing.

### 5. Listing Stored Tiles
- `GET /tiles`
  - **Description**: Retrieves all `CopernicusTileEntity` records stored in the database.

## Tests

The project includes an exhaustive testing suite built with **JUnit 5** and **Mockito**. It tests:
- The APIs and resource endpoints (`SateliteDownloaderResourceTest`)
- The logic within the workers (`DownloadWorkerTest`)
- The resilience and limits of the queues under stress (`QueueLoadTest`)
- Utilities such as GeoJSON conversions (`GeoJsonConverterTest`)

To run all tests:
```bash
./gradlew test
```

## Technologies Used

- [Dropwizard](https://www.dropwizard.io/): Java REST framework
- [Hibernate](https://hibernate.org/): ORM for Database Management
- [Gradle](https://gradle.org/): Build tool
- [JUnit 5 & Mockito](https://site.mockito.org/): Testing frameworks
- [Java Concurrency](https://docs.oracle.com/javase/tutorial/essential/concurrency/): Producer-Consumer architecture using queues.
