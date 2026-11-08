package com.inzinjeri.rs_projekat.services;

import com.inzinjeri.rs_projekat.exceptions.GitHubException;
import com.inzinjeri.rs_projekat.utils.GitHubFiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class GitHubService {
    private static final int MAX_RESPONSE_BYTES = 4 * 1024 * 1024;
    private static final int MAX_REF_ATTEMPTS = 3;
    private final RestClient client;
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final String owner;
    private final String token;

    public record UploadFile(String path, byte[] content) {}
    public record TreeEntry(String path, String type, String sha, long size) {}
    public record FileContent(String path, String sha, String commit, byte[] content, boolean binary) {}

    @Autowired
    public GitHubService(@Value("${app.github.owner:}") String owner,
                         @Value("${app.github.token:}") String token) {
        this(owner, token, productionClient());
    }

    GitHubService(String owner, String token, RestClient client) {
        this.owner = owner == null ? "" : owner.trim();
        this.token = token == null ? "" : token.trim();
        this.client = client.mutate()
                .defaultHeader("Authorization", "Bearer " + this.token)
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2026-03-10")
                .defaultHeader("User-Agent", "rs-projekat")
                .build();
    }

    private static RestClient productionClient() {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(15));
        return RestClient.builder().baseUrl("https://api.github.com").requestFactory(factory).build();
    }

    public void verifyRepository(String repo) {
        String branch = repositoryBranch(repo);
        head(repo, branch);
    }

    public String head(String repo) {
        return head(repo, repositoryBranch(repo));
    }

    public List<TreeEntry> tree(String repo, String commit, String folder) {
        repositoryBranch(repo);
        return treeAt(repo, commit, folder);
    }

    public FileContent read(String repo, String commit, String folder, String path) {
        repositoryBranch(repo);
        path = GitHubFiles.path(path);
        List<TreeEntry> entries = treeAt(repo, commit, folder);
        return readBlob(repo, commit, file(entries, path));
    }

    public byte[] archive(String repo, String commit, String folder) {
        repositoryBranch(repo);
        List<TreeEntry> entries = treeAt(repo, commit, folder);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ZipOutputStream zip = new ZipOutputStream(output)) {
            long total = 0;
            for (TreeEntry entry : entries) {
                if (!"blob".equals(entry.type())) continue;
                byte[] bytes = readBlob(repo, commit, entry).content();
                total += bytes.length;
                if (total > GitHubFiles.MAX_TOTAL_BYTES) throw limit();
                ZipEntry zipped = new ZipEntry(entry.path());
                zipped.setTime(0);
                zip.putNextEntry(zipped);
                zip.write(bytes);
                zip.closeEntry();
            }
            zip.finish();
            return output.toByteArray();
        } catch (IOException e) {
            throw new GitHubException(502, "Nije moguce pripremiti arhivu projekta");
        }
    }

    public String publish(String repo, String folder, List<UploadFile> files, String expectedCommit) {
        String branch = repositoryBranch(repo);
        folder = GitHubFiles.path(folder);
        validateUpload(files);
        if (expectedCommit != null) sha(expectedCommit);
        String replacement = null;
        String expectedFolder = null;
        if (expectedCommit != null) {
            expectedFolder = folderTree(repo, commitTree(repo, expectedCommit), folder);
            if (expectedFolder == null) throw new GitHubException(409, "Sacuvana verzija nema trazeni folder");
        }
        for (int attempt = 0; attempt < MAX_REF_ATTEMPTS; attempt++) {
            String parent = head(repo, branch);
            String root = commitTree(repo, parent);
            String current = folderTree(repo, root, folder);
            if (expectedCommit == null ? current != null : !expectedFolder.equals(current)) {
                throw new GitHubException(409,
                        "Folder vec postoji ili je izmenjen. Proveri GitHub i sacuvanu vezu pre ponovnog upisa");
            }
            if (replacement == null) {
                List<Map<String, Object>> entries = new ArrayList<>();
                for (UploadFile file : files) {
                    entries.add(blobEntry(file.path(), writeBlob(repo, file.content())));
                }
                replacement = responseSha(request(repo, HttpMethod.POST, "git/trees", Map.of("tree", entries), false));
            }
            try {
                return commitFolder(repo, branch, parent, root, folder, replacement);
            } catch (RefConflict ignored) {
                // Samo pomeranje grane ponavljamo; folder ponovo proveravamo pre upisa.
            }
        }
        throw new GitHubException(409, "Repozitorijum se istovremeno menja. Pokusaj ponovo");
    }

    public String edit(String repo, String folder, String path, String previousBlobSha, String text) {
        String branch = repositoryBranch(repo);
        folder = GitHubFiles.path(folder);
        path = GitHubFiles.path(path);
        sha(previousBlobSha);
        byte[] content = textBytes(text);
        String replacementBlob = null;
        for (int attempt = 0; attempt < MAX_REF_ATTEMPTS; attempt++) {
            String parent = head(repo, branch);
            String root = commitTree(repo, parent);
            String currentFolder = folderTree(repo, root, folder);
            if (currentFolder == null) throw new GitHubException(404, "Folder rada ne postoji");
            List<TreeEntry> currentEntries = entries(repo, currentFolder);
            TreeEntry currentFile = file(currentEntries, path);
            if (!previousBlobSha.equals(currentFile.sha())) {
                throw new GitHubException(409, "Fajl je u medjuvremenu izmenjen. Ucitaj novu verziju pre cuvanja");
            }
            long total = currentEntries.stream().mapToLong(TreeEntry::size).sum();
            if (total - currentFile.size() + content.length > GitHubFiles.MAX_TOTAL_BYTES) throw limit();
            if (readBlob(repo, parent, currentFile).binary()) {
                throw new GitHubException(400, "Binarni fajl nije moguce menjati tekstualnim editorom");
            }
            if (replacementBlob == null) replacementBlob = writeBlob(repo, content);
            String replacementFolder = responseSha(request(repo, HttpMethod.POST, "git/trees",
                    Map.of("base_tree", currentFolder, "tree", List.of(blobEntry(path, replacementBlob))), false));
            try {
                return commitFolder(repo, branch, parent, root, folder, replacementFolder);
            } catch (RefConflict ignored) {
                // Prethodni blob SHA ostaje uslov i nakon pomeranja zajednicke grane.
            }
        }
        throw new GitHubException(409, "Repozitorijum se istovremeno menja. Ucitaj fajl i pokusaj ponovo");
    }

    private String repositoryBranch(String repo) {
        if (owner.isBlank() || token.isBlank()) {
            throw new GitHubException(503, "GitHub nije podesen: potrebni su GITHUB_OWNER i GITHUB_TOKEN");
        }
        if (repo == null || !repo.matches("[A-Za-z0-9-]+/[A-Za-z0-9._-]+")
                || !repo.substring(0, repo.indexOf('/')).equalsIgnoreCase(owner)) {
            throw new GitHubException(400, "Repozitorijum mora pripadati podesenom GitHub vlasniku");
        }
        JsonNode metadata = request(repo, HttpMethod.GET, "", null, false);
        if (!metadata.path("private").asBoolean()) {
            throw new GitHubException(409, "Repozitorijum roka mora biti privatan");
        }
        String branch = metadata.path("default_branch").asString("");
        if (branch.isBlank()) throw new GitHubException(409, "Inicijalizuj GitHub repo README fajlom");
        if (!branch.matches("[A-Za-z0-9._/-]{1,255}") || branch.contains("..")
                || branch.startsWith("/") || branch.contains("//") || branch.endsWith("/")) {
            throw malformed();
        }
        return branch;
    }

    private String head(String repo, String branch) {
        try {
            return sha(request(repo, HttpMethod.GET, "git/ref/heads/" + branch, null, false)
                    .path("object").path("sha").asString(""));
        } catch (GitHubException e) {
            if (e.getStatus() == 404 || e.getStatus() == 409) {
                throw new GitHubException(409, "Repo nema pocetni commit. Dodaj README na GitHub-u");
            }
            throw e;
        }
    }

    private String commitTree(String repo, String commit) {
        return sha(request(repo, HttpMethod.GET, "git/commits/" + sha(commit), null, false)
                .path("tree").path("sha").asString(""));
    }

    private String folderTree(String repo, String root, String folder) {
        String current = root;
        for (String segment : GitHubFiles.path(folder).split("/")) {
            JsonNode tree = request(repo, HttpMethod.GET, "git/trees/" + current, null, false);
            checkTree(tree);
            JsonNode found = null;
            for (JsonNode entry : tree.get("tree")) {
                if (segment.equals(entry.path("path").asString())) {
                    found = entry;
                    break;
                }
            }
            if (found == null) return null;
            if (!"tree".equals(found.path("type").asString()) || !"040000".equals(found.path("mode").asString())) {
                throw new GitHubException(409, "Putanja foldera se poklapa sa fajlom ili nepodrzanom Git stavkom");
            }
            current = sha(found.path("sha").asString(""));
        }
        return current;
    }

    private List<TreeEntry> treeAt(String repo, String commit, String folder) {
        String folderSha = folderTree(repo, commitTree(repo, commit), folder);
        if (folderSha == null) throw new GitHubException(404, "Folder ne postoji u trazenoj verziji");
        return entries(repo, folderSha);
    }

    private List<TreeEntry> entries(String repo, String folderSha) {
        JsonNode response = request(repo, HttpMethod.GET, "git/trees/" + folderSha + "?recursive=1", null, false);
        checkTree(response);
        List<TreeEntry> entries = new ArrayList<>();
        Set<String> paths = new HashSet<>();
        Set<String> filePaths = new HashSet<>();
        int files = 0;
        long total = 0;
        for (JsonNode entry : response.get("tree")) {
            String path = GitHubFiles.path(entry.path("path").asString(""));
            if (!paths.add(path.toLowerCase(Locale.ROOT))) {
                throw new GitHubException(409, "Folder sadrzi nazive koji se razlikuju samo po velicini slova");
            }
            String type = entry.path("type").asString("");
            String mode = entry.path("mode").asString("");
            boolean directory = "tree".equals(type) && "040000".equals(mode);
            if (!directory && !("blob".equals(type) && ("100644".equals(mode) || "100755".equals(mode)))) {
                throw new GitHubException(409, "Simbolicki linkovi i Git submoduli nisu podrzani");
            }
            long size = directory ? 0 : entry.path("size").asLong(-1);
            if (!directory) {
                filePaths.add(path.toLowerCase(Locale.ROOT));
                files++;
                total += size;
                if (size < 0 || size > GitHubFiles.MAX_FILE_BYTES || files > GitHubFiles.MAX_FILES
                        || total > GitHubFiles.MAX_TOTAL_BYTES) throw limit();
            }
            entries.add(new TreeEntry(path, type, sha(entry.path("sha").asString("")), size));
        }
        rejectFileParents(paths, filePaths, 409);
        return List.copyOf(entries);
    }

    private FileContent readBlob(String repo, String commit, TreeEntry entry) {
        JsonNode response = request(repo, HttpMethod.GET, "git/blobs/" + entry.sha(), null, false);
        if (!"base64".equals(response.path("encoding").asString())) throw malformed();
        byte[] content;
        try {
            content = Base64.getDecoder().decode(response.path("content").asString("").replace("\n", "").replace("\r", ""));
        } catch (IllegalArgumentException e) {
            throw malformed();
        }
        if (content.length > GitHubFiles.MAX_FILE_BYTES) throw limit();
        if (response.path("size").asLong(-1) != content.length || entry.size() != content.length) throw malformed();
        return new FileContent(entry.path(), entry.sha(), commit, content, !GitHubFiles.isText(content));
    }

    private TreeEntry file(List<TreeEntry> entries, String path) {
        return entries.stream().filter(e -> "blob".equals(e.type()) && path.equals(e.path()))
                .findFirst().orElseThrow(() -> new GitHubException(404, "Fajl ne postoji u trazenoj verziji"));
    }

    private String writeBlob(String repo, byte[] content) {
        return responseSha(request(repo, HttpMethod.POST, "git/blobs",
                Map.of("encoding", "base64", "content", Base64.getEncoder().encodeToString(content)), false));
    }

    private Map<String, Object> blobEntry(String path, String blob) {
        return Map.of("path", path, "mode", "100644", "type", "blob", "sha", blob);
    }

    private String commitFolder(String repo, String branch, String parent, String root, String folder, String replacement) {
        String newRoot = responseSha(request(repo, HttpMethod.POST, "git/trees",
                Map.of("base_tree", root, "tree", List.of(
                        Map.of("path", folder, "mode", "040000", "type", "tree", "sha", replacement))), false));
        String commit = responseSha(request(repo, HttpMethod.POST, "git/commits",
                Map.of("message", "Sacuvani fajlovi kroz ispitnu aplikaciju", "tree", newRoot, "parents", List.of(parent)), false));
        request(repo, HttpMethod.PATCH, "git/refs/heads/" + branch, Map.of("sha", commit, "force", false), true);
        return commit;
    }

    private void validateUpload(List<UploadFile> files) {
        if (files == null || files.isEmpty() || files.size() > GitHubFiles.MAX_FILES) {
            throw new GitHubException(400, "Objava mora imati izmedju 1 i 200 fajlova");
        }
        Set<String> paths = new HashSet<>();
        long total = 0;
        for (UploadFile file : files) {
            if (file == null || file.content() == null) throw new GitHubException(400, "Nedostaje sadrzaj fajla");
            String path = GitHubFiles.path(file.path());
            if (!paths.add(path.toLowerCase(Locale.ROOT))) throw new GitHubException(400, "Putanje fajlova se ponavljaju");
            total += file.content().length;
            if (file.content().length > GitHubFiles.MAX_FILE_BYTES || total > GitHubFiles.MAX_TOTAL_BYTES) throw limit();
        }
        rejectFileParents(paths, paths, 400);
    }

    private void rejectFileParents(Set<String> paths, Set<String> filePaths, int status) {
        for (String path : paths) {
            int slash = path.indexOf('/');
            while (slash >= 0) {
                if (filePaths.contains(path.substring(0, slash))) {
                    throw new GitHubException(status, "Putanja je istovremeno fajl i folder");
                }
                slash = path.indexOf('/', slash + 1);
            }
        }
    }

    private byte[] textBytes(String text) {
        if (text == null) throw new GitHubException(400, "Nedostaje tekst fajla");
        try {
            ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder().encode(CharBuffer.wrap(text));
            if (encoded.remaining() > GitHubFiles.MAX_FILE_BYTES) throw limit();
            byte[] bytes = new byte[encoded.remaining()];
            encoded.get(bytes);
            if (!GitHubFiles.isText(bytes)) throw new GitHubException(400, "Sadrzaj nije ispravan tekstualni fajl");
            return bytes;
        } catch (CharacterCodingException e) {
            throw new GitHubException(400, "Tekst nije ispravan UTF-8");
        }
    }

    private void checkTree(JsonNode response) {
        if (response.path("truncated").asBoolean() || !response.path("tree").isArray()) {
            throw new GitHubException(502, "GitHub nije vratio potpuno stablo fajlova");
        }
    }

    private String responseSha(JsonNode response) {
        return sha(response.path("sha").asString(""));
    }

    private String sha(String value) {
        if (value == null || !value.matches("(?:[0-9a-fA-F]{40}|[0-9a-fA-F]{64})")) throw malformed();
        return value;
    }

    private JsonNode request(String repo, HttpMethod method, String suffix, Object body, boolean refUpdate) {
        try {
            RestClient.RequestBodySpec request = client.method(method).uri("/repos/" + repo + (suffix.isEmpty() ? "" : "/" + suffix));
            if (body != null) request.contentType(MediaType.APPLICATION_JSON).body(body);
            return request.exchange((httpRequest, response) -> {
                int status = response.getStatusCode().value();
                if (status < 200 || status >= 300) {
                    if (refUpdate && (status == 409 || status == 422)) throw new RefConflict();
                    if (status == 429 || "0".equals(response.getHeaders().getFirst("X-RateLimit-Remaining"))
                            || response.getHeaders().getFirst("Retry-After") != null) {
                        throw new GitHubException(429, "GitHub ogranicava broj poziva. Pokusaj ponovo kasnije");
                    }
                    if (status == 401 || status == 403) {
                        throw new GitHubException(503, "GitHub token nema potreban pristup. Proveri Contents dozvole i izabrani repo");
                    }
                    if (status == 404) throw new GitHubException(404, "GitHub repo ili trazena verzija nisu dostupni podesenom tokenu");
                    if (status == 409) throw new GitHubException(409, "GitHub repo nije inicijalizovan ili je upis u konfliktu");
                    if (status == 422) throw new GitHubException(409, "GitHub odbija upis. Proveri sadrzaj i pravila grane");
                    throw new GitHubException(502, "GitHub trenutno ne moze da obradi zahtev");
                }
                byte[] bytes = response.getBody().readNBytes(MAX_RESPONSE_BYTES + 1);
                if (bytes.length > MAX_RESPONSE_BYTES) throw new GitHubException(502, "GitHub odgovor prelazi dozvoljenu velicinu");
                try {
                    JsonNode value = mapper.readTree(bytes);
                    if (value == null || !value.isObject()) throw malformed();
                    return value;
                } catch (GitHubException e) {
                    throw e;
                } catch (RuntimeException e) {
                    throw malformed();
                }
            });
        } catch (RestClientException e) {
            throw new GitHubException(503, "Veza sa GitHub-om nije uspela ili je isteklo vreme cekanja");
        }
    }

    private GitHubException malformed() {
        return new GitHubException(502, "GitHub je vratio neispravan odgovor ili nedostaje sacuvana verzija");
    }

    private GitHubException limit() {
        return new GitHubException(413, "Projekat prelazi granicu od 200 fajlova, 2 MiB po fajlu ili 10 MiB ukupno");
    }

    private static final class RefConflict extends RuntimeException {}
}
