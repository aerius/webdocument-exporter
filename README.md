# PDF Exporter

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=aerius_webdocument-exporter&metric=alert_status)](https://sonarcloud.io/dashboard?id=aerius_webdocument-exporter)

## Introduction

This simple library performs the task of exporting a web document to either PDF or PNG, and optionally postprocessing the result. It accomplishes this by sending a document (via url) to a Chromium-type instance, and performing either a PDF export, or snapping a screenshot.

## Quick Start

```xml
<dependency>
  <groupId>nl.aerius</groupId>
  <artifactId>webdocument-exporter</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

and make sure to have a chromium-type instance running, for example:

```shell
docker run -d --net=host -p 9222:9222 --cap-add=SYS_ADMIN justinribeiro/chrome-headless
```

## Usage

### PDF Export

To export a web page to PDF;

```java
ExportJob job = ExportJob.create()
  .url(url)
  .print();
  
// A byte[] array of the resulting job
byte[] bytes = job.result();

// The printed document will be available at 'job.outputDocument()'
job.save();
```

Or, to post-process the resulting PDF document:

```java
job
  .toProcessor()
  .target(destination)
  // Add a front page - by prepending the first page of another PDF document to the document being processed.
  // This can be useful because correctly formatting a front page inside a web document can be hard
  .frontPage(frontPageDocument)
  // Add a title to the bottom-left of the document
  .documentTitle(title)
  // Add a subtitle to the bottom-left of the document, right under the title
  .documentSubtitle(subtitle)
  // Add a Map<String, String> type meta data to the document
  .metaData(meta)
  // Add page numbers to the bottom-right of the document
  .pageNumbers()
  // Process the document using the definitions set above
  .process();

// The post-processed document will be available at 'destination'
```

It is possible to add custom processors to the post-processing step, via:

```java
job
  .toProcessor()
  .documentProcessor((document) -> {
    // Manipulate the document in some way
  })
  .pageProcessor((document, page, number) -> {
    // Manipulate each page (by number) of the document in some way
  })
  .process();
```

It is also possible to forego the print job, and process an existing PDF, as per the following example:

```java
PdfProcessingHandle.create(pdfDocument)
  .target(destination)
  .pageNumbers()
  .title(title)
  .process();
```

### Snapshot Export

To export a web page to PNG;

```java
ExportJob job = ExportJob.create()
  .url(url)
  .snapshot();

// A byte[] array of the resulting job
byte[] bytes = job.result();

// The screenshotted image will be available at 'job.outputDocument()'
job.save();
```

## Chromium

A chromium-headless server (or fork) must be running to facilitate the exporting of the document. By default, this server is assumed to be running on `localhost:9222`

The simplest way to get this type of server up and running for development environments is via docker:

```shell
docker run -d --net=host -p 9222:9222 --cap-add=SYS_ADMIN justinribeiro/chrome-headless
```

If the chromium-headless server ends up running somewhere other than `localhost:9222`, it can be configured with `ExportJob.chromeHost(String)`.
