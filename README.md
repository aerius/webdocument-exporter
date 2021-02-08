# PDF Exporter

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
docker run -d --net=host --name=chrome-headless -p 9222:9222 --rm --cap-add=SYS_ADMIN 857d0939726d
```

## Usage

### PDF Export

To export a web page to PDF;

```java
ExportJob job = ExportJob.create()
  .url(url)
  .print();

// The printed document will be available at 'job.outputDocument()'
```

To post-process the resulting PDF document:

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
  .target(destination)
  // Add a document processor
  .documentProcessor((document) -> {
    // Manipulate the document in some way
  })
  .pageProcessor((document, page, number) -> {
    // Manipulate each page (by number) of the document in some way
  })
  .process();
```

### Snapshot Export

To export a web page to PNG;

```java
ExportJob job = ExportJob.create()
  .url(url)
  .snapshot();

// The screenshotted image will be available at 'job.outputDocument()'
```

## Chromium

A chromium-headless server (or fork) must be running to facilitate the exporting of the document. By default, this server is assumed to be running on `localhost:9222`

The simplest way to get this type of server up and running for development environments is via docker:

```shell
docker run -d --net=host --name=chrome-headless -p 9222:9222 --rm --cap-add=SYS_ADMIN 857d0939726d
```

If the chromium-headless server ends up running somewhere other than `localhost:9222`, it can be configured with `ExportJob.chromeHost(String)`.
