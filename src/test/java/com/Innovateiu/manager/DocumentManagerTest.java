package com.Innovateiu.manager;

import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.Innovateiu.manager.DocumentManager.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentManagerTest {

    private DocumentManager documentManager;

    private final Faker faker = new Faker();

    @BeforeEach
    void setUp() {
        documentManager = new DocumentManager();
    }

    @Test
    void save_shouldGenerateIdAndCreatedDate_whenNewDocument() {
        Document newDocument = generateDocument();
        Document savedDocument = documentManager.save(newDocument);

        assertThat(savedDocument.getId()).isNotNull();
        assertThat(savedDocument.getCreated()).isNotNull();
        assertThat(savedDocument.getAuthor().getId()).isNotNull();
        assertThat(savedDocument).usingRecursiveComparison()
                .ignoringFields("created", "author.id", "id")
                .isEqualTo(newDocument);
    }

    @Test
    void save_shouldUpdateDocument_whenDocumentIsExist() {
        Document savedDocument = documentManager.save(generateDocument());

        Document newDocumentForUpdate = generateDocument();
        newDocumentForUpdate.setId(savedDocument.getId());
        newDocumentForUpdate.setAuthor(Author.builder()
                .id(savedDocument.getAuthor().getId())
                .name(newDocumentForUpdate.getAuthor().getName())
                .build());
        newDocumentForUpdate.setCreated(savedDocument.getCreated());

        Document updatedDocument = documentManager.save(newDocumentForUpdate);

        assertThat(updatedDocument).usingRecursiveComparison().isEqualTo(newDocumentForUpdate);
    }

    @Test
    void save_shouldThrowException_whenDocumentIsNull() {
        assertThatThrownBy(() -> documentManager.save(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Document cannot be null");
    }

    @Test
    void save_shouldThrowException_whenDocumentHasIdButNotExistsInStorage() {
        Document document = generateDocument();
        document.setId(UUID.randomUUID().toString());

        assertThatThrownBy(() -> documentManager.save(document))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findById_shouldReturnDocument_whenDocumentIsExist() {
        Document savedDocument = documentManager.save(generateDocument());

        assertThat(savedDocument.getId()).isNotEmpty();
    }

    @Test
    void findById_shouldReturnEmpty_whenDocumentNotFound() {
        assertThat(documentManager.findById(UUID.randomUUID().toString())).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("searchRequest")
    void search_shouldReturnDocuments_whenHaveSpecificSearchRequest(SearchRequest searchRequest, int expectedSize) {
        IntStream.rangeClosed(1, 10)
                .forEach(it -> {
                    var document = generateDocument();
                    document.setTitle("dummy title");
                    document.setContent("dummy content");
                    documentManager.save(document);
                });

        assertThat(documentManager.search(searchRequest)).hasSize(expectedSize);
    }

    private static Stream<Arguments> searchRequest() {
        return Stream.of(
                Arguments.of(
                        SearchRequest.builder().build(),
                        10
                ),
                Arguments.of(
                        SearchRequest.builder().
                                createdFrom(Instant.now().minusSeconds(2)).
                                createdTo(Instant.now().plusSeconds(5)).build(),
                        10
                ),
                Arguments.of(
                        SearchRequest.builder().titlePrefixes(List.of("dummy title")).build(),
                        10
                ),
                Arguments.of(
                        SearchRequest.builder().containsContents(List.of("dummy content")).build(),
                        10
                ),
                Arguments.of(
                        SearchRequest.builder().
                                titlePrefixes(List.of("dummy title")).
                                containsContents(List.of("dummy content")).build(),
                        10
                ),
                Arguments.of(
                        SearchRequest.builder().
                                titlePrefixes(List.of("pom-pom")).
                                containsContents(List.of("pip-pip")).build(),
                        0
                ));
    }

    private Document generateDocument() {
        return Document.builder()
                .title(faker.book().title())
                .content(faker.lorem().sentence(5))
                .author(generateAuthor())
                .build();
    }

    private Author generateAuthor() {
        return Author.builder()
                .name(faker.book().author())
                .build();
    }

}