package com.inzinjeri.rs_projekat.services;

import com.inzinjeri.rs_projekat.exceptions.GitHubException;
import com.inzinjeri.rs_projekat.exceptions.BadRequestException;
import com.inzinjeri.rs_projekat.utils.GitHubFiles;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.InetSocketAddress;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.zip.ZipInputStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class GitHubServiceTest {
    private static final String HEAD = "1".repeat(40);
    private static final String NEXT = "2".repeat(40);
    private static final String ROOT = "a".repeat(40);
    private static final String SUBJECT = "b".repeat(40);
    private static final String GROUP = "c".repeat(40);
    private static final String FOLDER = "d".repeat(40);
    private static final String BLOB = "e".repeat(40);
    private static final String NEW_TREE = "f".repeat(40);
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final Queue<Reply> replies = new ConcurrentLinkedQueue<>();
    private final List<Call> calls = new CopyOnWriteArrayList<>();
    private HttpServer server;
    private GitHubService service;
    private volatile String serverFailure;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().toString();
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            calls.add(new Call(exchange.getRequestMethod(), path, body,
                    exchange.getRequestHeaders().getFirst("Authorization")));
            Reply reply = replies.poll();
            if (reply == null || !reply.method.equals(exchange.getRequestMethod()) || !reply.path.equals(path)) {
                serverFailure = "Unexpected request " + exchange.getRequestMethod() + " " + path + "; expected " + reply;
                reply = new Reply("", "", 500, "{}");
            }
            byte[] response = reply.body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(reply.status, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        service = new GitHubService("owner", "test-secret",
                RestClient.builder().baseUrl("http://127.0.0.1:" + server.getAddress().getPort()).build());
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
        assertNull(serverFailure);
        assertTrue(replies.isEmpty(), "Expected requests not performed: " + replies);
    }

    @Test
    void missingConfigurationAndWrongOwnerNeverSendRequests() {
        assertEquals(503, assertThrows(GitHubException.class,
                () -> new GitHubService("", "").head("owner/repo")).getStatus());
        assertEquals(400, assertThrows(GitHubException.class, () -> service.head("other/repo")).getStatus());
        assertTrue(calls.isEmpty());
    }

    @Test
    void privateRepositoryAndTokenAreChecked() {
        repo(false);
        assertEquals(409, assertThrows(GitHubException.class, () -> service.verifyRepository("owner/repo")).getStatus());
        assertEquals("Bearer test-secret", calls.getFirst().authorization);
    }

    @Test
    void upstreamErrorDoesNotExposeTokenOrResponse() {
        expect("GET", "", 403, "{\"message\":\"test-secret upstream-internal\"}");
        GitHubException error = assertThrows(GitHubException.class, () -> service.head("owner/repo"));
        assertFalse(error.getMessage().contains("test-secret"));
        assertFalse(error.getMessage().contains("upstream-internal"));
        assertEquals(503, error.getStatus());
    }

    @Test
    void readReturnsOriginalBinaryBytesFromPinnedCommitAndOnlyRequestedFolder() {
        repo(true);
        folderAt(HEAD, ROOT, FOLDER);
        recursive(FOLDER, false, entry("data.bin", "blob", "100644", BLOB, 3));
        expect("GET", "git/blobs/" + BLOB, 200,
                "{\"encoding\":\"base64\",\"size\":3,\"content\":\"AP+A\"}");

        var result = service.read("owner/repo", HEAD, "Subject/Group/student", "data.bin");
        assertArrayEquals(new byte[]{0, (byte) 255, (byte) 128}, result.content());
        assertTrue(result.binary());
        assertEquals(HEAD, result.commit());
        assertEquals(BLOB, result.sha());
        assertTrue(calls.stream().noneMatch(c -> c.path.contains("archive") || c.path.contains("contents")));
    }

    @Test
    void incompleteTreeAndSymlinksAreRejected() {
        repo(true);
        folderAt(HEAD, ROOT, FOLDER);
        recursive(FOLDER, true, "");
        assertEquals(502, assertThrows(GitHubException.class,
                () -> service.tree("owner/repo", HEAD, "Subject/Group/student")).getStatus());

        repo(true);
        folderAt(HEAD, ROOT, FOLDER);
        recursive(FOLDER, false, entry("secret", "blob", "120000", BLOB, 5));
        assertEquals(409, assertThrows(GitHubException.class,
                () -> service.tree("owner/repo", HEAD, "Subject/Group/student")).getStatus());
    }

    @Test
    void publishEncodesBytesAndCommitsOnlyNewFolderWithoutForce() {
        repo(true);
        head(HEAD);
        emptyRoot(HEAD, ROOT);
        writeObjects(HEAD, ROOT);
        expect("PATCH", "git/refs/heads/main", 200, "{}");

        String sha = service.publish("owner/repo", "Subject/Group/student",
                List.of(new GitHubService.UploadFile("data.bin", new byte[]{0, (byte) 255})), null);

        assertEquals(NEXT, sha);
        JsonNode blob = mapper.readTree(calls.stream().filter(c -> c.path.endsWith("/git/blobs")).findFirst().orElseThrow().body);
        assertEquals("base64", blob.get("encoding").asString());
        assertArrayEquals(new byte[]{0, (byte) 255}, Base64.getDecoder().decode(blob.get("content").asString()));
        List<Call> trees = calls.stream().filter(c -> c.method.equals("POST") && c.path.endsWith("/git/trees")).toList();
        assertEquals(ROOT, mapper.readTree(trees.get(1).body).get("base_tree").asString());
        assertEquals("Subject/Group/student", mapper.readTree(trees.get(1).body).get("tree").get(0).get("path").asString());
        assertFalse(mapper.readTree(calls.getLast().body).get("force").asBoolean());
    }

    @Test
    void existingUnownedFolderIsNeverOverwritten() {
        repo(true);
        head(HEAD);
        folderAt(HEAD, ROOT, FOLDER);
        assertEquals(409, assertThrows(GitHubException.class, () -> service.publish("owner/repo",
                "Subject/Group/student", List.of(new GitHubService.UploadFile("A.java", new byte[]{1})), null)).getStatus());
        assertTrue(calls.stream().noneMatch(c -> c.method.equals("POST")));
    }

    @Test
    void staleEditorVersionDoesNotWriteAnything() {
        repo(true);
        head(HEAD);
        folderAt(HEAD, ROOT, FOLDER);
        recursive(FOLDER, false, entry("A.java", "blob", "100644", BLOB, 1));
        assertEquals(409, assertThrows(GitHubException.class, () -> service.edit("owner/repo",
                "Subject/Group/student", "A.java", "0".repeat(40), "new text")).getStatus());
        assertTrue(calls.stream().noneMatch(c -> c.method.equals("POST")));
    }

    @Test
    void branchAdvancementInOtherFolderIsRetriedWithoutForce() {
        repo(true);
        head(HEAD);
        emptyRoot(HEAD, ROOT);
        writeObjects(HEAD, ROOT);
        expect("PATCH", "git/refs/heads/main", 422, "{\"message\":\"not fast forward\"}");
        head("3".repeat(40));
        emptyRoot("3".repeat(40), "4".repeat(40));
        commitObjects();
        expect("PATCH", "git/refs/heads/main", 200, "{}");

        assertEquals(NEXT, service.publish("owner/repo", "Subject/Group/student",
                List.of(new GitHubService.UploadFile("a", new byte[]{1})), null));
        assertEquals(2, calls.stream().filter(c -> c.method.equals("PATCH")).count());
        List<Call> rootTrees = calls.stream().filter(c -> c.method.equals("POST")
                && c.path.endsWith("/git/trees") && mapper.readTree(c.body).has("base_tree")).toList();
        assertEquals("4".repeat(40), mapper.readTree(rootTrees.getLast().body).get("base_tree").asString());
        Call lastCommit = calls.stream().filter(c -> c.path.endsWith("/git/commits") && c.method.equals("POST"))
                .toList().getLast();
        assertEquals("3".repeat(40), mapper.readTree(lastCommit.body).get("parents").get(0).asString());
    }

    @Test
    void branchConflictDoesNotOverwriteFolderCreatedByOtherRequest() {
        repo(true);
        head(HEAD);
        emptyRoot(HEAD, ROOT);
        writeObjects(HEAD, ROOT);
        expect("PATCH", "git/refs/heads/main", 422, "{}");
        head("3".repeat(40));
        folderAt("3".repeat(40), ROOT, FOLDER);

        assertEquals(409, assertThrows(GitHubException.class,
                () -> service.publish("owner/repo", "Subject/Group/student",
                        List.of(new GitHubService.UploadFile("a", new byte[]{1})), null)).getStatus());
        assertEquals(1, calls.stream().filter(c -> c.method.equals("PATCH")).count());
    }

    @Test
    void replacingKnownFolderChecksSavedSubtreeBeforeWriting() {
        repo(true);
        folderAt("0".repeat(40), ROOT, FOLDER);
        head(HEAD);
        folderAt(HEAD, ROOT, "9".repeat(40));

        assertEquals(409, assertThrows(GitHubException.class,
                () -> service.publish("owner/repo", "Subject/Group/student",
                        List.of(new GitHubService.UploadFile("a", new byte[]{1})), "0".repeat(40))).getStatus());
        assertTrue(calls.stream().noneMatch(c -> c.method.equals("POST")));
    }

    @Test
    void editCreatesCommitWithBaseFolderAndPreviousBlobGuard() {
        repo(true);
        head(HEAD);
        folderAt(HEAD, ROOT, FOLDER);
        recursive(FOLDER, false, entry("A.java", "blob", "100644", BLOB, 1));
        expect("GET", "git/blobs/" + BLOB, 200, "{\"encoding\":\"base64\",\"size\":1,\"content\":\"eA==\"}");
        writeObjects(HEAD, ROOT);
        expect("PATCH", "git/refs/heads/main", 200, "{}");

        assertEquals(NEXT, service.edit("owner/repo", "Subject/Group/student", "A.java", BLOB, "new text"));
        Call folderTree = calls.stream().filter(c -> c.method.equals("POST") && c.path.endsWith("/git/trees")).findFirst().orElseThrow();
        assertEquals(FOLDER, mapper.readTree(folderTree.body).get("base_tree").asString());
        assertEquals("A.java", mapper.readTree(folderTree.body).get("tree").get(0).get("path").asString());
    }

    @Test
    void archiveContainsOnlyRelativeFilesOfSelectedFolder() throws Exception {
        repo(true);
        folderAt(HEAD, ROOT, FOLDER);
        recursive(FOLDER, false, entry("src/A.java", "blob", "100644", BLOB, 1));
        expect("GET", "git/blobs/" + BLOB, 200, "{\"encoding\":\"base64\",\"size\":1,\"content\":\"eA==\"}");

        byte[] archive = service.archive("owner/repo", HEAD, "Subject/Group/student");
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive))) {
            assertEquals("src/A.java", zip.getNextEntry().getName());
            assertArrayEquals(new byte[]{'x'}, zip.readAllBytes());
            assertNull(zip.getNextEntry());
        }
    }

    @Test
    void oversizedFilesAreRejectedBeforeReadingBlob() {
        repo(true);
        folderAt(HEAD, ROOT, FOLDER);
        recursive(FOLDER, false, entry("large", "blob", "100644", BLOB, 2 * 1024 * 1024 + 1));
        assertEquals(413, assertThrows(GitHubException.class,
                () -> service.tree("owner/repo", HEAD, "Subject/Group/student")).getStatus());
    }

    @Test
    void upstreamResponseHasFiniteSizeLimit() {
        expect("GET", "", 200, "{\"padding\":\"" + "x".repeat(4 * 1024 * 1024) + "\"}");
        assertEquals(502, assertThrows(GitHubException.class, () -> service.head("owner/repo")).getStatus());
    }

    @Test
    void repositoryMustHaveInitialCommit() {
        repo(true);
        expect("GET", "git/ref/heads/main", 409, "{}");
        assertEquals(409, assertThrows(GitHubException.class, () -> service.verifyRepository("owner/repo")).getStatus());
    }

    @Test
    void editCannotGrowWholeFolderBeyondDownloadLimit() {
        repo(true);
        head(HEAD);
        folderAt(HEAD, ROOT, FOLDER);
        int twoMiB = 2 * 1024 * 1024;
        recursive(FOLDER, false, String.join(",",
                entry("A.java", "blob", "100644", BLOB, 1),
                entry("one.bin", "blob", "100644", BLOB, twoMiB),
                entry("two.bin", "blob", "100644", BLOB, twoMiB),
                entry("three.bin", "blob", "100644", BLOB, twoMiB),
                entry("four.bin", "blob", "100644", BLOB, twoMiB),
                entry("five.bin", "blob", "100644", BLOB, twoMiB - 1)));

        assertEquals(413, assertThrows(GitHubException.class,
                () -> service.edit("owner/repo", "Subject/Group/student", "A.java", BLOB, "xx")).getStatus());
        assertTrue(calls.stream().noneMatch(c -> c.method.equals("POST")));
    }

    @Test
    void externalTreeRejectsCaseCollisions() {
        repo(true);
        folderAt(HEAD, ROOT, FOLDER);
        recursive(FOLDER, false, entry("A.java", "blob", "100644", BLOB, 1) + ","
                + entry("a.java", "blob", "100644", BLOB, 1));

        assertEquals(409, assertThrows(GitHubException.class,
                () -> service.tree("owner/repo", HEAD, "Subject/Group/student")).getStatus());
    }

    @Test
    void externalTreeRejectsFileParentWithDifferentCase() {
        repo(true);
        folderAt(HEAD, ROOT, FOLDER);
        recursive(FOLDER, false, entry("src", "blob", "100644", BLOB, 1) + ","
                + entry("SRC/A.java", "blob", "100644", BLOB, 1));

        assertEquals(409, assertThrows(GitHubException.class,
                () -> service.tree("owner/repo", HEAD, "Subject/Group/student")).getStatus());
    }

    @Test
    void directPublicationRejectsCaseCollidingNames() {
        repo(true);
        assertEquals(400, assertThrows(GitHubException.class,
                () -> service.publish("owner/repo", "Subject/Group/student", List.of(
                        new GitHubService.UploadFile("A.java", new byte[]{1}),
                        new GitHubService.UploadFile("a.java", new byte[]{1})), null)).getStatus());
        assertTrue(calls.stream().noneMatch(c -> c.method.equals("POST")));
    }

    @Test
    void directPublicationRejectsFileParentWithDifferentCase() {
        repo(true);
        assertEquals(400, assertThrows(GitHubException.class,
                () -> service.publish("owner/repo", "Subject/Group/student", List.of(
                        new GitHubService.UploadFile("src", new byte[]{1}),
                        new GitHubService.UploadFile("SRC/A.java", new byte[]{1})), null)).getStatus());
        assertTrue(calls.stream().noneMatch(c -> c.method.equals("POST")));
    }

    @Test
    void archiveManyKeepsBothPinnedVersionsFullPathsAndBinaryBytes() throws Exception {
        repo(true);
        folderAt(HEAD, ROOT, FOLDER);
        recursive(FOLDER, false, entry("src", "tree", "040000", SUBJECT, 0) + ","
                + entry("src/A.java", "blob", "100644", BLOB, 3));
        blob(new byte[]{0, (byte)255, (byte)128});
        folderAt(NEXT, ROOT, FOLDER, "other");
        recursive(FOLDER, false, entry("src/A.java", "blob", "100644", BLOB, 1));
        blob(new byte[]{'x'});

        byte[] archive = service.archiveMany(List.of(
                archiveSource(1L, HEAD, "student"), archiveSource(2L, NEXT, "other")));

        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive))) {
            var first = zip.getNextEntry();
            assertEquals("Subject/Group/student/src/A.java", first.getName());
            assertEquals(0, first.getTime());
            assertArrayEquals(new byte[]{0, (byte)255, (byte)128}, zip.readAllBytes());
            assertEquals("Subject/Group/other/src/A.java", zip.getNextEntry().getName());
            assertArrayEquals(new byte[]{'x'}, zip.readAllBytes());
            assertNull(zip.getNextEntry());
        }
        assertEquals(1, calls.stream().filter(c -> c.path.equals("/repos/owner/repo")).count());
        assertTrue(calls.stream().allMatch(c -> c.method.equals("GET")));
        assertTrue(calls.stream().noneMatch(c -> c.path.contains("/git/ref/")));
    }

    @ParameterizedTest
    @MethodSource("invalidArchiveSources")
    void archiveManyRejectsInvalidSourcesBeforeNetwork(List<GitHubService.ArchiveSource> sources, int status) {
        assertEquals(status, assertThrows(GitHubException.class, () -> service.archiveMany(sources)).getStatus());
        assertTrue(calls.isEmpty());
    }

    static Stream<Arguments> invalidArchiveSources() {
        return Stream.of(
                Arguments.of(null, 404), Arguments.of(List.of(), 404),
                Arguments.of(Collections.singletonList(null), 400),
                Arguments.of(List.of(new GitHubService.ArchiveSource(null, "owner/repo", HEAD, "a")), 400),
                Arguments.of(List.of(new GitHubService.ArchiveSource(0L, "owner/repo", HEAD, "a")), 400),
                Arguments.of(List.of(new GitHubService.ArchiveSource(-1L, "owner/repo", HEAD, "a")), 400),
                Arguments.of(List.of(new GitHubService.ArchiveSource(1L, null, HEAD, "a")), 409),
                Arguments.of(List.of(new GitHubService.ArchiveSource(1L, " ", HEAD, "a")), 409),
                Arguments.of(List.of(new GitHubService.ArchiveSource(1L, "owner/repo", null, "a")), 409),
                Arguments.of(List.of(new GitHubService.ArchiveSource(1L, "owner/repo", " ", "a")), 409),
                Arguments.of(List.of(new GitHubService.ArchiveSource(1L, "owner/repo", HEAD, null)), 409),
                Arguments.of(List.of(new GitHubService.ArchiveSource(1L, "owner/repo", HEAD, " ")), 409),
                Arguments.of(List.of(new GitHubService.ArchiveSource(1L, "owner/repo", "main", "a")), 502),
                Arguments.of(List.of(new GitHubService.ArchiveSource(1L, "other/repo", HEAD, "a")), 400));
    }

    @Test
    void archiveManyRejectsDuplicateWorkBeforeReadingFiles() {
        repo(true);
        var source = archiveSource(1L, HEAD, "student");

        assertEquals(400, assertThrows(GitHubException.class,
                () -> service.archiveMany(List.of(source, source))).getStatus());
        assertEquals(1, calls.size());
    }

    @Test
    void archiveManyRejectsUnsafeFolderBeforeNetwork() {
        assertThrows(BadRequestException.class,
                () -> service.archiveMany(List.of(archiveSource(1L, HEAD, "../secret"))));
        assertTrue(calls.isEmpty());
    }

    @Test
    void archiveManyRejectsPublicRepositoriesBeforeReadingFiles() {
        repo(false);
        assertEquals(409, assertThrows(GitHubException.class,
                () -> service.archiveMany(List.of(archiveSource(1L, HEAD, "student")))).getStatus());
        assertEquals(1, calls.size());
    }

    @Test
    void archiveManyRejectsEmptyWorkInsteadOfReturningPartialArchive() {
        repo(true);
        folderAt(HEAD, ROOT, FOLDER);
        recursive(FOLDER, false, entry("src", "tree", "040000", SUBJECT, 0));

        assertEquals(409, assertThrows(GitHubException.class,
                () -> service.archiveMany(List.of(archiveSource(1L, HEAD, "student")))).getStatus());
    }

    @Test
    void archiveManyRejectsOverlappingZipPathsBeforeReadingSecondBlob() {
        repo(true);
        folderAt(HEAD, ROOT, FOLDER);
        recursive(FOLDER, false, entry("A.java", "blob", "100644", BLOB, 1));
        blob(new byte[]{'x'});
        folderAt(NEXT, ROOT, FOLDER);
        recursive(FOLDER, false, entry("a.java", "blob", "100644", BLOB, 1));

        assertEquals(409, assertThrows(GitHubException.class,
                () -> service.archiveMany(List.of(archiveSource(1L, HEAD, "student"),
                        archiveSource(2L, NEXT, "student")))).getStatus());
        assertEquals(1, calls.stream().filter(c -> c.path.contains("/git/blobs/")).count());
    }

    @ParameterizedTest
    @CsvSource({"200,true", "201,false"})
    void archiveManyEnforcesFileCountAcrossWorks(int count, boolean allowed) throws Exception {
        repo(true);
        folderAt(HEAD, ROOT, FOLDER);
        recursive(FOLDER, false, String.join(",", IntStream.range(0, 100)
                .mapToObj(i -> entry("a" + i, "blob", "100644", BLOB, 0)).toList()));
        for (int i = 0; i < 100; i++) blob(new byte[0]);
        folderAt(NEXT, ROOT, FOLDER, "other");
        recursive(FOLDER, false, String.join(",", IntStream.range(100, count)
                .mapToObj(i -> entry("a" + i, "blob", "100644", BLOB, 0)).toList()));
        for (int i = 100; i < Math.min(count, GitHubFiles.MAX_FILES); i++) blob(new byte[0]);
        var sources = List.of(archiveSource(1L, HEAD, "student"), archiveSource(2L, NEXT, "other"));

        if (allowed) {
            try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(service.archiveMany(sources)))) {
                int files = 0;
                while (zip.getNextEntry() != null) files++;
                assertEquals(count, files);
            }
        } else {
            assertEquals(413, assertThrows(GitHubException.class, () -> service.archiveMany(sources)).getStatus());
        }
        assertEquals(200, calls.stream().filter(c -> c.path.contains("/git/blobs/")).count());
    }

    @Test
    void archiveManyEnforcesTotalBytesAcrossWorksBeforeReadingExcessBlob() {
        repo(true);
        folderAt(HEAD, ROOT, FOLDER);
        recursive(FOLDER, false, String.join(",", IntStream.range(0, 5)
                .mapToObj(i -> entry("file" + i, "blob", "100644", BLOB, GitHubFiles.MAX_FILE_BYTES)).toList()));
        byte[] content = new byte[GitHubFiles.MAX_FILE_BYTES];
        for (int i = 0; i < 5; i++) blob(content);
        folderAt(NEXT, ROOT, FOLDER, "other");
        recursive(FOLDER, false, entry("extra", "blob", "100644", BLOB, 1));

        assertEquals(413, assertThrows(GitHubException.class, () -> service.archiveMany(List.of(
                archiveSource(1L, HEAD, "student"), archiveSource(2L, NEXT, "other")))).getStatus());
        assertEquals(5, calls.stream().filter(c -> c.path.contains("/git/blobs/")).count());
    }

    private static GitHubService.ArchiveSource archiveSource(long id, String commit, String student) {
        return new GitHubService.ArchiveSource(id, "owner/repo", commit, "Subject/Group/" + student);
    }

    private void blob(byte[] content) {
        expect("GET", "git/blobs/" + BLOB, 200, "{\"encoding\":\"base64\",\"size\":" + content.length
                + ",\"content\":\"" + Base64.getEncoder().encodeToString(content) + "\"}");
    }

    private void repo(boolean privateRepo) {
        expect("GET", "", 200, "{\"private\":" + privateRepo + ",\"default_branch\":\"main\"}");
    }

    private void head(String commit) {
        expect("GET", "git/ref/heads/main", 200, "{\"object\":{\"sha\":\"" + commit + "\"}}");
    }

    private void emptyRoot(String commit, String root) {
        expect("GET", "git/commits/" + commit, 200, "{\"tree\":{\"sha\":\"" + root + "\"}}");
        expect("GET", "git/trees/" + root, 200, "{\"truncated\":false,\"tree\":[]}");
    }

    private void folderAt(String commit, String root, String folder) {
        folderAt(commit, root, folder, "student");
    }

    private void folderAt(String commit, String root, String folder, String student) {
        expect("GET", "git/commits/" + commit, 200, "{\"tree\":{\"sha\":\"" + root + "\"}}");
        expect("GET", "git/trees/" + root, 200, "{\"truncated\":false,\"tree\":[" + entry("Subject", "tree", "040000", SUBJECT, 0) + "]}");
        expect("GET", "git/trees/" + SUBJECT, 200, "{\"truncated\":false,\"tree\":[" + entry("Group", "tree", "040000", GROUP, 0) + "]}");
        expect("GET", "git/trees/" + GROUP, 200, "{\"truncated\":false,\"tree\":[" + entry(student, "tree", "040000", folder, 0) + "]}");
    }

    private void recursive(String folder, boolean truncated, String entries) {
        expect("GET", "git/trees/" + folder + "?recursive=1", 200,
                "{\"truncated\":" + truncated + ",\"tree\":[" + entries + "]}");
    }

    private String entry(String path, String type, String mode, String sha, long size) {
        return "{\"path\":\"" + path + "\",\"type\":\"" + type + "\",\"mode\":\"" + mode
                + "\",\"sha\":\"" + sha + "\",\"size\":" + size + "}";
    }

    private void writeObjects(String parent, String root) {
        expect("POST", "git/blobs", 201, "{\"sha\":\"" + BLOB + "\"}");
        expect("POST", "git/trees", 201, "{\"sha\":\"" + NEW_TREE + "\"}");
        commitObjects();
    }

    private void commitObjects() {
        expect("POST", "git/trees", 201, "{\"sha\":\"" + ROOT + "\"}");
        expect("POST", "git/commits", 201, "{\"sha\":\"" + NEXT + "\"}");
    }

    private void expect(String method, String suffix, int status, String body) {
        replies.add(new Reply(method, "/repos/owner/repo" + (suffix.isEmpty() ? "" : "/" + suffix), status, body));
    }

    private record Reply(String method, String path, int status, String body) {}
    private record Call(String method, String path, String body, String authorization) {}
}
