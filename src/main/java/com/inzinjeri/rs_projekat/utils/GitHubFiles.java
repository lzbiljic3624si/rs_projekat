package com.inzinjeri.rs_projekat.utils;

import com.inzinjeri.rs_projekat.exceptions.BadRequestException;
import com.inzinjeri.rs_projekat.services.GitHubService;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public final class GitHubFiles {
    public static final int MAX_FILES = 200;
    public static final int MAX_FILE_BYTES = 2 * 1024 * 1024;
    public static final int MAX_TOTAL_BYTES = 10 * 1024 * 1024;
    public static final int MAX_PATH_LENGTH = 500;

    private GitHubFiles() {}

    public static List<GitHubService.UploadFile> upload(List<MultipartFile> files, List<String> paths) {
        if (files == null || paths == null || files.isEmpty() || files.size() != paths.size()
                || files.size() > MAX_FILES) {
            throw new BadRequestException("Posalji od 1 do 200 fajlova i po jednu relativnu putanju za svaki fajl");
        }
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (String name : paths) {
            if (!names.add(path(name))) {
                throw new BadRequestException("Putanja fajla je ponovljena");
            }
        }
        for (String name : names) {
            for (int slash = name.indexOf('/'); slash >= 0; slash = name.indexOf('/', slash + 1)) {
                if (names.contains(name.substring(0, slash))) {
                    throw new BadRequestException("Ista putanja ne moze biti i fajl i direktorijum");
                }
            }
        }

        List<GitHubService.UploadFile> result = new ArrayList<>(files.size());
        long total = 0;
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            if (file == null || file.getSize() > MAX_FILE_BYTES) {
                throw new BadRequestException("Jedan fajl sme imati najvise 2 MiB");
            }
            try (InputStream input = file.getInputStream()) {
                byte[] bytes = input.readNBytes(MAX_FILE_BYTES + 1);
                if (bytes.length > MAX_FILE_BYTES) {
                    throw new BadRequestException("Jedan fajl sme imati najvise 2 MiB");
                }
                total += bytes.length;
                if (total > MAX_TOTAL_BYTES) {
                    throw new BadRequestException("Ukupna velicina fajlova sme biti najvise 10 MiB");
                }
                result.add(new GitHubService.UploadFile(paths.get(i), bytes));
            } catch (IOException e) {
                throw new BadRequestException("Fajl nije moguce procitati; ponovi slanje");
            }
        }
        return List.copyOf(result);
    }

    public static String path(String value) {
        if (value == null || value.isBlank() || value.length() > MAX_PATH_LENGTH
                || value.chars().anyMatch(c -> "\\:*?\"<>|".indexOf(c) >= 0)
                || value.codePoints().anyMatch(Character::isISOControl)) {
            throw new BadRequestException("Neispravna relativna putanja fajla");
        }
        for (String part : value.split("/", -1)) {
            String basename = part.split("\\.", 2)[0].stripTrailing();
            if (part.isEmpty() || part.equals(".") || part.equals("..") || part.equalsIgnoreCase(".git")
                    || part.endsWith(".") || part.endsWith(" ")
                    || basename.matches("(?i:CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])")) {
                throw new BadRequestException("Putanja mora biti bezbedna za Windows, bez .git i izlaska iz foldera");
            }
        }
        return value;
    }

    public static String segment(String value, String fallback) {
        if (value != null && value.strip().equalsIgnoreCase(".git")) {
            throw new BadRequestException("Naziv .git nije dozvoljen za folder");
        }
        String result = normalizeSegment(value);
        if (result.isEmpty()) result = normalizeSegment(fallback);
        if (result.isEmpty()) throw new BadRequestException("Nedostaje bezbedan naziv foldera");
        if (result.length() > 80) result = result.substring(0, 80).replaceAll("[._-]+$", "");
        return path(result);
    }

    private static String normalizeSegment(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value.replace("đ", "dj").replace("Đ", "Dj"), Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9._-]+", "-")
                .replaceAll("^[._-]+|[._-]+$", "");
    }

    public static String repository(String url) {
        if (url == null || url.isBlank()) throw invalidRepository();
        URI uri;
        try {
            uri = URI.create(url.strip());
        } catch (IllegalArgumentException e) {
            throw invalidRepository();
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || !"github.com".equalsIgnoreCase(uri.getHost())
                || uri.getPort() != -1 || uri.getRawUserInfo() != null
                || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw invalidRepository();
        }
        String[] parts = uri.getRawPath().split("/", -1);
        if (parts.length == 4 && !parts[3].isEmpty() || parts.length != 3 && parts.length != 4
                || !parts[0].isEmpty()) {
            throw invalidRepository();
        }
        String owner = parts[1];
        String repo = parts[2];
        if (repo.toLowerCase(Locale.ROOT).endsWith(".git")) repo = repo.substring(0, repo.length() - 4);
        if (!owner.matches("[A-Za-z0-9](?:[A-Za-z0-9-]{0,37}[A-Za-z0-9])?")
                || !repo.matches("[A-Za-z0-9._-]{1,100}") || repo.equals(".") || repo.equals("..")) {
            throw invalidRepository();
        }
        return (owner + "/" + repo).toLowerCase(Locale.ROOT);
    }

    private static BadRequestException invalidRepository() {
        return new BadRequestException("Adresa repozitorijuma mora biti https://github.com/vlasnik/repozitorijum");
    }

    public static String folderUrl(String repo, String commit, String folder) {
        String canonicalRepo = repository("https://github.com/" + repo);
        if (commit == null || !commit.matches("[A-Za-z0-9_-]{1,100}")) {
            throw new BadRequestException("Nedostaje ispravna GitHub verzija fajlova");
        }
        var uri = UriComponentsBuilder.fromUriString("https://github.com")
                .pathSegment(canonicalRepo.split("/"))
                .pathSegment("tree", commit)
                .pathSegment(path(folder).split("/"));
        return uri.build().encode(StandardCharsets.UTF_8).toUriString();
    }

    public static boolean isText(byte[] bytes) {
        if (bytes == null) return false;
        try {
            String text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
            return text.codePoints().noneMatch(c -> Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t');
        } catch (CharacterCodingException e) {
            return false;
        }
    }
}
