# PDF Exporter

## Introduction

This simple library performs the task of exporting a web document to either PDF or PNG, and optionally postprocessing the result. It accomplishes this by sending a document (via url) to a Chromium-type instance, and performing either a PDF export, or snapping a screenshot.

## Quick Start

```xml
<dependency>
  <groupId>nl.aerius</groupId>
  <artifactId>aerius-webdocument-exporter</artifactId>
  <version>1.1-SNAPSHOT</version>
</dependency>
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