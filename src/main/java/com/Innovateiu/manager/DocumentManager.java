package com.Innovateiu.manager;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.*;

/**
 * For implement this task focus on clear code, and make this solution as simple readable as possible
 * Don't worry about performance, concurrency, etc
 * You can use in Memory collection for sore data
 * <p>
 * Please, don't change class name, and signature for methods save, search, findById
 * Implementations should be in a single class
 * This class could be auto tested
 */
public class DocumentManager {

    private final Map<String, Document> storage = new HashMap<>();

    /**
     * Implementation of this method should upsert the document to your storage
     * And generate unique id if it does not exist, don't change [created] field
     *
     * @param document - document content and author data
     * @return saved document
     */

    public Document save(Document document) {
        if (Objects.isNull(document)) {
            throw new IllegalArgumentException("Document cannot be null");
        }

        if (Objects.isNull(document.getId()) || document.getId().isEmpty()) {
            return save(document, document.getAuthor());
        }

        if (storage.containsKey(document.getId())) {
            return update(document, document.getAuthor());
        }

        throw new IllegalArgumentException("Document with ID '" + document.getId() + "' not found.");
    }

    private Document save(Document document, Author author) {
        Document newDocument = Document.builder()
                .id(generateUUID())
                .title(document.getTitle())
                .content(document.getContent())
                .author(Objects.isNull(author) ? null : Author.builder()
                        .id(generateUUID())
                        .name(author.getName())
                        .build())
                .created(Instant.now())
                .build();

        storage.put(newDocument.getId(), newDocument);

        return newDocument;
    }

    private Document update(Document document, Author author) {
        var oldDocument = storage.get(document.getId());
        var updatedDocument = Document.builder()
                .id(oldDocument.getId())
                .title(document.getTitle())
                .content(document.getContent())
                .author(Author.builder()
                        .id(oldDocument.getAuthor().getId())
                        .name(Objects.isNull(author) ? null : author.getName()).build())
                .created(oldDocument.getCreated())
                .build();

        storage.put(updatedDocument.getId(), updatedDocument);

        return updatedDocument;
    }

    /**
     * Implementation this method should find document by id
     *
     * @param id - document id
     * @return optional document
     */
    public Optional<Document> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    /**
     * Implementation this method should find documents which match with request
     *
     * @param request - search request, each field could be null
     * @return list matched documents
     */
    public List<Document> search(SearchRequest request) {
        return storage.values().stream()
                .filter(doc -> matchesSearchRequest(doc, request))
                .toList();
    }

    private boolean matchesSearchRequest(Document doc, SearchRequest request) {
        return matchesTitlePrefixes(doc.getTitle(), request.getTitlePrefixes())
                && matchesContent(doc.getContent(), request.getContainsContents())
                && matchesAuthor(doc.getAuthor(), request.getAuthorIds())
                && matchesCreatedFrom(doc.getCreated(), request.getCreatedFrom())
                && matchesCreatedTo(doc.getCreated(), request.getCreatedTo());
    }

    private boolean matchesTitlePrefixes(String title, List<String> titlePrefixes) {
        if (Objects.isNull(titlePrefixes) || titlePrefixes.isEmpty()) {
            return true;
        }

        return Objects.nonNull(title) && titlePrefixes.stream().anyMatch(title::startsWith);
    }

    private boolean matchesContent(String content, List<String> containsContents) {
        if (Objects.isNull(containsContents) || containsContents.isEmpty()) {
            return true;
        }

        return Objects.nonNull(content) && containsContents.contains(content);
    }

    private boolean matchesAuthor(Author author, List<String> authorIds) {
        if (Objects.isNull(authorIds) || authorIds.isEmpty()) {
            return true;
        }

        return Objects.nonNull(author) && Objects.nonNull(author.getId()) && authorIds.contains(author.getId());
    }

    private boolean matchesCreatedFrom(Instant created, Instant createdFrom) {
        return Objects.isNull(createdFrom) || (Objects.nonNull(created) && !created.isBefore(createdFrom));
    }

    private boolean matchesCreatedTo(Instant created, Instant createdTo) {
        return Objects.isNull(createdTo) || (Objects.nonNull(created) && !created.isAfter(createdTo));
    }

    private String generateUUID() {
        return UUID.randomUUID().toString();
    }

    @Data
    @Builder
    public static class SearchRequest {
        private List<String> titlePrefixes;
        private List<String> containsContents;
        private List<String> authorIds;
        private Instant createdFrom;
        private Instant createdTo;
    }

    @Data
    @Builder
    public static class Document {
        private String id;
        private String title;
        private String content;
        private Author author;
        private Instant created;
    }

    @Data
    @Builder
    public static class Author {
        private String id;
        private String name;
    }

}